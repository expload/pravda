package pravda.cmdopt

import utest._

object PravdaCommandLineTest extends TestSuite {

  import pravda.cmdopt.instances.show.console._

  val tests = Tests {

    "parse" - {
      println( PravdaCommandLine.help() )
      println( PravdaCommandLine.help("compile") )
    }

  }

  sealed trait Config

  object Config {

    final val DefaultExecutor = "e74b91ee9dda326116a08703eb387cc27a47e5d832072346fd65c40b89629b86"

    sealed trait CompileMode

    object CompileMode {
      case object Nope   extends CompileMode
      case object Asm    extends CompileMode
      case object Disasm extends CompileMode
      case object Forth  extends CompileMode
      case object DotNet extends CompileMode
    }

    case object Nope extends Config

    final case class GenAddress(output: Option[String] = None) extends Config

    final case class Broadcast(mode: Broadcast.Mode = Broadcast.Mode.Nope,
                               wallet: Option[String] = None,
                               input: Option[String] = None,
                               wattLimit: Long = 300,
                               endpoint: String = "http://localhost:8080/api/public/broadcast")
        extends Config

    object Broadcast {

      sealed trait Mode

      object Mode {
        case object Nope                                 extends Mode
        case object Deploy                               extends Mode
        case object Run                                  extends Mode
        final case class Update(program: Option[String]) extends Mode
      }
    }

    final case class Compile(compiler: CompileMode, input: Option[String] = None, output: Option[String] = None)
        extends Config

    final case class RunBytecode(storage: Option[String] = None,
                                 input: Option[String] = None,
                                 executor: String = DefaultExecutor)
        extends Config

    object Node {

      sealed trait Network

      object Network {
        case object Local   extends Network
        case object Testnet extends Network
      }

      sealed trait Mode

      object Mode {
        case object Nope                        extends Mode
        final case class Init(network: Network) extends Mode
        case object Run                         extends Mode
      }
    }

    final case class Node(mode: Node.Mode, dataDir: Option[String]) extends Config
  }

  import java.io.File

  object PravdaCommandLine extends CommandLine[Config] { def model = List(

    head("pravda")
      .text("Usage:  pravda COMMAND [...COMMAND]")
      .desc("Superficial blockchain platform"),

    opt[Unit]('h', "help")
      .text("Show this help"),

    opt[Unit]('v', "verbose")
      .text("Lot's and lot's of talks"),

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
      ),

    cmd("run")
      .text("Run bytecode given from stdin or file on Pravda VM.")
      .action((_, _) => Config.RunBytecode())
      .children(
        opt[String]('e', "executor")
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
      .action((_, _) => Config.Compile(Config.CompileMode.Nope))
      .children(

        head("Compilation related stuff")
          .text("Usage:  pravda compile COMMAND --input --output")
          .desc("Compile and debug programs"),

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
      ),

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
      ),
    )
  }
}
