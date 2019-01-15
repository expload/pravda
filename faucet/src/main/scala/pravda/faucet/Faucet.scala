/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.faucet

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.Success

object Faucet extends App {

  private implicit val system: ActorSystem = ActorSystem("pravda-system")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val config = Config.faucetConfig

  val httpServer = {
    val healthz = pathPrefix("healthz")(complete("faucet-service: OK"))
    val gui = pathPrefix("ui")(new GuiRoute().route)

    Http().bindAndHandle(healthz ~ gui, config.host, config.port) andThen {
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
