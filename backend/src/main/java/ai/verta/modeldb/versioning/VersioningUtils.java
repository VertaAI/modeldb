package ai.verta.modeldb.versioning;

import ai.verta.common.ArtifactPart;
import ai.verta.modeldb.entities.ArtifactPartEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
}
