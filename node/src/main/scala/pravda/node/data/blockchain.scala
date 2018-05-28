package pravda.node.data

import cats.Show
import com.google.protobuf.ByteString
import common._
import supertagged.TaggedType

object blockchain {

  sealed trait Transaction {
    val from: Address
    val program: TransactionData
    val fee: NativeCoins
    val wattPrice: BigDecimal
    val nonce: Int

    def forSignature: (Address, TransactionData, NativeCoins, BigDecimal, Int) =
      (from, program, fee, wattPrice, nonce)
  }

  object Transaction {

    final case class UnsignedTransaction(from: Address, program: TransactionData, fee: NativeCoins, wattPrice: BigDecimal, nonce: Int)
        extends Transaction

    final case class SignedTransaction(from: Address,
                                       program: TransactionData,
                                       signature: ByteString,
                                       fee: NativeCoins,
                                       wattPrice: BigDecimal,
                                       nonce: Int)
        extends Transaction

    import pravda.common.bytes

    object SignedTransaction {
      implicit val showInstance: Show[SignedTransaction] = { t =>
        val from = bytes.byteString2hex(t.from)
        val program = bytes.byteString2hex(t.program)
        val signature = bytes.byteString2hex(t.signature)
        s"transaction.signed[from=$from,program=$program,signature=$signature,nonce=${t.nonce},fee=${t.fee}]"
      }
    }

    final case class AuthorizedTransaction(from: Address,
                                           program: TransactionData,
                                           signature: ByteString,
                                           fee: NativeCoins,
                                           wattPrice: BigDecimal,
                                           nonce: Int)
        extends Transaction
  }

  object TransactionData extends TaggedType[ByteString]
  type TransactionData = TransactionData.Type
}
