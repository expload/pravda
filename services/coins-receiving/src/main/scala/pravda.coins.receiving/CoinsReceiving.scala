package pravda.coins.receiving

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.stream.ActorMaterializer

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.Success

object CoinsReceiving extends App {

  private implicit val system: ActorSystem = ActorSystem("pravda-system")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val config = Config.coinsReceivingConfig

  val httpServer = {
    Http().bindAndHandle(pathPrefix("ui")(new GuiRoute().route), config.host, config.port) andThen {
      case Success(_) => println(s"API server started at ${config.host}:${config.port}")
    }
  }

  sys.addShutdownHook {
    val tcp = Await.result(httpServer, 10.seconds)
    print("Shutting down API server...")
    Await.result(tcp.unbind(), 10.seconds)
    system.terminate()
    println(s"${Console.GREEN} done${Console.RESET}")
  }

}
