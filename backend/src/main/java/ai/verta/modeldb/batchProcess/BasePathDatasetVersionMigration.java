package ai.verta.modeldb.batchProcess;

import ai.verta.modeldb.*;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
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
import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.container.BlobContainer;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.verta.modeldb.versioning.RepositoryDAORdbImpl.CHECK_BRANCH_IN_REPOSITORY_HQL;

public class BasePathDatasetVersionMigration {
  private static final Logger LOGGER = LogManager.getLogger(BasePathDatasetVersionMigration.class);
  private static AuthService authService;
  private static RoleService roleService;
  private static MetadataDAO metadataDAO;
  private static CommitDAO commitDAO;
  private static RepositoryDAO repositoryDAO;
  private static BlobDAO blobDAO;

  private BasePathDatasetVersionMigration() {}

  public static void run(AuthService authService, RoleService roleService) throws ModelDBException {
    BasePathDatasetVersionMigration.authService = authService;
    BasePathDatasetVersionMigration.roleService = roleService;
    List<Long> ids = new ArrayList<>();
    ids.add(121001L);
    ids.add(121076L);
    ids.add(121077L);
    ids.add(121078L);
    ids.add(121079L);
    ids.add(121080L);
    ids.add(121081L);
    ids.add(121084L);
    ids.add(121085L);
    ids.add(121086L);
    ids.add(121088L);
    ids.add(121090L);
    ids.add(121091L);
    ids.add(121092L);
    ids.add(121093L);
    ids.add(121094L);
    ids.add(121095L);
    ids.add(121096L);
    ids.add(121097L);
    ids.add(121098L);
    ids.add(121099L);
    ids.add(121100L);
    ids.add(121101L);
    ids.add(121105L);
    ids.add(121106L);
    ids.add(121107L);
    ids.add(121108L);
    ids.add(121109L);
    ids.add(121110L);
    ids.add(121111L);
    ids.add(121112L);
    ids.add(121113L);
    ids.add(121114L);
    ids.add(121116L);
    ids.add(121117L);
    ids.add(121118L);
    ids.add(121119L);
    ids.add(121120L);
    ids.add(121121L);
    ids.add(121122L);
    ids.add(151001L);
    ids.add(151098L);
    ids.add(151099L);
    ids.add(151100L);
    ids.add(151102L);
    ids.add(151104L);
    ids.add(151105L);
    ids.add(151106L);
    ids.add(151109L);
    ids.add(151110L);
    ids.add(151111L);
    ids.add(151112L);
    ids.add(151113L);
    ids.add(151115L);
    ids.add(151116L);
    ids.add(151117L);
    ids.add(151118L);
    ids.add(151119L);
    ids.add(211002L);
    ids.add(211003L);
    ids.add(241003L);
    ids.add(301001L);
    ids.add(301002L);
    ids.add(331001L);
    ids.add(361001L);
    ids.add(391001L);
    ids.add(421001L);
    ids.add(421002L);
    ids.add(451001L);
    ids.add(481001L);
    ids.add(481003L);
    ids.add(481004L);
    ids.add(481029L);
    ids.add(511001L);
    ids.add(511002L);
    ids.add(511003L);
    ids.add(511005L);
    ids.add(511018L);
    ids.add(571015L);
    ids.add(601001L);
    ids.add(601002L);
    ids.add(601003L);
    ids.add(601009L);
    ids.add(631001L);
    ids.add(631003L);
    ids.add(631004L);
    ids.add(631005L);
    ids.add(631006L);
    execute(ids);
  }

  public static void execute(List<Long> datasetIds) throws ModelDBException {
    metadataDAO = new MetadataDAORdbImpl();
    commitDAO = new CommitDAORdbImpl(authService, roleService);
    repositoryDAO = new RepositoryDAORdbImpl(authService, roleService);
    blobDAO = new BlobDAORdbImpl(authService, roleService);

    LOGGER.info("Migration start");
    try {
      ModelDBUtils.registeredBackgroundUtilsCount();
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
      ModelDBUtils.unregisteredBackgroundUtilsCount();
    }
    LOGGER.info("Migration End");
  }

  private static void migrateDatasetVersionBasePath(Long datasetId)
      throws NoSuchAlgorithmException, ModelDBException {
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

    String previouslyCreatedId = null;
    for (DatasetVersion datasetVersion : datasetVersions) {
      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        String datasetVersionId = datasetVersion.getId();
        LOGGER.debug(
            "Dataset version base migration started for datasetVersion ID: {}, Version: {}",
            datasetVersionId,
            datasetVersion.getVersion());
        Transaction transaction = session.beginTransaction();
        CreateCommitRequest.Response createCommitResponse =
            setCommitFromDatasetVersion(
                session,
                datasetVersion,
                blobDAO,
                metadataDAO,
                repositoryEntity,
                previouslyCreatedId);
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
        previouslyCreatedId = newDatasetVersionId;
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
      RepositoryEntity repositoryEntity,
      String previouslyCreatedId)
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
              pathDatasetBlob.toBuilder()
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
                          s3DatasetComponentBlob.toBuilder()
                              .setPath(
                                  populateBasePathInComponentPath(s3DatasetComponentBlob.getPath()))
                              .build())
                  .collect(Collectors.toList());
          s3DatasetBlob =
              s3DatasetBlob.toBuilder()
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
    if (previouslyCreatedId != null) {
      builder.addParentShas(previouslyCreatedId);
    } else if (!datasetVersion.getParentId().isEmpty()) {
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

    LOGGER.debug("Dataset version parent: {}", datasetVersion.getParentId());
    CommitEntity commitEntity =
        commitDAO.saveCommitEntity(
            session, commit, rootSha, datasetVersion.getOwner(), repositoryEntity);
    try {
      blobDAO.setBlobsAttributes(
          session, repositoryEntity.getId(), commitEntity.getCommit_hash(), blobList, true);
    } catch (StatusRuntimeException ex) {
      if (ex.getStatus().getCode() == Status.ALREADY_EXISTS.getCode()) {
        LOGGER.warn("skipping already exists: {}", ex.getMessage());
      } else {
        throw ex;
      }
    }
    String compositeId =
        VersioningUtils.getVersioningCompositeId(
            repositoryEntity.getId(), commitEntity.getCommit_hash(), location);
    try {
      metadataDAO.addProperty(
          session,
          IdentificationType.newBuilder()
              .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB)
              .setStringId(compositeId)
              .build(),
          "description",
          datasetVersion.getDescription());
    } catch (StatusRuntimeException ex) {
      if (ex.getStatus().getCode() == Status.ALREADY_EXISTS.getCode()) {
        LOGGER.warn("skipping already exists: {}", ex.getMessage());
      } else {
        throw ex;
      }
    }

    try {
      metadataDAO.addLabels(
          session,
          IdentificationType.newBuilder()
              .setStringId(compositeId)
              .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB)
              .build(),
          datasetVersion.getTagsList());
    } catch (StatusRuntimeException ex) {
      if (ex.getStatus().getCode() == Status.ALREADY_EXISTS.getCode()) {
        LOGGER.warn("skipping already exists: {}", ex.getMessage());
      } else {
        throw ex;
      }
    }

    try {
      metadataDAO.addProperty(
          session,
          IdentificationType.newBuilder()
              .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB)
              .setStringId(compositeId)
              .build(),
          ModelDBConstants.VERSION,
          String.valueOf(datasetVersion.getVersion()));
    } catch (StatusRuntimeException ex) {
      if (ex.getStatus().getCode() == Status.ALREADY_EXISTS.getCode()) {
        LOGGER.warn("skipping already exists: {}", ex.getMessage());
      } else {
        throw ex;
      }
    }

    saveBranch(
        session, commitEntity.getCommit_hash(), ModelDBConstants.MASTER_BRANCH, repositoryEntity);

    return CreateCommitRequest.Response.newBuilder()
        .setCommit(commitEntity.toCommitProto())
        .build();
  }

  private static PathDatasetComponentBlob populateBasePathInComponentPath(
      PathDatasetComponentBlob part) {
    String componentPath = part.getPath();
    String basePath = part.getBasePath();
    if (!basePath.isEmpty() && !componentPath.startsWith(basePath)) {
      if (basePath.endsWith(componentPath)) {
        basePath = basePath.split(componentPath)[0];
      }
      componentPath = "s3://" + basePath + componentPath;
      LOGGER.debug("New component: {}", componentPath);
      LOGGER.debug("Old base: {}", part.getBasePath());
      LOGGER.debug("Old path: {}", part.getPath());
      return part.toBuilder().setPath(componentPath).setBasePath("").build();
    }
    return part;
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
