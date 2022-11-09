package ai.verta.modeldb.batchProcess;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.EntitiesEnum;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.App;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.DatasetTypeEnum;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.MDBAuthServiceUtils;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.authservice.MDBRoleServiceUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.common.collaborator.CollaboratorOrg;
import ai.verta.modeldb.common.collaborator.CollaboratorTeam;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.entities.DatasetEntity;
import ai.verta.modeldb.entities.DatasetVersionEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAORdbImpl;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.metadata.MetadataDAORdbImpl;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.*;
import ai.verta.uac.GetCollaboratorResponseItem;
import ai.verta.uac.Role;
import ai.verta.uac.UserInfo;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class DatasetToRepositoryMigration {

  private static final String STARTED = "started";
  private static final String DATASET_ID_KEY = "datasetId";
  private static final String STATUS_KEY = "status";

  private DatasetToRepositoryMigration() {}

  private static final Logger LOGGER = LogManager.getLogger(DatasetToRepositoryMigration.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private static AuthService authService;
  private static UAC uac;
  private static MDBRoleService mdbRoleService;
  private static CommitDAO commitDAO;
  private static RepositoryDAO repositoryDAO;
  private static ExperimentRunDAO experimentRunDAO;
  private static MetadataDAO metadataDAO;
  private static BlobDAO blobDAO;
  private static int recordUpdateLimit = 100;
  private static Role writeOnlyRole;
  private static MDBConfig config;

  public static void execute(int recordUpdateLimit) {
    DatasetToRepositoryMigration.recordUpdateLimit = recordUpdateLimit;
    config = App.getInstance().mdbConfig;
    uac = UAC.FromConfig(config);
    authService = MDBAuthServiceUtils.FromConfig(config, uac);
    mdbRoleService = MDBRoleServiceUtils.FromConfig(config, authService, uac);

    commitDAO = new CommitDAORdbImpl(authService, mdbRoleService);
    repositoryDAO = new RepositoryDAORdbImpl(authService, mdbRoleService, commitDAO, metadataDAO);
    blobDAO = new BlobDAORdbImpl(authService, mdbRoleService);
    metadataDAO = new MetadataDAORdbImpl();
    experimentRunDAO =
        new ExperimentRunDAORdbImpl(
            config, authService, mdbRoleService, repositoryDAO, commitDAO, blobDAO, metadataDAO);
    migrateDatasetsToRepositories();
  }

  private static void migrateDatasetsToRepositories() {
    LOGGER.debug("Datasets To Repositories migration started");
    LOGGER.debug("using batch size {}", recordUpdateLimit);

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      try {
        var addBackupLinkedArtifactId =
            "ALTER TABLE artifact ADD COLUMN backup_linked_artifact_id varchar(255);";
        Query query = session.createSQLQuery(addBackupLinkedArtifactId);
        query.executeUpdate();

        var copyDataToBackupColumn =
            "UPDATE artifact set backup_linked_artifact_id = linked_artifact_id";
        query = session.createSQLQuery(copyDataToBackupColumn);
        query.executeUpdate();
      } catch (Exception e) {
        LOGGER.debug("backup_linked_artifact_id already exists");
      }

      String createDatasetMigrationTable;
      if (config.getDatabase().getRdbConfiguration().isMssql()) {
        createDatasetMigrationTable =
            "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = N'dataset_migration_status' AND type = 'U') "
                + "BEGIN "
                + "CREATE TABLE dataset_migration_status (dataset_id nvarchar(225) NOT NULL, repo_id bigint NOT NULL, status nvarchar(255) DEFAULT NULL) "
                + "END";
      } else {
        createDatasetMigrationTable =
            "CREATE TABLE IF NOT EXISTS dataset_migration_status (dataset_id varchar(225) NOT NULL, repo_id bigint(20) NOT NULL, status varchar(255) DEFAULT NULL)";
      }
      Query createQuery = session.createSQLQuery(createDatasetMigrationTable);
      createQuery.executeUpdate();
      session.getTransaction().commit();
    }

    LOGGER.debug("created backup linked_artifact column");
    LOGGER.debug("created dataset_migration_status table");

    Long count = getEntityCount(DatasetEntity.class);

    var lowerBound = 0;
    final int pagesize = recordUpdateLimit;
    LOGGER.debug("Total Datasets {}", count);

    while (lowerBound < count) {

      try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        LOGGER.debug("starting Dataset Processing for batch starting with {}", lowerBound);
        var transaction = session.beginTransaction();
        var criteriaBuilder = session.getCriteriaBuilder();

        CriteriaQuery<DatasetEntity> criteriaQuery =
            criteriaBuilder.createQuery(DatasetEntity.class);
        Root<DatasetEntity> root = criteriaQuery.from(DatasetEntity.class);

        CriteriaQuery<DatasetEntity> selectQuery =
            criteriaQuery
                .select(root)
                .where(criteriaBuilder.equal(root.get(ModelDBConstants.DELETED), false))
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
            userInfoMap = authService.getUserInfoFromAuthServer(userIds, null, null, true);
          }
          LOGGER.debug("Resolved owners in the batch from uac ");

          for (DatasetEntity datasetEntity : datasetEntities) {
            if (datasetEntity.getDataset_type() == DatasetTypeEnum.DatasetType.PATH_VALUE) {
              LOGGER.debug("Starting migrating dataset {}", datasetEntity.getId());

              if (checkDatasetMigrationStatus(session, datasetEntity.getId(), "done")) {
                LOGGER.debug("Dataset {} already migrated, continuing", datasetEntity.getId());
              } else {
                try {
                  if (checkDatasetMigrationStatus(session, datasetEntity.getId(), STARTED)) {
                    LOGGER.debug("Rolling back Dataset migration {}", datasetEntity.getId());
                    deleteAlreadyMigratedEntities(datasetEntity);
                  }
                  createRepository(
                      session, datasetEntity, userInfoMap.get(datasetEntity.getOwner()));
                } catch (Exception e) {
                  LOGGER.error(e.getMessage(), e);
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
    try (var innerSession = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Long repoId =
          getRepoIdFromDatasetMigrationStatus(innerSession, datasetEntity.getId(), STARTED);
      repositoryDAO.deleteRepositories(
          innerSession, experimentRunDAO, Collections.singletonList(String.valueOf(repoId)));
      updateDatasetMigrationStatus(datasetEntity.getId(), repoId, "deleted");
      LOGGER.debug("Dataset {} deleted", datasetEntity.getId());
      LOGGER.debug("Restart Dataset {} migration", datasetEntity.getId());
    }
  }

  private static boolean checkDatasetMigrationStatus(
      Session session, String datasetId, String status) {
    var updateStatusToStarted =
        "select COUNT(*) FROM dataset_migration_status WHERE status = :status AND dataset_id = :datasetId";
    Query query = session.createSQLQuery(updateStatusToStarted);
    query.setParameter(DATASET_ID_KEY, datasetId);
    query.setParameter(STATUS_KEY, status);
    BigInteger existsCount = (BigInteger) query.uniqueResult();
    return existsCount.longValue() > 0;
  }

  private static Long getRepoIdFromDatasetMigrationStatus(
      Session session, String datasetId, String status) {
    var updateStatusToStarted =
        "select repo_id FROM dataset_migration_status WHERE status = :status AND dataset_id = :datasetId";
    Query query = session.createSQLQuery(updateStatusToStarted);
    query.setParameter(DATASET_ID_KEY, datasetId);
    query.setParameter(STATUS_KEY, status);
    BigInteger repoId = (BigInteger) query.uniqueResult();
    return repoId.longValue();
  }

  private static void markStartedDatasetMigration(String datasetId, Long repoId, String status) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var updateStatusToStarted =
          "INSERT dataset_migration_status VALUES(:datasetId, :repoId, :status)";
      Query query = session.createSQLQuery(updateStatusToStarted);
      query.setParameter(DATASET_ID_KEY, datasetId);
      query.setParameter("repoId", repoId);
      query.setParameter(STATUS_KEY, status);
      session.beginTransaction();
      int insertedCount = query.executeUpdate();
      session.getTransaction().commit();
      LOGGER.trace("Inserted count: {}", insertedCount);
    }
  }

  private static void updateDatasetMigrationStatus(String datasetId, Long repoId, String status) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var updateStatusToStarted =
          "UPDATE dataset_migration_status SET status = :status WHERE dataset_id = :datasetId AND repo_id = :repoId";
      Query query = session.createSQLQuery(updateStatusToStarted);
      query.setParameter(DATASET_ID_KEY, datasetId);
      query.setParameter("repoId", repoId);
      query.setParameter(STATUS_KEY, status);
      session.beginTransaction();
      int updatedCount = query.executeUpdate();
      session.getTransaction().commit();
      LOGGER.trace("updated count: {}", updatedCount);
    }
  }

  private static void createRepository(
      Session session, DatasetEntity datasetEntity, UserInfo userInfoValue)
      throws ModelDBException, NoSuchAlgorithmException {
    String datasetId = datasetEntity.getId();
    var newDataset = datasetEntity.getProtoObject(mdbRoleService).toBuilder().setId("").build();
    Dataset dataset;
    try {
      LOGGER.debug("Creating repository for dataset {}", datasetEntity.getId());
      dataset =
          repositoryDAO.createOrUpdateDataset(
              newDataset, authService.getUsernameFromUserInfo(userInfoValue), true, userInfoValue);
      markStartedDatasetMigration(datasetId, Long.parseLong(dataset.getId()), STARTED);
      LOGGER.debug("Adding repository collaborattor for dataset {}", datasetEntity.getId());
      migrateDatasetCollaborators(datasetId, dataset);
    } catch (Exception e) {
      if (e instanceof StatusRuntimeException) {
        LOGGER.info("Getting error while migrating {} dataset", datasetId);
        LOGGER.info(e.getMessage());
        var status = Status.fromThrowable(e);
        if (status.getCode().equals(Status.Code.ALREADY_EXISTS)) {
          dataset =
              repositoryDAO.createOrUpdateDataset(
                  newDataset,
                  authService.getUsernameFromUserInfo(userInfoValue),
                  false,
                  userInfoValue);
          LOGGER.debug(
              "Continuing with repository {} already created for dataset {}",
              dataset.getId(),
              datasetEntity.getId());
        } else {
          throw e;
        }
      } else {
        throw e;
      }
    }
    LOGGER.debug("Created repository {} for dataset {}", dataset.getId(), datasetEntity.getId());
    migrateDatasetVersionToCommitsBlobsMigration(
        session, datasetId, Long.parseLong(dataset.getId()));
    updateDatasetMigrationStatus(datasetId, Long.parseLong(dataset.getId()), "done");
  }

  private static void migrateDatasetCollaborators(String datasetId, Dataset dataset) {
    List<GetCollaboratorResponseItem> collaboratorResponses =
        mdbRoleService.getResourceCollaborators(
            ModelDBResourceEnum.ModelDBServiceResourceTypes.DATASET,
            datasetId,
            dataset.getOwner(),
            null);

    if (!collaboratorResponses.isEmpty()) {
      for (GetCollaboratorResponseItem collaboratorResponse : collaboratorResponses) {
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
            .getPermission()
            .getCollaboratorType()
            .equals(CollaboratorTypeEnum.CollaboratorType.READ_WRITE)) {
          mdbRoleService.createRoleBinding(
              ModelDBConstants.ROLE_REPOSITORY_READ_ONLY,
              collaboratorBase,
              dataset.getId(),
              ModelDBResourceEnum.ModelDBServiceResourceTypes.DATASET);
        } else {
          mdbRoleService.createRoleBinding(
              ModelDBConstants.ROLE_REPOSITORY_READ_WRITE,
              collaboratorBase,
              dataset.getId(),
              ModelDBResourceEnum.ModelDBServiceResourceTypes.DATASET);
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
    var countQuery =
        "SELECT COUNT(dv) FROM DatasetVersionEntity dv WHERE dv.dataset_id = :datasetId";
    var query = session.createQuery(countQuery);
    query.setParameter(DATASET_ID_KEY, datasetId);
    Long count = (Long) query.uniqueResult();

    var lowerBound = 0;
    final int pagesize = recordUpdateLimit;
    LOGGER.debug("Total DatasetVersions {} in dataset {}", count, datasetId);

    while (lowerBound < count) {

      try (var session1 = modelDBHibernateUtil.getSessionFactory().openSession()) {
        LOGGER.debug("starting Dataset Version Processing for batch starting with {}", lowerBound);
        var transaction = session1.beginTransaction();
        var criteriaBuilder = session1.getCriteriaBuilder();

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
                .orderBy(criteriaBuilder.desc(root.get(ModelDBConstants.TIME_LOGGED)));

        TypedQuery<DatasetVersionEntity> typedQuery = session1.createQuery(selectQuery);

        typedQuery.setFirstResult(lowerBound);
        typedQuery.setMaxResults(pagesize);
        List<DatasetVersionEntity> datasetVersionEntities = typedQuery.getResultList();
        LOGGER.debug("got dataset versions");

        if (datasetVersionEntities.size() > 0) {
          for (DatasetVersionEntity datasetVersionEntity : datasetVersionEntities) {
            try {
              var newDatasetVersion = datasetVersionEntity.getProtoObject();
              if (newDatasetVersion.hasPathDatasetVersionInfo()) {
                String commitHash =
                    createCommitAndBlobsFromDatsetVersion(session1, newDatasetVersion, repoId);
                LOGGER.debug(
                    "{} datasetversion mapped to {} commit", newDatasetVersion.getId(), commitHash);

                var updateDatasetsLinkedArtifactId =
                    "UPDATE ArtifactEntity ar SET ar.linked_artifact_id = :commitHash WHERE ar.linked_artifact_id = :datasetVersionId";
                var linkedArtifactQuery = session1.createQuery(updateDatasetsLinkedArtifactId);
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
                LOGGER.info(
                    "DatasetVersion found with versionInfo type : {}",
                    newDatasetVersion.getDatasetVersionInfoCase());
              }
            } catch (Exception e) {
              LOGGER.error(e.getMessage(), e);
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
    var repositoryEntity = session.get(RepositoryEntity.class, repoId);
    var createCommitResponse =
        commitDAO.setCommitFromDatasetVersion(
            newDatasetVersion, repositoryDAO, blobDAO, metadataDAO, repositoryEntity);
    return createCommitResponse.getCommit().getCommitSha();
  }

  private static Long getEntityCount(Class<?> klass) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var criteriaBuilder = session.getCriteriaBuilder();
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
