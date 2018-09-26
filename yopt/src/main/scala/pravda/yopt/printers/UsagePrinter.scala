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

import pravda.yopt.CommandLine.{Cmd, CmdPath}

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
