package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.entities.CommentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import io.opentelemetry.api.OpenTelemetry;
import java.util.List;
import java.util.Set;
import org.hibernate.query.Query;

public class SoftDeleteExperimentRuns extends Reconciler<String> {
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();

  public SoftDeleteExperimentRuns(
      ReconcilerConfig config, FutureJdbi futureJdbi, OpenTelemetry openTelemetry) {
    super(config, futureJdbi, openTelemetry, true);
  }

  @Override
  public void resync() {
    var queryString =
        String.format(
            "select id from %s where deleted=:deleted", ExperimentRunEntity.class.getSimpleName());

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Query<String> deletedQuery = session.createQuery(queryString, String.class);
      deletedQuery.setParameter("deleted", true);
      deletedQuery.setMaxResults(config.getMaxSync());
      deletedQuery.stream().forEach(id -> this.insert((String) id));
    }
  }

  @Override
  protected ReconcileResult reconcile(Set<String> ids) {
    logger.debug("Reconciling experiment runs " + ids.toString());

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunQueryString =
          String.format("from %s where id in (:ids)", ExperimentRunEntity.class.getSimpleName());

      Query<ExperimentRunEntity> experimentRunDeleteQuery =
          session.createQuery(experimentRunQueryString, ExperimentRunEntity.class);
      experimentRunDeleteQuery.setParameter("ids", ids);
      List<ExperimentRunEntity> experimentRunEntities = experimentRunDeleteQuery.list();

      var transaction = session.beginTransaction();
      var delete =
          String.format("FROM %s WHERE entity_id IN (:ids)", CommentEntity.class.getSimpleName());
      var deleteQuery = session.createQuery(delete);
      deleteQuery.setParameterList("ids", ids);
      List<CommentEntity> comments = deleteQuery.list();
      for (CommentEntity commentEntity : comments) {
        session.delete(commentEntity);
      }
      transaction.commit();

      for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
        transaction = session.beginTransaction();
        session.delete(experimentRunEntity);
        transaction.commit();
      }
    }

    return new ReconcileResult();
  }
}
