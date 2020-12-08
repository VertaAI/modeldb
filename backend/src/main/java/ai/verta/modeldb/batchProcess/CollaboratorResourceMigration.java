package ai.verta.modeldb.batchProcess;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.VisibilityEnum;
import ai.verta.modeldb.App;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceChannel;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Resources;
import ai.verta.uac.ServiceEnum.Service;
import ai.verta.uac.SetResources;
import ai.verta.uac.UserInfo;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
      LOGGER.debug("AuthService Host & Port not found");
      return;
    }

    LOGGER.info("Migration start");
    migrateProjects();
    LOGGER.info("Projects done migration");
    // migrateRepositories();
    // LOGGER.info("Repositories done migration");

    LOGGER.info("Migration End");
  }

  private void migrateProjects() {
    LOGGER.debug("Projects migration started");
    Long count = getEntityCount(ProjectEntity.class);

    int lowerBound = 0;
    final int pagesize = this.paginationSize;
    LOGGER.debug("Total Projects {}", count);

    while (lowerBound < count) {

      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession();
          AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
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
          for (ProjectEntity project : projectEntities) {
            userIds.add(project.getOwner());
          }
          LOGGER.debug("Projects userId list : " + userIds);
          if (userIds.size() == 0) {
            LOGGER.warn("userIds not found for Projects on page lower boundary {}", lowerBound);
            lowerBound += pagesize;
            continue;
          }

          // Fetch the project owners userInfo
          Map<String, UserInfo> userInfoMap =
              authService.getUserInfoFromAuthServer(userIds, null, null);
          for (ProjectEntity projectEntity : projectEntities) {
            if (userInfoMap.containsKey(projectEntity.getOwner())) {
              UserInfo ownerInfo = userInfoMap.get(projectEntity.getOwner());
              SetResources.Builder setResources =
                  SetResources.newBuilder()
                      .setResources(
                          Resources.newBuilder()
                              .setResourceType(
                                  ResourceType.newBuilder()
                                      .setModeldbServiceResourceType(
                                          ModelDBServiceResourceTypes.PROJECT))
                              .setService(Service.MODELDB_SERVICE)
                              .addResourceIds(projectEntity.getId()))
                      .setWorkspaceId(Long.parseLong(projectEntity.getWorkspace()))
                      .setOwnerId(Long.parseLong(authService.getVertaIdFromUserInfo(ownerInfo)))
                      .setVisibility(
                          projectEntity
                                  .getProjectVisibility()
                                  .equals(VisibilityEnum.Visibility.ORG_SCOPED_PUBLIC)
                              ? ResourceVisibility.ORG_DEFAULT
                              : ResourceVisibility.PRIVATE);
              authServiceChannel
                  .getCollaboratorServiceBlockingStub()
                  .setResources(setResources.build());
            } else {
              LOGGER.debug("UserInfo not found for the ownerId: {}", projectEntity.getOwner());
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
}
