package pravda.cmdopt

object CommandLine {

  sealed trait Verb[C, +A]

  final case class Head[C](
      name: String,
      text: String = "",
      desc: String = "",
      action: (String, C) => C = (_: String, c: C) => c,
      verbs: List[Verb[C, _]] = List.empty[Verb[C, _]]
    ) extends Verb[C, Nothing] {

    def children(xs: Verb[C, _]*): Verb[C, Nothing] = {
      copy(verbs = verbs ++ xs)
    }

    def text(msg: String): Head[C] = copy(text = msg)
    def desc(msg: String): Head[C] = copy(desc = msg)
    def action(f: (String, C) => C): Head[C] = copy(action = f)
  }

  final case class Cmd[C](
      name: String,
      text: String = "",
      desc: String = "",
      action: (String, C) => C = (_: String, c: C) => c,
      docref: String = "",
      verbs: List[Verb[C, _]] = List.empty[Verb[C, _]]
    ) extends Verb[C, Nothing] {

    def children(xs: Verb[C, _]*): Verb[C, Nothing] = {
      copy(verbs = verbs ++ xs)
    }

    def text(msg: String): Cmd[C] = copy(text = msg)
    def desc(msg: String): Cmd[C] = copy(desc = msg)
    def action(f: (String, C) => C): Cmd[C] = copy(action = f)
  }

  final case class Opt[C, A: Read](
      short: Char,
      name: String,
      text: String = "",
      desc: String = "",
      abbrs: List[String] = List.empty[String],
      action: (A, C) => C = (_: A, c: C) => c,
    ) extends Verb[C, A] {

    def read(line: Line, cfg: C): Either[String, (Line, C)] = {
      val reader = implicitly[Read[A]]
      reader.read(line) match {
        case Right((v, newLine)) =>
          val newCfg = action(v, cfg)
          Right((newLine, newCfg))
        case Left(msg) => Left(msg)
      }
    }

    def text(msg: String): Opt[C, A] = copy(text = msg)
    def desc(msg: String): Opt[C,A] = copy(desc = msg)
    def abbr(name: String): Opt[C, A] = copy(abbrs = abbrs :+ name)
    def action(f: (A, C) => C): Opt[C, A] = copy(action = f)
  }

  final case class Arg[C, A](
      name: String,
      text: String = "",
      desc: String = "",
      action: (A, C) => C = (_: A, c: C) => c,
    ) extends Verb[C, A] {

    def text(msg: String): Arg[C,A] = copy(text = msg)
    def desc(msg: String): Arg[C,A] = copy(desc = msg)
    def action(f: (A, C) => C): Arg[C,A] = copy(action = f)
  }

}

trait CommandLine[C] {

  import CommandLine._

  def model: List[Verb[C, _]]

  def head(name: String): Head[C] = Head[C](name)
  def cmd(name: String): Cmd[C] = Cmd[C](name)
  def opt[A: Read](name: String): Opt[C, A] = Opt[C, A]('\u0000', name)
  def opt[A: Read](short: Char, name: String): Opt[C, A] = Opt[C, A](short, name)
  def arg[A]: Arg[C,A] = Arg[C,A]("")

  def help[O](cmd: String = "")(implicit printer: Show[List[Verb[C,_]], O]): String = {
    if (cmd.isEmpty) {
      printer.show(model)
    } else {
      model.collect{ case x: Cmd[_] => x }
        .find(_.name == cmd)
        .map(x => printer.show(x.verbs))
        .getOrElse(s"Unknown command: ${cmd}")
    }
  }

  def parse(rawLine: Line, init: C): Either[String, C] = {
    val line = splitShortOptions(rawLine)
    def next(line: Line, m: List[Verb[C, _]], init: C): Either[String, C] = {
      val cmds = m.collect{ case x: Cmd[_] => (x.name, x) }.toMap
      // val args = m.collect{ case x: Arg[_, C] => (x.name, x) }.toMap[String, Arg[_, C]]
      val opts = m.collect{ case x: Opt[_, _] => (x.name, x) }.toMap[String, Opt[C, _]] ++
                 m.collect{ case x: Opt[_, _] if x.short != '\u0000' => (x.short.toString, x) }.toMap[String, Opt[C, _]] ++
                 m.collect{ case x: Opt[_, _] => x.abbrs.map{ n => (n, x) } }.flatten[(String, Opt[C, _])].toMap
      def parseArg(line: Line, cfg: C): Either[String, C] = {
        // TODO: Implement arguments parsing
        Left("Not implemented yet")
      }
      def parseCmd(line: Line, cfg: C): Either[String, C] = {
        if (line.isEmpty) {
          Right(cfg)
        } else {
          cmds.get(line.head).map{ cmd =>
            val newCfg = cmd.action(cmd.name, cfg)
            next(line.tail, cmd.verbs ++ opts.values, newCfg)
          } getOrElse parseArg(line, cfg)
        }
      }
      def parseOpt(line: Line, cfg: C): Either[String, C] = {
        if (line.isEmpty) {
          Right(cfg)
        } else if (line.head.startsWith("-")) {
          val name = line.head
          val short = if (name.startsWith("--")) name.drop(2) else name.drop(1)
          val shopt = opts.get(short)
          if (shopt.isEmpty) {
            parseArg(line, cfg)
          } else {
            shopt.map{ opt =>
              opt.read(line.tail, cfg) match {
                case Right((newLine, newCfg)) => parseOpt(newLine, newCfg)
                case Left(err) => Left(err)
              }
            }.getOrElse(Left("Assertion failure: Option should not be empty"))
          }
        } else {
          parseCmd(line, cfg)
        }
      }
      parseOpt(line, init)
    }
    next(line, model, init)
  }

  private def splitShortOptions(line: Line): Line = {
    line.flatMap{ x =>
      if (x.startsWith("-") && !x.startsWith("--")) {
        x.drop(1).map("-" + _).toSeq
      } else {
        Seq(x)
      }
    }
  }


}
