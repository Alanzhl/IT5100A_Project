package models

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{Await, ExecutionContext, Future}
import models.Tables._

import java.sql.Timestamp
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}


class DBOperations(db: Database)(implicit ec: ExecutionContext) {
  // initialize the schemas of tables
  def init(): Future[String] = {
    db.run((slots.schema ++ users.schema ++ bookings.schema).create.asTry).map {
      case Failure(e) => e.getMessage
      case Success(_) => "Tables initiated."
    }
  }

  // (one-time insertion) insert sample data to the tables for testing purpose
  def insertSamples(): Future[String] = {
    db.run(DBIO.seq(
      slots ++= Seq(
        Slot(1, Timestamp.valueOf("2022-02-16 13:00:00"), Timestamp.valueOf("2022-02-16 15:00:00"), 19, 1),
        Slot(2, Timestamp.valueOf("2022-02-16 15:00:00"), Timestamp.valueOf("2022-02-16 17:00:00"), 6, 1),
        Slot(3, Timestamp.valueOf("2022-02-16 17:00:00"), Timestamp.valueOf("2022-02-16 19:00:00"), 0, 2),
        Slot(4, Timestamp.valueOf("2022-02-16 19:00:00"), Timestamp.valueOf("2022-02-16 21:00:00"), 15, 1)
      ),
      users ++= Seq(
        User(1, "A0123456B", "John Doe", "johndoe@u.nus.edu", "user1pw", 1),
        User(2, "A4567890W", "Rolph Hatwells", "rhatwells1@hhs.gov", "user2pw", 0),
        User(3, "A0234587Y", "Olive Yew", "yeosw@u.nus.edu", "user3pw", 1)
      ),
      bookings ++= Seq(
        Booking(1, 3, 1, 2),
        Booking(2, 2, 1, 3),
        Booking(3, 0, 2, 1),
        Booking(4, 1, 2, 3),
        Booking(5, 1, 3, 4)
      )
    ).asTry).map {
      case Failure(e) => e.getMessage
      case Success(_) => "Inserted sample with 4 slots, 3 users and 5 bookings."
    }
  }

  // validate user by his matriculation number
  // return value: validated user id / None
  def validateUserByID(id: Int, password: String): Future[Option[Int]] = {
    val matches = db.run(users.filter(u => u.userID === id && u.password === password).result)
    matches.map(u =>
      if (u.nonEmpty) Some(u.head.userID)
      else None)
  }

  // validate user by his email
  // return value: validated user id / None
  def validateUserByEmail(email: String, password: String): Future[Option[Int]] = {
    val matches = db.run(users.filter(u => u.email === email && u.password === password).result)
    matches.map(u =>
      if (u.nonEmpty) Some(u.head.userID)
      else None)
  }

  // create a new user
  // return value: number of user created / conflict message
  def createUser(name: String, identifier: String, email: String, password: String, identity: Short = 1): Future[Either[Future[Int], String]] = {
    val matches = db.run(users.filter(u => u.identifier === identifier || u.email === email).result)
    matches.map {
      case head +: _ =>
        if (head.identifier == identifier)
          Right(s"Matric number conflict with existing user ${head.userID}.")
        else
          Right(s"Email conflict with existing user ${head.userID}")
      // handle count == 0 at the controller
      case _ => Left(db.run(users += User(0, identifier, name, email, password, identity)))
    }
  }

  // get detailed user info
  // param: user id
  // return value: (userID, identifier, name, email) / None
  def getUserInfo(id: Int): Future[Option[User]] = {
    val matches = db.run(users.filter(u => u.userID === id).result)
    matches.map(r => r.headOption)
  }

  // create a new timeslot
  // return value: slot id / None
  def createTimeslot(startAt: Timestamp, endAt: Timestamp, vacancy: Int = 50): Future[Option[Future[Int]]] = {
    val matches = db.run(slots.filter(s =>
      (s.startAt <= startAt && s.endAt > startAt)
        || (s.startAt < endAt && s.endAt >= endAt)).result)
    matches.map { r => if (r.isEmpty) {
        Some(db.run((slots returning slots.map(_.slotID)) += Slot(0, startAt, endAt, vacancy, 1)))
      } else {
        None
      }
    }
  }

  // get a specific timeslot according to its start time
  // return value: detailed slot messages (slotID, startAt, endAt, vacancy, status)
  def getATimeslot(startAt: Timestamp): Future[Option[Slot]] = {
    db.run(slots.filter(s => s.startAt === startAt).result).map{_.headOption}
  }

  // get the time slots between the interval [startAt, endAt]
  // return value: a list of detailed slot messages (slotID, startAt, endAt, vacancy, status)
  def listTimeslots(startAt: Timestamp, endAt: Timestamp): Future[Seq[Slot]] = {
    db.run(slots.filter(s => s.startAt >= startAt && s.endAt <= endAt).result)
  }

  // update the status of a time slot
  // return value: number of affected rows (fail if it is 0)
  def updateATimeslot(slotID: Int, status: Short): Future[Int] = {
    val q = slots.filter(s => s.slotID === slotID).map(c => c.status)
    db.run(q.update(status))
  }

  // update the vacancy limit of a slot
  // return value: current limit of the slot / None (fail to change due exceeding bookings)
  def updateVacancy(slotID: Int, vacancy: Int): Future[Int] = {
    val q = slots.filter(s => s.slotID === slotID).map(c => c.vacancy)
    db.run(q.update(vacancy))
  }

  // book a slot (please ensure the user exists...)
  // return value: booking ID / None (not successful)
  def bookASlot(userID: Int, slotID: Int): Future[Either[Future[Int], String]] = {
    db.run(bookings.filter(b => b.userID === userID && b.slotID === slotID).result).map(r1 => {
      if (r1.nonEmpty) {
        Right(s"User $userID has already booked slot $slotID.")
      } else {
        Await.result(db.run(slots.filter(s => s.slotID === slotID).result).map( r2 => {
          if (r2.isEmpty) {
            Right(s"Slot $slotID is empty!")
          } else if (r2.head.status == 0) {
            Right(s"Slot $slotID is closed!")
          } else if (r2.head.vacancy == 0) {
            Right(s"Slot $slotID is full!")
          } else {
            Left(db.run((bookings returning bookings.map(_.bookingID)) += Booking(0, 1, userID, slotID)))
          }
        }), 10.seconds)
      }
    })
  }

  // delete a booking record
  // return value: the canceled booking ID / None
  def cancelABooking(bookingID: Int): Future[Option[Int]] = {
    db.run(bookings.filter(b => b.bookingID === bookingID).delete).map{
      case 0 => None
      case _ => Some(bookingID)
    }
  }

  // add a new booking and cancel the old one
  // return value: the new booking ID / booking failure messages
  def updateABooking(bookingID: Int, userID: Int, slotID: Int): Future[Either[Future[Int], String]] = {
    bookASlot(userID, slotID).map{
      case Left(bid) => Await.result(cancelABooking(bookingID).map{_ => Left(bid)}, 10.seconds)
      case Right(info) => Right(info)
    }
  }

  // list all the bookings of a user
  // return value: a list of detailed booking messages (bookingID, status, userID, slotID, startAt, endAt)
  def listBookingsByUser(userID: Int): Future[Seq[Booking]] = {
    db.run(bookings.filter(b => b.userID === userID).result)
  }

  // check if a booking created by a user
  // return value: true or false
  def checkBookingOfUser(userID: Int, bookingID: Int): Future[Boolean] = {
    db.run(bookings.filter(b => b.bookingID === bookingID && b.userID === userID).result).map{_.nonEmpty}
  }

  // list all the bookings in a timeslot
  // return value: a list of detailed booking messages (bookingID, status, userID, slotID, startAt, endAt)
  def listBookingsByTimeslot(slotID: Int): Future[Seq[Booking]] = {
    db.run(bookings.filter(b => b.slotID === slotID).result)
  }

  // mark a booking as "attended"
  // return value: bookingID / None (not exist)
  def markBookingAttended(bookingID: Int): Future[Option[Int]] = {
    val q = bookings.filter(b => b.bookingID === bookingID).map(b => b.status)
    db.run(q.update(2)).map{
      case 0 => None
      case _ => Some(bookingID)
    }
  }

  // mark a booking as "finished"
  // return value: bookingID / None (not exist)
  def markBookingFinished(bookingID: Int): Future[Option[Int]] = {
    val q = bookings.filter(b => b.bookingID === bookingID).map(b => b.status)
    db.run(q.update(3)).map{
      case 0 => None
      case _ => Some(bookingID)
    }
  }
}
