package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.query.OrderColumn;
import ai.verta.modeldb.common.query.OrderTable;
import ai.verta.modeldb.common.query.QueryFilterContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SortingHandler {

  private final String tableName;

  public SortingHandler(String tableName) {
    this.tableName = tableName;
  }

  private String getEntityColumn() {
    String entityColumn = "";
    switch (tableName) {
      case "project":
        entityColumn = "project_id";
        break;
      case "experiment":
        entityColumn = "experiment_id";
        break;
      case "experiment_run":
        entityColumn = "experiment_run_id";
        break;
    }
    return entityColumn;
  }

  private String getEntityName() {
    String entityColumn = "";
    switch (tableName) {
      case "project":
        entityColumn = "ProjectEntity";
        break;
      case "experiment":
        entityColumn = "ExperimentEntity";
        break;
      case "experiment_run":
        entityColumn = "ExperimentRunEntity";
        break;
    }
    return entityColumn;
  }

  public InternalFuture<QueryFilterContext> processSort(String key, boolean ascending) {
    if (key == null || key.isEmpty()) {
      key = "date_updated";
    }

    switch (key) {
      case "date_updated":
      case "date_created":
      case "name":
        return InternalFuture.completedInternalFuture(
            new QueryFilterContext().addOrderItem(new OrderColumn(key, ascending)));
      default:
        // Do nothing
        break;
    }

    String[] names = key.split("\\.");
    // TODO: check length is 2

    switch (names[0]) {
      case ModelDBConstants.METRICS:
        return InternalFuture.completedInternalFuture(
            processKeyValueSort(names[1], ascending, "metrics"));
      case ModelDBConstants.ATTRIBUTES:
        return InternalFuture.completedInternalFuture(
            processAttributeSort(names[1], ascending, "attributes"));
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
      default:
        // Do nothing
        break;
    }

    return InternalFuture.failedStage(new InvalidArgumentException("Sort key cannot be handled"));
  }

  private QueryFilterContext processKeyValueSort(String key, boolean ascending, String fieldType) {
    var sql =
        String.format(
            "select %s as entityId, kv_value as value from keyvalue where entity_name=:entityName and field_type=:sort_field_type and kv_key=:sort_key",
            getEntityColumn());
    var queryContext =
        new QueryFilterContext()
            .addBind(q -> q.bind("sort_field_type", fieldType))
            .addBind(q -> q.bind("entityName", getEntityName()))
            .addBind(q -> q.bind("sort_key", key));
    queryContext.addOrderItem(
        new OrderTable(
            sql, ascending, Collections.singletonList(new OrderColumn("value", ascending))));
    return queryContext;
  }

  private QueryFilterContext processAttributeSort(String key, boolean ascending, String fieldType) {
    var sql =
        String.format(
            "select %s as entityId, kv_value as kvValue from attribute where entity_name=:entityName and field_type=:sort_field_type and kv_key=:sort_key",
            getEntityColumn());
    var queryContext =
        new QueryFilterContext()
            .addBind(q -> q.bind("sort_field_type", fieldType))
            .addBind(q -> q.bind("entityName", getEntityName()))
            .addBind(q -> q.bind("sort_key", key));
    queryContext.addOrderItem(
        new OrderTable(
            sql, ascending, Collections.singletonList(new OrderColumn("kvValue", ascending))));
    return queryContext;
  }

  private QueryFilterContext processConfigHyperparametersSort(String key, boolean ascending) {
    var hyperparameterFromBlobMappingSql =
        String.format(
            "select distinct %s as entityId, int_value, float_value, string_value from hyperparameter_element_mapping where entity_type=:entity_type and experiment_run_id IS NOT NULL and name=:name",
            getEntityColumn());
    var queryContext =
        new QueryFilterContext()
            .addBind(q -> q.bind("entity_type", getEntityName()))
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
