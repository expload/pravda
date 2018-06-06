package pravda.dotnet

import fastparse.byte.all._
import fastparse.core.Parsed._

package object utils {
  private[dotnet] def nullTerminatedString(len: Int): P[String] =
    AnyBytes(len).!.map(bs => new String(bs.takeWhile(_ != 0).toArray))

  private[dotnet] val nullTerminatedString: P[String] =
    P(BytesWhile(_ != 0, min = 0).! ~ BS(0)).map(bs => new String(bs.toArray))

  private[dotnet] implicit class ParserOps[T](p: Parsed[T]) {

    def toEither: Either[String, T] = p match {
      case Success(t, _)        => Right(t)
      case f @ Failure(_, _, _) => Left(s"An error occurred during parsing: ${f.extra.traced.traceParsers}")
    }
  }
}
