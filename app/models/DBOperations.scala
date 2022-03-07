package models

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}
import models.Tables._

import java.sql.Timestamp


class DBOperations(db: Database)(implicit ec: ExecutionContext) {
  // initialize the schemas of tables
  def init():Future[Unit] = {
    db.run((slots.schema ++ users.schema ++ bookings.schema).createIfNotExists)
  }

  // (one-time insertion) insert sample data to the tables for testing purpose
  def insertSamples(): Future[Unit] = {
    db.run(DBIO.seq(
      slots ++= Seq(
        (1, Timestamp.valueOf("2022-02-16 13:00:00"), Timestamp.valueOf("2022-02-16 15:00:00"), 19, 1),
        (2, Timestamp.valueOf("2022-02-16 15:00:00"), Timestamp.valueOf("2022-02-16 17:00:00"), 6, 1),
        (3, Timestamp.valueOf("2022-02-16 17:00:00"), Timestamp.valueOf("2022-02-16 19:00:00"), 0, 2),
        (4, Timestamp.valueOf("2022-02-16 19:00:00"), Timestamp.valueOf("2022-02-16 21:00:00"), 15, 1)
      ),
      users ++= Seq(
        (1, "A0123456B", "John Doe", "johndoe@u.nus.edu", "user1pw"),
        (2, "A4567890W", "Rolph Hatwells", "rhatwells1@hhs.gov", "user2pw"),
        (3, "A0234587Y", "Olive Yew", "yeosw@u.nus.edu", "user3pw")
      ),
      bookings ++= Seq(
        (1, 3, 1, 2),
        (2, 2, 1, 3),
        (3, 0, 2, 1),
        (4, 1, 2, 3),
        (5, 1, 3, 4)
      )
    ))
  }

  // validate user by his matriculation number
  // return value: validated user id / None
  def validateUserByID(id: Int, password: String): Future[Option[Int]] = {
    val matches = db.run(users.filter(u => u.userID === id && u.password === password).result)
    matches.map(u =>
      if (u.isEmpty) Some(u.head._1)
      else None)
  }

  // validate user by his email
  // return value: validated user id / None
  def validateUserByEmail(email: String, password: String): Future[Option[Int]] = {
    val matches = db.run(users.filter(u => u.email === email && u.password === password).result)
    matches.map(u =>
      if (u.isEmpty) Some(u.head._1)
      else None)
  }

  // create a new user
  // return value: number of user created / conflict message
  def createUser(name: String, identifier: String, email: String, password: String): Future[Either[Future[Int], String]] = {
    val matches = db.run(users.filter(u => u.identifier === identifier || u.email === email).result)
    matches.map(u => if (u.head._2 == identifier) {
      Right(s"Matric number conflict with existing user ${u.head._1}.")
    } else if (u.head._4 == email) {
      Right(s"Email conflict with existing user ${u.head._4}")
    } else {
      Left(db.run(users += (0, identifier, name, email, password)))    // handle count == 0 at the controller
    })
  }

  // get detailed user info
  // param: user id
  // return value: (userID, identifier, name, email) / None
  def getUserInfo(id: Int): Future[Option[(Int, String, String, String)]] = ???

  // create a new timeslot
  // return value: slot id / None
  def createTimeslot(startAt: Timestamp, endAt: Timestamp, vacancy: Int = 50):Future[Option[Future[Int]]] = ???

  // get a specific timeslot according to its start time
  // return value: detailed slot messages (slotID, startAt, endAt, vacancy, status)
  def getATimeslot(startAt: Timestamp): Future[Option[(Int, Timestamp, Timestamp, Int, Short)]] = ???

  // get the time slots between the interval [startAt, endAt]
  // return value: a list of detailed slot messages (slotID, startAt, endAt, vacancy, status)
  def listTimeslots(startAt: Timestamp, endAt: Timestamp): Future[List[(Int, Timestamp, Timestamp, Int, Short)]] = ???

  // update the status of a time slot
  // return value: current status of the slot
  def updateATimeslot(slotID: Int, status: Short): Future[Short] = ???

  // update the vacancy limit of a slot
  // return value: current limit of the slot / None (fail to change due exceeding bookings)
  def updateVacancy(slotID: Int, vacancy: Int): Future[Option[Int]] = ???

  // book a slot
  // return value: booking ID / None (not successful)
  def bookASlot(userID: Int, slotID: Int): Future[Either[Future[Int], String]] = ???

  // delete a booking record
  // return value: the canceled booking ID / None
  def cancelABooking(bookingID: Int): Future[Option[Int]] = ???

  // add a new booking and delete the old one
  // return value: the new booking ID / booking failure messages
  def updateABooking(bookingID: Int, userID: Int, slotID: Int): Future[Either[Future[Int], String]] = ???

  // list all the bookings of a user
  // return value: a list of detailed booking messages (bookingID, status, userID, slotID, startAt, endAt)
  def listBookingsByUser(userID: Int): Future[List[(Int, Short, Int, Int, Timestamp, Timestamp)]] = ???

  // list all the bookings in a timeslot
  // return value: a list of detailed booking messages (bookingID, status, userID, slotID, startAt, endAt)
  def listBookingsByTimeslot(slotID: Int): Future[List[(Int, Short, Int, Int, Timestamp, Timestamp)]] = ???

  // mark a booking as "attended"
  // return value: bookingID / None (not exist)
  def markBookingAttended(bookingID: Int): Future[Option[Int]] = ???

  // mark a booking as "finished"
  // return value: bookingID / None (not exist)
  def markBookingFinished(bookingID: Int): Future[Option[Int]] = ???
}
