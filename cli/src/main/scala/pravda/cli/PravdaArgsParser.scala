package pravda.cli

import java.io.File

import pravda.cli.PravdaConfig.{CompileMode, CodegenMode}
import pravda.common.bytes
import pravda.common.domain.NativeCoin
import pravda.yopt._

object PravdaArgsParser extends CommandLine[PravdaConfig] {

  val broadcastInput: CommandLine.Opt[PravdaConfig, File] =
    opt[File]('i', "input")
      .text("Input file.")
      .action {
        case (file, config: PravdaConfig.Broadcast) =>
          config.copy(input = Some(file.getAbsolutePath))
        case (_, otherwise) => otherwise
      }

  val model: CommandLine.Head[PravdaConfig] =
    head("pravda")
      .text("Pravda Command Line Interface to Pravda SDK")
      .children(
        cmd("gen")
          .text("Generate auxiliary data for Pravda.")
          .children(
            cmd("address")
              .text("Generate ed25519 key pair. It can be used as regular wallet or validator node identifier.")
              .action(_ => PravdaConfig.GenAddress())
              .children(
                opt[File]('o', "output")
                  .text("Output file")
                  .action {
                    case (file, PravdaConfig.GenAddress(_)) =>
                      PravdaConfig.GenAddress(Some(file.getAbsolutePath))
                    case (_, otherwise) => otherwise
                  }
              ),
            cmd("unity")
              .text("Generate auxiliary code to call program's methods from Unity")
              .action(_ => PravdaConfig.Codegen(CodegenMode.Dotnet))
              .children(
                opt[File]('d', "dir")
                  .text("Output directory for generated sources.")
                  .action {
                    case (f, conf: PravdaConfig.Codegen) => conf.copy(outDir = Some(f.getAbsolutePath))
                    case (_, otherwise)                  => otherwise
                  },
                opt[File]('i', "input")
                  .text("Input file with assembly.")
                  .action {
                    case (f, conf: PravdaConfig.Codegen) => conf.copy(input = Some(f.getAbsolutePath))
                    case (_, otherwise)                  => otherwise
                  },
                opt[Unit]("exclude-big-integer")
                  .text("Exclude custom BigInteger class.")
                  .action {
                    case ((), conf: PravdaConfig.Codegen) => conf.copy(excludeBigInteger = true)
                    case (_, otherwise)                   => otherwise
                  }
              )
          ),
        cmd("run")
          .text("Run byte-code on Pravda VM")
          .action(_ => PravdaConfig.RunBytecode())
          .children(
            opt[String]('e', "executor")
              .text("Executor address HEX representation")
              .validate {
                case s if bytes.isHex(s) && s.length == 64 => Right(())
                case s                                     => Left(s"`$s` is not valid address. It should be 32 bytes hex string.")
              }
              .action {
                case (address, config: PravdaConfig.RunBytecode) =>
                  config.copy(executor = address)
                case (_, otherwise) => otherwise
              },
            opt[File]('i', "input")
              .text("Input file")
              .action {
                case (file, config: PravdaConfig.RunBytecode) =>
                  config.copy(input = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            opt[File]("storage")
              .text("Storage name")
              .action {
                case (file, config: PravdaConfig.RunBytecode) =>
                  config.copy(storage = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              }
          ),
        cmd("compile")
          .text("Compile Pravda programs.")
          .action(_ => PravdaConfig.Compile(CompileMode.Nope))
          .children(
            opt[File]('i', "input")
              .text("Input file")
              .action {
                case (file, config: PravdaConfig.Compile) =>
                  config.copy(input = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            opt[File]('o', "output")
              .text("Output file")
              .action {
                case (file, config: PravdaConfig.Compile) =>
                  config.copy(output = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            cmd("asm")
              .text(
                "Assemble Pravda VM bytecode from text representation. " +
                  "Input file is a Pravda assembly language text file. " +
                  "Output is binary Pravda program. " +
                  "By default read from stdin and print to stdout.")
              .action(_ => PravdaConfig.Compile(PravdaConfig.CompileMode.Asm)),
            cmd("disasm")
              .text(
                "Disassemble Pravda VM bytecode to text presentation. " +
                  "Input file is a Pravda executable binary. " +
                  "Output is a text file with Pravda assembly code. " +
                  "By default read from stdin and print to stdout.")
              .action(_ => PravdaConfig.Compile(PravdaConfig.CompileMode.Disasm)),
            cmd("dotnet")
              .text(
                "Compile .exe produced by .NET compiler to Pravda VM bytecode. " +
                  "Input file is a .NET PE (portable executable). " +
                  "Output is binary Pravdaprogram. " +
                  "By default read from stdin and print to stdout")
              .action(_ => PravdaConfig.Compile(PravdaConfig.CompileMode.DotNet))
              .children(
                opt[Unit]("visualize").action {
                  case ((), config: PravdaConfig.Compile) =>
                    config.copy(compiler = PravdaConfig.CompileMode.DotNetVisualize)
                }
              )
          ),
        cmd("broadcast")
          .text("Broadcast transactions and programs to the Pravda blockchain.")
          .children(
            cmd("run")
              .text("Send a transaction with Pravda Program address to the blockchain to run it")
              .action(_ => PravdaConfig.Broadcast(PravdaConfig.Broadcast.Mode.Run))
              .children(broadcastInput),
            cmd("transfer")
              .text("Transfer native coins to a given wallet.")
              .action(_ => PravdaConfig.Broadcast(PravdaConfig.Broadcast.Mode.Transfer(None, None)))
              .children(
                opt[String]('t', "to")
                  .action {
                    case (hex,
                          config @ PravdaConfig.Broadcast(mode: PravdaConfig.Broadcast.Mode.Transfer, _, _, _, _, _)) =>
                      config.copy(mode = mode.copy(to = Some(hex)))
                    case (_, otherwise) => otherwise
                  },
                opt[Long]('a', "amount")
                  .action {
                    case (amount,
                          config @ PravdaConfig.Broadcast(mode: PravdaConfig.Broadcast.Mode.Transfer, _, _, _, _, _)) =>
                      config.copy(mode = mode.copy(amount = Some(amount)))
                    case (_, otherwise) => otherwise
                  }
              ),
            cmd("deploy")
              .text("Deploy Pravda program to the blockchain.")
              .action(_ => PravdaConfig.Broadcast(PravdaConfig.Broadcast.Mode.Deploy))
              .children(broadcastInput),
            cmd("update")
              .text("Update existing Pravda program in the blockchain.")
              .action(_ => PravdaConfig.Broadcast(PravdaConfig.Broadcast.Mode.Update(None)))
              .children(
                broadcastInput,
                opt[String]('p', "program")
                  .text("Address of the program to update")
                  .action {
                    case (hex, config: PravdaConfig.Broadcast) =>
                      config.copy(mode = PravdaConfig.Broadcast.Mode.Update(Some(hex)))
                    case (_, otherwise) => otherwise
                  }
              ),
            opt[File]('w', "wallet")
              .text("File with user wallet. You can obtain it using 'pravda gen address' command. Format: {\"address\": <public key>, \"privateKey\": <private key>}")
              .action {
                case (file, config: PravdaConfig.Broadcast) =>
                  config.copy(wallet = Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            opt[Long]('l', "limit")
              .text("Watt limit (300 by default).") // FIXME what to do with default values?
              .action {
                case (limit, config: PravdaConfig.Broadcast) =>
                  config.copy(wattLimit = limit)
                case (_, otherwise) => otherwise
              },
            opt[Long]('p', "price")
              .text("Watt price (1 by default).") // FIXME what to do with default values?
              .action {
                case (price, config: PravdaConfig.Broadcast) =>
                  config.copy(wattPrice = NativeCoin @@ price)
                case (_, otherwise) => otherwise
              },
            opt[String]('e', "endpoint")
              .text("Node endpoint (http://localhost:8080/api/public/broadcast by default).") // FIXME what to do with default values?
              .action {
                case (endpoint, config: PravdaConfig.Broadcast) =>
                  config.copy(endpoint = endpoint)
                case (_, otherwise) => otherwise
              },
          ),
        cmd("node")
          .text("Control Pravda Network Node.")
          .action(_ => PravdaConfig.Node(PravdaConfig.Node.Mode.Nope, None))
          .children(
            opt[File]('d', "data-dir")
              .action {
                case (dataDir, config: PravdaConfig.Node) =>
                  config.copy(dataDir = Some(dataDir.getAbsolutePath))
                case (_, otherwise) => otherwise
              },
            cmd("init")
              .text("Create data directory and configuration for a new node.")
              .action(_ => PravdaConfig.Node(PravdaConfig.Node.Mode.Init(PravdaConfig.Node.Network.Local(None)), None))
              .children(
                opt[Unit]("local")
                  .text("Initialize local node with self-validation.")
                  .action {
                    case (_, config: PravdaConfig.Node) =>
                      config.copy(mode = PravdaConfig.Node.Mode.Init(PravdaConfig.Node.Network.Local(None)))
                    case (_, otherwise) => otherwise
                  },
//                opt[Unit]("testnet")
//                  .action {
//                    case (_, config: PravdaConfig.Node) =>
//                      config.copy(mode = PravdaConfig.Node.Mode.Init(PravdaConfig.Node.Network.Testnet))
//                    case (_, otherwise) => otherwise
//                  },
                opt[String]("coin-distribution")
                  .text("Initialize local node with addresses which have some amount of coins at initial state. JSON file. Format: [{\"address\":<public key in hex>,\"amount\":<number>}]")
                  .action {
                    case (coinDistribution,
                          config @ PravdaConfig
                            .Node(PravdaConfig.Node.Mode.Init(local: PravdaConfig.Node.Network.Local), _)) =>
                      config.copy(mode = PravdaConfig.Node.Mode.Init(local.copy(Some(coinDistribution))))
                    case (_, otherwise) => otherwise
                  }
              ),
            cmd("run")
              .text("Run initialized node.")
              .action(_ => PravdaConfig.Node(PravdaConfig.Node.Mode.Run, None))
          )
      )
}
