package ai.verta.client.entities.utils

import scala.util.{Failure, Success, Try}

object TryOptionConverter {
  def convert[T](v: Try[Option[T]]): Option[Try[T]] =
    v match {
      case Success(Some(x)) => Some(Success(x))
      case Success(None) => None
      case Failure(exception) => Some(Failure(exception))
    }
}
