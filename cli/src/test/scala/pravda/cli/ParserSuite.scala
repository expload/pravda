package pravda.cli

import utest._

object ParserSuite extends TestSuite {

  import Config._

  final val Address = "0000000000000000000000000000000000000000000000000000000000000000"

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
    "run" - {
      "-i a.out" - {
        assert {
          Parser
            .parse(Array("run", "-i", "a.out"), Config.Nope)
            .collect { case RunBytecode(_, Some(file), _) if file.endsWith("a.out") => () }
            .nonEmpty
        }
      }
      "--storage db" - assert {
        Parser
          .parse(Array("run", "--storage", "db"), Config.Nope)
          .collect { case RunBytecode(Some(file), _, _) if file.endsWith("db") => () }
          .nonEmpty
      }
      "--executor <address>" - assert {
        Parser
          .parse(Array("run", "--executor", Address), Config.Nope)
          .exists {
            case config: RunBytecode => config.executor == Address
            case _ => false
          }
      }
      assert {
        Parser
          .parse(Array("run"), Config.Nope)
          .contains(RunBytecode())
      }
    }
  }
}
