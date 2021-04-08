package ai.verta.modeldb.common.cron_job;

import ai.verta.modeldb.common.CommonHibernateUtil;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.config.DatabaseConfig;
import java.sql.Connection;
import java.util.TimerTask;
import liquibase.database.jvm.JdbcConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MaxPacketCron extends TimerTask {
  private static final Logger LOGGER = LogManager.getLogger(MaxPacketCron.class);
  private final CommonHibernateUtil hibernateUtil;
  private final DatabaseConfig databaseConfig;

  public MaxPacketCron(CommonHibernateUtil hibernateUtil, DatabaseConfig databaseConfig) {
    this.hibernateUtil = hibernateUtil;
    this.databaseConfig = databaseConfig;
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("MaxPacketCron wakeup");

    try (Connection con = hibernateUtil.getDBConnection(databaseConfig.RdbConfiguration)) {
      JdbcConnection jdbcCon = new JdbcConnection(con);
      hibernateUtil.setMaxAllowedPacket(jdbcCon, databaseConfig.RdbConfiguration.maxAllowedPacket);
    } catch (Exception ex) {
      LOGGER.warn("MaxPacketCron Exception: ", ex);
    } finally {
      CommonUtils.unregisteredBackgroundUtilsCount();
    }
    LOGGER.info("MaxPacketCron finish tasks and reschedule");
  }
}
