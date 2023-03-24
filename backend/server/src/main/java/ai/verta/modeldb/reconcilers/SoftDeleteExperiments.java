package ai.verta.modeldb.reconcilers;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import io.opentelemetry.api.OpenTelemetry;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.hibernate.query.Query;

public class SoftDeleteExperiments extends Reconciler<String> {
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final MDBRoleService mdbRoleService;

  public SoftDeleteExperiments(
      ReconcilerConfig config,
      MDBRoleService mdbRoleService,
      FutureJdbi futureJdbi,
      FutureExecutor executor,
      OpenTelemetry openTelemetry) {
    super(config, futureJdbi, executor, openTelemetry, true);
    this.mdbRoleService = mdbRoleService;
  }

  @Override
  public void resync() {
    var queryString =
        String.format(
            "select id from %s where deleted=:deleted", ExperimentEntity.class.getSimpleName());

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Query<String> deletedQuery = session.createQuery(queryString, String.class);
      deletedQuery.setParameter("deleted", true);
      deletedQuery.setMaxResults(config.getMaxSync());
      deletedQuery.stream().forEach(this::insert);
    }
  }

  @Override
  protected ReconcileResult reconcile(Set<String> ids) {
    logger.debug(() -> "Reconciling experiments " + ids.toString());

    deleteRoleBindings(ids);

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentQueryString =
          String.format("from %s where id in (:ids)", ExperimentEntity.class.getSimpleName());

      Query<ExperimentEntity> experimentDeleteQuery =
          session.createQuery(experimentQueryString, ExperimentEntity.class);
      experimentDeleteQuery.setParameter("ids", ids);
      List<ExperimentEntity> experimentEntities = experimentDeleteQuery.list();

      var transaction = session.beginTransaction();
      var updateDeletedChildren =
          String.format(
              "UPDATE %s SET deleted=:deleted WHERE experiment_id IN (:ids)",
              ExperimentRunEntity.class.getSimpleName());
      var updateDeletedChildrenQuery = session.createQuery(updateDeletedChildren);
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var deleteExperimentQueryString =
          String.format("FROM %s WHERE id IN (:ids)", ExperimentEntity.class.getSimpleName());

      Query<ExperimentEntity> experimentDeleteQuery =
          session.createQuery(deleteExperimentQueryString, ExperimentEntity.class);
      experimentDeleteQuery.setParameter("ids", ids);
      List<ExperimentEntity> experimentEntities = experimentDeleteQuery.list();

      List<String> roleBindingNames = new LinkedList<>();
      for (ExperimentEntity experimentEntity : experimentEntities) {
        String ownerRoleBindingName =
            mdbRoleService.buildRoleBindingName(
                ModelDBConstants.ROLE_EXPERIMENT_OWNER,
                experimentEntity.getId(),
                experimentEntity.getOwner(),
                ModelDBResourceEnum.ModelDBServiceResourceTypes.EXPERIMENT.name());
        if (ownerRoleBindingName != null) {
          roleBindingNames.add(ownerRoleBindingName);
        }
      }
      if (!roleBindingNames.isEmpty()) {
        mdbRoleService.deleteRoleBindingsUsingServiceUser(roleBindingNames);
      }
    }
  }
}
