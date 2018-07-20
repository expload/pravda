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

package pravda.node.servers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Success

import pravda.node.data.PravdaConfig

object HttpServer {

  def start(config: PravdaConfig.HttpConfig, apiRoute: Route, guiRoute: Route)(
      implicit system: ActorSystem,
      materializer: ActorMaterializer,
      executionContext: ExecutionContextExecutor): Future[Http.ServerBinding] = {
    val route = pathPrefix("healthz") {
      complete("pravda node: I'm OK :)")
    } ~
      pathPrefix("api")(apiRoute) ~
      pathPrefix("ui")(guiRoute)
    Http().bindAndHandle(route, config.host, config.port) andThen {
      case Success(_) => println(s"API server started at ${config.host}:${config.port}")
    }
  }

}
