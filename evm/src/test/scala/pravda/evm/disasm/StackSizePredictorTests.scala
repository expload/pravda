package pravda.evm

package disasm

import java.io.File

import pravda.evm.EVM._
import pravda.evm.parse.Parser
import pravda.evm.translate.Translator.{ActualCode, CreationCode}
import utest._

object StackSizePredictorTests extends TestSuite {

  val tests = Tests {

    "Stack size prediction" - {

      new File(getClass.getResource("/disasm").getPath).listFiles.foreach { f =>
        val bytes = readSolidityBinFile(f)
        val Right(ops) = Parser.parseWithIndices(bytes)
        val Right(opt) = JumpTargetRecognizer(ops)

        Predef.assert(
          opt match {
            case (CreationCode(newOps1), ActualCode(newOps2)) =>
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
