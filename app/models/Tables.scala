package models

import slick.jdbc.PostgresProfile.api._

import java.sql.Timestamp


// importable object
object Tables extends {
  val profile = slick.jdbc.PostgresProfile
} with Tables

// schemas of the tables
class Tables {
  class Slots(tag: Tag) extends Table[(Int, Timestamp, Timestamp, Int, Short)](tag, "SLOTS") {
    def slotID = column[Int]("slot_id", O.PrimaryKey, O.AutoInc)
    def startAt = column[Timestamp]("start_at")
    def endAt = column[Timestamp]("end_at")
    def vacancy = column[Int]("vacancy")
    def status = column[Short]("status")
    def * = (slotID, startAt, endAt, vacancy, status)
  }
  lazy val slots = TableQuery[Slots]

  class Users(tag: Tag) extends Table[(Int, String, String, String, String)](tag, "USERS") {
    def userID = column[Int]("user_id", O.PrimaryKey, O.AutoInc)
    def identifier = column[String]("identifier", O.Unique)
    def name = column[String]("name")
    def email = column[String]("email", O.Unique)
    def password = column[String]("password")
    def * = (userID, identifier, name, email, password)
  }
  lazy val users = TableQuery[Users]

  class Bookings(tag: Tag) extends Table[(Int, Short, Int, Int)](tag, "BOOKINGS") {
    def bookingID = column[Int]("booking_id", O.PrimaryKey, O.AutoInc)
    def status = column[Short]("status")
    def userID = column[Int]("user_id")
    def slotID = column[Int]("slot_id")
    def * = (bookingID, status, userID, slotID)

    def user = foreignKey(
      "user_fk", userID, users)(
      _.userID, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    def slot = foreignKey(
      "slot_fk", slotID, slots)(
      _.slotID, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  lazy val bookings = TableQuery[Bookings]
}
