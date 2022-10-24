package ai.verta.modeldb.reconcilers;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.entities.CommentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.hibernate.query.Query;

public class SoftDeleteExperimentRuns extends Reconciler<String> {
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final MDBRoleService mdbRoleService;

  public SoftDeleteExperimentRuns(
      ReconcilerConfig config,
      MDBRoleService mdbRoleService,
      FutureJdbi futureJdbi,
      FutureExecutor executor) {
    super(config, LogManager.getLogger(SoftDeleteExperimentRuns.class), futureJdbi, executor, true);
    this.mdbRoleService = mdbRoleService;
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

    deleteRoleBindings(ids);

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

  private void deleteRoleBindings(Set<String> ids) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var deleteExperimentRunQueryString =
          String.format("FROM %s WHERE id IN (:ids)", ExperimentRunEntity.class.getSimpleName());

      Query<ExperimentRunEntity> experimentRunDeleteQuery =
          session.createQuery(deleteExperimentRunQueryString, ExperimentRunEntity.class);
      experimentRunDeleteQuery.setParameter("ids", ids);
      List<ExperimentRunEntity> experimentRunEntities = experimentRunDeleteQuery.list();

      List<String> roleBindingNames = new LinkedList<>();
      for (ExperimentRunEntity entity : experimentRunEntities) {
        String ownerRoleBindingName =
            mdbRoleService.buildRoleBindingName(
                ModelDBConstants.ROLE_EXPERIMENT_RUN_OWNER,
                entity.getId(),
                entity.getOwner(),
                ModelDBResourceEnum.ModelDBServiceResourceTypes.EXPERIMENT_RUN.name());
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
