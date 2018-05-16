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
    asmCode.foreach(println)
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
