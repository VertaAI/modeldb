package ai.verta.modeldb.common.db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import ai.verta.modeldb.common.CommonDBUtil;
import ai.verta.modeldb.common.config.RdbConfig;
import java.io.IOException;
import java.sql.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MigratorTest {
  @Test
  void singleMigration_h2() throws Exception {
    Connection connection = buildH2Connection();

    Migrator migrator = new Migrator(connection, "migrations/testing/h2");

    verifyDdlExecution(connection, migrator);
  }

  @Test
  @Disabled("only run manually to test things against sqlserver for now")
  void singleMigration_sqlserver() throws Exception {
    RdbConfig config = createSqlServerConfig();
    CommonDBUtil.createDBIfNotExists(config);
    Connection connection = buildStandardDbConnection(config);

    Migrator migrator = new Migrator(connection, "migrations/testing/sqlsvr");

    verifyDdlExecution(connection, migrator);
  }

  @Test
  @Disabled("only run manually to test things against mysql for now")
  void singleMigration_mysql() throws Exception {
    RdbConfig config = createMysqlConfig();
    CommonDBUtil.createDBIfNotExists(config);
    Connection connection = buildStandardDbConnection(config);

    Migrator migrator = new Migrator(connection, "migrations/testing/mysql");

    verifyDdlExecution(connection, migrator);
  }

  private static void verifyDdlExecution(Connection connection, Migrator migrator)
      throws IOException, SQLException {
    migrator.executeMigration(new Migration("1_create_test_table.up.sql"));
    try (ResultSet tables = connection.getMetaData().getTables(null, null, "TEST_TABLE", null)) {
      assertThat(tables.next()).isTrue();
      assertThat(tables.getString("TABLE_NAME")).isEqualToIgnoringCase("TEST_TABLE");
    }

    try (ResultSet rs = connection.prepareStatement("SELECT i from test_table").executeQuery()) {
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt("i")).isEqualTo(999);
    }

    migrator.executeMigration(new Migration("1_create_test_table.down.sql"));

    try (ResultSet tables = connection.getMetaData().getTables(null, null, "TEST_TABLE", null)) {
      assertThat(tables.next()).isFalse();
    }
  }

  @Test
  void performMigration_h2() throws Exception {
    RdbConfig config = createH2Config();
    Connection connection = buildStandardDbConnection(config);
    Migrator migrator = new Migrator(connection, "migrations/testing/h2");

    migrator.performMigration(config);

    verifyMigrations(connection);
  }

  private static void verifyMigrations(Connection connection) throws SQLException {
    try (PreparedStatement ps =
        connection.prepareStatement("select count(*) from schema_migrations")) {
      ResultSet resultSet = ps.executeQuery();
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getLong(1)).isEqualTo(0);
    }
  }

  @Test
  @Disabled
  void performMigration_sqlServer() throws Exception {
    RdbConfig config = createSqlServerConfig();
    CommonDBUtil.createDBIfNotExists(config);
    Connection connection = buildStandardDbConnection(config);
    Migrator migrator = new Migrator(connection, "migrations/testing/sqlsvr");

    migrator.performMigration(config);
    verifyMigrations(connection);
  }

  @Test
  @Disabled
  void performMigration_mySql() throws Exception {
    RdbConfig config = createMysqlConfig();
    CommonDBUtil.createDBIfNotExists(config);
    Connection connection = buildStandardDbConnection(config);
    Migrator migrator = new Migrator(connection, "migrations/testing/mysql");
    migrator.performMigration(config);
    verifyMigrations(connection);
  }

  private static Connection buildH2Connection() throws SQLException {
    return buildStandardDbConnection(createH2Config());
  }

  private static Connection buildStandardDbConnection(RdbConfig rdbConfig) throws SQLException {
    String connectionString = RdbConfig.buildDatabaseConnectionString(rdbConfig);
    return DriverManager.getConnection(
        connectionString, rdbConfig.getRdbUsername(), rdbConfig.getRdbPassword());
  }

  private static RdbConfig createH2Config() {
    return RdbConfig.builder()
        .DBConnectionURL("jdbc:h2:mem:migratorTestDb")
        .RdbDriver("org.h2.Driver")
        .RdbDialect("org.hibernate.dialect.H2Dialect")
        .RdbDatabaseName("migrationTestDb")
        .RdbPassword("password")
        .RdbUsername("sa")
        .build();
  }

  private static RdbConfig createSqlServerConfig() {
    return RdbConfig.builder()
        .RdbUrl("jdbc:sqlserver://localhost:1433")
        .RdbDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver")
        .RdbDialect("org.hibernate.dialect.SQLServer2008Dialect")
        .RdbDatabaseName("migrationTestDb")
        .RdbUsername("SA")
        .RdbPassword("MyPass@word")
        .sslEnabled(false)
        .build();
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
}
