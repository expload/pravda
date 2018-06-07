package pravda.cli

import java.io.File

import pravda.cli.Config.CompileMode
import pravda.common.bytes
import pravda.common.domain.NativeCoin
import scopt.OptionParser

object ArgumentsParser extends OptionParser[Config]("pravda") {

  head("Pravda Command Line Interface")

  cmd("gen")
    .text("Useful stuff generators.")
    .children(
      cmd("address")
        .text("Generate ed25519 key pair. It can be used as regualr wallet or validator node identifier.")
        .action((_, _) => Config.GenAddress())
        .children(
          opt[File]('o', "output")
            .text("Output file")
            .action {
              case (file, Config.GenAddress(_)) =>
                Config.GenAddress(Some(file.getAbsolutePath))
              case (_, otherwise) => otherwise
            }
        )
    )

  cmd("run")
    .text("Run bytecode given from stdin or file on Pravda VM.")
    .action((_, _) => Config.RunBytecode())
    .children(
      opt[String]('e', "executor")
        .validate {
          case s if bytes.isHex(s) && s.length == 64 => Right(())
          case s                                     => Left(s"`$s` is not valid address. It should be 32 bytes hex string.")
        }
        .text("Executor address HEX representation")
        .action {
          case (address, config: Config.RunBytecode) =>
            config.copy(executor = address)
          case (_, otherwise) => otherwise
        },
      opt[File]('i', "input")
        .text("Input file")
        .action {
          case (file, config: Config.RunBytecode) =>
            config.copy(input = Some(file.getAbsolutePath))
          case (_, otherwise) => otherwise
        },
      opt[File]("storage")
        .text("Storage name")
        .action {
          case (file, config: Config.RunBytecode) =>
            config.copy(storage = Some(file.getAbsolutePath))
          case (_, otherwise) => otherwise
        }
    )

  cmd("compile")
    .action((_, _) => Config.Compile(CompileMode.Nope))
    .children(
      opt[File]('i', "input")
        .text("Input file")
        .action {
          case (file, config: Config.Compile) =>
            config.copy(input = Some(file.getAbsolutePath))
          case (_, otherwise) => otherwise
        },
      opt[File]('o', "output")
        .text("Output file")
        .action {
          case (file, config: Config.Compile) =>
            config.copy(output = Some(file.getAbsolutePath))
          case (_, otherwise) => otherwise
        },
      cmd("asm")
        .text("Assemble Pravda VM bytecode from text presentation.")
        .action {
          case (_, config: Config.Compile) =>
            config.copy(compiler = Config.CompileMode.Asm)
          case (_, otherwise) => otherwise
        },
      cmd("disasm")
        .text("Disassemble Pravda VM bytecode to text presentation.")
        .action {
          case (_, config: Config.Compile) =>
            config.copy(compiler = Config.CompileMode.Disasm)
          case (_, otherwise) => otherwise
        },
      cmd("forth")
        .text("Compile Pravda pseudo-forth to Pravda VM bytecode.")
        .action {
          case (_, config: Config.Compile) =>
            config.copy(compiler = Config.CompileMode.Forth)
          case (_, otherwise) => otherwise
        },
      cmd("dotnet")
        .text("Compile .exe produced byt .NET compiler to Pravda VM bytecode.")
        .action {
          case (_, config: Config.Compile) =>
            config.copy(compiler = Config.CompileMode.DotNet)
          case (_, otherwise) => otherwise
        },
    )

  cmd("broadcast")
    .text("Broadcasting program to the network")
    .children(
      cmd("run")
        .action((_, _) => Config.Broadcast(Config.Broadcast.Mode.Run))
        .text("")
        .action {
          case (_, config: Config.Broadcast) =>
            config.copy(mode = Config.Broadcast.Mode.Run)
          case (_, otherwise) => otherwise
        },
      cmd("transfer")
        .action((_, _) => Config.Broadcast(Config.Broadcast.Mode.Transfer(None, None)))
        .children(
          opt[String]('t', "to")
            .action {
              case (hex, config @ Config.Broadcast(mode: Config.Broadcast.Mode.Transfer, _, _, _, _, _)) =>
                config.copy(mode = mode.copy(to = Some(hex)))
              case (_, otherwise) => otherwise
            },
          opt[Long]('a', "amount")
            .action {
              case (amount, config @ Config.Broadcast(mode: Config.Broadcast.Mode.Transfer, _, _, _, _, _)) =>
                config.copy(mode = mode.copy(amount = Some(amount)))
              case (_, otherwise) => otherwise
            }
        ),
      cmd("deploy")
        .text("")
        .action((_, _) => Config.Broadcast(Config.Broadcast.Mode.Deploy))
        .action {
          case (_, config: Config.Broadcast) =>
            config.copy(mode = Config.Broadcast.Mode.Deploy)
          case (_, otherwise) => otherwise
        },
      cmd("update")
        .text("")
        .action((_, _) => Config.Broadcast(Config.Broadcast.Mode.Update(None)))
        .children(
          opt[String]('p', "program")
            .action {
              case (hex, config: Config.Broadcast) =>
                config.copy(mode = Config.Broadcast.Mode.Update(Some(hex)))
              case (_, otherwise) => otherwise
            }
        ),
      opt[File]('i', "input")
        .text("Input file.")
        .action {
          case (file, config: Config.Broadcast) =>
            config.copy(input = Some(file.getAbsolutePath))
          case (_, otherwise) => otherwise
        },
      opt[File]('w', "wallet")
        .action {
          case (file, config: Config.Broadcast) =>
            config.copy(wallet = Some(file.getAbsolutePath))
          case (_, otherwise) => otherwise
        },
      opt[Long]('l', "limit")
        .text("Watt limit (300 by default).")
        .action {
          case (limit, config: Config.Broadcast) =>
            config.copy(wattLimit = limit)
          case (_, otherwise) => otherwise
        },
      opt[Long]('p', "price")
        .text("Watt price (1 by default).")
        .action {
          case (price, config: Config.Broadcast) =>
            config.copy(wattPrice = NativeCoin @@ price)
          case (_, otherwise) => otherwise
        },
      opt[String]('e', "endpoint")
        .text("Node endpoint (http://localhost:8080/api/public/broadcast by default).")
        .action {
          case (endpoint, config: Config.Broadcast) =>
            config.copy(endpoint = endpoint)
          case (_, otherwise) => otherwise
        },
    )

  cmd("node")
    .text("Control Pravda Network Node using CLI.")
    .action((_, _) => Config.Node(Config.Node.Mode.Nope, None))
    .children(
      cmd("init")
        .text("Initialize node.")
        .action {
          case (_, config: Config.Node) =>
            config.copy(mode = Config.Node.Mode.Init(Config.Node.Network.Local))
          case (_, otherwise) => otherwise
        }
        .children(
          opt[Unit]("local")
            .action {
              case (_, config: Config.Node) =>
                config.copy(mode = Config.Node.Mode.Init(Config.Node.Network.Local))
              case (_, otherwise) => otherwise
            },
          opt[Unit]("testnet")
            .action {
              case (_, config: Config.Node) =>
                config.copy(mode = Config.Node.Mode.Init(Config.Node.Network.Testnet))
              case (_, otherwise) => otherwise
            }
        ),
      cmd("run")
        .text("Run initialized node.")
        .action {
          case (_, config: Config.Node) =>
            config.copy(mode = Config.Node.Mode.Run)
          case (_, otherwise) => otherwise
        },
      opt[File]('d', "data-dir")
        .action {
          case (dataDir, config: Config.Node) =>
            config.copy(dataDir = Some(dataDir.getAbsolutePath))
          case (_, otherwise) => otherwise
        },
    )

  override def showUsageOnError: Boolean = false
}
