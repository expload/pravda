package pravda.testkit

import java.io.File
import java.nio.file.Files

import fastparse.all._
import pravda.dotnet.parser.FileParser
import pravda.dotnet.translation.Translator
import pravda.plaintest._
import pravda.vm.{VmSandbox, asm}

import scala.sys.process._

object DotnetSandbox extends Plaintest {

  private def dotnetToAsm(filename: String, dlls: List[String]): Either[String, List[asm.Operation]] = {
    val exploadDll = new File("PravdaDotNet/Pravda.dll")

    new File("/tmp/pravda/").mkdirs()

    val tmpSrcs =
      (filename :: dlls).map(f => (new File(s"dotnet-tests/resources/$f"), new File(s"/tmp/pravda/$f")))

    tmpSrcs.foreach {
      case (from, dest) =>
        if (!dest.exists()) {
          Files.copy(from.toPath, dest.toPath)
        }
    }

    val exe = File.createTempFile("dotnet-", ".exe")
    val pdb = File.createTempFile("dotnet-", ".pdb")
    s"""csc ${tmpSrcs.head._2.getAbsolutePath}
         |-out:${exe.getAbsolutePath}
         |-reference:${exploadDll.getAbsolutePath}
         |${tmpSrcs.tail.map(dll => s"-reference:${dll._2.getAbsolutePath}").mkString("\n")}
         |-debug:portable
         |-pdb:${pdb.getAbsolutePath}
      """.stripMargin.!!

    for {
      pe <- FileParser.parsePe(Files.readAllBytes(exe.toPath))
      (_, cilData, methods, signatures) = pe
      pdb <- FileParser.parsePdb(Files.readAllBytes(pdb.toPath))
      (_, pdbTables) = pdb
      asm <- Translator.translateAsm(methods, cilData, signatures, Some(pdbTables)).left.map(_.mkString)
    } yield asm
  }

  lazy val dir = new File("testkit/src/test/resources")
  override lazy val ext = "sbox"

  type State = VmSandbox.Case
  lazy val initState = VmSandbox.Case()

  lazy val scheme = Seq(
    parserInput("preconditions")(VmSandbox.preconditions.map(p => s => s.copy(preconditions = Some(p)))),
    input("dotnet-files") { text =>
      val file :: dlls = text.split("\\s+").toList
      dotnetToAsm(file, dlls).map(ops => s => s.copy(program = Some(ops)))
    },
    textOutput("expectations") { c =>
      val res = for {
        pre <- c.preconditions
        prog <- c.program
      } yield VmSandbox.printExpectations(VmSandbox.sandboxRun(prog, pre))

      res.toRight("preconditions or program is missing")
    }
  )

}
