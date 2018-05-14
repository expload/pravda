package io.mytc.timechain.servers

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import korolev.Context
import korolev.akkahttp._
import korolev.execution._
import korolev.server.{KorolevServiceConfig, ServerRouter}
import korolev.state.StateStorage
import korolev.state.javaSerialization._

import scala.concurrent.Future


class GuiRoute(implicit system: ActorSystem,
               materializer: ActorMaterializer,
              ) {

  import GuiRoute._
  import globalContext._
  import symbolDsl._

//  private def panel(caption: String, content: Node) = {
//    'div(
//      'border       @= "1px solid black",
//      'marginBottom @= 10,
//      'borderRadius @= 3,
//      'padding      @= 10,
//      'div(
//        'fontWeight @= "600",
//        'fontSize   @= 18,
//        caption
//      ),
//      content
//    )
//  }
//
//  private def verticalTable(xs: (String, Node)*) = {
//    'table(
//      'marginTop    @= 10,
//      'marginBottom @= 10,
//      'cellspacing  /= "5",
//      'tbody(
//        xs map {
//          case (k, v) =>
//            'tr('td('fontStyle @= "italic", k), 'td(v))
//        }
//      )
//    )
//  }

  private val service = akkaHttpService(
    KorolevServiceConfig[Future, GuiState, Any](
      serverRouter = ServerRouter
        .empty[Future, GuiState]
        .withRootPath("/ui/"),
      stateStorage = StateStorage.default(GuiState()),
      render = {
        case state => 'body ("This is Pravda!")
      }
    )
  )

  val route: Route = service(AkkaHttpServerConfig())
}

object GuiRoute {

  case class GuiState(
 )

  val globalContext: Context[Future, GuiState, Any] =
    Context[Future, GuiState, Any]
}
