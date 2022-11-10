package ai.verta.modeldb.migration;

import static org.assertj.core.api.Assertions.assertThat;

import ai.verta.modeldb.common.CommonDBUtil;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.db.migration.MigrationException;
import ai.verta.modeldb.common.db.migration.Migrator;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MigratorTest {

  private static RdbConfig createMysqlConfig() {
    return RdbConfig.builder()
        .RdbUrl("jdbc:mysql://localhost:3306")
        .RdbDriver("org.mariadb.jdbc.Driver")
        .RdbDialect("org.hibernate.dialect.MySQL5Dialect")
        .RdbDatabaseName("migrationTestDb")
        .RdbUsername("root")
        .RdbPassword("MyN3wP4ssw0rd!")
        .sslEnabled(false)
        .build();
  }

  private static RdbConfig createSqlServerConfig() {
    return RdbConfig.builder()
        .RdbUrl("jdbc:sqlserver://localhost:1433")
        .RdbDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver")
        .RdbDialect("org.hibernate.dialect.SQLServer2008Dialect")
        .RdbDatabaseName("migrationTestDb")
        .RdbUsername("SA")
        .RdbPassword("MyN3wP4ssw0rd!")
        .sslEnabled(false)
        .build();
  }

  @Test
  @Disabled("only run manually to test things against mysql for now")
  void release_2022_08_ddl_migration_mysql() throws Exception {
    RdbConfig config = createMysqlConfig();
    CommonDBUtil.createDBIfNotExists(config);
    Connection connection = buildStandardDbConnection(config);

    try {
      Migrator migrator = new Migrator(connection, "migrations/testing/mysql");
      verifyAllStateTransitions(config, connection, migrator);
    } finally {
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate(String.format("drop database %s;", config.getRdbDatabaseName()));
      }
    }
  }

  @Test
  @Disabled("only run manually to test things against sqlserver for now")
  void release_2022_08_ddl_migration_sqlserver() throws Exception {
    RdbConfig config = createSqlServerConfig();
    CommonDBUtil.createDBIfNotExists(config);
    Connection connection = buildStandardDbConnection(config);

    try {
      Migrator migrator = new Migrator(connection, "migrations/testing/sqlsvr");

      verifyAllStateTransitions(config, connection, migrator);
    } finally {
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate(
            String.format("USE Master; drop database %s;", config.getRdbDatabaseName()));
      }
    }
  }

  private static void verifyRelease202208DdlExecution(Connection connection, Migrator migrator)
      throws SQLException {
    try (ResultSet tables = connection.getMetaData().getTables(null, null, "artifact", null)) {
      assertThat(tables.next()).isTrue();
      assertThat(tables.getString("TABLE_NAME")).isEqualToIgnoringCase("artifact");
    }

    try (ResultSet tables =
        connection
            .getMetaData()
            .getTables(null, null, "versioning_modeldb_entity_mapping_config_blob", null)) {
      assertThat(tables.next()).isTrue();
      assertThat(tables.getString("TABLE_NAME"))
          .isEqualToIgnoringCase("versioning_modeldb_entity_mapping_config_blob");
    }
  }

  private static Connection buildStandardDbConnection(RdbConfig rdbConfig) throws SQLException {
    String connectionString = RdbConfig.buildDatabaseConnectionString(rdbConfig);
    return DriverManager.getConnection(
        connectionString, rdbConfig.getRdbUsername(), rdbConfig.getRdbPassword());
  }

  private static void verifyAllStateTransitions(
      RdbConfig config, Connection connection, Migrator migrator)
      throws SQLException, MigrationException, IOException {
    migrator.performMigration(config, 0);
    verifyVersionState(connection, 0);

    migrator.performMigration(config, 1);
    verifyVersionState(connection, 1);

    verifyRelease202208DdlExecution(connection, migrator);
  }

  private static void verifyVersionState(Connection connection, int expectedVersion)
      throws SQLException {
    try (PreparedStatement ps =
        connection.prepareStatement("select version, dirty from schema_migrations")) {
      ResultSet resultSet = ps.executeQuery();
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getLong(1)).isEqualTo(expectedVersion);
      assertThat(resultSet.getBoolean(2)).isEqualTo(false);
    }
  }
}
