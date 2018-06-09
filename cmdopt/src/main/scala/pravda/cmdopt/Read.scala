package pravda.cmdopt

trait Read[T] {
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
