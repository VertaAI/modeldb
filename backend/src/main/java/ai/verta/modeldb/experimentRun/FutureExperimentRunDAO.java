package ai.verta.modeldb.experimentRun;

import java.util.concurrent.Executor;

public class FutureExperimentRunDAO {
  private final Executor executor;

  public FutureExperimentRunDAO(Executor executor) {
    this.executor = executor;
  }
}
