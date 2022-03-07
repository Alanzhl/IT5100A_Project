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

  def index: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok("Hello World")
  }

  def getJSON: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("message" -> "Hello")
    ))
  }

  // initialize the schemas of tables
  def createTables: Action[AnyContent] = Action.async { _ =>
    model.init().map(_ => Ok("Tables initiated."))
  }

  // (one-time insertion) insert sample data to the tables for testing purpose
  def insertSamples(): Action[AnyContent] = Action.async { _ =>
    model.insertSamples().map(_ => Ok("Inserted sample with 4 slots, 3 users and 5 bookings."))
  }
}
