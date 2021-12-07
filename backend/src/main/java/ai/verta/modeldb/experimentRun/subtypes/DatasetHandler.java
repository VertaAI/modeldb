package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.subtypes.ArtifactHandlerBase;
import ai.verta.modeldb.config.MDBConfig;
import java.util.concurrent.Executor;

public class DatasetHandler extends ArtifactHandlerBase {
  public DatasetHandler(
      Executor executor, FutureJdbi jdbi, String entityName, MDBConfig mdbConfig) {
    super(executor, jdbi, "datasets", entityName, mdbConfig.artifactStoreConfig);
  }

  @Override
  protected void setEntityIdReferenceColumn(String entityName) {
    switch (entityName) {
      case "ProjectEntity":
        this.entityIdReferenceColumn = "project_id";
        break;
      case "ExperimentRunEntity":
        this.entityIdReferenceColumn = "experiment_run_id";
        break;
      default:
        throw new InternalErrorException("Invalid entity name: " + entityName);
    }
  }

  @Override
  public void validateMaxArtifactsForTrial(int newArtifactsCount, int existingArtifactsCount)
      throws ModelDBException {
    // No any logic for datasets
  }
}
