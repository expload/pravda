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

import pravda.vm.{Meta, asm}

final case class OpCodeTranslation(sourceMarks: List[Meta.SourceMark], asmOps: List[asm.Operation])

final case class MethodTranslation(kind: String, name: String, forceAdd: Boolean, opcodes: List[OpCodeTranslation]) {
  lazy val label: String = s"${kind}_$name"
}

final case class Translation(methods: List[MethodTranslation], funcs: List[MethodTranslation])
