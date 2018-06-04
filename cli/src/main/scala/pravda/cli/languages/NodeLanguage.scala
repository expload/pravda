package pravda.cli.languages

import com.google.protobuf.ByteString
import pravda.common.domain.NativeCoin

import scala.language.higherKinds

trait NodeLanguage[F[_]] {

  def launch(configPath: String): F[Unit]

  def singAndBroadcastTransaction(uriPrefix: String,
                                  address: ByteString,
                                  privateKey: ByteString,
                                  wattLimit: Long,
                                  wattPrice: NativeCoin,
                                  data: ByteString): F[Either[String, String]]
}
