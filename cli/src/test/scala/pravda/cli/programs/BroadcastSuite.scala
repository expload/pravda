package pravda.cli.programs

import cats.Id
import com.google.protobuf.ByteString
import pravda.cli.PravdaConfig
import pravda.cli.languages.{CompilersLanguage, IoLanguageStub, NodeLanguageStub}
import utest._

import scala.collection.mutable

object BroadcastSuite extends TestSuite {

  final val Wallet = ByteString.copyFromUtf8(
    """{
      |  "address":"ed0fdb5d8aa0672a04737f4a017fdafac47b9607a47e553c792681358f0a1d54",
      |  "privateKey":"e5dda154fae3407b24d2b8a6457a449809234f18a812f95844db25af519f9073ed0fdb5d8aa0672a04737f4a017fdafac47b9607a47e553c792681358f0a1d54"
      |}
      """.stripMargin)

  val tests = Tests {
    "run -w w.json" - {
      val api = new NodeLanguageStub(Right("[]"))
      val io = new IoLanguageStub(files = mutable.Map("w.json" -> Wallet))
      val compilers = new CompilersLanguage[Id] {
        def asm(source: String): Id[Either[String, ByteString]] = Left("nope")
        def disasm(source: ByteString): Id[String] = ""
        def forth(source: String): Id[Either[String, ByteString]] = Left("nope")
        def dotnet(sourse: ByteString): Id[Either[String, ByteString]] = Left("nope")
      }
      val program = new Broadcast(io, api, compilers)
      program(PravdaConfig.Broadcast(mode = PravdaConfig.Broadcast.Mode.Run, wallet = Some("w.json")))
      assert(io.stdout.headOption.contains(ByteString.copyFromUtf8("[]\n")))
    }

    "run" - {
      val api = new NodeLanguageStub(Right("[]"))
      val io = new IoLanguageStub()
      val compilers = new CompilersLanguage[Id] {
        def asm(source: String): Id[Either[String, ByteString]] = Left("nope")
        def disasm(source: ByteString): Id[String] = ""
        def forth(source: String): Id[Either[String, ByteString]] = Left("nope")
        def dotnet(sourse: ByteString): Id[Either[String, ByteString]] = Left("nope")
      }
      val program = new Broadcast(io, api, compilers)
      program(PravdaConfig.Broadcast(mode = PravdaConfig.Broadcast.Mode.Run))
      assert(io.stderr.headOption.contains(ByteString.copyFromUtf8("Wallet file should be defined\n")))
    }

  }
}
