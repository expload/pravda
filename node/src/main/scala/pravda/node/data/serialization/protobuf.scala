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

import cats.syntax.contravariant._
import cats.syntax.functor._
import cats.syntax.invariant._
import com.google.protobuf.ByteString
import pravda.common.domain.NativeCoin
import pravda.node.data.blockchain.SignatureData
import pravda.node.data.blockchain.Transaction.SignedTransaction
import pravda.node.data.common.TransactionId
import pravda.node.data.cryptography.EncryptedPrivateKey
import pravda.node.data.domain.Wallet
import pravda.node.servers.Abci
import pravda.node.servers.Abci.StoredProgram
import pravda.vm.{Data, Effect, MarshalledData}
import supertagged.{Tagged, lifterF}
import pbdirect._

import scala.collection.mutable

object protobuf extends ProtobufTranscoder with PBDirectInstances {

  implicit def tuple2PbReader[T1: PBReader, T2: PBReader]: PBReader[(T1, T2)] = PBDirectInstancesInferred.tuple2PbReader

  implicit def tuple2PbWriter[T1: PBWriter, T2: PBWriter]: PBWriter[(T1, T2)] = PBDirectInstancesInferred.tuple2PbWriter

  implicit val signatureDataFormat: PBFormat[SignatureData] = PBDirectInstancesInferred.signatureDataFormat

  implicit val dataFormat: PBFormat[Data] = PBDirectInstancesInferred.dataFormat

  implicit val marshaledDataFormat: PBFormat[MarshalledData] = PBDirectInstancesInferred.marshaledDataFormat

  implicit val programEventsFormat: PBFormat[Abci.TransactionEffects.ProgramEvents] =
    PBDirectInstancesInferred.programEventsFormat

  implicit val transactionAllEffectsFormat: PBFormat[Abci.TransactionEffects.AllEffects] =
    PBDirectInstancesInferred.transactionAllEffectsFormat

  implicit val transferEffectsFormat: PBFormat[Abci.TransactionEffects.Transfers] =
    PBDirectInstancesInferred.transferEffectsFormat

  implicit val signedTransactionFormat: PBFormat[SignedTransaction] = PBDirectInstancesInferred.signedTransactionFormat

  implicit val storedProgramFormat: PBFormat[StoredProgram] = PBDirectInstancesInferred.storedProgramFormat

  implicit val epkFormat: PBFormat[EncryptedPrivateKey] = PBDirectInstancesInferred.epkFormat

  implicit val walletFormat: PBFormat[Wallet] = PBDirectInstancesInferred.walletFormat

  implicit val mtiseFormat: PBFormat[Map[TransactionId, Seq[Effect]]] = PBDirectInstancesInferred.mtiseFormat
}

trait ProtobufTranscoder {

  type ProtobufEncoder[T] = Transcoder[T, Protobuf]
  type ProtobufDecoder[T] = Transcoder[Protobuf, T]

  implicit def protobufEncoder[T: PBWriter]: ProtobufEncoder[T] =
    t => Protobuf @@ t.toPB

  implicit def protobufDecoder[T: PBReader]: ProtobufDecoder[T] =
    t => t.pbTo[T]
}

private object PBDirectInstancesInferred {

  import PBDirectInstances._
  import cats.instances.option._

  def tuple2PbReader[T1: PBReader, T2: PBReader]: PBReader[(T1, T2)] = implicitly[PBReader[(T1, T2)]]

  def tuple2PbWriter[T1: PBWriter, T2: PBWriter]: PBWriter[(T1, T2)] = implicitly[PBWriter[(T1, T2)]]

  val signatureDataFormat = PBFormat[SignatureData]

  val dataFormat = PBFormat[Data]

  val marshaledDataFormat = PBFormat[MarshalledData]

  val programEventsFormat = {
    import PBDirectInstances._

    implicit val mData = marshaledDataFormat

    PBFormat[Abci.TransactionEffects.ProgramEvents]
  }

  val transactionAllEffectsFormat = {
    import PBDirectInstances._

    implicit val data = dataFormat
    implicit val mData = marshaledDataFormat

    PBFormat[Abci.TransactionEffects.AllEffects]
  }

  val transferEffectsFormat = {
    import PBDirectInstances._

    PBFormat[Abci.TransactionEffects.Transfers]
  }

  val signedTransactionFormat = {
    import PBDirectInstances._

    PBFormat[SignedTransaction]
  }

  val storedProgramFormat = PBFormat[StoredProgram]

  val epkFormat = PBFormat[EncryptedPrivateKey]

  val walletFormat = PBFormat[Wallet]

  val mtiseFormat = {
    import PBDirectInstances._

    implicit val data = dataFormat
    implicit val mData = marshaledDataFormat

    PBFormat[List[(TransactionId, Seq[Effect])]].imap(_.toMap)(_.toList)
  }
}

object PBDirectInstances extends PBDirectInstances

trait PBDirectInstances extends PBDirectLowPriorityInstances {

  implicit val nativeCoinFormat: PBFormat[NativeCoin] = PBFormat[Tuple1[Long]].imap(t => NativeCoin @@ t._1)(Tuple1(_))

  implicit val bytestringFormat: PBFormat[ByteString] = PBFormat[Array[Byte]].imap(ByteString.copyFrom)(_.toByteArray)

  implicit val byteFormat: PBFormat[Byte] = PBFormat[Int].imap(_.toByte)(_.toInt)
  implicit val shortFormat: PBFormat[Short] = PBFormat[Int].imap(_.toShort)(_.toInt)

  implicit val bigIntFormat: PBFormat[scala.BigInt] = PBFormat[Array[Byte]].imap(scala.BigInt(_))(_.toByteArray)

  implicit val dataStructFormat: PBFormat[Data.Struct] = {
    import cats.instances.list._
    PBFormat[List[(Data.Primitive, Data.Primitive)]].imap(m => Data.Struct(mutable.Map(m: _*)))(_.data.toList)
  }
}

trait PBDirectLowPriorityInstances {

  implicit def pbWriterLifter[T: PBWriter, U]: PBWriter[Tagged[T, U]] =
    lifterF[PBWriter].lift[T, U]

  implicit def pbReaderLifter[T: PBReader, U]: PBReader[Tagged[T, U]] =
    lifterF[PBReader].lift[T, U]

  implicit def seqReader[T: PBReader]: PBReader[Seq[T]] =
    PBReader[List[T]].map(l => l)

  implicit def seqWriter[T: PBWriter]: PBWriter[Seq[T]] = {
    import cats.instances.list._
    PBWriter[List[T]].contramap(l => l.toList)
  }

  implicit def bufferReader[T: PBReader]: PBReader[mutable.Buffer[T]] =
    PBReader[List[T]].map(l => l.toBuffer)

  implicit def bufferWriter[T: PBWriter]: PBWriter[mutable.Buffer[T]] = {
    import cats.instances.list._
    PBWriter[List[T]].contramap(l => l.toList)
  }
}