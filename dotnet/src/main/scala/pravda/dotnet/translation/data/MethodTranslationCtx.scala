package pravda.dotnet.translation.data

import pravda.dotnet.parsers.CIL.CilData
import pravda.dotnet.parsers.Signatures

final case class MethodTranslationCtx(argsCount: Int,
                                      localsCount: Int,
                                      name: String,
                                      signatures: Map[Long, Signatures.Signature],
                                      cilData: CilData,
                                      local: Boolean,
                                      void: Boolean)
