package controllers

import akka.stream.Materializer
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.Future
import play.api.libs.json.Json


class SessionFilter @Inject()(implicit val mat: Materializer) extends Filter {
  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    // If not logged in or at login page
    if (requestHeader.session.get("username").isEmpty && !requestHeader.path.contains("/login")) {
      Future.successful(Results.Unauthorized)
    } else {
      nextFilter(requestHeader)
    }
  }
}
