package ai.verta.modeldb.common.reconcilers;

public class ReconcilerConfig {
  private long resyncPeriodSeconds = 60L;
  private int batchSize = 10;
  private int workerCount = 10;
  private int maxSync = 10000;
  private final boolean isTestReconciler;

  public ReconcilerConfig(
      long resyncPeriodSeconds,
      int batchSize,
      int workerCount,
      int maxSync,
      boolean isTestReconciler) {
    this.resyncPeriodSeconds = resyncPeriodSeconds;
    this.batchSize = batchSize;
    this.workerCount = workerCount;
    this.maxSync = maxSync;
    this.isTestReconciler = isTestReconciler;
  }

  /**
   * @deprecated Use the {@link #builder()} method instead.
   */
  @Deprecated
  public ReconcilerConfig(boolean isTestReconciler) {
    this.isTestReconciler = isTestReconciler;
  }

  public static Builder builder() {
    return new Builder();
  }

  public long getResyncPeriodSeconds() {
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

  public boolean isTestReconciler() {
    return isTestReconciler;
  }

  public void setResyncPeriodSeconds(long resyncPeriodSeconds) {
    this.resyncPeriodSeconds = resyncPeriodSeconds;
  }

  public static class Builder {
    private long resyncPeriodSeconds = 60L;
    private int batchSize = 10;
    private int workerCount = 10;
    private int maxSync = 10000;
    private boolean isTestReconciler = false;

    public Builder setResyncPeriodSeconds(long resyncPeriodSeconds) {
      this.resyncPeriodSeconds = resyncPeriodSeconds;
      return this;
    }

    public Builder batchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public Builder workerCount(int workerCount) {
      this.workerCount = workerCount;
      return this;
    }

    public Builder maxSync(int maxSync) {
      this.maxSync = maxSync;
      return this;
    }

    public Builder isTestReconciler(boolean isTestReconciler) {
      this.isTestReconciler = isTestReconciler;
      return this;
    }

    public ReconcilerConfig build() {
      return new ReconcilerConfig(
          resyncPeriodSeconds, batchSize, workerCount, maxSync, isTestReconciler);
    }
  }
}
