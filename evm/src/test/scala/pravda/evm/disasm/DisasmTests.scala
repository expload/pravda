package pravda.evm.disasm

import pravda.evm.EVM._
import pravda.evm.translate.Translator.{ActualCode, CreationCode}
import utest._

object DisasmTests extends TestSuite {

  val tests = Tests {

    'Disasm - {

      DisasmTestHelper({ (ops, length) =>
        val opt = JumpTargetRecognizer(ops, length)
        opt.nonEmpty ==> true
        opt
          .map({
            case ((CreationCode(newOps1), ActualCode(newOps2)), others) =>
              ops
                .zip(newOps1 ::: newOps2)
                .map({
                  case ((_, JumpI), (_, j)) =>
                    j match {
                      case JumpI(_, _) => true
                      case _           => false
                    }
                  case ((_, Jump), (_, j)) =>
                    j match {
                      case Jump(_, _) => true
                      case _          => false
                    }
                  case ((_, JumpDest), (_, j)) =>
                    j match {
                      case JumpDest(_) => true
                      case _           => false
                    }

                  case (a, b) => true
                })
                .foldLeft(true)({ case (acc, bool) => acc && bool })
          })
          .get
      })
    }
  }
}
