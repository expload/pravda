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
    val cmdHead = s"## Commands$EOL$EOL|Command|Docs|Description|$EOL|----|----|----|"
    val cmdBody = cmds
      .map { path =>
        val desc = path.text.replace('\n', ' ')
        val comm = path.cmds.map(_.name).mkString(" ")
        val link = s"[docs]($comm.md)"
        s"|`$comm`|$link|$desc|"
      }
      .mkString(EOL)
    val optAll = if (opts.nonEmpty) s"$optHead$EOL$optBody" else s"${EOL}No options available"
    val cmdAll = if (cmds.nonEmpty) s"$cmdHead$EOL$cmdBody" else ""
    List(printHeader(cmdPath), optAll, cmdAll).mkString(EOL)
  }

  private def printHeader[C](cmdPath: CmdPath[C]): String = {
    val dne = s"<!--${EOL}THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!$EOL-->"
    val usage = s"```${cmdPath.toUsageString}```"
    val desc = s"## Description$EOL${cmdPath.text}"
    List(dne, usage, desc).mkString(s"$EOL$EOL")
  }

  private def printOpt[C](opt: CommandLine.Opt[C, _], ctx: MarkdownCtx): String =
    s"|${opt.short.map(x => s"`-$x`, ").getOrElse("")}`--${opt.name}`|${opt.text.replace('\n', ' ')}"
}
