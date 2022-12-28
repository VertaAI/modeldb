package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.UnimplementedException;
import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.query.QueryFilterContext;
import java.util.Date;

public class HyperparameterPredicatesHandler extends PredicateHandlerUtils {

  public Future<QueryFilterContext> processHyperparametersPredicate(
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

    try {
      switch (value.getKindCase()) {
        case NUMBER_VALUE:
          sql += applyOperator(operator, columnAsNumber(colValue, true), ":" + valueBindingName);
          queryContext =
              queryContext.addBind(q -> q.bind(valueBindingName, value.getNumberValue()));
          break;
        case STRING_VALUE:
          sql += applyOperator(operator, colValue, ":" + valueBindingName);
          var valueStr = CommonUtils.getStringFromProtoObject(value);
          if (operator.equals(OperatorEnum.Operator.CONTAIN)
              || operator.equals(OperatorEnum.Operator.NOT_CONTAIN)) {
            valueStr = value.getStringValue();
          }
          final var finalValueStr = valueStr;
          queryContext =
              queryContext.addBind(
                  q -> q.bind(valueBindingName, wrapValue(operator, finalValueStr)));
          break;
        default:
          return Future.failedStage(new UnimplementedException("Unknown 'Value' type recognized"));
      }
    } catch (Exception ex) {
      return Future.failedStage(ex);
    }

    String finalHyperparametersFromERSql;
    if (predicate.getOperator().equals(OperatorEnum.Operator.NOT_CONTAIN)
        || predicate.getOperator().equals(OperatorEnum.Operator.NE)) {
      finalHyperparametersFromERSql = String.format("experiment_run.id NOT IN (%s)", sql);
    } else {
      finalHyperparametersFromERSql = String.format("experiment_run.id IN (%s)", sql);
    }

    final var valueBindingNameForHyperBlob =
        String.format("v_p_%d_%d", index, new Date().getTime());
    var hyperparameterFromBlobMappingSql =
        "select distinct experiment_run_id from hyperparameter_element_mapping where entity_type=:entity_type and experiment_run_id IS NOT NULL ";
    hyperparameterFromBlobMappingSql += String.format(" and name=:%s ", valueBindingKey);
    hyperparameterFromBlobMappingSql += " and ";

    queryContext = queryContext.addBind(q -> q.bind("entity_type", "ExperimentRunEntity"));

    switch (value.getKindCase()) {
      case NUMBER_VALUE:
        String intColumnSQL =
            applyOperator(operator, "int_value", ":" + valueBindingNameForHyperBlob);
        String floatColumnSQL =
            applyOperator(operator, "float_value", ":" + valueBindingNameForHyperBlob);

        hyperparameterFromBlobMappingSql +=
            " (" + String.join(" OR ", intColumnSQL, floatColumnSQL) + ") ";
        queryContext =
            queryContext.addBind(q -> q.bind(valueBindingNameForHyperBlob, value.getNumberValue()));
        break;
      case STRING_VALUE:
        hyperparameterFromBlobMappingSql +=
            applyOperator(operator, "string_value", ":" + valueBindingNameForHyperBlob);
        queryContext =
            queryContext.addBind(
                q ->
                    q.bind(
                        valueBindingNameForHyperBlob, wrapValue(operator, value.getStringValue())));
        break;
      default:
        return Future.failedStage(new UnimplementedException("Unknown 'Value' type recognized"));
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
    return Future.of(queryContext);
  }
}
