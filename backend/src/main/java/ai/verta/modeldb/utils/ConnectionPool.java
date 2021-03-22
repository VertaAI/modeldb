package ai.verta.modeldb.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface ConnectionPool {
  Connection getConnection() throws SQLException, ClassNotFoundException;

  boolean releaseConnection(Connection connection);

  List<Connection> getConnectionPool();

  int getSize();

  void shutdown() throws SQLException;
}
