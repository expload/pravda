package pravda.cli.languages

import cats.Id
import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}

class NodeLanguageStub(result: Either[String, String]) extends NodeLanguage[Id] {
  def launch(configPath: String): Id[Unit] = ()

  def singAndBroadcastTransaction(uriPrefix: String,
                                  address: ByteString,
                                  privateKey: ByteString,
                                  wattPayerPrivateKey: Option[ByteString],
                                  wattLimit: Long,
                                  wattPrice: NativeCoin,
                                  wattPayer: Option[Address],
                                  data: ByteString): Id[Either[String, String]] = result
}
