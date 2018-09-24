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

package pravda.dotnet.translation.opcode

import pravda.dotnet.parser.CIL
import pravda.dotnet.parser.CIL._

object OpcodeDetectors {

  object IntLoad {

    def unapply(op: CIL.Op): Option[Int] = op match {
      case LdcI40      => Some(0)
      case LdcI41      => Some(1)
      case LdcI42      => Some(2)
      case LdcI43      => Some(3)
      case LdcI44      => Some(4)
      case LdcI45      => Some(5)
      case LdcI46      => Some(6)
      case LdcI47      => Some(7)
      case LdcI48      => Some(8)
      case LdcI4M1     => Some(-1)
      case LdcI4(num)  => Some(num)
      case LdcI4S(num) => Some(num.toInt)
      case _           => None
    }
  }
}
