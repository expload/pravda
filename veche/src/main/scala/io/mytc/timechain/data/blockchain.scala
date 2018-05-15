package io.mytc.timechain.data

import com.google.protobuf.ByteString
import common._
import supertagged.TaggedType

object blockchain {

  sealed trait Transaction {
    def from: Address
    def data: TransactionData
    def fee: Mytc

    def forSignature: (Address, TransactionData, Mytc) =
      (from, data, fee)
  }

  object Transaction {
    final case class UnsignedTransaction(from: Address, data: TransactionData, fee: Mytc) extends Transaction
    final case class SignedTransaction(from: Address, data: TransactionData, signature: ByteString, fee: Mytc)
        extends Transaction
    final case class AuthorizedTransaction(from: Address, data: TransactionData, signature: ByteString, fee: Mytc)
        extends Transaction
  }

  object TransactionData extends TaggedType[ByteString]
  type TransactionData = TransactionData.Type
}
