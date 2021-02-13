package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.entities.ProjectEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class DeletedProjects extends Reconciler<String> {
  private static final Logger LOGGER = LogManager.getLogger(DeletedProjects.class);

  public DeletedProjects(ReconcilerConfig config) {
    super(config);
  }

  @Override
  public void resync() {
    LOGGER.trace("Resync deleted projects");
    String deleteProjectsQueryString =
        String.format("select id from %s where deleted=1", ProjectEntity.class.getSimpleName());

    Session session;

    Query projectDeleteQuery = session.createQuery(deleteProjectsQueryString);
    projectDeleteQuery.stream().forEach(id -> this.insert((String) id));
  }

  @Override
  protected void reconcile(String id) {}
}
