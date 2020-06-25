package ai.verta.modeldb.versioning;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.App;
import ai.verta.modeldb.DatasetPartInfo;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.KeyValueQuery;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.OperatorEnum;
import ai.verta.modeldb.PathDatasetVersionInfo;
import ai.verta.modeldb.PathLocationTypeEnum.PathLocationType;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.dto.CommitPaginationDTO;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.TagsEntity;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.metadata.IdentificationType;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.blob.container.BlobContainer;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.Value;
import io.grpc.Status;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class CommitDAORdbImpl implements CommitDAO {

  private static final Logger LOGGER = LogManager.getLogger(CommitDAORdbImpl.class);
  private final AuthService authService;
  private final RoleService roleService;

  public CommitDAORdbImpl(AuthService authService, RoleService roleService) {
    this.authService = authService;
    this.roleService = roleService;
  }

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
        default:
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
          session, repositoryEntity.getId(), commitEntity.getCommit_hash(), blobList, true);
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
      RepositoryDAO repositoryDAO,
      boolean isDatasetVersion)
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

      if (isDatasetVersion) {
        for (BranchEntity branchEntity : branchEntities) {
          session.delete(branchEntity);
        }
      } else {
        for (CommitEntity commitEntity : commitEntities) {
          if (!commitEntity.getChild_commits().isEmpty()) {
            throw new ModelDBException(
                "Commit '"
                    + commitEntity.getCommit_hash()
                    + "' has the child, please delete child commit first",
                Code.FAILED_PRECONDITION);
          }
        }

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
        getTagsQuery.setParameter("commitHashes", commitShas);
        List<TagsEntity> tagsEntities = getTagsQuery.list();
        if (tagsEntities.size() > 0) {
          throw new ModelDBException(
              "Commit is associated with Tags : "
                  + tagsEntities.stream()
                      .map(tagsEntity -> tagsEntity.getId().getTag())
                      .collect(Collectors.joining(",")),
              Code.FAILED_PRECONDITION);
        }
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
      query.setParameter("entityType", IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_VALUE);
      List<LabelsMappingEntity> labelsMappingEntities = query.list();
      for (LabelsMappingEntity labelsMappingEntity : labelsMappingEntities) {
        session.delete(labelsMappingEntity);
      }

      commitEntities.forEach(
          (commitEntity) -> {
            if (commitEntity.getRepository().size() == 1) {
              String compositeId =
                  VersioningUtils.getVersioningCompositeId(
                      repositoryEntity.getId(),
                      commitEntity.getCommit_hash(),
                      Collections.singletonList(ModelDBConstants.DEFAULT_VERSIONING_BLOB_LOCATION));
              DeleteEntitiesCron.deleteLabels(
                  session, compositeId, IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB);
              DeleteEntitiesCron.deleteAttribute(session, compositeId);
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
        return deleteCommits(repositoryIdentification, commitShas, repositoryDAO, isDatasetVersion);
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

  @Override
  public CommitPaginationDTO findCommits(
      FindRepositoriesBlobs request,
      UserInfo currentLoginUserInfo,
      boolean idsOnly,
      boolean rootSHAOnly)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      CommitPaginationDTO commitPaginationDTO =
          findCommits(session, request, currentLoginUserInfo, idsOnly, rootSHAOnly);
      commitPaginationDTO.setCommits(
          commitPaginationDTO.getCommitEntities().stream()
              .map(CommitEntity::toCommitProto)
              .collect(Collectors.toList()));
      return commitPaginationDTO;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findCommits(request, currentLoginUserInfo, idsOnly, rootSHAOnly);
      } else {
        throw ex;
      }
    }
  }

  /**
   * This method find the blobs supported based on the following conditions
   *
   * <p>commit.author, commit.label, tags, repoIds, commitHashList
   *
   * @param session :hibernate session
   * @param request : FindRepositoriesBlobs request
   * @param currentLoginUserInfo : current login userInfo
   * @return {@link CommitPaginationDTO} : "result", "count" as a key
   */
  @Override
  public CommitPaginationDTO findCommits(
      Session session,
      FindRepositoriesBlobs request,
      UserInfo currentLoginUserInfo,
      boolean idsOnly,
      boolean rootSHAOnly)
      throws ModelDBException {
    try {
      List<KeyValueQuery> predicates = new ArrayList<>(request.getPredicatesList());
      for (KeyValueQuery predicate : predicates) {
        Value.KindCase predicateCase = predicate.getValue().getKindCase();
        if (predicate.getKey().equals(ModelDBConstants.ID)) {
          throw new ModelDBException(
              "predicates with ids not supported", Status.Code.INVALID_ARGUMENT);
        }
        if (predicate.getKey().isEmpty()) {
          throw new ModelDBException(
              "predicates with empty key not supported", Status.Code.INVALID_ARGUMENT);
        }
        if (predicateCase.equals(Value.KindCase.STRING_VALUE)
            && predicate.getValue().getStringValue().isEmpty()) {
          throw new ModelDBException(
              "Predicate does not contain string value in request", Status.Code.INVALID_ARGUMENT);
        }
        if (!predicateCase.equals(Value.KindCase.STRING_VALUE)
            && !predicateCase.equals(Value.KindCase.NUMBER_VALUE)
            && !predicateCase.equals(Value.KindCase.BOOL_VALUE)) {
          throw new ModelDBException(
              "Unknown 'Value' type recognized, valid 'Value' type are NUMBER_VALUE, STRING_VALUE, BOOL_VALUE",
              Status.Code.UNIMPLEMENTED);
        }

        if (predicate.getKey().equalsIgnoreCase(ModelDBConstants.WORKSPACE)
            || predicate.getKey().equalsIgnoreCase(ModelDBConstants.WORKSPACE_ID)
            || predicate.getKey().equalsIgnoreCase(ModelDBConstants.WORKSPACE_NAME)
            || predicate.getKey().equalsIgnoreCase(ModelDBConstants.WORKSPACE_TYPE)) {
          throw new ModelDBException(
              "Workspace name OR type not supported as predicate", Status.Code.INVALID_ARGUMENT);
        }
      }

      WorkspaceDTO workspaceDTO =
          roleService.getWorkspaceDTOByWorkspaceName(
              currentLoginUserInfo, request.getWorkspaceName());

      List<String> accessibleResourceIds =
          roleService.getAccessibleResourceIds(
              null,
              new CollaboratorUser(authService, currentLoginUserInfo),
              RepositoryVisibilityEnum.RepositoryVisibility.PRIVATE,
              ModelDBResourceEnum.ModelDBServiceResourceTypes.REPOSITORY,
              request.getRepoIdsList().stream().map(String::valueOf).collect(Collectors.toList()));

      Set<String> commitHashList = new HashSet<>(request.getCommitsList());

      Map<String, Object> parametersMap = new HashMap<>();

      String alias = "cm";
      StringBuilder rootQueryStringBuilder =
          new StringBuilder(" FROM ")
              .append(CommitEntity.class.getSimpleName())
              .append(" ")
              .append(alias)
              .append(" ");

      StringBuilder joinClause = new StringBuilder();
      String repoAlias = "repo";
      joinClause.append(" INNER JOIN ").append(alias).append(".repository ").append(repoAlias);
      joinClause
          .append(" INNER JOIN ")
          .append(InternalFolderElementEntity.class.getSimpleName())
          .append(" folderElm ")
          .append(" ON ");
      joinClause.append("folderElm.folder_hash = ").append(alias).append(".rootSha ");

      List<String> whereClauseList = new ArrayList<>();
      if (!predicates.isEmpty()) {
        for (int index = 0; index < predicates.size(); index++) {
          KeyValueQuery predicate = predicates.get(index);
          String[] names = predicate.getKey().split("\\.");
          switch (names[0].toLowerCase()) {
            case ModelDBConstants.COMMIT:
              LOGGER.debug("switch case : commit");
              if (names[1].contains(ModelDBConstants.LABEL)) {
                StringBuilder subQueryBuilder =
                    new StringBuilder("SELECT lb.id.entity_hash FROM ")
                        .append(LabelsMappingEntity.class.getSimpleName())
                        .append(" lb WHERE ")
                        .append(" lb.id.entity_type ");
                VersioningUtils.setValueWithOperatorInQuery(
                    index,
                    subQueryBuilder,
                    OperatorEnum.Operator.EQ,
                    IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_VALUE,
                    parametersMap);
                subQueryBuilder.append(" AND lb.id.label ");
                VersioningUtils.setValueWithOperatorInQuery(
                    index,
                    subQueryBuilder,
                    OperatorEnum.Operator.EQ,
                    predicate.getValue().getStringValue(),
                    parametersMap);
                whereClauseList.add(
                    alias + ".commit_hash IN (" + subQueryBuilder.toString() + ") ");
              } else if (names[1].toLowerCase().equals("author")) {
                StringBuilder authorBuilder = new StringBuilder(alias + "." + names[1]);
                VersioningUtils.setQueryParameters(index, authorBuilder, predicate, parametersMap);
                whereClauseList.add(authorBuilder.toString());
              } else {
                throw new ModelDBException(
                    "Given predicate not supported yet : " + predicate, Code.UNIMPLEMENTED);
              }
              break;
            case ModelDBConstants.ATTRIBUTES:
              Map<String, Object> attrQueryParametersMap = new HashMap<>();
              StringBuilder attrQueryBuilder =
                  new StringBuilder(
                          "SELECT attr.entity_hash From "
                              + AttributeEntity.class.getSimpleName()
                              + " attr where attr.")
                      .append(ModelDBConstants.KEY);
              VersioningUtils.setValueWithOperatorInQuery(
                  index,
                  attrQueryBuilder,
                  OperatorEnum.Operator.EQ,
                  names[1],
                  attrQueryParametersMap);
              attrQueryBuilder.append("AND attr.value ");
              VersioningUtils.setValueWithOperatorInQuery(
                  index,
                  attrQueryBuilder,
                  predicate.getOperator(),
                  ModelDBUtils.getStringFromProtoObject(predicate.getValue()),
                  attrQueryParametersMap);
              attrQueryBuilder.append("AND attr.field_type ");
              VersioningUtils.setValueWithOperatorInQuery(
                  index,
                  attrQueryBuilder,
                  OperatorEnum.Operator.EQ,
                  ModelDBConstants.ATTRIBUTES,
                  attrQueryParametersMap);
              attrQueryBuilder.append("AND attr.entity_name ");
              VersioningUtils.setValueWithOperatorInQuery(
                  index,
                  attrQueryBuilder,
                  OperatorEnum.Operator.EQ,
                  ModelDBConstants.BLOB,
                  attrQueryParametersMap);

              Query attrQuery = session.createQuery(attrQueryBuilder.toString());
              attrQueryParametersMap.forEach(attrQuery::setParameter);
              List<String> attrEntityHashes = attrQuery.list();
              Set<String> attrCommitHashes = new HashSet<>();
              attrEntityHashes.forEach(
                  blobHash -> {
                    String[] compositeIdArr =
                        VersioningUtils.getDatasetVersionBlobCompositeIdString(blobHash);
                    attrCommitHashes.add(compositeIdArr[1]);
                  });
              if (!attrCommitHashes.isEmpty()) {
                whereClauseList.add(alias + ".commit_hash IN (:attr_" + index + "_CommitHashes)");
                parametersMap.put("attr_" + index + "_CommitHashes", attrCommitHashes);
              }
              break;
            case ModelDBConstants.TAGS:
            case ModelDBConstants.BLOB:
              LOGGER.debug("switch case : Blob");
              StringBuilder subQueryBuilder =
                  new StringBuilder("SELECT lb.id.entity_hash FROM ")
                      .append(LabelsMappingEntity.class.getSimpleName())
                      .append(" lb WHERE ")
                      .append(" lb.id.entity_type = :entityType")
                      .append(" AND lb.id.label ");
              Map<String, Object> innerQueryParametersMap = new HashMap<>();
              VersioningUtils.setValueWithOperatorInQuery(
                  index,
                  subQueryBuilder,
                  predicate.getOperator(),
                  predicate.getValue().getStringValue(),
                  innerQueryParametersMap);
              Query labelQuery = session.createQuery(subQueryBuilder.toString());
              labelQuery.setParameter(
                  "entityType", IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB_VALUE);
              innerQueryParametersMap.forEach(labelQuery::setParameter);
              List<String> blobHashes = labelQuery.list();
              Set<String> commitHashes = new HashSet<>();
              blobHashes.forEach(
                  blobHash -> {
                    String[] compositeIdArr =
                        VersioningUtils.getDatasetVersionBlobCompositeIdString(blobHash);
                    commitHashes.add(compositeIdArr[1]);
                  });
              if (!commitHashes.isEmpty()) {
                whereClauseList.add(alias + ".commit_hash IN (:label_" + index + "_CommitHashes)");
                parametersMap.put("label_" + index + "_CommitHashes", commitHashes);
              }
              break;
            default:
              throw new ModelDBException(
                  "Invalid predicate found : " + predicate, Code.INVALID_ARGUMENT);
          }
        }
      }

      if (workspaceDTO != null
          && workspaceDTO.getWorkspaceId() != null
          && !workspaceDTO.getWorkspaceId().isEmpty()) {
        whereClauseList.add(
            repoAlias
                + "."
                + ModelDBConstants.WORKSPACE_ID
                + " = :"
                + ModelDBConstants.WORKSPACE_ID);
        parametersMap.put(ModelDBConstants.WORKSPACE_ID, workspaceDTO.getWorkspaceId());
        whereClauseList.add(
            repoAlias
                + "."
                + ModelDBConstants.WORKSPACE_TYPE
                + " = :"
                + ModelDBConstants.WORKSPACE_TYPE);
        parametersMap.put(
            ModelDBConstants.WORKSPACE_TYPE, workspaceDTO.getWorkspaceType().getNumber());
      }

      if (!accessibleResourceIds.isEmpty()) {
        whereClauseList.add(repoAlias + ".id IN (:repoIds) ");
        parametersMap.put(
            "repoIds",
            accessibleResourceIds.stream().map(Long::valueOf).collect(Collectors.toList()));
      } else if (roleService.IsImplemented()) {
        String errorMessage =
            "Access is denied. User is unauthorized for given resource IDs : "
                + accessibleResourceIds;
        ModelDBUtils.logAndThrowError(
            errorMessage,
            com.google.rpc.Code.PERMISSION_DENIED_VALUE,
            Any.pack(FindRepositoriesBlobs.getDefaultInstance()));
      }

      if (!commitHashList.isEmpty()) {
        whereClauseList.add(alias + ".commit_hash IN (:commitHashList)");
        parametersMap.put("commitHashList", commitHashList);
      }

      StringBuilder whereClause = new StringBuilder();
      whereClause.append(
          VersioningUtils.setPredicatesWithQueryOperator(
              " AND ", whereClauseList.toArray(new String[0])));

      // Order by clause
      StringBuilder orderClause =
          new StringBuilder(" ORDER BY ")
              .append(alias)
              .append(".")
              .append(ModelDBConstants.DATE_UPDATED)
              .append(" DESC");

      StringBuilder finalQueryBuilder = new StringBuilder();
      if (idsOnly) {
        finalQueryBuilder.append("SELECT ").append(alias).append(".commit_hash ");
      } else if (rootSHAOnly) {
        finalQueryBuilder.append("SELECT ").append(alias).append(".rootSha ");
      } else {
        finalQueryBuilder.append("SELECT ").append(alias).append(" ");
      }
      finalQueryBuilder.append(rootQueryStringBuilder);
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
      countQueryBuilder.append(rootQueryStringBuilder);
      countQueryBuilder.append(joinClause);
      if (!whereClause.toString().isEmpty()) {
        countQueryBuilder.append(" WHERE ").append(whereClause);
      }

      LOGGER.debug("Find Repository blob final query : {}", finalQueryBuilder.toString());
      Query query = session.createQuery(finalQueryBuilder.toString());
      LOGGER.debug("Find Repository blob final query : {}", query.getQueryString());
      Query countQuery = session.createQuery(countQueryBuilder.toString());
      if (!parametersMap.isEmpty()) {
        parametersMap.forEach(
            (key, value) -> {
              if (value instanceof List) {
                List<Object> objectList = (List<Object>) value;
                query.setParameterList(key, objectList);
                countQuery.setParameterList(key, objectList);
              } else {
                query.setParameter(key, value);
                countQuery.setParameter(key, value);
              }
            });
      }

      LOGGER.debug("Final find commit root_sha query : {}", query.getQueryString());
      if (request.getPageNumber() != 0 && request.getPageLimit() != 0) {
        // Calculate number of documents to skip
        int skips = request.getPageLimit() * (request.getPageNumber() - 1);
        query.setFirstResult(skips);
        query.setMaxResults(request.getPageLimit());
      }

      List<CommitEntity> commitEntities;
      if (idsOnly || rootSHAOnly) {
        List<String> resultSet = query.list();
        commitEntities =
            resultSet.stream()
                .map(
                    selectedField -> {
                      CommitEntity commitEntity = new CommitEntity();
                      if (idsOnly) {
                        commitEntity.setCommit_hash(selectedField);
                      } else if (rootSHAOnly) {
                        commitEntity.setRootSha(selectedField);
                      }
                      return commitEntity;
                    })
                .collect(Collectors.toList());
      } else {
        commitEntities = query.list();
      }

      CommitPaginationDTO commitPaginationDTO = new CommitPaginationDTO();
      commitPaginationDTO.setCommitEntities(commitEntities);
      commitPaginationDTO.setTotalRecords((Long) countQuery.uniqueResult());
      return commitPaginationDTO;
    } catch (InvalidProtocolBufferException e) {
      throw new ModelDBException(e);
    }
  }
}
