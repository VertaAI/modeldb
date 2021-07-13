package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.common.EnumerateList;
import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.modeldb.exceptions.UnimplementedException;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.Value;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

public class PredicatesHandler extends PredicateHandlerUtils {
  private final HyperparameterPredicatesHandler hyperparameterPredicatesHandler;

  public PredicatesHandler() {
    this.hyperparameterPredicatesHandler = new HyperparameterPredicatesHandler();
  }

  public InternalFuture<QueryFilterContext> processPredicates(
      List<KeyValueQuery> predicates, Executor executor) {
    final var futureFilters = new LinkedList<InternalFuture<QueryFilterContext>>();
    for (final var item : new EnumerateList<>(predicates).getList()) {
      futureFilters.add(processPredicate(item.getIndex(), item.getValue()));
    }

    return InternalFuture.sequence(futureFilters, executor)
        .thenApply(QueryFilterContext::combine, executor);
  }

  private InternalFuture<QueryFilterContext> processPredicate(long index, KeyValueQuery predicate) {
    final var key = predicate.getKey();
    final var value = predicate.getValue();

    if (!key.contains(ModelDBConstants.LINKED_ARTIFACT_ID)
        && predicate.getOperator().equals(OperatorEnum.Operator.IN)) {
      return InternalFuture.failedStage(
          new InvalidArgumentException(
              "Operator `IN` supported only with the linked_artifact_id as a key"));
    }

    final var bindingName = String.format("predicate_%d", index);

    // TODO: don't assume that the operator is equality for these
    switch (key) {
      case "id":
        return InternalFuture.completedInternalFuture(
            new QueryFilterContext()
                .addCondition("experiment_run.id = :" + bindingName)
                .addBind(q -> q.bind(bindingName, value.getStringValue())));
      case "project_id":
        return InternalFuture.completedInternalFuture(
            new QueryFilterContext()
                .addCondition("experiment_run.project_id = :" + bindingName)
                .addBind(q -> q.bind(bindingName, value.getStringValue())));
      case "experiment_id":
        String operator = predicate.getOperator().equals(OperatorEnum.Operator.NE) ? "<>" : "=";
        return InternalFuture.completedInternalFuture(
            new QueryFilterContext()
                .addCondition("experiment_run.experiment_id " + operator + " :" + bindingName)
                .addBind(q -> q.bind(bindingName, value.getStringValue())));
      case "name":
        return InternalFuture.completedInternalFuture(
            new QueryFilterContext()
                .addCondition("experiment_run.name = :" + bindingName)
                .addBind(q -> q.bind(bindingName, value.getStringValue())));
      case "owner":
        // case time created/updated:
        // case visibility:
      case "":
        return InternalFuture.failedStage(new InvalidArgumentException("Key is empty"));
      case "end_time":
        return InternalFuture.completedInternalFuture(
            new QueryFilterContext()
                .addCondition("experiment_run.end_time = :" + bindingName)
                .addBind(q -> q.bind(bindingName, value.getStringValue())));
    }

    String[] names = key.split("\\.");
    // TODO: check length is 2

    switch (names[0]) {
      case ModelDBConstants.METRICS:
        return processKeyValuePredicate(index, predicate, names[1], "metrics");
      case ModelDBConstants.HYPERPARAMETERS:
        return hyperparameterPredicatesHandler.processHyperparametersPredicate(
            index, predicate, names[1], "hyperparameters");
      case ModelDBConstants.ARTIFACTS:
        return processArtifactPredicate(index, predicate, names[1], "artifacts");
      case ModelDBConstants.DATASETS:
        return processArtifactPredicate(index, predicate, names[1], "datasets");
      case ModelDBConstants.ATTRIBUTES:
        return processAttributePredicate(index, predicate, names[1], "attributes");

      case ModelDBConstants.OBSERVATIONS:
        return processObservationPredicate(
            index, predicate, Arrays.copyOfRange(names, 1, names.length));
        // case ModelDBConstants.FEATURES: TODO?
      case ModelDBConstants.TAGS:
        return processTagsPredicate(index, predicate);
      case ModelDBConstants.VERSIONED_INPUTS:
    }

    // TODO: handle arbitrary key

    return InternalFuture.failedStage(new InvalidArgumentException("Predicate cannot be handled"));
  }

  private InternalFuture<QueryFilterContext> processTagsPredicate(
      long index, KeyValueQuery predicate) {
    try {
      final var value = predicate.getValue();
      var operator = predicate.getOperator();

      final var valueBindingName = String.format("v_t_%d", index);
      final var entityNameBindingName = String.format("entity_name_%d", index);

      var sql =
          "select distinct experiment_run_id from tag_mapping where entity_name=:"
              + entityNameBindingName;
      sql += " and ";

      final var colValue = "tags";
      var queryContext =
          new QueryFilterContext()
              .addBind(q -> q.bind(entityNameBindingName, "ExperimentRunEntity"));

      switch (value.getKindCase()) {
        case STRING_VALUE:
          sql += applyOperator(operator, colValue, ":" + valueBindingName);
          queryContext =
              queryContext.addBind(
                  q -> q.bind(valueBindingName, wrapValue(operator, value.getStringValue())));
          break;
        case LIST_VALUE:
          List<Object> valueList = new LinkedList<>();
          for (final var item : value.getListValue().getValuesList()) {
            if (item.getKindCase().ordinal() == Value.KindCase.STRING_VALUE.ordinal()) {
              valueList.add(item.getStringValue());
            }
          }

          sql += applyOperator(operator, colValue, "<" + valueBindingName + ">");
          queryContext = queryContext.addBind(q -> q.bindList(valueBindingName, valueList));
          break;
        default:
          return InternalFuture.failedStage(
              new UnimplementedException("Unknown 'Value' type: " + value.getKindCase().name()));
      }

      if (operator.equals(OperatorEnum.Operator.NOT_CONTAIN)
          || operator.equals(OperatorEnum.Operator.NE)) {
        queryContext =
            queryContext.addCondition(String.format("experiment_run.id NOT IN (%s)", sql));
      } else {
        queryContext = queryContext.addCondition(String.format("experiment_run.id IN (%s)", sql));
      }

      return InternalFuture.completedInternalFuture(queryContext);
    } catch (Exception ex) {
      return InternalFuture.failedStage(ex);
    }
  }

  private InternalFuture<QueryFilterContext> processObservationPredicate(
      long index, KeyValueQuery predicate, String[] names) {

    final var errorMessage =
        "Invalid predicate for observations, Valid format is like `observations.attribute.att_key`";
    if (names.length != 2) {
      return InternalFuture.failedStage(new InvalidArgumentException(errorMessage));
    }

    switch (names[0]) {
      case "attribute":
        return processKeyValuePredicate(index, predicate, names[1], "observations");
      case "artifact":
        // TODO: Implement here after adding support on insertion ER
      default:
        return InternalFuture.failedStage(new InvalidArgumentException(errorMessage));
    }
  }

  private InternalFuture<QueryFilterContext> processKeyValuePredicate(
      long index, KeyValueQuery predicate, String name, String fieldType) {
    final var value = predicate.getValue();
    final var operator = predicate.getOperator();

    final var valueBindingKey = String.format("k_p_%d", index);
    final var valueBindingName = String.format("v_p_%d", index);
    final var fieldTypeName = String.format("field_type_%d", index);

    String sql;
    if (fieldType.equals("observations")) {
      sql =
          "select distinct ob.experiment_run_id from observation as ob "
              + " inner join keyvalue as kv ON kv.id = ob.keyvaluemapping_id"
              + " where ob.entity_name=:entityName and ob.field_type=:"
              + fieldTypeName
              + " and kv.field_type = 'attributes' ";
    } else {
      sql =
          "select distinct kv.experiment_run_id from keyvalue as kv where kv.entity_name=:entityName and kv.field_type=:"
              + fieldTypeName;
    }

    sql += String.format(" and kv.kv_key=:%s ", valueBindingKey);
    sql += " and ";

    final var colValue = "kv.kv_value";
    var queryContext =
        new QueryFilterContext()
            .addBind(q -> q.bind(valueBindingKey, name))
            .addBind(q -> q.bind("entityName", "ExperimentRunEntity"))
            .addBind(q -> q.bind(fieldTypeName, fieldType));

    switch (value.getKindCase()) {
      case NUMBER_VALUE:
        sql += applyOperator(operator, columnAsNumber(colValue, true), ":" + valueBindingName);
        queryContext = queryContext.addBind(q -> q.bind(valueBindingName, value.getNumberValue()));
        break;
      case STRING_VALUE:
        sql += applyOperator(operator, colValue, ":" + valueBindingName);
        queryContext =
            queryContext.addBind(
                q -> q.bind(valueBindingName, wrapValue(operator, value.getStringValue())));
        break;
      case BOOL_VALUE:
        sql += applyOperator(operator, colValue, ":" + valueBindingName);
        queryContext = queryContext.addBind(q -> q.bind(valueBindingName, value.getBoolValue()));
        break;
      case LIST_VALUE:
        List<Object> valueList = new LinkedList<>();
        for (final var item : value.getListValue().getValuesList()) {
          if (item.getKindCase().ordinal() == Value.KindCase.STRING_VALUE.ordinal()) {
            valueList.add(item.getStringValue());
          } else if (item.getKindCase().ordinal() == Value.KindCase.NUMBER_VALUE.ordinal()) {
            valueList.add(item.getNumberValue());
          }
        }

        sql += applyOperator(operator, colValue, "<" + valueBindingName + ">");
        queryContext = queryContext.addBind(q -> q.bindList(valueBindingName, valueList));
        break;
      default:
        return InternalFuture.failedStage(
            new UnimplementedException("Unknown 'Value' type recognized"));
    }

    if (operator.equals(OperatorEnum.Operator.NOT_CONTAIN)
        || operator.equals(OperatorEnum.Operator.NE)) {
      queryContext = queryContext.addCondition(String.format("experiment_run.id NOT IN (%s)", sql));
    } else {
      queryContext = queryContext.addCondition(String.format("experiment_run.id IN (%s)", sql));
    }

    return InternalFuture.completedInternalFuture(queryContext);
  }

  private InternalFuture<QueryFilterContext> processAttributePredicate(
      long index, KeyValueQuery predicate, String name, String fieldType) {
    try {
      final var value = predicate.getValue();
      var operator = predicate.getOperator();

      final var valueBindingKey = String.format("k_p_%d", index);
      final var valueBindingName = String.format("v_p_%d", index);
      final var fieldTypeName = String.format("field_type_%d", index);

      var sql =
          "select distinct experiment_run_id from attribute where entity_name=\"ExperimentRunEntity\" and field_type=:"
              + fieldTypeName;
      sql += String.format(" and kv_key=:%s ", valueBindingKey);
      sql += " and ";

      final var colValue = "kv_value";
      var queryContext =
          new QueryFilterContext()
              .addBind(q -> q.bind(valueBindingKey, name))
              .addBind(q -> q.bind(fieldTypeName, fieldType));

      switch (value.getKindCase()) {
        case STRING_VALUE:
          sql += applyOperator(operator, colValue, ":" + valueBindingName);
          var valueStr = ModelDBUtils.getStringFromProtoObject(value);
          if (operator.equals(OperatorEnum.Operator.CONTAIN)) {
            valueStr = value.getStringValue();
          }
          final var finalValueStr = valueStr;
          queryContext =
              queryContext.addBind(
                  q -> q.bind(valueBindingName, wrapValue(operator, finalValueStr)));
          break;
        case LIST_VALUE:
          List<Object> valueList = new LinkedList<>();
          for (final var value1 : value.getListValue().getValuesList()) {
            if (value1.getKindCase().ordinal() == Value.KindCase.STRING_VALUE.ordinal()) {
              var valueStr1 = ModelDBUtils.getStringFromProtoObject(value1);
              if (operator.equals(OperatorEnum.Operator.CONTAIN)) {
                valueStr1 = value.getStringValue();
              }
              valueList.add(valueStr1);
            }
          }

          if (!valueList.isEmpty()) {
            sql += applyOperator(operator, colValue, "<" + valueBindingName + ">");
            queryContext = queryContext.addBind(q -> q.bindList(valueBindingName, valueList));
          }
          break;
        default:
          return InternalFuture.failedStage(
              new UnimplementedException("Unknown 'Value' type recognized"));
      }

      if (predicate.getOperator().equals(OperatorEnum.Operator.NOT_CONTAIN)
          || predicate.getOperator().equals(OperatorEnum.Operator.NE)) {
        queryContext =
            queryContext.addCondition(String.format("experiment_run.id NOT IN (%s)", sql));
      } else {
        queryContext = queryContext.addCondition(String.format("experiment_run.id IN (%s)", sql));
      }

      return InternalFuture.completedInternalFuture(queryContext);
    } catch (Exception ex) {
      return InternalFuture.failedStage(ex);
    }
  }

  private InternalFuture<QueryFilterContext> processArtifactPredicate(
      long index, KeyValueQuery predicate, String name, String fieldType) {
    try {
      final var value = predicate.getValue();
      var operator = predicate.getOperator();

      final var valueBindingKey = String.format("k_p_%d", index);
      final var valueBindingName = String.format("v_p_%d", index);
      final var fieldTypeName = String.format("field_type_%d", index);

      var sql =
          "select distinct experiment_run_id from artifact where entity_name=:entityName and field_type=:"
              + fieldTypeName;

      var queryContext =
          new QueryFilterContext()
              .addBind(q -> q.bind("entityName", "ExperimentRunEntity"))
              .addBind(q -> q.bind(fieldTypeName, fieldType));

      String colValue;
      if (name.equals(ModelDBConstants.LINKED_ARTIFACT_ID)) {
        colValue = "linked_artifact_id";
      } else {
        sql += String.format(" and ar_key=:%s ", valueBindingKey);

        colValue = "ar_value";
        queryContext.addBind(q -> q.bind(valueBindingKey, name));
      }
      sql += " and ";

      switch (value.getKindCase()) {
        case STRING_VALUE:
          sql += applyOperator(operator, colValue, ":" + valueBindingName);
          String valueStr;
          if (name.equals(ModelDBConstants.LINKED_ARTIFACT_ID)
              || operator.equals(OperatorEnum.Operator.CONTAIN)) {
            valueStr = value.getStringValue();
          } else {
            valueStr = ModelDBUtils.getStringFromProtoObject(value);
          }
          String finalValueStr = valueStr;
          queryContext =
              queryContext.addBind(
                  q -> q.bind(valueBindingName, wrapValue(operator, finalValueStr)));
          break;
        case LIST_VALUE:
          List<Object> valueList = new LinkedList<>();
          for (final var value1 : value.getListValue().getValuesList()) {
            if (value1.getKindCase().ordinal() == Value.KindCase.STRING_VALUE.ordinal()) {
              var valueStr1 = ModelDBUtils.getStringFromProtoObject(value1);
              if (operator.equals(OperatorEnum.Operator.CONTAIN)) {
                valueStr1 = value.getStringValue();
              }
              valueList.add(valueStr1);
            }
          }

          if (!valueList.isEmpty()) {
            sql += applyOperator(operator, colValue, "<" + valueBindingName + ">");
            queryContext = queryContext.addBind(q -> q.bindList(valueBindingName, valueList));
          }
          break;
        default:
          return InternalFuture.failedStage(
              new UnimplementedException("Unknown 'Value' type recognized"));
      }

      if (predicate.getOperator().equals(OperatorEnum.Operator.NOT_CONTAIN)
          || predicate.getOperator().equals(OperatorEnum.Operator.NE)) {
        queryContext =
            queryContext.addCondition(String.format("experiment_run.id NOT IN (%s)", sql));
      } else {
        queryContext = queryContext.addCondition(String.format("experiment_run.id IN (%s)", sql));
      }

      return InternalFuture.completedInternalFuture(queryContext);
    } catch (Exception ex) {
      return InternalFuture.failedStage(ex);
    }
  }
}
