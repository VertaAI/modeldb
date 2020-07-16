package ai.verta.modeldb.cron_jobs;

import static ai.verta.modeldb.authservice.AuthServiceChannel.isBackgroundUtilsCall;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.WorkspaceTypeEnum;
import ai.verta.modeldb.DatasetVisibilityEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorOrg;
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
import com.google.rpc.Code;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class DeleteEntitiesCron extends TimerTask {
  private static final Logger LOGGER = LogManager.getLogger(DeleteEntitiesCron.class);
  private final AuthService authService;
  private final RoleService roleService;
  private final Integer recordUpdateLimit;
  private static final String DATASET_GLOBAL_SHARING = "_DATASET_GLOBAL_SHARING";
  private static final String REPOSITORY_GLOBAL_SHARING = "_REPO_GLOBAL_SHARING";

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

    isBackgroundUtilsCall = true;
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      // Update project timestamp
      deleteProjects(session);

      // Update experiment timestamp
      deleteExperiments(session);

      // Update experimentRun timestamp
      deleteExperimentRuns(session);

      // Update dataset timestamp
      deleteDatasets(session);

      // Update datasetVersion timestamp
      deleteDatasetVersions(session);

      // Update repository timestamp
      deleteRepositories(session);
    } catch (Exception ex) {
      if (ex instanceof StatusRuntimeException) {
        StatusRuntimeException exception = (StatusRuntimeException) ex;
        if (exception.getStatus().getCode().value() == Code.PERMISSION_DENIED_VALUE) {
          LOGGER.error("DeleteEntitiesCron Exception: {}", ex.getMessage());
        } else {
          LOGGER.error("DeleteEntitiesCron Exception: ", ex);
        }
      } else {
        LOGGER.error("DeleteEntitiesCron Exception: ", ex);
      }
    }
    isBackgroundUtilsCall = false;
    LOGGER.info("DeleteEntitiesCron finish tasks and reschedule");
  }

  private void deleteProjects(Session session) {
    LOGGER.debug("Project deleting");
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
    LOGGER.debug("Project delete query: {}", projectDeleteQuery.getQueryString());
    List<ProjectEntity> projectEntities = projectDeleteQuery.list();

    List<String> projectIds = new ArrayList<>();
    if (!projectEntities.isEmpty()) {
      for (ProjectEntity projectEntity : projectEntities) {
        projectIds.add(projectEntity.getId());
      }
      try {
        deleteRoleBindingsForProjects(projectEntities);
      } catch (Exception ex) {
        LOGGER.error(
            "DeleteEntitiesCron : deleteProjects : deleteRoleBindingsForProjects : Exception: {}",
            ex.getMessage());
      }

      try {
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

        for (ProjectEntity projectEntity : projectEntities) {
          session.delete(projectEntity);
        }
        transaction.commit();
      } catch (Exception ex) {
        LOGGER.error("DeleteEntitiesCron : deleteProjects : Exception: ", ex);
      }
    }

    LOGGER.debug("Project Deleted successfully : Deleted projects count {}", projectIds.size());
  }

  private void deleteRoleBindingsForProjects(List<ProjectEntity> projectEntities) {
    // set roleBindings name by accessible projects
    List<String> roleBindingNames = new LinkedList<>();
    setRoleBindingsNameOfAccessibleProjectsInRoleBindingNamesList(
        projectEntities, roleBindingNames);
    LOGGER.debug("num bindings after Projects {}", roleBindingNames.size());

    // Delete all resources
    roleService.deleteAllResources(
        projectEntities.stream().map(ProjectEntity::getId).collect(Collectors.toList()),
        ModelDBServiceResourceTypes.PROJECT);

    // Remove all role bindings
    if (!roleBindingNames.isEmpty()) {
      roleService.deleteRoleBindings(roleBindingNames);
    }
  }

  private void setRoleBindingsNameOfAccessibleProjectsInRoleBindingNamesList(
      List<ProjectEntity> allowedProjects, List<String> roleBindingNames) {
    for (ProjectEntity project : allowedProjects) {
      String projectId = project.getId();

      String ownerRoleBindingName =
          roleService.buildRoleBindingName(
              ModelDBConstants.ROLE_PROJECT_OWNER,
              project.getId(),
              project.getOwner(),
              ModelDBServiceResourceTypes.PROJECT.name());
      if (ownerRoleBindingName != null) {
        roleBindingNames.add(ownerRoleBindingName);
      }

      if (project.getProject_visibility() == ProjectVisibility.PUBLIC.getNumber()) {
        String publicReadRoleBindingName =
            roleService.buildPublicRoleBindingName(projectId, ModelDBServiceResourceTypes.PROJECT);
        if (publicReadRoleBindingName != null) {
          roleBindingNames.add(publicReadRoleBindingName);
        }
      }

      // Delete workspace based roleBindings
      List<String> workspaceRoleBindingNames =
          roleService.getWorkspaceRoleBindings(
              project.getWorkspace(),
              WorkspaceTypeEnum.WorkspaceType.forNumber(project.getWorkspace_type()),
              projectId,
              ModelDBConstants.ROLE_PROJECT_ADMIN,
              ModelDBServiceResourceTypes.PROJECT,
              ProjectVisibility.forNumber(project.getProject_visibility())
                  .equals(ProjectVisibility.ORG_SCOPED_PUBLIC),
              "_GLOBAL_SHARING");
      roleBindingNames.addAll(workspaceRoleBindingNames);
    }
  }

  private void deleteExperiments(Session session) {
    LOGGER.debug("Experiment deleting");
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
    LOGGER.debug("Experiment delete query: {}", experimentDeleteQuery.getQueryString());
    List<ExperimentEntity> experimentEntities = experimentDeleteQuery.list();

    List<String> experimentIds = new ArrayList<>();
    if (!experimentEntities.isEmpty()) {
      for (ExperimentEntity experimentEntity : experimentEntities) {
        experimentIds.add(experimentEntity.getId());
      }

      try {
        deleteRoleBindingsForExperiments(experimentEntities);
      } catch (Exception ex) {
        LOGGER.error(
            "DeleteEntitiesCron : deleteExperiments : deleteRoleBindingsForExperiments : Exception: {}",
            ex.getMessage());
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

        for (ExperimentEntity experimentEntity : experimentEntities) {
          session.delete(experimentEntity);
        }
        transaction.commit();
      } catch (Exception ex) {
        LOGGER.error("DeleteEntitiesCron : deleteExperiments : Exception:", ex);
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
      roleService.deleteRoleBindings(roleBindingNames);
    }
  }

  private void deleteExperimentRuns(Session session) {
    LOGGER.debug("ExperimentRun deleting");
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
    LOGGER.debug("ExperimentRun delete query: {}", experimentRunDeleteQuery.getQueryString());
    List<ExperimentRunEntity> experimentRunEntities = experimentRunDeleteQuery.list();

    List<String> experimentRunIds = new ArrayList<>();
    if (!experimentRunEntities.isEmpty()) {
      for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
        experimentRunIds.add(experimentRunEntity.getId());
      }
      try {
        deleteRoleBindingsForExperimentRuns(experimentRunEntities);
      } catch (Exception ex) {
        LOGGER.error(
            "DeleteEntitiesCron : deleteExperimentRuns : deleteRoleBindingsForExperimentRuns : Exception: {}",
            ex.getMessage());
      }

      try {
        Transaction transaction = session.beginTransaction();
        // Delete the ExperimentRun comments
        if (!experimentRunIds.isEmpty()) {
          removeEntityComments(
              session, experimentRunIds, ExperimentRunEntity.class.getSimpleName());
        }

        for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
          session.delete(experimentRunEntity);
        }
        transaction.commit();
      } catch (Exception ex) {
        LOGGER.error("DeleteEntitiesCron : deleteExperimentRuns : Exception:", ex);
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
      roleService.deleteRoleBindings(roleBindingNames);
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
    LOGGER.debug("Comments delete query : {}", commentDeleteQuery.getQueryString());
    List<CommentEntity> commentEntities = commentDeleteQuery.list();
    for (CommentEntity commentEntity : commentEntities) {
      session.delete(commentEntity);
    }
  }

  private void deleteDatasets(Session session) {
    LOGGER.debug("Dataset deleting");
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
    LOGGER.debug("Dataset delete query: {}", datasetDeleteQuery.getQueryString());
    List<DatasetEntity> datasetEntities = datasetDeleteQuery.list();

    List<String> datasetIds = new ArrayList<>();
    if (!datasetEntities.isEmpty()) {
      for (DatasetEntity datasetEntity : datasetEntities) {
        datasetIds.add(datasetEntity.getId());
      }
      try {
        deleteRoleBindingsForDatasets(datasetEntities);
      } catch (Exception ex) {
        LOGGER.error(
            "DeleteEntitiesCron : deleteDatasets : deleteRoleBindingsForDatasets : Exception: {}",
            ex.getMessage());
      }

      try {
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

        for (DatasetEntity datasetEntity : datasetEntities) {
          session.delete(datasetEntity);
        }
        transaction.commit();
      } catch (Exception ex) {
        LOGGER.error("DeleteEntitiesCron : deleteDatasets : Exception:", ex);
      }
    }
    LOGGER.debug("Dataset Deleted successfully : Deleted datasets count {}", datasetIds.isEmpty());
  }

  private void deleteRoleBindingsForDatasets(List<DatasetEntity> datasetEntities) {
    // Remove roleBindings by accessible datasets
    List<String> roleBindingNames = new LinkedList<>();
    setRoleBindingsNameOfAccessibleDatasetsInRoleBindingsList(datasetEntities, roleBindingNames);
    LOGGER.debug("num bindings after Datasets {}", roleBindingNames.size());

    // Remove all datasetEntity collaborators
    roleService.deleteAllResources(
        datasetEntities.stream().map(DatasetEntity::getId).collect(Collectors.toList()),
        ModelDBServiceResourceTypes.DATASET);

    // Remove all role bindings
    if (!roleBindingNames.isEmpty()) {
      roleService.deleteRoleBindings(roleBindingNames);
    }
  }

  private void setRoleBindingsNameOfAccessibleDatasetsInRoleBindingsList(
      List<DatasetEntity> allowedDatasets, List<String> roleBindingNames) {
    for (DatasetEntity datasetEntity : allowedDatasets) {
      String datasetId = datasetEntity.getId();

      String ownerRoleBindingName =
          roleService.buildRoleBindingName(
              ModelDBConstants.ROLE_DATASET_OWNER,
              datasetEntity.getId(),
              datasetEntity.getOwner(),
              ModelDBServiceResourceTypes.DATASET.name());
      if (ownerRoleBindingName != null) {
        roleBindingNames.add(ownerRoleBindingName);
      }

      if (datasetEntity.getDataset_visibility()
          == DatasetVisibilityEnum.DatasetVisibility.PUBLIC.getNumber()) {
        String publicReadRoleBindingName =
            roleService.buildPublicRoleBindingName(datasetId, ModelDBServiceResourceTypes.DATASET);
        if (publicReadRoleBindingName != null && !publicReadRoleBindingName.isEmpty()) {
          roleBindingNames.add(publicReadRoleBindingName);
        }
      }

      // Delete workspace based roleBindings
      List<String> workspaceRoleBindingNames =
          getWorkspaceRoleBindingsForDataset(
              datasetEntity.getWorkspace(),
              WorkspaceTypeEnum.WorkspaceType.forNumber(datasetEntity.getWorkspace_type()),
              datasetEntity.getId(),
              DatasetVisibilityEnum.DatasetVisibility.forNumber(
                  datasetEntity.getDataset_visibility()));
      if (!workspaceRoleBindingNames.isEmpty()) {
        roleBindingNames.addAll(workspaceRoleBindingNames);
      }
    }
  }

  private List<String> getWorkspaceRoleBindingsForDataset(
      String workspaceId,
      WorkspaceTypeEnum.WorkspaceType workspaceType,
      String datasetId,
      DatasetVisibilityEnum.DatasetVisibility datasetVisibility) {
    List<String> workspaceRoleBindings = new ArrayList<>();
    if (workspaceId != null && !workspaceId.isEmpty()) {
      switch (workspaceType) {
        case ORGANIZATION:
          if (datasetVisibility.equals(DatasetVisibilityEnum.DatasetVisibility.ORG_SCOPED_PUBLIC)) {
            String orgDatasetReadRoleBindingName =
                roleService.buildRoleBindingName(
                    ModelDBConstants.ROLE_DATASET_READ_ONLY,
                    datasetId,
                    new CollaboratorOrg(workspaceId),
                    ModelDBServiceResourceTypes.DATASET.name());
            if (orgDatasetReadRoleBindingName != null && !orgDatasetReadRoleBindingName.isEmpty()) {
              workspaceRoleBindings.add(orgDatasetReadRoleBindingName);
            }
          }
          break;
        case USER:
        default:
          break;
      }
    }
    List<String> orgWorkspaceRoleBindings =
        roleService.getWorkspaceRoleBindings(
            workspaceId,
            workspaceType,
            datasetId,
            ModelDBConstants.ROLE_DATASET_ADMIN,
            ModelDBServiceResourceTypes.DATASET,
            datasetVisibility.equals(DatasetVisibilityEnum.DatasetVisibility.ORG_SCOPED_PUBLIC),
            DATASET_GLOBAL_SHARING);

    if (orgWorkspaceRoleBindings != null && !orgWorkspaceRoleBindings.isEmpty()) {
      workspaceRoleBindings.addAll(orgWorkspaceRoleBindings);
    }
    return workspaceRoleBindings;
  }

  private void deleteDatasetVersions(Session session) {
    LOGGER.debug("DatasetVersion deleting");
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
    LOGGER.debug("DatasetVersion delete query: {}", datasetVersionDeleteQuery.getQueryString());
    List<DatasetVersionEntity> datasetVersionEntities = datasetVersionDeleteQuery.list();

    try {
      // Remove all role bindings
      deleteRoleBindingsForDatasetVersions(datasetVersionEntities);
    } catch (Exception ex) {
      LOGGER.error(
          "DeleteEntitiesCron : deleteDatasetVersions : deleteRoleBindingsForDatasetVersions : Exception: {}",
          ex.getMessage());
    }

    try {
      Transaction transaction = session.beginTransaction();
      for (DatasetVersionEntity datasetVersionEntity : datasetVersionEntities) {
        session.delete(datasetVersionEntity);
      }
      transaction.commit();
    } catch (Exception ex) {
      LOGGER.error("DeleteEntitiesCron : deleteDatasetVersions : Exception:", ex);
    }
    LOGGER.debug(
        "DatasetVersion Deleted successfully : Deleted datasetVersions count {}",
        datasetVersionEntities.size());
  }

  private void deleteRoleBindingsForDatasetVersions(
      List<DatasetVersionEntity> datasetVersionEntities) {
    // Remove roleBindings by accessible datasetVersions
    List<String> roleBindingNames = new LinkedList<>();
    for (DatasetVersionEntity datasetVersionEntity : datasetVersionEntities) {
      String ownerRoleBindingName =
          roleService.buildRoleBindingName(
              ModelDBConstants.ROLE_DATASET_VERSION_OWNER,
              datasetVersionEntity.getId(),
              datasetVersionEntity.getOwner(),
              ModelDBServiceResourceTypes.DATASET_VERSION.name());
      if (ownerRoleBindingName != null && !ownerRoleBindingName.isEmpty()) {
        roleBindingNames.add(ownerRoleBindingName);
      }
    }
    // Remove all role bindings
    if (!roleBindingNames.isEmpty()) {
      roleService.deleteRoleBindings(roleBindingNames);
    }
  }

  private void deleteRepositories(Session session) {
    LOGGER.debug("Repository deleting");
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
    LOGGER.debug("Repository delete query: {}", repositoryDeleteQuery.getQueryString());
    List<RepositoryEntity> repositoryEntities = repositoryDeleteQuery.list();

    if (!repositoryEntities.isEmpty()) {
      for (RepositoryEntity repository : repositoryEntities) {
        try {
          deleteRoleBindingsOfRepositories(Collections.singletonList(repository));
        } catch (Exception ex) {
          LOGGER.error(
              "DeleteEntitiesCron : deleteRepositories : deleteRoleBindingsOfRepositories : Exception: {}",
              ex.getMessage());
        }

        try {
          Transaction transaction = session.beginTransaction();
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
                    deleteLabels(
                        session,
                        commitEntity.getCommit_hash(),
                        IDTypeEnum.IDType.VERSIONING_COMMIT);
                    deleteTagEntities(session, repository.getId(), commitEntity.getCommit_hash());
                    session.delete(commitEntity);
                  } else {
                    session.update(commitEntity);
                  }
                }
              });
          session.delete(repository);
          transaction.commit();
        } catch (Exception ex) {
          LOGGER.error("DeleteEntitiesCron : deleteRepositories : Exception: ", ex);
        }
      }
    }
    LOGGER.debug(
        "Repository Deleted successfully : Deleted repositories count {}",
        repositoryEntities.size());
  }

  private void deleteLabels(Session session, Object entityHash, IDTypeEnum.IDType idType) {
    String deleteLabelsQueryString =
        new StringBuilder("DELETE LabelsMappingEntity lm where lm.id.")
            .append(ModelDBConstants.ENTITY_HASH)
            .append(" = :entityHash ")
            .append(" AND lm.id.")
            .append(ModelDBConstants.ENTITY_TYPE)
            .append(" = :entityType")
            .toString();
    Query deleteLabelsQuery = session.createQuery(deleteLabelsQueryString);
    deleteLabelsQuery.setParameter("entityHash", entityHash);
    deleteLabelsQuery.setParameter("entityType", idType.getNumber());
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

  private void deleteRoleBindingsOfRepositories(List<RepositoryEntity> allowedResources) {
    final List<String> roleBindingNames = Collections.synchronizedList(new ArrayList<>());
    for (RepositoryEntity repositoryEntity : allowedResources) {
      String ownerRoleBindingName =
          roleService.buildRoleBindingName(
              ModelDBConstants.ROLE_REPOSITORY_OWNER,
              String.valueOf(repositoryEntity.getId()),
              repositoryEntity.getOwner(),
              ModelDBServiceResourceTypes.REPOSITORY.name());
      if (ownerRoleBindingName != null) {
        roleBindingNames.add(ownerRoleBindingName);
      }

      // Delete workspace based roleBindings
      List<String> repoOrgWorkspaceRoleBindings =
          roleService.getWorkspaceRoleBindings(
              repositoryEntity.getWorkspace_id(),
              WorkspaceTypeEnum.WorkspaceType.forNumber(repositoryEntity.getWorkspace_type()),
              String.valueOf(repositoryEntity.getId()),
              ModelDBConstants.ROLE_REPOSITORY_ADMIN,
              ModelDBServiceResourceTypes.REPOSITORY,
              repositoryEntity
                  .getRepository_visibility()
                  .equals(DatasetVisibilityEnum.DatasetVisibility.ORG_SCOPED_PUBLIC_VALUE),
              REPOSITORY_GLOBAL_SHARING);
      if (!repoOrgWorkspaceRoleBindings.isEmpty()) {
        roleBindingNames.addAll(repoOrgWorkspaceRoleBindings);
      }
    }
    // Remove all repositoryEntity collaborators
    roleService.deleteAllResources(
        allowedResources.stream()
            .map(repositoryEntity -> String.valueOf(repositoryEntity.getId()))
            .collect(Collectors.toList()),
        ModelDBServiceResourceTypes.REPOSITORY);

    // Remove all role bindings
    if (!roleBindingNames.isEmpty()) {
      roleService.deleteRoleBindings(roleBindingNames);
    }
  }
}
