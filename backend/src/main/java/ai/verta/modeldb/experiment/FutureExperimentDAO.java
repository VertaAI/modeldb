package ai.verta.modeldb.experiment;

import ai.verta.modeldb.CreateExperiment;
import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.experiment.subtypes.CreateExperimentHandler;
import ai.verta.modeldb.experimentRun.subtypes.ArtifactHandler;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.CodeVersionHandler;
import ai.verta.modeldb.experimentRun.subtypes.DatasetHandler;
import ai.verta.modeldb.experimentRun.subtypes.PredicatesHandler;
import ai.verta.modeldb.experimentRun.subtypes.SortingHandler;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import ai.verta.modeldb.project.FutureProjectDAO;
import ai.verta.uac.Empty;
import ai.verta.uac.ModelDBActionEnum;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FutureExperimentDAO {
  private static final Logger LOGGER = LogManager.getLogger(FutureExperimentDAO.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;
  private final boolean isMssql;

  private final FutureProjectDAO futureProjectDAO;
  private final AttributeHandler attributeHandler;
  private final TagsHandler tagsHandler;
  private final ArtifactHandler artifactHandler;
  private final PredicatesHandler predicatesHandler;
  private final CodeVersionHandler codeVersionHandler;
  private final SortingHandler sortingHandler;
  private final CreateExperimentHandler createExperimentHandler;

  public FutureExperimentDAO(
      Executor executor, FutureJdbi jdbi, UAC uac, MDBConfig mdbConfig, DAOSet daoSet) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;
    this.isMssql = mdbConfig.getDatabase().getRdbConfiguration().isMssql();
    this.futureProjectDAO = daoSet.futureProjectDAO;

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
            daoSet.artifactStoreDAO,
            daoSet.datasetVersionDAO,
            mdbConfig);
    predicatesHandler = new PredicatesHandler("experiment", "exp");
    sortingHandler = new SortingHandler("experiment");

    createExperimentHandler =
        new CreateExperimentHandler(
            executor, jdbi, mdbConfig, uac, attributeHandler, tagsHandler, artifactHandler);
  }

  public InternalFuture<Experiment> createExperiment(CreateExperiment request) {
    return FutureGrpc.ClientRequest(
            uac.getUACService().getCurrentUser(Empty.newBuilder().build()), executor)
        .thenCompose(
            userInfo ->
                createExperimentHandler
                    .convertCreateRequest(request, userInfo)
                    .thenCompose(
                        experiment ->
                            futureProjectDAO
                                .checkProjectPermission(
                                    request.getProjectId(),
                                    ModelDBActionEnum.ModelDBServiceActions.UPDATE)
                                .thenApply(unused -> experiment, executor),
                        executor)
                    .thenCompose(createExperimentHandler::insertExperiment, executor),
            executor);
  }
}
