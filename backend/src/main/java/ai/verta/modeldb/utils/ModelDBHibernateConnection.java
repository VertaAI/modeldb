package ai.verta.modeldb.utils;

import ai.verta.modeldb.common.HibernateConnection;
import org.hibernate.Session;

public class ModelDBHibernateConnection extends HibernateConnection {
  @Override
  public boolean checkDBConnection() {
    return ModelDBHibernateUtil.checkDBConnection();
  }

  @Override
  public void resetSessionFactory() {
    ModelDBHibernateUtil.resetSessionFactory();
  }

  @Override
  public Session openSession() {
    return ModelDBHibernateUtil.getSessionFactory().openSession();
  }
}
