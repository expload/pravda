package pravda.evm

import pravda.evm.abi.parse.AbiParser.AbiObject
import pravda.evm.translate.Translator
import pravda.evm.translate.Translator.Addressed
import pravda.vm._
import pravda.vm.asm.PravdaAssembler

object EvmSandbox {

  def runCode(input: VmSandbox.Preconditions,
              code: Seq[EVM.Op],
              abi: Seq[AbiObject]): Either[String, VmSandbox.ExpectationsWithoutWatts] = {
    val asmOps = Translator(code.toList, abi.toList)
    val asmProgramE = asmOps.map(ops => PravdaAssembler.assemble(ops, saveLabels = true))

    for {
      asmProgram <- asmProgramE
    } yield VmSandbox.ExpectationsWithoutWatts.fromExpectations(VmSandbox.run(input, asmProgram))
  }

  def runAddressedCode(input: VmSandbox.Preconditions,
                       code: Seq[Addressed[EVM.Op]],
                       abi: Seq[AbiObject]): Either[String, VmSandbox.ExpectationsWithoutWatts] = {
    val asmOps = Translator.translateActualContract(code.toList, abi.toList)
    val asmProgramE = asmOps.map(ops => PravdaAssembler.assemble(ops, saveLabels = true))

    for {
      asmProgram <- asmProgramE
    } yield VmSandbox.ExpectationsWithoutWatts.fromExpectations(VmSandbox.run(input, asmProgram))
  }
}
