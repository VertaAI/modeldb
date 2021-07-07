package ai.verta.modeldb.reconcilers;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class SoftDeleteExperiments extends Reconciler<String> {
  private static final Logger LOGGER = LogManager.getLogger(SoftDeleteExperiments.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final RoleService roleService;

  public SoftDeleteExperiments(
      ReconcilerConfig config, RoleService roleService, FutureJdbi futureJdbi, Executor executor) {
    super(config, LOGGER, futureJdbi, executor);
    this.roleService = roleService;
  }

  @Override
  public void resync() {
    String queryString =
        String.format(
            "select id from %s where deleted=:deleted", ExperimentEntity.class.getSimpleName());

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Query<String> deletedQuery = session.createQuery(queryString, String.class);
      deletedQuery.setParameter("deleted", true);
      deletedQuery.setMaxResults(config.maxSync);
      deletedQuery.stream().forEach(id -> this.insert((String) id));
    }
  }

  @Override
  protected ReconcileResult reconcile(Set<String> ids) {
    LOGGER.debug("Reconciling experiments " + ids.toString());

    deleteRoleBindings(ids);

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      String experimentQueryString =
          String.format("from %s where id in (:ids)", ExperimentEntity.class.getSimpleName());

      Query<ExperimentEntity> experimentDeleteQuery =
          session.createQuery(experimentQueryString, ExperimentEntity.class);
      experimentDeleteQuery.setParameter("ids", ids);
      List<ExperimentEntity> experimentEntities = experimentDeleteQuery.list();

      Transaction transaction = session.beginTransaction();
      String updateDeletedChildren =
          String.format(
              "UPDATE %s SET deleted=:deleted WHERE experiment_id IN (:ids)",
              ExperimentRunEntity.class.getSimpleName());
      Query updateDeletedChildrenQuery = session.createQuery(updateDeletedChildren);
      updateDeletedChildrenQuery.setParameter("deleted", true);
      updateDeletedChildrenQuery.setParameter("ids", ids);
      updateDeletedChildrenQuery.executeUpdate();
      transaction.commit();

      for (ExperimentEntity experimentEntity : experimentEntities) {
        transaction = session.beginTransaction();
        session.delete(experimentEntity);
        transaction.commit();
      }
    }

    return new ReconcileResult();
  }

  private void deleteRoleBindings(Set<String> ids) {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      String deleteExperimentQueryString =
          String.format("FROM %s WHERE id IN (:ids)", ExperimentEntity.class.getSimpleName());

      Query<ExperimentEntity> experimentDeleteQuery =
          session.createQuery(deleteExperimentQueryString, ExperimentEntity.class);
      experimentDeleteQuery.setParameter("ids", ids);
      List<ExperimentEntity> experimentEntities = experimentDeleteQuery.list();

      List<String> roleBindingNames = new LinkedList<>();
      for (ExperimentEntity experimentEntity : experimentEntities) {
        String ownerRoleBindingName =
            roleService.buildRoleBindingName(
                ModelDBConstants.ROLE_EXPERIMENT_OWNER,
                experimentEntity.getId(),
                experimentEntity.getOwner(),
                ModelDBResourceEnum.ModelDBServiceResourceTypes.EXPERIMENT.name());
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
