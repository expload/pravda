package pravda.cli.programs

import com.google.protobuf.ByteString
import pravda.cli.PravdaConfig
import pravda.node.client._
import utest._

object GenAddressSuite extends TestSuite {

  final val Pub = "e74b91ee9dda326116a08703eb387cc27a47e5d832072346fd65c40b89629b86"
  final val Sec = "359d41baf78afe0de1bbe7ae28c0450ce43c084f4bbb2bf1839dee466d852cb5" + Pub
  final val Json = s"""{"address":"$Pub","privateKey":"$Sec"}"""
  final val JsonBytes = ByteString.copyFromUtf8(Json)
  final val FileName = "a.out"
  final val Seed = 42

  val tests = Tests {
    "generate to stdout" - {
      val output = new IoLanguageStub(None)
      val random = new PredictableRandomLanguage(Seed)
      val program = new GenAddress(output, random)
      program(PravdaConfig.GenAddress())
      assert(output.stdout.headOption.contains(JsonBytes))
    }
    "generate to file" - {
      val output = new IoLanguageStub(None)
      val random = new PredictableRandomLanguage(Seed)
      val program = new GenAddress(output, random)
      program(PravdaConfig.GenAddress(Some(FileName)))
      assert(output.files.get(FileName).contains(JsonBytes))
    }
  }
}
