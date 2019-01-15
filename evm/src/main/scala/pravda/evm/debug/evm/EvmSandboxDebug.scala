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

package pravda.evm.debug.evm

import pravda.evm.EVM
import pravda.evm.abi.parse.AbiParser.AbiObject
import pravda.evm.debug.{Debugger, VmSandboxDebug}
import pravda.evm.translate.Translator.Addressed
import pravda.vm.asm.PravdaAssembler

object EvmSandboxDebug {

  def debugCode[L](input: VmSandboxDebug.Preconditions, code: Seq[EVM.Op], abi: Seq[AbiObject])(
      implicit deb: Debugger[L]): Either[String, L] = {
    val asmOps = EvmDebugTranslator(code.toList, abi.toList)
    val asmProgramE = asmOps.map(ops => PravdaAssembler.assemble(ops, saveLabels = true))

    for {
      asmProgram <- asmProgramE
    } yield VmSandboxDebug.run(input, asmProgram)
  }

  def debugAddressedCode[L](input: VmSandboxDebug.Preconditions, code: Seq[Addressed[EVM.Op]], abi: Seq[AbiObject])(
      implicit deb: Debugger[L]): Either[String, L] = {
    val asmOps = EvmDebugTranslator.debugTranslateActualContract(code.toList, abi.toList)
    val asmProgramE = asmOps.map(ops => PravdaAssembler.assemble(ops, saveLabels = true))

    for {
      asmProgram <- asmProgramE
    } yield VmSandboxDebug.run(input, asmProgram)
  }
}
