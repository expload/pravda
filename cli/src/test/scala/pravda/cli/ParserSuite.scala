package pravda.cli

import utest._

object ParserSuite extends TestSuite {

  import Config._

  final val Address = "0000000000000000000000000000000000000000000000000000000000000000"

  val tests: Tests = Tests {
    "gen" - {
      "address" - assert {
        ArgumentsParser
          .parse(Seq("gen", "address"), Nope)
          .contains(GenAddress())
      }
      "address -o a.out" - {
        assert {
          ArgumentsParser
            .parse(Seq("gen", "address", "-o", "a.out"), Config.Nope)
            .collect { case Config.GenAddress(Some(file)) if file.endsWith("a.out") => () }
            .nonEmpty
        }
      }
    }
    "run" - {
      "-i a.out" - {
        assert {
          ArgumentsParser
            .parse(Seq("run", "-i", "a.out"), Config.Nope)
            .collect { case RunBytecode(_, Some(file), _) if file.endsWith("a.out") => () }
            .nonEmpty
        }
      }
      "--storage db" - assert {
        ArgumentsParser
          .parse(Seq("run", "--storage", "db"), Config.Nope)
          .collect { case RunBytecode(Some(file), _, _) if file.endsWith("db") => () }
          .nonEmpty
      }
      "--executor <address>" - assert {
        ArgumentsParser
          .parse(Seq("run", "--executor", Address), Config.Nope)
          .exists {
            case config: RunBytecode => config.executor == Address
            case _ => false
          }
      }
      assert {
        ArgumentsParser
          .parse(Seq("run"), Config.Nope)
          .contains(RunBytecode())
      }
    }
    "compile" - {
      "-i program.forth -o a.out" - assert {
        ArgumentsParser
          .parse(Seq("compile", "asm", "-i", "program.forth", "-o", "a.out"), Config.Nope)
          .exists {
            case config: Compile  =>
              config.input.exists(_.endsWith("program.forth")) &&
                config.output.exists(_.endsWith("a.out"))
            case _ => false
          }
      }
      "*" - assert {
        import CompileMode._

        def compile(name: String, compiler: CompileMode) = {
          ArgumentsParser
            .parse(Seq("compile", name), Config.Nope)
            .exists {
              case config: Compile =>
                config.compiler == compiler &&
                config.output.isEmpty &&
                config.input.isEmpty
              case _ => false
            }
        }

        Seq("asm" -> Asm,"disasm" -> Disasm, "dotnet" -> DotNet, "forth" -> Forth)
          .map { case (name, compiler) => compile(name, compiler) }
          .reduce(_ && _)
      }
    }
    "broadcast" - {
      "run -e http://example.com -w hw.json" - assert {
        ArgumentsParser
          .parse(Seq("broadcast", "run", "-e", "http://example.com", "-w", "hw.json"), Config.Nope)
          .exists {
            case Broadcast(Broadcast.Mode.Run, Some(wallet), None, "http://example.com")
              if wallet.endsWith("hw.json") => true
            case _ => false
          }
      }
    }
  }
}
