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

package pravda.node

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import pravda.node.db.DB
import io.mytc.tendermint.abci.Server
import io.mytc.tendermint.abci.Server.ConnectionMethod
import pravda.node.clients.AbciClient
import pravda.node.servers.{Abci, ApiRoute, GuiRoute, HttpServer}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}
import pravda.node.persistence.FileStore

object launcher extends App {

  import Config._

  sys.env.get("PRAVDA_CONFIG_FILE") foreach { path =>
    sys.props.put("config.file", path)
  }

  private implicit val system: ActorSystem = ActorSystem("pravda-system")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val abciClient = new AbciClient(pravdaConfig.tendermint.rpcPort)

  val applicationStateDb = DB(
    path = new File(Config.pravdaConfig.dataDirectory, "application-state").getAbsolutePath,
    initialHash = FileStore.readApplicationStateInfo().map(_.appHash.toByteArray)
  )
  val abci = new Abci(applicationStateDb, abciClient, pravdaConfig.coinDistribution)

  val server = Server(
    cfg = Server.Config(
      connectionMethod = if (pravdaConfig.tendermint.useUnixDomainSocket) {
        val path = new File(pravdaConfig.dataDirectory, "abci.sock").getAbsolutePath
        ConnectionMethod.UnixSocket(path)
      } else {
        ConnectionMethod.Tcp(host = "127.0.0.1", port = pravdaConfig.tendermint.proxyAppPort)
      }
    ),
    api = abci
  )

  val httpServer = {
    val apiRoute = new ApiRoute(abciClient, applicationStateDb, abci)
    val guiRoute = new GuiRoute(abciClient, applicationStateDb)
    HttpServer.start(pravdaConfig.http, apiRoute.route, guiRoute.route)
  }

  val tendermintNode = Await.result(tendermint.run(pravdaConfig), 10.seconds)

  server.start()

  println("Tendermint node started")

  sys.addShutdownHook {
    print("Shutting down tendermint node...")
    tendermintNode.destroy()
    println(s"${Console.GREEN} done${Console.RESET}")

    print("Shutting down API server...")
    Await.result(httpServer.flatMap(_.unbind()), 10.second)
    system.terminate()
    println(s"${Console.GREEN} done${Console.RESET}")

    print("Closing application state db...")
    applicationStateDb.close()
  }

}
