package pravda.evm.disasm

import java.io.File
import java.net.URL

import pravda.evm.EVM.Op
import pravda.evm.evm
import pravda.evm.parse.Parser
import pravda.evm.translate.Translator.Addressed
import utest._

object DisasmTestHelper {

  def apply(func: (List[Addressed[Op]], Long) => Boolean): Unit = {
    val x: URL = Thread.currentThread().getContextClassLoader().getResource("disasm")
    new File(x.toURI).listFiles.foreach({ f =>
      val bytes = evm.readSolidityBinFile(f) //evm.readSolidityBinFile("SimpleStorage.bin")

      val parsed = Parser.parseWithIndices(bytes)

      parsed.isRight ==> true
      parsed.map(ops => func(ops, bytes.length)) ==> Right(true)
    })
  }
}
