package pravda.node.client

import com.google.protobuf.ByteString
import pravda.vm.Meta

import scala.language.higherKinds

trait IpfsLanguage[F[_]] {
  def loadFromIpfs(ipfsHost: String, base58: String): F[ByteString]

  def writeToIpfs(ipfsHost: String, bytes: ByteString): F[String]
}
