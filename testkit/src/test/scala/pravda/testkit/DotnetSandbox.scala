package pravda.testkit

import java.io.{File, PrintWriter}
import java.nio.file.Files

import pravda.dotnet.parsers.FileParser
import pravda.dotnet.translation.Translator
import pravda.vm.{SandboxUtils, VmSandbox, asm}
import utest._

import scala.io.Source
import scala.sys.process._

object DotnetSandbox extends TestSuite {

  def dotnetToAsm(content: String): List[asm.Operation] = {
    val exploadDll = new File(getClass.getResource("/expload.dll").getPath)
    val src = File.createTempFile("dotnet-", ".cs")
    val printer = new PrintWriter(src)
    printer.write(content)
    printer.close()
    val exe = File.createTempFile("dotnet-", ".exe")
    s"csc ${src.getAbsolutePath} /out:${exe.getAbsolutePath} /reference:${exploadDll.getAbsolutePath}".!!

    val Right((_, cilData, methods, signatures)) = FileParser.parsePe(Files.readAllBytes(exe.toPath))
    val Right(asm) = Translator.translateAsm(methods, cilData, signatures)
    asm
  }

  val tests = SandboxUtils.constructTestsFromDir(
    new File(getClass.getResource("/").getPath), {
      case VmSandbox.Macro("dotnet", List(filename)) =>
        val content = Source.fromFile(new File(getClass.getResource(s"/$filename").getPath)).mkString
        dotnetToAsm(content)
    }
  )
}
