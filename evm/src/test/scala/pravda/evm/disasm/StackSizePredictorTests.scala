package pravda.evm

package disasm

import java.io.File

import pravda.evm.EVM._
import pravda.evm.parse.Parser
import utest._

object StackSizePredictorTests extends TestSuite {

  val tests = Tests {

    "Stack size prediction" - {

      new File(getClass.getResource("/disasm").getPath).listFiles.foreach { f =>
        val bytes = readSolidityBinFile(f)
        val Right(ops) = Parser.parseWithIndices(bytes)
        val Right((c, r)) = Blocks.splitToCreativeAndRuntime(ops)

        //TODO use product
        val Right(opt1) = JumpTargetRecognizer(c).flatMap(a => JumpTargetRecognizer(r).map(b => (a, b)))

        Predef.assert(
          opt1 match {
            case (newOps1, newOps2) =>
              val res1 = StackSizePredictor.emulate(newOps1.map(_._2))
              val f = res1.forall {
                case (_, ind) if ind >= 0 => true
                case (Stop, -1)           => true
                case _                    => false
              }

              val res2 = StackSizePredictor.emulate(newOps2.map(_._2))
              val s = res2.forall {
                case (_, ind) if ind >= 0 => true
                case (Stop, -1)           => true
                case _                    => false
              }
              s && f
          },
          s"Error in ${f.getAbsolutePath}"
        )
      }
    }
  }
}
