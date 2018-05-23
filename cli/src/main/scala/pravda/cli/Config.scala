package pravda.cli

sealed trait Config

object Config {

  final val DefaultExecutor = "e74b91ee9dda326116a08703eb387cc27a47e5d832072346fd65c40b89629b86"

  sealed trait PravdaCompile

  object PravdaCompile {
    case object Nope   extends PravdaCompile
    case object Asm    extends PravdaCompile
    case object Disasm extends PravdaCompile
    case object Forth  extends PravdaCompile
    case object DotNet extends PravdaCompile
  }

  case object Nope extends Config

  final case class GenAddress(output: Option[String] = None) extends Config

  final case class Compile(compiler: PravdaCompile, input: Option[String] = None, output: Option[String] = None)
      extends Config

  final case class RunBytecode(storage: Option[String] = None,
                               input: Option[String] = None,
                               executor: String = DefaultExecutor)
      extends Config
}
