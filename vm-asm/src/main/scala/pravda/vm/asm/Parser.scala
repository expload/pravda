package pravda.vm.asm

class Parser {

  private val grammar = {

    import fastparse.all._

    val digit = P(CharIn('0' to '9'))
    val nSgnPart = P(CharIn("+-"))
    val nIntPart = P("0" | CharIn('1' to '9') ~ digit.rep)
    val nFrcPart = P("." ~ digit.rep)
    val nExpPart = P(CharIn("eE") ~ CharIn("+-").? ~ digit.rep)

    val alpha = P(CharIn("!@#$%^&*-_+=.<>/\\|~'") | CharIn('a' to 'z') | CharIn('A' to 'Z'))
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

    val eqls = P(IgnoreCase("eq"))
    val from = P(IgnoreCase("from"))
    val pcrt = P(IgnoreCase("pcreate"))
    val pupd = P(IgnoreCase("pupdate"))

    val lcall = P(IgnoreCase("lcall"))
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

          add.!.map(_ => Op.Add) |
          mul.!.map(_ => Op.Mul) |
          div.!.map(_ => Op.Div) |
          mod.!.map(_ => Op.Mod) |

          clt.!.map(_ => Op.Lt) |
          cgt.!.map(_ => Op.Gt) |

          eqls.!.map(_ => Op.Eq) |
          from.!.map(_ => Op.From) |
          pcrt.!.map(_ => Op.PCreate) |
          pupd.!.map(_ => Op.PUpdate) |

          pcall.map(_ => Op.PCall) |
          lcall.map(_ => Op.LCall) |

          transfer.map(_ => Op.Transfer)
      ).rep(sep = delim))

    P(Start ~ delim.rep ~ opseq ~ delim.rep ~ End)
  }

  def parse(code: String): Either[String, Seq[Op]] = grammar.parse(code) match {
    case fastparse.all.Parsed.Success(ast, idx) => Right(ast)
    case e: fastparse.all.Parsed.Failure        => Left(e.extra.traced.trace)
  }
}

object Parser {
  def apply(): Parser = new Parser
}
