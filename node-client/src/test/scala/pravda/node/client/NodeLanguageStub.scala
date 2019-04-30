package pravda.node.client

import cats._
import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}
import pravda.node.servers.Abci.TransactionResult

import scala.collection.mutable
import scala.language.higherKinds

class NodeLanguageStub[F[_]: Monad](result: Either[String, TransactionResult]) extends NodeLanguage[F] {

  val broadcastedData: mutable.Buffer[ByteString] = mutable.Buffer[ByteString]()

  def launch(configPath: String): F[Unit] = Monad[F].pure(())

  def singAndBroadcastTransaction(uriPrefix: String,
                                  address: ByteString,
                                  privateKey: ByteString,
                                  wattPayerPrivateKey: Option[ByteString],
                                  wattLimit: Long,
                                  wattPrice: NativeCoin,
                                  wattPayer: Option[Address],
                                  data: ByteString): F[Either[String, TransactionResult]] = Monad[F].pure {
    broadcastedData += data

    result
  }

  def execute(data: ByteString, address: Address, endpoint: String): F[Either[String, TransactionResult]] =
    Monad[F].pure(result)
}
