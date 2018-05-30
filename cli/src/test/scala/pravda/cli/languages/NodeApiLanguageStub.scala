package pravda.cli.languages

import cats.Id
import com.google.protobuf.ByteString

class NodeApiLanguageStub(result: Either[String, String]) extends NodeApiLanguage[Id] {
  def singAndBroadcastTransaction(uriPrefix: String, address: ByteString, privateKey: ByteString, data: ByteString): Id[Either[String, String]] = result
}
