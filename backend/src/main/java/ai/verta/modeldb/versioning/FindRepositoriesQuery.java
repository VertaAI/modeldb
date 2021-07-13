package ai.verta.modeldb.versioning;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.dto.UserInfoPaginationDTO;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEnums;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.UserInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    final RoleService roleService;
    String countQueryString;
    Map<String, Object> parametersMap = new HashMap<>();

    // optional parameters
    private List<Long> repoIds;
    private List<KeyValueQuery> predicates;
    private Integer pageNumber = 0;
    private Integer pageLimit = 0;

    public FindRepositoriesHQLQueryBuilder(
        Session session, AuthService authService, RoleService roleService) {
      this.session = session;
      this.authService = authService;
      this.roleService = roleService;
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

      LOGGER.debug("Final find repository query : {}", query.getQueryString());
      return query;
    }

    public Query buildCountQuery() {
      Query query = session.createQuery(this.countQueryString);
      RdbmsUtils.setParameterInQuery(query, parametersMap);
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
                joinClause.append(
                    VersioningUtils.setPredicatesWithQueryOperator("AND", joinClauses));
              }
              index[0]++;
            });
      }

      List<String> whereClauseList = new ArrayList<>();
      if (this.predicates != null && !this.predicates.isEmpty()) {
        for (int index = 0; index < this.predicates.size(); index++) {
          KeyValueQuery keyValueQuery = this.predicates.get(index);
          if (keyValueQuery.getKey().contains(ModelDBConstants.LABEL)) {
            String joinAlias = joinAliasMap.get(keyValueQuery.getKey() + index);
            StringBuilder joinStringBuilder = new StringBuilder(joinAlias).append(".id.label ");
            VersioningUtils.setQueryParameters(
                index, joinStringBuilder, keyValueQuery, parametersMap);
            whereClauseList.add(joinStringBuilder.toString());
          } else if (keyValueQuery.getKey().contains(ModelDBConstants.OWNER)) {
            StringBuilder predicateStringBuilder =
                new StringBuilder(alias).append(".").append(ModelDBConstants.ID);
            setOwnerPredicate(index, keyValueQuery, predicateStringBuilder);
            whereClauseList.add(predicateStringBuilder.toString());
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

      if (this.repoIds != null && !this.repoIds.isEmpty()) {
        whereClauseList.add(alias + "." + ModelDBConstants.ID + " IN (:repoIds) ");
        parametersMap.put("repoIds", this.repoIds);
      }
      whereClauseList.add(alias + "." + ModelDBConstants.DELETED + " = false ");
      whereClauseList.add(alias + "." + ModelDBConstants.CREATED + " = true ");
      whereClauseList.add(
          alias
              + ".repositoryAccessModifier = "
              + RepositoryEnums.RepositoryModifierEnum.REGULAR.ordinal());

      StringBuilder whereClause = new StringBuilder();
      whereClause.append(
          VersioningUtils.setPredicatesWithQueryOperator(
              "AND", whereClauseList.toArray(new String[0])));

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
        Set<String> repositoryIdSet =
            RdbmsUtils.getResourceIdsFromUserWorkspaces(
                authService,
                roleService,
                ModelDBResourceEnum.ModelDBServiceResourceTypes.REPOSITORY,
                userInfoList);
        List<Long> resourcesIds =
            repositoryIdSet.stream().map(Long::parseLong).collect(Collectors.toList());
        if (operator.equals(OperatorEnum.Operator.NOT_CONTAIN)
            || operator.equals(OperatorEnum.Operator.NE)) {
          String mapKey = "IN_VALUE_" + index;
          predicateStringBuilder.append(" NOT IN (:").append(mapKey).append(")");
          parametersMap.put(mapKey, resourcesIds);
        } else {
          RdbmsUtils.setValueWithOperatorInQuery(
              index, predicateStringBuilder, OperatorEnum.Operator.IN, resourcesIds, parametersMap);
        }
      } else {
        throw new ModelDBException(
            ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND, io.grpc.Status.Code.FAILED_PRECONDITION);
      }
    }
  }
}
