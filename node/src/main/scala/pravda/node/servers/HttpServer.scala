package pravda.node.servers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Success

import pravda.node.data.TimechainConfig

object HttpServer {

  def start(config: TimechainConfig.ApiConfig, apiRoute: Route, guiRoute: Route)(
      implicit system: ActorSystem,
      materializer: ActorMaterializer,
      executionContext: ExecutionContextExecutor): Future[Http.ServerBinding] = {
    val route = pathPrefix("api")(apiRoute) ~ pathPrefix("ui")(guiRoute)
    Http().bindAndHandle(route, config.host, config.port) andThen {
      case Success(_) => println(s"API server started at ${config.host}:${config.port}")
    }
  }

}
