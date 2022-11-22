package ai.verta.modeldb.migration;

import static org.assertj.core.api.Assertions.assertThat;

import ai.verta.modeldb.common.CommonDBUtil;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.db.migration.Migration;
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

  @Test
  void release_2022_08_ddl_migration_h2() throws Exception {
    var config = createH2Config();
    Connection connection = buildH2Connection(config);

    Migrator migrator = new Migrator(connection, "migration/h2", config);
    verifyStateTransitions(config, connection, migrator);
  }

  @Test
  void isDirty_migration_h2() throws Exception {
    var config = createH2Config();
    Connection connection = buildStandardDbConnection(config);

    Migrator migrator = new Migrator(connection, "migration/h2", config);

    migrator.performMigration(0);
    verifyVersionState(connection, 0);

    migrator.performMigration(1);
    verifyVersionState(connection, 1);

    migrator.performMigration(2);
    verifyVersionState(connection, 2);

    migrator.performMigration(3);
    verifyVersionState(connection, 3);

    /* While downgrading migration to 0 the second version 2_release_2022_10.down.sql is failing
     *  so migrator tool will mark migration 2 as dirty
     *  EX: migrator.performMigration(0);
     *     verifyVersionState(connection, 0);
     */
    migrator.executeSingleMigration(new Migration("3_release_2022_10.down.sql"));
    try (PreparedStatement ps =
        connection.prepareStatement("UPDATE schema_migrations set version = ?, dirty = ?")) {
      ps.setInt(1, 2);
      ps.setBoolean(2, true);
      ps.executeUpdate();
    }

    migrator.performMigration();
    verifyVersionState(connection, 3);
  }

  @Test
  @Disabled("only run manually to test things against mysql for now")
  void release_2022_08_ddl_migration_mysql() throws Exception {
    RdbConfig config = createMysqlConfig();
    CommonDBUtil.createDBIfNotExists(config);
    Connection connection = buildStandardDbConnection(config);

    try {
      Migrator migrator = new Migrator(connection, "migration/mysql", config);
      verifyStateTransitions(config, connection, migrator);
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
      Migrator migrator = new Migrator(connection, "migration/sqlsvr", config);

      verifyStateTransitions(config, connection, migrator);
    } finally {
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate(
            String.format("USE Master; drop database %s;", config.getRdbDatabaseName()));
      }
    }
  }

  private static void verifyStateTransitions(
      RdbConfig config, Connection connection, Migrator migrator)
      throws SQLException, MigrationException, IOException {
    migrator.performMigration(0);
    verifyVersionState(connection, 0);

    migrator.performMigration(1);
    verifyVersionState(connection, 1);

    verifyRelease202208DdlExecution(connection, migrator);

    if (!config.isH2()) {
      // our H2 down migration doesn't do anything (since H2 is only in-memory)
      verifyTablesDropped(connection);
    }
  }

  private static void verifyRelease202208DdlExecution(Connection connection, Migrator migrator)
      throws IOException, SQLException {
    try (ResultSet tables = connection.getMetaData().getTables(null, null, "ARTIFACT", null)) {
      assertThat(tables.next()).isTrue();
      assertThat(tables.getString("TABLE_NAME")).isEqualToIgnoringCase("artifact");
    }

    try (ResultSet tables =
        connection
            .getMetaData()
            .getTables(null, null, "VERSIONING_MODELDB_ENTITY_MAPPING_CONFIG_BLOB", null)) {
      assertThat(tables.next()).isTrue();
      assertThat(tables.getString("TABLE_NAME"))
          .isEqualToIgnoringCase("versioning_modeldb_entity_mapping_config_blob");
    }

    migrator.executeSingleMigration(new Migration("1_release_2022_08.down.sql"));
  }

  private static void verifyTablesDropped(Connection connection) throws SQLException {
    try (ResultSet tables = connection.getMetaData().getTables(null, null, "ARTIFACT", null)) {
      assertThat(tables.next()).isFalse();
    }

    try (ResultSet tables =
        connection
            .getMetaData()
            .getTables(null, null, "VERSIONING_MODELDB_ENTITY_MAPPING_CONFIG_BLOB", null)) {
      assertThat(tables.next()).isFalse();
    }
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

  private static Connection buildStandardDbConnection(RdbConfig rdbConfig) throws SQLException {
    String connectionString = RdbConfig.buildDatabaseConnectionString(rdbConfig);
    return DriverManager.getConnection(
        connectionString, rdbConfig.getRdbUsername(), rdbConfig.getRdbPassword());
  }

  static Connection buildH2Connection(RdbConfig config) throws SQLException {
    return buildStandardDbConnection(config);
  }

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
        .RdbDatabaseName("fullMigrationTestDb")
        .RdbUsername("SA")
        .RdbPassword("MyPass@word")
        .sslEnabled(false)
        .build();
  }

  private static RdbConfig createH2Config() {
    return RdbConfig.builder()
        .DBConnectionURL("jdbc:h2:mem:fullMigrationTestDb")
        .RdbDriver("org.h2.Driver")
        .RdbDialect("org.hibernate.dialect.H2Dialect")
        .RdbDatabaseName("fullMigrationTestDb")
        .RdbPassword("password")
        .RdbUsername("sa")
        .build();
  }
}
