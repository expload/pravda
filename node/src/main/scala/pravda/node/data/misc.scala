package pravda.node.data

import pravda.common.domain.Address

object misc {

  final case class BlockChainInfo(
      height: Long,
      validators: Vector[Address]
  )
}
