package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;

import java.util.concurrent.Executor;

public class DatasetHandler extends ArtifactHandlerBase {
  public DatasetHandler(
      Executor executor,
      FutureJdbi jdbi,
      String entityName,
      CodeVersionHandler codeVersionHandler,
      ArtifactStoreDAO artifactStoreDAO,
      DatasetVersionDAO datasetVersionDAO) {
    super(
        executor,
        jdbi,
        "datasets",
        entityName,
        codeVersionHandler,
        null,
        artifactStoreDAO,
        datasetVersionDAO);
  }
}
