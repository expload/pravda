package pravda.cli

import java.io.File

import pravda.cli.Config.CompileMode
import pravda.common.bytes
import pravda.common.domain.NativeCoin
import pravda.cmdopt._

object ArgumentsParser extends CommandLine[Config] {

  def model = List(
    head("pravda")
      .title("Pravda - blockchain SDK for games.")
      .text("pravda COMMAND [...SUBCOMMAND]")
      .desc("""
      |pravda is a unified command line interface to Pravda SDK.
    """),
    cmd("gen")
      .text("Useful stuff generators.")
      .children(
        head("pravda-gen")
          .title("A bunch of usefull generators.")
          .text("pravda gen [...SUBCOMMAND]")
          .desc("Generate addresses and documentation."),
        cmd("address")
          .text("Generate ed25519 key pair. It can be used as regular wallet or validator node identifier.")
          .action(_ => Config.GenAddress())
          .children(
            head("pravda-gen-address")
              .title("Generate public/private key.")
              .text("pravda gen address")
              .desc("Generate ed25519 conforming addresses. They can be used as regular wallet or validator node identifier."),
            opt[File]('o', "output")
              .text("Output file")
              .action {
                case (file, Config.GenAddress(_)) =>
                  Config.GenAddress(Some(file.getAbsolutePath))
                case (_, otherwise) => otherwise
              }
          ),
        cmd("docs")
          .text("Generate markdown documentation for command line tool.")
          .action(_ => Config.GenDocs(cl = ArgumentsParser))
          .children(
            head("pravda-gen-docs")
              .title("Generate markdown documentation.")
              .text("pravda gen docs")
              .desc("""
              |Generate and write to a given folder (docs/ref/ by default)
              |comprehensive markdown docs for CLI.
            """),
            opt[File]('o', "output")
              .text("Output directory")
              .action {
                case (file, Config.GenDocs(outDir, mainPageName, cl)) =>
                  Config.GenDocs(file.getAbsolutePath, mainPageName, cl)
                case (_, otherwise) => otherwise
              }
          )
      ),
    cmd("run")
      .text("Run bytecode given from stdin or file on Pravda VM.")
      .action(_ => Config.RunBytecode())
      .children(
        head("pravda-run")
          .title("Run and debug Pravda programs.")
          .text("pravda run")
          .desc("""
          |Run bytecode given from stdin or file on Pravda VM.
        """),
        opt[String]('e', "executor")
          .validate { (s: String) =>
            s match {
              case s if bytes.isHex(s) && s.length == 64 => Right(())
              case s                                     => Left(s"`$s` is not valid address. It should be 32 bytes hex string.")
            }
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
      ),
    cmd("compile")
      .text("Compile Pravda programs.")
      .action(_ => Config.Compile(CompileMode.Nope))
      .children(
        head("Compilation")
          .title("Compile Pravda programs.")
          .text("pravda compile [...SUBCOMMAND]")
          .desc("""
          |Compile Pravda programs from different sources
        """),
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
          .action(_ => Config.Compile(Config.CompileMode.Asm))
          .children(
            head("pravda-compile-asm")
              .title("Assemble Pravda VM bytecode from text representation.")
              .text("pravda compile asm")
              .desc("""
              |Input file is a Pravda assembly language text file. Output is binary Pravda
              |program. By default read from stdin and print to stdout.
            """)
          ),
        cmd("disasm")
          .text("Disassemble Pravda VM bytecode to text presentation.")
          .action(_ => Config.Compile(Config.CompileMode.Disasm))
          .children(
            head("pravda-compile-disasm")
              .title("Disassemble Pravda programs.")
              .text("pravda compile disasm [--input <filename>] [--output <filename>]")
              .desc("""
              |Input file is a Pravda executable binary. Output is a text file with
              |Pravda assembly code. By default read from stdin and print to stdout.
            """)
          ),
        cmd("forth")
          .text("Compile Pravda pseudo-forth to Pravda VM bytecode.")
          .action(_ => Config.Compile(Config.CompileMode.Forth))
          .children(
            head("pravda-compile-disasm")
              .title("Disassemble Pravda programs.")
              .text("pravda compile disasm [--input <filename>] [--output <filename>]")
              .desc("""
              |Input file is a Pravda executable binary. Output is a text file with
              |Pravda assembly code. By default read from stdin and print to stdout.
            """)
          ),
        cmd("dotnet")
          .text("Compile .exe produced byt .NET compiler to Pravda VM bytecode.")
          .action(_ => Config.Compile(Config.CompileMode.DotNet))
          .children(
            head("pravda-compile-dotnet")
              .title("Compile .Net PE executable to Pravda executable binary.")
              .text("pravda compile dotnet [--input <filename>] [--output <filename>]")
              .desc("""
              |Input file is a .Net PE (portable executable). Output is binary Pravda
              |program. By default read from stdin and print to stdout.
            """)
          )
      ),
    cmd("broadcast")
      .text("Broadcasting program to the network")
      .children(
        head("pravda-broadcast")
          .title("Send a transaction with Pravda Program to a blockchain.")
          .text("pravda broadcast SUBCOMMAND [...OPTIONS]")
          .desc(""),
        cmd("run")
          .action(_ => Config.Broadcast(Config.Broadcast.Mode.Run))
          .text("Run pointed program.")
          .children(
            head("pravda-broadcast-run")
              .title("Send a transaction with Pravda Program address to blockchain to run it.")
              .text("pravda broadcast run [--input <filename>] [--output <filename>]")
              .desc("""
            """)
          ),
        cmd("transfer")
          .text("Pravda is a unified command line interface to Pravda SDK.")
          .action(_ => Config.Broadcast(Config.Broadcast.Mode.Transfer(None, None)))
          .children(
            head("pravda-broadcast-transfer")
              .title("Transfer native coins to a given wallet.")
              .text("pravda broadcast transfer --to <wallet> --amount <amount>")
              .desc("""
              |Transfer native coins to a wallet.
            """),
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
          .text("Deploy Pravda program to a blockchain.")
          .action(_ => Config.Broadcast(Config.Broadcast.Mode.Deploy))
          .children(
            head("pravda-broadcast-deploy")
              .title("Deploy Pravda program to a blockchain.")
              .text("pravda broadcast deploy")
              .desc("""
            """)
          ),
        cmd("update")
          .text("Update existing Pravda program in a blockchain.")
          .action(_ => Config.Broadcast(Config.Broadcast.Mode.Update(None)))
          .children(
            head("pravda-broadcast-update")
              .title("Update existing Pravda program in a blockchain.")
              .text("pravda broadcast update")
              .desc("""
            """),
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
      ),
    cmd("node")
      .text("Control Pravda Network Node using CLI.")
      .action(_ => Config.Node(Config.Node.Mode.Nope, None))
      .children(
        head("pravda-node")
          .title("Control Pravda node.")
          .text("pravda node SUBCOMMAND")
          .desc("""
        """),
        cmd("init")
          .text("Initialize node.")
          .action(_ => Config.Node(Config.Node.Mode.Init(Config.Node.Network.Local, None), None))
          .children(
            head("pravda-node-init")
              .title("Create data directory and configuration for a new node.")
              .text("pravda node init")
              .desc("""
            """),
            opt[Unit]("local")
              .action {
                case (_, config @ Config.Node(Config.Node.Mode.Init(_, initDistrConf), _)) =>
                  config.copy(mode = Config.Node.Mode.Init(Config.Node.Network.Local, initDistrConf))
                case (_, otherwise) => otherwise
              },
            opt[Unit]("testnet")
              .action {
                case (_, config @ Config.Node(Config.Node.Mode.Init(_, initDistrConf), _)) =>
                  config.copy(mode = Config.Node.Mode.Init(Config.Node.Network.Testnet, initDistrConf))
                case (_, otherwise) => otherwise
              },
            opt[String]("init-distr-conf")
              .action {
                case (initDistrConf, config @ Config.Node(Config.Node.Mode.Init(network, _), _)) =>
                  config.copy(mode = Config.Node.Mode.Init(network, Some(initDistrConf)))
                case (_, otherwise) => otherwise
              }
          ),
        cmd("run")
          .text("Run initialized node.")
          .action(_ => Config.Node(Config.Node.Mode.Run, None))
          .children(
            head("pravda-node-run")
              .title("Create data directory and configuration for a new node.")
              .text("pravda node init")
              .desc("""
            """),
          ),
        opt[File]('d', "data-dir")
          .action {
            case (dataDir, config: Config.Node) =>
              config.copy(dataDir = Some(dataDir.getAbsolutePath))
            case (_, otherwise) => otherwise
          },
      )
  )
}
