package ai.verta.modeldb.batchProcess;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.HibernateConnection;
import ai.verta.modeldb.common.ResourceEntity;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.batchProcess.CommonCollaboratorResourceMigration;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.utils.ModelDBHibernateConnection;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.UserInfo;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CollaboratorResourceMigration extends CommonCollaboratorResourceMigration {
  private static final Logger LOGGER = LogManager.getLogger(CollaboratorResourceMigration.class);

  public static CollaboratorResourceMigration create() {
    if (Config.getInstance().hasAuth()) {
      AuthService authService = AuthServiceUtils.FromConfig(Config.getInstance());
      RoleService roleService = RoleServiceUtils.FromConfig(Config.getInstance(), authService);
      return new CollaboratorResourceMigration(authService, roleService);
    }
    return new CollaboratorResourceMigration(null, null);
  }

  public CollaboratorResourceMigration(AuthService authService, RoleService roleService) {
    super(authService, roleService);
  }

  public void execute() {
    if (authService == null) {
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

  private <T extends ResourceEntity> Optional<String> getWorkspaceName(
      Map<String, UserInfo> userInfoMap, T resourceEntity) {
    String owner = resourceEntity.getOwner();
    if (owner != null && !owner.isEmpty()) {
      WorkspaceDTO workspaceDTO =
          ((RoleService) roleService)
              .getWorkspaceDTOByWorkspaceId(
                  userInfoMap.get(resourceEntity.getOwner()),
                  resourceEntity.getOldWorkspaceId(),
                  resourceEntity.getWorkspaceType());
      return Optional.of(workspaceDTO.getWorkspaceName());
    } else {
      return Optional.empty();
    }
  }

  private void migrateProjects() {
    LOGGER.debug("Projects migration started");
    HibernateConnection hibernateConnection = new ModelDBHibernateConnection();
    migrateResources(
        () -> ModelDBHibernateUtil.getEntityCount(ProjectEntity.class),
        () -> ModelDBHibernateUtil.getSessionFactory().openSession(),
        ProjectEntity.class,
        project -> ModelDBServiceResourceTypes.PROJECT,
        hibernateConnection,
        this::getWorkspaceName,
        this::getResponseItemsProject);
    LOGGER.debug("Projects migration finished");
  }

  private void migrateRepositories() {
    LOGGER.debug("Repositories migration started");
    HibernateConnection hibernateConnection = new ModelDBHibernateConnection();
    migrateResources(
        () -> ModelDBHibernateUtil.getEntityCount(RepositoryEntity.class),
        () -> ModelDBHibernateUtil.getSessionFactory().openSession(),
        RepositoryEntity.class,
        ModelDBUtils::getModelDBServiceResourceTypesFromRepository,
        hibernateConnection,
        this::getWorkspaceName,
        this::getResponseItems);
    LOGGER.debug("Repositories migration finished");
  }

  private Map.Entry<Map<String, UserInfo>, List<GetResourcesResponseItem>> getResponseItemsProject(
      List<ProjectEntity> resourceEntities) {
    Set<String> userIds = new HashSet<>();
    Set<String> newVisibilityResourceIds = new HashSet<>();
    for (ResourceEntity resourceEntity : resourceEntities) {
      String owner = resourceEntity.getOwner();
      if (owner != null && !owner.isEmpty()) {
        userIds.add(owner);
      } else {
        newVisibilityResourceIds.add(resourceEntity.getStringId());
      }
    }
    LOGGER.debug("resource userId list : " + userIds);

    // Fetch the resource owners userInfo
    Map<String, UserInfo> userInfoMap = new HashMap<>();
    if (!userIds.isEmpty()) {
      userInfoMap.putAll(authService.getUserInfoFromAuthServer(userIds, null, null));
    }
    List<GetResourcesResponseItem> responseItems =
        roleService.getResourceItems(
            null, newVisibilityResourceIds, ModelDBServiceResourceTypes.PROJECT);
    return new AbstractMap.SimpleEntry<>(userInfoMap, responseItems);
  }

  private Map.Entry<Map<String, UserInfo>, List<GetResourcesResponseItem>> getResponseItems(
      List<RepositoryEntity> repositoryEntities) {
    Set<String> userIds = new HashSet<>();
    Set<String> newVisibilityRepositoryIds = new HashSet<>();
    Set<String> newVisibilityDatasetIds = new HashSet<>();
    for (RepositoryEntity repositoryEntity : repositoryEntities) {
      if (repositoryEntity.getOwner() != null && !repositoryEntity.getOwner().isEmpty()) {
        userIds.add(repositoryEntity.getOwner());
      } else {
        if (repositoryEntity.isDataset()) {
          newVisibilityDatasetIds.add(String.valueOf(repositoryEntity.getId()));
        } else {
          newVisibilityRepositoryIds.add(String.valueOf(repositoryEntity.getId()));
        }
      }
    }
    LOGGER.debug("Repository userId list : " + userIds);

    // Fetch the repository owners userInfo
    Map<String, UserInfo> userInfoMap = new HashMap<>();
    if (!userIds.isEmpty()) {
      userInfoMap.putAll(authService.getUserInfoFromAuthServer(userIds, null, null));
    }

    List<GetResourcesResponseItem> responseRepositoryItems =
        roleService.getResourceItems(
            null, newVisibilityRepositoryIds, ModelDBServiceResourceTypes.REPOSITORY);
    List<GetResourcesResponseItem> responseDatasetItems =
        roleService.getResourceItems(
            null, newVisibilityDatasetIds, ModelDBServiceResourceTypes.DATASET);
    Set<GetResourcesResponseItem> responseItems = new HashSet<>(responseRepositoryItems);
    responseItems.addAll(responseDatasetItems);
    return new AbstractMap.SimpleEntry<>(userInfoMap, responseDatasetItems);
  }
}
