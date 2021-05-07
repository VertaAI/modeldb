package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.query.OrderColumn;
import ai.verta.modeldb.common.query.OrderTable;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.modeldb.exceptions.InvalidArgumentException;

public class SortingHandler {
  public InternalFuture<QueryFilterContext> processSort(String key, boolean ascending) {
    if (key == null || key.isEmpty()) {
      key = "date_updated";
    }

    switch (key) {
      case "date_updated":
      case "date_created":
      case "name":
        return InternalFuture.completedInternalFuture(
            new QueryFilterContext()
                .addOrderItem(new OrderColumn("experiment_run." + key, ascending)));
    }

    String[] names = key.split("\\.");
    // TODO: check length is 2

    switch (names[0]) {
      case ModelDBConstants.METRICS:
        return processKeyValueSort(names[1], ascending, "metrics");
      case ModelDBConstants.HYPERPARAMETERS:
        return processKeyValueSort(names[1], ascending, "hyperparameters");
    }

    return InternalFuture.failedStage(new InvalidArgumentException("Sort key cannot be handled"));
  }

  private InternalFuture<QueryFilterContext> processKeyValueSort(
      String key, boolean ascending, String fieldType) {
    var sql =
        "select experiment_run_id as id, kv_value as value from keyvalue where entity_name=\"ExperimentRunEntity\" and field_type=:sort_field_type and kv_key=:sort_key";
    var queryContext =
        new QueryFilterContext()
            .addBind(q -> q.bind("sort_field_type", fieldType))
            .addBind(q -> q.bind("sort_key", key));
    queryContext.addOrderItem(new OrderTable(sql, ascending));
    return InternalFuture.completedInternalFuture(queryContext);
  }
}
