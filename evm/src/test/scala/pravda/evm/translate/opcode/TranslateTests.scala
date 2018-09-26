package pravda.evm.translate.opcode

import fastparse.byte.all._
import pravda.evm.EVM._
import utest._

object TranslateTests extends TestSuite {



  val tests = Tests {
    import SimpleTranslation._
    "Push(0x80)" - {
      evmOpToOps(Push(hex"0x80")) ==> Right(List(pushBigInt(BigInt(128))))
    }






  }
}
