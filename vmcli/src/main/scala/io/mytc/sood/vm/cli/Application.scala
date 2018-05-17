package io.mytc.sood.vm.cli

object Application {

  import io.mytc.sood.vm.state.Memory
  import io.mytc.sood.vm.state.VmErrorException


  final case class Config(
    exec: Boolean      = true,
    files: Seq[String] = Seq("stdin")
  )

  def hex(b: Byte): String = {
    val s = (b & 0xFF).toHexString
    if(s.length < 2){
      s"0$s"
    } else {
      s
    }
  }

  def hex(bs: Seq[Byte]): String = {
    bs.map(hex).mkString(" ")
  }

  def show(memory: Memory): String = {
    s"""
       |stack: ${memory.stack.map(d => hex(d.toByteArray)).mkString("[", ", ", "]")}
       |heap : ${memory.heap.zipWithIndex.map{case (d, i) => s"($i: ${hex(d.toByteArray)})"}.mkString("[", ", ", "]")}
       |""".stripMargin
  }

  def runBC(code: Array[Byte]): Memory = {
    import com.google.protobuf.ByteString
    import io.mytc.sood.vm.Vm
    import io.mytc.sood.vm.state._

    val emptyState = new Environment {
      override def getProgram(address: Address): Option[ProgramContext] = None
      override def updateProgram(address: Data, code: Data): Unit = ???
      override def createProgram(owner: Address, code: Data): Address = ???
      override def getProgramOwner(address: Address): Option[Address] = ???
    }
    Vm.runRaw(ByteString.copyFrom(code), ByteString.EMPTY, emptyState)
  }

  def run(filename: String): Unit = {
    import scala.io.Source
    val hexCode = if (filename == "stdin") {
      Source.stdin.getLines.toList.reduce(_ + _)
    } else {
      Source.fromFile(filename).getLines.toList.reduce(_ + _)
    }
    val code = (new java.math.BigInteger(hexCode, 16).toByteArray)
    try {
      val mem = runBC(code)
      println(show(mem))
    } catch {
      case e @ VmErrorException(err, trace) =>
        val point = trace.last

        println(err)
        point.address.foreach{ addr => println(s"\tprog: ${hex(addr.toByteArray)}") }
        println($"\tpos: ${point.position}")
    }
  }

  def show(trace: )
  def run(config: Config): Unit = {
    val fileName = config.files.head
    if (!(new java.io.File(fileName)).exists && fileName != "stdin") {
      System.err.println("File not found: " + fileName)
      System.exit(1)
    }
    run(fileName)
  }

  def main(argv: Array[String]): Unit = {

    val optParser = new scopt.OptionParser[Config]("scopt") {
      head("MyTC bytecode interpreter CLI", "")

      opt[Unit]('x', "execute")
        .action { (_, c) =>
          c.copy(exec = true)
        }
        .text("Exectute bytecode")

      arg[String]("<filename>")
        .unbounded()
        .optional()
        .action { (name, c) =>
          c.copy(files = c.files :+ name)
        }
        .text("Files to interpret")

      help("help").text("Simple usage: vmcli filename.forth")
    }

    optParser.parse(argv, Config()) match {
      case Some(config) => run(config)
      case None         => ()
    }
  }

}

