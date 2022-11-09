package ai.verta.modeldb.common.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySqlMigrationDatastore implements MigrationDatastore {
  private final Connection connection;

  public MySqlMigrationDatastore(Connection connection) {
    this.connection = connection;
  }

  @Override
  public void lock() throws SQLException {
    String catalog = connection.getCatalog();
    String lockId = MigrationTools.generateLockId(catalog, SCHEMA_MIGRATIONS_TABLE);
    PreparedStatement ps = connection.prepareStatement("SELECT GET_LOCK(?,?)");
    ps.setString(1, lockId);
    ps.setInt(2, 1); // timeout in seconds
    ResultSet resultSet = ps.executeQuery();
    if (resultSet.next()) {
      int result = resultSet.getInt(1);
      if (result != 1) {
        throw new SQLException("Failed to lock the database for migration. Result code: " + result);
      }
    }
  }

  @Override
  public void unlock() throws SQLException {
    String catalog = connection.getCatalog();
    String lockId = MigrationTools.generateLockId(catalog, SCHEMA_MIGRATIONS_TABLE);
    PreparedStatement ps = connection.prepareStatement("SELECT RELEASE_LOCK(?)");
    ps.setString(1, lockId);
    ResultSet resultSet = ps.executeQuery();
    if (resultSet.next()) {
      int result = resultSet.getInt(1);
      if (result != 1) {
        throw new SQLException(
            "Failed to unlock the database post-migration. Result code: " + result);
      }
    }
  }

  @Override
  public void ensureMigrationTableExists() throws SQLException {
    try (PreparedStatement ps =
        connection.prepareStatement(
            "CREATE TABLE IF NOT EXISTS "
                + MigrationDatastore.SCHEMA_MIGRATIONS_TABLE
                + " (version bigint not null primary key, dirty boolean not null)")) {
      ps.execute();
    }
  }
}
