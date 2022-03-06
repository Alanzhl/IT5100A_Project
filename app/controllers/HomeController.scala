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

  def createTables: Action[AnyContent] = Action.async { _ =>
    model.init(db).map(_ => Ok("Tables initiated."))
  }
}
