package pravda.cmdopt

import java.io.File

trait Read[T] {
  def read(line: Line): Either[String, (T, Line)]
}

object Read {
  implicit val intReader: Read[Int] = new Read[Int] {

    def read(line: Line): Either[String, (Int, Line)] = {
      line.headOption.map { item =>
        try {
          Right((Integer.parseInt(item), line.tail))
        } catch {
          case ex: NumberFormatException =>
            try {
              Right((Integer.parseInt(item.drop(2), 16), line.tail))
            } catch {
              case ex: NumberFormatException => Left(s"Error parsing option value: ${ex.getClass}:${ex.getMessage}")
            }
        }
      } getOrElse (Left(s"Option must have value. No value provided"))
    }
  }

  implicit val longReader: Read[Long] = new Read[Long] {

    def read(line: Line): Either[String, (Long, Line)] = {
      line.headOption.map { item =>
        try {
          Right((java.lang.Long.parseLong(item), line.tail))
        } catch {
          case ex: NumberFormatException =>
            try {
              Right((java.lang.Long.parseLong(item.drop(2), 16), line.tail))
            } catch {
              case ex: NumberFormatException => Left(s"Error parsing option value: ${ex.getClass}:${ex.getMessage}")
            }
        }
      } getOrElse (Left(s"Option must have value. No value provided"))
    }
  }

  implicit val stringReader: Read[String] = new Read[String] {

    def read(line: Line): Either[String, (String, Line)] = {
      line.headOption.map { item =>
        Right((item, line.tail))
      } getOrElse (Left(s"Option must have value. No value provided"))
    }
  }

  implicit val fileReader: Read[File] = new Read[java.io.File] {

    def read(line: Line): Either[String, (java.io.File, Line)] = {
      line.headOption.map { item =>
        Right((new java.io.File(item), line.tail))
      } getOrElse (Left(s"Option must have value. No value provided"))
    }
  }

  implicit val unitReader: Read[Unit] = new Read[Unit] {
    def read(line: Line): Either[String, (Unit, Line)] = Right(((), line))
  }
}
