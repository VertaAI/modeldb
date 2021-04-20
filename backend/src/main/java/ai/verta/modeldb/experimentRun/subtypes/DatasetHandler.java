package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.futures.FutureJdbi;
import java.util.concurrent.Executor;

public class DatasetHandler extends ArtifactHandler {
  public DatasetHandler(Executor executor, FutureJdbi jdbi, String entityName) {
    super(executor, jdbi, "datasets", entityName);
  }
}
