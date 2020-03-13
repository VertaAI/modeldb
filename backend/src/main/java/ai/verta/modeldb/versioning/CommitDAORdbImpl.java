package ai.verta.modeldb.versioning;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.versioning.CreateCommitRequest.Response;
import com.google.protobuf.ProtocolStringList;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class CommitDAORdbImpl implements CommitDAO {

  /**
   * commit : details of the commit and the blobs to be added setBlobs : recursively creates trees
   * and blobs in top down fashion and generates SHAs in bottom up fashion getRepository : fetches
   * the repository the commit is made on
   */
  public Response setCommit(
      String author, Commit commit, BlobFunction setBlobs, RepositoryFunction getRepository)
      throws ModelDBException, NoSuchAlgorithmException {
    final String rootSha = setBlobs.apply();
    long timeCreated = new Date().getTime();
    if (App.getInstance().getStoreClientCreationTimestamp() && commit.getDateCreated() != 0L) {
      timeCreated = commit.getDateCreated();
    }
    final String commitSha = generateCommitSHA(rootSha, commit, timeCreated);
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      Commit internalCommit =
          Commit.newBuilder()
              .setDateCreated(timeCreated)
              .setAuthor(author)
              .setMessage(commit.getMessage())
              .setCommitSha(commitSha)
              .build();
      CommitEntity commitEntity =
          new CommitEntity(
              getRepository.apply(session),
              getCommits(session, commit.getParentShasList()),
              internalCommit,
              rootSha);
      session.saveOrUpdate(commitEntity);
      session.getTransaction().commit();
      return Response.newBuilder().setCommit(commitEntity.toCommitProto()).build();
    }
  }

  @Override
  public ListCommitsRequest.Response listCommits(
      ListCommitsRequest request, RepositoryFunction getRepository) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepository.apply(session);

      StringBuilder commitQueryBuilder =
          new StringBuilder(
              "SELECT cm FROM "
                  + CommitEntity.class.getSimpleName()
                  + " cm LEFT JOIN cm.repository repo WHERE repo.id = :repoId ");
      if (!request.getCommitBase().isEmpty()) {
        CommitEntity baseCommitEntity =
            Optional.ofNullable(session.get(CommitEntity.class, request.getCommitBase()))
                .orElseThrow(
                    () ->
                        new ModelDBException(
                            "Couldn't find base commit by sha : " + request.getCommitBase(),
                            Code.NOT_FOUND));
        Long baseTime = baseCommitEntity.getDate_created();
        commitQueryBuilder.append(" AND cm.date_created >= " + baseTime);
      }

      if (!request.getCommitHead().isEmpty()) {
        CommitEntity headCommitEntity =
            Optional.ofNullable(session.get(CommitEntity.class, request.getCommitHead()))
                .orElseThrow(
                    () ->
                        new ModelDBException(
                            "Couldn't find head commit by sha : " + request.getCommitHead(),
                            Code.NOT_FOUND));
        Long headTime = headCommitEntity.getDate_created();
        commitQueryBuilder.append(" AND cm.date_created <= " + headTime);
      }

      Query<CommitEntity> commitEntityQuery =
          session.createQuery(
              commitQueryBuilder.append(" ORDER BY cm.date_created DESC").toString());
      commitEntityQuery.setParameter("repoId", repository.getId());
      if (request.hasPagination()) {
        int pageLimit = request.getPagination().getPageLimit();
        final int startPosition = (request.getPagination().getPageNumber() - 1) * pageLimit;
        commitEntityQuery.setFirstResult(startPosition);
        commitEntityQuery.setMaxResults(pageLimit);
      }

      List<Commit> commits =
          commitEntityQuery.list().stream()
              .map(CommitEntity::toCommitProto)
              .collect(Collectors.toList());
      return ListCommitsRequest.Response.newBuilder().addAllCommits(commits).build();
    }
  }

  private String generateCommitSHA(String blobSHA, Commit commit, long timeCreated)
      throws NoSuchAlgorithmException {

    StringBuilder sb = new StringBuilder();
    if (!commit.getParentShasList().isEmpty()) {
      List<String> parentSHAs = commit.getParentShasList();
      parentSHAs = parentSHAs.stream().sorted().collect(Collectors.toList());
      sb.append("parent:");
      parentSHAs.forEach(pSHA -> sb.append(pSHA));
    }
    sb.append(":message:")
        .append(commit.getMessage())
        .append(":date_created:")
        .append(timeCreated)
        .append(":author:")
        .append(commit.getAuthor())
        .append(":rootHash:")
        .append(blobSHA);

    return FileHasher.getSha(sb.toString());
  }

  /**
   * @param session
   * @param ShasList : a list of sha for which the function returns commits
   * @return
   * @throws ModelDBException : if any of the input sha are not identified as a commit
   */
  private List<CommitEntity> getCommits(Session session, ProtocolStringList ShasList)
      throws ModelDBException {
    List<CommitEntity> result =
        ShasList.stream()
            .map(sha -> session.get(CommitEntity.class, sha))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    if (result.size() != ShasList.size()) {
      throw new ModelDBException("Cannot find commits", Code.INVALID_ARGUMENT);
    }
    return result;
  }

  @Override
  public Commit getCommit(String commitHash, RepositoryFunction getRepository)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      CommitEntity commitEntity = getCommitEntity(session, commitHash, getRepository);

      session.getTransaction().commit();
      return commitEntity.toCommitProto();
    }
  }

  @Override
  public CommitEntity getCommitEntity(
      Session session, String commitHash, RepositoryFunction getRepositoryFunction)
      throws ModelDBException {
    RepositoryEntity repositoryEntity = getRepositoryFunction.apply(session);
    boolean exists =
        VersioningUtils.commitRepositoryMappingExists(
            session, commitHash, repositoryEntity.getId());
    if (!exists) {
      throw new ModelDBException("Commit_hash and repository_id mapping not found", Code.NOT_FOUND);
    }

    return session.load(CommitEntity.class, commitHash);
  }

  @Override
  public DeleteCommitRequest.Response deleteCommit(
      String commitHash, RepositoryFunction getRepository) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repositoryEntity = getRepository.apply(session);
      boolean exists =
          VersioningUtils.commitRepositoryMappingExists(
              session, commitHash, repositoryEntity.getId());
      if (!exists) {
        throw new ModelDBException(
            "Commit_hash and repository_id mapping not found", Code.NOT_FOUND);
      }

      Query deleteQuery =
          session.createQuery(
              "From "
                  + CommitEntity.class.getSimpleName()
                  + " c WHERE c.commit_hash = :commitHash");
      deleteQuery.setParameter("commitHash", commitHash);
      CommitEntity commitEntity = (CommitEntity) deleteQuery.uniqueResult();
      if (commitEntity.getRepository().size() == 1) {
        session.delete(commitEntity);
      } else {
        commitEntity.getRepository().remove(repositoryEntity);
        session.update(commitEntity);
      }
      session.getTransaction().commit();
      return DeleteCommitRequest.Response.newBuilder().build();
    }
  }
}
