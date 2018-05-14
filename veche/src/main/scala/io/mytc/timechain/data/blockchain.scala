package io.mytc.timechain.data

import com.google.protobuf.ByteString
import common._
import offchain._

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

    // Offer and exchange

    /**
      * Place data on blockchain and reward the [[user]]
      */
    case class Time(user: Address, dataRef: DataRef, reward: Mytc, tariff: Int) extends TransactionData

    /**
      * Transfer [[amount]] mytcs from signer account [[to]] account
      * @see [[Transaction.from]]
      */
    case class Transfer(to: Address, amount: Mytc) extends TransactionData

    // Misc

    /**
      * Company (data vendor or data consumer) should
      * describe itself to start operating.
      */
    case class ThisIsMe(domain: String) extends TransactionData

    // Data trading transactions

    /**
      * Customer deposit some money for seller
      * to prove significance of his intention
      */
    case class DataPurchasingDeposit(vendor: Address, amount: Mytc) extends TransactionData

    /**
      * Customer confirms that he received the data. Cost of data
      * transfers from customer deposit to vendor account
      */
    case class DataPurchasingConfirmation(dataRef: DataRef) extends TransactionData

    /**
      * Vendor should fill tariff matrix before any offer.
      * It has two dimensions - tariff and obsolescence.
      * The final data price depends on tariff, age of data,
      *   multiplier (@see io.mytc.timechain.data.blockchain.TransactionData.MultiplierUpdating)
      *   and some dynamically updated factor (market rate)
      */
    case class TariffMatrixUpdating(tariffMatrix: TariffMatrix) extends TransactionData

    /**
      * Multiplier is needed for calculating the final cost from the tariff matrix
      */
    case class MultiplierUpdating(value: BigDecimal) extends TransactionData

    /**
      * If vendor sure that he gave date to customer, but
      * customer didn't send confirmation, vendor can block
      * customer money forever
      */
    case class CheatingCustomerPunishment(intention: PurchaseIntention.SignedPurchaseIntention) extends TransactionData
  }

}
