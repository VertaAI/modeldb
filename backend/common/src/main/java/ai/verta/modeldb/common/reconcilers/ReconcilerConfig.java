package ai.verta.modeldb.common.reconcilers;

public class ReconcilerConfig {
  public int resyncPeriodSeconds = 60;
  public int batchSize = 10;
  public int workerCount = 10;
  public int maxSync = 10000;
}
