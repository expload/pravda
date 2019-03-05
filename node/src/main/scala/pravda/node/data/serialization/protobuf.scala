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

/*
import cats.instances.option._
import cats.instances.list._
import cats.syntax.invariant._
import cats.syntax.contravariant._
import cats.syntax.functor._
import pbdirect._

class A {}

implicit val af: PBFormat[A] = PBFormat[Int].imap(i => new A)(_ => 1)

case class B(a: A, i: Int)

PBFormat[B]

 */

package pravda.node.data.serialization

import cats.syntax.contravariant._
import cats.syntax.functor._
import cats.syntax.invariant._
import com.google.protobuf.ByteString
import pbdirect._
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
  //

  def tuple2PbReader[T1: PBReader, T2: PBReader]: PBReader[(T1, T2)] = implicitly[PBReader[(T1, T2)]]

  def tuple2PbWriter[T1: PBWriter, T2: PBWriter]: PBWriter[(T1, T2)] = implicitly[PBWriter[(T1, T2)]]

  val signatureDataFormat = PBFormat[SignatureData]

  val dataFormat = {
    PBFormat[mutable.Buffer[Byte]]
    ??? // PBFormat[Data]
  }

  val marshaledDataFormat = ??? // PBFormat[MarshalledData]

  val programEventsFormat = ??? /// PBFormat[Abci.TransactionEffects.ProgramEvents]

  val transactionAllEffectsFormat = ??? // PBFormat[Abci.TransactionEffects.AllEffects]

  val transferEffectsFormat = PBFormat[Abci.TransactionEffects.Transfers]

  val signedTransactionFormat = PBFormat[SignedTransaction]

  val storedProgramFormat = PBFormat[StoredProgram]

  val epkFormat = PBFormat[EncryptedPrivateKey]

  val walletFormat = PBFormat[Wallet]

  val mtiseFormat = ??? // PBFormat[Map[TransactionId, Seq[Effect]]]
}

object PBDirectInstances extends PBDirectInstances

trait PBDirectInstances extends PBDirectLowPriorityInstances {
  implicit val nativeCoinFormat: PBFormat[NativeCoin] = PBFormat[Tuple1[Long]].imap(t => NativeCoin @@ t._1)(Tuple1(_))

  implicit val bytestringFormat: PBFormat[ByteString] = PBFormat[Array[Byte]].imap(ByteString.copyFrom)(_.toByteArray)
}

trait PBDirectLowPriorityInstances {
  implicit def pbWriterLifter[T: PBWriter, U]: PBWriter[Tagged[T, U]] =
    lifterF[PBWriter].lift[T, U]

  implicit def pbReaderLifter[T: PBReader, U]: PBReader[Tagged[T, U]] =
    lifterF[PBReader].lift[T, U]

  implicit def seqReader[T: PBReader]: PBReader[List[T]] =
    PBReader[List[T]].map(l => l)

  implicit def seqWriter[T: PBWriter]: PBWriter[Seq[T]] = {
    import cats.instances.list._
    PBWriter[List[T]].contramap(l => l.toList)
  }

}
