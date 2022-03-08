package services

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import models.DBOperations

import java.sql.Timestamp
import java.time

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

  def getCurrentOpenSlots: Future[List[(Int, Timestamp, Timestamp, Int, Short)]] = {
    dbOperations.listTimeslots(Timestamp.valueOf(time.LocalDateTime.now),
      Timestamp.valueOf(time.LocalDateTime.now.plusWeeks(1))
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
      case -1 => Future.successful(Option[Int](-1))
      case _ =>
        if (Await.result(dbOperations.checkBookingOfUser(uid, bookingID), 10.seconds)) {
          dbOperations.cancelABooking(bookingID)
        } else {
          Future.successful(Option[Int](-1))
        }
    }
  }

  def getUserBookings(uid: Int): Future[List[(Int, Short, Int, Int, Timestamp, Timestamp)]] = {
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
}
