package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAODisabled;
import ai.verta.modeldb.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CronJobUtils {
  private static final Logger LOGGER = LogManager.getLogger(CronJobUtils.class);
  public static Integer updateParentTimestampFrequency = 60;
  public static Integer deleteEntitiesFrequency = 60;

  public static void initializeBasedOnConfig(
      Map<String, Object> propertiesMap,
      AuthService authService,
      RoleService roleService,
      ArtifactStoreService artifactStoreService) {

    App app = App.getInstance();
    LOGGER.info("Enter in CronJobUtils: initializeBasedOnConfig()");
    if (propertiesMap.containsKey(ModelDBConstants.CRON_JOB)) {
      Map<String, Object> cronJobMap =
          (Map<String, Object>) propertiesMap.get(ModelDBConstants.CRON_JOB);
      if (cronJobMap != null && !cronJobMap.isEmpty()) {
        for (Map.Entry<String, Object> cronJob : cronJobMap.entrySet()) {
          if (cronJob.getKey().equals(ModelDBConstants.UPDATE_PARENT_TIMESTAMP)) {
            Map<String, Object> updateParentTimestampCronMap =
                (Map<String, Object>) cronJob.getValue();
            updateParentTimestampFrequency =
                (int) updateParentTimestampCronMap.getOrDefault(ModelDBConstants.FREQUENCY, 60);
            int recordUpdateLimit =
                (int)
                    updateParentTimestampCronMap.getOrDefault(
                        ModelDBConstants.RECORD_UPDATE_LIMIT, 100);
            int initialDelay =
                (int)
                    updateParentTimestampCronMap.getOrDefault(
                        ModelDBConstants.INITIAL_DELAY, ModelDBConstants.INITIAL_CRON_DELAY);
            // creating an instance of task to be scheduled
            TimerTask task = new ParentTimestampUpdateCron(recordUpdateLimit);
            ModelDBUtils.scheduleTask(
                task, initialDelay, updateParentTimestampFrequency, TimeUnit.SECONDS);
            LOGGER.info(
                "{} cron job scheduled successfully", ModelDBConstants.UPDATE_PARENT_TIMESTAMP);
          } else if (cronJob.getKey().equals(ModelDBConstants.DELETE_ENTITIES)
              && ((app.getServiceUserEmail() != null && app.getServiceUserDevKey() != null)
                  || !roleService.IsImplemented())) {
            Map<String, Object> deleteEntitiesCronMap = (Map<String, Object>) cronJob.getValue();
            deleteEntitiesFrequency =
                (int) deleteEntitiesCronMap.getOrDefault(ModelDBConstants.FREQUENCY, 60);
            int recordUpdateLimit =
                (int) deleteEntitiesCronMap.getOrDefault(ModelDBConstants.RECORD_UPDATE_LIMIT, 100);
            int initialDelay =
                (int)
                    deleteEntitiesCronMap.getOrDefault(
                        ModelDBConstants.INITIAL_DELAY, ModelDBConstants.INITIAL_CRON_DELAY);
            // creating an instance of task to be scheduled
            TimerTask task = new DeleteEntitiesCron(authService, roleService, recordUpdateLimit);
            ModelDBUtils.scheduleTask(
                task, initialDelay, deleteEntitiesFrequency, TimeUnit.SECONDS);
            LOGGER.info("{} cron job scheduled successfully", ModelDBConstants.DELETE_ENTITIES);
          } else if (cronJob.getKey().equals(ModelDBConstants.UPDATE_RUN_ENVIRONMENTS)
              && artifactStoreService != null
              && !(artifactStoreService instanceof ArtifactStoreDAODisabled)) {
            Map<String, Object> updateRunEnvironmentCronMap =
                (Map<String, Object>) cronJob.getValue();
            int cronExecutionFrequency =
                (int) updateRunEnvironmentCronMap.getOrDefault(ModelDBConstants.FREQUENCY, 60);
            int recordUpdateLimit =
                (int)
                    updateRunEnvironmentCronMap.getOrDefault(
                        ModelDBConstants.RECORD_UPDATE_LIMIT, 100);
            int initialDelay =
                (int)
                    updateRunEnvironmentCronMap.getOrDefault(
                        ModelDBConstants.INITIAL_DELAY, ModelDBConstants.INITIAL_CRON_DELAY);
            // creating an instance of task to be scheduled
            TimerTask task =
                new PopulateEnvironmentInRunCron(artifactStoreService, recordUpdateLimit);
            ModelDBUtils.scheduleTask(task, initialDelay, cronExecutionFrequency, TimeUnit.SECONDS);
            LOGGER.info(
                "{} cron job scheduled successfully", ModelDBConstants.UPDATE_RUN_ENVIRONMENTS);
          } else if (cronJob.getKey().equals(ModelDBConstants.DELETE_AUDIT_LOGS)
              && (app.getServiceUserEmail() != null && app.getServiceUserDevKey() != null)) {
            Map<String, Object> deleteAuditLogsCronMap = (Map<String, Object>) cronJob.getValue();
            deleteEntitiesFrequency =
                (int) deleteAuditLogsCronMap.getOrDefault(ModelDBConstants.FREQUENCY, 60);
            int recordUpdateLimit =
                (int)
                    deleteAuditLogsCronMap.getOrDefault(ModelDBConstants.RECORD_UPDATE_LIMIT, 100);
            int initialDelay =
                (int)
                    deleteAuditLogsCronMap.getOrDefault(
                        ModelDBConstants.INITIAL_DELAY, ModelDBConstants.INITIAL_CRON_DELAY);
            // creating an instance of task to be scheduled
            TimerTask task = new AuditLogsCron(recordUpdateLimit);
            ModelDBUtils.scheduleTask(
                task, initialDelay, deleteEntitiesFrequency, TimeUnit.SECONDS);
            LOGGER.info("{} cron job scheduled successfully", ModelDBConstants.DELETE_AUDIT_LOGS);
          } else if (cronJob.getKey().equals(ModelDBConstants.CLEAN_UP_ENTITIES)
              && ((app.getServiceUserEmail() != null && app.getServiceUserDevKey() != null)
                  || !roleService.IsImplemented())) {
            Map<String, Object> deleteEntitiesCronMap = (Map<String, Object>) cronJob.getValue();
            int deleteEntitiesFrequency =
                (int) deleteEntitiesCronMap.getOrDefault(ModelDBConstants.FREQUENCY, 60);
            int recordUpdateLimit =
                (int) deleteEntitiesCronMap.getOrDefault(ModelDBConstants.RECORD_UPDATE_LIMIT, 100);
            int initialDelay =
                (int)
                    deleteEntitiesCronMap.getOrDefault(
                        ModelDBConstants.INITIAL_DELAY, ModelDBConstants.INITIAL_CRON_DELAY);
            // creating an instance of task to be scheduled
            TimerTask task = new CleanUpEntitiesCron(roleService, recordUpdateLimit);
            ModelDBUtils.scheduleTask(
                task, initialDelay, deleteEntitiesFrequency, TimeUnit.SECONDS);
            LOGGER.info("{} cron job scheduled successfully", ModelDBConstants.CLEAN_UP_ENTITIES);
          } else {
            LOGGER.info("Unknown config key ({}) found for the cron job", cronJob.getKey());
          }
        }
      }
    }
    LOGGER.info("Exit from CronJobUtils: initializeBasedOnConfig()");
  }
}
