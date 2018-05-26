package pravda.cli.languages.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import cats.Id
import com.google.protobuf.ByteString
import pravda.cli.languages.NodeApiLanguage
import pravda.common.bytes
import pravda.node.data.blockchain.Transaction.UnsignedTransaction
import pravda.node.data.blockchain.TransactionData
import pravda.node.data.common.{Address, Mytc}
import pravda.node.data.cryptography
import pravda.node.data.cryptography.PrivateKey

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random
import akka.util.{ByteString => AkkaByteString}
import scala.concurrent.ExecutionContextExecutor

final class NodeApiLanguageImpl extends NodeApiLanguage[Id] {

  private implicit val system: ActorSystem = ActorSystem()
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def singAndBroadcastTransaction(uriPrefix: String,
                                  address: ByteString,
                                  privateKey: ByteString,
                                  data: ByteString): Id[Either[String, String]] = {

    val tx = UnsignedTransaction(
      from = Address @@ address,
      program = TransactionData @@ data,
      Mytc.zero,
      nonce = Random.nextInt()
    )
    val stx = cryptography.signTransaction(PrivateKey @@ privateKey, tx)
    val fromHex = bytes.byteString2hex(address)
    val signatureHex = bytes.byteString2hex(stx.signature)

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"$uriPrefix?from=$fromHex&signature=$signatureHex&fee=0.00",
      entity = HttpEntity(data.toByteArray)
    )

    val future = Http()
      .singleRequest(request)
      .flatMap { response =>
        response.entity.dataBytes
          .runFold(AkkaByteString(""))(_ ++ _)
          .map(_.utf8String)
      }
    // FIXME fomkin: This why we should use Task/Future instead of Id
    Right(Await.result(future, 10.seconds))
  }
}
