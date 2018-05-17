package io.mytc.sood.asm

object Application {

  final case class Config(
      out: String = "a.mytc",
      hexDump: Boolean = false,
      disasm: Boolean = false,
      files: Seq[String] = Seq("stdin")
  )

  def decompile(filename: String): Unit = {
    import scala.io.Source
    val hexCode = if (filename == "stdin") {
      Source.stdin.getLines.toList.reduce(_ + _)
    } else {
      Source.fromFile(filename).getLines.toList.reduce(_ + _)
    }
    val byteCode = (new java.math.BigInteger(hexCode, 16).toByteArray)
    val asm = Assembler()
    val asmCode = asm.decompile(byteCode)
    asmCode.foreach {
      case (no, op) =>
        print("%06X:\t".format(no))
        op match {
          case Op.Label(name)                   =>
          case Op.Stop                          => println(s"stop")
          case Op.Jump(name)                    => println(s"jump $name")
          case Op.JumpI(name)                   => println(s"jumpi $name")
          case Op.Pop                           => println(s"pop")
          case Op.Push(d)                       => println(s"push $d")
          case Op.Dup                           => println(s"dup")
          case Op.Swap                          => println(s"swap")
          case Op.Call(name)                    => println(s"call $name")
          case Op.Ret                           => println(s"ret")
          case Op.MPut                          => println(s"mput")
          case Op.MGet                          => println(s"mget")
          case Op.I32Add                        => println(s"i32add")
          case Op.I32Mul                        => println(s"i32mul")
          case Op.I32Div                        => println(s"i32div")
          case Op.I32Mod                        => println(s"i32mod")
          case Op.FAdd                          => println(s"fadd")
          case Op.FMul                          => println(s"fmul")
          case Op.FDiv                          => println(s"fdiv")
          case Op.FMod                          => println(s"fmod")
          case Op.Not                           => println(s"fmod")
          case Op.I32LT                         => println(s"i32lt")
          case Op.I32GT                         => println(s"i32gt")
          case Op.Eq                            => println(s"eq")
          case Op.Nop                           => println(s"nop")
          case Op.Dupn                          => println(s"dupn")
          case Op.Concat                        => println(s"concat")
          case Op.From                          => println(s"from")
          case Op.PCreate                       => println(s"pcreate")
          case Op.PUpdate                       => println(s"pupdate")
          case Op.PCall                         => println(s"pcall")
          case Op.LCall(address, func, argsNum) => println(s"lcall $address $func $argsNum")
          case Op.SGet                          => println(s"sget")
          case Op.SPut                          => println(s"sput")
        }
    }
  }

  def compile(filename: String): Either[String, Array[Byte]] = {
    import scala.io.Source
    val code = Source.fromFile(filename).getLines.toList.reduce(_ + "\n" + _)
    val asm = Assembler()
    val bcode = asm.compile(code)
    bcode
  }

  def run(config: Config): Unit = {
    import java.io.FileOutputStream
    import java.io.BufferedOutputStream

    val fileName = config.files.head

    if (!(new java.io.File(fileName)).exists && fileName != "stdin") {
      System.err.println("File not found: " + fileName)
      System.exit(1)
    }

    if (config.disasm) {
      decompile(fileName)
    } else {
      compile(fileName) match {
        case Right(code) ⇒ {
          if (config.hexDump) {
            val hexStr = code.map("%02X" format _).mkString
            println(hexStr)
          } else {
            val out = new BufferedOutputStream(new FileOutputStream(config.out))
            out.write(code)
            out.close()
          }
        }
        case Left(err) ⇒ {
          System.err.println(err)
          System.exit(1)
        }
      }
    }
  }

  def main(argv: Array[String]): Unit = {

    val optParser = new scopt.OptionParser[Config]("scopt") {
      head("Forth language compiler", "")

      opt[String]('o', "output")
        .action { (name, c) =>
          c.copy(out = name)
        }
        .text("Output file")

      opt[Unit]('x', "hex")
        .action { (_, c) =>
          c.copy(hexDump = true)
        }
        .text("Hex dump of bytecode")

      opt[Unit]('d', "disasm")
        .action { (_, c) =>
          c.copy(disasm = true)
        }
        .text("Hex dump of bytecode")

      opt[String]('o', "output")
        .action { (name, c) =>
          c.copy(out = name)
        }
        .text("Output file or stdout (hex string)")

      arg[String]("<filename>")
        .unbounded()
        .optional()
        .action { (name, c) =>
          c.copy(files = c.files :+ name)
        }
        .text("Files to compile")

      help("help").text("Simple usage: forth filename.forth")
    }

    optParser.parse(argv, Config()) match {
      case Some(config) => run(config)
      case None         => ()
    }
  }

}
