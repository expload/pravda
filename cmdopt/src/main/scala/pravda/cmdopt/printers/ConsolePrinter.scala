package pravda.cmdopt.printers

import pravda.cmdopt.CommandLine.{Cmd, Head, Opt}
import pravda.cmdopt.{CmdDocsPrinter, CommandLine}

import scala.collection.mutable.ListBuffer

object ConsoleCtx {
  val Default = ConsoleCtx("  ")
}
final case class ConsoleCtx(tab: String, lvl: Int = 0)

class ConsolePrinter[C] extends CmdDocsPrinter[C, ConsoleCtx] {
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

  override def printCL(cl: CommandLine[C], ctx: ConsoleCtx = ConsoleCtx.Default): String =
    printVerbs(cl.model, ctx).replace(s"$EOL$EOL$EOL", s"$EOL$EOL")

  override def printVerbs(verbs: List[CommandLine.Verb[C, _]], ctx: ConsoleCtx): String = {
    lazy val cmds = verbs.collect { case x: Cmd[_]    => x }
    lazy val opts = verbs.collect { case x: Opt[_, _] => x }
    lazy val head = verbs.collect { case x: Head[_]   => x }
    lazy val headP = head.map(printVerb(_, ctx)).mkString
    lazy val cmdP = cmds.map(printVerb(_, ctx)).mkString
    lazy val optP = opts.map(printVerb(_, ctx)).mkString(EOL)
    if (ctx.lvl == 0) {
      val optText = if (opts.isEmpty) "" else s"Options:$EOL$optP$EOL$EOL"
      val cmdText = if (cmds.isEmpty) "" else s"Commands:$EOL$cmdP"
      s"$headP$EOL$EOL$optText$cmdText"
    } else {
      s"$cmdP"
    }
  }

  override def printVerb(verb: CommandLine.Verb[C, _], ctx: ConsoleCtx): String = verb match {
    case head: Head[C] =>
      s"${head.text}$EOL$EOL${head.desc}"
    case cmd: Cmd[C] =>
      val pads = 20 - ctx.tab.length
      val hasSubcommands = cmd.verbs.collect { case x: Cmd[_] => x }.nonEmpty
      val body = printVerbs(cmd.verbs, ConsoleCtx(tab = ctx.tab + "  ", lvl = ctx.lvl + 1)) +
        (if (hasSubcommands) EOL else "")
      val textLines = wrapWordsToLines(cmd.text.split("\\s+").toList, 80 - 20)
      val text = (textLines.head :: textLines.tail.map(" " * 20 + _)).mkString("\n")
      (if (hasSubcommands) EOL else "") + s"${ctx.tab}%-${pads}s$text$EOL$body".format(cmd.name)
    case opt: Opt[C, _] =>
      val pads = 20 - ctx.tab.length
      s"${ctx.tab}%-${pads}s${opt.text}".format(s"${opt.short.map(x => s"-$x").getOrElse("")}, --${opt.name}")
  }
}
