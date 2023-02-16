package ai.verta.modeldb.configuration;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAODisabled;
import ai.verta.modeldb.common.config.CronJobConfig;
import ai.verta.modeldb.common.configuration.ServerEnabled;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.cron_jobs.CleanUpEntitiesCron;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.cron_jobs.PopulateEnvironmentInRunCron;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@NoArgsConstructor
@Configuration
public class CronJobUtils {
  private static final Logger LOGGER = LogManager.getLogger(CronJobUtils.class);

  @Bean
  @Conditional(ServerEnabled.class)
  public CronJobUtils initializeCronJobs(MDBConfig mdbConfig, ServiceSet services) {
    LOGGER.info("Enter in CronJobUtils: initializeBasedOnConfig()");
    if (mdbConfig.getCron_job() != null) {
      for (Map.Entry<String, CronJobConfig> cronJob : mdbConfig.getCron_job().entrySet()) {
        TimerTask task = null;
        if (cronJob.getKey().equals(ModelDBConstants.DELETE_ENTITIES)
            && (mdbConfig.hasServiceAccount() || !services.getMdbRoleService().IsImplemented())) {
          task =
              new DeleteEntitiesCron(
                  services.getMdbRoleService(), cronJob.getValue().getRecord_update_limit());
        } else if (cronJob.getKey().equals(ModelDBConstants.UPDATE_RUN_ENVIRONMENTS)
            && services.getArtifactStoreService() != null
            && !(services.getArtifactStoreService() instanceof ArtifactStoreDAODisabled)) {
          task =
              new PopulateEnvironmentInRunCron(
                  services.getArtifactStoreService(),
                  cronJob.getValue().getRecord_update_limit(),
                  mdbConfig);
        } else if (cronJob.getKey().equals(ModelDBConstants.CLEAN_UP_ENTITIES)
            && (mdbConfig.hasServiceAccount() || !services.getMdbRoleService().IsImplemented())) {
          task =
              new CleanUpEntitiesCron(
                  services.getMdbRoleService(), cronJob.getValue().getRecord_update_limit());
        } else {
          LOGGER.info("Unknown config key ({}) found for the cron job", cronJob.getKey());
        }
        if (task != null) {
          CommonUtils.scheduleTask(
              task,
              cronJob.getValue().getInitial_delay(),
              cronJob.getValue().getFrequency(),
              TimeUnit.SECONDS);
          LOGGER.info("{} cron job scheduled successfully", cronJob.getKey());
        }
      }
    }
    LOGGER.info("Exit from CronJobUtils: initializeBasedOnConfig()");
    return this;
  }
}
