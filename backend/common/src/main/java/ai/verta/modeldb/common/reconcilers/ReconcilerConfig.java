package ai.verta.modeldb.common.reconcilers;

public class ReconcilerConfig {
  private int resyncPeriodSeconds = 60;
  private int batchSize = 10;
  private int workerCount = 10;
  private int maxSync = 10000;

  public int getResyncPeriodSeconds() {
    return resyncPeriodSeconds;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public int getWorkerCount() {
    return workerCount;
  }

  public int getMaxSync() {
    return maxSync;
  }
}
