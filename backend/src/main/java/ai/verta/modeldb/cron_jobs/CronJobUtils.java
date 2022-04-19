package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAODisabled;
import ai.verta.modeldb.common.config.CronJobConfig;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CronJobUtils {
  private static final Logger LOGGER = LogManager.getLogger(CronJobUtils.class);

  public static void initializeCronJobs(MDBConfig mdbConfig, ServiceSet services) {

    LOGGER.info("Enter in CronJobUtils: initializeBasedOnConfig()");
    if (mdbConfig.getCron_job() != null) {
      for (Map.Entry<String, CronJobConfig> cronJob : mdbConfig.getCron_job().entrySet()) {
        TimerTask task = null;
        if (cronJob.getKey().equals(ModelDBConstants.DELETE_ENTITIES)
            && (mdbConfig.hasServiceAccount() || !services.mdbRoleService.IsImplemented())) {
          task =
              new DeleteEntitiesCron(
                  services.authService,
                  services.mdbRoleService,
                  cronJob.getValue().getRecord_update_limit());
        } else if (cronJob.getKey().equals(ModelDBConstants.UPDATE_RUN_ENVIRONMENTS)
            && services.artifactStoreService != null
            && !(services.artifactStoreService instanceof ArtifactStoreDAODisabled)) {
          task =
              new PopulateEnvironmentInRunCron(
                  services.artifactStoreService,
                  cronJob.getValue().getRecord_update_limit(),
                  mdbConfig);
        } else if (cronJob.getKey().equals(ModelDBConstants.CLEAN_UP_ENTITIES)
            && (mdbConfig.hasServiceAccount() || !services.mdbRoleService.IsImplemented())) {
          task =
              new CleanUpEntitiesCron(
                  services.mdbRoleService, cronJob.getValue().getRecord_update_limit());
        } else {
          LOGGER.info("Unknown config key ({}) found for the cron job", cronJob.getKey());
        }
        if (task != null) {
          ModelDBUtils.scheduleTask(
              task,
              cronJob.getValue().getInitial_delay(),
              cronJob.getValue().getFrequency(),
              TimeUnit.SECONDS);
          LOGGER.info("{} cron job scheduled successfully", cronJob.getKey());
        }
      }
    }
    LOGGER.info("Exit from CronJobUtils: initializeBasedOnConfig()");
  }
}
