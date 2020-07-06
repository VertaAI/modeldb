package ai.verta.modeldb.versioning;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.dto.CommitPaginationDTO;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.TagsEntity;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.CreateCommitRequest.Response;
import com.google.protobuf.ProtocolStringList;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      final String rootSha = setBlobs.apply(session);
      RepositoryEntity repositoryEntity = getRepository.apply(session);

      CommitEntity commitEntity =
          saveCommitEntity(session, commit, rootSha, author, repositoryEntity);
      session.getTransaction().commit();
      return Response.newBuilder().setCommit(commitEntity.toCommitProto()).build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setCommit(author, commit, setBlobs, getRepository);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public CommitEntity saveCommitEntity(
      Session session,
      Commit commit,
      String rootSha,
      String author,
      RepositoryEntity repositoryEntity)
      throws ModelDBException, NoSuchAlgorithmException {
    long timeCreated = new Date().getTime();
    if (App.getInstance().getStoreClientCreationTimestamp() && commit.getDateCreated() != 0L) {
      timeCreated = commit.getDateCreated();
    }
    final String commitSha = generateCommitSHA(rootSha, commit, timeCreated);

    Map<String, CommitEntity> parentCommitEntities = new HashMap<>();
    if (!commit.getParentShasList().isEmpty()) {
      parentCommitEntities =
          getCommits(session, repositoryEntity.getId(), commit.getParentShasList());
      if (parentCommitEntities.size() != commit.getParentShasCount()) {
        for (String parentSHA : commit.getParentShasList()) {
          if (!parentCommitEntities.containsKey(parentSHA)) {
            throw new ModelDBException(
                "Parent commit '" + parentSHA + "' not found in DB", Code.INVALID_ARGUMENT);
          }
        }
      }
    }
    Map<Integer, CommitEntity> parentOrderMap = new HashMap<>();
    for (int index = 0; index < commit.getParentShasCount(); index++) {
      parentOrderMap.put(index, parentCommitEntities.get(commit.getParentShas(index)));
    }

    Commit internalCommit =
        Commit.newBuilder()
            .setDateCreated(timeCreated)
            .setAuthor(author)
            .setMessage(commit.getMessage())
            .setCommitSha(commitSha)
            .build();
    CommitEntity commitEntity =
        new CommitEntity(repositoryEntity, parentOrderMap, internalCommit, rootSha);
    session.saveOrUpdate(commitEntity);
    return commitEntity;
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
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return listCommits(request, getRepository);
      } else {
        throw ex;
      }
    }
  }

  private String generateCommitSHA(String blobSHA, Commit commit, long timeCreated)
      throws NoSuchAlgorithmException {
    return VersioningUtils.generateCommitSHA(
        commit.getParentShasList(), commit.getMessage(), timeCreated, commit.getAuthor(), blobSHA);
  }
  /**
   * @param session session
   * @param parentShaList : a list of sha for which the function returns commits
   * @return {@link Map<String, CommitEntity>}
   */
  private Map<String, CommitEntity> getCommits(
      Session session, Long repoId, ProtocolStringList parentShaList) {
    StringBuilder commitQueryBuilder =
        new StringBuilder(
            "SELECT cm FROM "
                + CommitEntity.class.getSimpleName()
                + " cm LEFT JOIN cm.repository repo WHERE repo.id = :repoId AND cm.commit_hash IN (:commitHashes)");

    Query<CommitEntity> commitEntityQuery =
        session.createQuery(commitQueryBuilder.append(" ORDER BY cm.date_created DESC").toString());
    commitEntityQuery.setParameter("repoId", repoId);
    commitEntityQuery.setParameter("commitHashes", parentShaList);
    return commitEntityQuery.list().stream()
        .collect(Collectors.toMap(CommitEntity::getCommit_hash, commitEntity -> commitEntity));
  }

  @Override
  public Commit getCommit(String commitHash, RepositoryFunction getRepository)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      CommitEntity commitEntity = getCommitEntity(session, commitHash, getRepository);

      return commitEntity.toCommitProto();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getCommit(commitHash, getRepository);
      } else {
        throw ex;
      }
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
      RepositoryEntity repositoryEntity =
          repositoryDAO.getRepositoryById(session, request.getRepositoryId(), true);
      boolean exists =
          VersioningUtils.commitRepositoryMappingExists(
              session, request.getCommitSha(), repositoryEntity.getId());
      if (!exists) {
        throw new ModelDBException(
            "Commit_hash and repository_id mapping not found", Code.NOT_FOUND);
      }

      Query getCommitQuery =
          session.createQuery(
              "From "
                  + CommitEntity.class.getSimpleName()
                  + " c WHERE c.commit_hash = :commitHash");
      getCommitQuery.setParameter("commitHash", request.getCommitSha());
      CommitEntity commitEntity = (CommitEntity) getCommitQuery.uniqueResult();

      if (!commitEntity.getChild_commits().isEmpty()) {
        throw new ModelDBException(
            "Commit has the child, please delete child commit first", Code.FAILED_PRECONDITION);
      }

      StringBuilder getBranchByCommitHQLBuilder =
          new StringBuilder("FROM ")
              .append(BranchEntity.class.getSimpleName())
              .append(" br where br.id.repository_id = :repositoryId ")
              .append(" AND br.commit_hash = :commitHash ");
      Query getBranchByCommitQuery = session.createQuery(getBranchByCommitHQLBuilder.toString());
      getBranchByCommitQuery.setParameter("repositoryId", repositoryEntity.getId());
      getBranchByCommitQuery.setParameter("commitHash", commitEntity.getCommit_hash());
      List<BranchEntity> branchEntities = getBranchByCommitQuery.list();
      if (branchEntities != null && !branchEntities.isEmpty()) {
        StringBuilder errorMessage = new StringBuilder("Commit is associated with branch name : ");
        int count = 0;
        for (BranchEntity branchEntity : branchEntities) {
          errorMessage.append(branchEntity.getId().getBranch());
          if (count < branchEntities.size() - 1) {
            errorMessage.append(", ");
          }
          count++;
        }
        throw new ModelDBException(errorMessage.toString(), Code.FAILED_PRECONDITION);
      }

      String getLabelsHql =
          new StringBuilder("From LabelsMappingEntity lm where lm.id.")
              .append(ModelDBConstants.ENTITY_HASH)
              .append(" = :entityHash ")
              .append(" AND lm.id.")
              .append(ModelDBConstants.ENTITY_TYPE)
              .append(" = :entityType")
              .toString();
      Query query = session.createQuery(getLabelsHql);
      query.setParameter("entityHash", commitEntity.getCommit_hash());
      query.setParameter("entityType", IDTypeEnum.IDType.VERSIONING_COMMIT_VALUE);
      List<LabelsMappingEntity> labelsMappingEntities = query.list();
      if (labelsMappingEntities.size() > 0) {
        throw new ModelDBException("Commit is associated with Label", Code.FAILED_PRECONDITION);
      }

      String getTagsHql =
          new StringBuilder("From TagsEntity te where te.id.")
              .append(ModelDBConstants.REPOSITORY_ID)
              .append(" = :repoId ")
              .append(" AND te.commit_hash")
              .append(" = :commitHash")
              .toString();
      Query getTagsQuery = session.createQuery(getTagsHql);
      getTagsQuery.setParameter("repoId", repositoryEntity.getId());
      getTagsQuery.setParameter("commitHash", commitEntity.getCommit_hash());
      List<TagsEntity> tagsEntities = getTagsQuery.list();
      if (tagsEntities.size() > 0) {
        throw new ModelDBException(
            "Commit is associated with Tags : "
                + tagsEntities.stream()
                    .map(tagsEntity -> tagsEntity.getId().getTag())
                    .collect(Collectors.joining(",")),
            Code.FAILED_PRECONDITION);
      }

      session.beginTransaction();
      // delete associated branch
      repositoryDAO.deleteBranchByCommit(session, repositoryEntity.getId(), request.getCommitSha());

      if (commitEntity.getRepository().size() == 1) {
        session.delete(commitEntity);
      } else {
        commitEntity.getRepository().remove(repositoryEntity);
        session.update(commitEntity);
      }
      session.getTransaction().commit();
      return DeleteCommitRequest.Response.newBuilder().build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteCommit(request, repositoryDAO);
      } else {
        throw ex;
      }
    }
  }
}
