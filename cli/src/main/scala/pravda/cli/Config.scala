package pravda.cli

sealed trait Config

object Config {

  sealed trait Output

  object Output {
    case object Stdout extends Output
    case class OsFile(path: String) extends Output
  }

  object Nope extends Config
  final case class GenWallet(output: Output = Output.Stdout) extends Config
}
