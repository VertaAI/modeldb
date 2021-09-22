package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
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

  public static void initialize(
      Config config, ServiceSet services, FutureJdbi futureJdbi, Executor executor) {
    LOGGER.info("Enter in ReconcilerUtils: initialize()");
    softDeleteProjects =
        new SoftDeleteProjects(
            new ReconcilerConfig(), services.mdbRoleService, futureJdbi, executor);
    softDeleteExperiments =
        new SoftDeleteExperiments(
            new ReconcilerConfig(), services.mdbRoleService, futureJdbi, executor);
    softDeleteExperimentRuns =
        new SoftDeleteExperimentRuns(
            new ReconcilerConfig(), services.mdbRoleService, futureJdbi, executor);
    softDeleteRepositories =
        new SoftDeleteRepositories(
            new ReconcilerConfig(), services.mdbRoleService, false, futureJdbi, executor);
    softDeleteDatasets =
        new SoftDeleteRepositories(
            new ReconcilerConfig(), services.mdbRoleService, true, futureJdbi, executor);
    updateRepositoryTimestampReconcile =
        new UpdateRepositoryTimestampReconcile(new ReconcilerConfig(), futureJdbi, executor);
    updateExperimentTimestampReconcile =
        new UpdateExperimentTimestampReconcile(new ReconcilerConfig(), futureJdbi, executor);
    updateProjectTimestampReconcile =
        new UpdateProjectTimestampReconcile(new ReconcilerConfig(), futureJdbi, executor);
    LOGGER.info("Exit from ReconcilerUtils: initialize()");
  }
}
