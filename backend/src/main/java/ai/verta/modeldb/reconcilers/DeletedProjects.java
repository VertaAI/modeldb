package ai.verta.modeldb.reconcilers;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ProjectEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.Set;

public class DeletedProjects extends Reconciler<String> {
  private static final Logger LOGGER = LogManager.getLogger(DeletedProjects.class);
  private final RoleService roleService;

  public DeletedProjects(ReconcilerConfig config, RoleService roleService) {
    super(config);
    this.roleService = roleService;
  }

  @Override
  public void resync() {
    LOGGER.trace("Resync deleted projects");
    String deleteProjectsQueryString =
        String.format(
            "select id from %s where deleted=:deleted", ProjectEntity.class.getSimpleName());

    Session session;

    Query projectDeleteQuery = session.createQuery(deleteProjectsQueryString);
    projectDeleteQuery.setParameter("deleted", true);
    projectDeleteQuery.stream().forEach(id -> this.insert((String) id));
  }

  @Override
  protected void reconcile(Set<String> ids) {
    Session session;

    try {
      roleService.deleteEntityResourcesWithServiceUser(
          new ArrayList<>(ids), ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);
      Transaction transaction = session.beginTransaction();
      String updateDeletedStatusExperimentQueryString =
          String.format(
              "UPDATE %s SET deleted=:deleted WHERE project_id IN (:ids)",
              ExperimentEntity.class.getSimpleName());
      Query deletedExperimentQuery = session.createQuery(updateDeletedStatusExperimentQueryString);
      deletedExperimentQuery.setParameter("deleted", true);
      deletedExperimentQuery.setParameter("ids", ids);
      deletedExperimentQuery.executeUpdate();
      transaction.commit();

      transaction = session.beginTransaction();
      String deleteProjects =
          String.format("DELETE FROM %s WHERE id in (:ids)", ProjectEntity.class.getSimpleName());
      Query deleteProjectsQuery = session.createQuery(deleteProjects);
      deleteProjectsQuery.setParameter("ids", ids);
      deleteProjectsQuery.executeUpdate();
      transaction.commit();
    } catch (Exception ex) {
      LOGGER.warn("DeletedProjects : reconcile : Exception: ", ex);
    }
  }
}
