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

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.domain.{Address, NativeCoin}
import pravda.node.data.blockchain.{SignatureData, TransactionData}
import pravda.node.data.blockchain.Transaction.SignedTransaction
import pravda.node.data.common.TransactionId
import pravda.node.data.cryptography.EncryptedPrivateKey
import pravda.node.data.domain.Wallet
import pravda.node.servers.Abci.{AdditionalDataForAddress, StoredProgram, TransactionEffects}
import pravda.vm.Effect.Event
import pravda.vm.{Data, Effect, MarshalledData}
import supertagged.{Tagged, lifterF}
import zhukov._
import zhukov.derivation._
import zhukov.Default.auto._

object protobuf extends ProtobufTranscoder with ZhukovInstances

trait ProtobufTranscoder {

  type ProtobufEncoder[T] = Transcoder[T, Protobuf]
  type ProtobufDecoder[T] = Transcoder[Protobuf, T]

  implicit def protobufEncoder[T: Marshaller]: ProtobufEncoder[T] =
    t => Protobuf @@ Marshaller[T].write(t)

  implicit def protobufDecoder[T: Unmarshaller]: ProtobufDecoder[T] =
    t => Unmarshaller[T].read(Protobuf.untag(t))
}

trait ZhukovInstances extends ZhukovLowPriorityInstances {

  implicit object ByteStringBytes extends Bytes[ByteString] {
    def empty: ByteString = ByteString.EMPTY
    def copyFromArray(bytes: Array[Byte]): ByteString = ByteString.copyFrom(bytes)

    def copyFromArray(bytes: Array[Byte], offset: Long, size: Long): ByteString =
      ByteString.copyFrom(bytes, offset.toInt, size.toInt)

    def copyToArray(value: ByteString, array: Array[Byte], sourceOffset: Int, targetOffset: Int, length: Int): Unit =
      value.copyTo(array, sourceOffset, targetOffset, length)
    def wrapArray(bytes: Array[Byte]): ByteString = ByteString.copyFrom(bytes)
    def copyBuffer(buffer: ByteBuffer): ByteString = ByteString.copyFrom(buffer)
    def toArray(bytes: ByteString): Array[Byte] = bytes.toByteArray
    def toBuffer(bytes: ByteString): ByteBuffer = bytes.asReadOnlyByteBuffer()
    def get(bytes: ByteString, i: Long): Int = bytes.byteAt(i.toInt).toInt
    def size(bytes: ByteString): Long = bytes.size().toLong
    def concat(left: ByteString, right: ByteString): ByteString = left.concat(right)
    def slice(value: ByteString, start: Long, end: Long): ByteString = value.substring(start.toInt, end.toInt)
  }

  implicit val nativeCoinMarshaller = Marshaller.LongMarshaller.contramap[NativeCoin](n => n)
  implicit val nativeCoinUnmarshaller = Unmarshaller.long.map(NativeCoin @@ _)
  implicit val nativeCoinSizeMeter = SizeMeter.long.contramap[NativeCoin](n => n)

  implicit val addressMarshaller = Marshaller.bytesMarshaller[ByteString].contramap[Address](bs => bs)
  implicit val addressUnmarshaller = Unmarshaller.bytes[ByteString].map(Address @@ _)
  implicit val addressSizeMeter = SizeMeter.bytes[ByteString].contramap[Address](bs => bs)

  implicit val transctionIdMarshaller = Marshaller.bytesMarshaller[ByteString].contramap[TransactionId](bs => bs)
  implicit val transctionIdUnmarshaller = Unmarshaller.bytes[ByteString].map(TransactionId @@ _)
  implicit val transctionIdSizeMeter = SizeMeter.bytes[ByteString].contramap[TransactionId](bs => bs)

  implicit val transactionDataMarshaller = Marshaller.bytesMarshaller[ByteString].contramap[TransactionData](bs => bs)
  implicit val transactionDataUnmarshaller = Unmarshaller.bytes[ByteString].map(TransactionData @@ _)
  implicit val transactionDataSizeMeter = SizeMeter.bytes[ByteString].contramap[TransactionData](bs => bs)

  implicit def dataUnmarshaller: Unmarshaller[Data] = Unmarshaller.bytes[Array[Byte]].map(Data.fromBytes)
  implicit val dataMarshaller: Marshaller[Data] =
    Marshaller.bytesMarshaller[Array[Byte]].contramap[Data](_.toByteString.toByteArray)
  implicit val dataDefault: Default[Data] = Default(Data.Primitive.Null)
  implicit def dataSizeMeter[T <: Data]: SizeMeter[T] = SizeMeter(d => d.volume)

  implicit val signatureDataMarshaller: Marshaller[SignatureData] = marshaller
  implicit val signatureDataUnmarshaller: Unmarshaller[SignatureData] = unmarshaller
  implicit val signatureDataSizeMeter: SizeMeter[SignatureData] = sizeMeter

  implicit val (marshaledDataMarshaller, marshaledDataUnmarshaller, marshaledDataSizeMeter) = {
    implicit val drefDefault: Default[Data.Primitive.Ref] = Default(Data.Primitive.Ref(0))

    implicit val marshaledDataSimpleMarshaller: Marshaller[MarshalledData.Simple] = marshaller

    implicit val drefMarshaller: Marshaller[Data.Primitive.Ref] = marshaller
    implicit val drefToDataMarshaller: Marshaller[(Data.Primitive.Ref, Data)] = marshaller
    implicit val marshaledDataComplexMarshaller: Marshaller[MarshalledData.Complex] = marshaller

    implicit val marshaledDataSimpleUnmarshaller: Unmarshaller[MarshalledData.Simple] = unmarshaller

    implicit val drefUnmarshaller: Unmarshaller[Data.Primitive.Ref] = unmarshaller
    implicit val drefToDataUnmarshaller: Unmarshaller[(Data.Primitive.Ref, Data)] = unmarshaller
    implicit val marshaledDataComplexUnmarshaller: Unmarshaller[MarshalledData.Complex] = unmarshaller

    implicit val marshaledDataSimpleSizeMeter: SizeMeter[MarshalledData.Simple] = sizeMeter

    implicit val drefSizeMeter: SizeMeter[Data.Primitive.Ref] = sizeMeter
    implicit val drefToDataSizeMeter: SizeMeter[(Data.Primitive.Ref, Data)] = sizeMeter
    implicit val marshaledDataComplexSizeMeter: SizeMeter[MarshalledData.Complex] = sizeMeter

    (marshaller[MarshalledData], unmarshaller[MarshalledData], sizeMeter[MarshalledData])
  }

  implicit val marshalledDataDefault: Default[MarshalledData] = Default(MarshalledData.Simple(Data.Primitive.Null))

  implicit val eventMarshaller: Marshaller[Event] = marshaller
  implicit val eventUnmarshaller: Unmarshaller[Event] = unmarshaller
  implicit val eventSizeMeter: SizeMeter[Event] = sizeMeter

  implicit val (effectMarshaller, effectUnmarshaller, effectSizeMeter) = {
    import pravda.vm.Effect._

    implicit val pBytesDefault: Default[Data.Primitive.Bytes] = Default(Data.Primitive.Bytes.apply(ByteString.EMPTY))

    implicit val pBytesMarshaller: Marshaller[Data.Primitive.Bytes] = marshaller
    implicit val storageRemoveMarshaller: Marshaller[StorageRemove] = marshaller
    implicit val storageWriteMarshaller: Marshaller[StorageWrite] = marshaller
    implicit val storageReadMarshaller: Marshaller[StorageRead] = marshaller
    implicit val programCreateMarshaller: Marshaller[ProgramCreate] = marshaller
    implicit val programSealMarshaller: Marshaller[ProgramSeal] = marshaller
    implicit val programUpdateMarshaller: Marshaller[ProgramUpdate] = marshaller
    implicit val transferMarshaller: Marshaller[Transfer] = marshaller
    implicit val showBalanceMarshaller: Marshaller[ShowBalance] = marshaller

    implicit val pBytesUnmarshaller: Unmarshaller[Data.Primitive.Bytes] = unmarshaller
    implicit val storageRemoveUnmarshaller: Unmarshaller[StorageRemove] = unmarshaller
    implicit val storageWriteUnmarshaller: Unmarshaller[StorageWrite] = unmarshaller
    implicit val storageReadUnmarshaller: Unmarshaller[StorageRead] = unmarshaller
    implicit val programCreateUnmarshaller: Unmarshaller[ProgramCreate] = unmarshaller
    implicit val programSealUnmarshaller: Unmarshaller[ProgramSeal] = unmarshaller
    implicit val programUpdateUnmarshaller: Unmarshaller[ProgramUpdate] = unmarshaller
    implicit val transferUnmarshaller: Unmarshaller[Transfer] = unmarshaller
    implicit val showBalanceUnmarshaller: Unmarshaller[ShowBalance] = unmarshaller

    implicit val pBytesSizeMeter: SizeMeter[Data.Primitive.Bytes] = sizeMeter
    implicit val storageRemoveSizeMeter: SizeMeter[StorageRemove] = sizeMeter
    implicit val storageWriteSizeMeter: SizeMeter[StorageWrite] = sizeMeter
    implicit val storageReadSizeMeter: SizeMeter[StorageRead] = sizeMeter
    implicit val programCreateSizeMeter: SizeMeter[ProgramCreate] = sizeMeter
    implicit val programSealSizeMeter: SizeMeter[ProgramSeal] = sizeMeter
    implicit val programUpdateSizeMeter: SizeMeter[ProgramUpdate] = sizeMeter
    implicit val transferSizeMeter: SizeMeter[Transfer] = sizeMeter
    implicit val showBalanceSizeMeter: SizeMeter[ShowBalance] = sizeMeter

    (marshaller[Effect], unmarshaller[Effect], sizeMeter[Effect])
  }

  implicit val epkDefault: Default[EncryptedPrivateKey] =
    Default(EncryptedPrivateKey(ByteString.EMPTY, ByteString.EMPTY, ByteString.EMPTY))

  implicit val programEventsMarshaller: Marshaller[TransactionEffects.ProgramEvents] = marshaller
  implicit val transactionAllEffectsMarshaller: Marshaller[TransactionEffects.AllEffects] = marshaller
  implicit val transferMarshaller: Marshaller[Effect.Transfer] = marshaller
  implicit val transferEffectsMarshaller: Marshaller[TransactionEffects.Transfers] = marshaller
  implicit val signedTransactionMarshaller: Marshaller[SignedTransaction] = marshaller
  implicit val storedProgramMarshaller: Marshaller[StoredProgram] = marshaller
  implicit val epkMarshaller: Marshaller[EncryptedPrivateKey] = marshaller
  implicit val walletMarshaller: Marshaller[Wallet] = marshaller
  implicit val additionalDataForAddressMarshaller: Marshaller[AdditionalDataForAddress] = marshaller
  implicit val tidMdataMarshaller: Marshaller[(TransactionId, MarshalledData)] = marshaller

  implicit val programEventsUnmarshaller: Unmarshaller[TransactionEffects.ProgramEvents] = unmarshaller
  implicit val transactionAllEffectsUnmarshaller: Unmarshaller[TransactionEffects.AllEffects] = unmarshaller
  implicit val transferUnmarshaller: Unmarshaller[Effect.Transfer] = unmarshaller
  implicit val transferEffectsUnmarshaller: Unmarshaller[TransactionEffects.Transfers] = unmarshaller
  implicit val signedTransactionUnmarshaller: Unmarshaller[SignedTransaction] = unmarshaller
  implicit val storedProgramUnmarshaller: Unmarshaller[StoredProgram] = unmarshaller
  implicit val epkUnmarshaller: Unmarshaller[EncryptedPrivateKey] = unmarshaller
  implicit val walletUnmarshaller: Unmarshaller[Wallet] = unmarshaller
  implicit val additionalDataForAddressUnmarshaller: Unmarshaller[AdditionalDataForAddress] = unmarshaller
  implicit val tidMdataUnmarshaller: Unmarshaller[(TransactionId, MarshalledData)] = unmarshaller

  implicit val programEventsSizeMeter: SizeMeter[TransactionEffects.ProgramEvents] = sizeMeter
  implicit val transactionAllEffectsSizeMeter: SizeMeter[TransactionEffects.AllEffects] = sizeMeter
  implicit val transferSizeMeter: SizeMeter[Effect.Transfer] = sizeMeter
  implicit val transferEffectsSizeMeter: SizeMeter[TransactionEffects.Transfers] = sizeMeter
  implicit val signedTransactionSizeMeter: SizeMeter[SignedTransaction] = sizeMeter
  implicit val storedProgramSizeMeter: SizeMeter[StoredProgram] = sizeMeter
  implicit val epkSizeMeter: SizeMeter[EncryptedPrivateKey] = sizeMeter
  implicit val walletSizeMeter: SizeMeter[Wallet] = sizeMeter
  implicit val additionalDataForAddressSizeMeter: SizeMeter[AdditionalDataForAddress] = sizeMeter
  implicit val tidMdataSizeMeter: SizeMeter[(TransactionId, MarshalledData)] = sizeMeter

  implicit val (mtiseMarshaller, mtiseUnmarshaller, mtiseSizeMeter) = {
    implicit val tidToEffectMarshaller: Marshaller[(TransactionId, Seq[Effect])] = marshaller
    implicit val tidToEffectUnmarshaller: Unmarshaller[(TransactionId, Seq[Effect])] = unmarshaller
    implicit val tidToEffectSizeMeter: SizeMeter[(TransactionId, Seq[Effect])] = sizeMeter

    (marshaller[Tuple1[Map[TransactionId, Seq[Effect]]]],
     unmarshaller[Tuple1[Map[TransactionId, Seq[Effect]]]],
     sizeMeter[Tuple1[Map[TransactionId, Seq[Effect]]]])
  }
}

trait ZhukovLowPriorityInstances {

  implicit def zhukovDefaultLifter[T: Default, U]: Default[Tagged[T, U]] =
    lifterF[Default].lift[T, U]
}
