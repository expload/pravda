package pravda.testkit

import java.io.File
import java.nio.file.Files

import pravda.dotnet.parsers.FileParser
import pravda.dotnet.translation.Translator
import pravda.vm.{SandboxUtils, VmSandbox, asm}
import utest._

import scala.sys.process._

object DotnetSandbox extends TestSuite {

  def dotnetToAsm(filename: String, dlls: List[String]): List[asm.Operation] = {
    val exploadDll = new File("PravdaDotNet/Pravda.dll")

    new File("/tmp/pravda/").mkdirs()

    val tmpSrcs =
      (filename :: dlls).map(f => (new File(getClass.getResource(s"/$f").getPath), new File(s"/tmp/pravda/$f")))

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

    val Right((_, cilData, methods, signatures)) = FileParser.parsePe(Files.readAllBytes(exe.toPath))
    val Right((_, pdbTables)) = FileParser.parsePdb(Files.readAllBytes(pdb.toPath))
    val Right(asm) = Translator.translateAsm(methods, cilData, signatures, Some(pdbTables))
    asm
  }

  val tests = SandboxUtils.constructTestsFromDir(
    new File(getClass.getResource("/").getPath), {
      case VmSandbox.Macro("dotnet", filename :: dlls) => dotnetToAsm(filename, dlls)
    }
  )
}
