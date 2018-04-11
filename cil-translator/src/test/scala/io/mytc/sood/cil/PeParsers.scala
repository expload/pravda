package io.mytc.sood.cil

import org.scalatest.{FlatSpec, Matchers}
import java.nio.file.{Files, Paths}

import fastparse.byte.all._

class PeParsers extends FlatSpec with Matchers {
  "hello_world.exe" should "be parsed correctly" in {
    val helloWorldExe = Files.readAllBytes(Paths.get(getClass.getResource("/hello_world.exe").getPath))
    val ans = PE.parse(Bytes(helloWorldExe))
    println(ans)
    helloWorldExe.length shouldBe 3072
  }
}
