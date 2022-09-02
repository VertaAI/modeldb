package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.config.MDBConfig;
import java.util.concurrent.Executor;

public class DatasetHandler extends ArtifactHandlerBase {
  public DatasetHandler(
      Executor executor, FutureJdbi jdbi, String entityName, MDBConfig mdbConfig) {
    super(executor, jdbi, "datasets", entityName, mdbConfig.getArtifactStoreConfig());
  }
}
