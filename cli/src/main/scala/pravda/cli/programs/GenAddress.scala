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
import cats.implicits._
import com.google.protobuf.ByteString
import pravda.cli.PravdaConfig
import pravda.node.client.{IoLanguage, RandomLanguage}
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
      (pub, sec) = crypto.generateKeyPair(randomBytes)
      json = s"""{"address":"${bytes.byteString2hex(pub)}","privateKey":"${bytes.byteString2hex(sec)}"}"""
      outputBytes = ByteString.copyFromUtf8(json)
      _ <- config.output.fold(io.writeToStdout(outputBytes))(io.writeToFile(_, outputBytes))
    } yield ()
}
