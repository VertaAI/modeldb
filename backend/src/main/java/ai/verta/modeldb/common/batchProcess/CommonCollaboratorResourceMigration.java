package ai.verta.modeldb.common.batchProcess;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.VisibilityEnum;
import ai.verta.common.WorkspaceTypeEnum;
import ai.verta.modeldb.common.HibernateConnection;
import ai.verta.modeldb.common.ResourceEntity;
import ai.verta.modeldb.common.authservice.RoleService;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.UserInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CommonCollaboratorResourceMigration {
  private static final Logger LOGGER =
      LogManager.getLogger(CommonCollaboratorResourceMigration.class);
  private static final int PAGINATION_SIZE = 100;
  private final AuthService authService;
  private final RoleService roleService;

  public CommonCollaboratorResourceMigration(AuthService authService, RoleService roleService) {
    this.authService = authService;
    this.roleService = roleService;
  }

  protected <T extends ResourceEntity> void migrateResources(
      Supplier<Long> countSupplier,
      Supplier<Session> sessionSupplier,
      Class<T> clazz,
      ModelDBResourceEnum.ModelDBServiceResourceTypes resourceType,
      HibernateConnection hibernateConnection) {
    LOGGER.debug("Resource migration started");

    int lowerBound = 0;
    final int pagesize = PAGINATION_SIZE;
    Long count = countSupplier.get();
    LOGGER.debug("Total Resources {}", count);

    while (lowerBound < count) {

      try (Session session = sessionSupplier.get()) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
        Root<T> root = criteriaQuery.from(clazz);

        CriteriaQuery<T> selectQuery =
            criteriaQuery
                .select(root)
                .where(
                    criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("visibility_migration"), false),
                        criteriaBuilder.equal(root.get("created"), true)));

        TypedQuery<T> typedQuery = session.createQuery(selectQuery);

        typedQuery.setFirstResult(lowerBound);
        typedQuery.setMaxResults(pagesize);
        List<T> resourceEntities = typedQuery.getResultList();

        if (resourceEntities.size() > 0) {
          Set<String> userIds = new HashSet<>();
          Set<String> newVisibilityresourceIds = new HashSet<>();
          for (T resourceEntity : resourceEntities) {
            String owner = resourceEntity.getOwner();
            if (owner != null && !owner.isEmpty()) {
              userIds.add(owner);
            } else {
              newVisibilityresourceIds.add(resourceEntity.getId());
            }
          }
          LOGGER.debug("resource userId list : " + userIds);

          // Fetch the resource owners userInfo
          Map<String, UserInfo> userInfoMap = new HashMap<>();
          if (!userIds.isEmpty()) {
            userInfoMap.putAll(authService.getUserInfoFromAuthServer(userIds, null, null));
          }

          List<GetResourcesResponseItem> responseItems =
              roleService.getResourceItems(null, newVisibilityresourceIds, resourceType);
          Map<String, GetResourcesResponseItem> responseItemMap =
              responseItems.stream()
                  .collect(Collectors.toMap(GetResourcesResponseItem::getResourceId, item -> item));

          for (T resource : resourceEntities) {
            boolean migrated = false;
            String owner = resource.getOwner();
            if (owner != null && !owner.isEmpty()) {
              // if resourceVisibility is not equals to ResourceVisibility.ORG_SCOPED_PUBLIC then
              // ignore the CollaboratorType
              roleService.createWorkspacePermissions(
                  resource.getWorkspaceId(),
                  resource.getWorkspaceName(),
                  resource.getId(),
                  resource.getName(),
                  Optional.of(Long.parseLong(resource.getOwner())),
                  resourceType,
                  CollaboratorPermissions.newBuilder()
                      .setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_ONLY)
                      .build(),
                  resource.getResourceVisibility());
              migrated = true;
            } else if (responseItemMap.containsKey(resource.getId())) {
              GetResourcesResponseItem resourceDetails = responseItemMap.get(resource.getId());
              roleService.createWorkspacePermissions(
                  resource.getWorkspaceId(),
                  resource.getWorkspaceName(),
                  resource.getId(),
                  resource.getName(),
                  Optional.of(resourceDetails.getOwnerId()),
                  resourceType,
                  resourceDetails.getCustomPermission(),
                  resourceDetails.getVisibility());
              migrated = true;
            }
            if (migrated) {
              Transaction transaction = null;
              try {
                resource.deleteRoleBindings();
                transaction = session.beginTransaction();
                resource.setVisibility_migration(true);
                session.update(resource);
                transaction.commit();
              } catch (Exception ex) {
                if (transaction != null && transaction.getStatus().canRollback()) {
                  transaction.rollback();
                }
              }
            }
          }
        } else {
          LOGGER.debug("Total resources count 0");
        }
        lowerBound += pagesize;
      } catch (Exception ex) {
        if (hibernateConnection.needToRetry(ex)) {
          migrateResources(
              countSupplier, sessionSupplier, clazz, resourceType, hibernateConnection);
        } else {
          throw ex;
        }
      }
    }

    LOGGER.debug("resources migration finished");
  }
}
