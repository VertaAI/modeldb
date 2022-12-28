package ai.verta.modeldb.common.futures;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

@Value
@Getter(AccessLevel.NONE)
class DoublingBackoffStrategy implements RetryStrategy {
  Function<Throwable, Boolean> exceptionChecker;
  int maxRetries;
  AtomicInteger numberRetried = new AtomicInteger();

  @Override
  public Retry shouldRetry(Throwable throwable) {
    Boolean apply = exceptionChecker.apply(throwable);
    if (!apply || numberRetried.getAndIncrement() == maxRetries) {
      return new Retry(false, 0, TimeUnit.SECONDS);
    }

    return new Retry(true, (int) Math.pow(2, numberRetried.get()), TimeUnit.MILLISECONDS);
  }
}
