package io.mytc.sood.asm


class Parser {
  object grammar {
    import fastparse.all._

    val nSgnPart = P( CharIn("+-") )
    val nIntPart = P( "0" | CharIn('1' to '9') ~ digit.rep )
    val nFrcPart = P( "." ~ digit.rep )
    val nExpPart = P( CharIn("eE") ~ CharIn("+-").? ~ digit.rep )

    val alpha = P( CharIn("!@#$%^&*-_+=.<>/\\|~'") | CharIn('a' to 'z') | CharIn('A' to 'Z') )
    val digit = P( CharIn('0' to '9') )
    val aldig = P( alpha | digit )
    val ident = P( (digit.rep ~ alpha ~ aldig.rep) | (aldig.rep ~ alpha ~ aldig.rep) )

    val delim = P( CharIn(" \t\r\n").rep(1) )

    val hexw  = P( "0x" ~ CharIn("0123456789ABCDEFabcdef").rep(1).! ).map(x ⇒ java.lang.Integer.valueOf(x, 16))
    val word  = P( CharIn('0' to '9').rep(1) ).!.map(x ⇒ java.lang.Integer.valueOf(x, 16))

    val integ = P( hexw | word ).map(x ⇒ Datum.Integral(x))
    val numbr = P( nSgnPart.? ~ nIntPart.? ~ nFrcPart ~ nExpPart.? ).!.map(x ⇒ Datum.Floating(java.lang.Double.valueOf(x)))

    // val alpha = P( CharIn('a' to 'z') | CharIn('A' to 'Z') | CharIn("_") )
    // val digit = P( CharIn('0' to '9') )
    // val ident = P( alpha ~ ( alpha | digit ).rep )

    val label = P( "@" ~ ident.! ~ ":" )

    val stop  = P( IgnoreCase("stp") )
    val jump  = P( IgnoreCase("jmp") )
    val jumpi = P( IgnoreCase("jmpi") )

    val pop   = P( IgnoreCase("pop") )
    val push  = P( IgnoreCase("push") ~ delim ~ ( integ | numbr ) )
    val dup   = P( IgnoreCase("dup") )
    val swap  = P( IgnoreCase("swp") )

    val call  = P( IgnoreCase("call") ~ delim ~ "@" ~ ident.! )
    val ret   = P( IgnoreCase("ret") )

    val mput  = P( IgnoreCase("mput") )
    val mget  = P( IgnoreCase("mget") )

    val add   = P( IgnoreCase("add") )
    val mul   = P( IgnoreCase("mul") )
    val div   = P( IgnoreCase("div") )
    val mod   = P( IgnoreCase("mod") )

    val fadd   = P( IgnoreCase("fadd") )
    val fmul   = P( IgnoreCase("fmul") )
    val fdiv   = P( IgnoreCase("fdiv") )
    val fmod   = P( IgnoreCase("fmod") )

    val lcall  = P( IgnoreCase("lcall") ~ delim ~ ident.! ~ delim ~ ident.! ~ word.map(_.intValue) )

    val opseq: P[Seq[Op]] = P( (
      label   .map(n ⇒ Op.Label(n)) |
      stop  .!.map(_ ⇒ Op.Stop)     |
      jump  .!.map(_ ⇒ Op.Jump)     |
      jumpi .!.map(_ ⇒ Op.JumpI)    |

      pop   .!.map(_ ⇒ Op.Pop)      |
      push    .map(x ⇒ Op.Push(x))  |
      dup     .map(_ ⇒ Op.Dup)      |
      swap    .map(_ ⇒ Op.Swap)     |

      call    .map(n ⇒ Op.Call(n))  |
      ret   .!.map(_ ⇒ Op.Ret)      |

      mput  .!.map(_ ⇒ Op.MPut)     |
      mget  .!.map(_ ⇒ Op.MGet)     |

      add   .!.map(_ ⇒ Op.I32Add)   |
      mul   .!.map(_ ⇒ Op.I32Mul)   |
      div   .!.map(_ ⇒ Op.I32Div)   |
      mod   .!.map(_ ⇒ Op.I32Mod)   |

      fadd  .!.map(_ ⇒ Op.FAdd)     |
      fmul  .!.map(_ ⇒ Op.FMul)     |
      fdiv  .!.map(_ ⇒ Op.FDiv)     |
      fmod  .!.map(_ ⇒ Op.FMod)     |
      lcall   .map(Op.LCall.tupled)
    ).rep(sep = delim) )

    val unit = P(Start ~ delim.rep ~ opseq ~ delim.rep ~ End)
  }

  def parse(code: String): Either[String, Seq[Op]] = grammar.unit.parse(code) match {
    case fastparse.all.Parsed.Success(ast, idx) ⇒ Right(ast)
    case e: fastparse.all.Parsed.Failure ⇒ Left(e.extra.traced.trace)
  }
}

object Parser {
  def apply(): Parser = new Parser
}
