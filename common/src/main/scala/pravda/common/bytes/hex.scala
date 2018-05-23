package pravda.common.bytes

import contextual._

object hex {

  case object EmptyContext extends Context

  object HexParser extends Interpolator {

    type Output = Array[Byte]
    type ContextType = EmptyContext.type
    type Input = Array[Byte]

    def contextualize(interpolation: StaticInterpolation): Seq[ContextType] = {

      interpolation.parts.foldLeft(List.empty[ContextType]) {
        case (ctxs, lit @ Literal(index, string)) =>
          val hexString = string.replaceAll("\\s", "")

          val invalidDigits = hexString.zipWithIndex.filterNot {
            case (ch, _) =>
              val lowerCh = ch.toLower
              !(lowerCh < 48 || (lowerCh > 57 && lowerCh < 97) || lowerCh > 102)
          }

          invalidDigits.foreach {
            case (ch, idx) =>
              interpolation.error(lit, idx, "bad hexadecimal digit")
          }

          if (invalidDigits.nonEmpty) interpolation.abort(lit, 0, "hexadecimal string has invalid digits")

          if (hexString.length % 2 != 0) interpolation.abort(lit, 0, "hexadecimal size is not an exact number of bytes")

          ctxs

        case (ctxs, Hole(index, input)) =>
          EmptyContext :: ctxs
      }
    }

    override def evaluator(contexts: Seq[ContextType],
                           interpolation: StaticInterpolation): interpolation.macroContext.Tree = {

      import interpolation.macroContext.universe.{Literal => _, _}

      val arrays = interpolation.parts.map {
        case lit @ Literal(_, string) =>
          val bytes = string
            .replaceAll("\\s", "")
            .grouped(2)
            .map(Integer.parseInt(_, 16).toByte)
            .map { byte =>
              q"$byte"
            }
            .toList
          q"Array[Byte](..$bytes)"
        case hole @ Hole(index, _) =>
          interpolation.holeTrees(index) match {
            case Apply(Apply(_, List(value)), List(embedder)) =>
              val cls = contexts(index).getClass
              val init :+ last = cls.getName.dropRight(1).split("\\.").to[Vector]

              val elements = init ++ last.split("\\$").to[Vector]

              val selector = elements.foldLeft(q"_root_": Tree) {
                case (t, p) =>
                  Select(t, TermName(p))
              }
              q"$embedder($selector).apply($value)"
          }
      }

      if (arrays.length == 1) {
        arrays.head
      } else {
        q"Array.concat(..$arrays)"
      }
    }

  }

  implicit class HexStringContext(sc: StringContext) { val hex = Prefix(HexParser, sc) }

  implicit val embedHexByte
    : Embedder[(hex.EmptyContext.type, hex.EmptyContext.type), Byte, Array[Byte], hex.HexParser.type] =
    hex.HexParser.embed[Byte] {
      Case(EmptyContext, EmptyContext)(Array(_))
    }

  implicit val embedHexByteArray
    : Embedder[(hex.EmptyContext.type, hex.EmptyContext.type), Array[Byte], Array[Byte], hex.HexParser.type] =
    hex.HexParser.embed[Array[Byte]] {
      Case(EmptyContext, EmptyContext)(identity)
    }

}
