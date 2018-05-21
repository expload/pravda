package pravda.node.utils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

final class Var[T](private var value: T) extends Var.VarReader[T] with Var.VarWriter[T] {

  private var lock = Option.empty[Future[T]]

  def set(value: T)(implicit ec: ExecutionContext): Future[Unit] = this.synchronized {
    lock match {
      case Some(future) =>
        future.map { _ =>
          lock = None
          this.value = value
        }
      case None =>
        this.value = value
        Future.unit
    }
  }

  def get(): Future[T] = this.synchronized {
    lock match {
      case Some(future) => future
      case None         => Future.successful(value)
    }
  }

  def update(f: T => T)(implicit ec: ExecutionContext): Future[T] = this.synchronized {
    val future = lock match {
      case None             => Future.successful(f(value))
      case Some(prevFuture) => prevFuture.map(f)
    }
    lock = Some(future)
    future.andThen {
      case Success(x) =>
        lock = None
        value = x
    }
  }

  def updateAsync(f: T => Future[T])(implicit ec: ExecutionContext): Future[T] = this.synchronized {
    val future = lock match {
      case None             => f(value)
      case Some(prevFuture) => prevFuture.flatMap(f)
    }
    lock = Some(future)
    future.andThen {
      case Success(x) =>
        lock = None
        value = x
    }
  }
}

object Var {

  sealed trait VarReader[+T] {
    def get(): Future[T]
  }

  sealed trait VarWriter[T] {
    def set(value: T)(implicit ec: ExecutionContext): Future[Unit]
    def update(f: T => T)(implicit ec: ExecutionContext): Future[T]
    def updateAsync(f: T => Future[T])(implicit ec: ExecutionContext): Future[T]
  }

  def apply[T](value: T) = new Var[T](value)
}
