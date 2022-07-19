package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.config.MDBConfig;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;

public class UpdateRepositoryTimestampReconcile
    extends Reconciler<AbstractMap.SimpleEntry<Long, Long>> {

  private final MDBConfig mdbConfig;

  public UpdateRepositoryTimestampReconcile(
      ReconcilerConfig config, FutureJdbi futureJdbi, Executor executor, MDBConfig mdbConfig) {
    super(
        config,
        LogManager.getLogger(UpdateRepositoryTimestampReconcile.class),
        futureJdbi,
        executor,
        false);
    this.mdbConfig = mdbConfig;
  }

  @Override
  public void resync() {
    getEntriesForDateUpdate().forEach(this::insert);
  }

  private List<SimpleEntry<Long, Long>> getEntriesForDateUpdate() {
    var tableName = "commit";
    if (mdbConfig.getDatabase().getRdbConfiguration().isMssql()) {
      tableName = "\"commit\"";
    }
    var fetchUpdatedDatasetIds =
        new StringBuilder(
                "SELECT rc.repository_id as repository_id, MAX(cm.date_created) AS max_date ")
            .append(String.format(" FROM %s cm INNER JOIN repository_commit rc ", tableName))
            .append(" ON rc.commit_hash = cm.commit_hash ")
            .append(" INNER JOIN commit_parent cp ")
            .append(" ON cp.parent_hash IS NOT NULL ")
            .append(" AND cp.child_hash = cm.commit_hash ")
            .append(" INNER JOIN repository rp ")
            .append(" ON rp.id = rc.repository_id AND rp.date_updated < cm.date_created ")
            .append(" GROUP BY rc.repository_id")
            .toString();

    return futureJdbi
        .withHandle(
            handle ->
                handle
                    .createQuery(fetchUpdatedDatasetIds)
                    .setFetchSize(config.getMaxSync())
                    .map(
                        (rs, ctx) -> {
                          Long datasetId = rs.getLong("repository_id");
                          Long maxUpdatedDate = rs.getLong("max_date");
                          return new SimpleEntry<>(datasetId, maxUpdatedDate);
                        })
                    .list())
        .get();
  }

  @Override
  protected ReconcileResult reconcile(Set<AbstractMap.SimpleEntry<Long, Long>> updatedMaxDateMap) {
    logger.debug(
        "Reconciling update timestamp for repositories: "
            + updatedMaxDateMap.stream()
                .map(AbstractMap.SimpleEntry::getKey)
                .collect(Collectors.toList()));
    return futureJdbi
        .useHandle(
            handle -> {
              var updateDatasetTimestampQuery =
                  "UPDATE repository SET date_updated = :updatedDate, version_number=(version_number + 1) WHERE id = :id";

              for (AbstractMap.SimpleEntry<Long, Long> updatedRecord : updatedMaxDateMap) {
                long id = updatedRecord.getKey();
                long updatedDate = updatedRecord.getValue();
                handle
                    .createUpdate(updateDatasetTimestampQuery)
                    .bind("id", id)
                    .bind("updatedDate", updatedDate)
                    .execute();
              }
            })
        .thenApply(unused -> new ReconcileResult(), executor)
        .get();
  }
}
