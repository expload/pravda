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

package pravda.dotnet.translation

import pravda.dotnet.translation.data._
import pravda.dotnet.translation.opcode.OpcodeTranslator
import pravda.vm.asm.PravdaAssembler
import pravda.vm.asm

object TranslationVisualizer {
  // TODO simplify or rewrite, meta infromation

  private def renderAsmOp(op: asm.Operation): String =
    PravdaAssembler.render(op, pretty = true).replace('\n', ' ')

  private def visualizeOpcode(opcode: OpCodeTranslation): List[(String, String)] = {
    val sourceColumnRaw = opcode.source.fold(List(_), _.map(_.toString))
    val sourceColumn = sourceColumnRaw.map(c => s"[<$c>]")

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

    s"""|[method]
        |$opcodes""".stripMargin
  }

  private def visualizeFunction(func: OpcodeTranslator.HelperFunction): String = {
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
        |[funcs]
        |${translation.funcs.map(visualizeMethod).mkString("\n")}
        |
        |[functions]
        |${translation.helperFunctions.map(visualizeFunction).mkString("\n")}
        |
        |[finish]
        |${translation.finishOps.map(renderAsmOp).mkString("\n")}""".stripMargin
  }
}
