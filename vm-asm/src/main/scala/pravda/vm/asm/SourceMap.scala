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

package pravda.vm.asm

import pravda.common.{bytes, domain}
import pravda.vm
import pravda.vm.Meta

object SourceMap {

  case class StackTraceElement(address: Option[domain.Address], sourceMark: Option[Meta.SourceMark])

  def findNearestMark(metas: Map[Int, Seq[Meta]], pos: Int): Option[Meta.SourceMark] = {
    metas.toBuffer
      .filter(_._1 <= pos)
      .sortBy(_._1)
      .flatMap(_._2)
      .collectFirst {
        case m: Meta.MethodSignature => None
        case m: Meta.SourceMark      => Some(m)
      }
      .flatten
  }

  def stackTrace(metas: Map[Int, Seq[Meta]], re: vm.RuntimeException): Seq[StackTraceElement] = {
    // Add re.lastPosition to last address call stack
    val cs = {
      val xs = re.callStack
      if (xs.nonEmpty) {
        val (ma, ys) = xs.last
        val i = xs.length - 1
        xs.updated(i, (ma, ys :+ re.lastPosition))
      } else {
        Seq((None, Seq(re.lastPosition)))
      }
    }
    for ((ma, st) <- cs.reverse; pos <- st.reverse)
      yield StackTraceElement(ma, findNearestMark(metas, pos))
    // TODO take into the account the other programs called with pcall
  }

  def renderStackTrace(st: Seq[StackTraceElement], indent: Int = 0): String =
    st.collect {
        case StackTraceElement(maybeAddress, Some(mark)) =>
          val address = maybeAddress
            .map(bytes.byteString2hex)
            .getOrElse(" " * 51 + "(transaction)")
          s"${" " * indent}$address:${mark.source}:${mark.startLine}"
      }
      .mkString("\n")
}
