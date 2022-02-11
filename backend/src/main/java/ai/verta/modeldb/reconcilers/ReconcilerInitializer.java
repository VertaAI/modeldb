package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.common.reconcilers.SendEventsWithCleanUp;
import ai.verta.modeldb.config.TestConfig;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReconcilerInitializer {

  private static final Logger LOGGER = LogManager.getLogger(ReconcilerInitializer.class);
  public static SoftDeleteProjects softDeleteProjects;
  public static SoftDeleteExperiments softDeleteExperiments;
  public static SoftDeleteExperimentRuns softDeleteExperimentRuns;
  public static SoftDeleteRepositories softDeleteRepositories;
  public static SoftDeleteRepositories softDeleteDatasets;
  public static UpdateRepositoryTimestampReconcile updateRepositoryTimestampReconcile;
  public static UpdateExperimentTimestampReconcile updateExperimentTimestampReconcile;
  public static UpdateProjectTimestampReconcile updateProjectTimestampReconcile;
  public static SendEventsWithCleanUp sendEventsWithCleanUp;

  public static void initialize(
      Config config, ServiceSet services, DAOSet daos, FutureJdbi futureJdbi, Executor executor) {
    LOGGER.info("Enter in ReconcilerUtils: initialize()");

    ReconcilerConfig reconcilerConfig = new ReconcilerConfig(config instanceof TestConfig);

    softDeleteProjects =
        new SoftDeleteProjects(reconcilerConfig, services.mdbRoleService, futureJdbi, executor);
    softDeleteExperiments =
        new SoftDeleteExperiments(reconcilerConfig, services.mdbRoleService, futureJdbi, executor);
    softDeleteExperimentRuns =
        new SoftDeleteExperimentRuns(
            reconcilerConfig, services.mdbRoleService, futureJdbi, executor);
    softDeleteRepositories =
        new SoftDeleteRepositories(
            reconcilerConfig, services.mdbRoleService, false, futureJdbi, executor);
    softDeleteDatasets =
        new SoftDeleteRepositories(
            reconcilerConfig, services.mdbRoleService, true, futureJdbi, executor);
    updateRepositoryTimestampReconcile =
        new UpdateRepositoryTimestampReconcile(reconcilerConfig, futureJdbi, executor);
    updateExperimentTimestampReconcile =
        new UpdateExperimentTimestampReconcile(reconcilerConfig, futureJdbi, executor);
    updateProjectTimestampReconcile =
        new UpdateProjectTimestampReconcile(reconcilerConfig, futureJdbi, executor);

    if (config.isEvent_system_enabled()) {
      sendEventsWithCleanUp =
          new SendEventsWithCleanUp(
              reconcilerConfig, services.uac, daos.futureEventDAO, futureJdbi, executor);
    }

    LOGGER.info("Exit from ReconcilerUtils: initialize()");
  }
}
