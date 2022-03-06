package models

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}
import models.Tables._

class DBOperations(db: Database)(implicit ec: ExecutionContext) {
  def init(db: Database):Future[Unit] = {
    db.run((slots.schema ++ users.schema ++ bookings.schema).createIfNotExists)
  }
}
