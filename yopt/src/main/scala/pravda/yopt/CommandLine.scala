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

package pravda.yopt

import pravda.yopt.printers.{ConsolePrinter, MarkdownPrinter, UsagePrinter}

object CommandLine {

  final case class Head[C](
      name: String,
      text: String = "",
      verbs: List[Verb[C]] = List.empty[Verb[C]]
  ) {
    def children(xs: Verb[C]*): Head[C] = copy(verbs = verbs ++ xs)

    def text(msg: String): Head[C] = copy(text = msg)
  }

  sealed trait Verb[C]

  final case class Cmd[C](
      name: String,
      text: String = "",
      actionF: C => C = (c: C) => c,
      verbs: List[Verb[C]] = List.empty[Verb[C]]
  ) extends Verb[C] {

    def children(xs: Verb[C]*): Cmd[C] = {
      copy(verbs = verbs ++ xs)
    }

    def text(msg: String): Cmd[C] = copy(text = msg)
    def action(f: C => C): Cmd[C] = copy(actionF = f)
  }

  final case class Opt[C, A: CmdDecoder](
      short: Option[Char],
      name: String,
      text: String = "",
      actionF: (A, C) => C = (_: A, c: C) => c,
      validateF: A => Either[String, Unit] = (_: Any) => Right(()),
  ) extends Verb[C] {

    def paramInfo: Option[String] = implicitly[CmdDecoder[A]].optInfo

    def decodeLine(line: Line, cfg: C): Either[String, (Line, C)] = {
      val decoder = implicitly[CmdDecoder[A]]
      decoder.decode(line) map {
        case (v, newLine) =>
          val newCfg = actionF(v, cfg)
          (newLine, newCfg)
      }
    }

    def text(msg: String): Opt[C, A] = copy(text = msg)
    def action(f: (A, C) => C): Opt[C, A] = copy(actionF = f)
    def validate(f: A => Either[String, Unit]): Opt[C, A] = copy(validateF = f)
  }

  final case class CmdPath[C](head: Head[C], cmds: Vector[Cmd[C]]) {

    lazy val verbs: List[Verb[C]] =
      if (cmds.isEmpty) {
        head.verbs
      } else {
        cmds.last.verbs
      }

    lazy val text: String =
      if (cmds.isEmpty) {
        head.text
      } else {
        cmds.last.text
      }

    lazy val opts: List[Opt[C, _]] = // FIXME can be constructed faster in advance and apply
      head.verbs.collect { case o: Opt[C, _] => o } ++ cmds.flatMap(_.verbs.collect { case o: Opt[C, _] => o })

    def advance(cmd: Cmd[C]): CmdPath[C] = copy(cmds = cmds :+ cmd)

    override def toString: String = (head.name +: cmds.map(_.name)).mkString("-")
    def toUsageString: String = UsagePrinter.printPath(this)

    def toHelpString: String = ConsolePrinter.printPath(this)
    def toMarkdown: String = MarkdownPrinter.printPath(this)
  }

  object CmdPath {
    def apply[C](head: Head[C], cmds: Vector[Cmd[C]]): CmdPath[C] = new CmdPath(head, cmds)
    def apply[C](head: Head[C]): CmdPath[C] = new CmdPath(head, Vector.empty)

    /*
      Returns only "leaf" paths, e.g. such paths that don't have any children
     */
    def walk[C](path: CmdPath[C]): List[CmdPath[C]] = {
      path.verbs.collect { case x: Cmd[C] => x }.flatMap { cmd =>
        val newPath = path.advance(cmd)
        val list = walk(newPath)
        if (list.isEmpty) {
          List(newPath)
        } else {
          list
        }
      }
    }
  }

  sealed trait Result[+C]
  final case class Ok[C](config: C)                   extends Result[C]
  final case class ParseError(msg: String)            extends Result[Nothing]
  final case class HelpNeeded[C](cmdPath: CmdPath[C]) extends Result[C]

  val helpOpts = List("-h", "--help")
}

trait CommandLineBuilder[C] {
  import CommandLine._

  def head(name: String): Head[C] = Head[C](name)
  def cmd(name: String): Cmd[C] = Cmd[C](name)
  def opt[A: CmdDecoder](name: String): Opt[C, A] = Opt[C, A](Option.empty[Char], name)
  def opt[A: CmdDecoder](short: Char, name: String): Opt[C, A] = Opt[C, A](Some(short), name)
}

abstract class CommandLine[C] extends CommandLineBuilder[C] {

  import CommandLine._

  def model: Head[C]
  def root: CmdPath[C] = CmdPath(model)

  def paths: List[CmdPath[C]] = CmdPath.walk(CmdPath(model))

  def parse(rawLine: Line, init: C): Result[C] = {
    val line = splitShortOptions(rawLine)
    def next(line: Line, path: CmdPath[C], cfg: C): Result[C] = {
      val cmds: Map[String, Cmd[C]] = path.verbs.collect { case x: Cmd[C] => (x.name, x) }.toMap

      val opts: Map[String, Opt[C, _]] = (
        path.opts.map(o => (o.name, o)) ++
          path.opts.collect { case x: Opt[C, _] if x.short.nonEmpty => (x.short.get.toString, x) }
      ).toMap

      def parseCmd(line: Line, cfg: C): Result[C] = {
        if (line.isEmpty) {
          Ok[C](cfg)
        } else {
          cmds
            .get(line.head)
            .map { cmd =>
              val newCfg = cmd.actionF(cfg)
              next(line.tail, path.advance(cmd), newCfg)
            }
            .getOrElse(ParseError(s"Wrong command: ${line.head}"))
        }
      }

      def parseOpt(line: Line, cfg: C): Result[C] = {
        if (line.isEmpty) {
          Ok[C](cfg)
        } else if (helpOpts.contains(line.head)) {
          HelpNeeded[C](path)
        } else if (line.head.startsWith("-")) {
          val name = line.head
          val shortName = if (name.startsWith("--")) name.drop(2) else name.drop(1)
          val shopt = opts.get(shortName)
          if (shopt.isEmpty) {
            ParseError(s"Unknown option: $name")
          } else {
            shopt.map { opt =>
              opt.decodeLine(line.tail, cfg) match {
                case Right((newLine, newCfg)) => parseOpt(newLine, newCfg)
                case Left(err)                => ParseError(err)
              }
            }.get
          }
        } else {
          parseCmd(line, cfg)
        }
      }

      parseOpt(line, cfg)
    }
    next(line, root, init)
  }

  private def splitShortOptions(line: Line): Line = {
    line.flatMap { x =>
      if (x.startsWith("-") && !x.startsWith("--")) {
        x.drop(1).map("-" + _)
      } else {
        List(x)
      }
    }
  }
}
