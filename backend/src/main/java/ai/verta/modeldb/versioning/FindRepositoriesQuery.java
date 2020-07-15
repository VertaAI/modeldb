package ai.verta.modeldb.versioning;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.dto.UserInfoPaginationDTO;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.versioning.RepositoryVisibilityEnum.RepositoryVisibility;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

  private FindRepositoriesQuery(FindRepositoriesHQLQueryBuilder builder) throws ModelDBException {
    this.findRepositoriesHQLQuery = builder.buildQuery();
    this.findRepositoriesCountHQLQuery = builder.buildCountQuery();
  }

  // Builder Class
  public static class FindRepositoriesHQLQueryBuilder {
    private static final Logger LOGGER =
        LogManager.getLogger(FindRepositoriesHQLQueryBuilder.class);

    final Session session;
    final AuthService authService;
    final WorkspaceDTO workspaceDTO;
    String countQueryString;
    Map<String, Object> parametersMap = new HashMap<>();

    // optional parameters
    private List<Long> repoIds;
    private List<KeyValueQuery> predicates;
    private Integer pageNumber = 0;
    private Integer pageLimit = 0;

    public FindRepositoriesHQLQueryBuilder(
        Session session, AuthService authService, WorkspaceDTO workspaceDTO) {
      this.session = session;
      this.authService = authService;
      this.workspaceDTO = workspaceDTO;
    }

    public FindRepositoriesHQLQueryBuilder setRepoIds(List<Long> repoIds) {
      if (repoIds != null && !repoIds.isEmpty()) {
        this.repoIds = repoIds;
      }
      return this;
    }

    public FindRepositoriesHQLQueryBuilder setPredicates(List<KeyValueQuery> predicates) {
      if (predicates != null && !predicates.isEmpty()) {
        this.predicates = predicates;
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

    public FindRepositoriesQuery build() throws ModelDBException {
      return new FindRepositoriesQuery(this);
    }

    public Query buildQuery() throws ModelDBException {
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

    private String getHQLQueryString() throws ModelDBException {
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
                String[] joinClauses = new String[2];
                joinClauses[0] =
                    joinAlias
                        + ".id.entity_hash = CAST("
                        + alias
                        + "."
                        + ModelDBConstants.ID
                        + " as string)";
                joinClauses[1] =
                    joinAlias
                        + ".id.entity_type = "
                        + IDTypeEnum.IDType.VERSIONING_REPOSITORY.getNumber();
                setPredicatesWithQueryOperator(joinClause, "AND", joinClauses);
              }
              index[0]++;
            });
      }

      List<String> whereClauseList = new ArrayList<>();
      if (workspaceDTO != null
          && workspaceDTO.getWorkspaceId() != null
          && !workspaceDTO.getWorkspaceId().isEmpty()) {
        whereClauseList.add(
            alias + "." + ModelDBConstants.WORKSPACE_ID + " = :" + ModelDBConstants.WORKSPACE_ID);
        parametersMap.put(ModelDBConstants.WORKSPACE_ID, workspaceDTO.getWorkspaceId());
        whereClauseList.add(
            alias
                + "."
                + ModelDBConstants.WORKSPACE_TYPE
                + " = :"
                + ModelDBConstants.WORKSPACE_TYPE);
        parametersMap.put(
            ModelDBConstants.WORKSPACE_TYPE, workspaceDTO.getWorkspaceType().getNumber());
      }

      if (this.predicates != null && !this.predicates.isEmpty()) {
        for (int index = 0; index < this.predicates.size(); index++) {
          KeyValueQuery keyValueQuery = this.predicates.get(index);
          if (keyValueQuery.getKey().contains(ModelDBConstants.LABEL)) {
            String joinAlias = joinAliasMap.get(keyValueQuery.getKey() + index);
            StringBuilder joinStringBuilder = new StringBuilder(joinAlias).append(".id.label ");
            setQueryParameters(joinStringBuilder, keyValueQuery, parametersMap);
            whereClauseList.add(joinStringBuilder.toString());
          } else if (keyValueQuery.getKey().contains(ModelDBConstants.OWNER)) {
            StringBuilder predicateStringBuilder =
                new StringBuilder(alias).append(".").append(keyValueQuery.getKey());
            setOwnerPredicate(index, keyValueQuery, predicateStringBuilder);
            whereClauseList.add(predicateStringBuilder.toString());
          } else {
            StringBuilder predicateStringBuilder = new StringBuilder();
            predicateStringBuilder.append(alias).append(".").append(keyValueQuery.getKey());
            setQueryParameters(predicateStringBuilder, keyValueQuery, parametersMap);
            whereClauseList.add(predicateStringBuilder.toString());
          }
        }
      }

      if (this.repoIds != null && !this.repoIds.isEmpty()) {
        whereClauseList.add(alias + "." + ModelDBConstants.ID + " IN (:repoIds) ");
        parametersMap.put("repoIds", this.repoIds);
      }
      whereClauseList.add(alias + "." + ModelDBConstants.DELETED + " = false ");

      StringBuilder whereClause = new StringBuilder();
      setPredicatesWithQueryOperator(whereClause, "AND", whereClauseList.toArray(new String[0]));

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
      countQueryBuilder.append(queryBuilder);
      countQueryBuilder.append(joinClause);
      if (!whereClause.toString().isEmpty()) {
        countQueryBuilder.append(" WHERE ").append(whereClause);
      }
      this.countQueryString = countQueryBuilder.toString();

      LOGGER.trace("Creating HQL query");
      return finalQueryBuilder.toString();
    }

    private void setOwnerPredicate(
        int index, KeyValueQuery keyValueQuery, StringBuilder predicateStringBuilder)
        throws ModelDBException {
      OperatorEnum.Operator operator = keyValueQuery.getOperator();
      if (operator.equals(OperatorEnum.Operator.IN)) {
        String ownerIdsArrString = keyValueQuery.getValue().getStringValue();
        List<String> ownerIds = Arrays.asList(ownerIdsArrString.split(","));
        setValueWithOperatorInQuery(predicateStringBuilder, operator, ownerIds, parametersMap);
      } else if (operator.equals(OperatorEnum.Operator.CONTAIN)
          || operator.equals(OperatorEnum.Operator.NOT_CONTAIN)) {
        UserInfoPaginationDTO userInfoPaginationDTO =
            authService.getFuzzyUserInfoList(keyValueQuery.getValue().getStringValue());
        List<UserInfo> userInfoList = userInfoPaginationDTO.getUserInfoList();
        if (userInfoList != null && !userInfoList.isEmpty()) {
          List<String> ownerIds =
              userInfoList.stream()
                  .map(authService::getVertaIdFromUserInfo)
                  .collect(Collectors.toList());
          if (operator.equals(OperatorEnum.Operator.CONTAIN)) {
            setValueWithOperatorInQuery(
                predicateStringBuilder, OperatorEnum.Operator.IN, ownerIds, parametersMap);
          } else {
            String mapKey = "IN_VALUE_" + index;
            predicateStringBuilder.append(" NOT IN (:").append(mapKey).append(")");
            parametersMap.put(mapKey, ownerIds);
          }
        } else {
          throw new ModelDBException(
              ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND,
              io.grpc.Status.Code.FAILED_PRECONDITION);
        }
      } else {
        setQueryParameters(predicateStringBuilder, keyValueQuery, parametersMap);
      }
    }

    private void setPredicatesWithQueryOperator(
        StringBuilder queryStringBuilder, String operatorName, String[] predicateClause) {
      queryStringBuilder.append(String.join(" " + operatorName + " ", predicateClause));
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
            if (keyValueQuery.getKey().equals(ModelDBConstants.REPOSITORY_VISIBILITY)) {
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
