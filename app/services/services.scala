package services

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import models.DBOperations
import models.Tables._

import java.sql.Timestamp

class services(dbOperations: DBOperations) {
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
        if (Await.result(dbOperations.checkBookingOfUser(uid, bookingID), 10.seconds)) {
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
          dbOperations.updateABooking(bookingID, uid, slotID)
        } else {
          Future.successful(Right("Permission denied"))
        }
    }
  }

  def listBookingsByTimeslot(uid: Int, slotID: Int): Future[Seq[Booking]] = {
    slotID match {
      case -1 => Future.successful(Vector.empty[Booking])
      case _ => dbOperations.listBookingsByTimeslot(slotID)
    }
  }

  def createSlot(uid: Int, startAt: Timestamp, endAt: Timestamp, vacancy: Int): Future[Option[Future[Int]]] = {
    if (vacancy < 0) {
      Future.successful(Option[Future[Int]](Future.successful(0)))
    } else {
      dbOperations.createTimeslot(startAt, endAt, vacancy)
    }
  }

  def closeTimeslot(uid: Int, slotID: Int): Future[Int] = {
    slotID match {
      case -1 => Future.successful(-1)
      case _ => dbOperations.updateATimeslot(slotID, 0)
    }
  }

  def editTimeslot(uid: Int, slotID: Int, vacancy: Int): Future[Int] = {
    slotID match {
      case -1 => Future.successful(-1)
      case _ => dbOperations.updateVacancy(slotID, vacancy)
    }
  }

  def markBooking(uid: Int, bookingID: Int, status: Short): Future[Option[Int]] = {
    bookingID match {
      case -1 => Future.successful(Option[Int](-1))
      case _ =>
        if (status == 2) {
          dbOperations.markBookingAttended(bookingID)
        } else {
          dbOperations.markBookingFinished(bookingID)
        }
    }
  }

  def getSlotsInPeriod(uid: Int, startAt: Timestamp, endAt: Timestamp): Future[Seq[Slot]] = {
    dbOperations.listTimeslots(startAt, endAt)
  }
}
