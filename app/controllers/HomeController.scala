package controllers

import models.Tables._
import models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.Json
import play.api.mvc._
import services._
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class HomeController @Inject()(protected val dbConfigProvider: DatabaseConfigProvider,
                               val controllerComponents: ControllerComponents)
                              (implicit ec: ExecutionContext)
  extends BaseController
    with HasDatabaseConfigProvider[JdbcProfile] {

  private val model = new DBOperations(db)
  private val service = new services(model)

  private def extractForm(request: Request[AnyContent]): Map[String, Seq[String]] =
    request.body.asFormUrlEncoded match {
      case Some(m: Map[String, Seq[String]]) => m
      case _ => Map.empty[String, Seq[String]]
    }

  private def getUserSession(request: Request[AnyContent]): Int = {
    request.session.get("uid").get.toInt
  }


  // index: return a list of available time slots
  def index: Action[AnyContent] = Action.async { _ =>
    service.getCurrentOpenSlots.map[Result] {
      case m: Seq[Tables.Slot] => Ok(Json.obj(
        "success" -> 0,
        "slots" -> m.toString()
      ))
      case _ => InternalServerError
    }
  }

  def login: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val username = form.getOrElse("username", Seq("")).head
    val password = form.getOrElse("password", Seq("")).head
    service.validateUserLogin(username, password).map[Result] {
      case Some(uid) =>
        Ok(Json.obj(
          "success" -> 0
        )).withSession("uid" -> uid.toString)
      case _ =>
        Unauthorized(Json.obj(
          "success" -> 1,
          "message" -> "Username or password error")
        )
    }
  }

  def logout: Action[AnyContent] = Action.async { _ =>
    Future.successful(Ok(Json.toJson(
      Map("success" -> 0)
    )).withNewSession)
  }

  def register: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val email = form.getOrElse("email", Seq("")).head
    val password = form.getOrElse("password", Seq("")).head
    val name = form.getOrElse("name", Seq("")).head
    val identifier = form.getOrElse("identifier", Seq("")).head
    service.register(email, password, name, identifier).map[Future[Result]] {
      case Right(e: String) =>
        Future.successful(BadRequest(Json.obj("success" -> 1, "message" -> e)))
      case Left(r: Future[Int]) => r.map[Result] {
        case 1 => Ok(Json.obj("success" -> 0))
        case _ => BadRequest(Json.obj("success" -> 1))
      }
    }.flatten
  }

  def getTimeslots: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val startAt = form.getOrElse("startAt", Seq())
    val endAt = form.getOrElse("endAt", Seq())
    if (startAt.isEmpty || endAt.isEmpty) {
      Future.successful(Unauthorized(Json.obj(
        "success" -> 1,
        "message" -> "missing compulsory field: startAt or endAt (YYYY-MM-DD hh:mm:ss)."
      )))
    } else {
      model.listTimeslots(Timestamp.valueOf(startAt.head), Timestamp.valueOf(endAt.head)).map{ r =>
        if (r.nonEmpty) {
          Ok(Json.obj(
            "success" -> 0,
            "slots" -> r.toString()))
        } else {
          Ok(Json.obj(
            "success" -> 0,
            "slots" -> "No available slots"))
        }
      }
    }
  }

  // bookTimeslot: user book a time slot
  def bookTimeslot: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val uid = getUserSession(request)
    val form = extractForm(request)
    val slotID = form.getOrElse("slotID", Seq("-1")).head.toInt
    (service.bookASlot(uid, slotID) map[Future[Result]] {
      case Right(e: String) =>
        Future.successful(BadRequest(Json.obj("success" -> 1, "message" -> e)))
      case Left(r: Future[Int]) => r.map[Result] {
        case 1 => Ok(Json.obj("success" -> 0))
        case _ => Ok(Json.obj("success" -> 1, "message" -> "Failed"))
      }
    }).flatten
  }

  // cancelTimeslot: user cancel a booking
  def cancelTimeslot: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val uid = getUserSession(request)
    val form = extractForm(request)
    val slotID = form.getOrElse("bookingID", Seq("-1")).head.toInt
    service.cancelBooking(uid, slotID) map[Result] {
      case Some(1) => Ok(Json.obj("success" -> 0))
      case Some(-1) =>
        BadRequest(Json.obj("success" -> 1, "message" -> "Invalid Request"))
      case _ => Ok(Json.obj("success" -> 1, "message" -> "Failed"))
    }
  }

  //queryUserBookings: get user's current bookings
  def queryUserBookings: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val uid = getUserSession(request)
    service.getUserBookings(uid) map[Result] {
      case m: Seq[Booking] => Ok(Json.obj("success" -> 0, "bookings" -> m.toString()))
      case _ => InternalServerError(Json.obj("success" -> 1, "message" -> "Failed"))
    }
  }

  // editUserBooking: edit time slot for a booking
  def editUserBooking: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val slotID = form.getOrElse("slotID", Seq("-1")).head.toInt
    val bookingID = form.getOrElse("bookingID", Seq("-1")).head.toInt
    val uid = getUserSession(request)
    (service.editUserBooking(uid, bookingID, slotID) map[Future[Result]] {
      case Right(e: String) =>
        Future.successful(BadRequest(Json.obj("success" -> 1, "message" -> e)))
      case Left(r: Future[Int]) => r.map[Result] {
        case 1 => Ok(Json.obj("success" -> 0))
        case _ => Ok(Json.obj("success" -> 1, "message" -> "Failed"))
      }
    }).flatten
  }

  // queryBookingRecords: staffs query bookings in a time slot
  def queryBookingRecords: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val slotID = form.getOrElse("slotID", Seq("-1")).head.toInt
    val uid = getUserSession(request)
    service.listBookingsByTimeslot(uid, slotID) map[Result] {
      case m: Seq[Booking] => Ok(Json.obj("success" -> 0, "bookings" -> m.toString()))
      case _ => InternalServerError(Json.obj("success" -> 1, "message" -> "Failed"))
    }
  }

  // createTimeslot: staffs add a timeslot
  def createTimeslot: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val uid = getUserSession(request)
    val starAt = Timestamp.valueOf(form.getOrElse("starAt", Seq("-1")).head)
    val endAt = Timestamp.valueOf(form.getOrElse("endAt", Seq("-1")).head)
    val vacancy = form.getOrElse("vacancy", Seq("-1")).head.toInt

    (service.createSlot(uid, starAt, endAt, vacancy) map[Future[Result]] {
      case Some(r: Future[Int]) => r map[Result] {
        case 1 => Ok(Json.obj("success" -> 0))
        case _ => Ok(Json.obj("success" -> 1, "message" -> "Failed"))
      }
      case _ =>
        Future.successful(
          InternalServerError(Json.obj("success" -> 1, "message" -> "Failed"))
        )
    }).flatten
  }

  // closeTimeslot: staffs delete a timeslot
  def closeTimeslot: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val uid = getUserSession(request)
    val slotID = form.getOrElse("slotID", Seq("-1")).head.toInt
    service.closeTimeslot(uid, slotID) map[Result] {
      case 0 => Ok(Json.toJson(
        Map("success" -> 0)
      ))
      case _ => InternalServerError(Json.obj("success" -> 1, "message" -> "Failed"))
    }
  }

  // editTimeslot: staffs edit a timeslot
  def editTimeslot: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val vacancy = form.getOrElse("vacancy", Seq("-1")).head.toInt
    val uid = getUserSession(request)
    val slotID = form.getOrElse("slotID", Seq("-1")).head.toInt
    service.editTimeslot(uid, slotID, vacancy) map[Result] {
      case -1 => InternalServerError(Json.obj("success" -> 1, "message" -> "Failed"))
      case _ => Ok(Json.toJson(
        Map("success" -> 0)
      ))
    }
  }

  // markBookingStatus: staffs mark for booking records status
  def markBookingStatus: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val status = form.getOrElse("status", Seq("-1")).head.toShort
    val uid = getUserSession(request)
    val bookingID = form.getOrElse("bookingID", Seq("-1")).head.toInt
    service.markBooking(uid, bookingID, status) map[Result] {
      case Some(r: Int) if r >= 0 => Ok(Json.toJson(
        Map("success" -> 0)
      ))
      case _ => InternalServerError(Json.obj("success" -> 1, "message" -> "Failed"))
    }
  }

  // getSlotsInPeriod: list all time slots in a specific time period for staffs
  def getSlotsInPeriod: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val endAt = Timestamp.valueOf(form.getOrElse("endAt", Seq("-1")).head)
    val starAt = Timestamp.valueOf(form.getOrElse("starAt", Seq("-1")).head)
    val uid = getUserSession(request)
    service.getSlotsInPeriod(uid, starAt, endAt) map[Result] {
      case m: Seq[Slot] => Ok(Json.obj("success" -> 0, "slots" -> m.toString()))
      case _ => InternalServerError(Json.obj("success" -> 1, "message" -> "Failed"))
    }
  }

  // initialize the schemas of tables
  def createTables: Action[AnyContent] = Action.async { _ =>
    model.init().map(info => Ok(info))
  }

  // (one-time insertion) insert sample data to the tables for testing purpose
  def insertSamples(): Action[AnyContent] = Action.async { _ =>
    model.insertSamples().map(info => Ok(info))
  }

  // current test: getUserInfo
  def test(): Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val startAt = form.getOrElse("startAt", Seq())
    val endAt = form.getOrElse("endAt", Seq())
    if (startAt.isEmpty || endAt.isEmpty) {
      Future.successful(Unauthorized(Json.obj(
        "success" -> 1,
        "message" -> "missing compulsory field: startAt or endAt (YYYY-MM-DD hh:mm:ss)."
      )))
    } else {
      model.listTimeslots(Timestamp.valueOf(startAt.head), Timestamp.valueOf(endAt.head)).map{ r =>
        if (r.nonEmpty) {
          Ok(Json.obj(
            "success" -> 0,
            "slots" -> r.toString()))
        } else {
          Ok(Json.obj(
            "success" -> 0,
            "slots" -> "No available slots"))
        }
      }
    }
  }
}
