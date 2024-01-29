package ai.verta.modeldb.reconcilers;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import io.opentelemetry.api.OpenTelemetry;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class SoftDeleteProjects extends Reconciler<String> {
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final MDBRoleService mdbRoleService;

  public SoftDeleteProjects(
      ReconcilerConfig config,
      MDBRoleService mdbRoleService,
      FutureJdbi futureJdbi,
      OpenTelemetry openTelemetry) {
    super(config, futureJdbi, openTelemetry, true);
    this.mdbRoleService = mdbRoleService;
  }

  @Override
  public void resync() {
    var queryString =
        String.format(
            "select id from %s where deleted=:deleted OR (created=:created AND date_created < :dateCreated) ",
            ProjectEntity.class.getSimpleName());

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var deletedQuery = session.createQuery(queryString);
      deletedQuery.setParameter("deleted", true);
      deletedQuery.setParameter("created", false);
      deletedQuery.setParameter("dateCreated", new Date().getTime() - 60000L); // before 1 min
      deletedQuery.setMaxResults(config.getMaxSync());
      deletedQuery.stream().forEach(id -> this.insert((String) id));
    }
  }

  @Override
  protected ReconcileResult reconcile(Set<String> ids) {
    logger.debug("Reconciling projects " + ids.toString());

    mdbRoleService.deleteEntityResourcesWithServiceUser(
        new ArrayList<>(ids), ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var projectsQueryString =
          String.format("from %s where id in (:ids)", ProjectEntity.class.getSimpleName());

      var projectDeleteQuery = session.createQuery(projectsQueryString);
      projectDeleteQuery.setParameter("ids", ids);
      List<ProjectEntity> projectEntities = projectDeleteQuery.list();

      var transaction = session.beginTransaction();
      var updateDeletedChildren =
          String.format(
              "UPDATE %s SET deleted=:deleted WHERE project_id IN (:ids)",
              ExperimentEntity.class.getSimpleName());
      var updateDeletedChildrenQuery = session.createQuery(updateDeletedChildren);
      updateDeletedChildrenQuery.setParameter("deleted", true);
      updateDeletedChildrenQuery.setParameter("ids", ids);
      updateDeletedChildrenQuery.executeUpdate();
      transaction.commit();

      // TODO: figure out if doing a single query to delete all projects
      for (ProjectEntity projectEntity : projectEntities) {
        transaction = session.beginTransaction();
        session.delete(projectEntity);
        transaction.commit();
      }
    }

    return new ReconcileResult();
  }
}
