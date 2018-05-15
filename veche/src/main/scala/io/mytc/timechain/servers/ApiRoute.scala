package io.mytc.timechain

package servers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.mytc.timechain.clients.AbciClient

class ApiRoute(abciClient: AbciClient) {

//  import ApiRoute._
//  import io.mytc.timechain.utils.AkkaHttpSpecials._

  val route: Route =
    pathPrefix("public") {
      get {
        complete("OK")
      }
    }

}

object ApiRoute {}
