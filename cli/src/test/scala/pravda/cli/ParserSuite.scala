package pravda.cli

import utest._

object ParserSuite extends TestSuite {

  import Config._

  val tests: Tests = Tests {
    "gen" - {
      "address" - assert {
        Parser
          .parse(Array("gen", "address"), Nope)
          .contains(GenAddress())
      }
      "address -o a.out" - {
        assert {
          Parser
            .parse(Array("gen", "address", "-o", "a.out"), Config.Nope)
            .collect { case Config.GenAddress(Output.OsFile(file)) if file.endsWith("a.out") => () }
            .nonEmpty
        }
      }
    }
    "test" - {
      "-i a.out" - {
        assert {
          Parser
            .parse(Array("test", "-i", "a.out"), Config.Nope)
            .collect { case TestBytecode(_, Input.OsFile(file)) if file.endsWith("a.out") => () }
            .nonEmpty
        }
      }
      "--storage db" - assert {
        Parser
          .parse(Array("test", "--storage", "db"), Config.Nope)
          .collect { case TestBytecode(Some(file), _) if file.endsWith("db") => () }
          .nonEmpty
      }
      assert {
        Parser
          .parse(Array("test"), Config.Nope)
          .contains(TestBytecode())
      }
    }
  }
}
