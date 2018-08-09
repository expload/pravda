package pravda.testkit

import java.io.File
import java.nio.file.Files

import pravda.dotnet.parsers.FileParser
import pravda.dotnet.translation.Translator
import pravda.vm.{SandboxUtils, VmSandbox, asm}
import utest._

import scala.sys.process._

object DotnetSandbox extends TestSuite {

  def dotnetToAsm(filename: String): List[asm.Operation] = {
    val exploadDll = new File(getClass.getResource("/expload.dll").getPath)
    val src = new File(getClass.getResource(s"/$filename").getPath)
    val exe = File.createTempFile("dotnet-", ".exe")
    val pdb = File.createTempFile("dotnet-", ".pdb")
    s"""csc ${src.getAbsolutePath}
         |/out:${exe.getAbsolutePath}
         |/reference:${exploadDll.getAbsolutePath}
         |/debug:portable
         |/pdb:${pdb.getAbsolutePath}
      """.stripMargin.!!

    val Right((_, cilData, methods, signatures)) = FileParser.parsePe(Files.readAllBytes(exe.toPath))
    val Right((_, pdbTables)) = FileParser.parsePdb(Files.readAllBytes(pdb.toPath))
    val Right(asm) = Translator.translateAsm(methods, cilData, signatures, Some(pdbTables))
    asm
  }

  val tests = SandboxUtils.constructTestsFromDir(
    new File(getClass.getResource("/").getPath), {
      case VmSandbox.Macro("dotnet", List(filename)) => dotnetToAsm(filename)
    }
  )
}
