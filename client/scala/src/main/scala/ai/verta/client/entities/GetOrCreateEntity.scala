package ai.verta.client.entities

import ai.verta.swagger.client.{ClientSet, HttpException}
import sttp.model.StatusCode

import scala.util.{Failure, Success, Try}

object GetOrCreateEntity {
  def getOrCreate[T](get: () => Try[T], create: () => Try[T]): Try[T] = {
    create() match {
      case Success(x) => Success(x)
      case Failure(except: HttpException) => {
        if (except.code == StatusCode.Conflict)
          get()
        else
          Failure(except)
      }
      case default => default
    }
  }
}
