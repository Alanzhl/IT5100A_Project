package services

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import models.DBOperations
import models.Tables._

import java.sql.Timestamp

class services(dbOperations: DBOperations)(implicit ec: ExecutionContext) {
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
    dbOperations.listTimeslots(Timestamp.valueOf("2022-02-16 9:0:0"),
      Timestamp.valueOf("2022-02-17 9:0:0")
    )
  }

  def bookASlot(uid: Int, slotID: Int): Future[Either[Future[Int], String]] = {
    (uid, slotID) match {
      case (_, -1) => Future.successful(Right("Invalid Request"))
      case _ => dbOperations.bookASlot(uid, slotID)
    }
  }

  def cancelBooking(uid: Int, bookingID: Int): Future[Option[Int]] = {
    bookingID match {
      case -1 => Future.successful(None)
      case _ =>
        for {
          valid <- dbOperations.checkBookingOfUser(uid, bookingID)
          bookingID <-
            if (valid) dbOperations.cancelABooking(bookingID) else Future.successful(None)
        } yield bookingID
    }
  }

  def getUserBookings(uid: Int): Future[Seq[Booking]] = {
    dbOperations.listBookingsByUser(uid)
  }

  def editUserBooking(uid: Int, bookingID: Int, slotID: Int): Future[Either[Future[Int], String]] = {
    (uid, bookingID, slotID) match {
      case (_, -1, _) | (_, _, -1) => Future.successful(Right("Invalid request"))
      case _ =>
        for {
          valid <- dbOperations.checkBookingOfUser(uid, bookingID)
          bookingID <-
            if (valid) dbOperations.updateABooking(bookingID, uid, slotID)
            else Future.successful(Right("Permission denied"))
        } yield bookingID
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
      if (vacancy < 0) {
        Future.successful(Some(Future.successful(0)))
      } else {
        dbOperations.createTimeslot(startAt, endAt, vacancy)
      }
    } else {
      Future.successful(Some(Future.successful(0)))
    }
  }

  def closeTimeslot(uid: Int, slotID: Int): Future[Int] = {
    if (checkUserPermission(uid)) {
      slotID match {
        case -1 => Future.successful(-1)
        case _ => dbOperations.updateATimeslot(slotID, 0)
      }
    } else {
      Future.successful(-1)
    }
  }

  def editTimeslot(uid: Int, slotID: Int, vacancy: Int): Future[Int] = {
    if (checkUserPermission(uid)) {
      slotID match {
        case -1 => Future.successful(-1)
        case _ => dbOperations.updateVacancy(slotID, vacancy)
      }
    } else {
      Future.successful(-1)
    }
  }

  def markBooking(uid: Int, bookingID: Int, status: Short): Future[Option[Int]] = {
    if (checkUserPermission(uid)) {
      bookingID match {
        case -1 => Future.successful(Some(-1))
        case _ =>
          if (status == 2) {
            dbOperations.markBookingAttended(bookingID)
          } else {
            dbOperations.markBookingFinished(bookingID)
          }
      }
    } else {
      Future.successful(Some(-1))
    }
  }

  def getSlotsInPeriod(uid: Int, startAt: Timestamp, endAt: Timestamp): Future[Seq[Slot]] = {
    if (checkUserPermission(uid)) {
      dbOperations.listTimeslots(startAt, endAt)
    } else {
      Future.successful(Seq.empty[Slot])
    }
  }
}
