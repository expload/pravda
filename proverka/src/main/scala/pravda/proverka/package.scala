package pravda
import fastparse.StringReprOps
import pravda.proverka.Proverka.{Error, Text}
import pravda.proverka.Proverka._
import fastparse.all._

package object proverka {

  private def parseEither[T](text: String, p: P[T]): Either[String, T] = {
    p.parse(text) match {
      case Parsed.Success(c, _) =>
        Right(c)
      case Parsed.Failure(_, index, extra) =>
        val in = extra.input
        def aux(start: Int, i: Int, lim: Int): String = {
          if (lim > 0 && i < text.length
              && text.charAt(i) != '\n'
              && text.charAt(i) != '\r'
              && text.charAt(i) != ' ') aux(start, i + 1, lim - 1)
          else text.substring(start, i - 1)
        }
        val pos = StringReprOps.prettyIndex(in, index)
        val found = aux(index, index, 20)
        Left(s"$pos: ${extra.traced.expected} expected but '$found' found.")
    }
  }

  def input[State](name: String)(parse: Text => Either[Error, State => State]): InputPart[State] =
    InputPart(name, parse)

  def parserInput[State](name: String)(parser: P[State => State]): InputPart[State] =
    input(name) { text =>
      parseEither(text, parser)
    }

  def textOutput[State](name: String)(produce: State => Either[Error, Text]): OutputPart[State] =
    OutputPart(name, produce)

  def output[State, T](name: String)(produce: State => Either[Error, T]): OutputPart[State] =
    textOutput(name) { s =>
      for {
        p <- produce(s)
      } yield pprint.apply(p).plainText
    }
}
