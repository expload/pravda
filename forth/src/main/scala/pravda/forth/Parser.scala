package pravda.forth

final case class NamedFunction[T, V](f: T => V, name: String) extends (T => V) {
  def apply(t: T) = f(t)
  override def toString() = name
}

class Parser {

  object grammar {
    import fastparse.all._
    val keyword = P("if" | "else" | "then" | "while" | "loop")

    val hexDigit = P(CharIn('0' to '9', 'a' to 'f', 'A' to 'F'))
    val StringChars = NamedFunction(!"\"\\".contains(_: Char), "StringChars")
    val strChars = P(CharsWhile(StringChars))
    val unicodeEscape = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)

    val escape = P("\\" ~ (CharIn("\"/\\bfnrt") | unicodeEscape))
    val alpha = P(CharIn("!@#$%^&*-_+=<>/\\|~'") | CharIn('a' to 'z') | CharIn('A' to 'Z'))
    val digit = P(CharIn('0' to '9'))
    val aldig = P(alpha | digit)
    val delim = P(CharIn(" \t\r\n").rep(1))
    val nSgnPart = P(CharIn("+-"))
    val nIntPart = P("0" | CharIn('1' to '9') ~ digit.rep)
    val nFrcPart = P("." ~ digit.rep)
    val nExpPart = P(CharIn("eE") ~ CharIn("+-").? ~ digit.rep)
    val integStmt = P(nSgnPart.? ~ digit.rep(1))
    val numbrStmt = P(nSgnPart.? ~ nIntPart.? ~ nFrcPart ~ nExpPart.?)
    val chrarStmt = P("\"" ~/ (strChars | escape).rep.! ~ "\"")

    val hexarStmt = P("$x" ~ CharIn("0123456789ABCDEFabcdef").rep(1).!).map { str =>
      str.sliding(2, 2).map(x => java.lang.Integer.valueOf(x, 16).toByte).toArray
    }
    val identStmt = P((digit.rep ~ alpha ~ aldig.rep) | (aldig.rep ~ alpha ~ aldig.rep))
    val dwordStmt = P(":" ~ delim ~ identStmt.! ~ delim ~ blockStmt ~ delim ~ ";")
    val ifStmt = P("if" ~ delim ~ blockStmt ~ delim ~ "then")

    val blockStmt: P[Seq[Statement]] = P(
      dwordStmt.map { case (n, b) => Statement.Dword(n, b) } |
        ifStmt.map(b => Statement.If(b, Seq())) |
        numbrStmt.!.map(v => Statement.Float(v.toDouble)) |
        integStmt.!.map(v => Statement.Integ(v.toInt)) |
        hexarStmt.map { v =>
          Statement.Hexar(v)
        } |
        chrarStmt.map { v =>
          Statement.Chrar(v)
        } |
        (!keyword ~ identStmt).!.map(v => Statement.Ident(v))
    ).rep(sep = delim)

    val forthUnit = P(Start ~ delim.? ~ blockStmt ~ delim.? ~ End)
  }

  def lex(code: String): Either[String, Seq[Statement]] = grammar.forthUnit.parse(code) match {
    case fastparse.all.Parsed.Success(ast, idx) => Right(ast)
    case e: fastparse.all.Parsed.Failure        => Left(e.extra.traced.trace)
  }

  def parse(code: String): Either[String, Seq[Statement]] = {

    lex(code) match {
      case Right(seq) => Right(seq)
      case Left(err)  => Left(err)
    }

  }

}

object Parser {
  def apply(): Parser = new Parser
}
