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

package pravda.node.client

import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}
import pravda.node.servers.Abci.TransactionResult

import scala.language.higherKinds

trait NodeLanguage[F[_]] {

  def launch(configPath: String): F[Unit]

  def singAndBroadcastTransaction(uriPrefix: String,
                                  address: ByteString,
                                  privateKey: ByteString,
                                  wattPayerPrivateKey: Option[ByteString],
                                  wattLimit: Long,
                                  wattPrice: NativeCoin,
                                  wattPayer: Option[Address],
                                  data: ByteString): F[Either[String, TransactionResult]]

  def broadcastMethodCall(uriPrefix: String,
                          walletAddress: ByteString,
                          walletPrivateKey: ByteString,
                          wattPayerPrivateKey: Option[ByteString],
                          wattLimit: Long,
                          wattPrice: NativeCoin,
                          wattPayer: Option[Address],
                          programAddress: String,
                          programMethod: String,
                          programArgs: Seq[String]): F[Either[String, TransactionResult]]
  def execute(data: ByteString, address: Address, endpoint: String): F[Either[String, TransactionResult]]
}
