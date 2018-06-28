package pravda.cli.programs

import cats._
import cats.implicits._
import com.google.protobuf.ByteString
import pravda.cli.PravdaConfig
import pravda.cli.languages.{IoLanguage, RandomLanguage}
import pravda.common.{bytes, crypto}

import scala.language.higherKinds

class GenAddress[F[_]: Monad](io: IoLanguage[F], random: RandomLanguage[F]) {

  /**
    * Generates Pravda address and private key for it.
    * Writes to stdout or file depends on config.
    */
  def apply(config: PravdaConfig.GenAddress): F[Unit] =
    for {
      randomBytes <- random.secureBytes64()
      (pub, sec) = crypto.ed25519KeyPair(randomBytes)
      json = s"""{"address":"${bytes.byteString2hex(pub)}","privateKey":"${bytes.byteString2hex(sec)}"}"""
      outputBytes = ByteString.copyFromUtf8(json)
      _ <- config.output.fold(io.writeToStdout(outputBytes))(io.writeToFile(_, outputBytes))
    } yield ()
}
