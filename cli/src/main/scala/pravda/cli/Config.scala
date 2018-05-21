package pravda.cli

sealed trait Config

object Config {

  sealed trait Output

  object Output {
    case object Stdout                    extends Output
    final case class OsFile(path: String) extends Output
  }

  object Nope                                                 extends Config
  final case class GenAddress(output: Output = Output.Stdout) extends Config
}
