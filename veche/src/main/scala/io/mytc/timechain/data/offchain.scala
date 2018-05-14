package io.mytc.timechain.data

import com.google.protobuf.ByteString
import io.mytc.timechain.data.common.{Address, DataRef}

object offchain {

  /**
    * @param nonce random 4 bytes to identify the intention
    * @param dataRef data reference customer want to buy
    */
  case class PurchaseIntentionData(
    nonce: Int,
    address: Address,
    dataRef: DataRef
  )

  sealed trait PurchaseIntention

  object PurchaseIntention {

    /**
      * @param signature signature of data reference
      */
    case class SignedPurchaseIntention(
      data: PurchaseIntentionData,
      signature: ByteString
    ) extends PurchaseIntention

    /**
      * @param signature signature of data reference
      */
    case class AuthorizedPurchaseIntention(
      data: PurchaseIntentionData,
      signature: ByteString
    ) extends PurchaseIntention
  }
}
