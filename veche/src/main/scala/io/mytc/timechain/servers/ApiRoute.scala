package io.mytc.timechain
package servers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.mytc.timechain.clients.AbciClient
// import io.mytc.timechain.data.serialization.json._
import io.mytc.timechain.servers.Abci.ConsensusState
import io.mytc.timechain.utils.Var



class ApiRoute(
      consensusState: Var.VarReader[ConsensusState],
      abciClient: AbciClient) {

//  import ApiRoute._
//  import io.mytc.timechain.utils.AkkaHttpSpecials._

  val route: Route =
    pathPrefix("public") {
      get {
        complete("OK")
      }
    }

}

object ApiRoute {

}
