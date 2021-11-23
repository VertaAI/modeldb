package ai.verta.modeldb.common.reconcilers;

public class ReconcilerConfig {
  private int resyncPeriodSeconds = 60;
  private int batchSize = 10;
  private int workerCount = 10;
  private int maxSync = 10000;
  private boolean isTestReconciler = false;

  public int getResyncPeriodSeconds() {
    return resyncPeriodSeconds;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public int getWorkerCount() {
    return workerCount;
  }

  public int getMaxSync() {
    return maxSync;
  }

  public void setTestReconciler(boolean testReconciler) {
    isTestReconciler = testReconciler;
  }

  public boolean isTestReconciler() {
    return isTestReconciler;
  }
}
