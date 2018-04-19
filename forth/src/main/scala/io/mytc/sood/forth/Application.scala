package io.mytc.sood.forth

object Application {

  def compile(filename: String): Either[String, Array[Byte]] = {
    import scala.io.Source
    val compiler = Compiler()
    val code = Source.fromFile(filename).getLines.toList.reduce(_ + "\n" + _)
    val bcode = compiler.compile(code)
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
