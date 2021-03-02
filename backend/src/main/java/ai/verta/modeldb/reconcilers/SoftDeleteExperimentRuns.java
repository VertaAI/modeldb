package ai.verta.modeldb.reconcilers;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.entities.CommentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SoftDeleteExperimentRuns extends Reconciler<String> {
  private static final Logger LOGGER = LogManager.getLogger(SoftDeleteExperimentRuns.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final RoleService roleService;

  public SoftDeleteExperimentRuns(ReconcilerConfig config, RoleService roleService) {
    super(config, LOGGER);
    this.roleService = roleService;
  }

  @Override
  public void resync() {
    String queryString =
        String.format(
            "select id from %s where deleted=:deleted", ExperimentRunEntity.class.getSimpleName());

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Query<String> deletedQuery = session.createQuery(queryString, String.class);
      deletedQuery.setParameter("deleted", true);
      deletedQuery.setMaxResults(config.maxSync);
      deletedQuery.stream().forEach(id -> this.insert((String) id));
    }
  }

  @Override
  protected ReconcileResult reconcile(Set<String> ids) {
    LOGGER.debug("Reconciling experiment runs " + ids.toString());

    deleteRoleBindings(ids);

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      String experimentRunQueryString =
          String.format("from %s where id in (:ids)", ExperimentRunEntity.class.getSimpleName());

      Query<ExperimentRunEntity> experimentRunDeleteQuery =
          session.createQuery(experimentRunQueryString, ExperimentRunEntity.class);
      experimentRunDeleteQuery.setParameter("ids", ids);
      List<ExperimentRunEntity> experimentRunEntities = experimentRunDeleteQuery.list();

      Transaction transaction = session.beginTransaction();
      String delete =
          String.format(
              "DELETE FROM %s WHERE entity_id IN (:ids)", CommentEntity.class.getSimpleName());
      Query deleteQuery = session.createQuery(delete);
      deleteQuery.setParameter("ids", ids);
      deleteQuery.executeUpdate();
      transaction.commit();

      for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
        transaction = session.beginTransaction();
        session.delete(experimentRunEntity);
        transaction.commit();
      }
    } catch (Exception ex) {
      LOGGER.error("reconcile: ", ex);
    }

    return new ReconcileResult();
  }

  private void deleteRoleBindings(Set<String> ids) {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      String deleteExperimentRunQueryString =
          String.format("FROM %s WHERE id IN (:ids)", ExperimentRunEntity.class.getSimpleName());

      Query<ExperimentRunEntity> experimentRunDeleteQuery =
          session.createQuery(deleteExperimentRunQueryString, ExperimentRunEntity.class);
      experimentRunDeleteQuery.setParameter("ids", ids);
      List<ExperimentRunEntity> experimentRunEntities = experimentRunDeleteQuery.list();

      List<String> roleBindingNames = new LinkedList<>();
      for (ExperimentRunEntity entity : experimentRunEntities) {
        String ownerRoleBindingName =
            roleService.buildRoleBindingName(
                ModelDBConstants.ROLE_EXPERIMENT_RUN_OWNER,
                entity.getId(),
                entity.getOwner(),
                ModelDBResourceEnum.ModelDBServiceResourceTypes.EXPERIMENT_RUN.name());
        if (ownerRoleBindingName != null) {
          roleBindingNames.add(ownerRoleBindingName);
        }
      }
      if (!roleBindingNames.isEmpty()) {
        roleService.deleteRoleBindings(roleBindingNames);
      }
    }
  }
}
