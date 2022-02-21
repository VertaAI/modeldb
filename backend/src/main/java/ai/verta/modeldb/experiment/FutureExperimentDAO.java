package ai.verta.modeldb.experiment;

import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.experimentRun.subtypes.ArtifactHandler;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.CodeVersionHandler;
import ai.verta.modeldb.experimentRun.subtypes.DatasetHandler;
import ai.verta.modeldb.experimentRun.subtypes.PredicatesHandler;
import ai.verta.modeldb.experimentRun.subtypes.SortingHandler;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FutureExperimentDAO {
  private static final Logger LOGGER = LogManager.getLogger(FutureExperimentDAO.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;
  private final boolean isMssql;

  private final AttributeHandler attributeHandler;
  private final TagsHandler tagsHandler;
  private final ArtifactHandler artifactHandler;
  private final PredicatesHandler predicatesHandler;
  private final CodeVersionHandler codeVersionHandler;
  private final SortingHandler sortingHandler;

  public FutureExperimentDAO(
      Executor executor,
      FutureJdbi jdbi,
      UAC uac,
      MDBConfig mdbConfig,
      ArtifactStoreDAO artifactStoreDAO,
      DatasetVersionDAO datasetVersionDAO) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;
    this.isMssql = mdbConfig.getDatabase().getRdbConfiguration().isMssql();

    var entityName = "ExperimentEntity";
    attributeHandler = new AttributeHandler(executor, jdbi, entityName);
    tagsHandler = new TagsHandler(executor, jdbi, entityName);
    codeVersionHandler = new CodeVersionHandler(executor, jdbi, "experiment");
    DatasetHandler datasetHandler = new DatasetHandler(executor, jdbi, entityName, mdbConfig);
    artifactHandler =
        new ArtifactHandler(
            executor,
            jdbi,
            entityName,
            codeVersionHandler,
            datasetHandler,
            artifactStoreDAO,
            datasetVersionDAO,
            mdbConfig);
    predicatesHandler = new PredicatesHandler("experiment", "exp");
    sortingHandler = new SortingHandler("experiment");
  }
}
