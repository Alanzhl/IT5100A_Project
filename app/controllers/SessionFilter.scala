package controllers

import akka.stream.Materializer
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.Future


class SessionFilter @Inject()(implicit val mat: Materializer) extends Filter {
  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    // If not logged in or at login page
    if (requestHeader.session.get("uid").isEmpty
        && !(requestHeader.path.equals("/login")
        || requestHeader.path.equals("/test")    // for testing purpose
        || requestHeader.path.equals("/register"))) {
      Future.successful(Results.Unauthorized)
    } else {
      nextFilter(requestHeader)
    }
  }
}
