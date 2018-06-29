package pravda.node.persistence

import pravda.common.domain.{Address, NativeCoin}
import pravda.node.db.DB
import implicits._

object BlockChainStore {

  def balanceEntry(db: DB): Entry[Address, NativeCoin] = Entry[Address, NativeCoin](db, "balance")

}
