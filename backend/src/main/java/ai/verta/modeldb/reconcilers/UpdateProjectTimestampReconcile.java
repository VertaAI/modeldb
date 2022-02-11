package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import java.util.AbstractMap;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;

public class UpdateProjectTimestampReconcile
    extends Reconciler<AbstractMap.SimpleEntry<String, Long>> {

  public UpdateProjectTimestampReconcile(
      ReconcilerConfig config, FutureJdbi futureJdbi, Executor executor) {
    super(
        config,
        LogManager.getLogger(UpdateProjectTimestampReconcile.class),
        futureJdbi,
        executor,
        false);
  }

  @Override
  public void resync() {
    var fetchUpdatedProjectIds =
        new StringBuilder("SELECT ex.project_id, MAX(ex.date_updated) AS max_date ")
            .append(" FROM experiment ex INNER JOIN project p ")
            .append(" ON  p.id = ex.project_id AND p.date_updated < ex.date_updated ")
            .append(" GROUP BY ex.project_id")
            .toString();

    futureJdbi.useHandle(
        handle ->
            handle
                .createQuery(fetchUpdatedProjectIds)
                .setFetchSize(config.getMaxSync())
                .map(
                    (rs, ctx) -> {
                      var projectId = rs.getString("ex.project_id");
                      var maxUpdatedDate = rs.getLong("max_date");
                      this.insert(new AbstractMap.SimpleEntry<>(projectId, maxUpdatedDate));
                      return rs;
                    })
                .list());
  }

  @Override
  protected ReconcileResult reconcile(
      Set<AbstractMap.SimpleEntry<String, Long>> updatedMaxDateMap) {
    logger.debug(
        "Reconciling update timestamp for projects: "
            + updatedMaxDateMap.stream()
            .map(AbstractMap.SimpleEntry::getKey)
            .collect(Collectors.toList()));
    return futureJdbi
        .useHandle(
            handle -> {
              var updateProjectTimestampQuery =
                  "UPDATE project SET date_updated = :updatedDate, version_number=(version_number + 1) WHERE id = :id";

              final var batch = handle.prepareBatch(updateProjectTimestampQuery);
              for (AbstractMap.SimpleEntry<String, Long> updatedRecord : updatedMaxDateMap) {
                var id = updatedRecord.getKey();
                long updatedDate = updatedRecord.getValue();
                batch.bind("id", id).bind("updatedDate", updatedDate).add();
              }

              batch.execute();
            })
        .thenApply(unused -> new ReconcileResult(), executor)
        .get();
  }
}
