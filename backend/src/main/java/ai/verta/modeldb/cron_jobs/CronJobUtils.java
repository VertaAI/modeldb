package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAODisabled;
import ai.verta.modeldb.common.config.CronJobConfig;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CronJobUtils {
  private static final Logger LOGGER = LogManager.getLogger(CronJobUtils.class);

  public static void initializeCronJobs(Config config, ServiceSet services) {

    LOGGER.info("Enter in CronJobUtils: initializeBasedOnConfig()");
    if (config.cron_job != null) {
      for (Map.Entry<String, CronJobConfig> cronJob : config.cron_job.entrySet()) {
        TimerTask task = null;
        if (cronJob.getKey().equals(ModelDBConstants.DELETE_ENTITIES)
            && (config.hasServiceAccount() || !services.roleService.IsImplemented())) {
          task =
              new DeleteEntitiesCron(
                  services.authService,
                  services.roleService,
                  cronJob.getValue().record_update_limit);
        } else if (cronJob.getKey().equals(ModelDBConstants.UPDATE_RUN_ENVIRONMENTS)
            && services.artifactStoreService != null
            && !(services.artifactStoreService instanceof ArtifactStoreDAODisabled)) {
          task =
              new PopulateEnvironmentInRunCron(
                  services.artifactStoreService, cronJob.getValue().record_update_limit, config);
        } else if (cronJob.getKey().equals(ModelDBConstants.CLEAN_UP_ENTITIES)
            && (config.hasServiceAccount() || !services.roleService.IsImplemented())) {
          task =
              new CleanUpEntitiesCron(services.roleService, cronJob.getValue().record_update_limit);
        } else {
          LOGGER.info("Unknown config key ({}) found for the cron job", cronJob.getKey());
        }
        if (task != null) {
          ModelDBUtils.scheduleTask(
              task,
              cronJob.getValue().initial_delay,
              cronJob.getValue().frequency,
              TimeUnit.SECONDS);
          LOGGER.info("{} cron job scheduled successfully", cronJob.getKey());
        }
      }
    }
    LOGGER.info("Exit from CronJobUtils: initializeBasedOnConfig()");
  }
}
