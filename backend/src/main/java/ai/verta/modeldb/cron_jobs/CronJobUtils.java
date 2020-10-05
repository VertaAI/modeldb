package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CronJobUtils {

  private static final Logger LOGGER = LogManager.getLogger(CronJobUtils.class);
  public static final int DEFAULT_FREQUENCY = 60;
  public static final int DEFAULT_UPDATE_LIMIT = 100;

  public static void initializeBasedOnConfig(
      Map<String, Object> propertiesMap,
      AuthService authService,
      RoleService roleService,
      OrganizationResourceDAOs organizationResourceDAOs) {

    App app = App.getInstance();
    LOGGER.info("Enter in CronJobUtils: initializeBasedOnConfig()");
    if (propertiesMap.containsKey(ModelDBConstants.CRON_JOB)) {
      Map<String, Object> cronJobMap =
          (Map<String, Object>) propertiesMap.get(ModelDBConstants.CRON_JOB);
      if (cronJobMap != null && !cronJobMap.isEmpty()) {
        for (Map.Entry<String, Object> cronJob : cronJobMap.entrySet()) {
          if (cronJob.getKey().equals(ModelDBConstants.UPDATE_PARENT_TIMESTAMP)) {
            scheduleCronJob(
                cronJob, ModelDBConstants.UPDATE_PARENT_TIMESTAMP, ParentTimestampUpdateCron::new);
          } else if (cronJob.getKey().equals(ModelDBConstants.DELETE_ENTITIES)
              && ((app.getServiceUserEmail() != null && app.getServiceUserDevKey() != null)
                  || !roleService.IsImplemented())) {
            scheduleCronJob(
                cronJob,
                ModelDBConstants.DELETE_ENTITIES,
                recordUpdateLimit ->
                    new DeleteEntitiesCron(authService, roleService, recordUpdateLimit));
          } else if (cronJob.getKey().equals(ModelDBConstants.FIX_DELETED_ORG_RESOURCES)
              && ((app.getServiceUserEmail() != null && app.getServiceUserDevKey() != null)
                  || !roleService.IsImplemented())) {
            scheduleCronJob(
                cronJob,
                ModelDBConstants.FIX_DELETED_ORG_RESOURCES,
                recordUpdateLimit ->
                    new FixDeletedOrgResourcesCron(
                        roleService, recordUpdateLimit, organizationResourceDAOs));
          } else {
            LOGGER.info("Unknown config key ({}) found for the cron job", cronJob.getKey());
          }
        }
      }
    }
    LOGGER.info("Exit from CronJobUtils: initializeBasedOnConfig()");
  }

  private static void scheduleCronJob(
      Map.Entry<String, Object> cronJob, String jobName, Function<Integer, TimerTask> task) {
    Map<String, Object> updateParentTimestampCronMap = (Map<String, Object>) cronJob.getValue();
    int recordUpdateLimit =
        (int)
            updateParentTimestampCronMap.getOrDefault(
                ModelDBConstants.RECORD_UPDATE_LIMIT, DEFAULT_UPDATE_LIMIT);
    int initialDelay =
        (int)
            updateParentTimestampCronMap.getOrDefault(
                ModelDBConstants.INITIAL_DELAY, ModelDBConstants.INITIAL_CRON_DELAY);
    // creating an instance of task to be scheduled
    ModelDBUtils.scheduleTask(
        task.apply(recordUpdateLimit), initialDelay, DEFAULT_FREQUENCY, TimeUnit.SECONDS);
    LOGGER.info("{} cron job scheduled successfully", jobName);
  }
}
