package pravda.cli

import utest._

object ParserSuite extends TestSuite {

  import PravdaConfig._
  import pravda.yopt.CommandLine.Ok

  final val Address = "0000000000000000000000000000000000000000000000000000000000000000"

  val tests: Tests = Tests {
    "gen" - {
      "address" - {
        PravdaArgsParser.parse(List("gen", "address"), Nope) ==> Ok(GenAddress())
      }
      "address -o a.out" - {
        assert {
          PravdaArgsParser.parse(List("gen", "address", "-o", "a.out"), PravdaConfig.Nope) match {
            case Ok(PravdaConfig.GenAddress(Some(file))) => file.endsWith("a.out")
            case _                                       => false
          }
        }
      }
    }
    "run" - {
      "-i a.out" - assert {
        PravdaArgsParser.parse(List("run", "-i", "a.out"), PravdaConfig.Nope) match {
          case Ok(RunBytecode(_, Some(file), _)) => file.endsWith("a.out")
          case _                                 => false
        }
      }
      "--storage db" - assert {
        PravdaArgsParser.parse(List("run", "--storage", "db"), PravdaConfig.Nope) match {
          case Ok(RunBytecode(Some(file), _, _)) => file.endsWith("db")
          case _                                 => false
        }
      }
      "--executor <address>" - assert {
        PravdaArgsParser.parse(List("run", "--executor", Address), PravdaConfig.Nope) match {
          case Ok(config: RunBytecode) => config.executor == Address
          case _                       => false
        }
      }
      "run " - assert {
        PravdaArgsParser.parse(List("run"), PravdaConfig.Nope) match {
          case Ok(config) => config == RunBytecode()
          case _          => false
        }
      }
    }
    "compile" - {
      "-i program.forth -o a.out" - assert {
        PravdaArgsParser.parse(List("compile", "asm", "-o", "a.out", "-i", "program.forth"), PravdaConfig.Nope) match {
          case Ok(config: Compile) =>
            config.input.exists(_.endsWith("program.forth")) &&
              config.output.exists(_.endsWith("a.out"))
          case _ => false
        }
      }
      "*" - assert {
        import CompileMode._

        def compile(name: String, compiler: CompileMode) = {
          PravdaArgsParser.parse(List("compile", name), PravdaConfig.Nope) match {
            case Ok(config: Compile) =>
              config.compiler == compiler &&
                config.output.isEmpty &&
                config.input.isEmpty
            case _ => false
          }
        }

        List("asm" -> Asm, "disasm" -> Disasm, "dotnet" -> DotNet)
          .map { case (name, compiler) => compile(name, compiler) }
          .reduce(_ && _)
      }
    }
    "broadcast" - {
      "run -e http://example.com -w hw.json" - assert {
        PravdaArgsParser.parse(List("broadcast", "run", "-e", "http://example.com", "-w", "hw.json"), PravdaConfig.Nope) match {
          case Ok(Broadcast(Broadcast.Mode.Run, Some(wallet), _, None, _, _, _, "http://example.com"))
              if wallet.endsWith("hw.json") =>
            true
          case x => false
        }
      }
    }
  }
}
