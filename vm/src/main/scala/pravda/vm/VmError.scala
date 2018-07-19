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

sealed abstract class VmError(val code: Int)

object VmError {

  case object StackOverflow     extends VmError(100)
  case object StackUnderflow    extends VmError(101)
  case object WrongStackIndex   extends VmError(102)
  case object WrongHeapIndex    extends VmError(103)
  case object WrongType         extends VmError(104)
  case object InvalidCoinAmount extends VmError(104)
  case object InvalidAddress    extends VmError(104)

  case object OperationDenied      extends VmError(200)
  case object NoSuchProgram        extends VmError(300)
  case object NoSuchLibrary        extends VmError(301)
  case object NoSuchMethod         extends VmError(302)
  case object NoSuchElement        extends VmError(400)
  case object OutOfWatts           extends VmError(500)
  case object CallStackOverflow    extends VmError(600)
  case object CallStackUnderflow   extends VmError(601)
  case object ExtCallStackOverflow extends VmError(602)

  final case class SomethingWrong(ex: Throwable) extends VmError(999)
}
