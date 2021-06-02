package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import java.util.AbstractMap;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpdateRepositoryTimestampReconcile
    extends Reconciler<AbstractMap.SimpleEntry<Long, Long>> {
  private static final Logger LOGGER =
      LogManager.getLogger(UpdateRepositoryTimestampReconcile.class);
  private final FutureJdbi futureJdbi;
  private final Executor executor;

  public UpdateRepositoryTimestampReconcile(
      ReconcilerConfig config, FutureJdbi futureJdbi, Executor executor) {
    super(config, LOGGER);
    this.futureJdbi = futureJdbi;
    this.executor = executor;
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

    futureJdbi.useHandle(
        handle -> {
          handle
              .createQuery(fetchUpdatedDatasetIds)
              .setFetchSize(config.maxSync)
              .map(
                  (rs, ctx) -> {
                    Long datasetId = rs.getLong("rc.repository_id");
                    Long maxUpdatedDate = rs.getLong("max_date");
                    this.insert(new AbstractMap.SimpleEntry<>(datasetId, maxUpdatedDate));
                    return rs;
                  })
              .list();
        });
  }

  @Override
  protected ReconcileResult reconcile(Set<AbstractMap.SimpleEntry<Long, Long>> updatedMaxDateMap) {
    LOGGER.debug("Reconciling parent time update for repository" + updatedMaxDateMap.size());
    try {
      return futureJdbi
          .useHandle(
              handle -> {
                var updateDatasetTimestampQuery =
                    "UPDATE repository SET date_updated = :updatedDate WHERE id = :id";

                final var batch = handle.prepareBatch(updateDatasetTimestampQuery);
                for (AbstractMap.SimpleEntry<Long, Long> updatedRecord : updatedMaxDateMap) {
                  long id = updatedRecord.getKey();
                  long updatedDate = updatedRecord.getValue();
                  batch.bind("id", id).bind("updatedDate", updatedDate).add();
                }

                batch.execute();
              })
          .thenApply(unused -> new ReconcileResult(), executor)
          .get();
    } catch (ExecutionException | InterruptedException ex) {
      throw new ModelDBException(ex);
    }
  }
}
