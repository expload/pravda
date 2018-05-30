package pravda.cli

import cats._
import cats.data.EitherT
import cats.implicits._

import scala.language.higherKinds

package object programs {

  def useOption[F[_]: Monad, A, B](maybe: Option[A])(none: => F[B],
                                                     some: A => F[Either[String, B]]): EitherT[F, String, B] = {
    EitherT[F, String, B] {
      maybe.fold[F[Either[String, B]]](none.map(Right.apply))(some)
    }
  }
}
