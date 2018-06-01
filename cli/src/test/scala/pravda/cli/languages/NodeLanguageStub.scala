package pravda.cli.languages

import cats.Id
import com.google.protobuf.ByteString

class NodeLanguageStub(result: Either[String, String]) extends NodeLanguage[Id] {
  def launch(configPath: String): Id[Unit] = ()
  def singAndBroadcastTransaction(uriPrefix: String, address: ByteString, privateKey: ByteString, data: ByteString): Id[Either[String, String]] = result
}
