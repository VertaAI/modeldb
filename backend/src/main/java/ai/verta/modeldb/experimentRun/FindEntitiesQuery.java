package ai.verta.modeldb.experimentRun;

import ai.verta.modeldb.DatasetVisibilityEnum;
import ai.verta.modeldb.KeyValueQuery;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.OperatorEnum;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.ArtifactEntity;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.FeatureEntity;
import ai.verta.modeldb.entities.KeyValueEntity;
import ai.verta.modeldb.entities.ObservationEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.entities.versioning.VersioningModeldbEntityMapping;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class FindEntitiesQuery {
  private FindEntitiesQuery() {}

  private Query findEntitiesHQLQuery;
  private Query findEntitiesCountHQLQuery;

  public Query getFindEntitiesHQLQuery() {
    return findEntitiesHQLQuery;
  }

  public Query getFindEntitiesCountHQLQuery() {
    return findEntitiesCountHQLQuery;
  }

  private FindEntitiesQuery(FindEntitiesHQLQueryBuilder builder)
      throws InvalidProtocolBufferException {
    this.findEntitiesHQLQuery = builder.buildQuery();
    this.findEntitiesCountHQLQuery = builder.buildCountQuery();
  }

  // Builder Class
  public static class FindEntitiesHQLQueryBuilder {
    private static final Logger LOGGER = LogManager.getLogger(FindEntitiesHQLQueryBuilder.class);

    private final Session session;
    private final WorkspaceDTO workspaceDTO;
    private final String entityClassName;
    private String countQueryString;
    private Map<String, Object> parametersMap = new HashMap<>();
    private Map<String, String> joinAliasMap = new HashMap<>();

    // optional parameters
    private List<String> projectIds = new ArrayList<>();
    private List<String> experimentIds = new ArrayList<>();
    private List<String> experimentRunIds = new ArrayList<>();
    private List<KeyValueQuery> predicates = new ArrayList<>();
    private List<String> sortKey = new ArrayList<>();
    private Boolean ascending = false;
    private Integer pageNumber = 0;
    private Integer pageLimit = 0;

    public FindEntitiesHQLQueryBuilder(
        Session session, WorkspaceDTO workspaceDTO, String entityClassName) {
      this.session = session;
      this.workspaceDTO = workspaceDTO;
      this.entityClassName = entityClassName;
    }

    public FindEntitiesHQLQueryBuilder addProjectId(String projectId) {
      if (projectId != null && !projectId.isEmpty()) {
        this.projectIds.add(projectId);
      }
      return this;
    }

    public FindEntitiesHQLQueryBuilder setProjectIds(List<String> projectIds) {
      if (projectIds != null && !projectIds.isEmpty()) {
        this.projectIds.addAll(projectIds);
      }
      return this;
    }

    public FindEntitiesHQLQueryBuilder addExperimentId(String experimentId) {
      if (experimentId != null && !experimentId.isEmpty()) {
        this.experimentIds.add(experimentId);
      }
      return this;
    }

    public FindEntitiesHQLQueryBuilder setExperimentIds(List<String> experimentIds) {
      if (experimentIds != null && !experimentIds.isEmpty()) {
        this.experimentIds.addAll(experimentIds);
      }
      return this;
    }

    public FindEntitiesHQLQueryBuilder setExperimentRunIds(List<String> experimentRunIds) {
      if (experimentRunIds != null && !experimentRunIds.isEmpty()) {
        this.experimentRunIds.addAll(experimentRunIds);
      }
      return this;
    }

    public FindEntitiesHQLQueryBuilder setPredicates(List<KeyValueQuery> predicates) {
      if (predicates != null && !predicates.isEmpty()) {
        this.predicates.addAll(predicates);
      }
      return this;
    }

    public FindEntitiesHQLQueryBuilder setSortKey(List<String> sortKey) {
      if (sortKey != null && !sortKey.isEmpty()) {
        this.sortKey.addAll(sortKey);
      }
      return this;
    }

    public FindEntitiesHQLQueryBuilder addSortKey(String sortKey) {
      if (sortKey != null && !sortKey.isEmpty()) {
        this.sortKey.add(sortKey);
      }
      return this;
    }

    public FindEntitiesHQLQueryBuilder setAscending(Boolean ascending) {
      this.ascending = ascending;
      return this;
    }

    public FindEntitiesHQLQueryBuilder setPageNumber(Integer pageNumber) {
      this.pageNumber = pageNumber;
      return this;
    }

    public FindEntitiesHQLQueryBuilder setPageLimit(Integer pageLimit) {
      this.pageLimit = pageLimit;
      return this;
    }

    public FindEntitiesQuery build() throws InvalidProtocolBufferException {
      return new FindEntitiesQuery(this);
    }

    public Query buildQuery() throws InvalidProtocolBufferException {
      Query query = session.createQuery(getHQLQueryString());
      setQueryParameters(query, parametersMap);

      if (this.pageNumber != null
          && this.pageLimit != null
          && this.pageNumber != 0
          && this.pageLimit != 0) {
        // Calculate number of documents to skip
        int skips = this.pageLimit * (this.pageNumber - 1);
        query.setFirstResult(skips);
        query.setMaxResults(this.pageLimit);
      }

      LOGGER.debug("Final find repository query : {}", query.getQueryString());
      return query;
    }

    private void setQueryParameters(Query query, Map<String, Object> parametersMap) {
      if (parametersMap.size() > 0) {
        parametersMap.forEach(
            (key, value) -> {
              if (value instanceof List) {
                List<Object> objectList = (List<Object>) value;
                query.setParameterList(key, objectList);
              } else {
                query.setParameter(key, value);
              }
            });
      }
    }

    public Query buildCountQuery() {
      Query query = session.createQuery(this.countQueryString);
      setQueryParameters(query, parametersMap);
      return query;
    }

    private String getHQLQueryString() throws InvalidProtocolBufferException {
      String alias = "root_alias";
      StringBuilder rootQueryBuilder =
          new StringBuilder(" FROM ").append(entityClassName).append(" ").append(alias);

      StringBuilder joinClause = getJoinClause(alias);
      StringBuilder whereClause = getWhereClause(alias);
      // Order by clause
      StringBuilder orderClause = getOrderClause(joinClause, alias, ascending);

      StringBuilder finalQueryBuilder = new StringBuilder();
      if (!joinClause.toString().isEmpty()) {
        finalQueryBuilder.append("SELECT ").append(alias).append(" ");
      }
      finalQueryBuilder.append(rootQueryBuilder);
      finalQueryBuilder.append(joinClause);
      if (!whereClause.toString().isEmpty()) {
        finalQueryBuilder.append(" WHERE ").append(whereClause);
      }
      finalQueryBuilder.append(orderClause);

      // Build count query
      StringBuilder countQueryBuilder = new StringBuilder();
      if (!joinClause.toString().isEmpty()) {
        countQueryBuilder.append("SELECT COUNT(").append(alias).append(") ");
      } else {
        countQueryBuilder.append("SELECT COUNT(*) ");
      }
      countQueryBuilder.append(rootQueryBuilder);
      countQueryBuilder.append(joinClause);
      if (!whereClause.toString().isEmpty()) {
        countQueryBuilder.append(" WHERE ").append(whereClause);
      }
      countQueryBuilder.append(orderClause);
      this.countQueryString = countQueryBuilder.toString();

      LOGGER.trace("Creating HQL query");
      return finalQueryBuilder.toString();
    }

    private void validatePredicate(KeyValueQuery predicate) {
      String errorMessage = null;
      if (predicate.getKey().isEmpty()) {
        errorMessage = "Key not found in predicate";
      } else if (!predicate.getKey().contains(ModelDBConstants.LINKED_ARTIFACT_ID)
          && predicate.getOperator().equals(OperatorEnum.Operator.IN)) {
        errorMessage = "Operator `IN` supported only with the linked_artifact_id as a key";
      }

      Value value = predicate.getValue();
      if (value.getKindCase().equals(Value.KindCase.STRING_VALUE)
          && value.getStringValue().isEmpty()) {
        errorMessage = "Predicate does not contain string value in request";
      }

      Status.Builder invalidValueTypeErrorBuilder =
          Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE);
      if (!value.getKindCase().equals(Value.KindCase.STRING_VALUE)
          && !value.getKindCase().equals(Value.KindCase.NUMBER_VALUE)
          && !value.getKindCase().equals(Value.KindCase.BOOL_VALUE)) {
        invalidValueTypeErrorBuilder.setCode(Code.UNIMPLEMENTED_VALUE);
        errorMessage =
            "Unknown 'Value' type recognized, valid 'Value' type are NUMBER_VALUE, STRING_VALUE, BOOL_VALUE";
      }

      if (errorMessage != null) {
        invalidValueTypeErrorBuilder.setMessage(errorMessage);
        throw StatusProto.toStatusRuntimeException(invalidValueTypeErrorBuilder.build());
      }
    }

    private StringBuilder getJoinClause(String rootAlias) {
      StringBuilder joinClause = new StringBuilder();
      if (this.predicates != null && !this.predicates.isEmpty()) {
        for (int index = 0; index < predicates.size(); index++) {
          KeyValueQuery predicate = predicates.get(index);

          validatePredicate(predicate);
          String[] names = predicate.getKey().split("\\.");
          String fieldName = names[0];

          // create parent name
          char[] entityClassNameCharArr = this.entityClassName.toCharArray();
          entityClassNameCharArr[0] = Character.toLowerCase(entityClassNameCharArr[0]);
          String parentFieldName = new String(entityClassNameCharArr);

          String joinAlias =
              fieldName + "_alias_" + index + "_" + Calendar.getInstance().getTimeInMillis();
          joinAliasMap.put(predicate.getKey() + index, joinAlias);

          switch (fieldName) {
            case ModelDBConstants.ARTIFACTS:
            case ModelDBConstants.DATASETS:
              LOGGER.debug("switch case : {}", fieldName);
              joinCauseArtifactEntity(rootAlias, parentFieldName, joinClause, joinAlias);
              break;
            case ModelDBConstants.ATTRIBUTES:
              LOGGER.debug("switch case : {}", fieldName);
              joinCauseAttributeEntity(rootAlias, parentFieldName, joinClause, joinAlias);
              break;
            case ModelDBConstants.HYPERPARAMETERS:
            case ModelDBConstants.METRICS:
              LOGGER.debug("switch case : {}", fieldName);
              joinCauseKeyValueEntity(rootAlias, parentFieldName, joinClause, joinAlias);
              break;
            case ModelDBConstants.OBSERVATIONS:
              LOGGER.debug("switch case : {}", fieldName);
              if (names.length > 2) {
                switch (names[1]) {
                  case ModelDBConstants.ATTRIBUTES:
                    LOGGER.debug("switch case : {} --> {}", fieldName, names[1]);
                    joinCauseKeyValueEntity(rootAlias, parentFieldName, joinClause, joinAlias);
                    break;
                  case ModelDBConstants.ARTIFACTS:
                    LOGGER.debug("switch case : {} --> {}", fieldName, names[1]);
                    joinCauseArtifactEntity(rootAlias, parentFieldName, joinClause, joinAlias);
                    break;
                }
              } else {
                joinCauseObservationEntity(rootAlias, parentFieldName, joinClause, joinAlias);
              }
              break;
            case ModelDBConstants.FEATURES:
              LOGGER.debug("switch case : {}", fieldName);
              joinCauseFeatureEntity(rootAlias, parentFieldName, joinClause, joinAlias);
              break;
            case ModelDBConstants.TAGS:
              LOGGER.debug("switch case : {}", fieldName);
              joinCauseTagsEntity(rootAlias, parentFieldName, joinClause, joinAlias);
              break;
            case ModelDBConstants.VERSIONED_INPUTS:
              LOGGER.debug("switch case : {}", fieldName);
              joinCauseVersioningModeldbEntityMappingEntity(
                  rootAlias, parentFieldName, joinClause, joinAlias);
              break;
            default:
              joinAliasMap.put(predicate.getKey() + index, rootAlias);
          }
        }
      }
      return joinClause;
    }

    private void appendParentIdConditionClause(
        String rootAlias, String parentFieldName, StringBuilder queryBuilder, String joinAlias) {
      queryBuilder
          .append(joinAlias)
          .append(".")
          .append(parentFieldName)
          .append(".")
          .append(ModelDBConstants.ID)
          .append(" = ")
          .append(rootAlias)
          .append(".")
          .append(ModelDBConstants.ID)
          .append(" ");
    }

    private void joinCauseArtifactEntity(
        String rootAlias, String parentFieldName, StringBuilder joinClause, String joinAlias) {
      joinClause
          .append(" LEFT JOIN ")
          .append(ArtifactEntity.class.getSimpleName())
          .append(" ")
          .append(joinAlias)
          .append(" ");
      joinClause.append(" ON ");
      appendParentIdConditionClause(rootAlias, parentFieldName, joinClause, joinAlias);
    }

    private void joinCauseAttributeEntity(
        String rootAlias, String parentFieldName, StringBuilder joinClause, String joinAlias) {
      joinClause
          .append(" LEFT JOIN ")
          .append(AttributeEntity.class.getSimpleName())
          .append(" ")
          .append(joinAlias)
          .append(" ");
      joinClause.append(" ON ");
      appendParentIdConditionClause(rootAlias, parentFieldName, joinClause, joinAlias);
    }

    private void joinCauseKeyValueEntity(
        String rootAlias, String parentFieldName, StringBuilder joinClause, String joinAlias) {
      joinClause
          .append(" LEFT JOIN ")
          .append(KeyValueEntity.class.getSimpleName())
          .append(" ")
          .append(joinAlias)
          .append(" ");
      joinClause.append(" ON ");
      appendParentIdConditionClause(rootAlias, parentFieldName, joinClause, joinAlias);
    }

    private void joinCauseObservationEntity(
        String rootAlias, String parentFieldName, StringBuilder joinClause, String joinAlias) {
      joinClause
          .append(" LEFT JOIN ")
          .append(ObservationEntity.class.getSimpleName())
          .append(" ")
          .append(joinAlias)
          .append(" ");
      joinClause.append(" ON ");
      appendParentIdConditionClause(rootAlias, parentFieldName, joinClause, joinAlias);
    }

    private void joinCauseFeatureEntity(
        String rootAlias, String parentFieldName, StringBuilder joinClause, String joinAlias) {
      joinClause
          .append(" LEFT JOIN ")
          .append(FeatureEntity.class.getSimpleName())
          .append(" ")
          .append(joinAlias)
          .append(" ");
      joinClause.append(" ON ");
      appendParentIdConditionClause(rootAlias, parentFieldName, joinClause, joinAlias);
    }

    private void joinCauseTagsEntity(
        String rootAlias, String parentFieldName, StringBuilder joinClause, String joinAlias) {
      joinClause
          .append(" LEFT JOIN ")
          .append(TagsMapping.class.getSimpleName())
          .append(" ")
          .append(joinAlias)
          .append(" ");
      joinClause.append(" ON ");
      appendParentIdConditionClause(rootAlias, parentFieldName, joinClause, joinAlias);
    }

    private void joinCauseVersioningModeldbEntityMappingEntity(
        String rootAlias, String parentFieldName, StringBuilder joinClause, String joinAlias) {
      joinClause
          .append(" LEFT JOIN ")
          .append(VersioningModeldbEntityMapping.class.getSimpleName())
          .append(" ")
          .append(joinAlias)
          .append(" ");
      joinClause.append(" ON ");
      appendParentIdConditionClause(rootAlias, parentFieldName, joinClause, joinAlias);
    }

    private StringBuilder getWhereClause(String rootAlias) throws InvalidProtocolBufferException {
      List<String> whereClauseList = new ArrayList<>();
      if (workspaceDTO != null
          && workspaceDTO.getWorkspaceId() != null
          && !workspaceDTO.getWorkspaceId().isEmpty()) {
        // TODO: Implement in the future
        /*whereClauseList.add(
                rootAlias + "." + ModelDBConstants.WORKSPACE_ID + " = :" + ModelDBConstants.WORKSPACE_ID);
        parametersMap.put(ModelDBConstants.WORKSPACE_ID, workspaceDTO.getWorkspaceId());
        whereClauseList.add(
                rootAlias
                        + "."
                        + ModelDBConstants.WORKSPACE_TYPE
                        + " = :"
                        + ModelDBConstants.WORKSPACE_TYPE);
        parametersMap.put(
                ModelDBConstants.WORKSPACE_TYPE, workspaceDTO.getWorkspaceType().getNumber());*/
      }

      if (!projectIds.isEmpty()) {
        whereClauseList.add(rootAlias + "." + ModelDBConstants.PROJECT_ID + " IN (:projectIds) ");
        parametersMap.put("projectIds", this.projectIds);
      }

      if (!experimentIds.isEmpty()) {
        whereClauseList.add(
            rootAlias + "." + ModelDBConstants.EXPERIMENT_ID + " IN (:experimentId) ");
        parametersMap.put("experimentId", this.experimentIds);
      }

      if (!experimentRunIds.isEmpty()) {
        whereClauseList.add(rootAlias + "." + ModelDBConstants.ID + " IN (:experimentRunIds) ");
        parametersMap.put("experimentRunIds", this.experimentRunIds);
      }

      if (!this.predicates.isEmpty()) {
        whereClauseList.add(getPredicateWhereClause().toString());
      }

      StringBuilder whereClause = new StringBuilder();
      setPredicatesWithQueryOperator(whereClause, "AND", whereClauseList.toArray(new String[0]));
      return whereClause;
    }

    private StringBuilder getPredicateWhereClause() throws InvalidProtocolBufferException {
      StringBuilder predicateWhereClause = new StringBuilder();
      if (this.predicates != null && !this.predicates.isEmpty()) {
        List<String> whereClauseList = new ArrayList<>();
        for (int index = 0; index < predicates.size(); index++) {
          KeyValueQuery predicate = predicates.get(index);

          validatePredicate(predicate);
          String[] names = predicate.getKey().split("\\.");
          String fieldName = names[0];

          String joinAlias = joinAliasMap.get(predicate.getKey() + index);

          switch (fieldName) {
            case ModelDBConstants.ARTIFACTS:
            case ModelDBConstants.DATASETS:
              LOGGER.debug("switch case : {}", fieldName);
              whereClauseList.add(
                  getArtifactEntityWhereClause(names, joinAlias, predicate, fieldName));
              break;
            case ModelDBConstants.ATTRIBUTES:
            case ModelDBConstants.HYPERPARAMETERS:
            case ModelDBConstants.METRICS:
              LOGGER.debug("switch case : {}", fieldName);
              whereClauseList.add(
                  getKeyValueEntityWhereClause(names, joinAlias, predicate, fieldName));
              break;
            case ModelDBConstants.OBSERVATIONS:
              LOGGER.debug("switch case : {}", fieldName);
              if (names.length > 2) {
                switch (names[1]) {
                  case ModelDBConstants.ATTRIBUTES:
                    LOGGER.debug("switch case : {} --> {}", fieldName, names[1]);
                    whereClauseList.add(
                        getKeyValueEntityWhereClause(names, joinAlias, predicate, fieldName));
                    break;
                  case ModelDBConstants.ARTIFACTS:
                    LOGGER.debug("switch case : {} --> {}", fieldName, names[1]);
                    whereClauseList.add(
                        getArtifactEntityWhereClause(names, joinAlias, predicate, fieldName));
                    break;
                }
              } else {
                whereClauseList.add(defaultKeyValueWhereClause(names[1], joinAlias, predicate));
              }
              break;
            case ModelDBConstants.FEATURES:
              LOGGER.debug("switch case : {}", fieldName);
              whereClauseList.add(
                  defaultKeyValueWhereClause(names[names.length - 1], joinAlias, predicate));
              break;
            case ModelDBConstants.TAGS:
              LOGGER.debug("switch case : {}", fieldName);
              whereClauseList.add(
                  defaultKeyValueWhereClause(ModelDBConstants.TAGS, joinAlias, predicate));
              break;
            case ModelDBConstants.VERSIONED_INPUTS:
              LOGGER.debug("switch case : {}", fieldName);
              whereClauseList.add(defaultKeyValueWhereClause(names[1], joinAlias, predicate));
              break;
            default:
              whereClauseList.add(
                  defaultKeyValueWhereClause(predicate.getKey(), joinAlias, predicate));
              break;
          }
        }
        setPredicatesWithQueryOperator(
            predicateWhereClause, "AND", whereClauseList.toArray(new String[0]));
      }
      return predicateWhereClause;
    }

    private String getArtifactEntityWhereClause(
        String[] names, String joinAlias, KeyValueQuery predicate, String fieldName)
        throws InvalidProtocolBufferException {
      String childKey = names[names.length - 1];
      StringBuilder artifactStringBuilder = new StringBuilder();
      artifactStringBuilder
          .append(joinAlias)
          .append(".")
          .append(ModelDBConstants.FEILD_TYPE)
          .append(" = ")
          .append(fieldName);
      if (childKey.equals(ModelDBConstants.LINKED_ARTIFACT_ID)) {
        artifactStringBuilder.append(" AND ").append(joinAlias).append(".").append(childKey);
        setQueryParameters(artifactStringBuilder, predicate, parametersMap);
      } else {
        artifactStringBuilder
            .append(" AND ")
            .append(joinAlias)
            .append(".")
            .append(ModelDBConstants.KEY);
        setValueWithOperatorInQuery(
            artifactStringBuilder, OperatorEnum.Operator.EQ, childKey, parametersMap);
        artifactStringBuilder
            .append(" AND ")
            .append(joinAlias)
            .append(".")
            .append(ModelDBConstants.PATH);
        setQueryParameters(artifactStringBuilder, predicate, parametersMap);
      }
      return artifactStringBuilder.toString();
    }

    private String getKeyValueEntityWhereClause(
        String[] names, String joinAlias, KeyValueQuery predicate, String fieldName)
        throws InvalidProtocolBufferException {
      String childKey = names[names.length - 1];
      StringBuilder keyValueStringBuilder = new StringBuilder();
      keyValueStringBuilder.append(joinAlias).append(".").append(ModelDBConstants.FEILD_TYPE);
      setValueWithOperatorInQuery(
          keyValueStringBuilder, OperatorEnum.Operator.EQ, fieldName, parametersMap);
      keyValueStringBuilder
          .append(" AND ")
          .append(joinAlias)
          .append(".")
          .append(ModelDBConstants.KEY);
      setValueWithOperatorInQuery(
          keyValueStringBuilder, OperatorEnum.Operator.EQ, childKey, parametersMap);
      keyValueStringBuilder
          .append(" AND ")
          .append(joinAlias)
          .append(".")
          .append(ModelDBConstants.VALUE)
          .append(" ");
      setValueWithOperatorInQuery(
          keyValueStringBuilder,
          predicate.getOperator(),
          ModelDBUtils.getStringFromProtoObject(predicate.getValue()),
          parametersMap);
      return keyValueStringBuilder.toString();
    }

    private String defaultKeyValueWhereClause(String key, String joinAlias, KeyValueQuery predicate)
        throws InvalidProtocolBufferException {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(joinAlias).append(".").append(key);
      setQueryParameters(stringBuilder, predicate, parametersMap);
      return stringBuilder.toString();
    }

    private StringBuilder getOrderClause(
        StringBuilder joinClause, String rootAlias, boolean ascending) {
      List<String> orderByFields = new ArrayList<>();
      if (this.sortKey != null && !this.sortKey.isEmpty()) {
        for (int index = 0; index < this.sortKey.size(); index++) {
          String sortBy = this.sortKey.get(index);
          String[] keys = sortBy.split("\\.");
          String fieldName = keys[0];

          // create parent name
          char[] entityClassNameCharArr = this.entityClassName.toCharArray();
          entityClassNameCharArr[0] = Character.toLowerCase(entityClassNameCharArr[0]);
          String parentFieldName = new String(entityClassNameCharArr);

          String joinAlias =
              fieldName
                  + "_orderby_alias_"
                  + index
                  + "_"
                  + Calendar.getInstance().getTimeInMillis();
          joinAliasMap.put(fieldName + index, joinAlias);

          switch (fieldName) {
            case ModelDBConstants.ARTIFACTS:
            case ModelDBConstants.DATASETS:
              LOGGER.debug("switch case : {}", fieldName);
              joinCauseArtifactEntity(rootAlias, parentFieldName, joinClause, joinAlias);
              setOrderByField(joinClause, keys, fieldName, joinAlias);
              orderByFields.add(joinAlias + "." + ModelDBConstants.PATH);
              break;
            case ModelDBConstants.ATTRIBUTES:
              LOGGER.debug("switch case : {}", fieldName);
              joinCauseAttributeEntity(rootAlias, parentFieldName, joinClause, joinAlias);
              setOrderByField(joinClause, keys, fieldName, joinAlias);
              orderByFields.add(joinAlias + "." + ModelDBConstants.VALUE);
              break;
            case ModelDBConstants.HYPERPARAMETERS:
            case ModelDBConstants.METRICS:
              LOGGER.debug("switch case : {}", fieldName);
              joinCauseKeyValueEntity(rootAlias, parentFieldName, joinClause, joinAlias);
              setOrderByField(joinClause, keys, fieldName, joinAlias);
              orderByFields.add(joinAlias + "." + ModelDBConstants.VALUE);
              break;
            case ModelDBConstants.OBSERVATIONS:
              LOGGER.debug("switch case : {}", fieldName);
              if (keys.length > 2) {
                // If getting third level key like observation.attribute.attr_1 then it is not
                // supported
                Status status =
                    Status.newBuilder()
                        .setCode(Code.UNIMPLEMENTED_VALUE)
                        .setMessage("Third level of sorting not supported")
                        .build();
                throw StatusProto.toStatusRuntimeException(status);
              } else {
                joinCauseObservationEntity(rootAlias, parentFieldName, joinClause, joinAlias);
                setOrderByField(joinClause, keys, fieldName, joinAlias);
                orderByFields.add(joinAlias + "." + keys[1]);
              }
              break;
            case ModelDBConstants.FEATURES:
              LOGGER.debug("switch case : {}", fieldName);
              joinCauseFeatureEntity(rootAlias, parentFieldName, joinClause, joinAlias);
              orderByFields.add(joinAlias + "." + ModelDBConstants.NAME);
              break;
            case ModelDBConstants.TAGS:
              LOGGER.debug("switch case : {}", fieldName);
              joinCauseTagsEntity(rootAlias, parentFieldName, joinClause, joinAlias);
              orderByFields.add(joinAlias + "." + ModelDBConstants.TAGS);
              break;
            case ModelDBConstants.VERSIONED_INPUTS:
              LOGGER.debug("switch case : {}", fieldName);
              joinCauseVersioningModeldbEntityMappingEntity(
                  rootAlias, parentFieldName, joinClause, joinAlias);
              orderByFields.add(joinAlias + "." + keys[1]);
              break;
            default:
              orderByFields.add(joinAlias + "." + sortBy);
          }
        }
      } else {
        orderByFields.add(rootAlias + "." + ModelDBConstants.DATE_UPDATED);
      }

      StringBuilder orderStringBuilder = new StringBuilder(" ORDER BY ");
      orderStringBuilder.append(
          String.join(" " + ascending + ",", orderByFields.toArray(new String[0])));
      orderStringBuilder.append(ascending ? " ASC " : " DESC ");
      return orderStringBuilder;
    }

    private void setOrderByField(
        StringBuilder orderClause, String[] keys, String fieldName, String joinAlias) {
      String childKey = keys[keys.length - 1];

      StringBuilder orderWhereClauseBuilder = new StringBuilder(" AND ");
      orderWhereClauseBuilder.append(joinAlias).append(".").append(ModelDBConstants.FEILD_TYPE);
      setValueWithOperatorInQuery(
          orderWhereClauseBuilder, OperatorEnum.Operator.EQ, fieldName, parametersMap);
      orderWhereClauseBuilder
          .append(" AND ")
          .append(joinAlias)
          .append(".")
          .append(ModelDBConstants.KEY);
      setValueWithOperatorInQuery(
          orderWhereClauseBuilder, OperatorEnum.Operator.EQ, childKey, parametersMap);
      orderClause.append(orderWhereClauseBuilder + " ");
    }

    private void setPredicatesWithQueryOperator(
        StringBuilder queryStringBuilder, String operatorName, String[] predicateClause) {
      queryStringBuilder.append(String.join(" " + operatorName + " ", predicateClause));
    }

    private void setQueryParameters(
        StringBuilder queryBuilder, KeyValueQuery keyValueQuery, Map<String, Object> parametersMap)
        throws InvalidProtocolBufferException {
      Value value = keyValueQuery.getValue();
      OperatorEnum.Operator operator = keyValueQuery.getOperator();
      switch (value.getKindCase()) {
        case NUMBER_VALUE:
          LOGGER.debug("Called switch case : number_value");
          setValueWithOperatorInQuery(
              queryBuilder, operator, value.getNumberValue(), parametersMap);
          break;
        case STRING_VALUE:
          LOGGER.debug("Called switch case : string_value");
          if (!value.getStringValue().isEmpty()) {
            LOGGER.debug("Called switch case : string value exist");
            if (keyValueQuery.getKey().equals(ModelDBConstants.PROJECT_VISIBILITY)
                || keyValueQuery.getKey().equals(ModelDBConstants.DATASET_VISIBILITY)
                || keyValueQuery.getKey().equals(ModelDBConstants.DATASET_VERSION_VISIBILITY)) {
              int visibilityOrdinal = 0;
              switch (keyValueQuery.getKey()) {
                case ModelDBConstants.PROJECT_VISIBILITY:
                  visibilityOrdinal = ProjectVisibility.valueOf(value.getStringValue()).ordinal();
                  break;
                case ModelDBConstants.DATASET_VISIBILITY:
                case ModelDBConstants.DATASET_VERSION_VISIBILITY:
                  visibilityOrdinal =
                      DatasetVisibilityEnum.DatasetVisibility.valueOf(value.getStringValue())
                          .ordinal();
                  break;
              }
              setValueWithOperatorInQuery(queryBuilder, operator, visibilityOrdinal, parametersMap);
            } else {
              setValueWithOperatorInQuery(
                  queryBuilder, operator, value.getStringValue(), parametersMap);
            }
          }
          break;
        case BOOL_VALUE:
          LOGGER.debug("Called switch case : bool_value");
          setValueWithOperatorInQuery(
              queryBuilder, operator, value.getStringValue(), parametersMap);
          break;
        default:
          Status invalidValueTypeError =
              Status.newBuilder()
                  .setCode(Code.UNIMPLEMENTED_VALUE)
                  .setMessage(
                      "Unknown 'Value' type recognized, valid 'Value' type are NUMBER_VALUE, STRING_VALUE, BOOL_VALUE")
                  .build();
          throw StatusProto.toStatusRuntimeException(invalidValueTypeError);
      }
    }

    private void setValueWithOperatorInQuery(
        StringBuilder queryBuilder,
        OperatorEnum.Operator operator,
        Object value,
        Map<String, Object> parametersMap) {
      long timestamp = Math.round(100.0 * Math.random()) + Calendar.getInstance().getTimeInMillis();
      String key;
      switch (operator.ordinal()) {
        case OperatorEnum.Operator.GT_VALUE:
          key = "GT_VALUE_" + timestamp;
          queryBuilder.append(" > :").append(key);
          parametersMap.put(key, value);
          break;
        case OperatorEnum.Operator.GTE_VALUE:
          key = "GTE_VALUE_" + timestamp;
          queryBuilder.append(" >= :").append(key);
          parametersMap.put(key, value);
          break;
        case OperatorEnum.Operator.LT_VALUE:
          key = "LT_VALUE_" + timestamp;
          queryBuilder.append(" < :").append(key);
          parametersMap.put(key, value);
          break;
        case OperatorEnum.Operator.LTE_VALUE:
          key = "LTE_VALUE_" + timestamp;
          queryBuilder.append(" <= :").append(key);
          parametersMap.put(key, value);
          break;
        case OperatorEnum.Operator.NE_VALUE:
          key = "NE_VALUE_" + timestamp;
          queryBuilder.append(" <> :").append(key);
          parametersMap.put(key, value);
          break;
        case OperatorEnum.Operator.CONTAIN_VALUE:
          queryBuilder
              .append(" LIKE ")
              .append(("'%" + Pattern.compile((String) value) + "%'").toLowerCase());
          break;
        case OperatorEnum.Operator.NOT_CONTAIN_VALUE:
          queryBuilder
              .append(" NOT LIKE ")
              .append(("'%" + Pattern.compile((String) value) + "%'").toLowerCase());
          break;
        case OperatorEnum.Operator.IN_VALUE:
          key = "IN_VALUE_" + timestamp;
          queryBuilder.append(" IN (:").append(key).append(")");
          parametersMap.put(key, value);
          break;
        default:
          key = "default_" + timestamp;
          queryBuilder.append(" = :").append(key);
          parametersMap.put(key, value);
      }
      queryBuilder.append(" ");
    }
  }
}
