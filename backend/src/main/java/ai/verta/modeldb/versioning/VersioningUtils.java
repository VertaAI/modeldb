package ai.verta.modeldb.versioning;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class VersioningUtils {
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

  public static String getVersioningCompositeId(
      Long repoId, String commitHash, List<String> locations) {
    return repoId + "::" + commitHash + "::" + ModelDBUtils.getLocationWithSlashOperator(locations);
  }

  public static List<KeyValue> getAttributes(
      Session session, Long repoId, String commitHash, List<String> locations)
      throws ModelDBException {
    try {
      String getAttributesHQL =
          "From AttributeEntity kv where kv.entity_hash = :entityHash "
              + " AND kv.entity_name = :entityName AND kv.field_type = :fieldType";
      Query getQuery = session.createQuery(getAttributesHQL);
      getQuery.setParameter("entityName", ModelDBConstants.BLOB);
      getQuery.setParameter("entityHash", getVersioningCompositeId(repoId, commitHash, locations));
      getQuery.setParameter("fieldType", ModelDBConstants.ATTRIBUTES);
      List<AttributeEntity> attributeEntities = getQuery.list();
      return RdbmsUtils.convertAttributeEntityListFromAttributes(attributeEntities);
    } catch (InvalidProtocolBufferException e) {
      throw new ModelDBException(e);
    }
  }
}
