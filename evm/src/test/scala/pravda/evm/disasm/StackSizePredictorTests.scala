package pravda.evm.disasm

import pravda.evm.EVM._
import pravda.evm.translate.Translator.{ActualCode, CreationCode}
import utest._

object StackSizePredictorTests extends TestSuite {

  val tests = Tests {

    "Stack size prediction" - {

      DisasmTestHelper({ (ops, length) =>
        val opt = JumpTargetRecognizer(ops, length)
        opt.nonEmpty ==> true
        opt.map {
          case ((CreationCode(newOps1), ActualCode(newOps2)), others) =>
            val res = StackSizePredictor.emulate(newOps1.map(_._2))
            res
              .map({
                case (_, ind) if ind >= 0 => true
                case (Stop, -1)           => true
                case _                    => false
              })
              .foldLeft(true)({ case (acc, bool) => acc && bool })
        }.get
      })
    }
  }
}
