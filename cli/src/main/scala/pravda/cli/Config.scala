package pravda.cli

import pravda.common.domain.NativeCoin

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
                             wattPrice: NativeCoin = NativeCoin.amount(1),
                             endpoint: String = "http://localhost:8080/api/public/broadcast")
      extends Config

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
      final case class Init(network: Network, initDistrConf: Option[String]) extends Mode
      case object Run                         extends Mode
    }
  }

  final case class Node(mode: Node.Mode, dataDir: Option[String]) extends Config
}
