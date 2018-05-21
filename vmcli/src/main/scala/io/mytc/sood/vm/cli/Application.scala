package pravda.vm.cli

object Application {

  import pravda.vm.Vm
  import pravda.vm.state.Data
  import pravda.vm.state.Memory
  import pravda.vm.state.Address
  import pravda.vm.state.Storage
  import pravda.vm.state.Environment
  import pravda.vm.state.StackTrace
  import pravda.vm.state.ProgramContext
  import pravda.vm.state.VmErrorException

  import com.google.protobuf.ByteString

  class SimpleDb(val name: String) extends Storage {
    import java.io.ObjectInputStream
    import java.io.ObjectOutputStream
    import java.io.FileInputStream
    import java.io.FileOutputStream

    private type Cache = scala.collection.mutable.Map[Address, Data]
    private var cache = scala.collection.mutable.Map.empty[Address, Data]

    read()

    private def path(): String = "/tmp/" + name

    private def read(): Unit = {
      import java.io.File
      val f = new File(path)
      f.createNewFile()
      if (f.length != 0) {
        val in = new ObjectInputStream(new FileInputStream(path))
        cache = in.readObject.asInstanceOf[Cache]
        in.close()
      }
    }

    private def write(): Unit = {
      val out = new ObjectOutputStream(new FileOutputStream(path))
      out.writeObject(cache)
      out.close()
    }

    override def get(key: Address): Option[Data] = {
      cache.get(key)
    }

    override def put(key: Address, value: Data) = {
      cache += ((key, value))
      write()
    }

    override def delete(key: Address): Unit = {
      cache -= key
      write()
    }
  }

  class SimpleEnv(val name: String) extends Environment {

    private val storage = new SimpleDb(name)

    def rnKey(): Address = {
      val newAddress = scala.util.Random.alphanumeric.take(5).toArray.map(_.toByte)
      ByteString.copyFrom(newAddress)
    }

    def mkKey(prefix: String, address: Address): Address = {
      ByteString.copyFrom(prefix.toCharArray.map(_.toByte)).concat(address)
    }

    override def updateProgram(address: Data, code: Data): Unit = ???

    override def getProgram(address: Address): Option[ProgramContext] = {
      storage.get(mkKey("prg:", address)).map { prog =>
        new ProgramContext {
          override val storage = (new SimpleDb(name + "-inner"))
          override val code = prog.asReadOnlyByteBuffer
        }
      }
    }

    override def createProgram(owner: Address, code: Data): Address = {
      val newAddress = rnKey()
      storage.put(mkKey("prg:", newAddress), code)
      storage.put(mkKey("own:", newAddress), owner)
      newAddress
    }

    override def getProgramOwner(address: Address): Option[Address] = {
      storage.get(mkKey("own:", address))
    }
  }

  final case class Config(
      exec: Boolean = true,
      storageName: String = "mytc-storage",
      files: Seq[String] = Seq("stdin")
  )

  def hex(b: Byte): String = {
    val s = (b & 0xFF).toHexString
    if (s.length < 2) {
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
       |heap : ${memory.heap.zipWithIndex
         .map { case (d, i) => s"($i: ${hex(d.toByteArray)})" }
         .mkString("[", ", ", "]")}
       |""".stripMargin
  }

  def runBC(code: Array[Byte], config: Config): Memory = {
    Vm.runRaw(ByteString.copyFrom(code), ByteString.EMPTY, new SimpleEnv(config.storageName))
  }

  def run(filename: String, config: Config): Unit = {
    import scala.io.Source
    val hexCode = if (filename == "stdin") {
      Source.stdin.getLines.toList.reduce(_ + _)
    } else {
      Source.fromFile(filename).getLines.toList.reduce(_ + _)
    }
    val code = (new java.math.BigInteger(hexCode, 16).toByteArray)
    try {
      val mem = runBC(code, config)
      println(show(mem))
    } catch {
      case e @ VmErrorException(err, StackTrace(trace)) =>
        val point = trace.last
        println(err)
        point.address.foreach { addr =>
          println(s"\tprog: ${hex(addr.toByteArray)}")
        }
        println(s"\tpos: ${point.position}")
    }
  }

  def run(config: Config): Unit = {
    val fileName = config.files.head
    if (!(new java.io.File(fileName)).exists && fileName != "stdin") {
      System.err.println("File not found: " + fileName)
      System.exit(1)
    }
    run(fileName, config)
  }

  def main(argv: Array[String]): Unit = {

    val optParser = new scopt.OptionParser[Config]("scopt") {
      head("MyTC bytecode interpreter CLI", "")

      opt[String]('d', "db")
        .action { (name, c) =>
          c.copy(storageName = name)
        }
        .text("Storage name")

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
