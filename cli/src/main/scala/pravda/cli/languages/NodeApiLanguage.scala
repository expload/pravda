package pravda.cli.languages

import com.google.protobuf.ByteString

import scala.language.higherKinds

trait NodeApiLanguage[F[_]] {

  def singAndBroadcastTransaction(uriPrefix: String,
                                  address: ByteString,
                                  privateKey: ByteString,
                                  data: ByteString): F[Either[String, String]]
}
