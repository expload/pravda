package pravda.cmdopt.printers

import pravda.cmdopt.CommandLine.{Cmd, CmdPath}

object UsagePrinter {

  def printPath[C](cmdPath: CmdPath[C]): String = {
    val path = (cmdPath.head.name +: cmdPath.cmds.map(_.name)).mkString(" ")
    val opts = {
      val rawOpts = cmdPath.opts.map(opt => s"--${opt.name}${opt.paramInfo.fold("")(info => s" $info")}")
      if (rawOpts.isEmpty) {
        ""
      } else {
        s" ${rawOpts.mkString(" ")}"
      }
    }
    val cmds = {
      val rawCmds = cmdPath.verbs.collect { case c: Cmd[C] => c }.map(_.name)
      if (rawCmds.isEmpty) {
        ""
      } else {
        s" [${rawCmds.mkString("|")}]"
      }
    }

    s"$path$opts$cmds"
  }
}
