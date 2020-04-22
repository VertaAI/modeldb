package ai.verta.modeldb.versioning;

import ai.verta.modeldb.KeyValueQuery;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.OperatorEnum;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.versioning.RepositoryVisibilityEnum.RepositoryVisibility;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class FindRepositoriesQuery {
  private FindRepositoriesQuery() {}

  private Query findRepositoriesHQLQuery;
  private Query findRepositoriesCountHQLQuery;

  public Query getFindRepositoriesHQLQuery() {
    return findRepositoriesHQLQuery;
  }

  public Query getFindRepositoriesCountHQLQuery() {
    return findRepositoriesCountHQLQuery;
  }

  private FindRepositoriesQuery(FindRepositoriesHQLQueryBuilder builder) {
    this.findRepositoriesHQLQuery = builder.buildQuery();
    this.findRepositoriesCountHQLQuery = builder.buildCountQuery();
  }

  // Builder Class
  public static class FindRepositoriesHQLQueryBuilder {
    private static final Logger LOGGER =
        LogManager.getLogger(FindRepositoriesHQLQueryBuilder.class);

    boolean addWhereClause = false;
    final Session session;
    final WorkspaceDTO workspaceDTO;
    String countQueryString;
    Map<String, Object> parametersMap = new HashMap<>();

    // optional parameters
    private List<Long> repoIds;
    private List<KeyValueQuery> predicates;
    private Integer pageNumber = 0;
    private Integer pageLimit = 0;

    public FindRepositoriesHQLQueryBuilder(Session session, WorkspaceDTO workspaceDTO) {
      this.session = session;
      this.workspaceDTO = workspaceDTO;
    }

    public FindRepositoriesHQLQueryBuilder setRepoIds(List<Long> repoIds) {
      if (repoIds != null && !repoIds.isEmpty()) {
        this.repoIds = repoIds;
        this.addWhereClause = true;
      }
      return this;
    }

    public FindRepositoriesHQLQueryBuilder setPredicates(List<KeyValueQuery> predicates) {
      if (predicates != null && !predicates.isEmpty()) {
        this.predicates = predicates;
        this.addWhereClause = true;
      }
      return this;
    }

    public FindRepositoriesHQLQueryBuilder setPageNumber(Integer pageNumber) {
      this.pageNumber = pageNumber;
      return this;
    }

    public FindRepositoriesHQLQueryBuilder setPageLimit(Integer pageLimit) {
      this.pageLimit = pageLimit;
      return this;
    }

    public FindRepositoriesQuery build() {
      return new FindRepositoriesQuery(this);
    }

    public Query buildQuery() {
      Query query = session.createQuery(getHQLQueryString());
      setParameterInQuery(query);

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

    private void setParameterInQuery(Query query) {
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
      setParameterInQuery(query);
      return query;
    }

    private String getHQLQueryString() {
      boolean appendAND = false;
      String alias = " repo";
      StringBuilder queryBuilder =
          new StringBuilder(" FROM ").append(RepositoryEntity.class.getSimpleName()).append(alias);

      StringBuilder joinClause = new StringBuilder();
      Map<String, String> joinAliasMap = new HashMap<>();
      if (this.predicates != null && !this.predicates.isEmpty()) {
        final int[] index = {0};
        this.predicates.forEach(
            keyValueQuery -> {
              if (keyValueQuery.getKey().contains(ModelDBConstants.LABEL)) {
                String joinAlias =
                    keyValueQuery.getKey()
                        + "_alias_"
                        + index[0]
                        + "_"
                        + Calendar.getInstance().getTimeInMillis();
                joinAliasMap.put(keyValueQuery.getKey() + index[0], joinAlias);
                joinClause
                    .append(" INNER JOIN ")
                    .append(LabelsMappingEntity.class.getSimpleName())
                    .append(" ")
                    .append(joinAlias)
                    .append(" ON ");
                joinClause
                    .append(joinAlias)
                    .append(".id.entity_hash = ")
                    .append(alias)
                    .append(".")
                    .append(ModelDBConstants.ID)
                    .append(" AND ");
                joinClause
                    .append(joinAlias)
                    .append(".id.entity_type = ")
                    .append(IDTypeEnum.IDType.VERSIONING_REPOSITORY.getNumber());
              }
              index[0]++;
            });
      }

      StringBuilder whereClause = new StringBuilder();
      if (workspaceDTO != null
          && workspaceDTO.getWorkspaceId() != null
          && !workspaceDTO.getWorkspaceId().isEmpty()) {
        whereClause.append(" WHERE ");
        whereClause
            .append(alias)
            .append(".")
            .append(ModelDBConstants.WORKSPACE_ID)
            .append(" = :")
            .append(ModelDBConstants.WORKSPACE_ID);
        parametersMap.put(ModelDBConstants.WORKSPACE_ID, workspaceDTO.getWorkspaceId());
        whereClause
            .append(" AND ")
            .append(alias)
            .append(".")
            .append(ModelDBConstants.WORKSPACE_TYPE)
            .append(" = :")
            .append(ModelDBConstants.WORKSPACE_TYPE);
        parametersMap.put(
            ModelDBConstants.WORKSPACE_TYPE, workspaceDTO.getWorkspaceType().getNumber());
        appendAND = true;
      } else if (this.addWhereClause) {
        whereClause.append(" WHERE ");
      }

      if (this.predicates != null && !this.predicates.isEmpty()) {
        if (appendAND) {
          whereClause.append(" AND ");
        }
        for (int index = 0; index < this.predicates.size(); index++) {
          KeyValueQuery keyValueQuery = this.predicates.get(index);
          if (keyValueQuery.getKey().contains(ModelDBConstants.LABEL)) {
            String joinAlias = joinAliasMap.get(keyValueQuery.getKey() + index);
            whereClause.append(joinAlias).append(".id.label ");
            setQueryParameters(whereClause, keyValueQuery, parametersMap);
          } else {
            whereClause.append(alias).append(".").append(keyValueQuery.getKey());
            setQueryParameters(whereClause, keyValueQuery, parametersMap);
          }
          if (index < this.predicates.size() - 1) {
            whereClause.append(" AND ");
          }
        }
        appendAND = true;
      }

      if (this.repoIds != null && !this.repoIds.isEmpty()) {
        if (appendAND) {
          whereClause.append(" AND ");
        }
        whereClause.append(alias).append(".").append(ModelDBConstants.ID).append(" IN (:repoIds) ");
        parametersMap.put("repoIds", this.repoIds);
        appendAND = true;
      }

      // Order by clause
      StringBuilder orderClause =
          new StringBuilder(" ORDER BY")
              .append(alias)
              .append(".")
              .append(ModelDBConstants.DATE_UPDATED)
              .append(" DESC");

      StringBuilder finalQueryBuilder = new StringBuilder();
      if (!joinClause.toString().isEmpty()) {
        finalQueryBuilder.append("SELECT ").append(alias).append(" ");
      }
      finalQueryBuilder.append(queryBuilder);
      finalQueryBuilder.append(joinClause);
      finalQueryBuilder.append(whereClause);
      finalQueryBuilder.append(orderClause);

      // Build count query
      StringBuilder countQueryBuilder = new StringBuilder();
      if (!joinClause.toString().isEmpty()) {
        countQueryBuilder.append("SELECT COUNT(").append(alias).append(") ");
      } else {
        countQueryBuilder.append("SELECT COUNT(*) ");
      }
      countQueryBuilder.append(queryBuilder);
      countQueryBuilder.append(joinClause);
      countQueryBuilder.append(whereClause);
      countQueryBuilder.append(orderClause);
      this.countQueryString = countQueryBuilder.toString();

      LOGGER.trace("Creating HQL query");
      return finalQueryBuilder.toString();
    }

    private void setQueryParameters(
        StringBuilder queryBuilder,
        KeyValueQuery keyValueQuery,
        Map<String, Object> parametersMap) {
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
              setValueWithOperatorInQuery(
                  queryBuilder,
                  operator,
                  RepositoryVisibility.valueOf(value.getStringValue()).ordinal(),
                  parametersMap);
            } else {
              setValueWithOperatorInQuery(
                  queryBuilder, operator, value.getStringValue(), parametersMap);
            }
            break;
          } else {
            Status invalidValueTypeError =
                Status.newBuilder()
                    .setCode(com.google.rpc.Code.INVALID_ARGUMENT_VALUE)
                    .setMessage("Predicate does not contain string value in request")
                    .build();
            throw StatusProto.toStatusRuntimeException(invalidValueTypeError);
          }
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
