package pravda.coins.receiving

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}

object CoinsReceiving extends App {

  private implicit val system: ActorSystem = ActorSystem("pravda-system")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val httpServer = {

  }

  sys.addShutdownHook {
    print("Shutting down API server...")
    Await.result(httpServer.flatMap(_.unbind()), 10.second)
    system.terminate()
    println(s"${Console.GREEN} done${Console.RESET}")
  }

}
