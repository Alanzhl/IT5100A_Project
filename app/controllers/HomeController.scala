package controllers

import javax.inject._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import play.api.libs.json.Json
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

import models._

@Singleton
class HomeController @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                               val controllerComponents: ControllerComponents)
                              (implicit ec: ExecutionContext)
  extends BaseController
  with HasDatabaseConfigProvider[JdbcProfile] {

  private val model = new DBOperations(db)

  // index: return a list of available time slots
  def index: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("message" -> "Hello")
    ))
  }

  def login: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }

  def logout: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }

  def register: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }

  // bookTimeslot: user book a time slot
  def bookTimeslot: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }

  // cancelTimeslot: user cancel a booking
  def cancelTimeslot: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }

  //queryUserBookings: get user's current bookings
  def queryUserBookings: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }

  // editUserBooking: edit time slot for a booking
  def editUserBooking: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }

  // queryBookingRecords: staffs query bookings in a time slot
  def queryBookingRecords: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }
  // addTimeslot: staffs add a timeslot
  def addTimeslot: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }
  // deleteTimeslot: staffs delete a timeslot
  def deleteTimeslot: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }
  // editTimeslot: staffs edit a timeslot
  def editTimeslot: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }
  // markBookingStatus: staffs mark for booking records status
  def markBookingStatus: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }
  // getStatistics: staffs get statistics of bookings in a week
  def getStatistics: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    ))
  }

  // initialize the schemas of tables
  def createTables: Action[AnyContent] = Action.async { _ =>
    model.init().map(info => Ok(info))
  }

  // (one-time insertion) insert sample data to the tables for testing purpose
  def insertSamples(): Action[AnyContent] = Action.async { _ =>
    model.insertSamples().map(info => Ok(info))
  }
}
