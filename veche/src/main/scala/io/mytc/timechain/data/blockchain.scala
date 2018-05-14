package io.mytc.timechain.data

import com.google.protobuf.ByteString
import common._

object blockchain {

  sealed trait Transaction {
    def from: Address
    def data: TransactionData
    def fee: Mytc
  }

  object Transaction {
    case class UnsignedTransaction(from: Address, data: TransactionData, fee: Mytc) extends Transaction
    case class SignedTransaction(from: Address, data: TransactionData, signature: ByteString, fee: Mytc) extends Transaction
    case class AuthorizedTransaction(from: Address, data: TransactionData, signature: ByteString, fee: Mytc) extends Transaction
  }

  sealed trait TransactionData

  object TransactionData {

    /**
      * Special transaction intended to solve one
      * of four PoS problems named "distribution".
      * In Timechain ICO members getting theirs
      * share of token sale via this transaction.
      * The transaction can be applied only in
      * the first block.
      */
    case class Distribution(accounts: List[(Address, Mytc)]) extends TransactionData

  }

}
