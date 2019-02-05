package pravda.node.client.impl

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import io.ipfs.api.{IPFS, NamedStreamable}
import io.ipfs.multihash.Multihash
import pravda.node.client.IpfsLanguage
import pravda.vm.Meta

import scala.concurrent.{ExecutionContext, Future}

final class IpfsLanguageImpl(implicit executionContext: ExecutionContext) extends IpfsLanguage[Future] {

  def loadFromIpfs(ipfsHost: String, base58: String): Future[ByteString] = {
    val ipfs = new IPFS(ipfsHost)
    val hash = Multihash.fromBase58(base58)
    Future { ByteString.copyFrom(ipfs.cat(hash)) }
  }

  def writeToIpfs(ipfsHost: String, bytes: ByteString): Future[String] = {
    val ipfs = new IPFS(ipfsHost)
    val file = new NamedStreamable.ByteArrayWrapper(bytes.toByteArray)
    Future { ipfs.add(file).get(0).hash.toBase58 }
  }
}
