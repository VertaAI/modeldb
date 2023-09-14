package ai.verta.modeldb.common.reconcilers;

import static io.opentelemetry.api.common.AttributeKey.longKey;

import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import io.opentelemetry.sdk.internal.DaemonThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Reconciler<T> {
  private static final AttributeKey<String> RECONCILER_NAME_ATTRIBUTE_KEY =
      AttributeKey.stringKey("reconciler");
  private static final AttributeKey<Long> NUMBER_OF_ITEMS_ATTRIBUTE_KEY = longKey("numberOfItems");
  protected final Logger logger;
  protected final HashSet<T> elements = new HashSet<>();
  protected final LinkedList<T> order = new LinkedList<>();
  private final Lock lock = new ReentrantLock();
  private final Condition notEmpty = lock.newCondition();
  // To prevent OptimisticLockException
  private final Set<T> processingIdSet = ConcurrentHashMap.newKeySet();
  private final boolean deduplicate;

  protected final ReconcilerConfig config;
  protected final FutureJdbi futureJdbi;
  protected final FutureExecutor executor;
  private final Tracer tracer;

  @Deprecated(forRemoval = true)
  protected Reconciler(
      ReconcilerConfig config,
      Logger logger,
      FutureJdbi futureJdbi,
      FutureExecutor executor,
      boolean deduplicate) {
    this(config, futureJdbi, executor, OpenTelemetry.noop(), deduplicate);
  }

  protected Reconciler(
      ReconcilerConfig config,
      FutureJdbi futureJdbi,
      FutureExecutor executor,
      OpenTelemetry openTelemetry,
      boolean deduplicate) {
    this.config = config;
    this.futureJdbi = futureJdbi;
    this.executor = executor;
    this.deduplicate = deduplicate;
    this.logger = LogManager.getLogger(this.getClass());
    this.tracer = openTelemetry.getTracer("ai.verta.reconciler");

    if (!config.isTestReconciler()) {
      startResync();
    }
    startWorkers();
  }

  private void startResync() {
    Runnable runnable =
        () -> {
          Span span =
              tracer
                  .spanBuilder(getClass().getSimpleName() + " resync")
                  .setAttribute(RECONCILER_NAME_ATTRIBUTE_KEY, this.getClass().getSimpleName())
                  .startSpan();
          try (Scope ignored = span.makeCurrent()) {
            this.resync();
          } catch (Exception ex) {
            logger.error("Resync: ", ex);
            span.recordException(ex);
            span.setStatus(StatusCode.ERROR);
          } finally {
            span.end();
          }
        };

    var executorService =
        Executors.newSingleThreadScheduledExecutor(
            new DaemonThreadFactory(getClass().getSimpleName() + "-resyncer"));
    executorService.scheduleAtFixedRate(
        runnable,
        30L /*as per last parameter timeunit*/,
        config.getResyncPeriodSeconds(),
        TimeUnit.SECONDS);
  }

  private void startWorkers() {
    var executorService =
        Executors.newFixedThreadPool(
            config.getWorkerCount(),
            new DaemonThreadFactory(getClass().getSimpleName() + "-workers"));
    for (var i = 0; i < config.getWorkerCount(); i++) {
      Runnable runnable =
          () -> {
            while (true) {
              try {
                if (deduplicate) {
                  Set<T> idsToProcess;
                  // Fetch ids to process while avoiding race conditions
                  try {
                    lock.lock();
                    idsToProcess =
                        pop().stream()
                            .filter(id -> !processingIdSet.contains(id))
                            .collect(Collectors.toSet());
                    if (!idsToProcess.isEmpty()) {
                      processingIdSet.addAll(idsToProcess);
                    }
                  } finally {
                    lock.unlock();
                  }

                  if (!idsToProcess.isEmpty()) {
                    try {
                      traceReconcile(idsToProcess);
                    } finally {
                      lock.lock();
                      try {
                        processingIdSet.removeAll(idsToProcess);
                      } finally {
                        lock.unlock();
                      }
                    }
                  }
                } else {
                  traceReconcile(pop());
                }
              } catch (Exception ex) {
                logger.error("Worker for " + getClass().getSimpleName() + " reconcile error: ", ex);
              }
            }
          };
      executorService.execute(runnable);
    }
  }

  private ReconcileResult traceReconcile(Set<T> idsToProcess) throws Exception {
    Span span =
        tracer
            .spanBuilder(getClass().getSimpleName() + " reconcile")
            .setAttribute(RECONCILER_NAME_ATTRIBUTE_KEY, this.getClass().getName())
            .setAttribute(NUMBER_OF_ITEMS_ATTRIBUTE_KEY, (long) idsToProcess.size())
            .startSpan();
    try (Scope ignored = span.makeCurrent()) {
      return reconcile(idsToProcess);
    } catch (Exception e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } finally {
      span.end();
    }
  }

  public void insert(T element) {
    lock.lock();
    try {
      if (!elements.contains(element)) {
        elements.add(element);
        order.push(element);
      }
      notEmpty.signal();
    } finally {
      lock.unlock();
    }
  }

  private Set<T> pop() {
    Set<T> ret = new HashSet<>();
    lock.lock();
    try {
      while (elements.isEmpty()) notEmpty.await();

      while (!elements.isEmpty() && ret.size() < config.getBatchSize()) {
        T obj = order.pop();
        elements.remove(obj);
        ret.add(obj);
      }
    } catch (InterruptedException ignored) {
      // Restore interrupted state...
      Thread.currentThread().interrupt();
    } finally {
      lock.unlock();
    }
    return ret;
  }

  public abstract void resync() throws Exception;

  protected abstract ReconcileResult reconcile(Set<T> objs) throws Exception;

  public boolean isEmpty() {
    return processingIdSet.isEmpty() && elements.isEmpty();
  }
}
