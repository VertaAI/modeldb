package ai.verta.modeldb.common.futures;

import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

public interface RetryStrategy<T> {
  Retry shouldRetry(T result, Throwable throwable);

  static <R> RetryStrategy<R> backoff(BiPredicate<R, Throwable> retryPredicate, int maxRetries) {
    return new DoublingBackoffStrategy<>(retryPredicate, maxRetries);
  }

  @Value
  class Retry {
    @Getter(AccessLevel.NONE)
    boolean should;

    int amountToDelay;
    TimeUnit timeUnit;

    public boolean shouldRetry() {
      return should;
    }
  }
}
