package ai.verta.modeldb.common.reconcilers;

import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.futures.FutureJdbi;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Reconciler<T> {
  final Logger logger;
  protected final HashSet<T> elements = new HashSet<>();
  protected final LinkedList<T> order = new LinkedList<>();
  final Lock lock = new ReentrantLock();
  final Condition notEmpty = lock.newCondition();

  protected final ReconcilerConfig config;
  protected final FutureJdbi futureJdbi;
  protected final Executor executor;

  protected Reconciler(ReconcilerConfig config, Logger logger, FutureJdbi futureJdbi, Executor executor) {
    this.logger = logger;
    this.config = config;
    this.futureJdbi = futureJdbi;
    this.executor = executor;

    startResync();
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

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(runnable, 0, config.resyncPeriodSeconds, TimeUnit.SECONDS);
  }

  private void startWorkers() {
    ExecutorService executor = Executors.newFixedThreadPool(config.workerCount);
    for (int i = 0; i < config.workerCount; i++) {
      Runnable runnable =
          () -> {
            while (true) {
              try {
                reconcile(pop());
              } catch (Exception ex) {
                logger.error("Worker reconcile: ", ex);
              }
            }
          };
      executor.execute(runnable);
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

      while (!elements.isEmpty() && ret.size() < config.batchSize) {
        T obj = order.pop();
        elements.remove(obj);
        ret.add(obj);
      }
    } catch (InterruptedException ignored) {

    } finally {
      lock.unlock();
    }
    return ret;
  }

  public abstract void resync();

  protected abstract ReconcileResult reconcile(Set<T> objs);
}
