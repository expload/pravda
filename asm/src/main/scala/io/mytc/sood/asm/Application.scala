package io.mytc.sood.asm

class Application {

  def compile(filename: String): Either[String, Array[Byte]] = {
    import scala.io.Source
    val code = Source.fromFile(filename).getLines.toList.reduce(_ + "\n" + _)
    val asm = Assembler()
    val bcode = asm.compile(code)
    bcode
  }

  def main(argv: Array[String]): Unit = {

    import java.io.FileOutputStream
    import java.io.BufferedOutputStream

    if (argv.size < 1) {
      println("Specify filename")
      System.exit(1)
    }

    compile(argv(0)) match {
      case Right(code) ⇒ {
        val out = new BufferedOutputStream(new FileOutputStream("a.out"))
        out.write(code)
        out.close()
      }
      case Left(err) ⇒ System.err.println(err)
    }

  }

}
