package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import java.sql.PreparedStatement;
import java.util.AbstractMap;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class UpdateParentTimestampReconcile
    extends Reconciler<AbstractMap.SimpleEntry<Long, Long>> {
  private static final Logger LOGGER = LogManager.getLogger(UpdateParentTimestampReconcile.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();

  public UpdateParentTimestampReconcile(ReconcilerConfig config) {
    super(config, LOGGER);
  }

  @Override
  public void resync() {
    var fetchUpdatedDatasetIds =
        new StringBuilder("SELECT rc.repository_id, MAX(cm.date_created) AS max_date ")
            .append(" FROM commit cm INNER JOIN repository_commit rc ")
            .append(" ON rc.commit_hash = cm.commit_hash ")
            .append(" INNER JOIN commit_parent cp ")
            .append(" ON cp.parent_hash IS NOT NULL ")
            .append(" AND cp.child_hash = cm.commit_hash ")
            .append(" INNER JOIN repository rp ")
            .append(" ON rp.id = rc.repository_id AND rp.date_updated < cm.date_created ")
            .append(" GROUP BY rc.repository_id")
            .toString();

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Query fetchUpdatedDatasetIdQuery = session.createSQLQuery(fetchUpdatedDatasetIds);
      fetchUpdatedDatasetIdQuery.setMaxResults(config.maxSync);
      List<Object[]> selectedFieldsList = fetchUpdatedDatasetIdQuery.list();
      selectedFieldsList.stream()
          .forEach(
              selectedFields -> {
                Long datasetId = Long.parseLong(selectedFields[0].toString());
                Long maxUpdatedDate = Long.parseLong(selectedFields[1].toString());
                this.insert(new AbstractMap.SimpleEntry<>(datasetId, maxUpdatedDate));
              });
    }
  }

  @Override
  protected ReconcileResult reconcile(Set<AbstractMap.SimpleEntry<Long, Long>> updatedMaxDateMap) {
    LOGGER.debug("Reconciling parent time update for repository" + updatedMaxDateMap.size());

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var updateDatasetTimestamp = "UPDATE repository SET date_updated = ? WHERE id = ?";
      session.doWork(
          connection -> {
            try (PreparedStatement preparedStatement =
                connection.prepareStatement(updateDatasetTimestamp)) {
              int i = 1;
              for (AbstractMap.SimpleEntry<Long, Long> updatedRecord : updatedMaxDateMap) {
                long id = updatedRecord.getKey();
                long updatedDate = updatedRecord.getValue();
                preparedStatement.setLong(1, updatedDate);
                preparedStatement.setLong(2, id);
                preparedStatement.addBatch();
                // Batch size: 20
                if (i % 20 == 0) {
                  preparedStatement.executeBatch();
                }
                i++;
              }
              preparedStatement.executeBatch();
            }
          });
    }

    return new ReconcileResult();
  }
}
