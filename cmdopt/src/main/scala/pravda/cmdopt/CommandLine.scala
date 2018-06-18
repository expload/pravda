package pravda.cmdopt

object CommandLine {

  sealed trait Verb[C, +A]

  final case class Head[C](
      name: String,
      text: String = "",
      desc: String = "",
      title: String = "",
      action: (Any, C) => C = (_: Any, c: C) => c,
      validate: Any => Either[String, Any] = (_: Any) => Right(()),
      verbs: List[Verb[C, _]] = List.empty[Verb[C, _]]
  ) extends Verb[C, Nothing] {

    def children(xs: Verb[C, _]*): Verb[C, Nothing] = {
      copy(verbs = verbs ++ xs)
    }

    def text(msg: String): Head[C] = copy(text = msg)
    def desc(msg: String): Head[C] = copy(desc = msg.stripMargin)
    def title(msg: String): Head[C] = copy(title = msg)
    def action(f: (Any, C) => C): Head[C] = copy(action = f)
    def validate(f: Any => Either[String, Any]): Head[C] = copy(validate = f)
  }

  final case class Cmd[C](
      name: String,
      text: String = "",
      desc: String = "",
      action: (Any, C) => C = (_: Any, c: C) => c,
      validate: Any => Either[String, Any] = (_: Any) => Right(()),
      docref: String = "",
      verbs: List[Verb[C, _]] = List.empty[Verb[C, _]]
  ) extends Verb[C, Nothing] {

    def children(xs: Verb[C, _]*): Verb[C, Nothing] = {
      copy(verbs = verbs ++ xs)
    }

    def text(msg: String): Cmd[C] = copy(text = msg)
    def desc(msg: String): Cmd[C] = copy(desc = msg)
    def action(f: (Any, C) => C): Cmd[C] = copy(action = f)
    def validate(f: Any => Either[String, Any]): Cmd[C] = copy(validate = f)
  }

  final case class Opt[C, A: Read](
      short: Char,
      name: String,
      text: String = "",
      desc: String = "",
      abbrs: List[String] = List.empty[String],
      action: (A, C) => C = (_: A, c: C) => c,
      validate: A => Either[String, Unit] = (_: Any) => Right(()),
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
    def desc(msg: String): Opt[C, A] = copy(desc = msg)
    def abbr(name: String): Opt[C, A] = copy(abbrs = abbrs :+ name)
    def action(f: (A, C) => C): Opt[C, A] = copy(action = f)
    def validate(f: A => Either[String, Unit]): Opt[C, A] = copy(validate = f)
  }

  final case class Arg[C, A: Read](
      name: String,
      text: String = "",
      desc: String = "",
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

    def text(msg: String): Arg[C, A] = copy(text = msg)
    def desc(msg: String): Arg[C, A] = copy(desc = msg)
    def action(f: (A, C) => C): Arg[C, A] = copy(action = f)
  }

  sealed trait Result[+C]
  final case class Ok[C](config: C)                  extends Result[C]
  final case class ParseError(msg: String)           extends Result[Nothing]
  final case class HelpWanted[C](cl: CommandLine[C]) extends Result[Nothing]

  def walk[C](path: List[Cmd[C]], verbs: List[Verb[C, _]]): List[List[Cmd[C]]] = {
    verbs.collect { case x: Cmd[_] => x }.flatMap { cmd =>
      val newPath = path :+ cmd
      val list = walk(newPath, cmd.verbs)
      if (list.isEmpty) {
        List(newPath)
      } else {
        list.map(path ++ _)
      }
    }
  }
}

trait CommandLine[C] {

  import CommandLine._

  def model: List[Verb[C, _]]
  def helpOpts = List("-h", "--help", "/?")

  def head(name: String): Head[C] = Head[C](name)
  def cmd(name: String): Cmd[C] = Cmd[C](name)
  def opt[A: Read](name: String): Opt[C, A] = Opt[C, A]('\u0000', name)
  def opt[A: Read](short: Char, name: String): Opt[C, A] = Opt[C, A](short, name)
  def arg[A: Read]: Arg[C, A] = Arg[C, A]("")

  def help[O](cmd: String = "")(implicit clP: Show[CommandLine[C], O], verbsP: Show[List[Verb[C, _]], O]): String = {
    if (cmd.isEmpty) {
      clP.show(this)
    } else {
      model
        .collect { case x: Cmd[_] => x }
        .find(_.name == cmd)
        .map(x => verbsP.show(x.verbs))
        .getOrElse(s"Unknown command: ${cmd}")
    }
  }

  def paths(): List[List[Cmd[C]]] = {
    walk(List.empty[Cmd[C]], model)
  }

  def parse(rawLine: Line, init: C): Result[C] = {
    val line = splitShortOptions(rawLine)
    def next(line: Line, m: List[Verb[C, _]], init: C): Result[C] = {
      val cmds = m.collect { case x: Cmd[_]    => (x.name, x) }.toMap
      val args = m.collect { case x: Arg[_, _] => x }.toList
      val opts = m.collect { case x: Opt[_, _]               => (x.name, x) }.toMap[String, Opt[C, _]] ++
        m.collect { case x: Opt[_, _] if x.short != '\u0000' => (x.short.toString, x) }.toMap[String, Opt[C, _]] ++
        m.collect {
            case x: Opt[_, _] =>
              x.abbrs.map { n =>
                (n, x)
              }
          }
          .flatten[(String, Opt[C, _])]
          .toMap
      def parseArg(line: Line, args: List[Arg[C, _]], cfg: C): Result[C] = {
        if (args.isEmpty) {
          ParseError(s"Uknown argument: ${line.head}")
        } else {
          val arg: Arg[C, _] = args.head
          arg.read(line, cfg) match {
            case Right((newLine, newCfg)) => {
              if (newLine.isEmpty) {
                Ok(newCfg)
              } else {
                parseArg(newLine, args.tail, newCfg)
              }
            }
            case Left(err) => ParseError(err)
          }
        }
      }
      def parseCmd(line: Line, cfg: C): Result[C] = {
        if (line.isEmpty) {
          Ok[C](cfg)
        } else {
          cmds.get(line.head).map { cmd =>
            val newCfg = cmd.action(cmd.name, cfg)
            val passOpts = opts.values.toSet
            next(line.tail, cmd.verbs ++ passOpts, newCfg)
          } getOrElse parseArg(line, args, cfg)
        }
      }
      def parseOpt(line: Line, cfg: C): Result[C] = {
        if (line.isEmpty) {
          Ok[C](cfg)
        } else if (!helpOpts.find(_ == line.head).isEmpty) {
          HelpWanted[C](new CommandLine[C] { def model = m })
        } else if (line.head.startsWith("-")) {
          val name = line.head
          val short = if (name.startsWith("--")) name.drop(2) else name.drop(1)
          val shopt = opts.get(short)
          if (shopt.isEmpty) {
            ParseError(s"Uknown option: ${name}")
          } else {
            shopt
              .map { opt =>
                opt.read(line.tail, cfg) match {
                  case Right((newLine, newCfg)) => parseOpt(newLine, newCfg)
                  case Left(err)                => ParseError(err)
                }
              }
              .getOrElse(ParseError("Assertion failure: Option should not be empty"))
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
    line.flatMap { x =>
      if (x.startsWith("-") && !x.startsWith("--")) {
        x.drop(1).map("-" + _).toSeq
      } else {
        Seq(x)
      }
    }
  }
}
