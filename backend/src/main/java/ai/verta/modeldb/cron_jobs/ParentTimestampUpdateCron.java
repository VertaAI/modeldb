package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.util.TimerTask;
import javax.persistence.OptimisticLockException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class ParentTimestampUpdateCron extends TimerTask {
  private static final Logger LOGGER = LogManager.getLogger(ParentTimestampUpdateCron.class);
  private final ModelDBHibernateUtil modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
  private final boolean isPostgres;
  private static String updateExperimentQuery;

  public ParentTimestampUpdateCron(int recordUpdateLimit, boolean isPostgres) {

    this.isPostgres = isPostgres;
    initExperimentUpdateQueryString(recordUpdateLimit);
  }

  public void initExperimentUpdateQueryString(int recordUpdateLimit) {
    if (isPostgres) {
      updateExperimentQuery =
          new StringBuilder("with expr_alias as ")
              .append(" ( SELECT expr.experiment_id, MAX(expr.date_updated) AS max_date ")
              .append(" FROM experiment_run expr ")
              .append(" GROUP BY expr.experiment_id limit ")
              .append(recordUpdateLimit)
              .append(" ) UPDATE experiment as ex ")
              .append(" SET date_updated = expr_alias.max_date ")
              .append(" from expr_alias ")
              .append(" WHERE ex.id = expr_alias.experiment_id ")
              .append(" and ex.date_updated < expr_alias.max_date")
              .toString();
    } else {
      updateExperimentQuery =
          new StringBuilder("UPDATE experiment ex ")
              .append(" INNER JOIN ")
              .append(" (SELECT expr.experiment_id, MAX(expr.date_updated) AS max_date ")
              .append(" FROM experiment_run expr INNER JOIN experiment e ")
              .append(" ON e.id = expr.experiment_id AND e.date_updated < expr.date_updated ")
              .append(" GROUP BY expr.experiment_id LIMIT ")
              .append(recordUpdateLimit)
              .append(" ) expr_alias  ON ex.id = expr_alias.experiment_id ")
              .append(" SET ex.date_updated = expr_alias.max_date ")
              .append(" WHERE ex.id = expr_alias.experiment_id ")
              .toString();
    }
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("ParentTimestampUpdateCron wakeup");

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      // Update experiment timestamp
      session.beginTransaction();
      try {
        updateExperimentByExperimentRunTimestamp(session);
      } catch (Exception ex) {
        LOGGER.warn(
            "ParentTimestampUpdateCron : updateExperimentByExperimentRunTimestamp Exception: ", ex);
      } finally {
        session.getTransaction().commit();
      }
    } catch (OptimisticLockException ex) {
      LOGGER.info("ParentTimestampUpdateCron Exception: {}", ex.getMessage());
    } catch (Exception ex) {
      LOGGER.warn("ParentTimestampUpdateCron Exception: ", ex);
      if (ModelDBUtils.needToRetry(ex)) {
        run();
      }
    }

    LOGGER.info("ParentTimestampUpdateCron finish tasks and reschedule");
  }

  private void updateExperimentByExperimentRunTimestamp(Session session) {
    LOGGER.trace("Experiment timestamp updating");
    Query query = session.createSQLQuery(updateExperimentQuery);
    int count = query.executeUpdate();
    LOGGER.info("Experiment timestamp updated successfully : Updated experiments count {}", count);
  }
}
