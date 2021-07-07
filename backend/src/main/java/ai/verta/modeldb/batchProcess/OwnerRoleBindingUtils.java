package ai.verta.modeldb.batchProcess;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.App;
import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.entities.DatasetVersionEntity;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.UserInfo;
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

public class OwnerRoleBindingUtils {
  private OwnerRoleBindingUtils() {}

  private static final Logger LOGGER = LogManager.getLogger(OwnerRoleBindingUtils.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private static AuthService authService;
  private static UAC uac;
  private static RoleService roleService;

  public static void execute() {
    Config config = App.getInstance().config;
    if (config.hasAuth()) {
      uac = UAC.FromConfig(config);
      authService = AuthServiceUtils.FromConfig(config, uac);
      roleService = RoleServiceUtils.FromConfig(config, authService, uac);
    } else {
      LOGGER.debug("AuthService Host & Port not found");
      return;
    }

    LOGGER.info("Migration start");
    migrateExperiments();
    LOGGER.info("Experiments done migration");
    migrateExperimentRuns();
    LOGGER.info("ExperimentRuns done migration");
    migrateDatasetVersions();
    LOGGER.info("DatasetVersions done migration");
    migrateRepositories();
    LOGGER.info("Repositories done migration");

    LOGGER.info("Migration End");
  }

  private static void migrateExperiments() {
    LOGGER.debug("Experiments migration started");
    Long count = getEntityCount(ExperimentEntity.class);

    int lowerBound = 0;
    final int pagesize = 5000;
    LOGGER.debug("Total experiments {}", count);

    while (lowerBound < count) {

      try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        Transaction transaction = session.beginTransaction();
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

        CriteriaQuery<ExperimentEntity> criteriaQuery =
            criteriaBuilder.createQuery(ExperimentEntity.class);
        Root<ExperimentEntity> root = criteriaQuery.from(ExperimentEntity.class);

        CriteriaQuery<ExperimentEntity> selectQuery =
            criteriaQuery.select(root).orderBy(criteriaBuilder.asc(root.get("id")));

        TypedQuery<ExperimentEntity> typedQuery = session.createQuery(selectQuery);

        typedQuery.setFirstResult(lowerBound);
        typedQuery.setMaxResults(pagesize);
        List<ExperimentEntity> experimentEntities = typedQuery.getResultList();

        if (experimentEntities.size() > 0) {
          Set<String> userIds = new HashSet<>();
          for (ExperimentEntity experiment : experimentEntities) {
            userIds.add(experiment.getOwner());
          }
          LOGGER.debug("Experiments userId list : " + userIds);
          if (userIds.size() == 0) {
            LOGGER.warn("userIds not found for Experiments on page lower boundary {}", lowerBound);
            lowerBound += pagesize;
            continue;
          }

          // Fetch the experiment owners userInfo
          Map<String, UserInfo> userInfoMap =
              authService.getUserInfoFromAuthServer(userIds, null, null);
          for (ExperimentEntity experimentEntity : experimentEntities) {
            UserInfo userInfoValue = userInfoMap.get(experimentEntity.getOwner());
            if (userInfoValue != null) {
              try {
                roleService.createRoleBinding(
                    ModelDBConstants.ROLE_EXPERIMENT_OWNER,
                    new CollaboratorUser(authService, userInfoValue),
                    experimentEntity.getId(),
                    ModelDBServiceResourceTypes.EXPERIMENT);
              } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
              }
            } else {
              LOGGER.info(
                  "Experiment owner not found from UAC response list : experimentId - {} & userId - {}",
                  experimentEntity.getId(),
                  experimentEntity.getOwner());
              new ModelDBException(
                      "Experiment owner not found from UAC response list : experimentId - "
                          + experimentEntity.getId()
                          + " & userId - "
                          + experimentEntity.getOwner())
                  .printStackTrace();
            }
          }
        } else {
          LOGGER.debug("Experiments total count 0");
        }
        transaction.commit();
        lowerBound += pagesize;
      } catch (Exception ex) {
        if (ModelDBUtils.needToRetry(ex)) {
          migrateExperiments();
        } else {
          throw ex;
        }
      }
    }

    LOGGER.debug("Experiments migration finished");
  }

  private static void migrateExperimentRuns() {
    LOGGER.debug("ExperimentRuns migration started");
    Long count = getEntityCount(ExperimentRunEntity.class);

    int lowerBound = 0;
    final int pagesize = 5000;
    LOGGER.debug("Total experimentruns {}", count);

    while (lowerBound < count) {

      try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        Transaction transaction = session.beginTransaction();

        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

        CriteriaQuery<ExperimentRunEntity> criteriaQuery =
            criteriaBuilder.createQuery(ExperimentRunEntity.class);
        Root<ExperimentRunEntity> root = criteriaQuery.from(ExperimentRunEntity.class);

        CriteriaQuery<ExperimentRunEntity> selectQuery =
            criteriaQuery.select(root).orderBy(criteriaBuilder.asc(root.get("id")));

        TypedQuery<ExperimentRunEntity> typedQuery = session.createQuery(selectQuery);

        typedQuery.setFirstResult(lowerBound);
        typedQuery.setMaxResults(pagesize);
        Set<String> userIds = new HashSet<>();
        List<ExperimentRunEntity> experimentRunEntities = typedQuery.getResultList();
        for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
          userIds.add(experimentRunEntity.getOwner());
        }

        LOGGER.debug("ExperimentRuns userId list : " + userIds);
        if (userIds.size() == 0) {
          LOGGER.warn("userIds not found for ExperimentRuns on page lower boundary {}", lowerBound);
          lowerBound += pagesize;
          continue;
        }
        Map<String, UserInfo> userInfoMap =
            authService.getUserInfoFromAuthServer(userIds, null, null);
        for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
          UserInfo userInfoValue = userInfoMap.get(experimentRunEntity.getOwner());
          if (userInfoValue != null) {
            try {
              roleService.createRoleBinding(
                  ModelDBConstants.ROLE_EXPERIMENT_RUN_OWNER,
                  new CollaboratorUser(authService, userInfoValue),
                  experimentRunEntity.getId(),
                  ModelDBServiceResourceTypes.EXPERIMENT_RUN);
            } catch (Exception e) {
              e.printStackTrace();
              LOGGER.error(e.getMessage());
            }
          } else {
            LOGGER.info(
                "ExperimentRun owner not found from UAC response list : ExperimentRunId - {} & userId - {}",
                experimentRunEntity.getId(),
                experimentRunEntity.getOwner());
            new ModelDBException(
                    "ExperimentRun owner not found from UAC response list : ExperimentRunId - "
                        + experimentRunEntity.getId()
                        + " & userId - "
                        + experimentRunEntity.getOwner())
                .printStackTrace();
          }
        }
        LOGGER.debug("finished processing page lower boundary {}", lowerBound);
        transaction.commit();
        lowerBound += pagesize;
      } catch (Exception ex) {
        if (ModelDBUtils.needToRetry(ex)) {
          migrateExperimentRuns();
        } else {
          throw ex;
        }
      }
    }
    LOGGER.debug("ExperimentRuns migration finished");
  }

  private static void migrateDatasetVersions() {
    LOGGER.debug("DatasetVersions migration started");
    Long count = getEntityCount(DatasetVersionEntity.class);

    int lowerBound = 0;
    final int pagesize = 5000;
    LOGGER.debug("Total datasetVersions {}", count);

    while (lowerBound < count) {

      try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        Transaction transaction = session.beginTransaction();
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

        CriteriaQuery<DatasetVersionEntity> criteriaQuery =
            criteriaBuilder.createQuery(DatasetVersionEntity.class);
        Root<DatasetVersionEntity> root = criteriaQuery.from(DatasetVersionEntity.class);

        CriteriaQuery<DatasetVersionEntity> selectQuery =
            criteriaQuery.select(root).orderBy(criteriaBuilder.asc(root.get("id")));

        TypedQuery<DatasetVersionEntity> typedQuery = session.createQuery(selectQuery);

        typedQuery.setFirstResult(lowerBound);
        typedQuery.setMaxResults(pagesize);
        List<DatasetVersionEntity> datasetVersionEntities = typedQuery.getResultList();

        if (datasetVersionEntities.size() > 0) {
          Set<String> userIds = new HashSet<>();
          for (DatasetVersionEntity datasetVersionEntity : datasetVersionEntities) {
            userIds.add(datasetVersionEntity.getOwner());
          }
          LOGGER.debug("DatasetVersions userId list : " + userIds);
          if (userIds.size() == 0) {
            LOGGER.warn(
                "userIds not found for DatasetVersions on page lower boundary {}", lowerBound);
            lowerBound += pagesize;
            continue;
          }
          // Fetch the DatasetVersion owners userInfo
          Map<String, UserInfo> userInfoMap =
              authService.getUserInfoFromAuthServer(userIds, null, null);
          for (DatasetVersionEntity datasetVersionEntity : datasetVersionEntities) {
            UserInfo userInfoValue = userInfoMap.get(datasetVersionEntity.getOwner());
            if (userInfoValue != null) {
              try {
                roleService.createRoleBinding(
                    ModelDBConstants.ROLE_DATASET_VERSION_OWNER,
                    new CollaboratorUser(authService, userInfoValue),
                    datasetVersionEntity.getId(),
                    ModelDBServiceResourceTypes.DATASET_VERSION);
              } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
              }
            } else {
              LOGGER.info(
                  "DatasetVersion owner not found from UAC response list : DatasetVersionId - {} & userId - {}",
                  datasetVersionEntity.getId(),
                  datasetVersionEntity.getOwner());
              new ModelDBException(
                      "DatasetVersion owner not found from UAC response list : DatasetVersionId - "
                          + datasetVersionEntity.getId()
                          + " & userId - "
                          + datasetVersionEntity.getOwner())
                  .printStackTrace();
            }
          }
        } else {
          LOGGER.debug("DatasetVersions total count 0");
        }

        transaction.commit();
        lowerBound += pagesize;
      } catch (Exception ex) {
        if (ModelDBUtils.needToRetry(ex)) {
          migrateDatasetVersions();
        } else {
          throw ex;
        }
      }
    }

    LOGGER.debug("DatasetVersions migration finished");
  }

  private static void migrateRepositories() {
    LOGGER.debug("Repositories migration started");
    Long count = getEntityCount(RepositoryEntity.class);

    int lowerBound = 0;
    final int pagesize = 5000;
    LOGGER.debug("Total repositories {}", count);

    while (lowerBound < count) {

      try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        Transaction transaction = session.beginTransaction();
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

        CriteriaQuery<RepositoryEntity> criteriaQuery =
            criteriaBuilder.createQuery(RepositoryEntity.class);
        Root<RepositoryEntity> root = criteriaQuery.from(RepositoryEntity.class);

        CriteriaQuery<RepositoryEntity> selectQuery =
            criteriaQuery.select(root).orderBy(criteriaBuilder.asc(root.get("id")));

        TypedQuery<RepositoryEntity> typedQuery = session.createQuery(selectQuery);

        typedQuery.setFirstResult(lowerBound);
        typedQuery.setMaxResults(pagesize);
        List<RepositoryEntity> repositoryEntities = typedQuery.getResultList();

        if (repositoryEntities.size() > 0) {
          Set<String> userIds = new HashSet<>();
          for (RepositoryEntity repositoryEntity : repositoryEntities) {
            userIds.add(repositoryEntity.getOwner());
          }
          LOGGER.debug("Repositories userId list : " + userIds);
          if (userIds.size() == 0) {
            LOGGER.warn("userIds not found for Repositories on page lower boundary {}", lowerBound);
            lowerBound += pagesize;
            continue;
          }
          // Fetch the Repository owners userInfo
          Map<String, UserInfo> userInfoMap =
              authService.getUserInfoFromAuthServer(userIds, null, null);
          for (RepositoryEntity repositoryEntity : repositoryEntities) {
            UserInfo userInfoValue = userInfoMap.get(repositoryEntity.getOwner());
            if (userInfoValue != null) {
              try {
                ModelDBServiceResourceTypes modelDBServiceResourceTypes =
                    ModelDBUtils.getModelDBServiceResourceTypesFromRepository(repositoryEntity);
                roleService.createRoleBinding(
                    ModelDBConstants.ROLE_REPOSITORY_OWNER,
                    new CollaboratorUser(authService, userInfoValue),
                    String.valueOf(repositoryEntity.getId()),
                    modelDBServiceResourceTypes);
              } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
              }
            } else {
              LOGGER.info(
                  "Repository owner not found from UAC response list : RepositoryId - {} & userId - {}",
                  repositoryEntity.getId(),
                  repositoryEntity.getOwner());
              new ModelDBException(
                      "Repository owner not found from UAC response list : RepositoryId - "
                          + repositoryEntity.getId()
                          + " & userId - "
                          + repositoryEntity.getOwner())
                  .printStackTrace();
            }
          }
        } else {
          LOGGER.debug("Repositories total count 0");
        }

        transaction.commit();
        lowerBound += pagesize;
      } catch (Exception ex) {
        if (ModelDBUtils.needToRetry(ex)) {
          migrateRepositories();
        } else {
          throw ex;
        }
      }
    }

    LOGGER.debug("Repositories migration finished");
  }

  private static Long getEntityCount(Class<?> klass) {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
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
