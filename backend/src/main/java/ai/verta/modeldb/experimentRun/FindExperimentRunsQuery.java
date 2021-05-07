package ai.verta.modeldb.experimentRun;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.dto.UserInfoPaginationDTO;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.versioning.VersioningUtils;
import ai.verta.uac.UserInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class FindExperimentRunsQuery {
  private FindExperimentRunsQuery() {}

  private Query findExperimentRunsSQLQuery;
  private Query findExperimentRunsCountSQLQuery;

  public Query getFindExperimentRunsSQLQuery() {
    return findExperimentRunsSQLQuery;
  }

  public Query getFindExperimentRunsCountSQLQuery() {
    return findExperimentRunsCountSQLQuery;
  }

  private FindExperimentRunsQuery(FindExperimentRunsSQLQueryBuilder builder)
      throws ModelDBException {
    this.findExperimentRunsSQLQuery = builder.buildQuery();
    this.findExperimentRunsCountSQLQuery = builder.buildCountQuery();
  }

  // Builder Class
  public static class FindExperimentRunsSQLQueryBuilder {
    private static final Logger LOGGER =
        LogManager.getLogger(FindExperimentRunsSQLQueryBuilder.class);

    final Session session;
    final AuthService authService;
    final RoleService roleService;
    String countQueryString;
    Map<String, Object> parametersMap = new HashMap<>();

    // optional parameters
    private List<String> experimentRunIds;
    private List<String> experimentIds;
    private List<String> projectIds;
    private List<KeyValueQuery> predicates;
    private Integer pageNumber = 0;
    private Integer pageLimit = 0;
    private String sortKey = ModelDBConstants.DATE_UPDATED;
    private boolean ascending = false;
    private boolean idsOnly = false;
    private final List<String> selectedResponseFields;

    public FindExperimentRunsSQLQueryBuilder(
        Session session,
        AuthService authService,
        RoleService roleService,
        List<String> selectedResponseFields) {
      this.session = session;
      this.authService = authService;
      this.roleService = roleService;
      this.selectedResponseFields = selectedResponseFields;
    }

    public FindExperimentRunsSQLQueryBuilder setExperimentRunIds(List<String> experimentRunIds) {
      if (experimentRunIds != null && !experimentRunIds.isEmpty()) {
        this.experimentRunIds = experimentRunIds;
      }
      return this;
    }

    public FindExperimentRunsSQLQueryBuilder setExperimentIds(List<String> experimentIds) {
      if (experimentIds != null && !experimentIds.isEmpty()) {
        this.experimentIds = experimentIds;
      }
      return this;
    }

    public FindExperimentRunsSQLQueryBuilder setProjectIds(List<String> projectIds) {
      if (projectIds != null && !projectIds.isEmpty()) {
        this.projectIds = projectIds;
      }
      return this;
    }

    public FindExperimentRunsSQLQueryBuilder setPredicates(List<KeyValueQuery> predicates) {
      if (predicates != null && !predicates.isEmpty()) {
        this.predicates = predicates;
      }
      return this;
    }

    public FindExperimentRunsSQLQueryBuilder setPageNumber(Integer pageNumber) {
      this.pageNumber = pageNumber;
      return this;
    }

    public FindExperimentRunsSQLQueryBuilder setPageLimit(Integer pageLimit) {
      this.pageLimit = pageLimit;
      return this;
    }

    public FindExperimentRunsSQLQueryBuilder setSortKey(String sortKey) {
      this.sortKey = sortKey;
      return this;
    }

    public FindExperimentRunsSQLQueryBuilder setAscending(boolean ascending) {
      this.ascending = ascending;
      return this;
    }

    public FindExperimentRunsSQLQueryBuilder setIdsOnly(boolean idsOnly) {
      this.idsOnly = idsOnly;
      return this;
    }

    public FindExperimentRunsQuery build() throws ModelDBException {
      return new FindExperimentRunsQuery(this);
    }

    public Query buildQuery() throws ModelDBException {
      Query query = session.createSQLQuery(getSQLQueryString());
      RdbmsUtils.setParameterInQuery(query, parametersMap);

      if (this.pageNumber != null
          && this.pageLimit != null
          && this.pageNumber != 0
          && this.pageLimit != 0) {
        // Calculate number of documents to skip
        int skips = this.pageLimit * (this.pageNumber - 1);
        query.setFirstResult(skips);
        query.setMaxResults(this.pageLimit);
      }

      LOGGER.debug("Final find experimentRun query : {}", query.getQueryString());
      return query;
    }

    public Query buildCountQuery() {
      Query query = session.createQuery(this.countQueryString);
      RdbmsUtils.setParameterInQuery(query, parametersMap);
      return query;
    }

    private String getSQLQueryString() throws ModelDBException {
      String alias = " run";
      StringBuilder queryBuilder = new StringBuilder(" FROM experiment_run").append(alias);

      StringBuilder joinClause = new StringBuilder();
      Map<String, String> joinAliasMap = new HashMap<>();
      joinAliasMap.put(ModelDBConstants.PROJECT, "pr");
      joinClause
          .append(" INNER JOIN project pr ON pr.")
          .append(ModelDBConstants.ID)
          .append(" = ")
          .append(alias)
          .append(".")
          .append(ModelDBConstants.PROJECT_ID);
      joinAliasMap.put(ModelDBConstants.EXPERIMENT, "ex");
      joinClause
          .append(" INNER JOIN experiment ex ON ex.")
          .append(ModelDBConstants.ID)
          .append(" = ")
          .append(alias)
          .append(".")
          .append(ModelDBConstants.EXPERIMENT_ID);

      joinAliasMap.put(ModelDBConstants.KEY_VALUES, "kv");
      joinClause
          .append(" INNER JOIN keyvalue kv ON kv.experiment_run_id = ")
          .append(alias)
          .append(".")
          .append(ModelDBConstants.ID);

      StringBuilder selectClause = new StringBuilder();
      if (idsOnly) {
        selectClause.append(alias).append(".").append(ModelDBConstants.ID);
      } else {
        if (selectedResponseFields != null && !selectedResponseFields.isEmpty()) {
          for (int index = 0; index < selectedResponseFields.size(); index++) {
            String selectedField = selectedResponseFields.get(index);
            boolean isAppend = false;
            switch (selectedField) {
              case ModelDBConstants.METRICS:
              case ModelDBConstants.HYPERPARAMETERS:
              case ModelDBConstants.ATTRIBUTES:
                selectClause
                    .append(joinAliasMap.get(ModelDBConstants.KEY_VALUES))
                    .append(".")
                    .append("kv_key");
                isAppend = true;
                break;
            }
            if (index < selectedResponseFields.size() - 1 && isAppend) {
              selectClause.append(",");
            }
          }
        } else {
          selectClause.append(alias);
        }
      }

      String orderBy = alias + "." + sortKey;
      String[] sortKeyArr = sortKey.split("\\.");
      if (sortKeyArr.length > 1) {
        switch (sortKeyArr[0]) {
          case ModelDBConstants.METRICS:
          case ModelDBConstants.HYPERPARAMETERS:
          case ModelDBConstants.ATTRIBUTES:
            orderBy = joinAliasMap.get(ModelDBConstants.KEY_VALUES) + "." + sortKeyArr[1];
            break;
          default:
            throw new InvalidArgumentException("Sort key '" + sortKey + "' not supported");
        }
      }

      List<String> whereClauseList = new ArrayList<>();
      if (this.predicates != null && !this.predicates.isEmpty()) {
        for (int index = 0; index < this.predicates.size(); index++) {
          KeyValueQuery keyValueQuery = this.predicates.get(index);
          String[] keyArray = keyValueQuery.getKey().split("\\.");
          if (keyArray[0].contains(ModelDBConstants.METRICS)
              || keyArray[0].contains(ModelDBConstants.HYPERPARAMETERS)
              || keyArray[0].contains(ModelDBConstants.ATTRIBUTES)) {
            if (keyArray.length != 2) {
              throw new InvalidArgumentException("Valid key not found in predicates");
            }
            String joinAlias = joinAliasMap.get(ModelDBConstants.KEY_VALUES);
            StringBuilder joinStringBuilder =
                new StringBuilder(joinAlias).append(".").append(keyArray[1]).append(" ");
            VersioningUtils.setQueryParameters(
                index, joinStringBuilder, keyValueQuery, parametersMap);
            whereClauseList.add(joinStringBuilder.toString());
          } else if (keyValueQuery.getKey().contains(ModelDBConstants.OWNER)) {
            StringBuilder predicateStringBuilder =
                new StringBuilder(alias).append(".").append(ModelDBConstants.ID);
            setOwnerPredicate(index, keyValueQuery, predicateStringBuilder);
            whereClauseList.add(predicateStringBuilder.toString());
          } else if (keyArray.length > 1) {
            throw new InvalidArgumentException(
                "Predicate '" + keyValueQuery.getKey() + "' not supported");
          } else {
            StringBuilder predicateStringBuilder = new StringBuilder();
            predicateStringBuilder
                .append("lower(")
                .append(alias)
                .append(".")
                .append(keyValueQuery.getKey())
                .append(") ");
            VersioningUtils.setQueryParameters(
                index, predicateStringBuilder, keyValueQuery, parametersMap);
            whereClauseList.add(predicateStringBuilder.toString());
          }
        }
      }

      if (this.experimentRunIds.isEmpty()
          && this.projectIds.isEmpty()
          && this.experimentIds.isEmpty()) {
        throw new PermissionDeniedException(
            "Access is denied. Accessible projects, experiment, experimentRunIds not found");
      }

      if (!this.experimentRunIds.isEmpty()) {
        whereClauseList.add(alias + "." + ModelDBConstants.ID + " IN (:runIds) ");
        parametersMap.put("runIds", this.experimentRunIds);
      }

      if (this.experimentIds != null && !this.experimentIds.isEmpty()) {
        whereClauseList.add(alias + "." + ModelDBConstants.EXPERIMENT_ID + " IN (:exIds) ");
        parametersMap.put("exIds", this.experimentIds);
      }

      if (this.projectIds != null && !this.projectIds.isEmpty()) {
        whereClauseList.add(alias + "." + ModelDBConstants.PROJECT_ID + " IN (:projectIds) ");
        parametersMap.put("projectIds", this.projectIds);
      }

      whereClauseList.add(alias + "." + ModelDBConstants.DELETED + " = false ");
      whereClauseList.add(
          joinAliasMap.get(ModelDBConstants.PROJECT)
              + "."
              + ModelDBConstants.DELETED
              + " = false ");
      whereClauseList.add(
          joinAliasMap.get(ModelDBConstants.EXPERIMENT)
              + "."
              + ModelDBConstants.DELETED
              + " = false ");

      StringBuilder whereClause = new StringBuilder();
      whereClause.append(
          VersioningUtils.setPredicatesWithQueryOperator(
              "AND", whereClauseList.toArray(new String[0])));

      // Order by clause
      StringBuilder orderClause =
          new StringBuilder(" ORDER BY ")
              .append(orderBy)
              .append(" ")
              .append(ascending ? "ASC" : "DESC");

      StringBuilder finalQueryBuilder =
          new StringBuilder("SELECT ").append(selectClause).append(" ");
      finalQueryBuilder.append(queryBuilder);
      finalQueryBuilder.append(joinClause);
      if (!whereClause.toString().isEmpty()) {
        finalQueryBuilder.append(" WHERE ").append(whereClause);
      }
      finalQueryBuilder.append(orderClause);

      // Build count query
      StringBuilder countQueryBuilder =
          new StringBuilder("SELECT COUNT(").append(alias).append(") ");
      countQueryBuilder.append(queryBuilder);
      countQueryBuilder.append(joinClause);
      if (!whereClause.toString().isEmpty()) {
        countQueryBuilder.append(" WHERE ").append(whereClause);
      }
      this.countQueryString = countQueryBuilder.toString();

      LOGGER.trace("Creating SQL query");
      return finalQueryBuilder.toString();
    }

    private void setOwnerPredicate(
        int index, KeyValueQuery keyValueQuery, StringBuilder predicateStringBuilder)
        throws ModelDBException {
      OperatorEnum.Operator operator = keyValueQuery.getOperator();
      List<UserInfo> userInfoList;
      if (operator.equals(OperatorEnum.Operator.CONTAIN)
          || operator.equals(OperatorEnum.Operator.NOT_CONTAIN)) {
        UserInfoPaginationDTO userInfoPaginationDTO =
            authService.getFuzzyUserInfoList(keyValueQuery.getValue().getStringValue());
        userInfoList = userInfoPaginationDTO.getUserInfoList();
      } else {
        String ownerIdsArrString = keyValueQuery.getValue().getStringValue();
        List<String> ownerIds = new ArrayList<>();
        if (operator.equals(OperatorEnum.Operator.IN)) {
          ownerIds = Arrays.asList(ownerIdsArrString.split(","));
        } else {
          ownerIds.add(ownerIdsArrString);
        }
        Map<String, UserInfo> userInfoMap =
            authService.getUserInfoFromAuthServer(
                new HashSet<>(ownerIds), Collections.emptySet(), Collections.emptyList());
        userInfoList = new ArrayList<>(userInfoMap.values());
      }

      if (userInfoList != null && !userInfoList.isEmpty()) {
        List<String> vertaIds =
            userInfoList.stream()
                .map(authService::getVertaIdFromUserInfo)
                .collect(Collectors.toList());
        if (operator.equals(OperatorEnum.Operator.NOT_CONTAIN)
            || operator.equals(OperatorEnum.Operator.NE)) {
          String mapKey = "IN_VALUE_" + index;
          predicateStringBuilder.append(" NOT IN (:").append(mapKey).append(")");
          parametersMap.put(mapKey, vertaIds);
        } else {
          RdbmsUtils.setValueWithOperatorInQuery(
              index, predicateStringBuilder, OperatorEnum.Operator.IN, vertaIds, parametersMap);
        }
      } else {
        throw new ModelDBException(
            ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND, io.grpc.Status.Code.FAILED_PRECONDITION);
      }
    }
  }
}
