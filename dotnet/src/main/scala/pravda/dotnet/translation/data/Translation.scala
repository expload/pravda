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
import pravda.dotnet.translation.opcode.OpcodeTranslator
import pravda.vm.asm

final case class OpCodeTranslation(source: Either[String, List[CIL.Op]], // some name or actual opcode
                                   stackOffset: Option[Int],
                                   asmOps: List[asm.Operation])

final case class MethodTranslation(name: String,
                                   argsCount: Int,
                                   localsCount: Int,
                                   local: Boolean,
                                   void: Boolean,
                                   opcodes: List[OpCodeTranslation],
                                   additionalFunctions: List[OpcodeTranslator.AdditionalFunction])

final case class ConstructorTranslation(jumpToConstructor: List[asm.Operation],
                                        ctorPrefix: List[asm.Operation],
                                        ctor: MethodTranslation)

final case class Translation(jumpToMethods: List[asm.Operation],
                             methods: List[MethodTranslation],
                             constructor: Option[ConstructorTranslation],
                             funcs: List[MethodTranslation],
                             functions: List[OpcodeTranslator.AdditionalFunction],
                             finishOps: List[asm.Operation])
