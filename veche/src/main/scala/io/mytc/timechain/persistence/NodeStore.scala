package io.mytc

package timechain.persistence

import io.mytc.keyvalue.DB
import io.mytc.timechain.data.domain._
import scala.concurrent.Future

// Implicits
import io.mytc.timechain.persistence.implicits._

object NodeStore {
  def apply(path: String): NodeStore = new NodeStore(path)
}

class NodeStore(path: String) {

  type FOpt[A] = Future[Option[A]]

  private implicit val db: DB = DB(path)

  private val walletEntry = Entry[String, Wallet]("wallet")

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
