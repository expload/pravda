package pravda.dotnet.translation

import pravda.dotnet.parsers.Signatures._

object MethodTranslationUtils {

  def methodType(sig: Signature): Option[SigType] =
    sig match {
      case MethodRefDefSig(_, _, _, _, 0, Tpe(tpe, _), _) => Some(tpe)
      case _                                              => None
    }

  def methodParams(sig: Signature): Option[List[Tpe]] =
    sig match {
      case MethodRefDefSig(_, _, _, _, 0, Tpe(tpe, _), params) => Some(params)
      case _                                                   => None
    }

  def methodParamsCount(sig: Signature): Int =
    methodParams(sig).map(_.length).getOrElse(0)

  def isMethodVoid(sig: Signature): Boolean = methodType(sig) match {
    case Some(SigType.Void) => true
    case _                  => false
  }
}
