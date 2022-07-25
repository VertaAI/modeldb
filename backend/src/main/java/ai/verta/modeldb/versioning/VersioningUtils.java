package ai.verta.modeldb.versioning;

import ai.verta.common.ArtifactPart;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.UnimplementedException;
import ai.verta.modeldb.entities.ArtifactPartEntity;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.ResourceVisibility;
import io.grpc.Status;
import java.security.NoSuchAlgorithmException;
import java.util.*;
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
    var query = session.createQuery(COMMIT_BELONGS_TO_REPO_QUERY);
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
      StringBuilder childQuery =
          new StringBuilder(GET_PARENT_COMMIT_SHA_PREFIX).append(childCommit).append("\'");
      @SuppressWarnings("unchecked")
      Query<String> sqlQuery = session.createSQLQuery(childQuery.toString());
      List<String> parentCommitSHAs = sqlQuery.list();
      childCommitSHAs.addAll(parentCommitSHAs);
    }
    var getChildCommits =
        "FROM CommitEntity c WHERE c.commit_hash IN (:childCommitSHAs)  ORDER BY c.date_created DESC";
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
    var sb = new StringBuilder();
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
    var value = keyValueQuery.getValue();
    var operator = keyValueQuery.getOperator();
    switch (value.getKindCase()) {
      case NUMBER_VALUE:
        LOGGER.debug("Called switch case : number_value");
        RdbmsUtils.setValueWithOperatorInQuery(
            index, queryBuilder, operator, value.getNumberValue(), parametersMap);
        break;
      case STRING_VALUE:
        LOGGER.debug("Called switch case : string_value");
        if (!value.getStringValue().isEmpty()) {
          LOGGER.debug("Called switch case : string value exist");
          if (keyValueQuery.getKey().equals(ModelDBConstants.VISIBILITY)) {
            RdbmsUtils.setValueWithOperatorInQuery(
                index,
                queryBuilder,
                operator,
                ResourceVisibility.valueOf(value.getStringValue()).ordinal(),
                parametersMap);
          } else {
            RdbmsUtils.setValueWithOperatorInQuery(
                index, queryBuilder, operator, value.getStringValue(), parametersMap);
          }
          break;
        } else {
          throw new InvalidArgumentException("Predicate does not contain string value in request");
        }
      case BOOL_VALUE:
        LOGGER.debug("Called switch case : bool_value");
        RdbmsUtils.setValueWithOperatorInQuery(
            index, queryBuilder, operator, value.getStringValue(), parametersMap);
        break;
      default:
        throw new UnimplementedException(
            "Unknown 'Value' type recognized, valid 'Value' type are NUMBER_VALUE, STRING_VALUE, BOOL_VALUE");
    }
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
    List<AttributeEntity> attributeEntities =
        getAttributeEntities(session, repoId, commitHash, locations, attributeKeysList);
    return RdbmsUtils.convertAttributeEntityListFromAttributes(attributeEntities);
  }

  public static void saveArtifactPartEntity(
      ArtifactPart artifactPart, Session session, String artifactId, int artifactType) {
    var artifactPartEntity =
        new ArtifactPartEntity(
            artifactId, artifactType, artifactPart.getPartNumber(), artifactPart.getEtag());
    session.beginTransaction();
    session.saveOrUpdate(artifactPartEntity);
    session.getTransaction().commit();
  }

  public static Set<ArtifactPartEntity> getArtifactPartEntities(
      Session session, String artifactId, int artifactType) {
    var queryString =
        "From ArtifactPartEntity arp WHERE arp.artifact_type = :artifactType AND arp.artifact_id = :artifactId";
    var query = session.createQuery(queryString);
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
    var getAttributesHQLBuilder =
        new StringBuilder(
            "From AttributeEntity kv where kv.entity_hash = :entityHash "
                + " AND kv.entity_name = :entityName AND kv.field_type = :fieldType");
    if (attributeKeys != null && !attributeKeys.isEmpty()) {
      getAttributesHQLBuilder.append(" AND kv.key IN (:keys) ");
    }
    getAttributesHQLBuilder.append(" ORDER BY kv.id");
    var getQuery = session.createQuery(getAttributesHQLBuilder.toString());
    getQuery.setParameter("entityName", ModelDBConstants.BLOB);
    getQuery.setParameter("entityHash", getVersioningCompositeId(repoId, commitHash, locations));
    getQuery.setParameter("fieldType", ModelDBConstants.ATTRIBUTES);
    if (attributeKeys != null && !attributeKeys.isEmpty()) {
      getQuery.setParameterList("keys", attributeKeys);
    }
    return getQuery.list();
  }

  public static RepositoryEntity getDatasetRepositoryEntity(
      Session session,
      RepositoryDAO repositoryDAO,
      String datasetId,
      String datasetVersionId,
      boolean checkWrite)
      throws ModelDBException {
    RepositoryEntity repositoryEntity;

    var repositoryIdentification = RepositoryIdentification.newBuilder();
    if (datasetId == null || datasetId.isEmpty()) {
      var commitEntity = session.get(CommitEntity.class, datasetVersionId);

      if (commitEntity == null) {
        throw new ModelDBException("DatasetVersion not found", Status.Code.NOT_FOUND);
      }

      if (commitEntity.getRepository() != null && commitEntity.getRepository().size() > 1) {
        throw new ModelDBException(
            "DatasetVersion '"
                + commitEntity.getCommit_hash()
                + "' associated with multiple datasets",
            Status.Code.INTERNAL);
      }
      Long newRepoId = new ArrayList<>(commitEntity.getRepository()).get(0).getId();
      repositoryIdentification.setRepoId(newRepoId);
    } else {
      repositoryIdentification.setRepoId(Long.parseLong(datasetId));
    }
    repositoryEntity =
        repositoryDAO.getProtectedRepositoryById(repositoryIdentification.build(), checkWrite);
    return repositoryEntity;
  }
}
