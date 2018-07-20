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

package pravda.node.data

import cats.Show
import com.google.protobuf.ByteString
import pravda.common.domain._
import pravda.vm.Data
import pravda.vm.ExecutionResult
import pravda.vm.VmError.SomethingWrong
import supertagged.TaggedType

object blockchain {

  sealed trait Transaction {
    def from: Address
    def program: TransactionData
    def wattLimit: Long
    def wattPrice: NativeCoin
    def nonce: Int

    def forSignature: (Address, TransactionData, Long, NativeCoin, Int) =
      (from, program, wattLimit, wattPrice, nonce)
  }

  object Transaction {

    final case class UnsignedTransaction(from: Address,
                                         program: TransactionData,
                                         wattLimit: Long,
                                         wattPrice: NativeCoin,
                                         nonce: Int)
        extends Transaction

    final case class SignedTransaction(from: Address,
                                       program: TransactionData,
                                       signature: ByteString,
                                       wattLimit: Long,
                                       wattPrice: NativeCoin,
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
                                           nonce: Int)
        extends Transaction
  }

  object TransactionData extends TaggedType[ByteString]
  type TransactionData = TransactionData.Type

  final case class ExecutionInfo(
      error: Option[String],
      spentWatts: Long,
      refundWatts: Long,
      totalWatts: Long,
      stack: Seq[Data],
      heap: Seq[Data]
  ) {

    def status: String = error.fold("Ok")(identity)

  }

  object ExecutionInfo {

    def from(executionResult: ExecutionResult): ExecutionInfo = {
      ExecutionInfo(
        error = executionResult.error.map(_.error match {
          case SomethingWrong(err) => err.getMessage
          case err => err.toString
        }),
        spentWatts = executionResult.wattCounter.spent,
        refundWatts = executionResult.wattCounter.refund,
        totalWatts = executionResult.wattCounter.total,
        stack = executionResult.memory.stack,
        heap = executionResult.memory.heap
      )
    }
  }

}
