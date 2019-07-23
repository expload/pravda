package pravda.cli.programs

import cats.implicits._
import com.google.protobuf.ByteString
import pravda.cli.PravdaConfig
import pravda.node.client.impl.{CompilersLanguageImpl, MetadataLanguageImpl}
import pravda.node.client.{IoLanguageStub, IpfsLanguageStub, NodeLanguageStub}
import pravda.common.data.blockchain.TransactionId
import pravda.node.servers.Abci.TransactionResult
import pravda.vm.asm.{Operation, PravdaAssembler}
import pravda.common.vm._
import utest._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object ExternalMetaSuite extends TestSuite {

  val tests = Tests {

    val wallet = ByteString.copyFromUtf8(
      """{
        |  "address":"69924b322fd02c372c7723d99a3899e5e642308610c6105caeb90df4c5bdcfeb",
        |  "privateKey":"9025d5131469794a20c6cd11cceac38b2c2155a30baf52a94b980d15c927283669924b322fd02c372c7723d99a3899e5e642308610c6105caeb90df4c5bdcfeb"
        |}""".stripMargin)

    val programWallet = ByteString.copyFromUtf8(
      """{
        |  "address":"8d207d6ff17c53c6ca3962204b1774e85af99a34dd1d660a3b8dccea032ef0bd",
        |  "privateKey":"bc9f6b10c18cb4e36e4f7cd25612d736821ba6c613e8d64d541c37651f1ca4718d207d6ff17c53c6ca3962204b1774e85af99a34dd1d660a3b8dccea032ef0bd"
        |}""".stripMargin)

    val ops = Seq(
      Operation.Meta(Meta.Custom("custom1")),
      Operation.Meta(Meta.Custom("custom2")),
      Operation.Push(Data.Primitive.Int32(2)),
      Operation.Push(Data.Primitive.Int32(2)),
      Operation.Meta(Meta.ProgramName("program_name")),
      Operation.Meta(Meta.SourceMark("source.src", 1, 2, 10, 20)),
      Operation(Opcodes.ADD)
    )

    val program = PravdaAssembler.assemble(
      ops,
      saveLabels = true
    )

    val ipfsHash = "Qm" + ("a" * 40) + "0000"

    val programWithoutMeta = PravdaAssembler.assemble(
      Seq(
        Operation.Meta(Meta.IpfsFile(ipfsHash)),
        Operation.Push(Data.Primitive.Int32(2)),
        Operation.Push(Data.Primitive.Int32(2)),
        Operation(Opcodes.ADD)
      ),
      saveLabels = true
    )

    val programWithMeta = PravdaAssembler.assemble(
      Operation.Meta(Meta.IpfsFile(ipfsHash)) +: ops,
      saveLabels = true
    )

    // offsets starts from 48, because of ipfs hash size
    val metas = Map(
      48 -> Seq(Meta.Custom("custom1"), Meta.Custom("custom2")),
      54 -> Seq(Meta.ProgramName("program_name"), Meta.SourceMark("source.src", 1, 2, 10, 20))
    )

    "deploy and save external meta" - {

      val node = new NodeLanguageStub[Future](
        Right(TransactionResult(TransactionId @@ ByteString.EMPTY, Right(FinalState.Empty), Nil)))

      val ipfs = new IpfsLanguageStub[Future]()

      val broadcast =
        new Broadcast[Future](
          new IoLanguageStub[Future](files =
            mutable.Map("wallet.json" -> wallet, "program_wallet.json" -> programWallet, "program.pravda" -> program)),
          node,
          new CompilersLanguageImpl(),
          ipfs,
          new MetadataLanguageImpl()
        )

      Await.ready(
        broadcast(
          PravdaConfig.Broadcast(
            mode = PravdaConfig.Broadcast.Mode.Deploy,
            wallet = Some("wallet.json"),
            programWallet = Some("program_wallet.json"),
            input = Some("program.pravda"),
            metaToIpfs = true
          )),
        5.seconds
      )

      val broadcastedProgram = PravdaAssembler.disassemble(node.broadcastedData(0)).map(_._2)
      val Seq(Operation.Push(_),
              Operation.Push(code: Data.Primitive.Bytes),
              Operation.Push(_),
              Operation.Orphan(Opcodes.PCREATE)) =
        broadcastedProgram

      code.data ==> programWithoutMeta

      assert(ipfs.files.contains(ipfsHash))

      Meta.externalReadFromByteBuffer(ipfs.files(ipfsHash).asReadOnlyByteBuffer()) ==> metas
    }

    "disasm with external meta" - {

      val io = new IoLanguageStub[Future](
        files = mutable.Map(
          "wallet.json" -> wallet,
          "program_wallet.json" -> programWallet,
          "program.pravda" -> programWithoutMeta
        ))

      val compile =
        new Compile[Future](
          io,
          new CompilersLanguageImpl(),
          new IpfsLanguageStub[Future](Map(ipfsHash -> Meta.externalWriteToByteString(metas))),
          new MetadataLanguageImpl()
        )

      Await.ready(
        compile(
          PravdaConfig.Compile(compiler = PravdaConfig.CompileMode.Disasm,
                               input = List("program.pravda"),
                               metaFromIpfs = true)),
        5.seconds
      )

      PravdaAssembler.assemble(io.stdout(0).toStringUtf8, saveLabels = true).right.get ==> programWithMeta
    }
  }
}
