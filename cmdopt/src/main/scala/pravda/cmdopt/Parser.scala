package pravda.cmdopt

trait CommandLine[C] {

  type Line = Seq[String]

  sealed trait Read[T] {
    def read(line: Line): Either[String, (T, Line)]
  }

  object Read {
    implicit val intReader = new Read[Int] {
      def read(line: Line): Either[String, (Int, Line)] = {
        line.headOption.map{ item =>
          try {
            Right((Integer.parseInt(item), line.tail))
          } catch {
            case ex: NumberFormatException => try {
              Right((Integer.parseInt(item.drop(2), 16), line.tail))
            } catch {
              case ex: NumberFormatException => Left(s"Error parsing option value: ${ex.getClass}:${ex.getMessage}")
            }
          }
        } getOrElse(Left(s"Option must have value. No value provided"))
      }
    }
    implicit val stringReader = new Read[String] {
      def read(line: Line): Either[String, (String, Line)] = {
        line.headOption.map{ item =>
          Right((item, line.tail))
        } getOrElse(Left(s"Option must have value. No value provided"))
      }
    }
    implicit val fileReader = new Read[java.io.File] {
      def read(line: Line): Either[String, (java.io.File, Line)] = {
        line.headOption.map{ item =>
          Right((new java.io.File(item), line.tail))
        } getOrElse(Left(s"Option must have value. No value provided"))
      }
    }
    implicit val unitReader = new Read[Unit] {
      def read(line: Line): Either[String, (Unit, Line)] = Right(((), line))
    }
  }

  sealed trait Verb[+A]

  case class Cmd(
      name: String,
      text: String = "",
      action: (String, C) => C = (_: String, c: C) => c,
      docref: String = "",
      verbs: List[Verb[_]] = List.empty[Verb[_]]
    ) extends Verb[Nothing] {

    def children(xs: Verb[_]*): Verb[Nothing] = {
      copy(verbs = verbs ++ xs)
    }

    def text(msg: String): Cmd = copy(text = msg)
    def action(f: (String, C) => C): Cmd = copy(action = f)
  }

  case class Opt[T: Read](
      short: Char,
      name: String,
      text: String = "",
      abbrs: List[String] = List.empty[String],
      action: (T, C) => C = (_: T, c: C) => c,
    ) extends Verb[T] {

    def read(line: Line, cfg: C): Either[String, (Line, C)] = {
      val reader = implicitly[Read[T]]
      reader.read(line) match {
        case Right((v, newLine)) =>
          val newCfg = action(v, cfg)
          Right((newLine, newCfg))
        case Left(msg) => Left(msg)
      }
    }

    def text(msg: String): Opt[T] = copy(text = msg)
    def abbr(name: String): Opt[T] = copy(abbrs = abbrs :+ name)
    def action(f: (T, C) => C): Opt[T] = copy(action = f)
  }

  case class Arg[T](
      name: String,
      text: String = "",
      action: (T, C) => C = (_: T, c: C) => c,
    ) extends Verb[T] {

    def text(msg: String): Arg[T] = copy(text = msg)
    def action(f: (T, C) => C): Arg[T] = copy(action = f)
  }

  def model: List[Verb[_]]
  def cmd(name: String): Cmd = Cmd(name)
  def opt[T: Read](name: String): Opt[T] = Opt[T]('\u0000', name)
  def opt[T: Read](short: Char, name: String): Opt[T] = Opt[T](short, name)
  def arg[T]: Arg[T] = Arg[T]("")

  def parse(rawLine: Line, init: C): Either[String, C] = {
    val line = splitShortOptions(rawLine)
    def next(line: Line, m: List[Verb[_]], init: C): Either[String, C] = {
      val cmds = m.collect{ case x: Cmd => (x.name, x) }.toMap
      // val args = m.collect{ case x: Arg[_, C] => (x.name, x) }.toMap[String, Arg[_, C]]
      val opts = m.collect{ case x: Opt[_] => (x.name, x) }.toMap[String, Opt[_]] ++
                 m.collect{ case x: Opt[_] if x.short != '\u0000' => (x.short.toString, x) }.toMap[String, Opt[_]] ++
                 m.collect{ case x: Opt[_] => x.abbrs.map{ n => (n, x) } }.flatten[(String, Opt[_])].toMap
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
