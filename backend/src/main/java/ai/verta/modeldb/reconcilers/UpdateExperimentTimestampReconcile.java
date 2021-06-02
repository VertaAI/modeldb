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
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpdateExperimentTimestampReconcile
    extends Reconciler<AbstractMap.SimpleEntry<String, Long>> {
  private static final Logger LOGGER =
      LogManager.getLogger(UpdateExperimentTimestampReconcile.class);

  public UpdateExperimentTimestampReconcile(
      ReconcilerConfig config, FutureJdbi futureJdbi, Executor executor) {
    super(config, LOGGER, futureJdbi, executor);
  }

  @Override
  public void resync() {
    var fetchUpdatedExperimentIds =
        new StringBuilder("SELECT expr.experiment_id, MAX(expr.date_updated) AS max_date ")
            .append(" FROM experiment_run expr INNER JOIN experiment e ")
            .append(" ON e.id = expr.experiment_id AND e.date_updated < expr.date_updated ")
            .append(" GROUP BY expr.experiment_id")
            .toString();

    futureJdbi.useHandle(
        handle -> {
          handle
              .createQuery(fetchUpdatedExperimentIds)
              .setFetchSize(config.maxSync)
              .map(
                  (rs, ctx) -> {
                    String experimentId = rs.getString("expr.experiment_id");
                    Long maxUpdatedDate = rs.getLong("max_date");
                    this.insert(new AbstractMap.SimpleEntry<>(experimentId, maxUpdatedDate));
                    return rs;
                  })
              .list();
        });
  }

  @Override
  protected ReconcileResult reconcile(
      Set<AbstractMap.SimpleEntry<String, Long>> updatedMaxDateMap) {
    LOGGER.debug(
        "Reconciling update timestamp for experiments: "
            + updatedMaxDateMap.stream()
                .map(AbstractMap.SimpleEntry::getKey)
                .collect(Collectors.toList()));
    try {
      return futureJdbi
          .useHandle(
              handle -> {
                var updateExperimentTimestampQuery =
                    "UPDATE experiment SET date_updated = :updatedDate WHERE id = :id";

                final var batch = handle.prepareBatch(updateExperimentTimestampQuery);
                for (AbstractMap.SimpleEntry<String, Long> updatedRecord : updatedMaxDateMap) {
                  var id = updatedRecord.getKey();
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
