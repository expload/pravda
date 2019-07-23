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

package pravda.common.vm

import pravda.common.domain.{Address, NativeCoin}

sealed trait Effect

object Effect {

  final case class StorageRemove(program: Address, key: Data, value: Option[Data]) extends Effect

  final case class StorageWrite(program: Address, key: Data, previous: Option[Data], value: Data) extends Effect

  final case class StorageRead(program: Address, key: Data, value: Option[Data]) extends Effect

  final case class ProgramCreate(address: Address, program: Data.Primitive.Bytes) extends Effect

  final case class ProgramSeal(address: Address) extends Effect

  final case class ProgramUpdate(address: Address, program: Data.Primitive.Bytes) extends Effect

  // TODO program address
  final case class Transfer(from: Address, to: Address, amount: NativeCoin) extends Effect

  final case class ShowBalance(address: Address, amount: NativeCoin) extends Effect

  final case class Event(program: Address, name: String, data: MarshalledData) extends Effect
}
