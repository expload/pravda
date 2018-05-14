//package io.mytc.timechain.utils
//
//import scala.language.higherKinds
//
//case class EitherT[F[_], L, R](v: F[Either[L, R]]) {
//  def flatMap(f: R => EitherT[F, L, R]): EitherT[F, L, R] =
//    f()
//}
