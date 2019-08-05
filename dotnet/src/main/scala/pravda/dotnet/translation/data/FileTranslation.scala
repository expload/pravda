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

import pravda.common.vm.Meta
import pravda.vm.asm

/**
  * Translation of one CIL opcode
  * @param sourceMarks source marks to the C# source of CIL opcode
  * @param asmOps resulted Pravda opcodes
  */
final case class OpCodeTranslation(sourceMarks: List[Meta.SourceMark], asmOps: List[asm.Operation])

/**
  * Translation of CIL method
  *
  * @param kind special prefix needed to distinguish different kinds of methods inside translation
  * @param name name of the method
  * @param forceAdd mark indicating that this method shouldn't be removed by dead code elimination
  * @param opcodes translations of each opcode in the method
  */
final case class MethodTranslation(kind: String, name: String, forceAdd: Boolean, opcodes: List[OpCodeTranslation]) {
  lazy val label: String = s"${kind}_$name"
}

/**
  * Translation of file with CIL code
  *
  * @param methods translations of each public [Program] method in the file
  * @param funcs translations of other method in the file
  */
final case class FileTranslation(methods: List[MethodTranslation], funcs: List[MethodTranslation])

/**
  * Translation of the whole compiled program
  *
  * @param file merged [[FileTranslation]]s of all files in the program
  * @param programName name of the program
  */
final case class Translation(file: FileTranslation, programName: String)
