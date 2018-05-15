package io.mytc.timechain.data

import com.google.protobuf.ByteString
import common._
import supertagged.TaggedType

object blockchain {

  sealed trait Transaction {
    def from: Address
    def program: TransactionData
    def fee: Mytc

    def forSignature: (Address, TransactionData, Mytc) =
      (from, program, fee)
  }

  object Transaction {

    final case class UnsignedTransaction(from: Address, program: TransactionData, fee: Mytc) extends Transaction

    final case class SignedTransaction(from: Address, program: TransactionData, signature: ByteString, fee: Mytc)
        extends Transaction

    final case class AuthorizedTransaction(from: Address, program: TransactionData, signature: ByteString, fee: Mytc)
        extends Transaction
  }

  object TransactionData extends TaggedType[ByteString]
  type TransactionData = TransactionData.Type
}
