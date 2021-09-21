package ai.verta.modeldb.batchProcess;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.MDBAuthServiceUtils;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.authservice.MDBRoleServiceUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.UserInfo;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OwnerRoleBindingRepositoryUtils {
  private OwnerRoleBindingRepositoryUtils() {}

  private static final Logger LOGGER = LogManager.getLogger(OwnerRoleBindingUtils.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private static AuthService authService;
  private static UAC uac;
  private static MDBRoleService mdbRoleService;

  public static void execute() {
    var config = App.getInstance().mdbConfig;
    if (config.hasAuth()) {
      uac = UAC.FromConfig(config);
      authService = MDBAuthServiceUtils.FromConfig(config, uac);
      mdbRoleService = MDBRoleServiceUtils.FromConfig(config, authService, uac);
    } else {
      LOGGER.debug("AuthService Host & Port not found");
      return;
    }

    migrateRepositories();
    LOGGER.info("Repositories done migration");
  }

  private static void migrateRepositories() {
    LOGGER.debug("Repositories migration started");
    Long count = getEntityCount(RepositoryEntity.class);

    var lowerBound = 0;
    final var pagesize = 5000;
    LOGGER.debug("Total repositories {}", count);

    while (lowerBound < count) {

      try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        var transaction = session.beginTransaction();
        var criteriaBuilder = session.getCriteriaBuilder();

        CriteriaQuery<RepositoryEntity> criteriaQuery =
            criteriaBuilder.createQuery(RepositoryEntity.class);
        Root<RepositoryEntity> root = criteriaQuery.from(RepositoryEntity.class);

        CriteriaQuery<RepositoryEntity> selectQuery =
            criteriaQuery.select(root).orderBy(criteriaBuilder.asc(root.get("id")));

        TypedQuery<RepositoryEntity> typedQuery = session.createQuery(selectQuery);

        typedQuery.setFirstResult(lowerBound);
        typedQuery.setMaxResults(pagesize);
        List<RepositoryEntity> repositoryEntities = typedQuery.getResultList();

        if (repositoryEntities.size() > 0) {
          Set<String> userIds = new HashSet<>();
          for (RepositoryEntity repositoryEntity : repositoryEntities) {
            userIds.add(repositoryEntity.getOwner());
          }
          LOGGER.debug("Repositories userId list : " + userIds);
          if (userIds.size() == 0) {
            LOGGER.warn("userIds not found for Repositories on page lower boundary {}", lowerBound);
            lowerBound += pagesize;
            continue;
          }
          // Fetch the Repository owners userInfo
          Map<String, UserInfo> userInfoMap =
              authService.getUserInfoFromAuthServer(userIds, null, null, true);
          for (RepositoryEntity repositoryEntity : repositoryEntities) {
            var userInfoValue = userInfoMap.get(repositoryEntity.getOwner());
            if (userInfoValue != null) {
              try {
                var modelDBServiceResourceTypes =
                    ModelDBUtils.getModelDBServiceResourceTypesFromRepository(repositoryEntity);
                mdbRoleService.createRoleBinding(
                    ModelDBConstants.ROLE_REPOSITORY_OWNER,
                    new CollaboratorUser(authService, userInfoValue),
                    String.valueOf(repositoryEntity.getId()),
                    modelDBServiceResourceTypes);
              } catch (Exception e) {
                LOGGER.error(e.getMessage());
              }
            } else {
              LOGGER.warn(
                  "Repository owner not found from UAC response list : RepositoryId - {} & userId - {}",
                  repositoryEntity.getId(),
                  repositoryEntity.getOwner());
            }
          }
        } else {
          LOGGER.debug("Repositories total count 0");
        }

        transaction.commit();
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var criteriaBuilder = session.getCriteriaBuilder();
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
