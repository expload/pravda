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

sealed abstract class Error(val code: Int)

object Error {

  case object StackOverflow     extends Error(100)
  case object StackUnderflow    extends Error(101)
  case object WrongStackIndex   extends Error(102)
  case object WrongHeapIndex    extends Error(103)
  case object WrongType         extends Error(104)
  case object InvalidCoinAmount extends Error(105)
  case object InvalidAddress    extends Error(106)

  case object OperationDenied           extends Error(200)
  case object PcallDenied               extends Error(201)
  case object NotEnoughMoney            extends Error(202)
  case object AmountShouldNotBeNegative extends Error(203)

  case object NoSuchProgram         extends Error(300)
  case object NoSuchMethod          extends Error(302)
  case object NoSuchElement         extends Error(400)
  case object OutOfWatts            extends Error(500)
  case object CallStackOverflow     extends Error(600)
  case object CallStackUnderflow    extends Error(601)
  case object ExtCallStackOverflow  extends Error(602)
  case object ExtCallStackUnderflow extends Error(603)

  final case class UserError(message: String) extends Error(700)
  final case class DataError(message: String) extends Error(701)
}
