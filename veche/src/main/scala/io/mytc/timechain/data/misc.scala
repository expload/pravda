package io.mytc.timechain.data

import io.mytc.timechain.data.common.Address

object misc {

  final case class BlockChainInfo(
    height: Long,
    validators: Vector[Address]
  )
}
