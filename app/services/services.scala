package services

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import models.{DBOperations, Tables}
import models.Tables._
import play.api.cache.redis.{CacheApi, Done, RedisList, SynchronousResult}

import java.sql.Timestamp

class services(cache: CacheApi, dbOperations: DBOperations)(implicit ec: ExecutionContext) {
  // initialize a timeslotsList in redis
  def timeslotsList: RedisList[String, SynchronousResult] = cache.list[String]("timeslotsList")

  // Add a timeslot in redis
  def addATimeslot(curId: Int, start: Timestamp, end: Timestamp, cur_vacancy: Int = 50, cur_status: Short): Unit = {
    cache.set(curId.toString + "startAt", start.toString)
    cache.set(curId.toString + "endAt", end.toString)
    cache.set(curId.toString + "vacancy", cur_vacancy)
    cache.set(curId.toString + "status", cur_status)
    timeslotsList.append(curId.toString)
  }

  // get the instance of timeslot
  def getTimeslot(slotId: Int): (String, String, String, Int, Short) = {
    val startAt = cache.get[String](slotId.toString + "startAt").getOrElse(throw new RuntimeException("Wrong request"))
    val endAt = cache.get[String](slotId.toString + "endAt").getOrElse(throw new RuntimeException("Wrong request"))
    val vacancy = cache.get[Int](slotId.toString + "vacancy").getOrElse(throw new RuntimeException("Wrong request"))
    val status = cache.get[Short](slotId.toString + "status").getOrElse(throw new RuntimeException("Wrong request"))
    (slotId.toString, startAt, endAt, vacancy, status)
  }

  // close time slot in redis
  def closeSlotInRedis(curID: Int): SynchronousResult[Done] = {
    cache.set(curID.toString + "status", 0)
  }

  // edit vacancy for time slot in redis
  def editSlotInRedis(curID: Int, vacancy: Int): SynchronousResult[Done] = {
    cache.set(curID.toString + "vacancy", vacancy)
  }

  // check if a slot is open in redis
  def getSlotOpeningInRedis(curID: Int): Boolean = {
    cache.get[Short](curID.toString + "status").getOrElse(throw new RuntimeException("Wrong request")) == 1
  }

  // get the vacancy of the timeslot
  def getVacancy(slotId: Int): Int = {
    cache.get[String](slotId.toString + "vacancy")
      .getOrElse(throw new RuntimeException("Wrong request"))
      .toInt
  }

  // decrease vacancy of the timeslot by 1 atomically
  def decreaseVacancy(slotId: Int): Int = {
    cache.decrement(slotId.toString + "vacancy").toInt
  }

  // increase vacancy of the timeslot by 1 atomically
  def increaseVacancy(slotId: Int): Int = {
    cache.increment(slotId.toString + "vacancy").toInt
  }

  // check if a user logged in
  def validateUserLogin(email: String, password: String): Future[Option[Int]] = {
    (email, password) match {
      case ("", _) | (_, "") => Future.successful(None)
      case _ => dbOperations.validateUserByEmail(email, password)
    }
  }

  def register(email: String, password: String, name: String, identifier: String): Future[Either[Future[Int], String]] = {
    (email, password, name, identifier) match {
      case ("", _, _, _) | (_, "", _, _) | (_, _, "", _) | (_, _, _, "") => Future.successful(Right("Invalid request"))
      case _ => dbOperations.createUser(name, identifier, email, password)
    }
  }

  def getCurrentOpenSlots: Future[Seq[Slot]] = {
    val slotIdList = cache.list[String]("timeslotsList").toList
    Future.successful(slotIdList.map { id => {
      val (slotId, startAt, endAt, vacancy, status) = getTimeslot(id.toInt)
      Tables.Slot(slotId.toInt, Timestamp.valueOf(startAt), Timestamp.valueOf(endAt), vacancy, status)
    }
    }.filter { s => s.status == 1 })
  }

  def bookASlot(uid: Int, slotID: Int): Future[Either[Future[Int], String]] = {
    (uid, slotID) match {
      case (_, -1) => Future.successful(Right("Invalid Request"))
      case _ =>
        try {
          if (getSlotOpeningInRedis(slotID)) {
            // update vacancy information in Redis atomically
            decreaseVacancy(slotID) match {
              case r: Int if (r >= 0) =>
                dbOperations.bookASlot(uid, slotID) map[Either[Future[Int], String]] {
                  case Left(n: Future[Int]) => Left(n)
                  case Right(e: String) =>
                    // revert change if errors occur
                    increaseVacancy(slotID)
                    Right(e)
                }
              case r: Int if (r < 0) =>
                // revert change if no vacancy
                increaseVacancy(slotID)
                Future.successful(Right("No vacancy"))
              case _ => Future.successful(Right("No vacancy"))
            }
          } else {
            Future.successful(Right("Time slot closed"))
          }
        } catch {
          case _: Throwable => Future.successful(Right("Bad request"))
        }
    }
  }

  def cancelBooking(uid: Int, bookingID: Int): Future[Option[Int]] = {
    bookingID match {
      case -1 => Future.successful(None)
      case _ =>
        if (Await.result(dbOperations.checkBookingOfUser(uid, bookingID), 10.seconds)) {
          val userBookings = Await.result(dbOperations.listBookingsByUser(uid), 10.seconds)
          val slotID = userBookings.filter(booking => booking.bookingID == bookingID).head.slotID
          increaseVacancy(slotID)
          dbOperations.cancelABooking(bookingID)
        } else {
          Future.successful(None)
        }
    }
  }

  def getUserBookings(uid: Int): Future[Seq[Booking]] = {
    dbOperations.listBookingsByUser(uid)
  }

  def editUserBooking(uid: Int, bookingID: Int, slotID: Int): Future[Either[Future[Int], String]] = {
    (uid, bookingID, slotID) match {
      case (_, -1, _) | (_, _, -1) => Future.successful(Right("Invalid request"))
      case _ =>
        if (Await.result(dbOperations.checkBookingOfUser(uid, bookingID), 10.seconds)) {
          decreaseVacancy(slotID) match {
            case r: Int if (r >= 0) =>
              val userBookings = Await.result(dbOperations.listBookingsByUser(uid), 10.seconds)
              val oldSlotID = userBookings.filter(booking => booking.bookingID == bookingID).head.slotID
              increaseVacancy(oldSlotID)
              dbOperations.updateABooking(bookingID, uid, slotID)
            case r: Int if (r < 0) =>
              // revert change if no vacancy
              increaseVacancy(slotID)
              Future.successful(Right("No vacancy"))
            case _ => Future.successful(Right("No vacancy"))
          }
        } else {
          Future.successful(Right("Permission denied"))
        }
    }
  }

  private def checkUserPermission(uid: Int) = {
    Await.result(dbOperations.getUserInfo(uid), 10.seconds) match {
      case Some(u) => u.identity == 0
      case _ => false
    }
  }

  def listBookingsByTimeslot(uid: Int, slotID: Int): Future[Seq[Booking]] = {
    if (checkUserPermission(uid)) {
      slotID match {
        case -1 => Future.successful(Vector.empty[Booking])
        case _ => dbOperations.listBookingsByTimeslot(slotID)
      }
    } else {
      Future.successful(Vector.empty[Booking])
    }
  }

  def createSlot(uid: Int, startAt: Timestamp, endAt: Timestamp, vacancy: Int): Future[Option[Future[Int]]] = {
    if (checkUserPermission(uid)) {
      if (startAt.before(endAt) && vacancy > 0)
        dbOperations.createTimeslot(startAt, endAt, vacancy) map[Option[Future[Int]]] {
          case Some(r: Future[Int]) => Some(r.map[Int] { n: Int =>
            addATimeslot(n, startAt, endAt, vacancy, 1)
            n
          })
          case _ => None
        }
      else Future.successful(None)
    } else {
      Future.successful(None)
    }
  }

  def closeTimeslot(uid: Int, slotID: Int): Future[Int] = {
    if (checkUserPermission(uid)) {
      slotID match {
        case -1 => Future.successful(-1)
        case _ =>
          closeSlotInRedis(slotID)
          dbOperations.updateATimeslot(slotID, 0)
      }
    } else {
      Future.successful(-1)
    }
  }

  def editTimeslot(uid: Int, slotID: Int, vacancy: Int): Future[Int] = {
    if (checkUserPermission(uid)) {
      (slotID, vacancy) match {
        case (-1, _) | (_, -1) => Future.successful(-1)
        case _ =>
          dbOperations.updateVacancy(slotID, vacancy) map[Int] {
            case n: Int if (n > 0) =>
              editSlotInRedis(slotID, vacancy)
              n
            case _ => -1
          }
      }
    } else {
      Future.successful(-1)
    }
  }

  def markBooking(uid: Int, bookingID: Int, status: Short): Future[Option[Int]] = {
    if (checkUserPermission(uid)) {
      status match {
        case 2 => dbOperations.markBookingAttended(bookingID)
        case 3 => dbOperations.markBookingFinished(bookingID)
        case _ => Future.successful(None)
      }
    } else {
      Future.successful(None)
    }
  }

  def getSlotsInPeriod(uid: Int, startAt: Timestamp, endAt: Timestamp): Future[Seq[Slot]] = {
    if (checkUserPermission(uid)) {
      dbOperations.listTimeslots(startAt, endAt)
    } else {
      Future.successful(Seq.empty[Slot])
    }
  }

  // initialize the schemas of tables
  def createTables: Future[String] = {
    dbOperations.init()
  }

  // (one-time insertion) insert sample data to the tables for testing purpose
  def insertSamples(): Future[String] = {
    addATimeslot(1, Timestamp.valueOf("2022-02-16 13:00:00"), Timestamp.valueOf("2022-02-16 15:00:00"), 19, 1)
    addATimeslot(2, Timestamp.valueOf("2022-02-16 15:00:00"), Timestamp.valueOf("2022-02-16 17:00:00"), 6, 1)
    addATimeslot(3, Timestamp.valueOf("2022-02-16 17:00:00"), Timestamp.valueOf("2022-02-16 19:00:00"), 1, 1)
    addATimeslot(4, Timestamp.valueOf("2022-02-16 19:00:00"), Timestamp.valueOf("2022-02-16 21:00:00"), 15, 1)
    dbOperations.insertSamples()
  }
}
