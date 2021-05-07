package ai.verta.modeldb.utils;

import ai.verta.modeldb.common.CommonHibernateUtil;
import ai.verta.modeldb.common.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BasicConnectionPool implements ConnectionPool {
  private final DatabaseConfig databaseConfig;
  private final List<Connection> connectionPool;
  private final List<Connection> usedConnections = new ArrayList<>();

  public static BasicConnectionPool create(DatabaseConfig databaseConfig)
      throws SQLException, ClassNotFoundException {
    int initialPoolSize = Integer.parseInt(databaseConfig.minConnectionPoolSize);
    List<Connection> pool = new ArrayList<>(initialPoolSize);
    for (int i = 0; i < initialPoolSize; i++) {
      pool.add(CommonHibernateUtil.getDBConnection(databaseConfig.RdbConfiguration));
    }
    return new BasicConnectionPool(databaseConfig, pool);
  }

  private BasicConnectionPool(DatabaseConfig databaseConfig, List<Connection> connectionPool) {
    this.databaseConfig = databaseConfig;
    this.connectionPool = connectionPool;
  }

  @Override
  public Connection getConnection() throws SQLException, ClassNotFoundException {
    if (connectionPool.isEmpty()) {
      int maxPoolSize = Integer.parseInt(databaseConfig.minConnectionPoolSize);
      if (usedConnections.size() < maxPoolSize) {
        connectionPool.add(CommonHibernateUtil.getDBConnection(databaseConfig.RdbConfiguration));
      } else {
        throw new RuntimeException("Maximum pool size reached, no available connections!");
      }
    }

    Connection connection = connectionPool.remove(connectionPool.size() - 1);

    int maxTimeout = Integer.parseInt(databaseConfig.connectionTimeout);
    if (!connection.isValid(maxTimeout)) {
      connection = CommonHibernateUtil.getDBConnection(databaseConfig.RdbConfiguration);
    }

    usedConnections.add(connection);
    return connection;
  }

  @Override
  public boolean releaseConnection(Connection connection) {
    connectionPool.add(connection);
    return usedConnections.remove(connection);
  }

  @Override
  public int getSize() {
    return connectionPool.size() + usedConnections.size();
  }

  @Override
  public List<Connection> getConnectionPool() {
    return connectionPool;
  }

  @Override
  public void shutdown() throws SQLException {
    usedConnections.forEach(this::releaseConnection);
    for (Connection c : connectionPool) {
      c.close();
    }
    connectionPool.clear();
  }
}
