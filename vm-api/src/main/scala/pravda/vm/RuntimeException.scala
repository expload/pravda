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

import pravda.common.domain.Address

final case class RuntimeException(error: Error,
                                  finalState: FinalState,
                                  callStack: Seq[(Option[Address], Seq[Int])],
                                  lastPosition: Int)

//object RuntimeException {
//
//  def constructStackTraceLine(metas: List[Meta], pos: Int): String = {
//    val translatorMessage = metas.collectFirst { case TranslatorMark(mark) => mark }
//    val sources = metas.collectFirst { case s: SourceMark                  => s.markString }
//    s"${translatorMessage.getOrElse(s"program:$pos")}${sources.map(s => s" ($s)").getOrElse("")}"
//  }
//}
