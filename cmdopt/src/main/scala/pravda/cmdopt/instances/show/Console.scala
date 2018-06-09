package pravda.cmdopt
package instances.show

package console {
  final case class ShowOpt(tab: String, lvl: Int = 0)
  case object ShowOpt {
    val Default = ShowOpt("  ")
  }
  private[console] trait ConsoleShow[A] extends Show[A, ShowOpt] {
    def show(a: A) = show(a, ShowOpt.Default)
  }
}

package object console {

  import CommandLine._

  private[console] val EOL = sys.props("line.separator")
  private[console] def fshow[A](a: A, o: ShowOpt)(implicit s: Show[A, ShowOpt]): String = s.show(a, o)

  implicit def clShow[C]: ConsoleShow[CommandLine[C]] = (cl, o) => {
    fshow(cl.model, o)
  }

  implicit def clVerbListShow[C]: ConsoleShow[List[Verb[C, _]]] = (list, o) => {
    val cmds = list.collect{ case x: Cmd[_] => x }
    val opts = list.collect{ case x: Opt[_,_] => x }
    val head = list.collect{ case x: Head[_] => x }
    val headP = head.map(fshow(_, o)).mkString
    val cmdP = cmds.map(fshow(_, o)).mkString
    val optP = opts.map(fshow(_, o)).mkString(EOL)
    if (o.lvl == 0) {
      f"${headP}${EOL}${EOL}Options:${EOL}${optP}${EOL}${EOL}Commands:${EOL}${cmdP}"
    } else {
      f"${cmdP}"
    }
  }

  implicit def clCmdShow[C]: ConsoleShow[Cmd[C]] = (cmd, o) => {
    val pads = 20 - o.tab.size
    val body = fshow(cmd.verbs, ShowOpt(tab = o.tab + "  ", lvl = o.lvl + 1))
    s"${o.tab}%-${pads}s${cmd.text}${EOL}${body}".format(cmd.name)
  }

  implicit def clHeadShow[C]: ConsoleShow[Head[C]] = (head, o) => {
    s"${head.text}${EOL}${EOL}${head.desc}"
  }

  implicit def clOptShow[C,A]: ConsoleShow[Opt[C, A]] = (opt, o) => {
    val pads = 20 - o.tab.size
    s"${o.tab}%-${pads}s${opt.text}".format(s"-${opt.short}, --${opt.name}")
  }

}
