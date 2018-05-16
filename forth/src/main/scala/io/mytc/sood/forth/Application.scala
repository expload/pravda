package io.mytc.sood.forth

object Application {

  case class Config(
    out: String         = "a.mytc",
    hexDump: Boolean    = false,
    files: Seq[String]  = Seq.empty[String]
  )

  def compile(filename: String): Either[String, Array[Byte]] = {
    import scala.io.Source
    val compiler = Compiler()
    val code = Source.fromFile(filename).getLines.toList.reduce(_ + "\n" + _)
    val bcode = compiler.compile(code, useStdLib = true)
    bcode
  }

  def run(config: Config): Unit = {
    import java.io.FileOutputStream
    import java.io.BufferedOutputStream

    val fileName = config.files.head

    if (!(new java.io.File(fileName)).exists) {
      System.err.println("File not found: " + fileName)
      System.exit(1)
    }

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
      case Left(err) ⇒ System.err.println(err)
    }
  }

  def main(argv: Array[String]): Unit = {

    val optParser = new scopt.OptionParser[Config]("scopt") {
      head("Forth language compiler", "")

      opt[String]('o', "output").action{ (name, c) =>
        c.copy(out = name)
      }.text("Output file")

      opt[Unit]('x', "hex").action{ (_, c) =>
        c.copy(hexDump = true)
      }.text("Hex dump of bytecode")

      opt[String]('o', "output").action{ (name, c) =>
        c.copy(out = name)
      }.text("Output file or stdout (hex string)")

      arg[String]("<filename>").unbounded().action{ (name, c) =>
        c.copy(files = c.files :+ name)
      }.text("Files to compile")

      help("help").text("Simple usage: forth filename.forth")
    }

    optParser.parse(argv, Config()) match {
      case Some(config) => run(config)
      case None         => ()
    }
  }

}
