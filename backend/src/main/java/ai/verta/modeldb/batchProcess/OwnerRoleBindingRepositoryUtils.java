package ai.verta.modeldb.batchProcess;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.Role;
import ai.verta.uac.UserInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OwnerRoleBindingRepositoryUtils {
  private OwnerRoleBindingRepositoryUtils() {}

  private static final Logger LOGGER = LogManager.getLogger(OwnerRoleBindingUtils.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private static AuthService authService;
  private static UAC uac;
  private static RoleService roleService;

  public static void execute() {
    if (Config.getInstance().hasAuth()) {
      authService = AuthServiceUtils.FromConfig(Config.getInstance());
      uac = UAC.FromConfig(Config.getInstance());
      roleService = RoleServiceUtils.FromConfig(Config.getInstance(), authService, uac);
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

    int lowerBound = 0;
    final int pagesize = 5000;
    LOGGER.debug("Total repositories {}", count);

    Role ownerRole = roleService.getRoleByName(ModelDBConstants.ROLE_REPOSITORY_OWNER, null);
    while (lowerBound < count) {

      try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        Transaction transaction = session.beginTransaction();
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

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
              authService.getUserInfoFromAuthServer(userIds, null, null);
          for (RepositoryEntity repositoryEntity : repositoryEntities) {
            UserInfo userInfoValue = userInfoMap.get(repositoryEntity.getOwner());
            if (userInfoValue != null) {
              try {
                ModelDBServiceResourceTypes modelDBServiceResourceTypes =
                    ModelDBUtils.getModelDBServiceResourceTypesFromRepository(repositoryEntity);
                roleService.createRoleBinding(
                    ownerRole,
                    new CollaboratorUser(authService, userInfoValue),
                    String.valueOf(repositoryEntity.getId()),
                    modelDBServiceResourceTypes);
              } catch (Exception e) {
                LOGGER.error(e.getMessage());
              }
            } else {
              LOGGER.info(
                  "Repository owner not found from UAC response list : RepositoryId - {} & userId - {}",
                  repositoryEntity.getId(),
                  repositoryEntity.getOwner());
              new ModelDBException(
                      "Repository owner not found from UAC response list : RepositoryId - "
                          + repositoryEntity.getId()
                          + " & userId - "
                          + repositoryEntity.getOwner())
                  .printStackTrace();
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
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
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
