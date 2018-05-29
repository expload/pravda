package pravda.node.data

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import pravda.common.contrib.ripemd160
import supertagged.TaggedType

object common {

  /**
    * Ripemd160 hash of BSON representation of signed transaction
    */
  object TransactionId extends TaggedType[ByteString] {

    def forEncodedTransaction(tx: ByteString): TransactionId = {
      // go-wire encoding
      val buffer = ByteBuffer
        .allocate(3 + tx.size)
        .put(0x02.toByte) // size of size
        .putShort(tx.size.toShort) // size
        .put(tx.toByteArray) // data
      val hash = ripemd160.getHash(buffer.array())
      TransactionId @@ ByteString.copyFrom(hash)
    }
  }

  type TransactionId = TransactionId.Type

  final case class ApplicationStateInfo(blockHeight: Long, appHash: ByteString)
}
