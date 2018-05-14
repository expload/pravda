package io.mytc.sood.forth

class Parser {
  object grammar {
    import fastparse.all._
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
    val identStmt = P((digit.rep ~ alpha ~ aldig.rep) | (aldig.rep ~ alpha ~ aldig.rep))

    val dwordStmt = P(":" ~ delim ~ identStmt.! ~ delim ~ blockStmt ~ delim ~ ";")

    val blockStmt: P[Seq[Statement]] = P(
      dwordStmt.map { case (n, b) ⇒ Statement.Dword(n, b) } |
        identStmt.!.map(v ⇒ Statement.Ident(v)) |
        numbrStmt.!.map(v ⇒ Statement.Float(v.toDouble)) |
        integStmt.!.map(v ⇒ Statement.Integ(v.toInt))
    ).rep(sep = delim)

    val forthUnit = P(Start ~ delim.rep ~ blockStmt ~ delim.rep ~ End)
  }

  def parse(code: String): Either[String, Seq[Statement]] = grammar.forthUnit.parse(code) match {
    case fastparse.all.Parsed.Success(ast, idx) ⇒ Right(ast)
    case e: fastparse.all.Parsed.Failure        ⇒ Left(e.extra.traced.trace)
  }
}

object Parser {
  def apply(): Parser = new Parser
}
