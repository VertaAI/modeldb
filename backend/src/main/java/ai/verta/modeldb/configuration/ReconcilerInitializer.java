package ai.verta.modeldb.configuration;

import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.common.configuration.RunLiquibaseSeparately.RunLiquibaseWithMainService;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.common.reconcilers.SendEventsWithCleanUp;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.modeldb.reconcilers.SoftDeleteExperimentRuns;
import ai.verta.modeldb.reconcilers.SoftDeleteExperiments;
import ai.verta.modeldb.reconcilers.SoftDeleteProjects;
import ai.verta.modeldb.reconcilers.SoftDeleteRepositories;
import ai.verta.modeldb.reconcilers.UpdateExperimentTimestampReconcile;
import ai.verta.modeldb.reconcilers.UpdateProjectTimestampReconcile;
import ai.verta.modeldb.reconcilers.UpdateRepositoryTimestampReconcile;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
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

  @Bean
  @Conditional({RunLiquibaseWithMainService.class})
  public ReconcilerInitializer initialize(
      MDBConfig config, ServiceSet services, DAOSet daos, Executor executor) {
    LOGGER.info("Enter in ReconcilerUtils: initialize()");

    var futureJdbi = config.getJdbi();
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
        new UpdateRepositoryTimestampReconcile(reconcilerConfig, futureJdbi, executor, config);
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
    return this;
  }
}
