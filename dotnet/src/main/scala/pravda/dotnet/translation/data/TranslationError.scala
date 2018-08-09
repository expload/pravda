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

package pravda.dotnet.translation.data

import pravda.dotnet.parsers.CIL
import pravda.vm.Meta

sealed trait InnerTranslationError {
  def mkString: String
}
case object UnknownOpcode extends InnerTranslationError {
  def mkString: String = ???
}
final case class NotSupportedOpcode(op: CIL.Op) extends InnerTranslationError {
  def mkString: String = s"$op is not supported"
}
final case class InternalError(err: String) extends InnerTranslationError {
  def mkString: String = err
}

final case class TranslationError(inner: InnerTranslationError, pos: Option[Meta.SourceMark]) {

  def mkString: String = {
    s"${inner.mkString}${pos.map(p => s"\n  ${p.markString}").getOrElse("")}"
  }
}
