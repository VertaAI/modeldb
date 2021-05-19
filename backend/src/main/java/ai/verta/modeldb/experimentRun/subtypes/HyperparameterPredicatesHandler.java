package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.modeldb.exceptions.UnimplementedException;

public class HyperparameterPredicatesHandler extends PredicateHandlerUtils {

  public InternalFuture<QueryFilterContext> processHyperparametersPredicate(
      long index, KeyValueQuery predicate, String name, String fieldType) {
    final var value = predicate.getValue();
    final var operator = predicate.getOperator();

    final var valueBindingKey = String.format("k_p_%d", index);
    final var valueBindingName = String.format("v_p_%d", index);
    final var fieldTypeName = String.format("field_type_%d", index);

    var sql =
        "select distinct experiment_run_id from keyvalue where entity_name=:entityName and field_type=:"
            + fieldTypeName;
    sql += String.format(" and kv_key=:%s ", valueBindingKey);
    sql += " and ";

    final var colValue = "kv_value";
    var queryContext =
        new QueryFilterContext()
            .addBind(q -> q.bind(valueBindingKey, name))
            .addBind(q -> q.bind("entityName", "ExperimentRunEntity"))
            .addBind(q -> q.bind(fieldTypeName, fieldType));

    switch (value.getKindCase()) {
      case NUMBER_VALUE:
        sql += applyOperator(operator, colValue, ":" + valueBindingName);
        queryContext = queryContext.addBind(q -> q.bind(valueBindingName, value.getNumberValue()));
        break;
      case STRING_VALUE:
        sql += applyOperator(operator, colValue, ":" + valueBindingName);
        queryContext =
            queryContext.addBind(
                q -> q.bind(valueBindingName, wrapValue(operator, value.getStringValue())));
        break;
      default:
        return InternalFuture.failedStage(
            new UnimplementedException("Unknown 'Value' type recognized"));
    }

    String finalHyperparametersFromERSql;
    if (predicate.getOperator().equals(OperatorEnum.Operator.NOT_CONTAIN)
        || predicate.getOperator().equals(OperatorEnum.Operator.NE)) {
      finalHyperparametersFromERSql = String.format("experiment_run.id NOT IN (%s)", sql);
    } else {
      finalHyperparametersFromERSql = String.format("experiment_run.id IN (%s)", sql);
    }

    var hyperparameterFromBlobMappingSql =
        "select distinct experiment_run_id from hyperparameter_element_mapping where entity_type=:entity_type and experiment_run_id IS NOT NULL ";
    hyperparameterFromBlobMappingSql += String.format(" and name=:%s ", valueBindingKey);
    hyperparameterFromBlobMappingSql += " and ";

    queryContext = queryContext.addBind(q -> q.bind("entity_type", "ExperimentRunEntity"));

    switch (value.getKindCase()) {
      case NUMBER_VALUE:
        String intColumnSQL = applyOperator(operator, "int_value", ":" + valueBindingName);
        String floatColumnSQL = applyOperator(operator, "float_value", ":" + valueBindingName);

        hyperparameterFromBlobMappingSql +=
            " (" + String.join(" OR ", intColumnSQL, floatColumnSQL) + ") ";
        queryContext = queryContext.addBind(q -> q.bind(valueBindingName, value.getNumberValue()));
        break;
      case STRING_VALUE:
        hyperparameterFromBlobMappingSql +=
            applyOperator(operator, "string_value", ":" + valueBindingName);
        queryContext =
            queryContext.addBind(
                q -> q.bind(valueBindingName, wrapValue(operator, value.getStringValue())));
        break;
      default:
        return InternalFuture.failedStage(
            new UnimplementedException("Unknown 'Value' type recognized"));
    }

    String finalHyperparameterFromBlobMappingSql;
    if (predicate.getOperator().equals(OperatorEnum.Operator.NOT_CONTAIN)
        || predicate.getOperator().equals(OperatorEnum.Operator.NE)) {
      finalHyperparameterFromBlobMappingSql =
          String.format("experiment_run.id NOT IN (%s)", hyperparameterFromBlobMappingSql);
    } else {
      finalHyperparameterFromBlobMappingSql =
          String.format("experiment_run.id IN (%s)", hyperparameterFromBlobMappingSql);
    }

    if (predicate.getOperator().equals(OperatorEnum.Operator.NOT_CONTAIN)
        || predicate.getOperator().equals(OperatorEnum.Operator.NE)) {
      queryContext =
          queryContext.addCondition(
              "("
                  + String.join(
                      " AND ", finalHyperparametersFromERSql, finalHyperparameterFromBlobMappingSql)
                  + ")");
    } else {
      queryContext =
          queryContext.addCondition(
              "("
                  + String.join(
                      " OR ", finalHyperparametersFromERSql, finalHyperparameterFromBlobMappingSql)
                  + ")");
    }
    return InternalFuture.completedInternalFuture(queryContext);
  }
}
