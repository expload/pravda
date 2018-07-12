package pravda.dotnet.translation

import pravda.dotnet.translation.data._
import pravda.dotnet.translation.opcode.OpcodeTranslator
import pravda.vm.asm.PravdaAssembler
import pravda.vm.asm

object TranslationVisualizer {
  private def renderAsmOp(op: asm.Operation): String =
    PravdaAssembler.render(op, pretty = true).replace('\n', ' ')

  private def visualizeOpcode(opcode: OpCodeTranslation): List[(String, String)] = {
    val sourceColumnRaw = opcode.source.fold(List(_), _.map(_.toString))

    val sourceColumnHead = (sourceColumnRaw match {
      case head :: _ => s"[<$head> "
      case _         => "["
    }) + s"stack_offset=${opcode.stackOffset.fold("none")(_.toString)}]"

    val sourceColumnTail = sourceColumnRaw match {
      case _ :: tail => tail.map(s => s"[<$s>]")
      case _         => List.empty
    }

    val sourceColumn = sourceColumnHead :: sourceColumnTail

    opcode.asmOps
      .map(renderAsmOp)
      .zipAll(sourceColumn, "", "")
  }

  private def visualizeMethod(method: MethodTranslation): String = {
    val opcodesColumns = method.opcodes.flatMap(visualizeOpcode)
    val firstColumn = opcodesColumns.map(_._1).map(_.length).max
    val opcodes = opcodesColumns
      .map {
        case (fst, snd) => fst.padTo(firstColumn + 1, ' ') + snd
      }
      .mkString("\n")

    s"""|[method ${method.name} args=${method.argsCount} locals=${method.localsCount} local=${method.local}]
        |$opcodes""".stripMargin
  }

  private def visualizeFunction(func: OpcodeTranslator.AdditionalFunction): String = {
    s"""|[function ${func.name}]
        |${func.ops.map(renderAsmOp).mkString("\n")}""".stripMargin
  }

  def visualize(translation: Translation): String = {
    s"""|[jump to methods]
        |${translation.jumpToMethods.map(renderAsmOp).mkString("\n")}
        |
        |[methods]
        |${translation.methods.map(visualizeMethod).mkString("\n")}
        |
        |[functions]
        |${translation.functions.map(visualizeFunction).mkString("\n")}
        |
        |[finish]
        |${translation.finishOps.map(renderAsmOp).mkString("\n")}""".stripMargin
  }
}
