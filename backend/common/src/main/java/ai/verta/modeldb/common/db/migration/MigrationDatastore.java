package ai.verta.modeldb.common.db.migration;

import ai.verta.modeldb.common.config.RdbConfig;
import java.sql.Connection;
import java.sql.SQLException;

public interface MigrationDatastore {
  String SCHEMA_MIGRATIONS_TABLE = "schema_migrations";

  /** Lock the database in order to perform migrations. */
  void lock() throws SQLException;

  void unlock() throws SQLException;

  void ensureMigrationTableExists() throws SQLException;

  static MigrationDatastore create(RdbConfig config, Connection connection) {
    if (config.isH2()) {
      return new H2MigrationDatastore(connection);
    }

    if (config.isMssql()) {
      return new SqlServerMigrationDatastore(connection);
    }

    if (config.isMysql()) {
      return new MySqlMigrationDatastore(connection);
    }

    throw new UnsupportedOperationException(
        "Unknown datastore for config. Requested dialect: " + config.getRdbDialect());
  }
}
