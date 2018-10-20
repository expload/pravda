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

package pravda.yopt.printers

import pravda.yopt.CommandLine
import pravda.yopt.CommandLine.CmdPath

object MarkdownPrinter {

  object MarkdownCtx {
    val Default = MarkdownCtx("  ")
  }
  final case class MarkdownCtx(tab: String, lvl: Int = 0)

  private val EOL = "\n"

  def printPath[C](cmdPath: CmdPath[C], ctx: MarkdownCtx = MarkdownCtx.Default): String = {
    val opts = cmdPath.opts
    val cmds = CmdPath.walk(cmdPath)
    val optHead = s"## Options$EOL$EOL|Option|Description|$EOL|----|----|"
    val optBody = opts.map(printOpt(_, ctx)).mkString(EOL)
    val cmdHead = s"## Commands$EOL$EOL|Command|Description|$EOL|----|----|"
    val cmdBody = cmds
      .map { path =>
        val desc = path.text.replace('\n', ' ')
        val comm = path.cmds.map(_.name).mkString(" ")
        val link = s"[`$comm`](${path.toString}.md)"
        s"|$link|$desc|"
      }
      .mkString(EOL)
    val optAll = if (opts.nonEmpty) s"$optHead$EOL$optBody" else s"${EOL}No options available"
    val cmdAll = if (cmds.nonEmpty) s"$cmdHead$EOL$cmdBody" else ""
    List(printHeader(cmdPath), optAll, cmdAll).mkString(EOL)
  }

  private def printHeader[C](cmdPath: CmdPath[C]): String = {
    val dne = s"<!--${EOL}THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!$EOL-->"
    val usage = s"```${cmdPath.toUsageString}```"
    val desc = s"## Description$EOL${cmdPath.mdText}"
    List(dne, usage, desc).mkString(s"$EOL$EOL")
  }

  private def printOpt[C](opt: CommandLine.Opt[C, _], ctx: MarkdownCtx): String =
    s"|${opt.short.map(x => s"`-$x`, ").getOrElse("")}`--${opt.name}`|${opt.text.replace('\n', ' ')}"
}
