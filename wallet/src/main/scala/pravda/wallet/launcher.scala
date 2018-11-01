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

package pravda.wallet

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import pravda.cli.languages.impl.NodeLanguageImpl
import pravda.common.domain.Address
import pravda.node.data.cryptography.PrivateKey

import scala.concurrent.ExecutionContextExecutor
import scala.util.Success

object launcher extends App {

  implicit val system: ActorSystem = ActorSystem("pravda-wallet")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  final case class HttpConfig(host: String, port: Int)
  val config = pureconfig.loadConfigOrThrow[HttpConfig]("http")

  val broadcastEnpoint = sys.env("PRAVDA_BROADCAST_ENDPOINT")
  val broadcastPk = Address.fromHex(sys.env("PRAVDA_BROADCAST_PK"))
  val broadcastSk = PrivateKey.fromHex(sys.env("PRAVDA_BROADCAST_SK"))

  lazy val nodeLanguage = new NodeLanguageImpl()
  lazy val apiRoute = new ApiRoute(nodeLanguage, broadcastEnpoint, broadcastPk, broadcastSk)

  Http().bindAndHandle(apiRoute.route, config.host, config.port) andThen {
    case Success(_) => println(s"API server started at ${config.host}:${config.port}")
  }
}
