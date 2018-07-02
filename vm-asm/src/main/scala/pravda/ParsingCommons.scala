package pravda

object ParsingCommons {

  import fastparse.noApi._

  final case class ParsingError(line: Int, col: Int, expected: String, found: String) {

    def mkString: String = {
      s"$line:$col: $expected expected but '$found' found."
    }
  }

  def prettyPrintError[T](source: String, p: Parsed[T], fileName: String = ""): Either[ParsingError, T] = {
    p match {
      case Parsed.Success(value, _) => Right(value)
      case Parsed.Failure(_, index, extra) =>
        def aux(start: Int, i: Int, lim: Int): String = {
          if (lim > 0 && i < source.length
              && source.charAt(i) != '\n'
              && source.charAt(i) != '\r'
              && source.charAt(i) != ' ') aux(start, i + 1, lim - 1)
          else source.substring(start, i - 1)
        }
        val (line, col) = {
          var line = 1
          var col = 1
          var i = 0
          while (i < index) {
            if (source.charAt(i) == '\n') {
              col = 1
              line += 1
            } else {
              col += 1
            }
            i += 1
          }
          (line, col)
        }
        val found = aux(index, index, 20)
        Left(ParsingError(line, col, extra.traced.expected, found))
    }
  }
}
