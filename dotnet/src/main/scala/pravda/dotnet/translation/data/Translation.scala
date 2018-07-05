package pravda.dotnet.translation.data

import pravda.dotnet.parsers.CIL
import pravda.vm.asm

final case class OpCodeTranslation(source: Either[String, CIL.Op], // some name or actual opcode
                                   stackOffset: Option[Int],
                                   asmOps: List[asm.Operation])

final case class MethodTranslation(name: String,
                                   argsCount: Int,
                                   localsCount: Int,
                                   local: Boolean,
                                   void: Boolean,
                                   opcodes: List[OpCodeTranslation])

final case class Translation(jumpToMethods: List[asm.Operation],
                             methods: List[MethodTranslation],
                             finishOps: List[asm.Operation])
