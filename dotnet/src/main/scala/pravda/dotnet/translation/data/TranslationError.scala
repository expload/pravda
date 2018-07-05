package pravda.dotnet.translation.data

sealed trait TranslationError
case object UnknownOpcode                   extends TranslationError
final case class InternalError(err: String) extends TranslationError
