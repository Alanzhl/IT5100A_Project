package controllers

import models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.Json
import play.api.mvc._
import services._
import slick.jdbc.JdbcProfile

import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

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
  def addTimeslot: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future.successful(Ok(Json.toJson(
      Map("success" -> 0)
    )))
  }

  // deleteTimeslot: staffs delete a timeslot
  def deleteTimeslot: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future.successful(Ok(Json.toJson(
      Map("success" -> 0)
    )))
  }

  // editTimeslot: staffs edit a timeslot
  def editTimeslot: Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future.successful(Ok(Json.toJson(
      Map("success" -> 0)
    )))
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
    val result = model.getUserInfo(1)
    result.map[Result] {
      case Some(user) => Ok(Json.obj(
        "success" -> 0,
        "user" -> user.toString
      )).withSession("uid" -> request.session.get("uid").get)
      case _ => Unauthorized(Json.obj(
        "success" -> 1
      ))
    }
  }
}
