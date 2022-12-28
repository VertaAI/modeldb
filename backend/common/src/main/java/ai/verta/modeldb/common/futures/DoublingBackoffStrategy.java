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
  BiFunction<R, Throwable, Boolean> exceptionChecker;
  int maxRetries;
  AtomicInteger numberRetried = new AtomicInteger();

  @Override
  public Retry shouldRetry(R result, Throwable throwable) {
    Boolean apply = exceptionChecker.apply(result, throwable);
    if (!apply || numberRetried.getAndIncrement() == maxRetries) {
      return new Retry(false, 0, TimeUnit.SECONDS);
    }

    return new Retry(true, (int) Math.pow(2, numberRetried.get()), TimeUnit.MILLISECONDS);
  }
}
