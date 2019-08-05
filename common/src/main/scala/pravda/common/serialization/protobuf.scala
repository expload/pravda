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

package pravda.common.serialization

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.data.blockchain.{Address, NativeCoin}
import supertagged.{Tagged, lifterF}
import zhukov._
import com.google.protobuf.ByteString
import pravda.common.data.blockchain
import pravda.common.data.blockchain.{Address, NativeCoin}
import pravda.common.data.blockchain._
import pravda.common.data.blockchain.Transaction._
import pravda.common.vm.Data
import pravda.common.cryptography.EncryptedPrivateKey
import pravda.common.vm.{Effect, MarshalledData}
import pravda.common.data.blockchain.{StoredProgram, TransactionEffects}
import supertagged.{@@, Tagged, lifterF}
import zhukov._
import zhukov.derivation._
import zhukov.Default.auto._

object protobuf extends ProtobufTranscoder with ZhukovInstances

trait ProtobufTranscoder {

  type ProtobufEncoder[T] = Transcoder[T, Protobuf]
  type ProtobufDecoder[T] = Transcoder[Protobuf, T]

  implicit def protobufEncoder[T: Marshaller]: ProtobufEncoder[T] =
    t => Protobuf @@ Marshaller[T].write(t, 1024 * 1024)

  implicit def protobufDecoder[T: Unmarshaller]: ProtobufDecoder[T] =
    t => Unmarshaller[T].read(Protobuf.untag(t))
}

trait CommonZhukovInstances extends ZhukovLowPriorityInstances {

  implicit lazy val nativeCoinMarshaller = Marshaller.LongMarshaller.contramap[NativeCoin](n => n)
  implicit lazy val nativeCoinUnmarshaller = Unmarshaller.long.map(NativeCoin @@ _)
  implicit lazy val nativeCoinSizeMeter = SizeMeter.long.contramap[NativeCoin](n => n)

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

  implicit lazy val addressMarshaller = Marshaller.bytesMarshaller[ByteString].contramap[Address](bs => bs)
  implicit lazy val addressUnmarshaller = Unmarshaller.bytes[ByteString].map(Address @@ _)
  implicit lazy val addressSizeMeter = SizeMeter.bytes[ByteString].contramap[Address](bs => bs)

}

trait VmZhukovInstances extends CommonZhukovInstances {
  implicit lazy val dataUnmarshaller = Unmarshaller.bytes[ByteString].map[Data](Data.fromByteString(_)._2)
  implicit lazy val dataMarshaller = Marshaller.bytesMarshaller[ByteString].contramap[Data](_.toByteString)
  implicit lazy val dataDefault: Default[Data] = Default(Data.Primitive.Null)
  implicit lazy val dataSizeMeter: SizeMeter[Data] = SizeMeter.bytes[ByteString].contramap[Data](_.toByteString)
  implicit lazy val (marshaledDataMarshaller: Marshaller[MarshalledData],
                     marshaledDataUnmarshaller: Unmarshaller[MarshalledData],
                     marshaledDataSizeMeter: SizeMeter[MarshalledData]) = {
    implicit lazy val drefDefault: Default[Data.Primitive.Ref] = Default(Data.Primitive.Ref(0))

    implicit lazy val marshaledDataSimpleMarshaller: Marshaller[MarshalledData.Simple] = marshaller

    implicit lazy val drefMarshaller: Marshaller[Data.Primitive.Ref] = marshaller
    implicit lazy val drefToDataMarshaller: Marshaller[(Data.Primitive.Ref, Data)] = marshaller
    implicit lazy val marshaledDataComplexMarshaller: Marshaller[MarshalledData.Complex] = marshaller

    implicit lazy val marshaledDataSimpleUnmarshaller: Unmarshaller[MarshalledData.Simple] = unmarshaller

    implicit lazy val drefUnmarshaller: Unmarshaller[Data.Primitive.Ref] = unmarshaller
    implicit lazy val drefToDataUnmarshaller: Unmarshaller[(Data.Primitive.Ref, Data)] = unmarshaller
    implicit lazy val marshaledDataComplexUnmarshaller: Unmarshaller[MarshalledData.Complex] = unmarshaller

    implicit lazy val marshaledDataSimpleSizeMeter: SizeMeter[MarshalledData.Simple] = sizeMeter

    implicit lazy val drefSizeMeter: SizeMeter[Data.Primitive.Ref] = sizeMeter
    implicit lazy val drefToDataSizeMeter: SizeMeter[(Data.Primitive.Ref, Data)] = sizeMeter
    implicit lazy val marshaledDataComplexSizeMeter: SizeMeter[MarshalledData.Complex] = sizeMeter

    (marshaller[MarshalledData], unmarshaller[MarshalledData], sizeMeter[MarshalledData])
  }

  implicit lazy val marshalledDataDefault: Default[MarshalledData] = Default(MarshalledData.Simple(Data.Primitive.Null))

  implicit lazy val eventMarshaller: Marshaller[Effect.Event] = marshaller
  implicit lazy val eventUnmarshaller: Unmarshaller[Effect.Event] = unmarshaller
  implicit lazy val eventSizeMeter: SizeMeter[Effect.Event] = sizeMeter

  implicit lazy val transferMarshaller: Marshaller[Effect.Transfer] = marshaller
  implicit lazy val transferUnmarshaller: Unmarshaller[Effect.Transfer] = unmarshaller
  implicit lazy val transferSizeMeter: SizeMeter[Effect.Transfer] = sizeMeter

  implicit lazy val showBalanceUnmarshaller: Unmarshaller[Effect.ShowBalance] = unmarshaller // ¯\_(ツ)_/¯

  implicit lazy val (effectMarshaller: Marshaller[Effect],
                     effectUnmarshaller: Unmarshaller[Effect],
                     effectSizeMeter: SizeMeter[Effect]) = {
    import pravda.common.vm.Effect._

    implicit lazy val pBytesDefault: Default[Data.Primitive.Bytes] = Default(
      Data.Primitive.Bytes.apply(ByteString.EMPTY))

    implicit lazy val pBytesMarshaller: Marshaller[Data.Primitive.Bytes] = marshaller
    implicit lazy val storageRemoveMarshaller: Marshaller[StorageRemove] = marshaller
    implicit lazy val storageWriteMarshaller: Marshaller[StorageWrite] = marshaller
    implicit lazy val storageReadMarshaller: Marshaller[StorageRead] = marshaller
    implicit lazy val programCreateMarshaller: Marshaller[ProgramCreate] = marshaller
    implicit lazy val programSealMarshaller: Marshaller[ProgramSeal] = marshaller
    implicit lazy val programUpdateMarshaller: Marshaller[ProgramUpdate] = marshaller
    implicit lazy val transferMarshaller: Marshaller[Transfer] = marshaller
    implicit lazy val showBalanceMarshaller: Marshaller[ShowBalance] = marshaller

    implicit lazy val pBytesUnmarshaller: Unmarshaller[Data.Primitive.Bytes] = unmarshaller
    implicit lazy val storageRemoveUnmarshaller: Unmarshaller[StorageRemove] = unmarshaller
    implicit lazy val storageWriteUnmarshaller: Unmarshaller[StorageWrite] = unmarshaller
    implicit lazy val storageReadUnmarshaller: Unmarshaller[StorageRead] = unmarshaller
    implicit lazy val programCreateUnmarshaller: Unmarshaller[ProgramCreate] = unmarshaller
    implicit lazy val programSealUnmarshaller: Unmarshaller[ProgramSeal] = unmarshaller
    implicit lazy val programUpdateUnmarshaller: Unmarshaller[ProgramUpdate] = unmarshaller

    implicit lazy val pBytesSizeMeter: SizeMeter[Data.Primitive.Bytes] = sizeMeter
    implicit lazy val storageRemoveSizeMeter: SizeMeter[StorageRemove] = sizeMeter
    implicit lazy val storageWriteSizeMeter: SizeMeter[StorageWrite] = sizeMeter
    implicit lazy val storageReadSizeMeter: SizeMeter[StorageRead] = sizeMeter
    implicit lazy val programCreateSizeMeter: SizeMeter[ProgramCreate] = sizeMeter
    implicit lazy val programSealSizeMeter: SizeMeter[ProgramSeal] = sizeMeter
    implicit lazy val programUpdateSizeMeter: SizeMeter[ProgramUpdate] = sizeMeter
    implicit lazy val transferSizeMeter: SizeMeter[Transfer] = sizeMeter
    implicit lazy val showBalanceSizeMeter: SizeMeter[ShowBalance] = sizeMeter

    (marshaller[Effect], unmarshaller[Effect], sizeMeter[Effect])
  }

  implicit lazy val transactionDataMarshaller =
    Marshaller.bytesMarshaller[ByteString].contramap[TransactionData](bs => bs)
  implicit lazy val transactionDataUnmarshaller = Unmarshaller.bytes[ByteString].map(TransactionData @@ _)
  implicit lazy val transactionDataSizeMeter = SizeMeter.bytes[ByteString].contramap[TransactionData](bs => bs)

  implicit lazy val signatureDataMarshaller: Marshaller[SignatureData] = marshaller
  implicit lazy val signatureDataUnmarshaller: Unmarshaller[SignatureData] = unmarshaller
  implicit lazy val signatureDataSizeMeter: SizeMeter[SignatureData] = sizeMeter
}

trait ZhukovInstances extends CommonZhukovInstances with VmZhukovInstances {
  implicit lazy val transctionIdMarshaller = Marshaller.bytesMarshaller[ByteString].contramap[TransactionId](bs => bs)
  implicit lazy val transctionIdUnmarshaller = Unmarshaller.bytes[ByteString].map(TransactionId @@ _)
  implicit lazy val transctionIdSizeMeter = SizeMeter.bytes[ByteString].contramap[TransactionId](bs => bs)

  implicit lazy val epkDefault: Default[EncryptedPrivateKey] =
    Default(EncryptedPrivateKey(ByteString.EMPTY, ByteString.EMPTY, ByteString.EMPTY))

  implicit lazy val programEventsMarshaller: Marshaller[TransactionEffects.ProgramEvents] = marshaller
  implicit lazy val transactionAllEffectsMarshaller: Marshaller[TransactionEffects.AllEffects] = marshaller
  implicit lazy val transferEffectsMarshaller: Marshaller[TransactionEffects.Transfers] = marshaller
  implicit lazy val signedTransactionMarshaller: Marshaller[SignedTransaction] = marshaller
  implicit lazy val storedProgramMarshaller: Marshaller[StoredProgram] = marshaller
  implicit lazy val epkMarshaller: Marshaller[EncryptedPrivateKey] = marshaller
  implicit lazy val eventDataMarshaller: Marshaller[(TransactionId, String, MarshalledData)] = marshaller
  implicit lazy val txIdIndexDataMarshaller: Marshaller[(Address, Long)] = marshaller

  implicit lazy val programEventsUnmarshaller: Unmarshaller[TransactionEffects.ProgramEvents] = unmarshaller
  implicit lazy val transactionAllEffectsUnmarshaller: Unmarshaller[TransactionEffects.AllEffects] = unmarshaller
  implicit lazy val transferEffectsUnmarshaller: Unmarshaller[TransactionEffects.Transfers] = unmarshaller
  implicit lazy val signedTransactionUnmarshaller: Unmarshaller[SignedTransaction] = unmarshaller
  implicit lazy val storedProgramUnmarshaller: Unmarshaller[StoredProgram] = unmarshaller
  implicit lazy val epkUnmarshaller: Unmarshaller[EncryptedPrivateKey] = unmarshaller
  implicit lazy val eventDataUnmarshaller: Unmarshaller[(TransactionId, String, MarshalledData)] = unmarshaller
  implicit lazy val txIdIndexDataUnmarshaller: Unmarshaller[(Address, Long)] = unmarshaller

  implicit lazy val programEventsSizeMeter: SizeMeter[TransactionEffects.ProgramEvents] = sizeMeter
  implicit lazy val transactionAllEffectsSizeMeter: SizeMeter[TransactionEffects.AllEffects] = sizeMeter
  implicit lazy val transferEffectsSizeMeter: SizeMeter[TransactionEffects.Transfers] = sizeMeter
  implicit lazy val signedTransactionSizeMeter: SizeMeter[SignedTransaction] = sizeMeter
  implicit lazy val storedProgramSizeMeter: SizeMeter[StoredProgram] = sizeMeter
  implicit lazy val epkSizeMeter: SizeMeter[EncryptedPrivateKey] = sizeMeter
  implicit lazy val eventDataSizeMeter: SizeMeter[(TransactionId, String, MarshalledData)] = sizeMeter
  implicit lazy val txIdIndexDataSizeMeter: SizeMeter[(Address, Long)] = sizeMeter

  implicit lazy val (mtiseMarshaller: Marshaller[Tuple1[Map[TransactionId, Seq[Effect]]]],
                     mtiseUnmarshaller: Unmarshaller[Tuple1[Map[TransactionId, Seq[Effect]]]],
                     mtiseSizeMeter: SizeMeter[Tuple1[Map[TransactionId, Seq[Effect]]]]) = {
    implicit lazy val tidToEffectMarshaller: Marshaller[(TransactionId, Seq[Effect])] = marshaller
    implicit lazy val tidToEffectUnmarshaller: Unmarshaller[(TransactionId, Seq[Effect])] = unmarshaller
    implicit lazy val tidToEffectSizeMeter: SizeMeter[(TransactionId, Seq[Effect])] = sizeMeter

    (marshaller[Tuple1[Map[TransactionId, Seq[Effect]]]],
     unmarshaller[Tuple1[Map[TransactionId, Seq[Effect]]]],
     sizeMeter[Tuple1[Map[TransactionId, Seq[Effect]]]])
  }
}

trait ZhukovLowPriorityInstances {
  implicit def zhukovDefaultLifter[T: Default, U]: Default[Tagged[T, U]] =
    lifterF[Default].lift[T, U]
}
