package pravda.cmdopt

object CommandLine {

  sealed trait Verb[C, +A]

  final case class Head[C](
      name: String,
      text: String = "",
      desc: String = "",
      title: String = "",
      verbs: List[Verb[C, _]] = List.empty[Verb[C, _]]
  ) extends Verb[C, Nothing] {

    def children(xs: Verb[C, _]*): Verb[C, Nothing] = {
      copy(verbs = verbs ++ xs)
    }

    def text(msg: String): Head[C] = copy(text = msg)
    def desc(msg: String): Head[C] = copy(desc = msg.stripMargin)
    def title(msg: String): Head[C] = copy(title = msg)
  }

  final case class Cmd[C](
      name: String,
      text: String = "",
      desc: String = "",
      action: C => C = (c: C) => c,
      docref: String = "",
      verbs: List[Verb[C, _]] = List.empty[Verb[C, _]]
  ) extends Verb[C, Nothing] {

    def children(xs: Verb[C, _]*): Verb[C, Nothing] = {
      copy(verbs = verbs ++ xs)
    }

    def text(msg: String): Cmd[C] = copy(text = msg)
    def desc(msg: String): Cmd[C] = copy(desc = msg)
    def action(f: C => C): Cmd[C] = copy(action = f)
  }

  final case class Opt[C, A: CmdDecoder](
      short: Option[Char],
      name: String,
      text: String = "",
      desc: String = "",
      abbrs: List[String] = List.empty[String],
      action: (A, C) => C = (_: A, c: C) => c,
      validate: A => Either[String, Unit] = (_: Any) => Right(()),
  ) extends Verb[C, A] {

    def read(line: Line, cfg: C): Either[String, (Line, C)] = {
      val reader = implicitly[CmdDecoder[A]]
      reader.read(line) map {
        case ((v, newLine)) =>
          val newCfg = action(v, cfg)
          (newLine, newCfg)
      }
    }

    def text(msg: String): Opt[C, A] = copy(text = msg)
    def desc(msg: String): Opt[C, A] = copy(desc = msg)
    def abbr(name: String): Opt[C, A] = copy(abbrs = abbrs :+ name)
    def action(f: (A, C) => C): Opt[C, A] = copy(action = f)
    def validate(f: A => Either[String, Unit]): Opt[C, A] = copy(validate = f)
  }

  sealed trait Result[+C]
  final case class Ok[C](config: C)                  extends Result[C]
  final case class ParseError(msg: String)           extends Result[Nothing]
  final case class HelpNeeded[C](cl: CommandLine[C]) extends Result[C]

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
  def opt[A: CmdDecoder](name: String): Opt[C, A] = Opt[C, A](Option.empty[Char], name)
  def opt[A: CmdDecoder](short: Char, name: String): Opt[C, A] = Opt[C, A](Option(short), name)

  def paths: List[List[Cmd[C]]] = {
    walk(List.empty[Cmd[C]], model)
  }

  def parse(rawLine: Line, init: C): Result[C] = {
    val line = splitShortOptions(rawLine)
    def next(line: Line, m: List[Verb[C, _]], init: C): Result[C] = {
      val cmds = m.collect { case x: Cmd[_] => (x.name, x) }.toMap
      val opts = m.collect { case x: Opt[_, _]            => (x.name, x) }.toMap[String, Opt[C, _]] ++
        m.collect { case x: Opt[_, _] if x.short.nonEmpty => (x.short.get.toString, x) }.toMap[String, Opt[C, _]] ++
        m.collect {
            case x: Opt[_, _] =>
              x.abbrs.map { n =>
                (n, x)
              }
          }
          .flatten[(String, Opt[C, _])]
          .toMap
      def parseCmd(line: Line, cfg: C): Result[C] = {
        if (line.isEmpty) {
          Ok[C](cfg)
        } else {
          cmds
            .get(line.head)
            .map { cmd =>
              val newCfg = cmd.action(cfg)
              val passOpts = opts.values.toSet
              next(line.tail, cmd.verbs ++ passOpts, newCfg)
            }
            .getOrElse(ParseError(s"Wrong command: ${line.head}"))
        }
      }
      def parseOpt(line: Line, cfg: C): Result[C] = {
        if (line.isEmpty) {
          Ok[C](cfg)
        } else if (helpOpts.contains(line.head)) {
          HelpNeeded[C](new CommandLine[C] { def model = m })
        } else if (line.head.startsWith("-")) {
          val name = line.head
          val shortName = if (name.startsWith("--")) name.drop(2) else name.drop(1)
          val shopt = opts.get(shortName)
          if (shopt.isEmpty) {
            ParseError(s"Unknown option: $name")
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
