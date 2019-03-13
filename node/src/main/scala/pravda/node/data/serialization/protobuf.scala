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

  implicit val signatureDataFormat: Format[SignatureData] = format

  implicit def dataUnmarshaller: Unmarshaller[Data] = Unmarshaller.bytes[Array[Byte]].map(Data.fromBytes)
  implicit val dataMarshaller: Marshaller[Data] =
    Marshaller.bytesMarshaller[Array[Byte]].contramap[Data](_.toByteString.toByteArray)
  implicit val dataDefault: Default[Data] = Default(Data.Primitive.Null)
  implicit def dataSizeMeter[T <: Data]: SizeMeter[T] = SizeMeter(d => d.volume)

  implicit val marshaledDataFormat: Format[MarshalledData] = {
    implicit val marshaledDataSimpleFormat: Format[MarshalledData.Simple] = format

    implicit val drefFormat: Format[Data.Primitive.Ref] = format
    implicit val drefDefault: Default[Data.Primitive.Ref] = Default(Data.Primitive.Ref(0))
    implicit val drefToDataFormat: Format[(Data.Primitive.Ref, Data)] = format
    implicit val marshaledDataComplexFormat: Format[MarshalledData.Complex] = format

    format
  }

  implicit val marshalledDataDefault: Default[MarshalledData] = Default(MarshalledData.Simple(Data.Primitive.Null))

  implicit val eventFormat: Format[Effect.Event] = format

  implicit val effectFormat: Format[Effect] = {
    import pravda.vm.Effect._

    implicit val pBytesDefault: Default[Data.Primitive.Bytes] = Default(Data.Primitive.Bytes.apply(ByteString.EMPTY))
    implicit val pBytesFormat: Format[Data.Primitive.Bytes] = format

    implicit val storageRemoveFormat: Format[StorageRemove] = format
    implicit val storageWriteFormat: Format[StorageWrite] = format
    implicit val storageReadFormat: Format[StorageRead] = format
    implicit val programCreateFormat: Format[ProgramCreate] = format
    implicit val programSealFormat: Format[ProgramSeal] = format
    implicit val programUpdateFormat: Format[ProgramUpdate] = format
    implicit val transferFormat: Format[Transfer] = format
    implicit val showBalanceFormat: Format[ShowBalance] = format
    implicit val eventFormat: Format[Event] = format

    format
  }

  implicit val programEventsFormat: Format[TransactionEffects.ProgramEvents] = format

  implicit val transactionAllEffectsFormat: Format[TransactionEffects.AllEffects] = format

  implicit val transferFormat: Format[Effect.Transfer] = format

  implicit val transferEffectsFormat: Format[TransactionEffects.Transfers] = format

  implicit val signedTransactionFormat: Format[SignedTransaction] = format

  implicit val storedProgramFormat: Format[StoredProgram] = format

  implicit val epkFormat: Format[EncryptedPrivateKey] = format
  implicit val epkDefault: Default[EncryptedPrivateKey] =
    Default(EncryptedPrivateKey(ByteString.EMPTY, ByteString.EMPTY, ByteString.EMPTY))

  implicit val walletFormat: Format[Wallet] = format

  implicit val additionalDataForAddressFormat: Format[AdditionalDataForAddress] = format

  implicit val tidMdataFormat: Format[(TransactionId, MarshalledData)] = format

  implicit val mtiseFormat: Format[Tuple1[Map[TransactionId, Seq[Effect]]]] = {
    implicit val tidToEffectFormat: Format[(TransactionId, Seq[Effect])] = format
    format
  }
}

trait ZhukovLowPriorityInstances {

  implicit def zhukovDefaultLifter[T: Default, U]: Default[Tagged[T, U]] =
    lifterF[Default].lift[T, U]
}
