package pravda.cli

import utest._

object ParserSuite extends TestSuite {

  import Config._

  val tests: Tests = Tests {

    "gen address" - assert {
      Parser
        .parse(Array("gen", "address"), Nope)
        .contains(GenAddress())
    }

    "gen address -o /dev/null" - assert {
      Parser
        .parse(Array("gen", "address", "-o", "/dev/null"), Config.Nope)
        .contains(GenAddress(output = Output.OsFile("/dev/null")))
    }
  }
}
