package ai.verta.modeldb.reconcilers;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class Reconciler<T> {
  private final HashSet<T> elements = new HashSet<>();
  private final LinkedList<T> order = new LinkedList<>();
  private final Object mutex = new Object();

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

  public void insert(T element) {
    synchronized (mutex) {
      if (!elements.contains(element)) {
        elements.add(element);
        order.push(element);
      }
    }
  }

  private HashSet<T> pop() {
    HashSet<T> ret = new HashSet<>();
    synchronized (mutex) {
      while (!elements.isEmpty() && ret.size() < config.batchSize) {
        T obj = order.pop();
        elements.remove(obj);
        ret.add(obj);
      }
    }
    return ret;
  }

  public abstract void resync();
}
