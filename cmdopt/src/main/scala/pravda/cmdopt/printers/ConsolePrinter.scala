package pravda.cmdopt.printers

import pravda.cmdopt.CommandLine
import pravda.cmdopt.CommandLine.{Cmd, CmdPath}

import scala.collection.mutable.ListBuffer

object ConsolePrinter {
  object ConsoleCtx {
    val Default = ConsoleCtx("  ")
  }
  final case class ConsoleCtx(tab: String, lvl: Int = 0)

  private val EOL = sys.props("line.separator")

  private def wrapWordsToLines(words: List[String], maxWidth: Int): List[String] = {
    val res = ListBuffer[List[String]]()
    val buffer = ListBuffer[String]()
    var curWidth = 0

    words.foreach(word => {
      if (curWidth + word.length + 1 > maxWidth) {
        res += buffer.toList
        buffer.clear()
        curWidth = -1
      }
      buffer += word
      curWidth += word.length + 1
    })

    res += buffer.toList
    res.toList.map(_.mkString(" "))
  }

  def printPath[C](cmdPath: CmdPath[C], ctx: ConsoleCtx = ConsoleCtx.Default): String = {
    val cmds = cmdPath.verbs.collect { case x: Cmd[C]    => x }
    val opts = cmdPath.opts
    val cmdP = cmds.map(printCmd(_, ctx)).mkString
    val optP = opts.map(printOpt(_, ctx)).mkString(EOL)

    val usageText = cmdPath.toUsageString
    val optText = if (opts.isEmpty) "" else s"Options:$EOL$optP$EOL"
    val cmdText = if (cmds.isEmpty) "" else s"Commands:$EOL$cmdP"
    s"$usageText$EOL$EOL${cmdPath.text}$EOL$EOL$optText$cmdText"
  }

  private def printCmd[C](cmd: CommandLine.Cmd[C], ctx: ConsoleCtx): String = {
    val pads = 20 - ctx.tab.length
    val hasSubcommands = cmd.verbs.collect { case x: Cmd[_] => x }.nonEmpty
    val body = printNestedCmds(cmd.verbs, ConsoleCtx(tab = ctx.tab + "  ", lvl = ctx.lvl + 1)) +
      (if (hasSubcommands) "" else "")
    val textLines = wrapWordsToLines(cmd.text.split("\\s+").toList, 80 - 20)
    val text = (textLines.head :: textLines.tail.map(" " * 20 + _)).mkString("\n")
    (if (hasSubcommands) "" else "") + s"${ctx.tab}%-${pads}s$text$EOL$body".format(cmd.name)
  }

  private def printNestedCmds[C](verbs: List[CommandLine.Verb[C]], ctx: ConsoleCtx): String =
    verbs.collect { case x: Cmd[C] => x }.map(printCmd(_, ctx)).mkString

  private def printOpt[C](opt: CommandLine.Opt[C, _], ctx: ConsoleCtx): String = {
    val pads = 20 - ctx.tab.length
    s"${ctx.tab}%-${pads}s${opt.text}".format(s"${opt.short.map(x => s"-$x, ").getOrElse("")}--${opt.name}")
  }
}
