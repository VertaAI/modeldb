package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import io.opentelemetry.api.OpenTelemetry;
import java.util.List;
import java.util.Set;
import org.hibernate.query.Query;

public class SoftDeleteExperiments extends Reconciler<String> {
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();

  public SoftDeleteExperiments(
      ReconcilerConfig config, FutureJdbi futureJdbi, OpenTelemetry openTelemetry) {
    super(config, futureJdbi, openTelemetry, true);
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
      deletedQuery.stream().forEach(id -> this.insert((String) id));
    }
  }

  @Override
  protected ReconcileResult reconcile(Set<String> ids) {
    logger.debug("Reconciling experiments " + ids.toString());

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
}
