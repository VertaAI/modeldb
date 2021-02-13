package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReconcilerUtils {
  private static final Logger LOGGER = LogManager.getLogger(ReconcilerUtils.class);

  public static void initialize(Config config, ServiceSet services) {
    LOGGER.info("Enter in ReconcilerUtils: initialize()");
    new SoftDeleteProjects(new ReconcilerConfig(), services.roleService);
    new SoftDeleteExperiments(new ReconcilerConfig(), services.roleService);
    new SoftDeleteExperimentRuns(new ReconcilerConfig(), services.roleService);
    LOGGER.info("Exit from ReconcilerUtils: initialize()");
  }
}
