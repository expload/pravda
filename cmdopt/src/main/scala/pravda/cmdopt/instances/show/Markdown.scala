package pravda.cmdopt
package instances.show

package markdown {
  final case class ShowOpt(tab: String, lvl: Int = 0)
  case object ShowOpt {
    val Default = ShowOpt("  ")
  }
  private[markdown] trait MarkdownShow[A] extends Show[A, ShowOpt] {
    def show(a: A) = show(a, ShowOpt.Default)
  }
}

package object markdown {

  import CommandLine._

  def fshow[A](a: A, o: ShowOpt = ShowOpt.Default)(implicit s: Show[A, ShowOpt]): String = s.show(a, o)

  private[markdown] val EOL = "\n"

  implicit def clShow[C]: MarkdownShow[CommandLine[C]] = (cl, o) => {
    fshow(cl.model, o)
  }

  implicit def clVerbListShow[C]: MarkdownShow[List[Verb[C, _]]] = (list, o) => {
    val opts = list.collect{ case x: Opt[_,_] => x }
    val head = list.collect{ case x: Head[_] => x }
    val cmdHead = head.headOption.map(h => s"${fshow(h, o)}").getOrElse("")
    val optHead = s"## Options${EOL}${EOL}|Option|Description|${EOL}|----|----|"
    val optBody = opts.map{ x => fshow(x, o) }.mkString(EOL)
    val tblHead = s"## Commands${EOL}${EOL}|Command|Docs|Description|${EOL}|----|----|----|"
    val tblBody = CommandLine.walk(List.empty[Cmd[C]], list).map{ path =>
      val desc = path.reverse.head.text
      val comm = path.map(_.name).mkString("-")
      val link = s"[docs](${comm}.md)"
      s"|`${comm}`|${link}|${desc}|"
    }.mkString(EOL)
    List(cmdHead, optHead, optBody, tblHead, tblBody).mkString(EOL)
  }

  implicit def clCmdShow[C]: MarkdownShow[Cmd[C]] = (cmd, o) => {
    val pads = 20 - o.tab.size
    val hasSubcommands = !cmd.verbs.collect{ case x: Cmd[_] => x }.isEmpty
    val body = fshow(cmd.verbs, ShowOpt(tab = o.tab + "  ", lvl = o.lvl + 1)) +
      (if (hasSubcommands) EOL else "")
    (if (hasSubcommands) EOL else "") + s"${o.tab}%-${pads}s${cmd.text}${EOL}${body}".format(cmd.name)
  }

  implicit def clHeadShow[C]: MarkdownShow[Head[C]] = (head, o) => {
    val name = s"## Name${EOL}${head.title}"
    val synopsys = s"## Synopsys${EOL}```${EOL}${head.text}${EOL}```"
    val desc = s"## Description${EOL}${head.desc}"
    List(name, synopsys, desc).mkString(s"${EOL}${EOL}")
  }

  implicit def clOptShow[C,A]: MarkdownShow[Opt[C, A]] = (opt, o) => {
    s"|-${opt.short}, --${opt.name}|${opt.text}"
  }

}
