package ai.verta.modeldb.common.db.migration;

import java.sql.*;

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
    String sql =
        "IF NOT EXISTS"
            + "(SELECT *  FROM sysobjects  WHERE id = object_id(N'[dbo].["
            + SCHEMA_MIGRATIONS_TABLE
            + "]') "
            + " AND OBJECTPROPERTY(id, N'IsUserTable') = 1 )"
            + "CREATE TABLE "
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
