package pravda.cli

import cats._
import cats.data.EitherT
import cats.implicits._

import scala.language.higherKinds

package object programs {

  def usePath[F[_]: Monad, T](
      maybePath: Option[String])(none: => F[T], some: String => F[Either[String, T]]): EitherT[F, String, T] = {
    EitherT[F, String, T] {
      maybePath.fold[F[Either[String, T]]](none.map(Right.apply))(some)
    }
  }
}
