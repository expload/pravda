package pravda.dotnet.translation.data

import pravda.dotnet.parsers.Signatures

final case class MethodTranslationCtx(argsCount: Int,
                                localsCount: Int,
                                name: String,
                                signatures: Map[Long, Signatures.Signature],
                                local: Boolean,
                                void: Boolean)
