package pravda.dotnet.translation.data

import pravda.dotnet.parsers.CIL

sealed trait TranslationError
case object UnknownOpcode                   extends TranslationError
final case class NotSupportedOpcode(op: CIL.Op) extends TranslationError
final case class InternalError(err: String) extends TranslationError
