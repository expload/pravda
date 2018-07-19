package pravda.testkit

//import java.io.File
//import java.nio.file.Files
//
//import pravda.dotnet.parsers.FileParser
//import pravda.dotnet.translation.Translator
//import pravda.vm.{SandboxUtils, VmSandbox}
import utest._

//import scala.sys.process._

object SmartProgramSandbox extends TestSuite {

  val tests = Tests {
    'nope - {
      assert(true)
    }
  }
//  val tests = SandboxUtils.constructTestsFromFolder(
//    new File(getClass.getResource("/smart_program").getPath), {
//      case VmSandbox.Macro("dotnet", List(file)) =>
//        val dotnetSrc = new File(getClass.getResource(s"/$file").getPath)
//        val exploadDll = new File(getClass.getResource("/expload.dll").getPath)
//        val exe = File.createTempFile("dotnet-", ".exe")
//        s"csc ${dotnetSrc.getAbsolutePath} /out:${exe.getAbsolutePath} /reference:${exploadDll.getAbsolutePath}".!!
//
//        val Right((_, cilData, methods, signatures)) = FileParser.parsePe(Files.readAllBytes(exe.toPath))
//        val Right(asm) = Translator.translateAsm(methods, cilData, signatures)
//        asm
//    }
//  )
}
