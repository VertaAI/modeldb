package ai.verta.modeldb.versioning;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.dto.CommitPaginationDTO;
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
    long timeCreated = new Date().getTime();
    if (App.getInstance().getStoreClientCreationTimestamp() && commit.getDateCreated() != 0L) {
      timeCreated = commit.getDateCreated();
    }
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      final String rootSha = setBlobs.apply(session);
      final String commitSha = generateCommitSHA(rootSha, commit, timeCreated);

      Commit internalCommit =
          Commit.newBuilder()
              .setDateCreated(timeCreated)
              .setAuthor(author)
              .setMessage(commit.getMessage())
              .setCommitSha(commitSha)
              .build();
      RepositoryEntity repositoryEntity = getRepository.apply(session);
      CommitEntity commitEntity =
          new CommitEntity(
              repositoryEntity,
              getCommits(session, commit.getParentShasList()),
              internalCommit,
              rootSha);
      session.saveOrUpdate(commitEntity);
      repositoryEntity.setDate_updated(commitEntity.getDate_created());
      session.update(repositoryEntity);
      session.getTransaction().commit();
      return Response.newBuilder().setCommit(commitEntity.toCommitProto()).build();
    }
  }

  @Override
  public CommitPaginationDTO fetchCommitEntityList(
      Session session, ListCommitsRequest request, Long repoId) throws ModelDBException {
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
        session.createQuery(commitQueryBuilder.append(" ORDER BY cm.date_created DESC").toString());
    commitEntityQuery.setParameter("repoId", repoId);
    if (request.hasPagination()) {
      int pageLimit = request.getPagination().getPageLimit();
      final int startPosition = (request.getPagination().getPageNumber() - 1) * pageLimit;
      commitEntityQuery.setFirstResult(startPosition);
      commitEntityQuery.setMaxResults(pageLimit);
    }

    Query countQuery = session.createQuery(commitQueryBuilder.toString());
    countQuery.setParameter("repoId", repoId);
    // TODO: improve query into count query
    Long totalRecords = (long) countQuery.list().size();

    CommitPaginationDTO commitPaginationDTO = new CommitPaginationDTO();
    commitPaginationDTO.setCommitEntities(commitEntityQuery.list());
    commitPaginationDTO.setTotalRecords(totalRecords);
    return commitPaginationDTO;
  }

  @Override
  public ListCommitsRequest.Response listCommits(
      ListCommitsRequest request, RepositoryFunction getRepository) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repository = getRepository.apply(session);

      CommitPaginationDTO commitPaginationDTO =
          fetchCommitEntityList(session, request, repository.getId());
      List<Commit> commits =
          commitPaginationDTO.getCommitEntities().stream()
              .map(CommitEntity::toCommitProto)
              .collect(Collectors.toList());
      return ListCommitsRequest.Response.newBuilder()
          .addAllCommits(commits)
          .setTotalRecords(commitPaginationDTO.getTotalRecords())
          .build();
    }
  }

  private String generateCommitSHA(String blobSHA, Commit commit, long timeCreated)
      throws NoSuchAlgorithmException {
    return VersioningUtils.generateCommitSHA(
        commit.getParentShasList(), commit.getMessage(), timeCreated, commit.getAuthor(), blobSHA);
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
      DeleteCommitRequest request, RepositoryDAO repositoryDAO) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repositoryEntity =
          repositoryDAO.getRepositoryById(session, request.getRepositoryId());
      boolean exists =
          VersioningUtils.commitRepositoryMappingExists(
              session, request.getCommitSha(), repositoryEntity.getId());
      if (!exists) {
        throw new ModelDBException(
            "Commit_hash and repository_id mapping not found", Code.NOT_FOUND);
      }

      Query deleteQuery =
          session.createQuery(
              "From "
                  + CommitEntity.class.getSimpleName()
                  + " c WHERE c.commit_hash = :commitHash");
      deleteQuery.setParameter("commitHash", request.getCommitSha());
      CommitEntity commitEntity = (CommitEntity) deleteQuery.uniqueResult();

      if (!commitEntity.getChild_commits().isEmpty()) {
        throw new ModelDBException(
            "Commit has the child, please delete child commit first", Code.FAILED_PRECONDITION);
      }

      // delete associated branch
      repositoryDAO.deleteBranchByCommit(repositoryEntity.getId(), request.getCommitSha());

      if (commitEntity.getRepository().size() == 1) {
        session.delete(commitEntity);
      } else {
        commitEntity.getRepository().remove(repositoryEntity);
        session.update(commitEntity);
      }
      repositoryEntity.setDate_updated(new Date().getTime());
      session.update(repositoryEntity);
      session.getTransaction().commit();
      return DeleteCommitRequest.Response.newBuilder().build();
    }
  }
}
