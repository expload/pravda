package pravda.cli

sealed trait Config

object Config {

  sealed trait Output

  object Output {
    case object Stdout                    extends Output
    final case class OsFile(path: String) extends Output
  }

  case object Nope                                            extends Config
  final case class GenAddress(output: Output = Output.Stdout) extends Config
  final case class RunBytecode(storage: Option[String] = None,
                               input: Option[String] = None,
                               executor: String = "e74b91ee9dda326116a08703eb387cc27a47e5d832072346fd65c40b89629b86")
      extends Config
}
