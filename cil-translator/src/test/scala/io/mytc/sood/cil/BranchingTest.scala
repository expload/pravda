package io.mytc.sood.cil

import java.nio.file.{Files, Paths}

import fastparse.byte.all._
import io.mytc.sood.cil.CIL._
import io.mytc.sood.cil.PE.Info._
import io.mytc.sood.cil.utils._
import org.scalatest.{FlatSpec, Matchers}


class BranchingTest extends FlatSpec with Matchers {

  def parsePe(file: String): Validated[(Pe, CilData, Seq[Seq[OpCode]])] = {
    val fileBytes = Files.readAllBytes(Paths.get(this.getClass.getResource(s"/$file").getPath))
    val peV = PE.parseInfo(Bytes(fileBytes))

    for {
      pe <- peV
      cilData <- CIL.fromPeData(pe.peData)
      codeParser = CIL.code(cilData)
      ops <- pe.methods.map(m => codeParser.parse(m.codeBytes).toValidated.joinRight).sequence
    } yield (pe, cilData, ops)
  }

  "simple_if.exe" should "translated correctly" in {

    val Right((pe, cilData, opCodes)) = parsePe("simple_if.exe")

    val tr = Translator
    val rs1 = tr.resolveRVI(Translator.CilContext(opcodes = opCodes(0)))
    val rs2 = tr.translate(Translator.CilContext(opcodes = opCodes(0)))

    println(rs1.opcodes)
    println(rs2)

  }

  "simple_for.exe" should "translated correctly" in {

    val Right((pe, cilData, opCodes)) = parsePe("simple_for.exe")

    println(opCodes)

    val tr = Translator
    val rs = tr.translate(Translator.CilContext(opcodes = opCodes(0)))

    // println(rs1.opcodes)
    // println()
    println(rs)

  }

}
