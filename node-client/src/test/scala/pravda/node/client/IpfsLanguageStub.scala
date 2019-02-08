package pravda.node.client

import cats._
import com.google.protobuf.ByteString

import scala.collection.mutable

import scala.language.higherKinds

class IpfsLanguageStub[F[_]: Monad](initialFiles: Map[String, ByteString] = Map.empty) extends IpfsLanguage[F] {
  val files = mutable.Map[String, ByteString](initialFiles.toSeq: _*)
  private var counter = 0

  override def loadFromIpfs(ipfsHost: String, base58: String): F[Option[ByteString]] = Monad[F].pure(files.get(base58))

  override def writeToIpfs(ipfsHost: String, bytes: ByteString): F[Option[String]] = Monad[F].pure {
    val hash = s"file$counter"
    counter += 1
    files.update(hash, bytes)
    Some(hash)
  }
}
