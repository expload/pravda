package io.mytc.timechain.data

import com.google.protobuf.ByteString
import common._
import supertagged.TaggedType

object blockchain {

  sealed trait Transaction {
    def from: Address
    def program: TransactionData
    def fee: Mytc
    def nonce: Int

    def forSignature: (Address, TransactionData, Mytc, Int) =
      (from, program, fee, nonce)
  }

  object Transaction {

    final case class UnsignedTransaction(from: Address, program: TransactionData, fee: Mytc, nonce: Int)
        extends Transaction

    final case class SignedTransaction(from: Address,
                                       program: TransactionData,
                                       signature: ByteString,
                                       fee: Mytc,
                                       nonce: Int)
        extends Transaction

    final case class AuthorizedTransaction(from: Address,
                                           program: TransactionData,
                                           signature: ByteString,
                                           fee: Mytc,
                                           nonce: Int)
        extends Transaction
  }

  object TransactionData extends TaggedType[ByteString]
  type TransactionData = TransactionData.Type
}
