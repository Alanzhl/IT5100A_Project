package controllers

import models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.Json
import play.api.mvc._
import services._
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

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
  def index: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future.successful(Ok(Json.toJson(
      Map("uid" -> getUserSession(request))
    )))
  }

  def login: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val username = form.getOrElse("username", Seq("")).head
    val password = form.getOrElse("password", Seq("")).head
    service.validateUserLogin(username, password).map[Result] {
      case Some(uid) =>
        Ok(Json.obj(
          "success" -> 0
        )).withSession(("uid" -> uid.toString))
      case _ =>
        Unauthorized(Json.obj(
          "success" -> 1,
          "message" -> "Username or password error")
        )
    }
  }

  def logout: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
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
    Await.result[Future[Result]](service.register(email, password, name, identifier).map[Future[Result]] {
      case Right(e: String) =>
        Future.successful(BadRequest(Json.obj("success" -> 1, "message" -> e)))
      case Left(r: Future[Int]) => r.map[Result] {
        case 1 => Ok(Json.obj("success" -> 0))
        case _ => BadRequest(Json.obj("success" -> 1))
      }
    }, 10.seconds)
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
    Future.successful(Ok(Json.toJson(
      Map("success" -> 0)
    )))
  }

  // cancelTimeslot: user cancel a booking
  def cancelTimeslot: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future.successful(Ok(Json.toJson(
      Map("success" -> 0)
    )))
  }

  //queryUserBookings: get user's current bookings
  def queryUserBookings: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future.successful(Ok(Json.toJson(
      Map("success" -> 0)
    )))
  }

  // editUserBooking: edit time slot for a booking
  def editUserBooking: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future.successful(Ok(Json.toJson(
      Map("success" -> 0)
    )))
  }

  // queryBookingRecords: staffs query bookings in a time slot
  def queryBookingRecords: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future.successful(Ok(Json.toJson(
      Map("success" -> 0)
    )))
  }

  // addTimeslot: staffs add a timeslot
  def addTimeslot(): Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    val form = extractForm(request)
    val startAt = form.getOrElse("startAt", Seq())
    val endAt = form.getOrElse("endAt", Seq())
    val vacancy = form.getOrElse("vacancy", Seq("50"))
    if (startAt.isEmpty || endAt.isEmpty) {
      Future.successful(Unauthorized(Json.obj(
        "success" -> 1,
        "message" -> "missing compulsory field: startAt or endAt (YYYY-MM-DD hh:mm:ss)."
      )))
    } else {
      val result = model.createTimeslot(
        Timestamp.valueOf(startAt.head),
        Timestamp.valueOf(endAt.head),
        vacancy.head.toInt)
      Await.result(result.map[Future[Result]] {
        case Some(sidFuture) => sidFuture.map{sid =>
          Ok(Json.obj(
            "success" -> 0,
            "slot_id" -> sid))}
        case _ => Future.successful(
          Unauthorized(Json.obj(
            "success" -> 1,
            "message" -> "Time slot creation fails."
          ))
        )
      }, 10.seconds)
    }
  }

  // deleteTimeslot: staffs delete a timeslot
  def deleteTimeslot(): Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future.successful(Ok(Json.toJson(
      Map("success" -> 0)
    )))
  }

  // editTimeslot: staffs edit a timeslot
  def editTimeslot(): Action[AnyContent] = Action.async { request =>
    val form = extractForm(request)
    val slotID = form.getOrElse("slotID", Seq())
    val status = form.getOrElse("status", Seq())
    val vacancy = form.getOrElse("vacancy", Seq())
    if (slotID.isEmpty) {
      Future.successful(Unauthorized(Json.obj(
        "success" -> 1,
        "message" -> "Missing compulsory field: slotID."
      )))
    } else if (status.isEmpty && vacancy.isEmpty) {
      Future.successful(Unauthorized(Json.obj(
        "success" -> 1,
        "message" -> "please at least specify the 'status' or 'vacancy' to be edited."
      )))
    } else {
      val result = {
        if (status.nonEmpty) model.updateATimeslot(slotID.head.toInt, status.head.toShort)
        else model.updateVacancy(slotID.head.toInt, vacancy.head.toInt)
      }
      result.map{
        case 0 => Ok(Json.obj(
          "success" -> 0,
          "message" -> s"Fail to find slot with id ${slotID.head.toInt}"))
        case _ => Ok(Json.obj(
          "success" -> 0,
          "message" -> s"Updated slot ${slotID.head.toInt}."))
      }
    }
  }

  // markBookingStatus: staffs mark for booking records status
  def markBookingStatus: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future.successful(Ok(Json.toJson(
      Map("success" -> 0)
    )))
  }

  // getStatistics: staffs get statistics of bookings in a week
  def getStatistics: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future.successful(Ok(Json.toJson(
      Map("success" -> 0)
    )))
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
