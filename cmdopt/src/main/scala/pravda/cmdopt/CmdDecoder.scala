package pravda.cmdopt

import java.io.File

trait CmdDecoder[T] {
  def decode(line: Line): Either[String, (T, Line)]
}

object CmdDecoder {
  private val noValueError = Left("Option must have value. No value provided")
  private def parseError(ex: NumberFormatException) =
    Left(s"Error parsing option value: ${ex.getClass}:${ex.getMessage}")

  implicit val intDecoder: CmdDecoder[Int] = (line: Line) => {
    line.headOption
      .map { item =>
        try {
          Right((Integer.parseInt(item), line.tail))
        } catch {
          case ex: NumberFormatException =>
            try {
              Right((Integer.parseInt(item.drop(2), 16), line.tail))
            } catch {
              case ex: NumberFormatException => parseError(ex)
            }
        }
      }
      .getOrElse(noValueError)
  }

  implicit val longReader: CmdDecoder[Long] = (line: Line) => {
    line.headOption
      .map { item =>
        try {
          Right((java.lang.Long.parseLong(item), line.tail))
        } catch {
          case ex: NumberFormatException =>
            try {
              Right((java.lang.Long.parseLong(item.drop(2), 16), line.tail))
            } catch {
              case ex: NumberFormatException => parseError(ex)
            }
        }
      }
      .getOrElse(noValueError)
  }

  implicit val stringReader: CmdDecoder[String] = (line: Line) => {
    line.headOption
      .map(item => Right((item, line.tail)))
      .getOrElse(noValueError)
  }

  implicit val fileReader: CmdDecoder[File] = (line: Line) => {
    line.headOption
      .map(item => Right((new java.io.File(item), line.tail)))
      .getOrElse(noValueError)
  }

  implicit val unitReader: CmdDecoder[Unit] = (line: Line) => Right(((), line))
}
