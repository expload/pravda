package io.mytc.sood.cil

import fastparse.byte.all._
import fastparse.core.Parsed._

package object utils {
  private[cil] def nullTerminatedString(len: Int): P[String] =
    AnyBytes(len).!.map(bs => new String(bs.takeWhile(_ != 0).toArray))

  private[cil] val nullTerminatedString: P[String] =
    P(BytesWhile(_ != 0, min = 0).! ~ BS(0)).map(bs => new String(bs.toArray))

  private[cil] def toEither[T](p: Parsed[T]): Either[String, T] = p match {
    case Success(t, _)        => Right(t)
    case f @ Failure(_, _, _) => Left(f.msg)
  }

  private[cil] def sequenceEither[L, R](seq: Seq[Either[L, R]]): Either[L, Seq[R]] =
    seq.foldRight(Right(Nil): Either[L, List[R]]) { (e, acc) =>
      for {
        xs <- acc
        x <- e
      } yield x +: xs
    }
}
