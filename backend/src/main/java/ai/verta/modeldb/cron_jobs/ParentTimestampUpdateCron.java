package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class ParentTimestampUpdateCron extends TimerTask {
  private static final Logger LOGGER = LogManager.getLogger(ParentTimestampUpdateCron.class);
  private Integer recordUpdateLimit;

  public ParentTimestampUpdateCron(int recordUpdateLimit) {
    this.recordUpdateLimit = recordUpdateLimit;
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("ParentTimestampUpdateCron wakeup");

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      // Update experiment timestamp
      session.beginTransaction();
      updateExperimentByExperimentRunTimestamp(session);
      session.getTransaction().commit();

      // Update project timestamp
      session.beginTransaction();
      updateProjectByExperimentTimestamp(session);
      session.getTransaction().commit();

      // Update experimentRun timestamp
      session.beginTransaction();
      updateDatasetByDatasetVersionTimestamp(session);
      session.getTransaction().commit();

      // Update repository timestamp
      session.beginTransaction();
      updateRepositoryByCommitTimestamp(session);
      session.getTransaction().commit();
    } catch (Exception ex) {
      LOGGER.warn("ParentTimestampUpdateCron Exception: ", ex);
      if (ModelDBUtils.needToRetry(ex)) {
        run();
      }
    }

    LOGGER.info("ParentTimestampUpdateCron finish tasks and reschedule");
  }

  private void updateProjectByExperimentTimestamp(Session session) {
    LOGGER.debug("Project timestamp updating");
    String projectUpdateQueryString =
        new StringBuilder()
            .append("UPDATE project p ")
            .append("INNER JOIN ")
            .append(" (SELECT ex.project_id, MAX(ex.date_updated) AS max_date ")
            .append(" FROM experiment ex ")
            .append(" GROUP BY ex.project_id) exp_alias ")
            .append("ON  p.id = exp_alias.project_id AND p.date_updated < exp_alias.max_date ")
            .append("SET p.date_updated = exp_alias.max_date WHERE p.id = exp_alias.project_id ")
            .append(" LIMIT ")
            .append(recordUpdateLimit)
            .toString();
    Query query = session.createSQLQuery(projectUpdateQueryString);
    LOGGER.debug("Project update timestamp query: {}", query.getQueryString());
    int count = query.executeUpdate();
    LOGGER.debug("Project timestamp updated successfully : Updated projects count {}", count);
  }

  private void updateExperimentByExperimentRunTimestamp(Session session) {
    LOGGER.debug("Experiment timestamp updating");
    String experimentUpdateQueryString =
        new StringBuilder("UPDATE experiment ex ")
            .append("INNER JOIN ")
            .append("(SELECT expr.experiment_id, MAX(expr.date_updated) AS max_date ")
            .append(" FROM experiment_run expr ")
            .append(" GROUP BY expr.experiment_id) expr_alias ")
            .append(
                "ON ex.id = expr_alias.experiment_id AND ex.date_updated < expr_alias.max_date ")
            .append(
                "SET ex.date_updated = expr_alias.max_date WHERE ex.id = expr_alias.experiment_id ")
            .append(" LIMIT ")
            .append(recordUpdateLimit)
            .toString();
    Query query = session.createSQLQuery(experimentUpdateQueryString);
    LOGGER.debug("Experiment update timestamp query: {}", query.getQueryString());
    int count = query.executeUpdate();
    LOGGER.debug("Experiment timestamp updated successfully : Updated experiments count {}", count);
  }

  private void updateDatasetByDatasetVersionTimestamp(Session session) {
    LOGGER.debug("Dataset timestamp updating");
    String datasetUpdateQueryString =
        new StringBuilder("UPDATE dataset d ")
            .append("INNER JOIN ")
            .append("(SELECT dv.dataset_id, MAX(dv.time_updated) AS max_date ")
            .append(" FROM dataset_version dv ")
            .append(" GROUP BY dv.dataset_id) dv_alias ")
            .append("ON d.id = dv_alias.dataset_id AND d.time_updated < dv_alias.max_date ")
            .append("SET d.time_updated = dv_alias.max_date WHERE d.id = dv_alias.dataset_id ")
            .append(" LIMIT ")
            .append(recordUpdateLimit)
            .toString();
    Query query = session.createSQLQuery(datasetUpdateQueryString);
    LOGGER.debug("Dataset update timestamp query: {}", query.getQueryString());
    int count = query.executeUpdate();
    LOGGER.debug("Dataset timestamp updated successfully : Updated datasets count {}", count);
  }

  private void updateRepositoryByCommitTimestamp(Session session) {
    LOGGER.debug("Repository timestamp updating");
    String repositoryUpdateQueryString =
        new StringBuilder("UPDATE repository rp INNER JOIN ")
            .append("(SELECT rc.repository_id, MAX(cm.date_created) AS max_date")
            .append(" FROM `commit` cm INNER JOIN repository_commit rc ")
            .append(" ON rc.commit_hash = cm.commit_hash ")
            .append(" GROUP BY rc.repository_id) cm_alias ")
            .append(" ON rp.id = cm_alias.repository_id AND rp.date_updated < cm_alias.max_date ")
            .append("SET rp.date_updated = cm_alias.max_date WHERE rp.id = cm_alias.repository_id ")
            .append(" LIMIT ")
            .append(recordUpdateLimit)
            .toString();
    Query query = session.createSQLQuery(repositoryUpdateQueryString);
    LOGGER.debug("Repository update timestamp query: {}", query.getQueryString());
    int count = query.executeUpdate();
    LOGGER.debug(
        "Repository timestamp updated successfully : Updated repositories count {}", count);
  }
}
