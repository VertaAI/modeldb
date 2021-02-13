package ai.verta.modeldb.reconcilers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Reconciler<T> {
  private final HashSet<T> elements = new HashSet<>();
  private final LinkedList<T> order = new LinkedList<>();
  final Lock lock = new ReentrantLock();
  final Condition notEmpty = lock.newCondition();

  private final ReconcilerConfig config;

  protected Reconciler(ReconcilerConfig config) {
    this.config = config;

    startResync();
  }

  private void startResync() {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(
        this::resync, 0, this.config.resyncPeriodSeconds, TimeUnit.SECONDS);
  }

  private void startWorkers() {}

  public void insert(T element) {
    lock.lock();
    try {
      if (!elements.contains(element)) {
        elements.add(element);
        order.push(element);
        notEmpty.signal();
      }
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

  protected abstract void reconcile(T obj);
}
