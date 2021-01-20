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
import ai.verta.uac.UserInfo;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CollaboratorResourceMigration extends CommonCollaboratorResourceMigration {
  private static final Logger LOGGER = LogManager.getLogger(CollaboratorResourceMigration.class);
  private static AuthService authService;
  private static RoleService roleService;
  private static int paginationSize;

  public static CollaboratorResourceMigration create() {
    CollaboratorResourceMigration.paginationSize = 100;
    if (Config.getInstance().hasAuth()) {
      authService = AuthServiceUtils.FromConfig(Config.getInstance());
      roleService = RoleServiceUtils.FromConfig(Config.getInstance(), authService);
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
          roleService.getWorkspaceDTOByWorkspaceId(
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
        ModelDBServiceResourceTypes.PROJECT,
        hibernateConnection,
        this::getWorkspaceName);
    LOGGER.debug("Projects migration finished");
  }

  private void migrateRepositories() {
    LOGGER.debug("Projects migration started");
    HibernateConnection hibernateConnection = new ModelDBHibernateConnection();
    migrateResources(
        () -> ModelDBHibernateUtil.getEntityCount(RepositoryEntity.class),
        () -> ModelDBHibernateUtil.getSessionFactory().openSession(),
        RepositoryEntity.class,
        ModelDBServiceResourceTypes.REPOSITORY,
        hibernateConnection,
        this::getWorkspaceName);
    LOGGER.debug("Repositories migration finished");
  }
}
