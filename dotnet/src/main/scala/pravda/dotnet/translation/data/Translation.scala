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

final case class Translation(jumpToMethods: List[asm.Operation],
                             methods: List[MethodTranslation],
                             functions: List[OpcodeTranslator.AdditionalFunction],
                             finishOps: List[asm.Operation])
