package ai.verta.modeldb.common.futures;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

@Value
@Getter(AccessLevel.NONE)
class DoublingBackoffStrategy<R> implements RetryStrategy<R> {
  private static final int INITIAL_DELAY_MILLIS = 10;
  BiFunction<R, Throwable, Boolean> resultChecker;
  int maxRetries;
  AtomicInteger numberRetried = new AtomicInteger();

  @Override
  public Retry shouldRetry(R result, Throwable throwable) {
    Boolean apply = resultChecker.apply(result, throwable);
    if (!apply || numberRetried.get() == maxRetries) {
      return new Retry(false, 0, TimeUnit.SECONDS);
    }

    int nextDelay = INITIAL_DELAY_MILLIS * (int) Math.pow(2, numberRetried.getAndIncrement());
    return new Retry(true, nextDelay, TimeUnit.MILLISECONDS);
  }
}
