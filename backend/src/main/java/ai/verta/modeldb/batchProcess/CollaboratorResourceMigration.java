package ai.verta.modeldb.batchProcess;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.VisibilityEnum;
import ai.verta.common.WorkspaceTypeEnum;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.UserInfo;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class CollaboratorResourceMigration {
  private static final Logger LOGGER = LogManager.getLogger(CollaboratorResourceMigration.class);
  private static AuthService authService;
  private static RoleService roleService;
  private static int paginationSize;

  public CollaboratorResourceMigration() {}

  public static void execute() {
    CollaboratorResourceMigration.paginationSize = 100;
    if (Config.getInstance().hasAuth()) {
      authService = AuthServiceUtils.FromConfig(Config.getInstance());
      roleService = RoleServiceUtils.FromConfig(Config.getInstance(), authService);
    } else {
      LOGGER.debug("AuthService Host & Port not found, OSS setup found");
      return;
    }

    LOGGER.info("Migration start");
    CommonUtils.registeredBackgroundUtilsCount();
    try {
      migrateProjects();
      LOGGER.info("Projects done migration");
      migrateRepositories();
      LOGGER.info("Repositories done migration");
    } finally {
      CommonUtils.unregisteredBackgroundUtilsCount();
    }

    LOGGER.info("Migration End");
  }

  private static void migrateProjects() {
    LOGGER.debug("Projects migration started");
    Long count = getEntityCount(ProjectEntity.class);

    int lowerBound = 0;
    final int pagesize = CollaboratorResourceMigration.paginationSize;
    LOGGER.debug("Total Projects {}", count);

    while (lowerBound < count) {

      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

        CriteriaQuery<ProjectEntity> criteriaQuery =
            criteriaBuilder.createQuery(ProjectEntity.class);
        Root<ProjectEntity> root = criteriaQuery.from(ProjectEntity.class);

        CriteriaQuery<ProjectEntity> selectQuery =
            criteriaQuery
                .select(root)
                .where(criteriaBuilder.equal(root.get("visibility_migration"), false));

        TypedQuery<ProjectEntity> typedQuery = session.createQuery(selectQuery);

        typedQuery.setFirstResult(lowerBound);
        typedQuery.setMaxResults(pagesize);
        List<ProjectEntity> projectEntities = typedQuery.getResultList();

        if (projectEntities.size() > 0) {
          Set<String> userIds = new HashSet<>();
          Set<String> newVisibilityProjectIds = new HashSet<>();
          for (ProjectEntity projectEntity : projectEntities) {
            if (projectEntity.getOwner() != null && !projectEntity.getOwner().isEmpty()) {
              userIds.add(projectEntity.getOwner());
            } else {
              newVisibilityProjectIds.add(projectEntity.getId());
            }
          }
          LOGGER.debug("Project userId list : " + userIds);

          // Fetch the project owners userInfo
          Map<String, UserInfo> userInfoMap = new HashMap<>();
          if (!userIds.isEmpty()) {
            userInfoMap.putAll(authService.getUserInfoFromAuthServer(userIds, null, null));
          }

          List<GetResourcesResponseItem> responseItems =
              roleService.getResourceItems(
                  null, newVisibilityProjectIds, ModelDBServiceResourceTypes.PROJECT);
          Map<String, GetResourcesResponseItem> responseItemMap =
              responseItems.stream()
                  .collect(Collectors.toMap(GetResourcesResponseItem::getResourceId, item -> item));

          for (ProjectEntity project : projectEntities) {
            if (project.getOwner() != null && !project.getOwner().isEmpty()) {
              WorkspaceDTO workspaceDTO =
                  roleService.getWorkspaceDTOByWorkspaceId(
                      userInfoMap.get(project.getOwner()),
                      project.getWorkspace(),
                      project.getWorkspace_type());
              // if projectVisibility is not equals to ResourceVisibility.ORG_SCOPED_PUBLIC then
              // ignore the CollaboratorType
              roleService.createWorkspacePermissions(
                  workspaceDTO.getWorkspaceName(),
                  project.getId(),
                  project.getName(),
                  Optional.of(Long.parseLong(project.getOwner())),
                  ModelDBServiceResourceTypes.PROJECT,
                  CollaboratorPermissions.newBuilder()
                      .setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_ONLY)
                      .build(),
                  getResourceVisibility(
                      Optional.ofNullable(
                          WorkspaceTypeEnum.WorkspaceType.forNumber(project.getWorkspace_type())),
                      VisibilityEnum.Visibility.forNumber(project.getProject_visibility())));
            } else {
              GetResourcesResponseItem resourceDetails = responseItemMap.get(project.getId());
              roleService.createWorkspacePermissions(
                  resourceDetails.getWorkspaceId(),
                  Optional.empty(),
                  project.getId(),
                  project.getName(),
                  Optional.of(resourceDetails.getOwnerId()),
                  ModelDBServiceResourceTypes.PROJECT,
                  resourceDetails.getCustomPermission(),
                  resourceDetails.getVisibility());
            }
            Transaction transaction = null;
            try {
              transaction = session.beginTransaction();
              project.setVisibility_migration(true);
              session.update(project);
              transaction.commit();
            } catch (Exception ex) {
              if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
              }
            }
          }
        } else {
          LOGGER.debug("Total projects count 0");
        }
        lowerBound += pagesize;
      } catch (Exception ex) {
        if (ModelDBUtils.needToRetry(ex)) {
          migrateProjects();
        } else {
          throw ex;
        }
      }
    }

    LOGGER.debug("Projects migration finished");
  }

  private static void migrateRepositories() {
    LOGGER.debug("Repositories migration started");
    Long count = getEntityCount(RepositoryEntity.class);

    int lowerBound = 0;
    final int pagesize = CollaboratorResourceMigration.paginationSize;
    LOGGER.debug("Total Repositories {}", count);

    while (lowerBound < count) {

      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

        CriteriaQuery<RepositoryEntity> criteriaQuery =
            criteriaBuilder.createQuery(RepositoryEntity.class);
        Root<RepositoryEntity> root = criteriaQuery.from(RepositoryEntity.class);

        CriteriaQuery<RepositoryEntity> selectQuery =
            criteriaQuery
                .select(root)
                .where(criteriaBuilder.equal(root.get("visibility_migration"), false));

        TypedQuery<RepositoryEntity> typedQuery = session.createQuery(selectQuery);

        typedQuery.setFirstResult(lowerBound);
        typedQuery.setMaxResults(pagesize);
        List<RepositoryEntity> repositoryEntities = typedQuery.getResultList();

        if (repositoryEntities.size() > 0) {
          Set<String> userIds = new HashSet<>();
          Set<String> newVisibilityRepositoryIds = new HashSet<>();
          for (RepositoryEntity repositoryEntity : repositoryEntities) {
            if (repositoryEntity.getOwner() != null && !repositoryEntity.getOwner().isEmpty()) {
              userIds.add(repositoryEntity.getOwner());
            } else {
              newVisibilityRepositoryIds.add(String.valueOf(repositoryEntity.getId()));
            }
          }
          LOGGER.debug("Repository userId list : " + userIds);

          // Fetch the repository owners userInfo
          Map<String, UserInfo> userInfoMap = new HashMap<>();
          if (!userIds.isEmpty()) {
            userInfoMap.putAll(authService.getUserInfoFromAuthServer(userIds, null, null));
          }

          List<GetResourcesResponseItem> responseItems =
              roleService.getResourceItems(
                  null, newVisibilityRepositoryIds, ModelDBServiceResourceTypes.REPOSITORY);
          Map<String, GetResourcesResponseItem> responseItemMap =
              responseItems.stream()
                  .collect(Collectors.toMap(GetResourcesResponseItem::getResourceId, item -> item));
          for (RepositoryEntity repository : repositoryEntities) {
            if (repository.getOwner() != null && !repository.getOwner().isEmpty()) {
              WorkspaceDTO workspaceDTO =
                  roleService.getWorkspaceDTOByWorkspaceId(
                      userInfoMap.get(repository.getOwner()),
                      repository.getWorkspace_id(),
                      repository.getWorkspace_type());
              // if repositoryVisibility is not equals to ResourceVisibility.ORG_SCOPED_PUBLIC then
              // ignore the CollaboratorType
              roleService.createWorkspacePermissions(
                  workspaceDTO.getWorkspaceName(),
                  String.valueOf(repository.getId()),
                  repository.getName(),
                  Optional.of(Long.parseLong(repository.getOwner())),
                  ModelDBServiceResourceTypes.REPOSITORY,
                  CollaboratorPermissions.newBuilder()
                      .setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_ONLY)
                      .build(),
                  getResourceVisibility(
                      Optional.ofNullable(
                          WorkspaceTypeEnum.WorkspaceType.forNumber(
                              repository.getWorkspace_type())),
                      VisibilityEnum.Visibility.forNumber(repository.getRepository_visibility())));
            } else {
              GetResourcesResponseItem resourceDetails =
                  responseItemMap.get(String.valueOf(repository.getId()));
              roleService.createWorkspacePermissions(
                  resourceDetails.getWorkspaceId(),
                  Optional.empty(),
                  String.valueOf(repository.getId()),
                  repository.getName(),
                  Optional.of(resourceDetails.getOwnerId()),
                  ModelDBServiceResourceTypes.REPOSITORY,
                  resourceDetails.getCustomPermission(),
                  resourceDetails.getVisibility());
            }
            Transaction transaction = null;
            try {
              transaction = session.beginTransaction();
              repository.setVisibility_migration(true);
              session.update(repository);
              transaction.commit();
            } catch (Exception ex) {
              if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
              }
            }
          }
        } else {
          LOGGER.debug("Total repositories count 0");
        }
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
}
