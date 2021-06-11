package ai.verta.modeldb.reconcilers;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class SoftDeleteProjects extends Reconciler<String> {
  private static final Logger LOGGER = LogManager.getLogger(SoftDeleteProjects.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final RoleService roleService;

  public SoftDeleteProjects(
      ReconcilerConfig config, RoleService roleService, FutureJdbi futureJdbi, Executor executor) {
    super(config, LOGGER, futureJdbi, executor);
    this.roleService = roleService;
  }

  @Override
  public void resync() {
    String queryString =
        String.format(
            "select id from %s where deleted=:deleted", ProjectEntity.class.getSimpleName());

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Query deletedQuery = session.createQuery(queryString);
      deletedQuery.setParameter("deleted", true);
      deletedQuery.setMaxResults(config.maxSync);
      deletedQuery.stream().forEach(id -> this.insert((String) id));
    }
  }

  @Override
  protected ReconcileResult reconcile(Set<String> ids) {
    LOGGER.debug("Reconciling projects " + ids.toString());

    roleService.deleteEntityResourcesWithServiceUser(
        new ArrayList<>(ids), ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      String projectsQueryString =
          String.format("from %s where id in (:ids)", ProjectEntity.class.getSimpleName());

      Query projectDeleteQuery = session.createQuery(projectsQueryString);
      projectDeleteQuery.setParameter("ids", ids);
      List<ProjectEntity> projectEntities = projectDeleteQuery.list();

      Transaction transaction = session.beginTransaction();
      String updateDeletedChildren =
          String.format(
              "UPDATE %s SET deleted=:deleted WHERE project_id IN (:ids)",
              ExperimentEntity.class.getSimpleName());
      Query updateDeletedChildrenQuery = session.createQuery(updateDeletedChildren);
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
