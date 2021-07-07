package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.query.OrderColumn;
import ai.verta.modeldb.common.query.OrderTable;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        return InternalFuture.completedInternalFuture(
            processKeyValueSort(names[1], ascending, "metrics"));
      case ModelDBConstants.HYPERPARAMETERS:
        final var hyperparameterSoryPredicate =
            processKeyValueSort(names[1], ascending, "hyperparameters");
        final var configHyperparameterSoryPredicate =
            processConfigHyperparametersSort(names[1], ascending);
        List<QueryFilterContext> queryFilterContexts = new ArrayList<>();
        queryFilterContexts.add(hyperparameterSoryPredicate);
        queryFilterContexts.add(configHyperparameterSoryPredicate);
        return InternalFuture.completedInternalFuture(
            QueryFilterContext.combine(queryFilterContexts));
    }

    return InternalFuture.failedStage(new InvalidArgumentException("Sort key cannot be handled"));
  }

  private QueryFilterContext processKeyValueSort(String key, boolean ascending, String fieldType) {
    var sql =
        "select experiment_run_id as id, kv_value as value from keyvalue where entity_name=:entityName and field_type=:sort_field_type and kv_key=:sort_key";
    var queryContext =
        new QueryFilterContext()
            .addBind(q -> q.bind("sort_field_type", fieldType))
            .addBind(q -> q.bind("entityName", "ExperimentRunEntity"))
            .addBind(q -> q.bind("sort_key", key));
    queryContext.addOrderItem(
        new OrderTable(
            sql, ascending, Collections.singletonList(new OrderColumn("value", ascending))));
    return queryContext;
  }

  private QueryFilterContext processConfigHyperparametersSort(String key, boolean ascending) {
    var hyperparameterFromBlobMappingSql =
        "select distinct experiment_run_id as id, int_value, float_value, string_value from hyperparameter_element_mapping where entity_type=:entity_type and experiment_run_id IS NOT NULL and name=:name";
    var queryContext =
        new QueryFilterContext()
            .addBind(q -> q.bind("entity_type", "ExperimentRunEntity"))
            .addBind(q -> q.bind("name", key));
    List<OrderColumn> orderColumnList = new ArrayList<>();
    orderColumnList.add(new OrderColumn("int_value", ascending));
    orderColumnList.add(new OrderColumn("float_value", ascending));
    orderColumnList.add(new OrderColumn("string_value", ascending));
    queryContext.addOrderItem(
        new OrderTable(hyperparameterFromBlobMappingSql, ascending, orderColumnList));
    return queryContext;
  }
}
