package pravda.evm.disasm

import java.io.File
import java.net.URL

import pravda.evm.EVM._
import pravda.evm._
import pravda.evm.parse.Parser
import utest._

object DisasmTests extends TestSuite {

  val tests = Tests {

    'Disasm - {

      val x: URL = Thread.currentThread().getContextClassLoader().getResource("disasm")
      new File(x.toURI).listFiles.foreach({ f =>
        val bytes = evm.readSolidityBinFile(f) //evm.readSolidityBinFile("SimpleStorage.bin")

        val parsed = Parser.parseWithIndices(bytes)

        parsed.isRight ==> true
        parsed.map { ops =>
          JumpTargetRecognizer(ops, bytes.length).foreach {
            case (newOps, others) =>
              ops.zip(newOps).foreach {
                case ((_, JumpI), (_, j)) =>
                  true ==> (j match {
                    case JumpI(_, _) => true
                    case _           => false
                  })
                case ((_, Jump), (_, j)) =>
                  true ==> (j match {
                    case Jump(_, _) => true
                    case _          => false
                  })
                case ((_, JumpDest), (_, j)) =>
                  true ==> (j match {
                    case JumpDest(_) => true
                    case _           => false
                  })

                case (a, b) => ()
              }

          }
        }
      })
    }
  }
}
