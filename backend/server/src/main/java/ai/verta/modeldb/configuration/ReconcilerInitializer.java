package ai.verta.modeldb.configuration;

import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.common.configuration.RunLiquibaseSeparately.RunLiquibaseWithMainService;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.modeldb.reconcilers.SoftDeleteExperimentRuns;
import ai.verta.modeldb.reconcilers.SoftDeleteExperiments;
import ai.verta.modeldb.reconcilers.SoftDeleteProjects;
import ai.verta.modeldb.reconcilers.SoftDeleteRepositories;
import ai.verta.modeldb.reconcilers.UpdateExperimentTimestampReconciler;
import ai.verta.modeldb.reconcilers.UpdateProjectTimestampReconciler;
import ai.verta.modeldb.reconcilers.UpdateRepositoryTimestampReconciler;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.opentelemetry.api.OpenTelemetry;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@NoArgsConstructor
@Getter
@Configuration
public class ReconcilerInitializer {
  private static final Logger LOGGER = LogManager.getLogger(ReconcilerInitializer.class);
  @JsonProperty private SoftDeleteProjects softDeleteProjects;
  @JsonProperty private SoftDeleteExperiments softDeleteExperiments;
  @JsonProperty private SoftDeleteExperimentRuns softDeleteExperimentRuns;
  @JsonProperty private SoftDeleteRepositories softDeleteRepositories;
  @JsonProperty private SoftDeleteRepositories softDeleteDatasets;
  @JsonProperty private UpdateRepositoryTimestampReconciler updateRepositoryTimestampReconciler;
  @JsonProperty private UpdateExperimentTimestampReconciler updateExperimentTimestampReconciler;
  @JsonProperty private UpdateProjectTimestampReconciler updateProjectTimestampReconciler;

  @Bean
  @Conditional({RunLiquibaseWithMainService.class})
  public ReconcilerInitializer initialize(
      MDBConfig config, ServiceSet services, FutureJdbi futureJdbi, OpenTelemetry openTelemetry) {
    LOGGER.info("Enter in ReconcilerUtils: initialize()");

    ReconcilerConfig reconcilerConfig =
        ReconcilerConfig.builder().isTestReconciler(config instanceof TestConfig).build();

    softDeleteProjects =
        new SoftDeleteProjects(
            reconcilerConfig, services.getMdbRoleService(), futureJdbi, openTelemetry);
    softDeleteExperiments = new SoftDeleteExperiments(reconcilerConfig, futureJdbi, openTelemetry);
    softDeleteExperimentRuns =
        new SoftDeleteExperimentRuns(reconcilerConfig, futureJdbi, openTelemetry);
    softDeleteRepositories =
        new SoftDeleteRepositories(
            reconcilerConfig, services.getMdbRoleService(), false, futureJdbi, openTelemetry);
    softDeleteDatasets =
        new SoftDeleteRepositories(
            reconcilerConfig, services.getMdbRoleService(), true, futureJdbi, openTelemetry);
    updateRepositoryTimestampReconciler =
        new UpdateRepositoryTimestampReconciler(
            reconcilerConfig, futureJdbi, config, openTelemetry);
    updateExperimentTimestampReconciler =
        new UpdateExperimentTimestampReconciler(reconcilerConfig, futureJdbi, openTelemetry);
    updateProjectTimestampReconciler =
        new UpdateProjectTimestampReconciler(reconcilerConfig, futureJdbi, openTelemetry);

    LOGGER.info("Exit from ReconcilerUtils: initialize()");
    return this;
  }
}
