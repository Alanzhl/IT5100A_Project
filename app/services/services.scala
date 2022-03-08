package services

import scala.concurrent.Future
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
}
