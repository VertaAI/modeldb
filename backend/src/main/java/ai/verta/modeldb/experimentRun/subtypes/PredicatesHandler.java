package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.exceptions.UnimplementedException;
import com.google.protobuf.Value;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

public class PredicatesHandler {
  private final Config config = Config.getInstance();

  public InternalFuture<QueryFilterContext> processPredicates(
      List<KeyValueQuery> predicates, Executor executor) {
    final var futureFilters = new LinkedList<InternalFuture<QueryFilterContext>>();
    final var iterator = predicates.listIterator();
    while (iterator.hasNext()) {
      futureFilters.add(processPredicate(iterator.nextIndex(), iterator.next()));
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
        return InternalFuture.completedInternalFuture(
            new QueryFilterContext()
                .addCondition("experiment_run.experiment_id = :" + bindingName)
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
    }

    String[] names = key.split("\\.");
    // TODO: check length is 2

    switch (names[0]) {
      case ModelDBConstants.METRICS:
        return processMetricsPredicate(index, predicate, names[1]);
      case ModelDBConstants.ARTIFACTS:
      case ModelDBConstants.DATASETS:
      case ModelDBConstants.ATTRIBUTES:
      case ModelDBConstants.HYPERPARAMETERS:
      case ModelDBConstants.OBSERVATIONS:
        // case ModelDBConstants.FEATURES: TODO?
      case ModelDBConstants.TAGS:
      case ModelDBConstants.VERSIONED_INPUTS:
    }

    // TODO: handle arbitrary key

    return InternalFuture.failedStage(new InvalidArgumentException("Predicate cannot be handled"));
  }

  private InternalFuture<QueryFilterContext> processMetricsPredicate(
      long index, KeyValueQuery predicate, String name) {
    final var value = predicate.getValue();
    final var operator = predicate.getOperator();

    final var valueBindingKey = String.format("k_p_%d", index);
    final var valueBindingName = String.format("v_p_%d", index);

    var sql =
        "select distinct experiment_run_id from keyvalue where entity_name=\"ExperimentRunEntity\" and field_type=\"metrics\"";
    sql += String.format(" and kv_key=:%s ", valueBindingKey);
    sql += " and ";

    final var colValue = "kv_value";
    var queryContext = new QueryFilterContext().addBind(q -> q.bind(valueBindingKey, name));

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

    queryContext = queryContext.addCondition(String.format("experiment_run.id in (%s)", sql));

    return InternalFuture.completedInternalFuture(queryContext);
  }

  private String columnAsNumber(String colName, boolean isString) {
    if (config.database.RdbConfiguration.isPostgres()) {
      if (isString) {
        return String.format("cast(trim('\"' from %s) as double precision)", colName);
      } else {
        return String.format("cast(%s as double precision)", colName);
      }
    } else {
      return String.format("cast(%s as decimal)", colName);
    }
  }

  private String applyOperator(
      OperatorEnum.Operator operator, String colName, String valueBinding) {
    switch (operator.ordinal()) {
      case OperatorEnum.Operator.GT_VALUE:
        return String.format("%s > %s", colName, valueBinding);
      case OperatorEnum.Operator.GTE_VALUE:
        return String.format("%s >= %s", colName, valueBinding);
      case OperatorEnum.Operator.LT_VALUE:
        return String.format("%s < %s", colName, valueBinding);
      case OperatorEnum.Operator.LTE_VALUE:
        return String.format("%s <= %s", colName, valueBinding);
      case OperatorEnum.Operator.NE_VALUE:
        return String.format("%s != %s", colName, valueBinding);
      case OperatorEnum.Operator.CONTAIN_VALUE:
        return String.format("%s LIKE %s", colName, valueBinding);
      case OperatorEnum.Operator.NOT_CONTAIN_VALUE:
        return String.format("%s NOT LIKE %s", colName, valueBinding);
      case OperatorEnum.Operator.IN_VALUE:
        return String.format("%s IN (%s)", colName, valueBinding);
      default:
        return String.format("%s = %s", colName, valueBinding);
    }
  }

  private String wrapValue(OperatorEnum.Operator operator, String value) {
    switch (operator.ordinal()) {
      case OperatorEnum.Operator.CONTAIN_VALUE:
      case OperatorEnum.Operator.NOT_CONTAIN_VALUE:
        return String.format("%%%s%%", value);
      default:
        return value;
    }
  }
}
