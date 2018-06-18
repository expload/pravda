package pravda.cli

import utest._

object ParserSuite extends TestSuite {

  import Config._
  import pravda.cmdopt.CommandLine.Ok

  final val Address = "0000000000000000000000000000000000000000000000000000000000000000"

  val tests: Tests = Tests {
    "gen" - {
      "address" - assert {
        ArgumentsParser
          .parse(Seq("gen", "address"), Nope) == Ok(GenAddress())
      }
      "address -o a.out" - {
        assert {
          ArgumentsParser.parse(Seq("gen", "address", "-o", "a.out"), Config.Nope) match {
            case Ok(Config.GenAddress(Some(file))) => file.endsWith("a.out")
            case _ => false
          }
        }
      }
    }
    "run" - {
      "-i a.out" - assert {
        ArgumentsParser.parse(Seq("run", "-i", "a.out"), Config.Nope) match {
          case Ok(RunBytecode(_, Some(file), _)) => file.endsWith("a.out")
          case _ => false
        }
      }
      "--storage db" - assert {
        ArgumentsParser.parse(Seq("run", "--storage", "db"), Config.Nope) match {
          case Ok(RunBytecode(Some(file), _, _)) => file.endsWith("db")
          case _ => false
        }
      }
      "--executor <address>" - assert {
        ArgumentsParser.parse(Seq("run", "--executor", Address), Config.Nope) match {
          case Ok(config: RunBytecode) => config.executor == Address
          case _ => false
        }
      }
      "run " - assert {
        ArgumentsParser.parse(Seq("run"), Config.Nope) match {
          case Ok(config) => config == RunBytecode()
          case _ => false
        }
      }
    }
    "compile" - {
      "-i program.forth -o a.out" - assert {
        ArgumentsParser.parse(Seq("compile", "asm", "-i", "program.forth", "-o", "a.out"), Config.Nope) match {
          case Ok(config: Compile) =>
            config.input.exists(_.endsWith("program.forth")) &&
                config.output.exists(_.endsWith("a.out"))
          case _ => false
        }
      }
      "*" - assert {
        import CompileMode._

        def compile(name: String, compiler: CompileMode) = {
          ArgumentsParser.parse(Seq("compile", name), Config.Nope) match {
            case Ok(config: Compile) =>
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
        ArgumentsParser.parse(Seq("broadcast", "run", "-e", "http://example.com", "-w", "hw.json"), Config.Nope) match {
          case Ok(Broadcast(Broadcast.Mode.Run, Some(wallet), None, _, _, "http://example.com"))
            if wallet.endsWith("hw.json") => true
          case x => false
        }
      }
    }
  }
}
