package pravda.evm.disasm

import java.io.File

import pravda.evm.EVM._
import pravda.evm._
import pravda.evm.parse.Parser
import utest._

object DisasmTests extends TestSuite {

  val tests = Tests {
    'Disasm - {
      new File(getClass.getResource("/disasm").getPath).listFiles.foreach { f =>
        val bytes = evm.readSolidityBinFile(f)
        val Right(ops) = Parser.parseWithIndices(bytes)

        Predef.assert(!JumpTargetRecognizer(ops, bytes.length).exists {
          case (newOps, _) =>
            ops.zip(newOps).exists {
              case ((_, JumpI), (_, j)) =>
                j match {
                  case JumpI(_, _) => false
                  case _           => true
                }
              case ((_, Jump), (_, j)) =>
                j match {
                  case Jump(_, _) => false
                  case _          => true
                }
              case ((_, JumpDest), (_, j)) =>
                j match {
                  case JumpDest(_) => false
                  case _           => true
                }
              case _ => false
            }
        }, s"Error in ${f.getAbsolutePath}")
      }
    }
  }
}
