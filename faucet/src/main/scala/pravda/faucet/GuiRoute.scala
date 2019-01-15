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
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import korolev.Context
import korolev.akkahttp._
import korolev.execution._
import korolev.server.{KorolevServiceConfig, ServerRouter}
import korolev.state.StateStorage
import korolev.state.javaSerialization._
import pravda.common.bytes
import pravda.common.domain.NativeCoin
import pravda.node.client.impl.{CompilersLanguageImpl, NodeLanguageImpl}

import scala.concurrent.Future

class GuiRoute(implicit system: ActorSystem, materializer: ActorMaterializer) {

  import GuiRoute._
  import globalContext._
  import symbolDsl._

  private val addressField = elementId()
  private val xcoinsField = elementId()

  private val node = new NodeLanguageImpl()
  private val compilers = new CompilersLanguageImpl()

  private val service = akkaHttpService(
    KorolevServiceConfig[Future, GuiState, Any](
      serverRouter = ServerRouter
        .empty[Future, GuiState]
        .withRootPath("/ui/"),
      stateStorage = StateStorage.default(AccrueCoins(None)),
      head = Seq(
        'link ('href /= "https://cdnjs.cloudflare.com/ajax/libs/bulma/0.7.1/css/bulma.min.css", 'rel /= "stylesheet"),
        'link (
          'href        /= "https://use.fontawesome.com/releases/v5.0.13/css/all.css",
          'rel         /= "stylesheet",
          'crossorigin /= "anonymous",
          'integrity   /= "sha384-DNOHZ68U8hZfKXOrtjWvjxusGo9WQnrNx2sqG0tfsghAvtVlRW3tvkXWZh58N9jp"
        )
      ),
      render = {
        case AccrueCoins(msg) =>
          'body (
            'div (
              'class /= "container",
              'form (
                'display       @= "flex",
                'flexDirection @= "column",
                'input ('class  /= "input", 'margin @= 10, addressField, 'placeholder /= "Address", 'value /= ""),
                'input ('class  /= "input",
                        'margin @= 10,
                        xcoinsField,
                        'placeholder /= "Number of Native Coins",
                        'value       /= "1000000"),
                'div (
                  'button (
                    'class  /= "button is-link",
                    'margin @= 10,
                    'width  @= 200,
                    "Accrue",
                    event('click) {
                      access =>
                        for {
                          address <- access.valueOf(addressField)
                          coins <- access.valueOf(xcoinsField)
                          codeOrError <- compilers.asm(s"push x$address push bigint($coins) transfer")
                          resOrError <- codeOrError match {
                            case Left(err) => Future.successful(Left(s"Error during request construction: $err"))
                            case Right(code) =>
                              node
                                .singAndBroadcastTransaction(
                                  Config.faucetConfig.testnetEndpoint,
                                  bytes.hex2byteString(Config.faucetConfig.walletAddress),
                                  bytes.hex2byteString(Config.faucetConfig.walletPrivateKey),
                                  None,
                                  10000L,
                                  NativeCoin @@ 1L,
                                  None,
                                  code
                                )
                                .map(_.left.map(e => s"Error during http request: $e"))
                          }
                          _ <- resOrError match {
                            case Left(err) => access.transition(_ => ErrorScreen(err))
                            case Right(res) =>
                              res.executionResult match {
                                case Left(runtimeError) =>
                                  access.transition(_ =>
                                    ErrorScreen(s"Runtime error during transferring: ${runtimeError.error}"))
                                case Right(finalState) =>
                                  access.transition(_ =>
                                    AccrueCoins(Some(s"Successfully transferred $coins to $address")))
                              }
                          }
                        } yield {}
                    }
                  )
                )
              ),
              msg match {
                case Some(m) => 'div ('class /= "notification is-success", m)
                case None    => void
              }
            )
          )
        case ErrorScreen(error) =>
          'body (
            'div ('class /= "container", 'div ('class /= "notification is-danger", error))
          )
      }
    )
  )

  val route: Route = service(AkkaHttpServerConfig())
}

object GuiRoute {

  sealed trait GuiState

  final case class AccrueCoins(successMessage: Option[String]) extends GuiState
  final case class ErrorScreen(error: String)                  extends GuiState

  val globalContext: Context[Future, GuiState, Any] =
    Context[Future, GuiState, Any]
}
