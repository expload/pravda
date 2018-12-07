package pravda.evm

import java.io.File

import fastparse.byte.all._

import scala.io.Source

package object evm {
  private def readSolidityBinFileS(s: String): Bytes = {
    val allBytes = Bytes(s.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte))
    allBytes.dropRight(43)
    // for dropRight(43) see https://ethereum.stackexchange.com/questions/42584/what-is-auxdata-in-the-asm-output-from-solc
    // we just drop auxdata
  }

  def readSolidityBinFile(filename: String): Bytes = readSolidityBinFileS(Source.fromResource(filename).mkString)

  def readSolidityBinFile(file: File): Bytes = readSolidityBinFileS(Source.fromFile(file).mkString)

  def writeSolidityBinFile(filename: String): Bytes = {

    val s = Source.fromResource(filename).mkString
    val allBytes = Bytes(s.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte))

    allBytes
  }

  def readSolidityABI(filename: String): String = Source.fromResource(filename).mkString
}
