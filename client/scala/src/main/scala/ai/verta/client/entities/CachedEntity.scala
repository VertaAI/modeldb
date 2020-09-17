package ai.verta.client.entities

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}


trait CachedEntity[T] {
  // T is type of the cached message

  // Needs to supply these when create subclass:
  protected def fetchMessage()(implicit ec: ExecutionContext): Try[T] // fetch message, without side effect

  // current version of the cached message
  private var cachedMessage: Try[T] = Failure(new IllegalStateException("cached message is uninitialized."))
  private var cachedTime: Try[Long] = Failure(new IllegalStateException("cached message is uninitialized."))

  protected def getMessage()(implicit ec: ExecutionContext): Try[T] = {
    val now = System.currentTimeMillis()

    if (cachedTime.isFailure || now - cachedTime.get > 5000)
      refreshCache(now)

    cachedMessage
  }

  // refresh current cached value.
  private def refreshCache(now: Long)(implicit ec: ExecutionContext): Unit = fetchMessage() match {
    case Success(m) => updateCache(Success(m), Success(now))
    case Failure(e) => updateCache(Failure(e), Failure(e))
  }

  // call this when initialized object
  protected def updateCache(newMessage: Try[T], newTime: Try[Long]): Unit = {
    cachedMessage = newMessage
    cachedTime = newTime
  }

  // clear current cached value.
  protected def clearCache(): Unit = {
    cachedMessage = Failure(new IllegalStateException("Cache has been cleared."))
    cachedTime = Failure(new IllegalStateException("Cache has been cleared."))
  }
}
