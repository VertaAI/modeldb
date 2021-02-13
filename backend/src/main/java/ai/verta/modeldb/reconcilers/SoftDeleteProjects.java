package ai.verta.modeldb.reconcilers;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.Set;

public class SoftDeleteProjects extends Reconciler<String> {
  private static final Logger LOGGER = LogManager.getLogger(SoftDeleteProjects.class);
  private final ModelDBHibernateUtil modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
  private final RoleService roleService;

  public SoftDeleteProjects(ReconcilerConfig config, RoleService roleService) {
    super(config);
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
      deletedQuery.stream().forEach(id -> this.insert((String) id));
    }
  }

  @Override
  protected void reconcile(Set<String> ids) {
    roleService.deleteEntityResourcesWithServiceUser(
        new ArrayList<>(ids), ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
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

      transaction = session.beginTransaction();
      String delete =
          String.format("DELETE FROM %s WHERE id IN (:ids)", ProjectEntity.class.getSimpleName());
      Query deleteQuery = session.createQuery(delete);
      deleteQuery.setParameter("ids", ids);
      deleteQuery.executeUpdate();
      transaction.commit();
    } catch (Exception ex) {
      LOGGER.error("SoftDeleteProjects : reconcile : Exception: ", ex);
    }
  }
}
