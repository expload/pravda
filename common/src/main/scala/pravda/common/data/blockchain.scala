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

package pravda.common.data

import cats.Show
import com.google.protobuf.ByteString
import pravda.common.Hasher
import pravda.common.vm.{Effect, ExecutionResult, MarshalledData}
import supertagged.TaggedType
import pravda.common.bytes._

import scala.collection.mutable
import scala.util.Try

object blockchain {

  object NativeCoin extends TaggedType[Long] {
    val zero = apply(0L)
    def amount(v: Int) = NativeCoin(v.toLong)
    def amount(v: String) = NativeCoin(v.toLong)
    def fromString(s: String) = amount(s)
  }

  type NativeCoin = NativeCoin.Type

  object Address extends TaggedType[ByteString] {

    final val Void = {
      val bytes = ByteString.copyFrom(Array.fill(32)(0.toByte))
      Address(bytes)
    }

    def tryFromHex(hex: String): Try[Address] =
      Try(Address(hex2byteString(hex)))

    def fromHex(hex: String): Address =
      Address(hex2byteString(hex))

    def fromByteArray(arr: Array[Byte]): Address = {
      Address(ByteString.copyFrom(arr))
    }
  }
  type Address = Address.Type

  def eventKey(address: Address): String =
    s"${byteString2hex(address)}"

  def eventKeyLength(address: Address): String =
    s"${byteString2hex(address)}:#len"
  // the # character has the lower ASCII code so it will be place before any number

  def eventKeyOffset(address: Address, offset: Long): String =
    f"${byteString2hex(address)}:$offset%016x"

  object PrivateKey extends TaggedType[ByteString] {

    def fromHex(hex: String): PrivateKey =
      PrivateKey(ByteString.copyFrom(hex2bytes(hex)))
  }
  type PrivateKey = PrivateKey.Type

  final case class SignatureData(address: Address,
                                 data: TransactionData,
                                 wattLimit: Long,
                                 wattPrice: NativeCoin,
                                 nonce: Int,
                                 wattPayer: Option[Address])

  sealed trait Transaction {
    def from: Address
    def program: TransactionData
    def wattLimit: Long
    def wattPrice: NativeCoin
    def wattPayer: Option[Address]
    def nonce: Int

    def forSignature: SignatureData =
      SignatureData(from, program, wattLimit, wattPrice, nonce, wattPayer)
  }

  object Transaction {

    final case class UnsignedTransaction(from: Address,
                                         program: TransactionData,
                                         wattLimit: Long,
                                         wattPrice: NativeCoin,
                                         wattPayer: Option[Address],
                                         nonce: Int)
        extends Transaction

    final case class SignedTransaction(from: Address,
                                       program: TransactionData,
                                       signature: ByteString,
                                       wattLimit: Long,
                                       wattPrice: NativeCoin,
                                       wattPayer: Option[Address],
                                       wattPayerSignature: Option[ByteString],
                                       nonce: Int)
        extends Transaction

    import pravda.common.bytes

    object SignedTransaction {
      implicit val showInstance: Show[SignedTransaction] = { t =>
        val from = bytes.byteString2hex(t.from)
        val program = bytes.byteString2hex(t.program)
        val signature = bytes.byteString2hex(t.signature)
        s"transaction.signed[from=$from,program=$program,signature=$signature,nonce=${t.nonce},wattLmit=${t.wattLimit},wattPrice=${t.wattPrice}]"
      }
    }

    final case class AuthorizedTransaction(from: Address,
                                           program: TransactionData,
                                           signature: ByteString,
                                           wattLimit: Long,
                                           wattPrice: NativeCoin,
                                           wattPayer: Option[Address],
                                           wattPayerSignature: Option[ByteString],
                                           nonce: Int)
        extends Transaction
  }

  object TransactionData extends TaggedType[ByteString]
  type TransactionData = TransactionData.Type

  /**
    * Sha256 hash of Protobuf representation of signed transaction
    */
  object TransactionId extends TaggedType[ByteString] {

    final val Empty = forEncodedTransaction(ByteString.EMPTY)

    def forEncodedTransaction(tx: ByteString): TransactionId = {
      // go-wire encoding
      //      val buffer = ByteBuffer
      //        .allocate(3 + tx.size)
      //        .put(0x02.toByte) // size of size
      //        .putShort(tx.size.toShort) // size
      //        .put(tx.toByteArray) // data
      //      val hash = ripemd160.getHash(buffer.array())
      val hash = Hasher.sha256.get().digest(tx.toByteArray)
      TransactionId @@ ByteString.copyFrom(hash)
    }
  }

  type TransactionId = TransactionId.Type

  def transactionIdKey(transactionId: TransactionId): String =
    s"${byteString2hex(transactionId)}"

  def transactionIdKeyLength(transactionId: TransactionId): String =
    s"${byteString2hex(transactionId)}:#len"
  // the # character has the lower ASCII code so it will be place before any number

  def transactionIdKeyOffset(transactionId: TransactionId, offset: Long): String =
    f"${byteString2hex(transactionId)}:$offset%016x"

  final case class ApplicationStateInfo(blockHeight: Long,
                                        appHash: ByteString,
                                        validators: Vector[Address],
                                        blockTimestamp: Long)

  object ApplicationStateInfo {
    lazy val Empty = ApplicationStateInfo(0, ByteString.EMPTY, Vector.empty[Address], 0L)
  }

  final case class CoinDistributionMember(address: Address, amount: NativeCoin)

  final case class StoredProgram(code: ByteString, `sealed`: Boolean)

  final case class TransactionResultInfo(timestamp: Long, effects: mutable.Buffer[Effect])

  sealed trait TransactionEffects {
    def num: Long
    def transactionId: TransactionId
    // A height of the block that the transaction was committed in
    def blockHeight: Long
    def blockTimestamp: Long
    def identifier: String
  }

  object TransactionEffects {
    final case class Transfers(num: Long,
                               blockHeight: Long,
                               blockTimestamp: Long,
                               transactionId: TransactionId,
                               transfers: Seq[Effect.Transfer])
        extends TransactionEffects {
      override val identifier = Transfers.identifier
    }

    object Transfers {
      lazy val identifier = "Transfers"
    }

    final case class ProgramEvents(num: Long,
                                   blockHeight: Long,
                                   blockTimestamp: Long,
                                   transactionId: TransactionId,
                                   events: Seq[Effect.Event])
        extends TransactionEffects {
      override val identifier = ProgramEvents.identifier
    }

    object ProgramEvents {
      lazy val identifier = "ProgramEvents"
    }

    final case class AllEffects(num: Long,
                                blockHeight: Long,
                                blockTimestamp: Long,
                                transactionId: TransactionId,
                                effects: Seq[Effect])
        extends TransactionEffects {
      override val identifier = AllEffects.identifier
    }

    object AllEffects {
      lazy val identifier = "AllEffects"
    }
  }

  final case class TransactionResult(
      transactionId: TransactionId,
      executionResult: ExecutionResult,
      effects: Seq[Effect]
  )

  final val UnknownError = RpcError(-1, "", "")

  final case class RpcException(error: RpcError) extends Exception(s"${error.message}: ${error.data}")
  final case class RpcHttpException(httpCode: Int)
      extends Exception(s"RPC request to Tendermint failed with HTTP code $httpCode")

  final case class TxSyncResult(check_tx: TxResult)
  final case class RpcSyncResponse(jsonrpc: String, id: String, result: TxSyncResult)
  final case class RpcAsyncResponse(jsonrpc: String, id: String, result: TxResult)

  final case class RpcCommitResponse(result: Option[TxCommitResult], error: Option[RpcError])
  final case class TxCommitResult(check_tx: TxResult, deliver_tx: TxResult)
  final case class TxResult(log: Option[String])

  final case class RpcError(code: Int, message: String, data: String)
  final case class RpcTxResponse(error: Option[RpcError], result: Option[RpcTxResponse.Result])

  object RpcTxResponse {
    import com.google.protobuf.{ByteString => PbByteString}
    final case class Result(hash: String, height: String, tx: PbByteString)
  }

  final case class EventItem(offset: Long,
                             transactionId: TransactionId,
                             timestamp: Long,
                             address: Address,
                             name: String,
                             data: MarshalledData)
}
