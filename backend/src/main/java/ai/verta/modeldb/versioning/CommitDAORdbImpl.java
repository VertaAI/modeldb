package ai.verta.modeldb.versioning;

import ai.verta.modeldb.App;
import ai.verta.modeldb.DatasetPartInfo;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.PathDatasetVersionInfo;
import ai.verta.modeldb.PathLocationTypeEnum.PathLocationType;
import ai.verta.modeldb.dto.CommitPaginationDTO;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.TagsEntity;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.metadata.IdentificationType;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.blob.container.BlobContainer;
import com.google.protobuf.ProtocolStringList;
import io.grpc.Status;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class CommitDAORdbImpl implements CommitDAO {

  /**
   * commit : details of the commit and the blobs to be added setBlobs : recursively creates trees
   * and blobs in top down fashion and generates SHAs in bottom up fashion getRepository : fetches
   * the repository the commit is made on
   */
  public CreateCommitRequest.Response setCommit(
      String author,
      Commit commit,
      BlobFunction setBlobs,
      BlobFunction.BlobFunctionAttribute setBlobsAttributes,
      RepositoryFunction getRepository)
      throws ModelDBException, NoSuchAlgorithmException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      final String rootSha = setBlobs.apply(session);
      RepositoryEntity repositoryEntity = getRepository.apply(session);

      CommitEntity commitEntity =
          saveCommitEntity(session, commit, rootSha, author, repositoryEntity);
      setBlobsAttributes.apply(session, repositoryEntity.getId(), commitEntity.getCommit_hash());
      session.getTransaction().commit();
      return CreateCommitRequest.Response.newBuilder()
          .setCommit(commitEntity.toCommitProto())
          .build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setCommit(author, commit, setBlobs, setBlobsAttributes, getRepository);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public CreateCommitRequest.Response setCommitFromDatasetVersion(
      DatasetVersion datasetVersion,
      BlobDAO blobDAO,
      MetadataDAO metadataDAO,
      RepositoryEntity repositoryEntity)
      throws ModelDBException, NoSuchAlgorithmException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetBlob.Builder datasetBlobBuilder = DatasetBlob.newBuilder();
      Blob.Builder blobBuilder = Blob.newBuilder();
      blobBuilder.addAllAttributes(datasetVersion.getAttributesList());
      switch (datasetVersion.getDatasetVersionInfoCase()) {
        case RAW_DATASET_VERSION_INFO:
        case QUERY_DATASET_VERSION_INFO:
          throw new ModelDBException("Not supported", Code.UNIMPLEMENTED);
        case PATH_DATASET_VERSION_INFO:
          PathDatasetVersionInfo pathDatasetVersionInfo =
              datasetVersion.getPathDatasetVersionInfo();
          List<DatasetPartInfo> partInfos = pathDatasetVersionInfo.getDatasetPartInfosList();
          Stream<PathDatasetComponentBlob> result = partInfos.stream().map(this::componentFromPart);
          if (pathDatasetVersionInfo.getLocationType() == PathLocationType.S3_FILE_SYSTEM) {
            datasetBlobBuilder.setS3(
                S3DatasetBlob.newBuilder()
                    .addAllComponents(
                        result
                            .map(path -> S3DatasetComponentBlob.newBuilder().setPath(path).build())
                            .collect(Collectors.toList())));
          } else {
            datasetBlobBuilder.setPath(
                PathDatasetBlob.newBuilder().addAllComponents(result.collect(Collectors.toList())));
          }
          break;
        case DATASETVERSIONINFO_NOT_SET:
          throw new ModelDBException("Wrong dataset version type", Code.INVALID_ARGUMENT);
      }
      List<String> location =
          Collections.singletonList(ModelDBConstants.DEFAULT_VERSIONING_BLOB_LOCATION);
      List<BlobContainer> blobList =
          Collections.singletonList(
              BlobContainer.create(
                  BlobExpanded.newBuilder()
                      .addAllLocation(location)
                      .setBlob(blobBuilder.setDataset(datasetBlobBuilder))
                      .build()));

      session.beginTransaction();
      final String rootSha = blobDAO.setBlobs(session, blobList, new FileHasher());

      Commit.Builder builder = Commit.newBuilder();
      if (!datasetVersion.getParentId().isEmpty()) {
        builder.addParentShas(datasetVersion.getParentId());
      }
      builder.setDateCreated(datasetVersion.getTimeLogged());
      builder.setDateUpdated(datasetVersion.getTimeUpdated());
      Commit commit = builder.build();

      if (!repositoryEntity.isDataset()) {
        throw new ModelDBException(
            "Repository should be created from Dataset to add Dataset Version to it",
            Status.Code.INVALID_ARGUMENT);
      }

      CommitEntity commitEntity =
          saveCommitEntity(session, commit, rootSha, datasetVersion.getOwner(), repositoryEntity);
      blobDAO.setBlobsAttributes(
          session, repositoryEntity.getId(), commitEntity.getCommit_hash(), blobList);
      String compositeId =
          VersioningUtils.getVersioningCompositeId(
              repositoryEntity.getId(), commitEntity.getCommit_hash(), location);
      metadataDAO.addProperty(
          session,
          IdentificationType.newBuilder()
              .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB)
              .setStringId(compositeId)
              .build(),
          "description",
          datasetVersion.getDescription());
      metadataDAO.addLabels(
          session,
          IdentificationType.newBuilder()
              .setStringId(compositeId)
              .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB)
              .build(),
          datasetVersion.getTagsList());
      session.getTransaction().commit();
      return CreateCommitRequest.Response.newBuilder()
          .setCommit(commitEntity.toCommitProto())
          .build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setCommitFromDatasetVersion(datasetVersion, blobDAO, metadataDAO, repositoryEntity);
      } else {
        throw ex;
      }
    }
  }

  private PathDatasetComponentBlob componentFromPart(DatasetPartInfo part) {
    return PathDatasetComponentBlob.newBuilder()
        .setPath(part.getPath())
        .setSize(part.getSize())
        .setLastModifiedAtSource(part.getLastModifiedAtSource())
        .setMd5(part.getChecksum())
        .build();
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

    Commit internalCommit =
        Commit.newBuilder()
            .setDateCreated(timeCreated)
            .setDateUpdated(timeCreated)
            .setAuthor(author)
            .setMessage(commit.getMessage())
            .setCommitSha(generateCommitSHA(rootSha, commit, timeCreated))
            .build();
    CommitEntity commitEntity =
        new CommitEntity(
            repositoryEntity,
            new ArrayList<>(parentCommitEntities.values()),
            internalCommit,
            rootSha);
    session.saveOrUpdate(commitEntity);
    return commitEntity;
  }

  public CommitPaginationDTO fetchCommitEntityList(
      Session session, ListCommitsRequest request, Long repoId) throws ModelDBException {
    StringBuilder commitQueryBuilder =
        new StringBuilder(
            " FROM "
                + CommitEntity.class.getSimpleName()
                + " cm INNER JOIN cm.repository repo WHERE repo.id = :repoId ");
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
            "SELECT cm " + commitQueryBuilder.toString() + " ORDER BY cm.date_updated DESC");
    commitEntityQuery.setParameter("repoId", repoId);
    if (request.hasPagination()) {
      int pageLimit = request.getPagination().getPageLimit();
      final int startPosition = (request.getPagination().getPageNumber() - 1) * pageLimit;
      commitEntityQuery.setFirstResult(startPosition);
      commitEntityQuery.setMaxResults(pageLimit);
    }
    List<CommitEntity> commitEntities = commitEntityQuery.list();

    Query countQuery = session.createQuery("SELECT count(cm) " + commitQueryBuilder.toString());
    countQuery.setParameter("repoId", repoId);
    Long totalRecords = (long) countQuery.uniqueResult();

    CommitPaginationDTO commitPaginationDTO = new CommitPaginationDTO();
    commitPaginationDTO.setCommitEntities(commitEntities);
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
  public boolean deleteCommits(
      RepositoryIdentification repositoryIdentification,
      List<String> commitShas,
      RepositoryDAO repositoryDAO)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repositoryEntity =
          repositoryDAO.getRepositoryById(session, repositoryIdentification, true);

      Query<CommitEntity> getCommitQuery =
          session.createQuery(
              "From "
                  + CommitEntity.class.getSimpleName()
                  + " c WHERE c.commit_hash IN (:commitHashes)",
              CommitEntity.class);
      getCommitQuery.setParameter("commitHashes", commitShas);
      List<CommitEntity> commitEntities = getCommitQuery.getResultList();

      for (CommitEntity commitEntity : commitEntities) {
        if (!commitEntity.getChild_commits().isEmpty()) {
          throw new ModelDBException(
              "Commit has the child, please delete child commit first", Code.FAILED_PRECONDITION);
        }
      }

      String getBranchByCommitHQLBuilder =
          "FROM "
              + BranchEntity.class.getSimpleName()
              + " br where br.id.repository_id = :repositoryId "
              + " AND br.commit_hash IN (:commitHashes) ";
      Query<BranchEntity> getBranchByCommitQuery =
          session.createQuery(getBranchByCommitHQLBuilder, BranchEntity.class);
      getBranchByCommitQuery.setParameter("repositoryId", repositoryEntity.getId());
      getBranchByCommitQuery.setParameter("commitHashes", commitShas);
      List<BranchEntity> branchEntities = getBranchByCommitQuery.list();
      if (branchEntities != null && !branchEntities.isEmpty()) {
        StringBuilder errorMessage =
            new StringBuilder("Commits are associated with branch name : ");
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

      String getTagsHql =
          "From TagsEntity te where te.id."
              + ModelDBConstants.REPOSITORY_ID
              + " = :repoId "
              + " AND te.commit_hash"
              + " IN (:commitHashes)";
      Query<TagsEntity> getTagsQuery = session.createQuery(getTagsHql, TagsEntity.class);
      getTagsQuery.setParameter("repoId", repositoryEntity.getId());
      getTagsQuery.setParameter("commitHash", commitShas);
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
      String getLabelsHql =
          "From LabelsMappingEntity lm where lm.id."
              + ModelDBConstants.ENTITY_HASH
              + " IN (:entityHashes) "
              + " AND lm.id."
              + ModelDBConstants.ENTITY_TYPE
              + " = :entityType";
      Query<LabelsMappingEntity> query =
          session.createQuery(getLabelsHql, LabelsMappingEntity.class);
      query.setParameter("entityHashes", commitShas);
      query.setParameter("entityType", IDTypeEnum.IDType.VERSIONING_COMMIT_VALUE);
      List<LabelsMappingEntity> labelsMappingEntities = query.list();
      for (LabelsMappingEntity labelsMappingEntity : labelsMappingEntities) {
        session.delete(labelsMappingEntity);
      }

      commitEntities.forEach(
          (commitEntity) -> {
            if (commitEntity.getRepository().size() == 1) {
              session.delete(commitEntity);
            } else {
              commitEntity.getRepository().remove(repositoryEntity);
              session.update(commitEntity);
            }
          });
      session.getTransaction().commit();
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteCommits(repositoryIdentification, commitShas, repositoryDAO);
      } else {
        throw ex;
      }
    }
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

      String getLabelsHql =
          new StringBuilder("From LabelsMappingEntity lm where lm.id.")
              .append(ModelDBConstants.ENTITY_HASH)
              .append(" = :entityHash ")
              .append(" AND lm.id.")
              .append(ModelDBConstants.ENTITY_TYPE)
              .append(" = :entityType")
              .toString();
      Query<LabelsMappingEntity> query =
          session.createQuery(getLabelsHql, LabelsMappingEntity.class);
      query.setParameter("entityHash", commitEntity.getCommit_hash());
      query.setParameter("entityType", IDTypeEnum.IDType.VERSIONING_COMMIT_VALUE);
      List<LabelsMappingEntity> labelsMappingEntities = query.list();
      for (LabelsMappingEntity labelsMappingEntity : labelsMappingEntities) {
        session.delete(labelsMappingEntity);
      }

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

  @Override
  public void addDeleteDatasetVersionTags(
      MetadataDAO metadataDAO,
      boolean addTags,
      RepositoryEntity repositoryEntity,
      String datasetVersionId,
      List<String> tagsList,
      boolean deleteAll)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      CommitEntity commitEntity =
          getCommitEntity(session, datasetVersionId, (session1 -> repositoryEntity));

      String compositeId =
          VersioningUtils.getVersioningCompositeId(
              repositoryEntity.getId(),
              commitEntity.getCommit_hash(),
              Collections.singletonList(ModelDBConstants.DEFAULT_VERSIONING_BLOB_LOCATION));

      IdentificationType identificationType =
          IdentificationType.newBuilder()
              .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB)
              .setStringId(compositeId)
              .build();
      if (addTags) {
        metadataDAO.addLabels(identificationType, ModelDBUtils.checkEntityTagsLength(tagsList));
      } else {
        metadataDAO.deleteLabels(
            identificationType, ModelDBUtils.checkEntityTagsLength(tagsList), deleteAll);
      }
      commitEntity.setDate_updated(new Date().getTime());
      session.update(commitEntity);
      session.getTransaction().commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        addDeleteDatasetVersionTags(
            metadataDAO, addTags, repositoryEntity, datasetVersionId, tagsList, deleteAll);
      } else {
        throw ex;
      }
    }
  }
}
