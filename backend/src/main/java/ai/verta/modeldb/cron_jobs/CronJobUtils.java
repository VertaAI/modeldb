package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CronJobUtils {
  private static final Logger LOGGER = LogManager.getLogger(CronJobUtils.class);

  public static void initializeBasedOnConfig(Map<String, Object> propertiesMap) {
    LOGGER.info("Enter in CronJobUtils: initializeBasedOnConfig()");
    if (propertiesMap.containsKey(ModelDBConstants.CRON_JOB)) {
      Map<String, Object> cronJobMap =
          (Map<String, Object>) propertiesMap.get(ModelDBConstants.CRON_JOB);
      if (cronJobMap != null && !cronJobMap.isEmpty()) {
        for (Map.Entry<String, Object> cronJob : cronJobMap.entrySet()) {
          if (cronJob.getKey().equals(ModelDBConstants.UPDATE_PARENT_TIMESTAMP)) {
            Map<String, Object> updateParentTimestampCronMap =
                (Map<String, Object>) cronJob.getValue();
            int frequency =
                (int) updateParentTimestampCronMap.getOrDefault(ModelDBConstants.FREQUENCY, 60);
            int recordUpdateLimit =
                (int)
                    updateParentTimestampCronMap.getOrDefault(
                        ModelDBConstants.RECORD_UPDATE_LIMIT, 100);
            // creating an instance of task to be scheduled
            TimerTask task = new ParentTimestampUpdateCron(recordUpdateLimit);
            ModelDBUtils.scheduleTask(task, frequency, TimeUnit.SECONDS);
            LOGGER.info(
                "{} cron job scheduled successfully", ModelDBConstants.UPDATE_PARENT_TIMESTAMP);
          }
        }
      }
    }
    LOGGER.info("Exit from CronJobUtils: initializeBasedOnConfig()");
  }
}
