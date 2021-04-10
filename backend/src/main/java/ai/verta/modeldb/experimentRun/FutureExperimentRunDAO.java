package ai.verta.modeldb.experimentRun;

import ai.verta.modeldb.common.futures.FutureJdbi;

import java.util.concurrent.Executor;

public class FutureExperimentRunDAO {
  private final Executor executor;
  private final FutureJdbi jdbi;

  public FutureExperimentRunDAO(Executor executor, FutureJdbi jdbi) {
    this.executor = executor;
    this.jdbi = jdbi;
  }
}
