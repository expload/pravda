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

package pravda.evm.translate

import pravda.evm.EVM
import pravda.vm.asm
import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._
import pravda.evm.EVM.{CodeCopy, Push}
import pravda.evm.translate.opcode.SimpleTranslation

object Translator {

  def apply(ops: List[EVM.Op]): Either[String, List[asm.Operation]] = {
    ops.map(SimpleTranslation.evmOpToOps).sequence.map(_.flatten)
  }

  def translateActualContract(ops: List[(Int,EVM.Op)]): Either[String, List[asm.Operation]] = {
    val offset = ops.takeWhile({case (_,CodeCopy) => false case _ => true}).reverse.tail.headOption match {
      case Some((_,Push(address))) => BigInt(1,address.toArray).intValue()
      case _ => throw new Exception("Parse error")
    }
    ops.flatMap({case(ind,op) => if (ind - offset < 0) None else Some((ind - offset,op)) })
  }

}
