package pravda.node.client

import com.google.protobuf.ByteString

import scala.language.higherKinds

trait IpfsLanguage[F[_]] {
  def loadFromIpfs(ipfsHost: String, base58: String): F[Option[ByteString]]

  def writeToIpfs(ipfsHost: String, bytes: ByteString): F[Option[String]]
}
