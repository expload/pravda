package pravda.dotnet.translation

object TranslationVisualizer {
  private def visualizeOpcode(opcode: Translator.OpCodeTranslation): List[(String, String)] = {
    val source = opcode.source.fold(identity, _.toString)
    val opcodeInfo = s"[<$source> stack_offset=${opcode.stackOffset.fold("none")(_.toString)}]"

    opcode.asm.map(_.toAsm) match {
      case head :: tail =>
        (head, opcodeInfo) :: tail.map((_, ""))
      case _ => List(("", opcodeInfo))
    }
  }

  private def visualizeMethod(method: Translator.MethodTranslation): String = {
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

  def visualize(translation: Translator.Translation): String = {
    s"""|[jump to methods]
        |${translation.jumpToMethods.map(_.toAsm).mkString("\n")}
        |
        |[methods]
        |${translation.methods.map(visualizeMethod).mkString("\n")}
        |
        |[finish]
        |${translation.finishOps.map(_.toAsm).mkString("\n")}""".stripMargin
  }
}
