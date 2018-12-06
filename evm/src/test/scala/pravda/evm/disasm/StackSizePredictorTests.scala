package pravda.evm.disasm

import pravda.evm.EVM._
import pravda.evm.translate.Translator.{ActualCode, CreationCode}
import utest._

object StackSizePredictorTests extends TestSuite {

  val tests = Tests {

    "Stack size prediction" - {

      DisasmTestHelper({ (ops, length) =>
        val opt = JumpTargetRecognizer(ops)
        opt.isRight ==> true
        opt
          .map {
            case (CreationCode(newOps1), ActualCode(newOps2)) =>
              val res = StackSizePredictor.emulate(newOps1.map(_._2))
              val f = res
                .map({
                  case (_, ind) if ind >= 0 => true
                  case (Stop, -1)           => true
                  case _                    => false
                })
                .foldLeft(true)({ case (acc, bool) => acc && bool })

              val res1 = StackSizePredictor.emulate(newOps2.map(_._2))
              val s = res1
                .map({
                  case (_, ind) if ind >= 0 => true
                  case (Stop, -1)           => true
                  case _                    => false
                })
                .foldLeft(true)({ case (acc, bool) => acc && bool })
              f && s
          }
          .right
          .get
      })
    }
  }
}
