package pravda.cli

import pravda.common.domain.NativeCoin

sealed trait PravdaConfig

object PravdaConfig {

  final val DefaultExecutor = "e74b91ee9dda326116a08703eb387cc27a47e5d832072346fd65c40b89629b86"

  sealed trait CompileMode

  object CompileMode {
    case object Nope   extends CompileMode
    case object Asm    extends CompileMode
    case object Disasm extends CompileMode
    case object DotNet extends CompileMode
  }

  case object Nope extends PravdaConfig

  final case class GenAddress(output: Option[String] = None) extends PravdaConfig

  final case class Broadcast(mode: Broadcast.Mode = Broadcast.Mode.Nope,
                             wallet: Option[String] = None,
                             input: Option[String] = None,
                             wattLimit: Long = 300,
                             wattPrice: NativeCoin = NativeCoin.amount(1),
                             endpoint: String = "http://localhost:8080/api/public/broadcast")
      extends PravdaConfig

  object Broadcast {

    sealed trait Mode

    object Mode {
      case object Nope                                                    extends Mode
      case object Deploy                                                  extends Mode
      case object Run                                                     extends Mode
      final case class Update(program: Option[String])                    extends Mode
      final case class Transfer(to: Option[String], amount: Option[Long]) extends Mode
    }
  }

  final case class Compile(compiler: CompileMode, input: Option[String] = None, output: Option[String] = None)
      extends PravdaConfig

  final case class RunBytecode(storage: Option[String] = None,
                               input: Option[String] = None,
                               executor: String = DefaultExecutor)
      extends PravdaConfig

  object Node {

    sealed trait Network

    object Network {
      final case class Local(coinDistribution: Option[String]) extends Network

      case object Testnet extends Network
    }

    sealed trait Mode

    object Mode {
      case object Nope extends Mode
      case object Run  extends Mode

      final case class Init(network: Network) extends Mode
    }
  }

  final case class Node(mode: Node.Mode, dataDir: Option[String]) extends PravdaConfig

  sealed trait CodegenMode

  object CodegenMode {
    case object Dotnet extends CodegenMode
  }

  final case class Codegen(codegenMode: CodegenMode,
                           input: Option[String] = None,
                           outDir: Option[String] = None,
                           excludeBigInteger: Boolean = false)
      extends PravdaConfig
}
