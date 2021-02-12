package ai.verta.modeldb.batchProcess;

import static ai.verta.modeldb.versioning.RepositoryDAORdbImpl.CHECK_BRANCH_IN_REPOSITORY_HQL;

import ai.verta.modeldb.DatasetPartInfo;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.PathDatasetVersionInfo;
import ai.verta.modeldb.PathLocationTypeEnum;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.metadata.IdentificationType;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.metadata.MetadataDAORdbImpl;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.BlobDAORdbImpl;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.Commit;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.CommitDAORdbImpl;
import ai.verta.modeldb.versioning.CreateCommitRequest;
import ai.verta.modeldb.versioning.DatasetBlob;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.ListCommitsRequest;
import ai.verta.modeldb.versioning.PathDatasetBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryDAORdbImpl;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.S3DatasetBlob;
import ai.verta.modeldb.versioning.S3DatasetComponentBlob;
import ai.verta.modeldb.versioning.VersioningUtils;
import ai.verta.modeldb.versioning.blob.container.BlobContainer;
import io.grpc.Status;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class BasePathDatasetVersionMigration {
  private static final Logger LOGGER = LogManager.getLogger(BasePathDatasetVersionMigration.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private static AuthService authService;
  private static RoleService roleService;
  private static MetadataDAO metadataDAO;
  private static CommitDAO commitDAO;
  private static RepositoryDAO repositoryDAO;
  private static BlobDAO blobDAO;

  private BasePathDatasetVersionMigration() {}

  public static void execute(List<Long> datasetIds) {
    if (Config.getInstance().hasAuth()) {
      authService = AuthServiceUtils.FromConfig(Config.getInstance());
      roleService = RoleServiceUtils.FromConfig(Config.getInstance(), authService);
    } else {
      LOGGER.debug("AuthService Host & Port not found, OSS setup found");
      return;
    }

    metadataDAO = new MetadataDAORdbImpl();
    commitDAO = new CommitDAORdbImpl(authService, roleService);
    repositoryDAO = new RepositoryDAORdbImpl(authService, roleService, commitDAO, metadataDAO);
    blobDAO = new BlobDAORdbImpl(authService, roleService);

    LOGGER.info("Migration start");
    try {
      CommonUtils.registeredBackgroundUtilsCount();
      for (Long datasetId : datasetIds) {
        LOGGER.info("Dataset {} migration start", datasetId);
        try {
          migrateDatasetVersionBasePath(datasetId);
        } catch (NoSuchAlgorithmException e) {
          throw new ModelDBException(e);
        }
        LOGGER.info("Dataset {} base path migration done", datasetId);
      }
    } finally {
      CommonUtils.unregisteredBackgroundUtilsCount();
    }
    LOGGER.info("Migration End");
  }

  private static void migrateDatasetVersionBasePath(Long datasetId)
      throws NoSuchAlgorithmException {
    LOGGER.debug("DatasetVersion base path migration started");
    RepositoryIdentification repositoryIdentification =
        RepositoryIdentification.newBuilder().setRepoId(datasetId).build();

    RepositoryEntity repositoryEntity =
        repositoryDAO.getProtectedRepositoryById(repositoryIdentification, false);

    ListCommitsRequest.Builder listCommitsRequest =
        ListCommitsRequest.newBuilder().setRepositoryId(repositoryIdentification);
    ListCommitsRequest.Response listCommitsResponse =
        commitDAO.listCommits(listCommitsRequest.build(), (session -> repositoryEntity), true);

    long totalRecords = listCommitsResponse.getTotalRecords();
    totalRecords = totalRecords > 0 ? totalRecords - 1 : totalRecords;
    LOGGER.debug(
        "Total DatasetVersion to migrate for dataset : {}, Count: {}", datasetId, totalRecords);

    List<DatasetVersion> datasetVersions =
        new ArrayList<>(
            convertRepoDatasetVersions(repositoryEntity, listCommitsResponse.getCommitsList()));

    for (DatasetVersion datasetVersion : datasetVersions) {
      try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        String datasetVersionId = datasetVersion.getId();
        LOGGER.debug(
            "Dataset version base migration started for datasetVersion ID: {}, Version: {}",
            datasetVersionId,
            datasetVersion.getVersion());
        Transaction transaction = session.beginTransaction();
        CreateCommitRequest.Response createCommitResponse =
            setCommitFromDatasetVersion(
                session, datasetVersion, blobDAO, metadataDAO, repositoryEntity);
        String newDatasetVersionId = createCommitResponse.getCommit().getCommitSha();
        if (!datasetVersionId.equals(newDatasetVersionId)) {
          updateLinkedArtifactInExperimentRun(session, datasetVersionId, newDatasetVersionId);

          deleteDatasetVersions(
              session, repositoryEntity, Collections.singletonList(datasetVersion.getId()));
        }
        transaction.commit();
        LOGGER.debug(
            "Dataset version base migration finish for datasetVersion ID: {}, Version: {}, new Id: {}",
            datasetId,
            datasetVersion.getVersion(),
            newDatasetVersionId);
      }
    }

    LOGGER.debug("DatasetVersion base path migration finished");
  }

  private static void updateLinkedArtifactInExperimentRun(
      Session session, String datasetVersionId, String newDatasetVersionId) {
    String updateDatasetsLinkedArtifactId =
        "UPDATE ArtifactEntity ar SET ar.linked_artifact_id = :newDatasetVersionId WHERE ar.linked_artifact_id = :datasetVersionId";
    Query linkedArtifactQuery = session.createQuery(updateDatasetsLinkedArtifactId);
    linkedArtifactQuery.setParameter("newDatasetVersionId", newDatasetVersionId);
    linkedArtifactQuery.setParameter("datasetVersionId", datasetVersionId);
    int updateCount = linkedArtifactQuery.executeUpdate();
    if (updateCount != 0) {
      LOGGER.debug(
          "Updated linked artifact id from {}  to {}", datasetVersionId, newDatasetVersionId);
    }
  }

  private static void deleteDatasetVersions(
      Session session, RepositoryEntity repositoryEntity, List<String> datasetVersionIds)
      throws ModelDBException {
    for (String datasetVersionId : datasetVersionIds) {
      Query<CommitEntity> getCommitQuery =
          session.createQuery(
              "From " + CommitEntity.class.getSimpleName() + " c WHERE c.commit_hash = :commitHash",
              CommitEntity.class);
      getCommitQuery.setParameter("commitHash", datasetVersionId);
      CommitEntity commitEntity = getCommitQuery.uniqueResult();
      if (commitEntity == null || commitEntity.getParent_commits().isEmpty()) {
        LOGGER.warn(
            "skipping deleting commit corresponding to dataset version {}", datasetVersionId);
        continue;
      }

      if (commitEntity.getRepository() != null && commitEntity.getRepository().size() > 1) {
        throw new ModelDBException(
            "DatasetVersion '"
                + commitEntity.getCommit_hash()
                + "' associated with multiple datasets",
            Status.Code.INTERNAL);
      } else if (commitEntity.getRepository() == null) {
        throw new ModelDBException(
            "DatasetVersion not associated with datasets", Status.Code.INTERNAL);
      }

      Query query = session.createQuery(CHECK_BRANCH_IN_REPOSITORY_HQL);
      query.setParameter("repositoryId", repositoryEntity.getId());
      query.setParameter("branch", ModelDBConstants.MASTER_BRANCH);
      BranchEntity branchEntity = (BranchEntity) query.uniqueResult();

      CommitEntity parentDatasetVersion = commitEntity.getParent_commits().get(0);

      if (branchEntity != null
          && branchEntity.getCommit_hash().equals(commitEntity.getCommit_hash())) {
        saveBranch(
            session,
            parentDatasetVersion.getCommit_hash(),
            ModelDBConstants.MASTER_BRANCH,
            repositoryEntity);
      }

      if (!commitEntity.getChild_commits().isEmpty()) {
        CommitEntity childCommit = new ArrayList<>(commitEntity.getChild_commits()).get(0);
        String updateChildEntity =
            "UPDATE commit_parent SET parent_hash = :parentHash WHERE child_hash = :childHash";
        Query updateChildQuery = session.createSQLQuery(updateChildEntity);
        updateChildQuery.setParameter("parentHash", parentDatasetVersion.getCommit_hash());
        updateChildQuery.setParameter("childHash", childCommit.getCommit_hash());
        updateChildQuery.executeUpdate();
      }

      String compositeId =
          VersioningUtils.getVersioningCompositeId(
              repositoryEntity.getId(),
              commitEntity.getCommit_hash(),
              Collections.singletonList(ModelDBConstants.DEFAULT_VERSIONING_BLOB_LOCATION));
      DeleteEntitiesCron.deleteLabels(
          session, compositeId, IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB);
      DeleteEntitiesCron.deleteAttribute(session, compositeId);
      session.delete(commitEntity);
    }
  }

  private static void saveBranch(
      Session session, String commitSHA, String branch, RepositoryEntity repository)
      throws ModelDBException {
    ModelDBUtils.validateEntityNameWithColonAndSlash(branch);
    boolean exists =
        VersioningUtils.commitRepositoryMappingExists(session, commitSHA, repository.getId());
    if (!exists) {
      throw new ModelDBException(
          "Commit_hash and repository_id mapping not found for repository "
              + repository.getId()
              + " and commit "
              + commitSHA,
          Status.Code.NOT_FOUND);
    }

    Query query =
        session
            .createQuery(CHECK_BRANCH_IN_REPOSITORY_HQL)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    query.setParameter("repositoryId", repository.getId());
    query.setParameter("branch", branch);
    BranchEntity branchEntity = (BranchEntity) query.uniqueResult();
    if (branchEntity != null) {
      if (branchEntity.getCommit_hash().equals(commitSHA)) return;
      session.delete(branchEntity);
    }

    branchEntity = new BranchEntity(repository.getId(), commitSHA, branch);
    session.save(branchEntity);
  }

  private static CreateCommitRequest.Response setCommitFromDatasetVersion(
      Session session,
      DatasetVersion datasetVersion,
      BlobDAO blobDAO,
      MetadataDAO metadataDAO,
      RepositoryEntity repositoryEntity)
      throws ModelDBException, NoSuchAlgorithmException {
    Blob.Builder blobBuilder = Blob.newBuilder();
    DatasetBlob.Builder datasetBlobBuilder = DatasetBlob.newBuilder();

    if (datasetVersion.hasDatasetBlob()) {
      datasetBlobBuilder = datasetVersion.getDatasetBlob().toBuilder();
      switch (datasetBlobBuilder.getContentCase()) {
        case PATH:
          PathDatasetBlob pathDatasetBlob = datasetBlobBuilder.getPath();
          List<PathDatasetComponentBlob> datasetComponentBlobs =
              pathDatasetBlob.getComponentsList();
          List<PathDatasetComponentBlob> convertedDatasetComponentBlobs =
              datasetComponentBlobs.stream()
                  .map(BasePathDatasetVersionMigration::populateBasePathInComponentPath)
                  .collect(Collectors.toList());
          pathDatasetBlob =
              pathDatasetBlob
                  .toBuilder()
                  .clearComponents()
                  .addAllComponents(convertedDatasetComponentBlobs)
                  .build();
          datasetBlobBuilder.setPath(pathDatasetBlob);
          break;
        case S3:
          S3DatasetBlob s3DatasetBlob = datasetBlobBuilder.getS3();
          List<S3DatasetComponentBlob> s3DatasetComponentBlobs =
              s3DatasetBlob.getComponentsList().stream()
                  .map(
                      s3DatasetComponentBlob ->
                          s3DatasetComponentBlob
                              .toBuilder()
                              .setPath(
                                  populateBasePathInComponentPath(s3DatasetComponentBlob.getPath()))
                              .build())
                  .collect(Collectors.toList());
          s3DatasetBlob =
              s3DatasetBlob
                  .toBuilder()
                  .clearComponents()
                  .addAllComponents(s3DatasetComponentBlobs)
                  .build();
          datasetBlobBuilder.setS3(s3DatasetBlob);
          break;
      }
    } else {
      switch (datasetVersion.getDatasetVersionInfoCase()) {
        case PATH_DATASET_VERSION_INFO:
          PathDatasetVersionInfo pathDatasetVersionInfo =
              datasetVersion.getPathDatasetVersionInfo();
          List<DatasetPartInfo> partInfos = pathDatasetVersionInfo.getDatasetPartInfosList();
          Stream<PathDatasetComponentBlob> result =
              partInfos.stream()
                  .map(
                      datasetPartInfo ->
                          componentFromPart(datasetPartInfo, pathDatasetVersionInfo.getBasePath()));
          if (pathDatasetVersionInfo.getLocationType()
              == PathLocationTypeEnum.PathLocationType.S3_FILE_SYSTEM) {
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
          throw new ModelDBException("Wrong dataset version type", Status.Code.INVALID_ARGUMENT);
      }
    }
    blobBuilder.setDataset(datasetBlobBuilder);
    List<String> location =
        Collections.singletonList(ModelDBConstants.DEFAULT_VERSIONING_BLOB_LOCATION);
    List<BlobContainer> blobList =
        Collections.singletonList(
            BlobContainer.create(
                BlobExpanded.newBuilder()
                    .addAllLocation(location)
                    .setBlob(blobBuilder.build())
                    .addAllAttributes(datasetVersion.getAttributesList())
                    .build()));

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
        commitDAO.saveCommitEntity(
            session, commit, rootSha, datasetVersion.getOwner(), repositoryEntity);
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

    metadataDAO.addProperty(
        session,
        IdentificationType.newBuilder()
            .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB)
            .setStringId(compositeId)
            .build(),
        ModelDBConstants.VERSION,
        String.valueOf(datasetVersion.getVersion()));

    saveBranch(
        session, commitEntity.getCommit_hash(), ModelDBConstants.MASTER_BRANCH, repositoryEntity);

    return CreateCommitRequest.Response.newBuilder()
        .setCommit(commitEntity.toCommitProto())
        .build();
  }

  private static PathDatasetComponentBlob populateBasePathInComponentPath(
      PathDatasetComponentBlob part) {
    String componentPath = part.getPath();
    if (!part.getBasePath().isEmpty() && !part.getPath().contains(part.getBasePath())) {
      componentPath = part.getBasePath() + componentPath;
    }
    return part.toBuilder().setPath(componentPath).setBasePath("").build();
  }

  private static PathDatasetComponentBlob componentFromPart(DatasetPartInfo part, String basePath) {
    String componentPath = part.getPath();
    if (!basePath.isEmpty() && !part.getPath().contains(basePath)) {
      componentPath = basePath + componentPath;
    }
    return PathDatasetComponentBlob.newBuilder()
        .setPath(componentPath)
        .setSize(part.getSize())
        .setLastModifiedAtSource(part.getLastModifiedAtSource())
        .setMd5(part.getChecksum())
        .setBasePath("")
        .build();
  }

  private static List<DatasetVersion> convertRepoDatasetVersions(
      RepositoryEntity repositoryEntity, List<Commit> commitList) throws ModelDBException {
    List<DatasetVersion> datasetVersions = new ArrayList<>();
    for (Commit commit : commitList) {
      if (commit.getParentShasList().isEmpty()) {
        continue;
      }
      datasetVersions.add(
          blobDAO.convertToDatasetVersion(
              repositoryDAO, metadataDAO, repositoryEntity, commit.getCommitSha(), false));
    }
    return datasetVersions;
  }
}
