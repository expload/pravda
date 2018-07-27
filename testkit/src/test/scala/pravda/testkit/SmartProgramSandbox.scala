package pravda.testkit

import java.io.{File, PrintWriter}
import java.nio.file.Files

import fastparse.all._
import pravda.dotnet.parsers.FileParser
import pravda.dotnet.translation.Translator
import pravda.vm.{SandboxUtils, VmSandbox, asm}
import utest._

import scala.io.Source
import scala.sys.process._

object SmartProgramSandbox extends TestSuite {

  sealed trait FileLocation

  object FileLocation {
    final case class Resource(filename: String) extends FileLocation
    final case class Url(url: String)           extends FileLocation
  }

  object FileLocationExtractor {
    private def fileLocationParser(prefix: String) = P(prefix ~ "(" ~ CharsWhile(_ != ')').! ~ ")")
    private val resourceParser = fileLocationParser("resource")
    private val urlParser = fileLocationParser("url")

    def unapply(arg: String): Option[FileLocation] = arg match {
      case resourceParser(r) => Some(FileLocation.Resource(r))
      case urlParser(u)      => Some(FileLocation.Url(u))
      case other             => Some(FileLocation.Resource(other))
    }
  }

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

  def fileContent(location: FileLocation): String = location match {
    case FileLocation.Resource(r) =>
      Source.fromFile(new File(getClass.getResource(s"/$r").getPath)).mkString
    case FileLocation.Url(u) => Source.fromURL(u).mkString
  }

  val tests = SandboxUtils.constructTestsFromDir(
    new File(getClass.getResource("/").getPath), {
      case VmSandbox.Macro("dotnet", List(FileLocationExtractor(loc))) => dotnetToAsm(fileContent(loc))
    }
  )
}
