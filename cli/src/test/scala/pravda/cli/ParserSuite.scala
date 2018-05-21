package pravda.cli

import utest._

object ParserSuite extends TestSuite {

  import Config._

  val tests: Tests = Tests {

    "gen address" - assert {
      Parser
        .parse(Array("gen", "address"), Nope)
        .contains(GenWallet())
    }

    "gen address -o /dev/null" - assert {
      Parser
        .parse(Array("gen", "address", "-o", "/dev/null"), Config.Nope)
        .contains(GenWallet(output = Output.OsFile("/dev/null")))
    }
  }
}
