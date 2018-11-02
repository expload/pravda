package pravda.coins.receiving

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import korolev.Context
import korolev.akkahttp._
import korolev.execution._
import korolev.server.{KorolevServiceConfig, ServerRouter}
import korolev.state.StateStorage
import korolev.state.javaSerialization._
import pravda.cli.languages.impl.{CompilersLanguageImpl, NodeLanguageImpl}
import pravda.common.bytes
import pravda.common.domain.NativeCoin

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
      stateStorage = StateStorage.default(ReceiveCoins(None)),
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
        case ReceiveCoins(msg) =>
          'body (
            'form (
              'display       @= "flex",
              'flexDirection @= "column",
              'input ('class  /= "input", 'margin @= 10, addressField, 'placeholder /= "Address", 'value /= ""),
              'input ('class  /= "input",
                      'margin @= 10,
                      xcoinsField,
                      'placeholder /= "Number of XCoins",
                      'value       /= "1000000"),
              'div (
                'button (
                  'class  /= "button is-link",
                  'margin @= 10,
                  'width  @= 200,
                  "Receive",
                  event('click) {
                    access =>
                      for {
                        address <- access.valueOf(addressField)
                        xcoins <- access.valueOf(xcoinsField)
                        codeOrError <- compilers.asm(s"push x$address push bigint($xcoins) transfer")
                        resOrError <- codeOrError match {
                          case Left(err) => Future.successful(Left(s"Error during request construction: $err"))
                          case Right(code) =>
                            node
                              .singAndBroadcastTransaction(
                                Config.coinsReceivingConfig.testnetEndpoint,
                                bytes.hex2byteString(Config.coinsReceivingConfig.walletAddress),
                                bytes.hex2byteString(Config.coinsReceivingConfig.walletPrivateKey),
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
                                  ReceiveCoins(Some(s"Successfully transferred $xcoins to $address")))
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
        case ErrorScreen(error) => 'div ('class /= "notification is-danger", error)
      }
    )
  )

  val route: Route = service(AkkaHttpServerConfig())
}

object GuiRoute {

  sealed trait GuiState

  final case class ReceiveCoins(successMessage: Option[String]) extends GuiState
  final case class ErrorScreen(error: String)                   extends GuiState

  val globalContext: Context[Future, GuiState, Any] =
    Context[Future, GuiState, Any]
}
