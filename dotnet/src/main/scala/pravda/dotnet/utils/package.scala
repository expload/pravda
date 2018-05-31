package pravda.dotnet

import fastparse.byte.all._
import fastparse.core.Parsed._

package object utils {
  private[dotnet] def nullTerminatedString(len: Int): P[String] =
    AnyBytes(len).!.map(bs => new String(bs.takeWhile(_ != 0).toArray))

  private[dotnet] val nullTerminatedString: P[String] =
    P(BytesWhile(_ != 0, min = 0).! ~ BS(0)).map(bs => new String(bs.toArray))

  private[dotnet] type Validated[T] = Either[String, T]
  private[dotnet] def validationError(msg: String): Validated[Nothing] = Left(msg)
  private[dotnet] def validated[T](t: T): Validated[T] = Right(t)

  private[dotnet] implicit class ValidatedSeqOps[T](vs: Seq[Validated[T]]) {

    def sequence: Validated[Seq[T]] = {
      val res = Seq.newBuilder[T]
      var i = 0
      while (i < vs.length && vs(i).isRight) {
        vs(i) match {
          case Right(r) => res += r
          case _        =>
        }
        i += 1
      }
      if (i < vs.length && vs(i).isLeft) {
        vs(i).asInstanceOf[Left[String, Nothing]]
      } else {
        Right(res.result())
      }
    }
  }

  private[dotnet] implicit class ParserOps[T](p: Parsed[T]) {

    def toValidated: Validated[T] = p match {
      case Success(t, _)        => Right(t)
      case f @ Failure(_, _, _) => Left(s"An error occurred during parsing: ${f.extra.traced.traceParsers}")
    }
  }

  private[dotnet] implicit class OptionOps[T](o: Option[T]) {
    def toValidated(err: String): Validated[T] = o match {
      case Some(x) => Right(x)
      case None => Left(err)
    }
  }
}