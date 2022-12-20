package ai.verta.modeldb.cron_jobs;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.MDBRoleService;
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
  private static final String DELETED_KEY = "deleted";
  private static final String REPO_ID_KEY = "repoId";
  private final ModelDBHibernateUtil modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
  private final AuthService authService;
  private final MDBRoleService mdbRoleService;
  private final Integer recordUpdateLimit;

  public DeleteEntitiesCron(
      AuthService authService, MDBRoleService mdbRoleService, Integer recordUpdateLimit) {
    this.authService = authService;
    this.mdbRoleService = mdbRoleService;
    this.recordUpdateLimit = recordUpdateLimit;
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("DeleteEntitiesCron wakeup");

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
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
    var projectDeleteQuery =
        session.createQuery("FROM ProjectEntity pr WHERE pr.deleted = :deleted ");
    projectDeleteQuery.setParameter(DELETED_KEY, true);
    projectDeleteQuery.setMaxResults(this.recordUpdateLimit);
    List<ProjectEntity> projectEntities = projectDeleteQuery.list();

    List<String> projectIds = new ArrayList<>();
    if (!projectEntities.isEmpty()) {
      for (ProjectEntity projectEntity : projectEntities) {
        projectIds.add(projectEntity.getId());
      }

      try {
        mdbRoleService.deleteEntityResourcesWithServiceUser(
            projectIds, ModelDBServiceResourceTypes.PROJECT);
        var transaction = session.beginTransaction();
        var updateDeletedStatusExperimentQueryString =
            "UPDATE ExperimentEntity exp SET exp.deleted = :deleted WHERE exp.project_id IN (:projectIds)";
        var deletedExperimentQuery = session.createQuery(updateDeletedStatusExperimentQueryString);
        deletedExperimentQuery.setParameter(DELETED_KEY, true);
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
    var experimentDeleteQuery =
        session.createQuery("FROM ExperimentEntity ex WHERE ex.deleted = :deleted ");
    experimentDeleteQuery.setParameter(DELETED_KEY, true);
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
        var transaction = session.beginTransaction();
        var updateDeletedStatusExperimentRunQueryString =
            "UPDATE ExperimentRunEntity expr SET expr.deleted = :deleted WHERE expr.experiment_id IN (:experimentIds)";
        var deletedExperimentRunQuery =
            session.createQuery(updateDeletedStatusExperimentRunQueryString);
        deletedExperimentRunQuery.setParameter(DELETED_KEY, true);
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
          mdbRoleService.buildRoleBindingName(
              ModelDBConstants.ROLE_EXPERIMENT_OWNER,
              experimentEntity.getId(),
              experimentEntity.getOwner(),
              ModelDBServiceResourceTypes.EXPERIMENT.name());
      if (ownerRoleBindingName != null) {
        roleBindingNames.add(ownerRoleBindingName);
      }
    }
    if (!roleBindingNames.isEmpty()) {
      mdbRoleService.deleteRoleBindingsUsingServiceUser(roleBindingNames);
    }
  }

  private void deleteExperimentRuns(Session session) {
    LOGGER.trace("ExperimentRun deleting");
    var experimentRunDeleteQuery =
        session.createQuery("FROM ExperimentRunEntity expr WHERE expr.deleted = :deleted ");
    experimentRunDeleteQuery.setParameter(DELETED_KEY, true);
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
        var transaction = session.beginTransaction();
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
          mdbRoleService.buildRoleBindingName(
              ModelDBConstants.ROLE_EXPERIMENT_RUN_OWNER,
              experimentRunEntity.getId(),
              experimentRunEntity.getOwner(),
              ModelDBServiceResourceTypes.EXPERIMENT_RUN.name());
      if (ownerRoleBindingName != null) {
        roleBindingNames.add(ownerRoleBindingName);
      }
    }
    if (!roleBindingNames.isEmpty()) {
      mdbRoleService.deleteRoleBindingsUsingServiceUser(roleBindingNames);
    }
  }

  private void removeEntityComments(Session session, List<String> entityIds, String entityName) {
    var commentDeleteHql =
        "From CommentEntity ce where ce.entity_id IN (:entityIds) AND ce.entity_name =:entityName";
    var commentDeleteQuery = session.createQuery(commentDeleteHql);
    commentDeleteQuery.setParameterList("entityIds", entityIds);
    commentDeleteQuery.setParameter("entityName", entityName);
    List<CommentEntity> commentEntities = commentDeleteQuery.list();
    for (CommentEntity commentEntity : commentEntities) {
      session.delete(commentEntity);
    }
  }

  private void deleteDatasets(Session session) {
    LOGGER.trace("Dataset deleting");
    var datasetDeleteQuery =
        session.createQuery("FROM DatasetEntity dt WHERE dt.deleted = :deleted ");
    datasetDeleteQuery.setParameter(DELETED_KEY, true);
    datasetDeleteQuery.setMaxResults(this.recordUpdateLimit);
    List<DatasetEntity> datasetEntities = datasetDeleteQuery.list();

    List<String> datasetIds = new ArrayList<>();
    if (!datasetEntities.isEmpty()) {
      for (DatasetEntity datasetEntity : datasetEntities) {
        datasetIds.add(datasetEntity.getId());
      }

      try {
        mdbRoleService.deleteEntityResourcesWithServiceUser(
            datasetIds, ModelDBServiceResourceTypes.DATASET);
        var transaction = session.beginTransaction();
        var updateDeletedStatusDatasetVersionQueryString =
            "UPDATE DatasetVersionEntity dv SET dv.deleted = :deleted WHERE dv.dataset_id IN (:datasetIds)";
        var deletedDatasetVersionQuery =
            session.createQuery(updateDeletedStatusDatasetVersionQueryString);
        deletedDatasetVersionQuery.setParameter(DELETED_KEY, true);
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
    var datasetVersionDeleteQuery =
        session.createQuery("FROM DatasetVersionEntity dv WHERE dv.deleted = :deleted ");
    datasetVersionDeleteQuery.setParameter(DELETED_KEY, true);
    List<DatasetVersionEntity> datasetVersionEntities = datasetVersionDeleteQuery.list();

    if (!datasetVersionEntities.isEmpty()) {
      try {
        mdbRoleService.deleteEntityResourcesWithServiceUser(
            datasetVersionEntities.stream()
                .map(DatasetVersionEntity::getId)
                .collect(Collectors.toList()),
            ModelDBServiceResourceTypes.DATASET_VERSION);
        for (DatasetVersionEntity datasetVersionEntity : datasetVersionEntities) {
          try {
            var transaction = session.beginTransaction();
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
    var repositoryDeleteQuery =
        session.createQuery("FROM RepositoryEntity rp WHERE rp.deleted = :deleted ");
    repositoryDeleteQuery.setParameter(DELETED_KEY, true);
    List<RepositoryEntity> repositoryEntities = repositoryDeleteQuery.list();

    if (!repositoryEntities.isEmpty()) {
      for (RepositoryEntity repository : repositoryEntities) {
        Transaction transaction = null;
        try {
          var modelDBServiceResourceTypes =
              ModelDBUtils.getModelDBServiceResourceTypesFromRepository(repository);
          mdbRoleService.deleteEntityResourcesWithServiceUser(
              Collections.singletonList(String.valueOf(repository.getId())),
              modelDBServiceResourceTypes);

          transaction = session.beginTransaction();

          var deleteTagsHql = "DELETE TagsEntity te where te.id.repository_id = :repoId ";
          var deleteTagsQuery = session.createQuery(deleteTagsHql);
          deleteTagsQuery.setParameter(REPO_ID_KEY, repository.getId());
          deleteTagsQuery.executeUpdate();

          deleteLabels(
              session, String.valueOf(repository.getId()), IDTypeEnum.IDType.VERSIONING_REPOSITORY);

          var getRepositoryBranchesHql =
              "From BranchEntity br where br.id.repository_id = :repoId ";
          var query = session.createQuery(getRepositoryBranchesHql);
          query.setParameter(REPO_ID_KEY, repository.getId());
          List<BranchEntity> branchEntities = query.list();

          List<String> branches =
              branchEntities.stream()
                  .map(branchEntity -> branchEntity.getId().getBranch())
                  .collect(Collectors.toList());

          if (!branches.isEmpty()) {
            var deleteBranchesHQL =
                "DELETE FROM BranchEntity br where br.id.repository_id = :repositoryId AND br.id.branch IN (:branches)";
            var deleteBranchQuery = session.createQuery(deleteBranchesHQL);
            deleteBranchQuery.setParameter("repositoryId", repository.getId());
            deleteBranchQuery.setParameterList("branches", branches);
            deleteBranchQuery.executeUpdate();
          }

          var commitQuery =
              "SELECT cm FROM CommitEntity cm LEFT JOIN cm.repository repo WHERE repo.id = :repoId ORDER BY cm.date_created DESC";
          Query<CommitEntity> commitEntityQuery = session.createQuery(commitQuery);
          commitEntityQuery.setParameter(REPO_ID_KEY, repository.getId());
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
    var deleteLabelsQueryString =
        "DELETE LabelsMappingEntity lm where lm.id.entity_hash = :entityHash AND lm.id.entity_type = :entityType";
    var deleteLabelsQuery =
        session
            .createQuery(deleteLabelsQueryString)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    deleteLabelsQuery.setParameter("entityHash", entityHash);
    deleteLabelsQuery.setParameter("entityType", idType.getNumber());
    deleteLabelsQuery.executeUpdate();
  }

  public static void deleteAttribute(Session session, String entityHash) {
    var deleteAllAttributes =
        "delete from AttributeEntity at WHERE at.entity_hash = :entityHash AND at.entity_name = :entityName";
    var deleteLabelsQuery =
        session
            .createQuery(deleteAllAttributes)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    deleteLabelsQuery.setParameter("entityHash", entityHash);
    deleteLabelsQuery.setParameter("entityName", ModelDBConstants.BLOB);
    deleteLabelsQuery.executeUpdate();
  }

  private static void deleteTagEntities(Session session, Long repoId, String commitHash) {
    String getTagsHql =
        "From TagsEntity te where te.id.repository_id = :repoId AND te.commit_hash = :commitHash";
    Query<TagsEntity> getTagsQuery = session.createQuery(getTagsHql, TagsEntity.class);
    getTagsQuery.setParameter(REPO_ID_KEY, repoId);
    getTagsQuery.setParameter("commitHash", commitHash);
    List<TagsEntity> tagsEntities = getTagsQuery.list();
    tagsEntities.forEach(session::delete);
  }
}
