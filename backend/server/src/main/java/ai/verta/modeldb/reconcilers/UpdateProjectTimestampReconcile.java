package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import io.opentelemetry.api.OpenTelemetry;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateProjectTimestampReconcile
    extends Reconciler<AbstractMap.SimpleEntry<String, Long>> {

  public UpdateProjectTimestampReconcile(
      ReconcilerConfig config,
      FutureJdbi futureJdbi,
      FutureExecutor executor,
      OpenTelemetry openTelemetry) {
    super(config, futureJdbi, executor, openTelemetry, false);
  }

  @Override
  public void resync() throws Exception {
    getEntitiesForDateUpdate().forEach(this::insert);
  }

  private List<SimpleEntry<String, Long>> getEntitiesForDateUpdate() throws Exception {
    var fetchUpdatedProjectIds =
        new StringBuilder("SELECT ex.project_id as project_id, MAX(ex.date_updated) AS max_date ")
            .append(" FROM experiment ex INNER JOIN project p ")
            .append(" ON  p.id = ex.project_id AND p.date_updated < ex.date_updated ")
            .append(" GROUP BY ex.project_id")
            .toString();

    return InternalFuture.fromFuture(
            futureJdbi.call(
                handle -> {
                  try (var findQuery = handle.createQuery(fetchUpdatedProjectIds)) {
                    return findQuery
                        .setFetchSize(config.getMaxSync())
                        .map(
                            (rs, ctx) -> {
                              var projectId = rs.getString("project_id");
                              var maxUpdatedDate = rs.getLong("max_date");
                              return new SimpleEntry<>(projectId, maxUpdatedDate);
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
        "Reconciling update timestamp for projects: "
            + updatedMaxDateMap.stream()
                .map(AbstractMap.SimpleEntry::getKey)
                .collect(Collectors.toList()));
    return InternalFuture.fromFuture(
            futureJdbi.run(
                handle -> {
                  var updateProjectTimestampQuery =
                      "UPDATE project SET date_updated = :updatedDate, version_number=(version_number + 1) WHERE id = :id";

                  for (SimpleEntry<String, Long> updatedRecord : updatedMaxDateMap) {
                    var id = updatedRecord.getKey();
                    long updatedDate = updatedRecord.getValue();
                    try (var updateQuery = handle.createUpdate(updateProjectTimestampQuery)) {
                      updateQuery.bind("id", id).bind("updatedDate", updatedDate).execute();
                    }
                  }
                }))
        .thenApply(unused -> new ReconcileResult(), executor)
        .blockAndGet();
  }
}
