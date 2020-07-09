package ai.verta.modeldb.batchProcess;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.EntitiesEnum;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.App;
import ai.verta.modeldb.Dataset;
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
import java.security.NoSuchAlgorithmException;
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

    migrateDatasetsToRepositories();
  }

  private static void migrateDatasetsToRepositories() {
    LOGGER.debug("Datasets To Repositories migration started");
    Long count = getEntityCount(DatasetEntity.class);

    int lowerBound = 0;
    final int pagesize = recordUpdateLimit;
    LOGGER.debug("Total Datasets {}", count);

    while (lowerBound < count) {

      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
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

        if (datasetEntities.size() > 0) {
          Set<String> userIds = new HashSet<>();
          for (DatasetEntity datasetsEntity : datasetEntities) {
            userIds.add(datasetsEntity.getOwner());
          }
          LOGGER.debug("Datasets userId list : " + userIds);

          // Fetch the Dataset owners userInfo
          Map<String, UserInfo> userInfoMap = new HashMap<>();
          if (!userIds.isEmpty()) {
            userInfoMap = authService.getUserInfoFromAuthServer(userIds, null, null);
          }
          for (DatasetEntity datasetEntity : datasetEntities) {
            UserInfo userInfoValue = userInfoMap.get(datasetEntity.getOwner());
            try {
              createRepository(session, datasetEntity, userInfoValue);
            } catch (Exception e) {
              e.printStackTrace();
              LOGGER.error(e.getMessage());
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
      }
    }

    LOGGER.debug("Datasets To Repositories migration finished");
  }

  private static void createRepository(
      Session session, DatasetEntity datasetEntity, UserInfo userInfoValue)
      throws ModelDBException, NoSuchAlgorithmException, InvalidProtocolBufferException {
    String datasetId = datasetEntity.getId();
    Dataset newDataset = datasetEntity.getProtoObject().toBuilder().setId("").build();
    Repository repository;
    try {
      repository =
          repositoryDAO.createRepository(commitDAO, metadataDAO, newDataset, true, userInfoValue);
      migrateDatasetCollaborators(datasetId, repository);
    } catch (Exception e) {
      if (e instanceof StatusRuntimeException) {
        LOGGER.error("Getting error while migrating {} dataset", datasetId);
        LOGGER.error(e.getMessage());
        Status status = Status.fromThrowable(e);
        if (status.getCode().equals(Status.Code.ALREADY_EXISTS)) {
          repository =
              repositoryDAO.createRepository(commitDAO, metadataDAO, newDataset, false, null);
        } else {
          throw e;
        }
      } else {
        throw e;
      }
    }
    migrateDatasetVersionToCommitsBlobsMigration(session, datasetId, repository.getId());
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
    LOGGER.info("DatasetVersions To Commits and Blobs migration started");
    String countQuery =
        "SELECT COUNT(dv) FROM "
            + DatasetVersionEntity.class.getSimpleName()
            + " dv WHERE dv.dataset_id = :datasetId";
    Query query = session.createQuery(countQuery);
    query.setParameter("datasetId", datasetId);
    Long count = (Long) query.uniqueResult();

    int lowerBound = 0;
    final int pagesize = recordUpdateLimit;
    LOGGER.debug("Total DatasetVersions {}", count);

    while (lowerBound < count) {

      try (Session session1 = ModelDBHibernateUtil.getSessionFactory().openSession()) {
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
                Query linekedArtifactQuery = session1.createQuery(updateDatasetsLinkedArtifactId);
                linekedArtifactQuery.setParameter("commitHash", commitHash);
                linekedArtifactQuery.setParameter("datasetVersionId", datasetVersionEntity.getId());
                linekedArtifactQuery.executeUpdate();
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
          LOGGER.debug("DatasetVersions total count 0");
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

    LOGGER.info("DatasetVersionVersions To Commits and Blobs migration finished");
  }

  private static String createCommitAndBlobsFromDatsetVersion(
      Session session, DatasetVersion newDatasetVersion, Long repoId)
      throws ModelDBException, NoSuchAlgorithmException {
    RepositoryEntity repositoryEntity = session.get(RepositoryEntity.class, repoId);
    CreateCommitRequest.Response createCommitResponse =
        commitDAO.setCommitFromDatasetVersion(
            newDatasetVersion, repositoryDAO, blobDAO, metadataDAO, repositoryEntity);
    LOGGER.debug(
        "Migration done for datasetVersion to commit - commit hash : {}",
        createCommitResponse.getCommit().getCommitSha());
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
