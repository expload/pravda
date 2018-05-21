package pravda.node.data

import pravda.node.data.common.Address

object misc {

  final case class BlockChainInfo(
      height: Long,
      validators: Vector[Address]
  )
}
