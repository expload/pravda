package pravda

import fastparse.byte.all._

import scala.io.Source

package object evm {

  def readSolidityBinFile(filename: String): Bytes = {
    val s = Source.fromResource(filename).mkString
    val allBytes = Bytes(s.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte))
    allBytes.dropRight(43)
    // for dropRight(43) see https://ethereum.stackexchange.com/questions/42584/what-is-auxdata-in-the-asm-output-from-solc
    // we just drop auxdata
  }
}
