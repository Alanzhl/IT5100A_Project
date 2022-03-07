package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json.Json

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents)
  extends BaseController {

  // index: return a list of available time slots
  def index: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("message" -> "Hello")
    ))
  }

  def login: Action[AnyContent] = Action { request: Request[AnyContent] =>
    val form = request.body.asFormUrlEncoded match {
      case Some(m: Map[String, Seq[String]]) => m
      case _ => Map.empty[String, Seq[String]]
    }
    val username = form.getOrElse("username", Seq("")).head
    val password = form.getOrElse("password", Seq("")).head
    // TODO: login logic
    Ok(Json.obj(
      "success" -> 0,
      "username" -> username
    )).withSession(("username" -> username))
  }

  def logout: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("success" -> 0)
    )).withNewSession
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
}
