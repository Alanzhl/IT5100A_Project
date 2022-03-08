package models

import slick.jdbc.PostgresProfile.api._

import java.sql.Timestamp


// importable object
object Tables extends {
  val profile = slick.jdbc.PostgresProfile
} with Tables

// schemas of the tables
class Tables {
  case class Slot(slotID:Int, startAt:Timestamp, endAt:Timestamp, vacancy:Int, status:Short)
  class Slots(tag: Tag) extends Table[Slot](tag, "SLOTS") {
    def slotID = column[Int]("slot_id", O.AutoInc, O.PrimaryKey)
    def startAt = column[Timestamp]("start_at")
    def endAt = column[Timestamp]("end_at")
    def vacancy = column[Int]("vacancy")
    def status = column[Short]("status")
    def * = (slotID, startAt, endAt, vacancy, status) <> (Slot.tupled, Slot.unapply)
  }
  lazy val slots = TableQuery[Slots]

  case class User(userID:Int, identifier:String, name:String, email:String, password:String, identity: Short)
  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def userID = column[Int]("user_id", O.AutoInc, O.PrimaryKey)
    def identifier = column[String]("identifier", O.Unique)
    def name = column[String]("name")
    def email = column[String]("email", O.Unique)
    def password = column[String]("password")
    def identity = column[Short]("identity")
    def * = (userID, identifier, name, email, password, identity) <> (User.tupled, User.unapply)
  }
  lazy val users = TableQuery[Users]

  case class Booking(bookingID:Int, status:Short, userID:Int, slotID:Int)
  class Bookings(tag: Tag) extends Table[Booking](tag, "BOOKINGS") {
    def bookingID = column[Int]("booking_id", O.AutoInc, O.PrimaryKey)
    def status = column[Short]("status")
    def userID = column[Int]("user_id")
    def slotID = column[Int]("slot_id")
    def * = (bookingID, status, userID, slotID) <> (Booking.tupled, Booking.unapply)

    def user = foreignKey(
      "user_fk", userID, users)(
      _.userID, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    def slot = foreignKey(
      "slot_fk", slotID, slots)(
      _.slotID, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  lazy val bookings = TableQuery[Bookings]
}
