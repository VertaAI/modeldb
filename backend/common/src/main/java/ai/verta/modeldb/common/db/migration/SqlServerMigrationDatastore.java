package ai.verta.modeldb.common.db.migration;

import java.sql.*;
import lombok.extern.log4j.Log4j2;

@Log4j2
class SqlServerMigrationDatastore implements MigrationDatastore {
  private final Connection connection;

  public SqlServerMigrationDatastore(Connection connection) {
    this.connection = connection;
  }

  @Override
  public void lock() throws SQLException {
    String lockId = generateLockId();

    try (CallableStatement cs = connection.prepareCall("{? = call sp_getapplock( ?, ?, ?, ? )}")) {
      cs.registerOutParameter(1, Types.INTEGER);
      cs.setString(2, lockId);
      cs.setString(3, "Update"); // lock mode
      cs.setString(4, "Session"); // lock owner
      cs.setInt(5, 0);
      cs.execute();
      int outValue = cs.getInt(1);
      if (outValue != 0) {
        throw new SQLException(
            "Failed to lock the database for migration. Result code: " + outValue);
      }
    }
  }

  @Override
  public void unlock() throws SQLException {
    String lockId = generateLockId();

    try (CallableStatement cs = connection.prepareCall("{? = call sp_releaseapplock( ?, ? )}")) {
      cs.registerOutParameter(1, Types.INTEGER);
      cs.setString(2, lockId);
      cs.setString(3, "Session"); // lock owner
      cs.executeUpdate();
      int outValue = cs.getInt(1);
      if (outValue != 0) {
        throw new SQLException(
            "Failed to unlock the database post-migration. Result code: " + outValue);
      }
    }
  }

  @Override
  public void ensureMigrationTableExists() throws SQLException {
    // the simplest way in sql server to see if a table exists is just to try to query it.
    try (PreparedStatement ps =
        connection.prepareStatement("select count(*) from " + SCHEMA_MIGRATIONS_TABLE)) {
      ps.executeQuery().close();
      return;
    } catch (SQLException e) {
      log.info(SCHEMA_MIGRATIONS_TABLE + " does not exist. Creating");
      // this means the table doesn't already exist, so go ahead and created it below...
    }

    String sql =
        "CREATE TABLE "
            + SCHEMA_MIGRATIONS_TABLE
            + " ( version BIGINT PRIMARY KEY NOT NULL, dirty BIT NOT NULL );";
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.execute();
    }
  }

  private String generateLockId() throws SQLException {
    String schema = connection.getSchema();
    return MigrationTools.generateLockId(schema, "schema_migrations");
  }
}
