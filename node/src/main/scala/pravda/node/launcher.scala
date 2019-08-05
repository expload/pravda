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

import scala.util.{Failure, Success}

object launcher extends App {

  import Config._

  sys.env.get("PRAVDA_CONFIG_FILE") foreach { path =>
    sys.props.put("config.file", path)
  }

  private lazy val NETWORKD_ADDRESS_CACHE_TTL = 60
  private lazy val NETWORKD_ADDRESS_CACHE_NEGATIVE_TTL = 20

  private implicit val system: ActorSystem = ActorSystem("pravda-system")
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  java.security.Security.setProperty(
    "networkaddress.cache.ttl",
    pravdaConfig.networkAddressCache.map(_.ttl).getOrElse(NETWORKD_ADDRESS_CACHE_TTL).toString
  )
  java.security.Security.setProperty(
    "networkaddress.cache.negative.ttl",
    pravdaConfig.networkAddressCache.map(_.negativeTtl).getOrElse(NETWORKD_ADDRESS_CACHE_NEGATIVE_TTL).toString
  )

  val abciClient = new AbciClient(pravdaConfig.tendermint.rpcPort)

  val applicationStateDb = DB(
    path = new File(Config.pravdaConfig.dataDirectory, "application-state").getAbsolutePath,
    initialHash = FileStore.readApplicationStateInfo().map(_.appHash.toByteArray)
  )

  val effectsDb = DB(
    path = new File(Config.pravdaConfig.dataDirectory, "effects").getAbsolutePath,
    initialHash = None
  )

  val abci =
    new Abci(applicationStateDb, effectsDb, abciClient, pravdaConfig.coinDistribution, pravdaConfig.validatorManager)

  val abciServer = Server(
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

  val apiRoute = new ApiRoute(abciClient, applicationStateDb, effectsDb, abci)
  val guiRoute = new GuiRoute(abciClient, effectsDb)

  val res = for {
    h <- HttpServer.start(pravdaConfig.http, apiRoute.route, guiRoute.route)
    a <- abciServer.start()
    t <- tendermint.run(pravdaConfig)
  } yield (h, a, t)

  println("Tendermint node started")

  // Handle tendermint shutdown
  res.map(_._3.waitFor()).onComplete {
    case Failure(t) =>
      println(
        s"${Console.RED}ERROR${Console.RESET}: Something wrong happened while running Tendermint: ${t.getLocalizedMessage}")
    case Success(code) =>
      if (code != 0) {
        println(s"${Console.RED}ERROR${Console.RESET}: Tendermint unexpected shutdown with exit code $code")
        sys.exit(code)
      }
  }

  sys.addShutdownHook {
    val (httpServer, abciServer, tendermintNode) = Await.result(res, 10.seconds)

    print("Shutting down tendermint node...")
    tendermintNode.destroy()
    println(s"${Console.GREEN} done${Console.RESET}")

    print("Shutting down API server...")
    Await.result(httpServer.unbind(), 10.seconds)
    Await.result(abciServer.unbind(), 10.seconds)
    system.terminate()
    println(s"${Console.GREEN} done${Console.RESET}")

    print("Closing application state db...")
    applicationStateDb.close()
    println(s"${Console.GREEN} done${Console.RESET}")
  }

}
