package pravda.node.client.impl

import java.io.IOException

import com.google.protobuf.ByteString
import io.ipfs.api.{IPFS, NamedStreamable}
import io.ipfs.multihash.Multihash
import pravda.node.client.IpfsLanguage

import scala.concurrent.{ExecutionContext, Future}

final class IpfsLanguageImpl(implicit executionContext: ExecutionContext) extends IpfsLanguage[Future] {

  def loadFromIpfs(ipfsHost: String, base58: String): Future[Option[ByteString]] = {
    val ipfs = new IPFS(ipfsHost)
    val hash = Multihash.fromBase58(base58)
    Future { Option(ByteString.copyFrom(ipfs.cat(hash))) }.recover {
      case e: RuntimeException => None
      case e: IOException => None
    }
  }

  def writeToIpfs(ipfsHost: String, bytes: ByteString): Future[Option[String]] = {
    val ipfs = new IPFS(ipfsHost)
    val file = new NamedStreamable.ByteArrayWrapper(bytes.toByteArray)
    Future { Option(ipfs.add(file).get(0).hash.toBase58) }.recover {
      case e: RuntimeException => None
      case e: IOException => None
    }
  }
}
