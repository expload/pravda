package pravda.vm.asm

class Parser {

  object grammar {
    import fastparse.all._

    val nSgnPart = P(CharIn("+-"))
    val nIntPart = P("0" | CharIn('1' to '9') ~ digit.rep)
    val nFrcPart = P("." ~ digit.rep)
    val nExpPart = P(CharIn("eE") ~ CharIn("+-").? ~ digit.rep)

    val alpha = P(CharIn("!@#$%^&*-_+=.<>/\\|~'") | CharIn('a' to 'z') | CharIn('A' to 'Z'))
    val digit = P(CharIn('0' to '9'))
    val aldig = P(alpha | digit)
    val ident = P((digit.rep ~ alpha ~ aldig.rep) | (aldig.rep ~ alpha ~ aldig.rep))

    val delim = P(CharIn(" \t\r\n").rep(1))

    val hexs = P("$x" ~ CharIn("0123456789ABCDEFabcdef").rep(1).!).map { str =>
      Datum.Rawbytes(str.sliding(2, 2).map(x => java.lang.Integer.valueOf(x, 16).toByte).toArray)
    }

    val hexw = P("0x" ~ CharIn("0123456789ABCDEFabcdef").rep(1).!).map(x => java.lang.Integer.valueOf(x, 16))
    val word = P(CharIn('0' to '9').rep(1)).!.map(x => java.lang.Integer.valueOf(x, 16))

    val integ = P(hexw | word).map(x => Datum.Integral(x))

    val numbr =
      P(nSgnPart.? ~ nIntPart.? ~ nFrcPart ~ nExpPart.?).!.map(x => Datum.Floating(java.lang.Double.valueOf(x)))

    // val alpha = P( CharIn('a' to 'z') | CharIn('A' to 'Z') | CharIn("_") )
    // val digit = P( CharIn('0' to '9') )
    // val ident = P( alpha ~ ( alpha | digit ).rep )

    val label = P("@" ~ ident.! ~ ":")

    val stop = P(IgnoreCase("stp"))
    val jump = P(IgnoreCase("jmp") ~ delim ~ "@" ~ ident.!)
    val jumpi = P(IgnoreCase("jmpi") ~ delim ~ "@" ~ ident.!)

    val pop = P(IgnoreCase("pop"))
    val push = P(IgnoreCase("push") ~ delim ~ (integ | numbr | hexs))
    val dup = P(IgnoreCase("dup"))
    val swap = P(IgnoreCase("swap"))

    val call = P(IgnoreCase("call") ~ delim ~ "@" ~ ident.!)
    val ret = P(IgnoreCase("ret"))

    val mput = P(IgnoreCase("mput"))
    val mget = P(IgnoreCase("mget"))

    val add = P(IgnoreCase("add"))
    val mul = P(IgnoreCase("mul"))
    val div = P(IgnoreCase("div"))
    val mod = P(IgnoreCase("mod"))

    val clt = P(IgnoreCase("clt"))
    val cgt = P(IgnoreCase("cgt"))

    val fadd = P(IgnoreCase("fadd"))
    val fmul = P(IgnoreCase("fmul"))
    val fdiv = P(IgnoreCase("fdiv"))
    val fmod = P(IgnoreCase("fmod"))

    val eqls = P(IgnoreCase("eq"))
    val conc = P(IgnoreCase("concat"))
    val from = P(IgnoreCase("from"))
    val pcrt = P(IgnoreCase("pcreate"))
    val pupd = P(IgnoreCase("pupdate"))

    val lcall = P(IgnoreCase("lcall") ~ delim ~ ident.! ~ delim ~ ident.! ~ delim ~ word.map(_.intValue))
    val pcall = P(IgnoreCase("pcall"))

    val transfer = P(IgnoreCase("transfer"))

    val opseq: P[Seq[Op]] = P(
      (
        label.map(n => Op.Label(n)) |
          stop.!.map(_ => Op.Stop) |
          jump.map(n => Op.Jump(n)) |
          jumpi.map(n => Op.JumpI(n)) |
          pop.!.map(_ => Op.Pop) |
          push.map(x => Op.Push(x)) |
          dup.map(_ => Op.Dup) |
          swap.map(_ => Op.Swap) |

          call.map(n => Op.Call(n)) |
          ret.!.map(_ => Op.Ret) |

          mput.!.map(_ => Op.MPut) |
          mget.!.map(_ => Op.MGet) |

          add.!.map(_ => Op.I32Add) |
          mul.!.map(_ => Op.I32Mul) |
          div.!.map(_ => Op.I32Div) |
          mod.!.map(_ => Op.I32Mod) |

          clt.!.map(_ => Op.I32LT) |
          cgt.!.map(_ => Op.I32GT) |

          eqls.!.map(_ => Op.Eq) |
          conc.!.map(_ => Op.Concat) |
          from.!.map(_ => Op.From) |
          pcrt.!.map(_ => Op.PCreate) |
          pupd.!.map(_ => Op.PUpdate) |

          fadd.!.map(_ => Op.FAdd) |
          fmul.!.map(_ => Op.FMul) |
          fdiv.!.map(_ => Op.FDiv) |
          fmod.!.map(_ => Op.FMod) |
          pcall.map(_ => Op.PCall) |
          lcall.map(Op.LCall.tupled) |

          transfer.map(_ => Op.Transfer)
      ).rep(sep = delim))

    val unit = P(Start ~ delim.rep ~ opseq ~ delim.rep ~ End)
  }

  def parse(code: String): Either[String, Seq[Op]] = grammar.unit.parse(code) match {
    case fastparse.all.Parsed.Success(ast, idx) => Right(ast)
    case e: fastparse.all.Parsed.Failure        => Left(e.extra.traced.trace)
  }
}

object Parser {
  def apply(): Parser = new Parser
}
