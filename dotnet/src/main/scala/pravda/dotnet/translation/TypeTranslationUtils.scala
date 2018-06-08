package pravda.dotnet.translation

import pravda.dotnet.data.TablesData.TypeDefData
import pravda.dotnet.parsers.Signatures.SigType._
import pravda.dotnet.parsers.Signatures._

object TypeTranslationUtils {
  def detectMapping(sig: Signature): Boolean = {
    sig match {
      case TypeSig(Tpe(Generic(Cls(TypeDefData(_, "Mapping`2", "io.mytc.pravda", _, _, _)), _), _)) => true
      case _ => false
    }
  }

}
