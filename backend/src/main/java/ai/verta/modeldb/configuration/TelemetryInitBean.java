package ai.verta.modeldb.configuration;

import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.telemetry.TelemetryCron;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.io.FileNotFoundException;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelemetryInitBean {

  private static final Logger LOGGER = LogManager.getLogger(TelemetryInitBean.class);

  @Bean
  @Conditional(MigrationsIncludedInAppStartup.class)
  public TelemetryInitBean initializeTelemetryBasedOnConfig(MDBConfig mdbConfig)
      throws FileNotFoundException, InvalidConfigException {
    if (!mdbConfig.telemetry.opt_out) {
      // creating an instance of task to be scheduled
      TimerTask task = new TelemetryCron(mdbConfig.telemetry.consumer);
      ModelDBUtils.scheduleTask(
          task, mdbConfig.telemetry.frequency, mdbConfig.telemetry.frequency, TimeUnit.HOURS);
      LOGGER.info("Telemetry scheduled successfully");
    } else {
      LOGGER.info("Telemetry opt out by user");
    }

    return this;
  }
}
