package io.mytc.timechain.data

import com.google.protobuf.ByteString
import io.mytc.timechain.data.common.Address
import io.mytc.timechain.data.serialization.json._

object misc {

  case class BlockChainInfo(
    height: Long,
    validators: Vector[Address]
  )
}
