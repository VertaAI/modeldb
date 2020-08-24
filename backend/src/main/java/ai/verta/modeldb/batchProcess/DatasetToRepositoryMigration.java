package ai.verta.modeldb.batchProcess;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.EntitiesEnum;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.App;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.DatasetTypeEnum;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.collaborator.CollaboratorBase;
import ai.verta.modeldb.collaborator.CollaboratorOrg;
import ai.verta.modeldb.collaborator.CollaboratorTeam;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.entities.DatasetEntity;
import ai.verta.modeldb.entities.DatasetVersionEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAORdbImpl;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.metadata.MetadataDAORdbImpl;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.BlobDAORdbImpl;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.CommitDAORdbImpl;
import ai.verta.modeldb.versioning.CreateCommitRequest;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryDAORdbImpl;
import ai.verta.uac.GetCollaboratorResponse;
import ai.verta.uac.Role;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class DatasetToRepositoryMigration {
  private DatasetToRepositoryMigration() {}

  private static final Logger LOGGER = LogManager.getLogger(DatasetToRepositoryMigration.class);
  private static AuthService authService;
  private static RoleService roleService;
  private static CommitDAO commitDAO;
  private static RepositoryDAO repositoryDAO;
  private static ExperimentRunDAO experimentRunDAO;
  private static MetadataDAO metadataDAO;
  private static BlobDAO blobDAO;
  private static int recordUpdateLimit = 100;
  private static Role readOnlyRole;
  private static Role writeOnlyRole;

  public static void execute(int recordUpdateLimit) {
    DatasetToRepositoryMigration.recordUpdateLimit = recordUpdateLimit;
    App app = App.getInstance();
    authService = new PublicAuthServiceUtils();
    roleService = new PublicRoleServiceUtils(authService);
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      authService = new AuthServiceUtils();
      roleService = new RoleServiceUtils(authService);

      readOnlyRole = roleService.getRoleByName(ModelDBConstants.ROLE_REPOSITORY_READ_ONLY, null);
      writeOnlyRole = roleService.getRoleByName(ModelDBConstants.ROLE_REPOSITORY_READ_WRITE, null);
    }

    commitDAO = new CommitDAORdbImpl(authService, roleService);
    repositoryDAO = new RepositoryDAORdbImpl(authService, roleService);
    blobDAO = new BlobDAORdbImpl(authService, roleService);
    metadataDAO = new MetadataDAORdbImpl();
    experimentRunDAO =
        new ExperimentRunDAORdbImpl(
            authService, roleService, repositoryDAO, commitDAO, blobDAO, metadataDAO);

    migrateDatasetsToRepositories();
  }

  private static void migrateDatasetsToRepositories() {
    LOGGER.debug("Datasets To Repositories migration started");
    LOGGER.debug("using batch size {}", recordUpdateLimit);

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      try {
        String addBackupLinkedArtifactId =
            "ALTER TABLE artifact ADD COLUMN backup_linked_artifact_id varchar(255);";
        Query query = session.createSQLQuery(addBackupLinkedArtifactId);
        query.executeUpdate();

        String copyDataToBackupColumn =
            "UPDATE artifact set backup_linked_artifact_id = linked_artifact_id";
        query = session.createSQLQuery(copyDataToBackupColumn);
        query.executeUpdate();
      } catch (Exception e) {
        LOGGER.debug("backup_linked_artifact_id already exists");
      }

      String createDatasetMigrationTable =
          "CREATE TABLE IF NOT EXISTS dataset_migration_status (dataset_id varchar(225) NOT NULL, repo_id bigint(20) NOT NULL, status varchar(255) DEFAULT NULL)";
      Query createQuery = session.createSQLQuery(createDatasetMigrationTable);
      createQuery.executeUpdate();
      session.getTransaction().commit();
    }

    LOGGER.debug("created backup linked_artifact column");
    LOGGER.debug("created dataset_migration_status table");

    Long count = getEntityCount(DatasetEntity.class);

    int lowerBound = 0;
    final int pagesize = recordUpdateLimit;
    LOGGER.debug("Total Datasets {}", count);

    while (lowerBound < count) {

      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        LOGGER.debug("starting Dataset Processing for batch starting with {}", lowerBound);
        Transaction transaction = session.beginTransaction();
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

        CriteriaQuery<DatasetEntity> criteriaQuery =
            criteriaBuilder.createQuery(DatasetEntity.class);
        Root<DatasetEntity> root = criteriaQuery.from(DatasetEntity.class);

        CriteriaQuery<DatasetEntity> selectQuery =
            criteriaQuery
                .select(root)
                .where(criteriaBuilder.equal(root.get("deleted"), false))
                .orderBy(criteriaBuilder.asc(root.get("time_created")));

        TypedQuery<DatasetEntity> typedQuery = session.createQuery(selectQuery);

        typedQuery.setFirstResult(lowerBound);
        typedQuery.setMaxResults(pagesize);
        List<DatasetEntity> datasetEntities = typedQuery.getResultList();
        LOGGER.debug("got datasets");
        if (datasetEntities.size() > 0) {
          Set<String> userIds = new HashSet<>();
          for (DatasetEntity datasetsEntity : datasetEntities) {
            userIds.add(datasetsEntity.getOwner());
          }
          LOGGER.debug("Distinct owners in the batch : " + userIds);

          // Fetch the Dataset owners userInfo
          Map<String, UserInfo> userInfoMap = new HashMap<>();
          if (!userIds.isEmpty()) {
            userInfoMap = authService.getUserInfoFromAuthServer(userIds, null, null);
          }
          LOGGER.debug("Resolved owners in the batch from uac ");

          for (DatasetEntity datasetEntity : datasetEntities) {
            if (datasetEntity.getDataset_type() == DatasetTypeEnum.DatasetType.PATH_VALUE) {
              LOGGER.debug("Starting migrating dataset {}", datasetEntity.getId());

              if (checkDatasetMigrationStatus(session, datasetEntity.getId(), "done")) {
                LOGGER.debug("Dataset {} already migrated, continuing", datasetEntity.getId());
              } else {
                try {
                  if (checkDatasetMigrationStatus(session, datasetEntity.getId(), "started")) {
                    LOGGER.debug("Rolling back Dataset migration {}", datasetEntity.getId());
                    deleteAlreadyMigratedEntities(datasetEntity);
                  }
                  createRepository(
                      session, datasetEntity, userInfoMap.get(datasetEntity.getOwner()));
                } catch (Exception e) {
                  e.printStackTrace();
                  LOGGER.error(e.getMessage());
                }
              }
            }
          }
        } else {
          LOGGER.debug("Datasets total count 0");
        }

        transaction.commit();
        lowerBound += pagesize;
      } catch (Exception ex) {
        if (ModelDBUtils.needToRetry(ex)) {
          migrateDatasetsToRepositories();
        } else {
          throw ex;
        }
      } finally {
        LOGGER.debug("gc starts");
        Runtime.getRuntime().gc();
        LOGGER.debug("gc ends");
      }
    }

    LOGGER.debug("Datasets To Repositories migration finished");
  }

  private static void deleteAlreadyMigratedEntities(DatasetEntity datasetEntity) {
    LOGGER.debug("Dataset {} already started", datasetEntity.getId());
    try (Session innerSession = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Long repoId =
          getRepoIdFromDatasetMigrationStatus(innerSession, datasetEntity.getId(), "started");
      repositoryDAO.deleteRepositories(
          innerSession, experimentRunDAO, Collections.singletonList(String.valueOf(repoId)));
      updateDatasetMigrationStatus(datasetEntity.getId(), repoId, "deleted");
      LOGGER.debug("Dataset {} deleted", datasetEntity.getId());
      LOGGER.debug("Restart Dataset {} migration", datasetEntity.getId());
    }
  }

  private static boolean checkDatasetMigrationStatus(
      Session session, String datasetId, String status) {
    String updateStatusToStarted =
        "select COUNT(*) FROM dataset_migration_status WHERE status = :status AND dataset_id = :datasetId";
    Query query = session.createSQLQuery(updateStatusToStarted);
    query.setParameter("datasetId", datasetId);
    query.setParameter("status", status);
    BigInteger existsCount = (BigInteger) query.uniqueResult();
    return existsCount.longValue() > 0;
  }

  private static Long getRepoIdFromDatasetMigrationStatus(
      Session session, String datasetId, String status) {
    String updateStatusToStarted =
        "select repo_id FROM dataset_migration_status WHERE status = :status AND dataset_id = :datasetId";
    Query query = session.createSQLQuery(updateStatusToStarted);
    query.setParameter("datasetId", datasetId);
    query.setParameter("status", status);
    BigInteger repoId = (BigInteger) query.uniqueResult();
    return repoId.longValue();
  }

  private static void markStartedDatasetMigration(String datasetId, Long repoId, String status) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      String updateStatusToStarted =
          "INSERT dataset_migration_status VALUES(:datasetId, :repoId, :status)";
      Query query = session.createSQLQuery(updateStatusToStarted);
      query.setParameter("datasetId", datasetId);
      query.setParameter("repoId", repoId);
      query.setParameter("status", status);
      session.beginTransaction();
      int insertedCount = query.executeUpdate();
      session.getTransaction().commit();
      LOGGER.trace("Inserted count: {}", insertedCount);
    }
  }

  private static void updateDatasetMigrationStatus(String datasetId, Long repoId, String status) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      String updateStatusToStarted =
          "UPDATE dataset_migration_status SET status = :status WHERE dataset_id = :datasetId AND repo_id = :repoId";
      Query query = session.createSQLQuery(updateStatusToStarted);
      query.setParameter("datasetId", datasetId);
      query.setParameter("repoId", repoId);
      query.setParameter("status", status);
      session.beginTransaction();
      int updatedCount = query.executeUpdate();
      session.getTransaction().commit();
      LOGGER.trace("updated count: {}", updatedCount);
    }
  }

  private static void createRepository(
      Session session, DatasetEntity datasetEntity, UserInfo userInfoValue)
      throws ModelDBException, NoSuchAlgorithmException, InvalidProtocolBufferException {
    String datasetId = datasetEntity.getId();
    Dataset newDataset = datasetEntity.getProtoObject().toBuilder().setId("").build();
    Repository repository;
    try {
      LOGGER.debug("Creating repository for dataset {}", datasetEntity.getId());
      repository =
          repositoryDAO.createRepository(commitDAO, metadataDAO, newDataset, true, userInfoValue);
      markStartedDatasetMigration(datasetId, repository.getId(), "started");
      LOGGER.debug("Adding repository collaborattor for dataset {}", datasetEntity.getId());
      migrateDatasetCollaborators(datasetId, repository);
    } catch (Exception e) {
      if (e instanceof StatusRuntimeException) {
        LOGGER.error("Getting error while migrating {} dataset", datasetId);
        LOGGER.error(e.getMessage());
        Status status = Status.fromThrowable(e);
        if (status.getCode().equals(Status.Code.ALREADY_EXISTS)) {
          repository =
              repositoryDAO.createRepository(commitDAO, metadataDAO, newDataset, false, null);
          LOGGER.debug(
              "Continuing with repository {} already created for dataset {}",
              repository.getId(),
              datasetEntity.getId());
        } else {
          throw e;
        }
      } else {
        throw e;
      }
    }
    LOGGER.debug("Created repository {} for dataset {}", repository.getId(), datasetEntity.getId());
    migrateDatasetVersionToCommitsBlobsMigration(session, datasetId, repository.getId());
    updateDatasetMigrationStatus(datasetId, repository.getId(), "done");
  }

  private static void migrateDatasetCollaborators(String datasetId, Repository repository) {
    List<GetCollaboratorResponse> collaboratorResponses =
        roleService.getResourceCollaborators(
            ModelDBResourceEnum.ModelDBServiceResourceTypes.DATASET,
            datasetId,
            repository.getOwner(),
            null);

    if (!collaboratorResponses.isEmpty()) {
      for (GetCollaboratorResponse collaboratorResponse : collaboratorResponses) {
        CollaboratorBase collaboratorBase;
        if (collaboratorResponse
            .getAuthzEntityType()
            .equals(EntitiesEnum.EntitiesTypes.ORGANIZATION)) {
          collaboratorBase = new CollaboratorOrg(collaboratorResponse.getVertaId());
        } else if (collaboratorResponse
            .getAuthzEntityType()
            .equals(EntitiesEnum.EntitiesTypes.TEAM)) {
          collaboratorBase = new CollaboratorTeam(collaboratorResponse.getVertaId());
        } else {
          collaboratorBase = new CollaboratorUser(authService, collaboratorResponse.getVertaId());
        }
        if (collaboratorResponse
            .getCollaboratorType()
            .equals(CollaboratorTypeEnum.CollaboratorType.READ_WRITE)) {
          roleService.createRoleBinding(
              readOnlyRole,
              collaboratorBase,
              String.valueOf(repository.getId()),
              ModelDBResourceEnum.ModelDBServiceResourceTypes.REPOSITORY);
        } else {
          roleService.createRoleBinding(
              writeOnlyRole,
              collaboratorBase,
              String.valueOf(repository.getId()),
              ModelDBResourceEnum.ModelDBServiceResourceTypes.REPOSITORY);
        }
      }
    }
  }

  private static void migrateDatasetVersionToCommitsBlobsMigration(
      Session session, String datasetId, Long repoId) {
    LOGGER.debug(
        "DatasetVersions To Commits and Blobs migration started for dataset {}, repo id {}",
        datasetId,
        repoId);
    String countQuery =
        "SELECT COUNT(dv) FROM "
            + DatasetVersionEntity.class.getSimpleName()
            + " dv WHERE dv.dataset_id = :datasetId";
    Query query = session.createQuery(countQuery);
    query.setParameter("datasetId", datasetId);
    Long count = (Long) query.uniqueResult();

    int lowerBound = 0;
    final int pagesize = recordUpdateLimit;
    LOGGER.debug("Total DatasetVersions {} in dataset {}", count, datasetId);

    while (lowerBound < count) {

      try (Session session1 = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        LOGGER.debug("starting Dataset Version Processing for batch starting with {}", lowerBound);
        Transaction transaction = session1.beginTransaction();
        CriteriaBuilder criteriaBuilder = session1.getCriteriaBuilder();

        CriteriaQuery<DatasetVersionEntity> criteriaQuery =
            criteriaBuilder.createQuery(DatasetVersionEntity.class);
        Root<DatasetVersionEntity> root = criteriaQuery.from(DatasetVersionEntity.class);

        CriteriaQuery<DatasetVersionEntity> selectQuery =
            criteriaQuery
                .select(root)
                .where(
                    criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("deleted"), false),
                        criteriaBuilder.equal(root.get("dataset_id"), datasetId)))
                .orderBy(criteriaBuilder.asc(root.get("id")));

        TypedQuery<DatasetVersionEntity> typedQuery = session1.createQuery(selectQuery);

        typedQuery.setFirstResult(lowerBound);
        typedQuery.setMaxResults(pagesize);
        List<DatasetVersionEntity> datasetVersionEntities = typedQuery.getResultList();
        LOGGER.debug("got dataset versions");

        if (datasetVersionEntities.size() > 0) {
          for (DatasetVersionEntity datasetVersionEntity : datasetVersionEntities) {
            try {
              DatasetVersion newDatasetVersion = datasetVersionEntity.getProtoObject();
              if (newDatasetVersion.hasPathDatasetVersionInfo()) {
                String commitHash =
                    createCommitAndBlobsFromDatsetVersion(session1, newDatasetVersion, repoId);
                LOGGER.debug(
                    "{} datasetversion mapped to {} commit", newDatasetVersion.getId(), commitHash);

                String updateDatasetsLinkedArtifactId =
                    "UPDATE ArtifactEntity ar SET ar.linked_artifact_id = :commitHash WHERE ar.linked_artifact_id = :datasetVersionId";
                Query linkedArtifactQuery = session1.createQuery(updateDatasetsLinkedArtifactId);
                linkedArtifactQuery.setParameter("commitHash", commitHash);
                linkedArtifactQuery.setParameter("datasetVersionId", datasetVersionEntity.getId());
                int updateCount = linkedArtifactQuery.executeUpdate();
                if (updateCount != 0) {
                  LOGGER.debug(
                      "Updated linked artifact id from {}  to {}",
                      datasetVersionEntity.getId(),
                      commitHash);
                }
              } else {
                LOGGER.warn(
                    "DatasetVersion found with versionInfo type : {}",
                    newDatasetVersion.getDatasetVersionInfoCase());
              }
            } catch (Exception e) {
              e.printStackTrace();
              LOGGER.error(e.getMessage());
            }
          }
        } else {
          LOGGER.debug("DatasetVersions total count 0 for dataset {}", datasetId);
        }

        transaction.commit();
        lowerBound += pagesize;
      } catch (Exception ex) {
        if (ModelDBUtils.needToRetry(ex)) {
          migrateDatasetVersionToCommitsBlobsMigration(session, datasetId, repoId);
        } else {
          throw ex;
        }
      }
    }

    LOGGER.info(
        "DatasetVersionVersions To Commits and Blobs migration finished for dataset {}, repo id {}",
        datasetId,
        repoId);
  }

  private static String createCommitAndBlobsFromDatsetVersion(
      Session session, DatasetVersion newDatasetVersion, Long repoId)
      throws ModelDBException, NoSuchAlgorithmException {
    RepositoryEntity repositoryEntity = session.get(RepositoryEntity.class, repoId);
    CreateCommitRequest.Response createCommitResponse =
        commitDAO.setCommitFromDatasetVersion(
            newDatasetVersion, repositoryDAO, blobDAO, metadataDAO, repositoryEntity);
    return createCommitResponse.getCommit().getCommitSha();
  }

  private static Long getEntityCount(Class<?> klass) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
      countQuery.select(criteriaBuilder.count(countQuery.from(klass)));
      return session.createQuery(countQuery).getSingleResult();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getEntityCount(klass);
      } else {
        throw ex;
      }
    }
  }
}
