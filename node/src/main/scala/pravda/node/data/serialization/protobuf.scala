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

package pravda.node.data.serialization

import com.google.protobuf.ByteString
import pbdirect._
import pravda.common.domain.{Address, NativeCoin}
import pravda.node.data.blockchain.Transaction.SignedTransaction
import pravda.node.data.blockchain.TransactionData
import pravda.node.data.common.TransactionId
import pravda.node.data.cryptography.EncryptedPrivateKey
import pravda.node.data.domain.Wallet
import pravda.node.servers.Abci
import pravda.node.servers.Abci.StoredProgram
import pravda.vm.{Data, Effect, MarshalledData}
import supertagged.{Tagged, lifterF}
import cats.syntax.invariant._

object protobuf extends ProtobufTranscoder

trait ProtobufTranscoder {

  type ProtobufEncoder[T] = Transcoder[T, Protobuf]
  type ProtobufDecoder[T] = Transcoder[Protobuf, T]

  (pravda.node.data.common.TransactionId, pravda.vm.MarshalledData)

  implicit def protobufEncoder[T: PBWriter]: ProtobufEncoder[T] =
    t => Protobuf @@ t.toPB

  implicit def protobufDecoder[T: PBReader]: ProtobufDecoder[T] =
    t => t.pbTo[T]

  implicit def pbWriterLifter[T: PBWriter, U]: PBWriter[Tagged[T, U]] =
    lifterF[PBWriter].lift[T, U]

  implicit def pbReaderLifter[T: PBReader, U]: PBReader[Tagged[T, U]] =
    lifterF[PBReader].lift[T, U]

  implicit def tuple2PbReader[T1: PBReader, T2: PBReader]: PBReader[(T1, T2)] =
    PBReader[Tuple2[T1, T2]]

  implicit def tuple2PbWriter[T1: PBWriter, T2: PBWriter]: PBWriter[(T1, T2)] =
    PBWriter[Tuple2[T1, T2]]

  implicit val bytestringFormat: PBFormat[ByteString] = PBFormat[Array[Byte]].imap(ByteString.copyFrom)(_.toByteArray)

  implicit val dataFormat: PBFormat[Data] = PBFormat[Data]

  implicit val marshaledDataFormat: PBFormat[MarshalledData] = PBFormat[MarshalledData]

  implicit val programEventsFormat: PBFormat[Abci.TransactionEffects.ProgramEvents] =
    PBFormat[Abci.TransactionEffects.ProgramEvents]

  implicit val transactionAllEffectsFormat: PBFormat[Abci.TransactionEffects.AllEffects] =
    PBFormat[Abci.TransactionEffects.AllEffects]

  implicit val transferEffectsFormat: PBFormat[Abci.TransactionEffects.Transfers] =
    PBFormat[Abci.TransactionEffects.Transfers]

  implicit val signedTransactionFormat: PBFormat[SignedTransaction] =
    PBFormat[SignedTransaction]

  implicit val storedProgramFormat: PBFormat[StoredProgram] =
    PBFormat[StoredProgram]

  implicit val forSignatureReaderFormat: PBFormat[(Address, TransactionData, Long, NativeCoin, Int, Option[Address])] =
    PBFormat[(Address, TransactionData, Long, NativeCoin, Int, Option[Address])]

  implicit val epkFormat: PBFormat[EncryptedPrivateKey] =
    PBFormat[EncryptedPrivateKey]

  implicit val walletFormat: PBFormat[Wallet] =
    PBFormat[Wallet]

  implicit val mtiseFormat: PBFormat[Map[TransactionId, Seq[Effect]]] = PBFormat[Map[TransactionId, Seq[Effect]]]
}
