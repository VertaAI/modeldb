package ai.verta.modeldb.versioning;

import ai.verta.common.ArtifactPart;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.ArtifactPartEntity;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class VersioningUtils {
  private static final Logger LOGGER = LogManager.getLogger(VersioningUtils.class);

  private static final String COMMIT_BELONGS_TO_REPO_QUERY =
      "SELECT count(*) FROM CommitEntity c Join c.repository r WHERE c.commit_hash =  :commitHash AND r.id = :repositoryId";
  private static final String GET_PARENT_COMMIT_SHA_PREFIX =
      "SELECT parent_hash FROM commit_parent WHERE child_hash = \'";

  /**
   * Checks the database and returns if a commitHash belongs to a repository
   *
   * @param session
   * @param commitHash : hash of commit
   * @param repositoryId : id of the repository
   * @return
   */
  static boolean commitRepositoryMappingExists(
      Session session, String commitHash, Long repositoryId) {
    Query query = session.createQuery(COMMIT_BELONGS_TO_REPO_QUERY);
    query.setParameter("commitHash", commitHash);
    query.setParameter("repositoryId", repositoryId);
    Long count = (Long) query.getSingleResult();
    return count > 0;
  }

  /**
   * Returns the parents of a commit sorted in descending orderof time
   *
   * @param session
   * @param commitSHA
   * @return
   */
  // TODO: RepoDAO.listBranchCommits should use this
  public static List<CommitEntity> getParentCommits(Session session, String commitSHA) {
    List<String> childCommitSHAs = new LinkedList<String>();
    childCommitSHAs.add(commitSHA);
    List<String> commitSHAs = new LinkedList<>();
    while (!childCommitSHAs.isEmpty()) {
      String childCommit = childCommitSHAs.remove(0);
      commitSHAs.add(childCommit);
      @SuppressWarnings("unchecked")
      Query<String> sqlQuery =
          session.createSQLQuery(GET_PARENT_COMMIT_SHA_PREFIX + childCommit + "\'");
      List<String> parentCommitSHAs = sqlQuery.list();
      childCommitSHAs.addAll(parentCommitSHAs);
    }
    String getChildCommits =
        "FROM "
            + CommitEntity.class.getSimpleName()
            + " c WHERE c.commit_hash IN (:childCommitSHAs)  ORDER BY c.date_created DESC";
    @SuppressWarnings("unchecked")
    Query<CommitEntity> query = session.createQuery(getChildCommits);
    query.setParameterList("childCommitSHAs", commitSHAs);
    return query.list();
  }
  /**
   * Given commit components returns commitSHA
   *
   * @param parentSHAs
   * @param message
   * @param timeCreated
   * @param author
   * @param blobSHA
   * @return
   * @throws NoSuchAlgorithmException
   */
  public static String generateCommitSHA(
      List<String> parentSHAs, String message, long timeCreated, String author, String blobSHA)
      throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    if (!parentSHAs.isEmpty()) {
      parentSHAs = parentSHAs.stream().sorted().collect(Collectors.toList());
      sb.append("parent:");
      parentSHAs.forEach(pSHA -> sb.append(pSHA));
    }
    sb.append(":message:")
        .append(message)
        .append(":date_created:")
        .append(timeCreated)
        .append(":author:")
        .append(author)
        .append(":rootHash:")
        .append(blobSHA);

    return FileHasher.getSha(sb.toString());
  }

  public static String revertCommitMessage(Commit revertCommit) {
    return "Revert \""
        + revertCommit.getMessage()
        + " ("
        + revertCommit.getCommitSha().substring(0, 7)
        + ")\"";
  }

  /**
   * joins the predicate Clauses with the operator name
   *
   * @param operatorName : like AND,OR etc.
   * @param predicateClauses : where clause string like ['repo.id = 123', 'repo.name = xyz']
   * @return {@link String} : 'repo.id = 123 AND repo.name = xyz'
   */
  public static String setPredicatesWithQueryOperator(
      String operatorName, String[] predicateClauses) {
    return String.join(" " + operatorName + " ", predicateClauses);
  }

  /**
   * Set keyValueQuery value in the query builder with requested operator and set query parameters
   * in parametersMap
   */
  public static void setQueryParameters(
      int index,
      StringBuilder queryBuilder,
      KeyValueQuery keyValueQuery,
      Map<String, Object> parametersMap) {
    Value value = keyValueQuery.getValue();
    OperatorEnum.Operator operator = keyValueQuery.getOperator();
    switch (value.getKindCase()) {
      case NUMBER_VALUE:
        LOGGER.debug("Called switch case : number_value");
        setValueWithOperatorInQuery(
            index, queryBuilder, operator, value.getNumberValue(), parametersMap);
        break;
      case STRING_VALUE:
        LOGGER.debug("Called switch case : string_value");
        if (!value.getStringValue().isEmpty()) {
          LOGGER.debug("Called switch case : string value exist");
          if (keyValueQuery.getKey().equals(ModelDBConstants.REPOSITORY_VISIBILITY)) {
            setValueWithOperatorInQuery(
                index,
                queryBuilder,
                operator,
                RepositoryVisibilityEnum.RepositoryVisibility.valueOf(value.getStringValue())
                    .ordinal(),
                parametersMap);
          } else {
            setValueWithOperatorInQuery(
                index, queryBuilder, operator, value.getStringValue(), parametersMap);
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
            index, queryBuilder, operator, value.getStringValue(), parametersMap);
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

  /**
   * @param index0 : predicate index for unique query parameter identity
   * @param queryBuilder : query builder
   * @param operator : query operator like GE, LE, EQ, GTE etc.
   * @param value : query parameter value like repoId(123), repoName(xyz) etc.
   * @param parametersMap : query parameter identity map.
   */
  public static void setValueWithOperatorInQuery(
      int index0,
      StringBuilder queryBuilder,
      OperatorEnum.Operator operator,
      Object value,
      Map<String, Object> parametersMap) {
    long index = index0 + Math.round(100.0 * Math.random());
    String key;
    switch (operator.ordinal()) {
      case OperatorEnum.Operator.GT_VALUE:
        key = "GT_VALUE_" + index;
        queryBuilder.append(" > :").append(key);
        parametersMap.put(key, value);
        break;
      case OperatorEnum.Operator.GTE_VALUE:
        key = "GTE_VALUE_" + index;
        queryBuilder.append(" >= :").append(key);
        parametersMap.put(key, value);
        break;
      case OperatorEnum.Operator.LT_VALUE:
        key = "LT_VALUE_" + index;
        queryBuilder.append(" < :").append(key);
        parametersMap.put(key, value);
        break;
      case OperatorEnum.Operator.LTE_VALUE:
        key = "LTE_VALUE_" + index;
        queryBuilder.append(" <= :").append(key);
        parametersMap.put(key, value);
        break;
      case OperatorEnum.Operator.NE_VALUE:
        key = "NE_VALUE_" + index;
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
        key = "IN_VALUE_" + index;
        queryBuilder.append(" IN (:").append(key).append(")");
        parametersMap.put(key, value);
        break;
      default:
        key = "default_" + index;
        queryBuilder.append(" = :").append(key);
        parametersMap.put(key, value);
    }
    queryBuilder.append(" ");
  }

  public static String getVersioningCompositeId(
      Long repoId, String commitHash, List<String> locations) {
    return repoId + "::" + commitHash + "::" + ModelDBUtils.getLocationWithSlashOperator(locations);
  }

  public static String[] getDatasetVersionBlobCompositeIdString(String compositeId) {
    return compositeId.split("::");
  }

  public static List<KeyValue> getAttributes(
      Session session,
      Long repoId,
      String commitHash,
      List<String> locations,
      List<String> attributeKeysList)
      throws ModelDBException {
    try {
      List<AttributeEntity> attributeEntities =
          getAttributeEntities(session, repoId, commitHash, locations, attributeKeysList);
      return RdbmsUtils.convertAttributeEntityListFromAttributes(attributeEntities);
    } catch (InvalidProtocolBufferException e) {
      throw new ModelDBException(e);
    }
  }

  public static void saveOrUpdateArtifactPartEntity(
      ArtifactPart artifactPart, Session session, String artifactId, int artifactType) {
    ArtifactPartEntity artifactPartEntity =
        new ArtifactPartEntity(
            artifactId, artifactType, artifactPart.getPartNumber(), artifactPart.getEtag());
    session.beginTransaction();
    session.saveOrUpdate(artifactPartEntity);
    session.getTransaction().commit();
  }

  public static Set<ArtifactPartEntity> getArtifactPartEntities(
      Session session, String artifactId, int artifactType) {
    String queryString =
        "From "
            + ArtifactPartEntity.class.getSimpleName()
            + " arp WHERE arp.artifact_type = :artifactType AND arp.artifact_id = :artifactId";
    Query query = session.createQuery(queryString);
    query.setParameter("artifactType", artifactType);
    query.setParameter("artifactId", artifactId);
    List<ArtifactPartEntity> artifactPartEntities = query.list();
    return new HashSet<>(artifactPartEntities);
  }

  public static List<AttributeEntity> getAttributeEntities(
      Session session,
      Long repoId,
      String commitHash,
      List<String> locations,
      List<String> attributeKeys) {
    StringBuilder getAttributesHQLBuilder =
        new StringBuilder(
            "From AttributeEntity kv where kv.entity_hash = :entityHash "
                + " AND kv.entity_name = :entityName AND kv.field_type = :fieldType");
    if (attributeKeys != null && !attributeKeys.isEmpty()) {
      getAttributesHQLBuilder.append(" AND kv.key IN (:keys) ");
    }
    getAttributesHQLBuilder.append(" ORDER BY kv.id");
    Query getQuery = session.createQuery(getAttributesHQLBuilder.toString());
    getQuery.setParameter("entityName", ModelDBConstants.BLOB);
    getQuery.setParameter("entityHash", getVersioningCompositeId(repoId, commitHash, locations));
    getQuery.setParameter("fieldType", ModelDBConstants.ATTRIBUTES);
    if (attributeKeys != null && !attributeKeys.isEmpty()) {
      getQuery.setParameterList("keys", attributeKeys);
    }
    return getQuery.list();
  }
}
