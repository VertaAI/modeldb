import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}


trait CachedEntity[T] {
  // T is type of Message

  // Needs to supply these when create subclass:
  protected var cachedMessage: Try[T] // current version of the cached message
  protected def fetchMessage()(implicit ec: ExecutionContext): Try[T] // fetch message, without side effect
  protected var cachedTime: Try[Long] // time of last cache

  private def getMessage()(implicit ec: ExecutionContext): Try[T] = {
    val curTime = System.currentTimeMillis()

    if (cachedTime.isFailure || curTime - cachedTime.get > 5000) {
      fetchMessage().flatMap(message => {
        cachedTime = Success(curTime)
        cachedMessage = Success(message)

        cachedMessage
      })
    }
    else
      cachedMessage
  }

  private def clearCache(): Unit = {
    cachedMessage = Failure(new IllegalStateException("Cache has been cleared."))
    cachedTime = Failure(new IllegalStateException("Cache has been cleared."))
  }
}
