package ai.verta.modeldb.reconcilers;

import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Set;

public abstract class DBSoftDeleter extends Reconciler<String> {
  private static final Logger LOGGER = LogManager.getLogger(SoftDeleteProjects.class);
  private final ModelDBHibernateUtil modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();

  protected DBSoftDeleter(ReconcilerConfig config) {
    super(config);
  }

  protected abstract String selfTableName();

  protected abstract Set<String> childrenTableName();

  protected abstract void deleteExternal(Set<String> ids);

  @Override
  public void resync() {
    String queryString = String.format("select id from %s where deleted=:deleted", selfTableName());

    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Query deletedQuery = session.createQuery(queryString);
      deletedQuery.setParameter("deleted", true);
      deletedQuery.stream().forEach(id -> this.insert((String) id));
    }
  }

  @Override
  protected void reconcile(Set<String> ids) {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      deleteExternal(ids);

      for (String childTable : childrenTableName()) {
        Transaction transaction = session.beginTransaction();
        String updateDeletedChildren =
            String.format(
                "UPDATE %s SET deleted=:deleted WHERE %s_id IN (:ids)",
                childTable, selfTableName().toLowerCase());
        Query updateDeletedChildrenQuery = session.createQuery(updateDeletedChildren);
        updateDeletedChildrenQuery.setParameter("deleted", true);
        updateDeletedChildrenQuery.setParameter("ids", ids);
        updateDeletedChildrenQuery.executeUpdate();
        transaction.commit();
      }

      Transaction transaction = session.beginTransaction();
      String delete = String.format("DELETE FROM %s WHERE id IN (:ids)", selfTableName());
      Query deleteQuery = session.createQuery(delete);
      deleteQuery.setParameter("ids", ids);
      deleteQuery.executeUpdate();
      transaction.commit();
    } catch (Exception ex) {
      LOGGER.warn("DeletedProjects : reconcile : Exception: ", ex);
    }
  }
}
