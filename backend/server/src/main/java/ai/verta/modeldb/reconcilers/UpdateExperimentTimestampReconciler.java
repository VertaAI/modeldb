package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.utils.InternalFuture;
import io.opentelemetry.api.OpenTelemetry;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Set;

public class UpdateExperimentTimestampReconciler
    extends Reconciler<AbstractMap.SimpleEntry<String, Long>> {

  public UpdateExperimentTimestampReconciler(
      ReconcilerConfig config, FutureJdbi futureJdbi, OpenTelemetry openTelemetry) {
    super(config, futureJdbi, openTelemetry, false);
  }

  @Override
  public void resync() throws Exception {
    getEntitiesForDateUpdate().forEach(this::insert);
  }

  private List<SimpleEntry<String, Long>> getEntitiesForDateUpdate() throws Exception {
    var fetchUpdatedExperimentIds =
        new StringBuilder("SELECT expr.experiment_id, MAX(expr.date_updated) AS max_date ")
            .append(" FROM experiment_run expr INNER JOIN experiment e ")
            .append(" ON e.id = expr.experiment_id AND e.date_updated < expr.date_updated ")
            .append(" GROUP BY expr.experiment_id")
            .toString();

    return InternalFuture.fromFuture(
            futureJdbi.call(
                handle -> {
                  try (var findQuery = handle.createQuery(fetchUpdatedExperimentIds)) {
                    return findQuery
                        .setFetchSize(config.getMaxSync())
                        .map(
                            (rs, ctx) -> {
                              var experimentId = rs.getString("experiment_id");
                              var maxUpdatedDate = rs.getLong("max_date");
                              return new SimpleEntry<>(experimentId, maxUpdatedDate);
                            })
                        .list();
                  }
                }))
        .blockAndGet();
  }

  @Override
  protected ReconcileResult reconcile(Set<AbstractMap.SimpleEntry<String, Long>> updatedMaxDateMap)
      throws Exception {
    logger.debug(
        "Reconciling update timestamp for experiments: "
            + updatedMaxDateMap.stream().map(AbstractMap.SimpleEntry::getKey).toList());
    return futureJdbi
        .run(
            handle -> {
              var updateExperimentTimestampQuery =
                  "UPDATE experiment SET date_updated = :updatedDate, version_number=(version_number + 1) WHERE id = :id";

              for (SimpleEntry<String, Long> updatedRecord : updatedMaxDateMap) {
                var id = updatedRecord.getKey();
                long updatedDate = updatedRecord.getValue();
                try (var updateQuery = handle.createUpdate(updateExperimentTimestampQuery)) {
                  updateQuery.bind("id", id).bind("updatedDate", updatedDate).execute();
                }
              }
            })
        .thenSupply(() -> Future.of(new ReconcileResult()))
        .blockAndGet();
  }
}
