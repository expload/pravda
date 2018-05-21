package pravda.node.persistence

import pravda.node.db.DB
import pravda.node.data.domain._
import scala.concurrent.Future

// Implicits
import pravda.node.persistence.implicits._

object NodeStore {
  def apply(path: String): NodeStore = new NodeStore(path)
}

class NodeStore(path: String) {

  type FOpt[A] = Future[Option[A]]

  private val db: DB = DB(path, None)

  private val walletEntry = Entry[String, Wallet](db, "wallet")

  def wallets(): Future[List[Wallet]] = {
    walletEntry.all
  }

  def getWallet(name: String): Future[Option[Wallet]] = {
    walletEntry.get(name)
  }

  def putWallet(wallet: Wallet): Future[Unit] = {
    walletEntry.put(wallet.name, wallet)
  }

  def close(): Unit = {
    db.close()
  }

}
