package pravda.cli.programs

import cats._
import cats.implicits._
import com.google.protobuf.ByteString
import pravda.cli.Config
import pravda.cli.languages.{OutputLanguage, RandomLanguage}
import pravda.common.{bytes, crypto}

import scala.language.higherKinds

class GenAddress[F[_]: Monad](output: OutputLanguage[F], random: RandomLanguage[F]) {

  import Config.Output._

  /**
    * Generates Pravda address and private key for it.
    * Writes to stdout or file depends on config.
    */
  def apply(config: Config.GenAddress): F[Unit] =
    for {
      randomBytes <- random.bytes64()
      (pub, sec) = crypto.ed25519KeyPair(randomBytes)
      json = s"""{"address":"${bytes.byteString2hex(pub)}","privateKey":"${bytes.byteString2hex(sec)}"}"""
      outputBytes = ByteString.copyFromUtf8(json)
      _ <- config.output match {
        case Stdout       => output.writeToStdout(outputBytes)
        case OsFile(path) => output.saveToFile(path, outputBytes)
      }
    } yield ()
}
