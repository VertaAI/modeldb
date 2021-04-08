package ai.verta.modeldb.common.cron_job;

import ai.verta.modeldb.common.CommonHibernateUtil;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.config.Config;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class MaxPacketCron extends TimerTask {
  private static final Logger LOGGER = LogManager.getLogger(MaxPacketCron.class);
  private final CommonHibernateUtil hibernateUtil;
  private final Integer maxAllowedPacket;

  public MaxPacketCron(CommonHibernateUtil hibernateUtil) {
    this.hibernateUtil = hibernateUtil;
    maxAllowedPacket = Config.getInstance().database.RdbConfiguration.maxAllowedPacket;
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("MaxPacketCron wakeup");

    CommonUtils.registeredBackgroundUtilsCount();
    try (Session session = hibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      session.createSQLQuery(CommonHibernateUtil
          .maxAllowedPacketQuery(maxAllowedPacket)).executeUpdate();
      transaction.commit();
    } catch (Exception ex) {
      LOGGER.warn("MaxPacketCron Exception: ", ex);
    } finally {
      CommonUtils.unregisteredBackgroundUtilsCount();
    }
    LOGGER.info("MaxPacketCron finish tasks and reschedule");
  }
}
