package ai.verta.modeldb.batchProcess;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.VisibilityEnum;
import ai.verta.common.WorkspaceTypeEnum;
import ai.verta.modeldb.App;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceChannel;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Resources;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.SetResources;
import ai.verta.uac.UserInfo;
import com.google.rpc.Code;
import io.grpc.StatusRuntimeException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

public class CollaboratorResourceMigration {
  private static final Logger LOGGER = LogManager.getLogger(CollaboratorResourceMigration.class);
  private static AuthService authService;
  private static RoleService roleService;
  private int paginationSize;

  public CollaboratorResourceMigration() {}

  public void execute(int recordUpdateLimit) {
    this.paginationSize = recordUpdateLimit;
    App app = App.getInstance();
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      app.setAuthServerHost(app.getAuthServerHost());
      app.setAuthServerPort(app.getAuthServerPort());

      authService = new AuthServiceUtils();
      roleService = new RoleServiceUtils(authService);
    } else {
      LOGGER.debug("AuthService Host & Port not found, OSS setup found");
      return;
    }

    LOGGER.info("Migration start");
    ModelDBUtils.registeredBackgroundUtilsCount();
    try {
      migrateProjects();
      LOGGER.info("Projects done migration");
      // migrateRepositories();
      // LOGGER.info("Repositories done migration");
    } catch (Exception ex) {
      if (ex instanceof StatusRuntimeException) {
        StatusRuntimeException exception = (StatusRuntimeException) ex;
        if (exception.getStatus().getCode().value() == Code.PERMISSION_DENIED_VALUE) {
          LOGGER.warn("CollaboratorResourceMigration Exception: {}", ex.getMessage());
        } else {
          LOGGER.warn("CollaboratorResourceMigration Exception: ", ex);
        }
      } else {
        LOGGER.warn("CollaboratorResourceMigration Exception: ", ex);
      }
    } finally {
      ModelDBUtils.unregisteredBackgroundUtilsCount();
    }

    LOGGER.info("Migration End");
  }

  private void migrateProjects() {
    LOGGER.debug("Projects migration started");
    Long count = getEntityCount(ProjectEntity.class);

    int lowerBound = 0;
    final int pagesize = this.paginationSize;
    LOGGER.debug("Total Projects {}", count);

    while (lowerBound < count) {

      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

        CriteriaQuery<ProjectEntity> criteriaQuery =
            criteriaBuilder.createQuery(ProjectEntity.class);
        Root<ProjectEntity> root = criteriaQuery.from(ProjectEntity.class);

        CriteriaQuery<ProjectEntity> selectQuery =
            criteriaQuery.select(root).orderBy(criteriaBuilder.asc(root.get("id")));

        TypedQuery<ProjectEntity> typedQuery = session.createQuery(selectQuery);

        typedQuery.setFirstResult(lowerBound);
        typedQuery.setMaxResults(pagesize);
        List<ProjectEntity> projectEntities = typedQuery.getResultList();

        if (projectEntities.size() > 0) {
          Set<String> userIds = new HashSet<>();
          for (ProjectEntity projectEntity : projectEntities) {
            userIds.add(projectEntity.getOwner());
          }
          LOGGER.debug("Project userId list : " + userIds);

          // Fetch the experiment owners userInfo
          Map<String, UserInfo> userInfoMap =
              authService.getUserInfoFromAuthServer(userIds, null, null);
          for (ProjectEntity project : projectEntities) {
            try {
              WorkspaceDTO workspaceDTO =
                  roleService.getWorkspaceDTOByWorkspaceId(
                      userInfoMap.get(project.getOwner()),
                      project.getWorkspace(),
                      project.getWorkspace_type());
              // if projectVisibility is not equals to ResourceVisibility.ORG_SCOPED_PUBLIC then
              // ignore the CollaboratorType
              createWorkspacePermissions(
                  Optional.of(workspaceDTO.getWorkspaceName()),
                  Optional.ofNullable(
                      WorkspaceTypeEnum.WorkspaceType.forNumber(project.getWorkspace_type())),
                  project.getId(),
                  Optional.of(Long.parseLong(project.getOwner())),
                  ModelDBServiceResourceTypes.PROJECT,
                  CollaboratorTypeEnum.CollaboratorType.READ_WRITE,
                  VisibilityEnum.Visibility.forNumber(project.getProject_visibility()));
            } catch (Exception ex) {
              LOGGER.warn("CollaboratorResourceMigration Exception: {}", ex.getMessage());
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

  private void createWorkspacePermissions(
      Optional<String> workspaceName,
      Optional<WorkspaceTypeEnum.WorkspaceType> workspaceType,
      String resourceId,
      Optional<Long> ownerId,
      ModelDBServiceResourceTypes resourceType,
      CollaboratorTypeEnum.CollaboratorType collaboratorType,
      VisibilityEnum.Visibility visibility) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info("Calling CollaboratorService to create resources");
      ResourceType modeldbServiceResourceType =
          ResourceType.newBuilder().setModeldbServiceResourceType(resourceType).build();
      Resources resources =
          Resources.newBuilder()
              .setResourceType(modeldbServiceResourceType)
              .setService(ServiceEnum.Service.MODELDB_SERVICE)
              .addResourceIds(resourceId)
              .build();
      ResourceVisibility resourceVisibility = getResourceVisibility(workspaceType, visibility);
      SetResources.Builder setResourcesBuilder =
          SetResources.newBuilder().setResources(resources).setVisibility(resourceVisibility);

      if (resourceVisibility.equals(ResourceVisibility.ORG_DEFAULT)) {
        setResourcesBuilder.setCollaboratorType(collaboratorType);
      }

      if (ownerId.isPresent()) {
        setResourcesBuilder.setOwnerId(ownerId.get());
      }
      if (workspaceName.isPresent()) {
        setResourcesBuilder = setResourcesBuilder.setWorkspaceName(workspaceName.get());
      } else {
        throw new IllegalArgumentException(
            "workspaceId and workspaceName are both empty.  One must be provided.");
      }
      SetResources setResources = setResourcesBuilder.build();
      SetResources.Response setResourcesResponse =
          authServiceChannel.getCollaboratorServiceBlockingStub().setResources(setResources);

      LOGGER.info("SetResources message sent.  Response: " + setResourcesResponse);
    } catch (StatusRuntimeException ex) {
      LOGGER.error(ex);
      ModelDBUtils.retryOrThrowException(ex, false, retry -> null);
    }
  }

  private ResourceVisibility getResourceVisibility(
      Optional<WorkspaceTypeEnum.WorkspaceType> workspaceType,
      VisibilityEnum.Visibility projectVisibility) {
    if (!workspaceType.isPresent()) {
      return ResourceVisibility.PRIVATE;
    }
    if (workspaceType.get() == WorkspaceTypeEnum.WorkspaceType.ORGANIZATION) {
      if (projectVisibility == VisibilityEnum.Visibility.ORG_SCOPED_PUBLIC) {
        return ResourceVisibility.ORG_DEFAULT;
      } else if (projectVisibility == VisibilityEnum.Visibility.PRIVATE) {
        return ResourceVisibility.PRIVATE;
      }
      return ResourceVisibility.ORG_DEFAULT;
    }
    return ResourceVisibility.PRIVATE;
  }
}
