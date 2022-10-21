package ai.verta.modeldb.common.reconcilers;

import ai.verta.modeldb.common.futures.FutureJdbi;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Logger;

public abstract class Reconciler<T> {
  protected final Logger logger;
  protected final HashSet<T> elements = new HashSet<>();
  protected final LinkedList<T> order = new LinkedList<>();
  final Lock lock = new ReentrantLock();
  final Condition notEmpty = lock.newCondition();
  // To prevent OptimisticLockException
  private final Set<T> processingIdSet = ConcurrentHashMap.newKeySet();
  private final boolean deduplicate;

  protected final ReconcilerConfig config;
  protected final FutureJdbi futureJdbi;
  protected final Executor executor;

  protected Reconciler(
      ReconcilerConfig config,
      Logger logger,
      FutureJdbi futureJdbi,
      Executor executor,
      boolean deduplicate) {
    this.logger = logger;
    this.config = config;
    this.futureJdbi = futureJdbi;
    this.executor = executor;
    this.deduplicate = deduplicate;

    if (!config.isTestReconciler()) {
      startResync();
    }
    startWorkers();
  }

  private void startResync() {
    Runnable runnable =
        () -> {
          try {
            this.resync();
          } catch (Exception ex) {
            logger.error("Resync: ", ex);
          }
        };

    var executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleAtFixedRate(
        runnable,
        30L /*as per last parameter timeunit*/,
        config.getResyncPeriodSeconds(),
        TimeUnit.SECONDS);
  }

  private void startWorkers() {
    var executorService = Executors.newFixedThreadPool(config.getWorkerCount());
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
                      reconcile(idsToProcess);
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
                  reconcile(pop());
                }
              } catch (Exception ex) {
                logger.error("Worker reconcile: ", ex);
              }
            }
          };
      executorService.execute(runnable);
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

  private HashSet<T> pop() {
    HashSet<T> ret = new HashSet<>();
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
