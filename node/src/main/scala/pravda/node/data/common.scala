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

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.bytes.byteString2hex
import pravda.common.domain.{Address, NativeCoin}
import supertagged.TaggedType
import java.security.MessageDigest

object common {

  private val sha256 = MessageDigest.getInstance("SHA-256")

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
      val hash = sha256.digest(tx.toByteArray)
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

}
