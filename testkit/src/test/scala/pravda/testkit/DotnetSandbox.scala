package pravda.testkit

import java.io.File
import java.nio.file.{Files, Paths}

import fastparse.all._
import pravda.dotnet.parser.FileParser
import pravda.dotnet.parser.FileParser.ParsedDotnetFile
import pravda.dotnet.translation.Translator
import pravda.proverka._
import pravda.vm.{VmSandbox, asm}

import cats.instances.list._
import cats.instances.either._
import cats.syntax.traverse._

object DotnetSandbox extends Proverka {

  private def dotnetToAsm(filename: String,
                          dllsFiles: List[String],
                          mainClass: Option[String]): Either[String, List[asm.Operation]] = {
    import scala.sys.process._

    val exploadDll = new File("PravdaDotNet/Pravda.dll")

    new File("/tmp/pravda/").mkdirs()

    val tmpSrcs =
      (filename :: dllsFiles).map(f => (new File(s"dotnet-tests/resources/$f"), new File(s"/tmp/pravda/$f")))

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
      pdb <- FileParser.parsePdb(Files.readAllBytes(pdb.toPath))
      dlls <- dllsFiles
        .map(f => FileParser.parsePe(Files.readAllBytes(Paths.get(s"dotnet-tests/resources/$f"))))
        .sequence
      asm <- Translator
        .translateAsm(ParsedDotnetFile(pe, Some(pdb)) :: dlls.map(dll => ParsedDotnetFile(dll, None)), mainClass)
        .left
        .map(_.mkString)
    } yield asm
  }

  lazy val dir = new File("testkit/src/test/resources")
  override lazy val ext = "sbox"

  type State = VmSandbox.Case
  lazy val initState = VmSandbox.Case()

  lazy val scheme = Seq(
    parserInput("preconditions")(VmSandbox.preconditions.map(p => s => s.copy(preconditions = Some(p)))),
    input("dotnet-files") { text =>
      val lines = text.lines.toList
      val file :: dlls = lines.head.split("\\s+").toList
      val mainClass = lines.tail.headOption
      dotnetToAsm(file, dlls, mainClass).map(ops => s => s.copy(program = Some(ops)))
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
