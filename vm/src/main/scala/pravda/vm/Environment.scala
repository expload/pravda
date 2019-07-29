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

package pravda.vm

import com.google.protobuf.ByteString
import pravda.common.data.blockchain.{Address, NativeCoin}
import pravda.common.vm.MarshalledData

trait Environment {

  /**
    * Current executor
    */
  def executor: Address

  def chainHeight: Long
  def lastBlockHash: ByteString
  def lastBlockTime: Long

  // Programs
  def sealProgram(address: Address): Unit
  def updateProgram(address: Address, code: ByteString): Unit
  def createProgram(address: Address, code: ByteString): Unit
  def getProgram(address: Address): Option[ProgramContext]

  def event(address: Address, name: String, data: MarshalledData): Unit

  // Balance
  def balance(address: Address): NativeCoin
  def transfer(from: Address, to: Address, amount: NativeCoin): Unit

  def updateValidator(validator: Address, power: Long): Unit
}
