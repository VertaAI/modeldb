package ai.verta.modeldb.experimentrun.subtypes;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.EnumerateList;
import ai.verta.modeldb.common.authservice.UACApisUtil;
import ai.verta.modeldb.common.dto.UserInfoPaginationDTO;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.UnimplementedException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.query.QueryFilterContext;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Value;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PredicatesHandler extends PredicateHandlerUtils {
  private static final String ENTITY_ID_NOT_IN_QUERY_CONDITION = "%s.id NOT IN (%s)";
  private static final String ENTITY_ID_IN_QUERY_CONDITION = "%s.id IN (%s)";
  private static final String K_P_D_VALUE_BINDING_KEY = "k_p_%d";
  private static final String V_P_D_VALUE_BINDING_NAME = "v_p_%d";
  private static final String FIELD_TYPE_NAME_PARAM = "field_type_%d";
  private HyperparameterPredicatesHandler hyperparameterPredicatesHandler;
  private final String tableName;
  private final String alias;
  private final FutureExecutor executor;
  private final UACApisUtil uacApisUtil;
  private final boolean isPermissionV2;

  public PredicatesHandler(
      FutureExecutor executor,
      String tableName,
      String alias,
      UACApisUtil uacApisUtil,
      boolean isPermissionV2) {
    this.executor = executor;
    this.tableName = tableName;
    this.alias = alias;
    this.uacApisUtil = uacApisUtil;
    this.isPermissionV2 = isPermissionV2;

    if ("experiment_run".equals(tableName)) {
      this.hyperparameterPredicatesHandler = new HyperparameterPredicatesHandler();
    }
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
      default:
        return entityColumn;
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
      default:
        return entityColumn;
    }
    return entityColumn;
  }

  public InternalFuture<QueryFilterContext> processPredicates(
      List<KeyValueQuery> predicates, FutureExecutor executor) {
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

    final var bindingName = String.format("predicate_%d", index);

    // TODO: don't assume that the operator is equality for these
    switch (key) {
      case "id":
      case "name":
        return getContextFilterForSingleStringFields(
            predicate, key, value.getStringValue(), bindingName);
      case "date_created":
      case "date_updated":
        if (value.getKindCase().equals(Value.KindCase.STRING_VALUE)) {
          return getContextFilterForSingleStringFields(
              predicate, key, value.getStringValue(), bindingName);
        } else {
          return getContextFilterForSingleStringFields(
              predicate, key, value.getNumberValue(), bindingName);
        }
      case "owner":
        return setOwnerPredicate(index, predicate);
        // case visibility:
      case "":
        return InternalFuture.failedStage(new InvalidArgumentException("Key is empty"));
      case "end_time":
        return InternalFuture.completedInternalFuture(
            new QueryFilterContext()
                .addCondition(String.format("%s.end_time = :%s", alias, bindingName))
                .addBind(q -> q.bind(bindingName, value.getStringValue())));
      default:
        InternalFuture<QueryFilterContext> filterContext =
            processEntityNameBasedPredicates(index, bindingName, predicate);
        if (filterContext != null) {
          return filterContext;
        }
        break;
    }

    String[] names = key.split("\\.");
    // TODO: check length is 2

    switch (names[0]) {
      case ModelDBConstants.METRICS:
        return processKeyValuePredicate(index, predicate, names[1], "metrics");
      case ModelDBConstants.HYPERPARAMETERS:
        if (tableName.equals("experiment_run")) {
          return hyperparameterPredicatesHandler.processHyperparametersPredicate(
              index, predicate, names[1], "hyperparameters");
        }
        break;
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
      default:
        // Do nothing
        break;
    }

    // TODO: handle arbitrary key

    return InternalFuture.failedStage(new InvalidArgumentException("Predicate cannot be handled"));
  }

  private <T> InternalFuture<QueryFilterContext> getContextFilterForSingleStringFields(
      KeyValueQuery predicate, String key, T value, String bindingName) {
    var sql = String.format("select distinct id from %s where ", tableName);
    sql += applyOperator(predicate.getOperator(), key, ":" + bindingName);
    var queryContext =
        new QueryFilterContext()
            .addBind(q -> q.bind(bindingName, wrapValue(predicate.getOperator(), value)));
    if (predicate.getOperator().equals(OperatorEnum.Operator.NOT_CONTAIN)
        || predicate.getOperator().equals(OperatorEnum.Operator.NE)) {
      queryContext =
          queryContext.addCondition(String.format(ENTITY_ID_NOT_IN_QUERY_CONDITION, alias, sql));
    } else {
      queryContext =
          queryContext.addCondition(String.format(ENTITY_ID_IN_QUERY_CONDITION, alias, sql));
    }
    return InternalFuture.completedInternalFuture(queryContext);
  }

  private InternalFuture<QueryFilterContext> processEntityNameBasedPredicates(
      long index, String bindingName, KeyValueQuery predicate) {
    switch (tableName) {
      case "project":
        return processProjectPredicates(index, bindingName, predicate);
      case "experiment":
        return processExperimentPredicates(index, bindingName, predicate);
      case "experiment_run":
        return processExperimentRunPredicates(index, bindingName, predicate);
      default:
        // return null for further process
        return null;
    }
  }

  private InternalFuture<QueryFilterContext> processExperimentRunPredicates(
      long index, String bindingName, KeyValueQuery predicate) {

    if (!predicate.getKey().contains(ModelDBConstants.LINKED_ARTIFACT_ID)
        && predicate.getOperator().equals(OperatorEnum.Operator.IN)) {
      return InternalFuture.failedStage(
          new InvalidArgumentException(
              "Operator `IN` supported only with the linked_artifact_id as a key"));
    }

    var value = predicate.getValue();
    switch (predicate.getKey()) {
      case "project_id":
        return InternalFuture.completedInternalFuture(
            new QueryFilterContext()
                .addCondition(String.format("%s.project_id = :%s", alias, bindingName))
                .addBind(q -> q.bind(bindingName, value.getStringValue())));
      case "experiment_id":
        String operator = predicate.getOperator().equals(OperatorEnum.Operator.NE) ? "<>" : "=";
        return InternalFuture.completedInternalFuture(
            new QueryFilterContext()
                .addCondition("experiment_run.experiment_id " + operator + " :" + bindingName)
                .addBind(q -> q.bind(bindingName, value.getStringValue())));
      case "experiment.name":
        var expSql = "select distinct id from experiment where ";
        expSql += applyOperator(predicate.getOperator(), "name", ":" + bindingName);

        var expQueryContext =
            new QueryFilterContext()
                .addBind(
                    q ->
                        q.bind(
                            bindingName,
                            wrapValue(predicate.getOperator(), value.getStringValue())));
        if (predicate.getOperator().equals(OperatorEnum.Operator.NOT_CONTAIN)
            || predicate.getOperator().equals(OperatorEnum.Operator.NE)) {
          expQueryContext =
              expQueryContext.addCondition(
                  String.format("%s.experiment_id NOT IN (%s)", alias, expSql));
        } else {
          expQueryContext =
              expQueryContext.addCondition(
                  String.format("%s.experiment_id IN (%s)", alias, expSql));
        }

        return InternalFuture.completedInternalFuture(expQueryContext);
      default:
        // return null for further process
        return null;
    }
  }

  private InternalFuture<QueryFilterContext> processExperimentPredicates(
      long index, String bindingName, KeyValueQuery predicate) {
    var value = predicate.getValue();
    switch (predicate.getKey()) {
      default:
        // return null for further process
        return null;
    }
  }

  private InternalFuture<QueryFilterContext> processProjectPredicates(
      long index, String bindingName, KeyValueQuery predicate) {
    var value = predicate.getValue();
    switch (predicate.getKey()) {
      default:
        // return null for further process
        return null;
    }
  }

  private InternalFuture<QueryFilterContext> processTagsPredicate(
      long index, KeyValueQuery predicate) {
    try {
      final var value = predicate.getValue();
      var operator = predicate.getOperator();

      final var valueBindingName = String.format("v_t_%d", index);
      final var entityNameBindingName = String.format("entity_name_%d", index);

      var sql =
          String.format(
              "select distinct %s from tag_mapping where entity_name=:%s",
              getEntityColumn(), entityNameBindingName);
      sql += " and ";

      final var colValue = "tags";
      var queryContext =
          new QueryFilterContext().addBind(q -> q.bind(entityNameBindingName, getEntityName()));

      switch (value.getKindCase()) {
        case STRING_VALUE:
          if (value.getStringValue().isEmpty()) {
            throw new InvalidArgumentException(
                "Predicate does not contain string value in request");
          }
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
            queryContext.addCondition(String.format(ENTITY_ID_NOT_IN_QUERY_CONDITION, alias, sql));
      } else {
        queryContext =
            queryContext.addCondition(String.format(ENTITY_ID_IN_QUERY_CONDITION, alias, sql));
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

    final var valueBindingKey = String.format(K_P_D_VALUE_BINDING_KEY, index);
    final var valueBindingName = String.format(V_P_D_VALUE_BINDING_NAME, index);
    final var fieldTypeName = String.format(FIELD_TYPE_NAME_PARAM, index);

    String sql;
    if (fieldType.equals("observations")) {
      sql =
          String.format(
              "select distinct ob.%s from observation as ob "
                  + " inner join keyvalue as kv ON kv.id = ob.keyvaluemapping_id"
                  + " where ob.entity_name=:entityName and ob.field_type=:"
                  + fieldTypeName
                  + " and kv.field_type = 'attributes' ",
              getEntityColumn());
    } else {
      sql =
          String.format(
              "select distinct kv.%s from keyvalue as kv where kv.entity_name=:entityName and kv.field_type=:%s",
              getEntityColumn(), fieldTypeName);
    }

    sql += String.format(" and kv.kv_key=:%s ", valueBindingKey);
    sql += " and ";

    final var colValue = "kv.kv_value";
    var queryContext =
        new QueryFilterContext()
            .addBind(q -> q.bind(valueBindingKey, name))
            .addBind(q -> q.bind("entityName", getEntityName()))
            .addBind(q -> q.bind(fieldTypeName, fieldType));

    switch (value.getKindCase()) {
      case NUMBER_VALUE:
        sql += applyOperator(operator, columnAsNumber(colValue), ":" + valueBindingName);
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
            new UnimplementedException(ModelDBMessages.UNKNOWN_VALUE_TYPE_RECOGNIZED_ERROR));
    }

    if (operator.equals(OperatorEnum.Operator.NOT_CONTAIN)
        || operator.equals(OperatorEnum.Operator.NE)) {
      queryContext =
          queryContext.addCondition(String.format(ENTITY_ID_NOT_IN_QUERY_CONDITION, alias, sql));
    } else {
      queryContext =
          queryContext.addCondition(String.format(ENTITY_ID_IN_QUERY_CONDITION, alias, sql));
    }

    return InternalFuture.completedInternalFuture(queryContext);
  }

  private InternalFuture<QueryFilterContext> processAttributePredicate(
      long index, KeyValueQuery predicate, String name, String fieldType) {
    try {
      final var value = predicate.getValue();
      var operator = predicate.getOperator();

      final var valueBindingKey = String.format(K_P_D_VALUE_BINDING_KEY, index);
      final var valueBindingName = String.format(V_P_D_VALUE_BINDING_NAME, index);
      final var fieldTypeName = String.format(FIELD_TYPE_NAME_PARAM, index);

      var sql =
          String.format(
              "select distinct %s from attribute where entity_name= '%s' and field_type=:%s",
              getEntityColumn(), getEntityName(), fieldTypeName);
      sql += String.format(" and kv_key=:%s ", valueBindingKey);
      sql += " and ";

      final var colValue = "kv_value";
      var queryContext =
          new QueryFilterContext()
              .addBind(q -> q.bind(valueBindingKey, name))
              .addBind(q -> q.bind(fieldTypeName, fieldType));

      switch (value.getKindCase()) {
        case NUMBER_VALUE:
          sql += applyOperator(operator, columnAsNumber(colValue), ":" + valueBindingName);
          queryContext =
              queryContext.addBind(q -> q.bind(valueBindingName, value.getNumberValue()));
          break;
        case STRING_VALUE:
          if (value.getStringValue().isEmpty()) {
            throw new InvalidArgumentException(
                "Predicate does not contain string value in request");
          }
          sql += applyOperator(operator, colValue, ":" + valueBindingName);
          var valueStr = CommonUtils.getStringFromProtoObject(value);
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
              var valueStr1 = CommonUtils.getStringFromProtoObject(value1);
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
              new UnimplementedException(ModelDBMessages.UNKNOWN_VALUE_TYPE_RECOGNIZED_ERROR));
      }

      if (predicate.getOperator().equals(OperatorEnum.Operator.NOT_CONTAIN)
          || predicate.getOperator().equals(OperatorEnum.Operator.NE)) {
        queryContext =
            queryContext.addCondition(String.format(ENTITY_ID_NOT_IN_QUERY_CONDITION, alias, sql));
      } else {
        queryContext =
            queryContext.addCondition(String.format(ENTITY_ID_IN_QUERY_CONDITION, alias, sql));
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

      final var valueBindingKey = String.format(K_P_D_VALUE_BINDING_KEY, index);
      final var valueBindingName = String.format(V_P_D_VALUE_BINDING_NAME, index);
      final var fieldTypeName = String.format(FIELD_TYPE_NAME_PARAM, index);

      var sql =
          String.format(
              "select distinct %s from artifact where entity_name=:entityName and field_type=:%s",
              getEntityColumn(), fieldTypeName);

      var queryContext =
          new QueryFilterContext()
              .addBind(q -> q.bind("entityName", getEntityName()))
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
        case NUMBER_VALUE:
          sql += applyOperator(operator, columnAsNumber(colValue), ":" + valueBindingName);
          queryContext =
              queryContext.addBind(q -> q.bind(valueBindingName, value.getNumberValue()));
          break;
        case STRING_VALUE:
          sql += applyOperator(operator, colValue, ":" + valueBindingName);
          String valueStr;
          if (name.equals(ModelDBConstants.LINKED_ARTIFACT_ID)
              || operator.equals(OperatorEnum.Operator.CONTAIN)) {
            valueStr = value.getStringValue();
          } else {
            valueStr = CommonUtils.getStringFromProtoObject(value);
          }
          String finalValueStr = valueStr;
          queryContext =
              queryContext.addBind(
                  q -> q.bind(valueBindingName, wrapValue(operator, finalValueStr)));
          break;
        case LIST_VALUE:
          var valueList = new LinkedList<>();
          for (final var value1 : value.getListValue().getValuesList()) {
            if (value1.getKindCase().ordinal() == Value.KindCase.STRING_VALUE.ordinal()) {
              var valueStr1 = CommonUtils.getStringFromProtoObject(value1);
              valueStr1 = valueStr1.replaceAll("^\"|\"$", "");
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
              new UnimplementedException(ModelDBMessages.UNKNOWN_VALUE_TYPE_RECOGNIZED_ERROR));
      }

      if (predicate.getOperator().equals(OperatorEnum.Operator.NOT_CONTAIN)
          || predicate.getOperator().equals(OperatorEnum.Operator.NE)) {
        queryContext =
            queryContext.addCondition(String.format(ENTITY_ID_NOT_IN_QUERY_CONDITION, alias, sql));
      } else {
        queryContext =
            queryContext.addCondition(String.format(ENTITY_ID_IN_QUERY_CONDITION, alias, sql));
      }

      return InternalFuture.completedInternalFuture(queryContext);
    } catch (Exception ex) {
      return InternalFuture.failedStage(ex);
    }
  }

  private InternalFuture<QueryFilterContext> setOwnerPredicate(long index, KeyValueQuery predicate)
      throws ModelDBException {
    var operator = predicate.getOperator();
    InternalFuture<List<UserInfo>> userInfoListFuture;
    if (operator.equals(OperatorEnum.Operator.CONTAIN)
        || operator.equals(OperatorEnum.Operator.NOT_CONTAIN)) {
      userInfoListFuture =
          uacApisUtil
              .getFuzzyUserInfoList(predicate.getValue().getStringValue())
              .thenApply(UserInfoPaginationDTO::getUserInfoList, executor);
    } else {
      var ownerIdsArrString = predicate.getValue().getStringValue();
      List<String> ownerIds = new ArrayList<>();
      if (operator.equals(OperatorEnum.Operator.IN)) {
        ownerIds = Arrays.asList(ownerIdsArrString.split(","));
      } else {
        ownerIds.add(ownerIdsArrString);
      }
      userInfoListFuture =
          uacApisUtil
              .getUserInfoFromAuthServer(
                  new HashSet<>(ownerIds), Collections.emptySet(), Collections.emptyList(), false)
              .thenApply(userInfoMap -> new ArrayList<>(userInfoMap.values()), executor);
    }

    return userInfoListFuture.thenCompose(
        userInfoList -> {
          if (userInfoList != null && !userInfoList.isEmpty()) {
            var resourceItemsFutures = new ArrayList<InternalFuture<Set<String>>>();
            for (var userInfo : userInfoList) {
              resourceItemsFutures.add(
                  uacApisUtil
                      .getResourceItemsForLoginUserWorkspace(
                          isPermissionV2
                              ? Optional.empty()
                              : Optional.of(userInfo.getVertaInfo().getUsername()),
                          isPermissionV2 ? userInfo.getVertaInfo().getDefaultWorkspaceId() : null,
                          Optional.empty(),
                          ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT)
                      .thenApply(
                          accessibleAllWorkspaceItems ->
                              accessibleAllWorkspaceItems.stream()
                                  .map(GetResourcesResponseItem::getResourceId)
                                  .collect(Collectors.toSet()),
                          executor));
            }
            return InternalFuture.sequence(resourceItemsFutures, executor)
                .thenCompose(
                    resourceIdsList -> {
                      var resourceIds = new HashSet<String>();
                      for (var resourceIdSet : resourceIdsList) {
                        resourceIds.addAll(resourceIdSet);
                      }

                      final var valueBindingName = String.format("fuzzy_id_%d", index);
                      var sql = "<" + valueBindingName + ">";

                      var queryContext =
                          new QueryFilterContext()
                              .addBind(q -> q.bindList(valueBindingName, resourceIds));
                      if (predicate.getOperator().equals(OperatorEnum.Operator.NOT_CONTAIN)
                          || predicate.getOperator().equals(OperatorEnum.Operator.NE)) {
                        queryContext =
                            queryContext.addCondition(
                                String.format(ENTITY_ID_NOT_IN_QUERY_CONDITION, alias, sql));
                      } else {
                        queryContext =
                            queryContext.addCondition(
                                String.format(ENTITY_ID_IN_QUERY_CONDITION, alias, sql));
                      }
                      return InternalFuture.completedInternalFuture(queryContext);
                    },
                    executor);
          } else {
            return InternalFuture.completedInternalFuture(
                new QueryFilterContext().addCondition(String.format("%s.id = '-1'", alias)));
          }
        },
        executor);
  }
}
