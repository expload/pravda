package pravda.cmdopt.printers

import pravda.cmdopt.CommandLine.{Cmd, Head, Opt}
import pravda.cmdopt.{CmdDocsPrinter, CommandLine}

object MarkdownCtx {
  val Default = MarkdownCtx("  ")
}
final case class MarkdownCtx(tab: String, lvl: Int = 0)

class MarkdownPrinter[C] extends CmdDocsPrinter[C, MarkdownCtx] {
  private val EOL = "\n"

  override def printCL(cl: CommandLine[C], ctx: MarkdownCtx = MarkdownCtx.Default): String = printVerbs(cl.model, ctx)

  override def printVerbs(verbs: List[CommandLine.Verb[C, _]], ctx: MarkdownCtx = MarkdownCtx.Default): String = {
    val opts = verbs.collect { case x: Opt[_, _] => x }
    val head = verbs.collect { case x: Head[_]   => x }
    val cmds = CommandLine.walk(List.empty[Cmd[C]], verbs)
    val header = head.headOption.map(h => s"${printVerb(h, ctx)}").getOrElse("")
    val optHead = s"## Options$EOL$EOL|Option|Description|$EOL|----|----|"
    val optBody = opts
      .map(x => printVerb(x, ctx))
      .mkString(EOL)
    val cmdHead = s"## Commands$EOL$EOL|Command|Docs|Description|$EOL|----|----|----|"
    val cmdBody = cmds
      .map { path =>
        val desc = path.reverse.head.text
        val comm = path.map(_.name).mkString("-")
        val link = s"[docs]($comm.md)"
        s"|`$comm`|$link|$desc|"
      }
      .mkString(EOL)
    val optAll = if (opts.nonEmpty) s"$optHead$EOL$optBody" else s"${EOL}No options available"
    val cmdAll = if (cmds.nonEmpty) s"$cmdHead$EOL$cmdBody" else ""
    List(header, optAll, cmdAll).mkString(EOL)
  }

  override def printVerb(verb: CommandLine.Verb[C, _], ctx: MarkdownCtx = MarkdownCtx.Default): String = verb match {
    case head: Head[C] =>
      val name = s"## Name$EOL${head.title}"
      val synopsys = s"## Synopsys$EOL```$EOL${head.text}$EOL```"
      val desc = s"## Description$EOL${head.desc}"
      List(name, synopsys, desc).mkString(s"$EOL$EOL")
    case cmd: Cmd[C] =>
      val pads = 20 - ctx.tab.length
      val hasSubcommands = cmd.verbs.collect { case x: Cmd[_] => x }.nonEmpty
      val body = printVerbs(cmd.verbs, MarkdownCtx(tab = ctx.tab + "  ", lvl = ctx.lvl + 1)) +
        (if (hasSubcommands) EOL else "")
      (if (hasSubcommands) EOL else "") + s"${ctx.tab}%-${pads}s${cmd.text}$EOL$body".format(cmd.name)
    case opt: Opt[C, _] =>
      s"|${opt.short.map(x => s"-$x, ").getOrElse("")}--${opt.name}|${opt.text}"
  }
}
