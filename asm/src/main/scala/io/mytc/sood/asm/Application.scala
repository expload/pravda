package io.mytc.sood.asm


class Application {

  import scala.io.Source
  import java.nio.ByteBuffer
  import scala.collection.mutable.ArrayBuffer
  import io.mytc.sood.vm.Vm


  def compile(filename: String): Either[String, Array[Byte]] = {
    val code = Source.fromFile(filename).getLines.toList.reduce(_ + "\n" + _)
    val asm = Assembler()
    val bcode = asm.compile(code)
    bcode
  }

  def compileAndRun(filename: String): Unit = {
    compile(filename) match {
      case Left(err)     ⇒ println("Compile error: " + err)
      case Right(bcode)  ⇒ Vm.run(ByteBuffer.wrap(bcode), Option.empty[ArrayBuffer[Array[Byte]]])
    }
  }

  def main(argv: Array[String]): Unit = {

    if (argv.size < 1) {
      println("Specify filename")
      System.exit(1)
    }

    compileAndRun(argv(0))

  }

}
