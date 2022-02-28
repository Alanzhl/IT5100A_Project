package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json.Json

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents)
  extends BaseController {

  def index = Action { request: Request[AnyContent] =>
    Ok("Hello World")
  }

  def getJSON = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(
      Map("message" -> "Hello")
    ))
  }
}
