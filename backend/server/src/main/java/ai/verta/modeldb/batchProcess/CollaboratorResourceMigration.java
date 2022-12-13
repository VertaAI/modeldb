package ai.verta.modeldb.batchProcess;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.VisibilityEnum;
import ai.verta.common.WorkspaceTypeEnum;
import ai.verta.modeldb.App;
import ai.verta.modeldb.DatasetVisibilityEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.MDBAuthServiceUtils;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.authservice.MDBRoleServiceUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.UserInfo;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class CollaboratorResourceMigration {
  private static final Logger LOGGER = LogManager.getLogger(CollaboratorResourceMigration.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private static final String REPOSITORY_GLOBAL_SHARING = "_REPO_GLOBAL_SHARING";
  private static final String VISIBILITY_MIGRATION = "visibility_migration";
  private static final String CREATED = "created";
  private static AuthService authService;
  private static UAC uac;
  private static MDBRoleService mdbRoleService;
  private static int paginationSize;

  public static void execute() {
    var config = App.getInstance().mdbConfig;
    CollaboratorResourceMigration.paginationSize = 100;
    if (config.hasAuth()) {
      uac = UAC.fromConfig(config);
      authService = MDBAuthServiceUtils.FromConfig(config, uac);
      mdbRoleService = MDBRoleServiceUtils.FromConfig(config, authService, uac);
    } else {
      LOGGER.debug("AuthService Host & Port not found, OSS setup found");
      return;
    }

    LOGGER.info("Migration start");
    migrateProjects();
    LOGGER.info("Projects done migration");
    migrateRepositories();
    LOGGER.info("Repositories done migration");

    LOGGER.info("Migration End");
  }

  private static void migrateProjects() {
    LOGGER.debug("Projects migration started");
    Long count = getEntityCount(ProjectEntity.class);

    var lowerBound = 0;
    final var pagesize = CollaboratorResourceMigration.paginationSize;
    LOGGER.debug("Total Projects to migrate {}", count);

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var criteriaBuilder = session.getCriteriaBuilder();

      CriteriaQuery<ProjectEntity> criteriaQuery = criteriaBuilder.createQuery(ProjectEntity.class);
      Root<ProjectEntity> root = criteriaQuery.from(ProjectEntity.class);

      CriteriaQuery<ProjectEntity> selectQuery =
          criteriaQuery
              .select(root)
              .where(
                  criteriaBuilder.and(
                      criteriaBuilder.equal(root.get(VISIBILITY_MIGRATION), false),
                      criteriaBuilder.equal(root.get(CREATED), true)));

      TypedQuery<ProjectEntity> typedQuery = session.createQuery(selectQuery);
      Stream<ProjectEntity> projectEntities = typedQuery.getResultStream();
      Iterator<ProjectEntity> iterator = projectEntities.iterator();

      Map<String, UserInfo> userInfoMap = new HashMap<>();

      var counter = 0;
      while (iterator.hasNext()) {
        LOGGER.debug("Migrated Projects: {}/{}", counter, count);
        counter++;

        ProjectEntity project = iterator.next();

        var migrated = false;
        if (project.getOwner() != null && !project.getOwner().isEmpty()) {
          WorkspaceDTO workspaceDTO;
          if (!userInfoMap.containsKey(project.getOwner())) {
            try {
              userInfoMap.putAll(
                  authService.getUserInfoFromAuthServer(
                      Collections.singleton(project.getOwner()), null, null, true));
            } catch (StatusRuntimeException ex) {
              if (ex.getStatus().getCode() == Status.Code.NOT_FOUND) {
                LOGGER.warn("Failed to get user info (skipping) : " + ex.toString());
                continue;
              }
              throw ex;
            }
          }
          try {
            workspaceDTO =
                mdbRoleService.getWorkspaceDTOByWorkspaceIdForServiceUser(
                    userInfoMap.get(project.getOwner()),
                    project.getWorkspace(),
                    project.getWorkspace_type());
          } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.NOT_FOUND) {
              LOGGER.warn("Failed to get workspace (skipping) : " + ex.toString());
              continue;
            }
            throw ex;
          }
          Long owner;
          try {
            owner = Long.parseLong(project.getOwner());
          } catch (NumberFormatException ex) {
            LOGGER.warn("Failed to convert owner (skipping) : " + ex.toString());
            continue;
          }
          // if projectVisibility is not equals to ResourceVisibility.ORG_SCOPED_PUBLIC then
          // ignore the CollaboratorType
          try {
            mdbRoleService.createWorkspacePermissions(
                Optional.empty(),
                Optional.of(workspaceDTO.getWorkspaceName()),
                project.getId(),
                project.getName(),
                Optional.of(owner),
                ModelDBServiceResourceTypes.PROJECT,
                CollaboratorPermissions.newBuilder()
                    .setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_ONLY)
                    .build(),
                getResourceVisibility(
                    Optional.ofNullable(
                        WorkspaceTypeEnum.WorkspaceType.forNumber(project.getWorkspace_type())),
                    VisibilityEnum.Visibility.forNumber(project.getProject_visibility())),
                true);
          } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.ALREADY_EXISTS) {
              LOGGER.info(
                  "Resource seem to have already been created (ignoring) : " + ex.toString());
            } else {
              throw ex;
            }
          }
          migrated = true;
        } else {
          List<GetResourcesResponseItem> resources =
              mdbRoleService.getResourceItems(
                  null,
                  Collections.singleton(project.getId()),
                  ModelDBServiceResourceTypes.PROJECT,
                  true);
          if (!resources.isEmpty()) {
            GetResourcesResponseItem resourceDetails = resources.get(0);
            mdbRoleService.createWorkspacePermissions(
                Optional.of(resourceDetails.getWorkspaceId()),
                Optional.empty(),
                project.getId(),
                project.getName(),
                Optional.of(resourceDetails.getOwnerId()),
                ModelDBServiceResourceTypes.PROJECT,
                resourceDetails.getCustomPermission(),
                resourceDetails.getVisibility(),
                true);
            migrated = true;
          }
        }
        if (migrated) {
          deleteRoleBindingsForProjects(Collections.singletonList(project));
          try (var session1 = modelDBHibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = null;
            try {
              transaction = session1.beginTransaction();
              Query query =
                  session1.createSQLQuery(
                      "UPDATE project p SET p.visibility_migration=true WHERE p.id=:id");
              query.setParameter("id", project.getId());
              query.executeUpdate();
              transaction.commit();
            } catch (Exception ex) {
              if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
              }
            }
          }
        }
      }
    }

    LOGGER.debug("Projects migration finished");
  }

  private static void migrateRepositories() {
    LOGGER.debug("Repositories migration started");
    Long count = getEntityCount(RepositoryEntity.class);

    LOGGER.debug("Total Repositories to migrate {}", count);

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var criteriaBuilder = session.getCriteriaBuilder();

      CriteriaQuery<RepositoryEntity> criteriaQuery =
          criteriaBuilder.createQuery(RepositoryEntity.class);
      Root<RepositoryEntity> root = criteriaQuery.from(RepositoryEntity.class);

      CriteriaQuery<RepositoryEntity> selectQuery =
          criteriaQuery
              .select(root)
              .where(
                  criteriaBuilder.and(
                      criteriaBuilder.equal(root.get(VISIBILITY_MIGRATION), false),
                      criteriaBuilder.equal(root.get(CREATED), true)));

      TypedQuery<RepositoryEntity> typedQuery = session.createQuery(selectQuery);
      List<RepositoryEntity> repositoryEntities = typedQuery.getResultList();
      Iterator<RepositoryEntity> iterator = repositoryEntities.iterator();

      Map<String, UserInfo> userInfoMap = new HashMap<>();

      var counter = 0;
      while (iterator.hasNext()) {
        LOGGER.debug("Migrated Repositories: {}/{}", counter, count);
        counter++;

        RepositoryEntity repository = iterator.next();

        var migrated = false;

        var modelDBServiceResourceTypes =
            ModelDBUtils.getModelDBServiceResourceTypesFromRepository(repository);

        if (repository.getOwner() != null && !repository.getOwner().isEmpty()) {
          WorkspaceDTO workspaceDTO;
          if (!userInfoMap.containsKey(repository.getOwner())) {
            try {
              userInfoMap.putAll(
                  authService.getUserInfoFromAuthServer(
                      Collections.singleton(repository.getOwner()), null, null, true));
            } catch (StatusRuntimeException ex) {
              if (ex.getStatus().getCode() == Status.Code.NOT_FOUND) {
                LOGGER.warn("Failed to get user info (skipping) : " + ex.toString());
                continue;
              }
              throw ex;
            }
          }
          try {
            workspaceDTO =
                mdbRoleService.getWorkspaceDTOByWorkspaceIdForServiceUser(
                    userInfoMap.get(repository.getOwner()),
                    repository.getWorkspace_id(),
                    repository.getWorkspace_type());
          } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.NOT_FOUND) {
              LOGGER.warn("Failed to get workspace (skipping) : " + ex.toString());
              continue;
            }
            throw ex;
          }
          Long owner;
          try {
            owner = Long.parseLong(repository.getOwner());
          } catch (NumberFormatException ex) {
            LOGGER.warn("Failed to convert owner (skipping) : " + ex.toString());
            continue;
          }
          // if repositoryVisibility is not equals to ResourceVisibility.ORG_SCOPED_PUBLIC then
          // ignore the CollaboratorType
          try {
            mdbRoleService.createWorkspacePermissions(
                Optional.empty(),
                Optional.of(workspaceDTO.getWorkspaceName()),
                String.valueOf(repository.getId()),
                repository.getName(),
                Optional.of(Long.parseLong(repository.getOwner())),
                modelDBServiceResourceTypes,
                CollaboratorPermissions.newBuilder()
                    .setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_ONLY)
                    .build(),
                getResourceVisibility(
                    Optional.ofNullable(
                        WorkspaceTypeEnum.WorkspaceType.forNumber(repository.getWorkspace_type())),
                    VisibilityEnum.Visibility.forNumber(repository.getRepository_visibility())),
                true);
          } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.ALREADY_EXISTS) {
              LOGGER.info(
                  "Resource seem to have already been created (ignoring) : " + ex.toString());
            } else {
              throw ex;
            }
          }
          migrated = true;
        } else {
          Set<String> newVisibilityRepositoryIds = new HashSet<>();
          Set<String> newVisibilityDatasetIds = new HashSet<>();
          if (repository.isDataset()) {
            newVisibilityDatasetIds.add(String.valueOf(repository.getId()));
          } else {
            newVisibilityRepositoryIds.add(String.valueOf(repository.getId()));
          }
          List<GetResourcesResponseItem> responseRepositoryItems =
              mdbRoleService.getResourceItems(
                  null, newVisibilityRepositoryIds, ModelDBServiceResourceTypes.REPOSITORY, true);
          List<GetResourcesResponseItem> responseDatasetItems =
              mdbRoleService.getResourceItems(
                  null, newVisibilityDatasetIds, ModelDBServiceResourceTypes.DATASET, true);
          Set<GetResourcesResponseItem> responseItems = new HashSet<>(responseRepositoryItems);
          responseItems.addAll(responseDatasetItems);
          Map<String, GetResourcesResponseItem> responseItemMap =
              responseItems.stream()
                  .collect(Collectors.toMap(GetResourcesResponseItem::getResourceId, item -> item));
          if (responseItemMap.containsKey(String.valueOf(repository.getId()))) {
            GetResourcesResponseItem resourceDetails =
                responseItemMap.get(String.valueOf(repository.getId()));
            mdbRoleService.createWorkspacePermissions(
                Optional.of(resourceDetails.getWorkspaceId()),
                Optional.empty(),
                String.valueOf(repository.getId()),
                repository.getName(),
                Optional.of(resourceDetails.getOwnerId()),
                modelDBServiceResourceTypes,
                resourceDetails.getCustomPermission(),
                resourceDetails.getVisibility(),
                true);
            migrated = true;
          }
        }
        if (migrated) {
          deleteRoleBindingsOfRepositories(Collections.singletonList(repository));
          try (var session1 = modelDBHibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = null;
            try {
              transaction = session1.beginTransaction();
              Query query =
                  session1.createSQLQuery(
                      "UPDATE repository r SET r.visibility_migration=true WHERE r.id=:id");
              query.setParameter("id", repository.getId());
              query.executeUpdate();
              transaction.commit();
            } catch (Exception ex) {
              if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
              }
            }
          }
        }
      }
    }

    LOGGER.debug("Repositories migration finished");
  }

  private static <T> Long getEntityCount(Class<T> klass) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var criteriaBuilder = session.getCriteriaBuilder();
      CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
      Root<T> root = countQuery.from(klass);
      countQuery
          .select(criteriaBuilder.count(root))
          .where(
              criteriaBuilder.and(
                  criteriaBuilder.equal(root.get(VISIBILITY_MIGRATION), false),
                  criteriaBuilder.equal(root.get(CREATED), true)));

      return session.createQuery(countQuery).getSingleResult();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getEntityCount(klass);
      } else {
        throw ex;
      }
    }
  }

  private static ResourceVisibility getResourceVisibility(
      Optional<WorkspaceTypeEnum.WorkspaceType> workspaceType,
      VisibilityEnum.Visibility visibility) {
    if (!workspaceType.isPresent()) {
      return ResourceVisibility.PRIVATE;
    }
    if (workspaceType.get() == WorkspaceTypeEnum.WorkspaceType.ORGANIZATION) {
      if (visibility == VisibilityEnum.Visibility.ORG_SCOPED_PUBLIC) {
        return ResourceVisibility.ORG_DEFAULT;
      } else if (visibility == VisibilityEnum.Visibility.PRIVATE) {
        return ResourceVisibility.PRIVATE;
      }
      return ResourceVisibility.ORG_DEFAULT;
    }
    return ResourceVisibility.PRIVATE;
  }

  private static void deleteRoleBindingsForProjects(List<ProjectEntity> projectEntities) {
    // set roleBindings name by accessible projects
    List<String> roleBindingNames = new LinkedList<>();
    setRoleBindingsNameOfAccessibleProjectsInRoleBindingNamesList(
        projectEntities, roleBindingNames);
    LOGGER.debug("num bindings after Projects {}", roleBindingNames.size());

    // Remove all role bindings
    if (!roleBindingNames.isEmpty()) {
      mdbRoleService.deleteRoleBindingsUsingServiceUser(roleBindingNames);
    }
  }

  private static void setRoleBindingsNameOfAccessibleProjectsInRoleBindingNamesList(
      List<ProjectEntity> allowedProjects, List<String> roleBindingNames) {
    for (ProjectEntity project : allowedProjects) {
      String projectId = project.getId();

      String ownerRoleBindingName =
          mdbRoleService.buildRoleBindingName(
              ModelDBConstants.ROLE_PROJECT_OWNER,
              project.getId(),
              project.getOwner(),
              ModelDBServiceResourceTypes.PROJECT.name());
      if (ownerRoleBindingName != null) {
        roleBindingNames.add(ownerRoleBindingName);
      }

      // Delete workspace based roleBindings
      List<String> workspaceRoleBindingNames =
          mdbRoleService.getWorkspaceRoleBindings(
              project.getWorkspace(),
              WorkspaceTypeEnum.WorkspaceType.forNumber(project.getWorkspace_type()),
              projectId,
              ModelDBConstants.ROLE_PROJECT_ADMIN,
              ModelDBServiceResourceTypes.PROJECT,
              project.getProjectVisibility().equals(ResourceVisibility.ORG_CUSTOM),
              "_GLOBAL_SHARING");
      roleBindingNames.addAll(workspaceRoleBindingNames);
    }
  }

  private static void deleteRoleBindingsOfRepositories(List<RepositoryEntity> allowedResources) {
    final List<String> roleBindingNames = Collections.synchronizedList(new ArrayList<>());
    for (RepositoryEntity repositoryEntity : allowedResources) {
      String ownerRoleBindingName =
          mdbRoleService.buildRoleBindingName(
              ModelDBConstants.ROLE_REPOSITORY_OWNER,
              String.valueOf(repositoryEntity.getId()),
              repositoryEntity.getOwner(),
              ModelDBServiceResourceTypes.REPOSITORY.name());
      if (ownerRoleBindingName != null) {
        roleBindingNames.add(ownerRoleBindingName);
      }

      // Delete workspace based roleBindings
      List<String> repoOrgWorkspaceRoleBindings =
          mdbRoleService.getWorkspaceRoleBindings(
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

    // Remove all role bindings
    if (!roleBindingNames.isEmpty()) {
      mdbRoleService.deleteRoleBindingsUsingServiceUser(roleBindingNames);
    }
  }
}
