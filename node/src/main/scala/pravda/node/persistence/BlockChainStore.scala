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

import pravda.common.domain.{Address, NativeCoin}
import pravda.node.db.DB
import pravda.node.persistence.implicits._
import pravda.node.data.serialization.protobuf._
import pravda.node.data.serialization.composite._

object BlockChainStore {
  def balanceEntry(db: DB): Entry[Address, NativeCoin] = Entry[Address, NativeCoin](db, "balance")
  def transferEffectsEntry(db: DB): DbPath = new PureDbPath(db, "transferEffectsByAddress")
  def eventsByAddressEntry(db: DB): DbPath = new PureDbPath(db, "eventsByAddress")
  def eventsEntry(db: DB): DbPath = new PureDbPath(db, "events")
  def transactionsEntry(db: DB): DbPath = new PureDbPath(db, "transactionsByAddress")
}
