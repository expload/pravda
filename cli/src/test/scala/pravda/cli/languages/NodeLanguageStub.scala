package pravda.cli.languages

import cats.Id
import com.google.protobuf.ByteString
import pravda.common.domain.NativeCoin

class NodeLanguageStub(result: Either[String, String]) extends NodeLanguage[Id] {
  def launch(configPath: String): Id[Unit] = ()
  def singAndBroadcastTransaction(uriPrefix: String, address: ByteString, privateKey: ByteString, wattLimit: Long, wattPrice: NativeCoin, data: ByteString): Id[Either[String, String]] = result
}
