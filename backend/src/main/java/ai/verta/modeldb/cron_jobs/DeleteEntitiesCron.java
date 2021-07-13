package ai.verta.modeldb.cron_jobs;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.entities.CommentEntity;
import ai.verta.modeldb.entities.DatasetEntity;
import ai.verta.modeldb.entities.DatasetVersionEntity;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.TagsEntity;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.VersioningUtils;
import com.google.rpc.Code;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.stream.Collectors;
import javax.persistence.OptimisticLockException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class DeleteEntitiesCron extends TimerTask {
  private static final Logger LOGGER = LogManager.getLogger(DeleteEntitiesCron.class);
  private final ModelDBHibernateUtil modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
  private final AuthService authService;
  private final RoleService roleService;
  private final Integer recordUpdateLimit;

  public DeleteEntitiesCron(
      AuthService authService, RoleService roleService, Integer recordUpdateLimit) {
    this.authService = authService;
    this.roleService = roleService;
    this.recordUpdateLimit = recordUpdateLimit;
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("DeleteEntitiesCron wakeup");

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      // Update project timestamp
      deleteProjects(session);

      // Update experiment timestamp
      deleteExperiments(session);

      // Update dataset timestamp
      deleteDatasets(session);

      // Update datasetVersion timestamp
      deleteDatasetVersions(session);

      // Update repository timestamp
      deleteRepositories(session);

      // Update experimentRun timestamp
      deleteExperimentRuns(session);
    } catch (Exception ex) {
      if (ex instanceof StatusRuntimeException) {
        StatusRuntimeException exception = (StatusRuntimeException) ex;
        if (exception.getStatus().getCode().value() == Code.PERMISSION_DENIED_VALUE) {
          LOGGER.warn("DeleteEntitiesCron Exception: {}", ex.getMessage());
        } else {
          LOGGER.warn("DeleteEntitiesCron Exception: ", ex);
        }
      } else {
        LOGGER.warn("DeleteEntitiesCron Exception: ", ex);
      }
    }
    LOGGER.info("DeleteEntitiesCron finish tasks and reschedule");
  }

  private void deleteProjects(Session session) {
    LOGGER.trace("Project deleting");
    String alias = "pr";
    String deleteProjectsQueryString =
        new StringBuilder("FROM ")
            .append(ProjectEntity.class.getSimpleName())
            .append(" ")
            .append(alias)
            .append(" WHERE ")
            .append(alias)
            .append(".")
            .append(ModelDBConstants.DELETED)
            .append(" = :deleted ")
            .toString();

    Query projectDeleteQuery = session.createQuery(deleteProjectsQueryString);
    projectDeleteQuery.setParameter("deleted", true);
    projectDeleteQuery.setMaxResults(this.recordUpdateLimit);
    List<ProjectEntity> projectEntities = projectDeleteQuery.list();

    List<String> projectIds = new ArrayList<>();
    if (!projectEntities.isEmpty()) {
      for (ProjectEntity projectEntity : projectEntities) {
        projectIds.add(projectEntity.getId());
      }

      try {
        roleService.deleteEntityResourcesWithServiceUser(
            projectIds, ModelDBServiceResourceTypes.PROJECT);
        Transaction transaction = session.beginTransaction();
        String updateDeletedStatusExperimentQueryString =
            new StringBuilder("UPDATE ")
                .append(ExperimentEntity.class.getSimpleName())
                .append(" exp ")
                .append("SET exp.")
                .append(ModelDBConstants.DELETED)
                .append(" = :deleted ")
                .append(" WHERE exp.")
                .append(ModelDBConstants.PROJECT_ID)
                .append(" IN (:projectIds)")
                .toString();
        Query deletedExperimentQuery =
            session.createQuery(updateDeletedStatusExperimentQueryString);
        deletedExperimentQuery.setParameter("deleted", true);
        deletedExperimentQuery.setParameter("projectIds", projectIds);
        deletedExperimentQuery.executeUpdate();
        transaction.commit();

        for (ProjectEntity projectEntity : projectEntities) {
          try {
            transaction = session.beginTransaction();
            session.delete(projectEntity);
            transaction.commit();
          } catch (OptimisticLockException ex) {
            LOGGER.info("DeleteEntitiesCron : deleteProjects : Exception: {}", ex.getMessage());
          }
        }
      } catch (OptimisticLockException ex) {
        LOGGER.info("DeleteEntitiesCron : deleteProjects : Exception: {}", ex.getMessage());
      } catch (Exception ex) {
        LOGGER.warn("DeleteEntitiesCron : deleteProjects : Exception: ", ex);
      }
    }

    LOGGER.debug("Project Deleted successfully : Deleted projects count {}", projectIds.size());
  }

  private void deleteExperiments(Session session) {
    LOGGER.trace("Experiment deleting");
    String deleteExperimentQueryString =
        new StringBuilder("FROM ")
            .append(ExperimentEntity.class.getSimpleName())
            .append(" ex WHERE ex.")
            .append(ModelDBConstants.DELETED)
            .append(" = :deleted ")
            .toString();

    Query experimentDeleteQuery = session.createQuery(deleteExperimentQueryString);
    experimentDeleteQuery.setParameter("deleted", true);
    experimentDeleteQuery.setMaxResults(this.recordUpdateLimit);
    List<ExperimentEntity> experimentEntities = experimentDeleteQuery.list();

    List<String> experimentIds = new ArrayList<>();
    if (!experimentEntities.isEmpty()) {
      for (ExperimentEntity experimentEntity : experimentEntities) {
        experimentIds.add(experimentEntity.getId());
      }

      try {
        deleteRoleBindingsForExperiments(experimentEntities);
      } catch (StatusRuntimeException ex) {
        LOGGER.debug(
            "DeleteEntitiesCron : deleteExperiments : deleteRoleBindingsForExperiments : Exception: {}",
            ex.getMessage());
      } catch (Exception ex) {
        LOGGER.warn(
            "DeleteEntitiesCron : deleteExperiments : deleteRoleBindingsForExperiments : Exception: ",
            ex);
      }

      try {
        Transaction transaction = session.beginTransaction();
        String updateDeletedStatusExperimentRunQueryString =
            new StringBuilder("UPDATE ")
                .append(ExperimentRunEntity.class.getSimpleName())
                .append(" expr ")
                .append("SET expr.")
                .append(ModelDBConstants.DELETED)
                .append(" = :deleted ")
                .append(" WHERE expr.")
                .append(ModelDBConstants.EXPERIMENT_ID)
                .append(" IN (:experimentIds)")
                .toString();
        Query deletedExperimentRunQuery =
            session.createQuery(updateDeletedStatusExperimentRunQueryString);
        deletedExperimentRunQuery.setParameter("deleted", true);
        deletedExperimentRunQuery.setParameter("experimentIds", experimentIds);
        deletedExperimentRunQuery.executeUpdate();
        transaction.commit();

        for (ExperimentEntity experimentEntity : experimentEntities) {
          try {
            transaction = session.beginTransaction();
            session.delete(experimentEntity);
            transaction.commit();
          } catch (OptimisticLockException ex) {
            LOGGER.info("DeleteEntitiesCron : deleteExperiments : Exception: {}", ex.getMessage());
          }
        }
      } catch (OptimisticLockException ex) {
        LOGGER.info("DeleteEntitiesCron : deleteExperiments : Exception: {}", ex.getMessage());
      } catch (Exception ex) {
        LOGGER.warn("DeleteEntitiesCron : deleteExperiments : Exception:", ex);
      }
    }

    LOGGER.debug(
        "Experiment deleted successfully : Deleted experiments count {}", experimentIds.size());
  }

  private void deleteRoleBindingsForExperiments(List<ExperimentEntity> experimentEntities) {
    List<String> roleBindingNames = new LinkedList<>();
    for (ExperimentEntity experimentEntity : experimentEntities) {
      String ownerRoleBindingName =
          roleService.buildRoleBindingName(
              ModelDBConstants.ROLE_EXPERIMENT_OWNER,
              experimentEntity.getId(),
              experimentEntity.getOwner(),
              ModelDBServiceResourceTypes.EXPERIMENT.name());
      if (ownerRoleBindingName != null) {
        roleBindingNames.add(ownerRoleBindingName);
      }
    }
    if (!roleBindingNames.isEmpty()) {
      roleService.deleteRoleBindingsUsingServiceUser(roleBindingNames);
    }
  }

  private void deleteExperimentRuns(Session session) {
    LOGGER.trace("ExperimentRun deleting");
    String deleteExperimentRunQueryString =
        new StringBuilder("FROM ")
            .append(ExperimentRunEntity.class.getSimpleName())
            .append(" expr WHERE expr.")
            .append(ModelDBConstants.DELETED)
            .append(" = :deleted ")
            .toString();

    Query experimentRunDeleteQuery = session.createQuery(deleteExperimentRunQueryString);
    experimentRunDeleteQuery.setParameter("deleted", true);
    experimentRunDeleteQuery.setMaxResults(this.recordUpdateLimit);
    List<ExperimentRunEntity> experimentRunEntities = experimentRunDeleteQuery.list();

    List<String> experimentRunIds = new ArrayList<>();
    if (!experimentRunEntities.isEmpty()) {
      for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
        experimentRunIds.add(experimentRunEntity.getId());
      }
      try {
        deleteRoleBindingsForExperimentRuns(experimentRunEntities);
      } catch (StatusRuntimeException ex) {
        LOGGER.info(
            "DeleteEntitiesCron : deleteExperimentRuns : deleteRoleBindingsForExperimentRuns : Exception: {}",
            ex.getMessage());
      } catch (Exception ex) {
        LOGGER.warn(
            "DeleteEntitiesCron : deleteExperimentRuns : deleteRoleBindingsForExperimentRuns : Exception: ",
            ex);
      }

      try {
        // Delete the ExperimentRun comments
        Transaction transaction = session.beginTransaction();
        if (!experimentRunIds.isEmpty()) {
          removeEntityComments(
              session, experimentRunIds, ExperimentRunEntity.class.getSimpleName());
        }
        transaction.commit();

        for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
          try {
            transaction = session.beginTransaction();
            session.delete(experimentRunEntity);
            transaction.commit();
          } catch (OptimisticLockException ex) {
            LOGGER.info(
                "DeleteEntitiesCron : deleteExperimentRuns : Exception: {}", ex.getMessage());
          }
        }
      } catch (OptimisticLockException ex) {
        LOGGER.info("DeleteEntitiesCron : deleteExperimentRuns : Exception: {}", ex.getMessage());
      } catch (Exception ex) {
        LOGGER.debug("DeleteEntitiesCron : deleteExperimentRuns : Exception:", ex);
      }
    }

    LOGGER.debug(
        "ExperimentRun deleted successfully : Deleted experimentRuns count {}",
        experimentRunIds.size());
  }

  private void deleteRoleBindingsForExperimentRuns(
      List<ExperimentRunEntity> experimentRunEntities) {
    List<String> roleBindingNames = new LinkedList<>();
    for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
      String ownerRoleBindingName =
          roleService.buildRoleBindingName(
              ModelDBConstants.ROLE_EXPERIMENT_RUN_OWNER,
              experimentRunEntity.getId(),
              experimentRunEntity.getOwner(),
              ModelDBServiceResourceTypes.EXPERIMENT_RUN.name());
      if (ownerRoleBindingName != null) {
        roleBindingNames.add(ownerRoleBindingName);
      }
    }
    if (!roleBindingNames.isEmpty()) {
      roleService.deleteRoleBindingsUsingServiceUser(roleBindingNames);
    }
  }

  private void removeEntityComments(Session session, List<String> entityIds, String entityName) {
    String commentDeleteHql =
        new StringBuilder()
            .append("From CommentEntity ce where ce.")
            .append(ModelDBConstants.ENTITY_ID)
            .append(" IN (:entityIds) AND ce.")
            .append(ModelDBConstants.ENTITY_NAME)
            .append(" =:entityName")
            .toString();
    Query commentDeleteQuery = session.createQuery(commentDeleteHql);
    commentDeleteQuery.setParameterList("entityIds", entityIds);
    commentDeleteQuery.setParameter("entityName", entityName);
    List<CommentEntity> commentEntities = commentDeleteQuery.list();
    for (CommentEntity commentEntity : commentEntities) {
      session.delete(commentEntity);
    }
  }

  private void deleteDatasets(Session session) {
    LOGGER.trace("Dataset deleting");
    String alias = "dt";
    String deleteDatasetsQueryString =
        new StringBuilder("FROM ")
            .append(DatasetEntity.class.getSimpleName())
            .append(" ")
            .append(alias)
            .append(" WHERE ")
            .append(alias)
            .append(".")
            .append(ModelDBConstants.DELETED)
            .append(" = :deleted ")
            .toString();
    Query datasetDeleteQuery = session.createQuery(deleteDatasetsQueryString);
    datasetDeleteQuery.setParameter("deleted", true);
    datasetDeleteQuery.setMaxResults(this.recordUpdateLimit);
    List<DatasetEntity> datasetEntities = datasetDeleteQuery.list();

    List<String> datasetIds = new ArrayList<>();
    if (!datasetEntities.isEmpty()) {
      for (DatasetEntity datasetEntity : datasetEntities) {
        datasetIds.add(datasetEntity.getId());
      }

      try {
        roleService.deleteEntityResourcesWithServiceUser(
            datasetIds, ModelDBServiceResourceTypes.DATASET);
        Transaction transaction = session.beginTransaction();
        String updateDeletedStatusDatasetVersionQueryString =
            new StringBuilder("UPDATE ")
                .append(DatasetVersionEntity.class.getSimpleName())
                .append(" dv ")
                .append("SET dv.")
                .append(ModelDBConstants.DELETED)
                .append(" = :deleted ")
                .append(" WHERE dv.")
                .append(ModelDBConstants.DATASET_ID)
                .append(" IN (:datasetIds)")
                .toString();
        Query deletedDatasetVersionQuery =
            session.createQuery(updateDeletedStatusDatasetVersionQueryString);
        deletedDatasetVersionQuery.setParameter("deleted", true);
        deletedDatasetVersionQuery.setParameter("datasetIds", datasetIds);
        deletedDatasetVersionQuery.executeUpdate();
        transaction.commit();

        for (DatasetEntity datasetEntity : datasetEntities) {
          try {
            transaction = session.beginTransaction();
            session.delete(datasetEntity);
            transaction.commit();
          } catch (OptimisticLockException ex) {
            LOGGER.info("DeleteEntitiesCron : deleteDatasets : Exception: {}", ex.getMessage());
          }
        }
      } catch (OptimisticLockException ex) {
        LOGGER.info("DeleteEntitiesCron : deleteDatasets : Exception: {}", ex.getMessage());
      } catch (Exception ex) {
        LOGGER.warn("DeleteEntitiesCron : deleteDatasets : Exception:", ex);
      }
    }
    LOGGER.debug("Dataset Deleted successfully : Deleted datasets count {}", datasetIds.isEmpty());
  }

  private void deleteDatasetVersions(Session session) {
    LOGGER.trace("DatasetVersion deleting");
    String alias = "dv";
    String deleteDatasetVersionsQueryString =
        new StringBuilder("FROM ")
            .append(DatasetVersionEntity.class.getSimpleName())
            .append(" ")
            .append(alias)
            .append(" WHERE ")
            .append(alias)
            .append(".")
            .append(ModelDBConstants.DELETED)
            .append(" = :deleted ")
            .toString();
    Query datasetVersionDeleteQuery = session.createQuery(deleteDatasetVersionsQueryString);
    datasetVersionDeleteQuery.setParameter("deleted", true);
    List<DatasetVersionEntity> datasetVersionEntities = datasetVersionDeleteQuery.list();

    if (!datasetVersionEntities.isEmpty()) {
      try {
        roleService.deleteEntityResourcesWithServiceUser(
            datasetVersionEntities.stream()
                .map(DatasetVersionEntity::getId)
                .collect(Collectors.toList()),
            ModelDBServiceResourceTypes.DATASET_VERSION);
        for (DatasetVersionEntity datasetVersionEntity : datasetVersionEntities) {
          try {
            Transaction transaction = session.beginTransaction();
            session.delete(datasetVersionEntity);
            transaction.commit();
          } catch (OptimisticLockException ex) {
            LOGGER.info(
                "DeleteEntitiesCron : deleteDatasetVersions : Exception: {}", ex.getMessage());
          }
        }
      } catch (OptimisticLockException ex) {
        LOGGER.info("DeleteEntitiesCron : deleteDatasetVersions : Exception: {}", ex.getMessage());
      } catch (Exception ex) {
        LOGGER.warn("DeleteEntitiesCron : deleteDatasetVersions : Exception:", ex);
      }
    }
    LOGGER.debug(
        "DatasetVersion Deleted successfully : Deleted datasetVersions count {}",
        datasetVersionEntities.size());
  }

  private void deleteRepositories(Session session) {
    LOGGER.trace("Repository deleting");
    String alias = "rp";
    String deleteRepositoriesQueryString =
        new StringBuilder("FROM ")
            .append(RepositoryEntity.class.getSimpleName())
            .append(" ")
            .append(alias)
            .append(" WHERE ")
            .append(alias)
            .append(".")
            .append(ModelDBConstants.DELETED)
            .append(" = :deleted ")
            .toString();
    Query repositoryDeleteQuery = session.createQuery(deleteRepositoriesQueryString);
    repositoryDeleteQuery.setParameter("deleted", true);
    List<RepositoryEntity> repositoryEntities = repositoryDeleteQuery.list();

    if (!repositoryEntities.isEmpty()) {
      for (RepositoryEntity repository : repositoryEntities) {
        Transaction transaction = null;
        try {
          ModelDBServiceResourceTypes modelDBServiceResourceTypes =
              ModelDBUtils.getModelDBServiceResourceTypesFromRepository(repository);
          roleService.deleteEntityResourcesWithServiceUser(
              Collections.singletonList(String.valueOf(repository.getId())),
              modelDBServiceResourceTypes);

          transaction = session.beginTransaction();

          String deleteTagsHql =
              new StringBuilder("DELETE " + TagsEntity.class.getSimpleName() + " te where te.id.")
                  .append(ModelDBConstants.REPOSITORY_ID)
                  .append(" = :repoId ")
                  .toString();
          Query deleteTagsQuery = session.createQuery(deleteTagsHql);
          deleteTagsQuery.setParameter("repoId", repository.getId());
          deleteTagsQuery.executeUpdate();

          deleteLabels(
              session, String.valueOf(repository.getId()), IDTypeEnum.IDType.VERSIONING_REPOSITORY);

          String getRepositoryBranchesHql =
              new StringBuilder("From ")
                  .append(BranchEntity.class.getSimpleName())
                  .append(" br where br.id.")
                  .append(ModelDBConstants.REPOSITORY_ID)
                  .append(" = :repoId ")
                  .toString();
          Query query = session.createQuery(getRepositoryBranchesHql);
          query.setParameter("repoId", repository.getId());
          List<BranchEntity> branchEntities = query.list();

          List<String> branches =
              branchEntities.stream()
                  .map(branchEntity -> branchEntity.getId().getBranch())
                  .collect(Collectors.toList());

          if (!branches.isEmpty()) {
            String deleteBranchesHQL =
                "DELETE FROM "
                    + BranchEntity.class.getSimpleName()
                    + " br where br.id.repository_id = :repositoryId AND br.id.branch IN (:branches)";
            Query deleteBranchQuery = session.createQuery(deleteBranchesHQL);
            deleteBranchQuery.setParameter("repositoryId", repository.getId());
            deleteBranchQuery.setParameterList("branches", branches);
            deleteBranchQuery.executeUpdate();
          }

          StringBuilder commitQueryBuilder =
              new StringBuilder(
                  "SELECT cm FROM "
                      + CommitEntity.class.getSimpleName()
                      + " cm LEFT JOIN cm.repository repo WHERE repo.id = :repoId ");
          Query<CommitEntity> commitEntityQuery =
              session.createQuery(
                  commitQueryBuilder.append(" ORDER BY cm.date_created DESC").toString());
          commitEntityQuery.setParameter("repoId", repository.getId());
          List<CommitEntity> commitEntities = commitEntityQuery.list();

          commitEntities.forEach(
              commitEntity -> {
                if (commitEntity.getRepository().contains(repository)) {
                  commitEntity.getRepository().remove(repository);
                  if (commitEntity.getRepository().isEmpty()) {
                    if (repository.isDataset()) {
                      String compositeId =
                          VersioningUtils.getVersioningCompositeId(
                              repository.getId(),
                              commitEntity.getCommit_hash(),
                              Collections.singletonList(
                                  ModelDBConstants.DEFAULT_VERSIONING_BLOB_LOCATION));
                      deleteLabels(
                          session, compositeId, IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB);
                      deleteAttribute(session, compositeId);
                    } else {
                      deleteLabels(
                          session,
                          commitEntity.getCommit_hash(),
                          IDTypeEnum.IDType.VERSIONING_COMMIT);
                    }
                    deleteTagEntities(session, repository.getId(), commitEntity.getCommit_hash());
                    session.delete(commitEntity);
                  } else {
                    session.update(commitEntity);
                  }
                }
              });
          session.delete(repository);
          transaction.commit();
        } catch (OptimisticLockException ex) {
          LOGGER.info("DeleteEntitiesCron : deleteRepositories : Exception: {}", ex.getMessage());
          if (transaction != null && transaction.getStatus().canRollback()) {
            transaction.rollback();
          }
        } catch (Exception ex) {
          LOGGER.warn("DeleteEntitiesCron : deleteRepositories : Exception: ", ex);
          if (transaction != null && transaction.getStatus().canRollback()) {
            transaction.rollback();
          }
        }
      }
    }
    LOGGER.debug(
        "Repository Deleted successfully : Deleted repositories count {}",
        repositoryEntities.size());
  }

  public static void deleteLabels(Session session, Object entityHash, IDTypeEnum.IDType idType) {
    String deleteLabelsQueryString =
        new StringBuilder("DELETE LabelsMappingEntity lm where lm.id.")
            .append(ModelDBConstants.ENTITY_HASH)
            .append(" = :entityHash ")
            .append(" AND lm.id.")
            .append(ModelDBConstants.ENTITY_TYPE)
            .append(" = :entityType")
            .toString();
    Query deleteLabelsQuery =
        session
            .createQuery(deleteLabelsQueryString)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    deleteLabelsQuery.setParameter("entityHash", entityHash);
    deleteLabelsQuery.setParameter("entityType", idType.getNumber());
    deleteLabelsQuery.executeUpdate();
  }

  public static void deleteAttribute(Session session, String entityHash) {
    String deleteAllAttributes =
        new StringBuilder("delete from AttributeEntity at WHERE at.")
            .append(ModelDBConstants.ENTITY_HASH)
            .append(" = :entityHash")
            .append(" AND at.entity_name ")
            .append(" = :entityName")
            .toString();
    Query deleteLabelsQuery =
        session
            .createQuery(deleteAllAttributes)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    deleteLabelsQuery.setParameter("entityHash", entityHash);
    deleteLabelsQuery.setParameter("entityName", ModelDBConstants.BLOB);
    deleteLabelsQuery.executeUpdate();
  }

  private static void deleteTagEntities(Session session, Long repoId, String commitHash) {
    String getTagsHql =
        "From TagsEntity te where te.id."
            + ModelDBConstants.REPOSITORY_ID
            + " = :repoId "
            + " AND te.commit_hash = :commitHash";
    Query<TagsEntity> getTagsQuery = session.createQuery(getTagsHql, TagsEntity.class);
    getTagsQuery.setParameter("repoId", repoId);
    getTagsQuery.setParameter("commitHash", commitHash);
    List<TagsEntity> tagsEntities = getTagsQuery.list();
    tagsEntities.forEach(session::delete);
  }
}
