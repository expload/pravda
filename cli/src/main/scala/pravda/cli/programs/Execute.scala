/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.cli.programs

import cats._
import cats.data.EitherT
import cats.implicits._
import com.google.protobuf.ByteString
import pravda.cli.PravdaConfig
import pravda.cli.programs.Broadcast.Wallet
import pravda.node.client.{IoLanguage, NodeLanguage}
import pravda.common.serialization._
import pravda.node.data.serialization.json._

import scala.language.higherKinds

class Execute[F[_]: Monad](io: IoLanguage[F], api: NodeLanguage[F]) {

  val readFromFile = (path: String) =>
    io.readFromFile(path)
      .map(_.toRight(s"`$path` is not found."))

  def apply(config: PravdaConfig.Execute): F[Unit] = {
    val errorOrResult: EitherT[F, String, String] =
      for {
        walletJson <- EitherT(
          config.wallet
            .fold[F[Either[String, ByteString]]](Monad[F].pure(Left("Wallet file should be defined")))(readFromFile))
        program <- useOption(config.input)(io.readFromStdin(), readFromFile)
        wallet = transcode(Json @@ walletJson.toStringUtf8).to[Wallet]
        result <- EitherT {
          api.execute(program, wallet.address, config.endpoint)
        }
      } yield transcode(result).to[Json]
    errorOrResult.value.flatMap {
      case Left(error)   => io.writeStringToStderrAndExit(s"$error\n")
      case Right(result) => io.writeStringToStdout(s"$result\n")
    }
  }
}
