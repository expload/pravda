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

package pravda.evm.parse

import fastparse.byte.all._
import pravda.evm.EVM
import pravda.evm.EVM._
import cats.syntax.traverse._
import cats.instances.list._
import cats.instances.either._
import pravda.evm.translate.Translator.Addressed

object Parser {

  def apply(bytes: Bytes): Either[String, List[EVM.Op]] = {
    ops.parse(bytes).get.value
  }

  def parseWithIndices(bytes: Bytes): Either[String, List[Addressed[EVM.Op]]] = {
    opsWithIndices.parse(bytes).get.value.toList.map({ case (i, e) => e.map(op => (i, op)) }).sequence
  }

  private def push(cnt: Int): P[Push] = AnyByte.rep(exactly = cnt).!.map(Push)
  // FIXME the bytes default to zero if they extend past the limits

  val op: P[Either[String, Op]] = {

    def checkPush(i: Int) =
      if ((0x60 to 0x7f).contains(i)) {
        Some(push(i - 0x60 + 1))
      } else {
        None
      }

    def checkRanges(i: Int) = rangeOps.find(_._1.contains(i)).map(r => r._2(i))
    def checkSingleOps(i: Int) = singleOps.get(i)

    Int8.flatMap(b => {
      val i = b & 0xff
      checkSingleOps(i)
        .orElse(checkRanges(i))
        .map(PassWith)
        .orElse(checkPush(i))
        .map(_.map(Right(_)))
        .getOrElse(PassWith(Left(s"Unknown opcode: 0x${i.toHexString}")))
    })
  }

  private val ops: P[Either[String, List[Op]]] = P(Start ~ op.rep ~ End).map(ops => ops.toList.sequence)

  val opsWithIndices = P(Start ~ (Index ~ op).rep ~ End)

}
