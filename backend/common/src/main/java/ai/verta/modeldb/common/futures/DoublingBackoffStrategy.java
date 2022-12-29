package ai.verta.modeldb.common.futures;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

@Value
@Getter(AccessLevel.NONE)
class DoublingBackoffStrategy<R> implements RetryStrategy<R> {
  private static final int INITIAL_DELAY_MILLIS = 10;
  BiPredicate<R, Throwable> resultChecker;
  int maxRetries;
  AtomicInteger numberRetried = new AtomicInteger();

  @Override
  public Retry shouldRetry(R result, Throwable throwable) {
    boolean apply = resultChecker.test(result, throwable);
    if (!apply || numberRetried.get() == maxRetries) {
      return new Retry(false, 0, TimeUnit.SECONDS);
    }

    int nextDelay = INITIAL_DELAY_MILLIS * (int) Math.pow(2, numberRetried.getAndIncrement());
    return new Retry(true, nextDelay, TimeUnit.MILLISECONDS);
  }
}
