package ai.verta.modeldb.common.futures;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

public interface RetryStrategy {
  Retry shouldRetry(Throwable throwable);

  static RetryStrategy backoff(Function<Throwable, Boolean> exceptionChecker, int maxRetries) {
    return new DoublingBackoffStrategy(exceptionChecker, maxRetries);
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
