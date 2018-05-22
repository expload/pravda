package pravda.cli

sealed trait Config

object Config {

  sealed trait Output

  object Output {
    case object Stdout                    extends Output
    final case class OsFile(path: String) extends Output
  }

  sealed trait Input

  object Input {
    case object Stdin                     extends Input
    final case class OsFile(path: String) extends Input
  }

  case object Nope                                                                          extends Config
  final case class GenAddress(output: Output = Output.Stdout)                               extends Config
  final case class TestBytecode(storage: Option[String] = None, input: Input = Input.Stdin) extends Config
}
