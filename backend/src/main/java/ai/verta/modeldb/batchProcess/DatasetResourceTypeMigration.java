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
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.UserInfo;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class DatasetResourceTypeMigration {
  private static final Logger LOGGER = LogManager.getLogger(DatasetResourceTypeMigration.class);
  private static AuthService authService;
  private static RoleService roleService;
  private static int paginationSize;

  public DatasetResourceTypeMigration() {}

  public static void execute() {
    DatasetResourceTypeMigration.paginationSize = 100;
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
      migrateDatasets();
      LOGGER.info("Dataset resource type migration done");
    } finally {
      CommonUtils.unregisteredBackgroundUtilsCount();
    }

    LOGGER.info("Migration End");
  }

  private static void migrateDatasets() {
    LOGGER.debug("Dataset resource type migration started");
    String queryString =
        "From RepositoryEntity r WHERE r.deleted = false AND r.created = true AND r.datasetRepositoryMappingEntity IS NOT EMPTY";
    Long count;
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery("SELECT COUNT(r) " + queryString);
      count = (Long) query.uniqueResult();
    }

    int lowerBound = 0;
    final int pagesize = DatasetResourceTypeMigration.paginationSize;
    LOGGER.debug("Total Repositories {}", count);

    while (lowerBound < count) {

      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        Query query = session.createQuery(queryString);
        query.setFirstResult(lowerBound);
        query.setMaxResults(pagesize);
        List<RepositoryEntity> repositoryEntities = query.list();

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

          List<GetResourcesResponseItem> responseItems =
              roleService.getResourceItems(
                  null, newVisibilityRepositoryIds, ModelDBServiceResourceTypes.REPOSITORY);

          Map<String, GetResourcesResponseItem> responseItemMap =
              responseItems.stream()
                  .collect(Collectors.toMap(GetResourcesResponseItem::getResourceId, item -> item));
          if (!responseItemMap.isEmpty()) {
            // Fetch the repository owners userInfo
            Map<String, UserInfo> userInfoMap = new HashMap<>();
            if (!userIds.isEmpty()) {
              userInfoMap.putAll(authService.getUserInfoFromAuthServer(userIds, null, null));
            }

            for (RepositoryEntity repository : repositoryEntities) {
              ModelDBServiceResourceTypes modelDBServiceResourceTypes =
                  ModelDBServiceResourceTypes.DATASET;
              if (repository.getOwner() != null && !repository.getOwner().isEmpty()) {
                WorkspaceDTO workspaceDTO =
                    roleService.getWorkspaceDTOByWorkspaceId(
                        userInfoMap.get(repository.getOwner()),
                        repository.getWorkspace_id(),
                        repository.getWorkspace_type());
                // if datasetVisibility is not equals to ResourceVisibility.ORG_SCOPED_PUBLIC then
                // ignore the CollaboratorType
                roleService.createWorkspacePermissions(
                    workspaceDTO.getWorkspaceName(),
                    String.valueOf(repository.getId()),
                    repository.getName(),
                    Optional.of(Long.parseLong(repository.getOwner())),
                    modelDBServiceResourceTypes,
                    CollaboratorPermissions.newBuilder()
                        .setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_ONLY)
                        .build(),
                    getResourceVisibility(
                        Optional.ofNullable(
                            WorkspaceTypeEnum.WorkspaceType.forNumber(
                                repository.getWorkspace_type())),
                        VisibilityEnum.Visibility.forNumber(
                            repository.getRepository_visibility())));
              } else if (responseItemMap.containsKey(String.valueOf(repository.getId()))) {
                GetResourcesResponseItem resourceDetails =
                    responseItemMap.get(String.valueOf(repository.getId()));
                roleService.createWorkspacePermissions(
                    resourceDetails.getWorkspaceId(),
                    Optional.empty(),
                    String.valueOf(repository.getId()),
                    repository.getName(),
                    Optional.of(resourceDetails.getOwnerId()),
                    modelDBServiceResourceTypes,
                    resourceDetails.getCustomPermission(),
                    resourceDetails.getVisibility());
              }

              roleService.deleteEntityResources(
                  Collections.singletonList(String.valueOf(repository.getId())),
                  ModelDBServiceResourceTypes.REPOSITORY);
            }
          }
        } else {
          LOGGER.debug("Total Datasets count 0");
        }
        lowerBound += pagesize;
      } catch (Exception ex) {
        if (ModelDBUtils.needToRetry(ex)) {
          migrateDatasets();
        } else {
          throw ex;
        }
      }
    }

    LOGGER.debug("Dataset resource type migration finished");
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
