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

  def start(apiConfig: TimechainConfig.ApiConfig, guiConfig: TimechainConfig.UiConfig, apiRoute: Route, guiRoute: Route)(
      implicit system: ActorSystem,
      materializer: ActorMaterializer,
      executionContext: ExecutionContextExecutor): Future[Http.ServerBinding] = {
    val apiR = path("healthz") { complete{ "I'm ok :)" } } ~ pathPrefix("api")(apiRoute)
    val guiR = path("healthz") { complete{ "I'm ok :)" } } ~ pathPrefix("ui")(guiRoute)
    Http().bindAndHandle(apiR, apiConfig.host, apiConfig.port) andThen {
      case Success(_) =>
        println(s"API server started at ${apiConfig.host}:${apiConfig.port}")
        Http().bindAndHandle(guiR, guiConfig.host, guiConfig.port) andThen {
          case Success(_) => println(s"API server started at ${guiConfig.host}:${guiConfig.port}")
        }
    }
  }

}
