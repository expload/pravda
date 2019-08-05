package pravda.cli.programs

import cats.Id
import com.google.protobuf.ByteString
import pravda.cli.PravdaConfig
import pravda.common.vm.Meta
import pravda.node.client._
import pravda.common.data.blockchain._
import pravda.common.vm._
import pravda.vm.asm.Operation
import utest._

import scala.collection.mutable

object BroadcastSuite extends TestSuite {

  final val expectedResult =
    s"""{
       |  "transactionId" : "",
       |  "executionResult" : {
       |    "success" : {
       |      "spentWatts" : 0,
       |      "refundWatts" : 0,
       |      "totalWatts" : 0,
       |      "stack" : [ ],
       |      "heap" : [ ]
       |    }
       |  },
       |  "effects" : [ ]
       |}
       |""".stripMargin

  final val Wallet = ByteString.copyFromUtf8(
    """{
      |  "address":"ed0fdb5d8aa0672a04737f4a017fdafac47b9607a47e553c792681358f0a1d54",
      |  "privateKey":"e5dda154fae3407b24d2b8a6457a449809234f18a812f95844db25af519f9073ed0fdb5d8aa0672a04737f4a017fdafac47b9607a47e553c792681358f0a1d54"
      |}
      """.stripMargin)

  val tests = Tests {
    "run -w w.json" - {
      val api =
        new NodeLanguageStub[Id](
          Right(TransactionResult(TransactionId @@ ByteString.EMPTY, Right(FinalState.Empty), Nil)))
      val io = new IoLanguageStub(files = mutable.Map("w.json" -> Wallet))
      val compilers = new CompilersLanguage[Id] {
        def asm(fileName: String, source: String): Id[Either[String, ByteString]] = Left("nope")
        def asm(source: String): Id[Either[String, ByteString]] = Left("nope")
        def disasm(source: ByteString): Id[String] = ""
        def disasmToOps(source: ByteString): Id[Seq[(Int, Operation)]] = Nil
        def dotnet(sources: Seq[(ByteString, Option[ByteString])],
                   mainClass: Option[String]): Id[Either[String, ByteString]] = Left("nope")
        def evm(source: ByteString, abi: ByteString): Id[Either[String, ByteString]] = Left("nope")
        def disasm(source: ByteString, metas: Map[Int, Seq[Meta]]): Id[String] = ???
        def disasmToOps(source: ByteString, metas: Map[Int, Seq[Meta]]): Id[Seq[(Int, Operation)]] = ???
      }
      val metadata = new MetadataLanguageStub[Id]()
      val ipfs = new IpfsLanguageStub[Id]()
      val program = new Broadcast(io, api, compilers, ipfs, metadata)
      program(PravdaConfig.Broadcast(mode = PravdaConfig.Broadcast.Mode.Run, wallet = Some("w.json")))
      assert(io.stdout.headOption.contains(ByteString.copyFromUtf8(expectedResult)))
    }

    "run" - {
      val api =
        new NodeLanguageStub[Id](
          Right(TransactionResult(TransactionId @@ ByteString.EMPTY, Right(FinalState.Empty), Nil)))
      val io = new IoLanguageStub()
      val compilers = new CompilersLanguage[Id] {
        def asm(source: String): Id[Either[String, ByteString]] = Left("nope")
        def asm(fileName: String, source: String): Id[Either[String, ByteString]] = Left("nope")
        def disasmToOps(source: ByteString): Id[Seq[(Int, Operation)]] = Nil
        def disasm(source: ByteString): Id[String] = ""
        def dotnet(sources: Seq[(ByteString, Option[ByteString])],
                   mainClass: Option[String]): Id[Either[String, ByteString]] = Left("nope")

        def evm(source: ByteString, abi: ByteString): Id[Either[String, ByteString]] = Left("nope")
        def disasm(source: ByteString, metas: Map[Int, Seq[Meta]]): Id[String] = ???
        def disasmToOps(source: ByteString, metas: Map[Int, Seq[Meta]]): Id[Seq[(Int, Operation)]] = ???
      }
      val metadata = new MetadataLanguageStub[Id]()
      val ipfs = new IpfsLanguageStub[Id]()
      val program = new Broadcast(io, api, compilers, ipfs, metadata)
      program(PravdaConfig.Broadcast(mode = PravdaConfig.Broadcast.Mode.Run))
      assert(io.stderr.headOption.contains(ByteString.copyFromUtf8("Wallet file should be defined\n")))
    }

  }
}
