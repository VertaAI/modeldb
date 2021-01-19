package ai.verta.modeldb.utils;

import ai.verta.modeldb.common.HibernateConnection;

public class ModelDBHibernateConnection extends HibernateConnection {
  @Override
  public boolean checkDBConnection() {
    return ModelDBHibernateUtil.checkDBConnection();
  }

  @Override
  public void resetSessionFactory() {
    ModelDBHibernateUtil.resetSessionFactory();
  }
}
