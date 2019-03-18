/*
 * Copyright (C) 2018  Expload.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pravda.node.persistence

import pravda.node.db.DB
import pravda.node.data.domain._
import scala.concurrent.Future

// Implicits
import pravda.node.persistence.implicits._
import pravda.node.data.serialization.composite._
import pravda.node.data.serialization.protobuf._

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
