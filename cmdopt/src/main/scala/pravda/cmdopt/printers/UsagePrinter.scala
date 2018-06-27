package pravda.cmdopt.printers

import pravda.cmdopt.CommandLine.CmdPath

object UsagePrinter {
  def printPath[C](cmdPath: CmdPath[C]): String =
    (cmdPath.head.name +: cmdPath.cmds.map(_.name)).mkString(" ")
}
