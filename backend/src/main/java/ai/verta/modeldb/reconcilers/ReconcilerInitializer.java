package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReconcilerInitializer {
  private static final Logger LOGGER = LogManager.getLogger(ReconcilerInitializer.class);
  public static SoftDeleteProjects softDeleteProjects;
  public static SoftDeleteExperiments softDeleteExperiments;
  public static SoftDeleteExperimentRuns softDeleteExperimentRuns;
  public static SoftDeleteRepositories softDeleteRepositories;

  public static void initialize(Config config, ServiceSet services) {
    LOGGER.info("Enter in ReconcilerUtils: initialize()");
    softDeleteProjects = new SoftDeleteProjects(new ReconcilerConfig(), services.roleService);
    softDeleteExperiments = new SoftDeleteExperiments(new ReconcilerConfig(), services.roleService);
    softDeleteExperimentRuns =
        new SoftDeleteExperimentRuns(new ReconcilerConfig(), services.roleService);
    softDeleteRepositories =
        new SoftDeleteRepositories(new ReconcilerConfig(), services.roleService);
    LOGGER.info("Exit from ReconcilerUtils: initialize()");
  }
}
