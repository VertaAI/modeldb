package ai.verta.modeldb.common.db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import ai.verta.modeldb.common.CommonDBUtil;
import ai.verta.modeldb.common.config.RdbConfig;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    RdbConfig config =
        RdbConfig.builder()
            .RdbUrl("jdbc:sqlserver://localhost:1433")
            .RdbDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver")
            .RdbDialect("org.hibernate.dialect.SQLServer2008Dialect")
            .RdbDatabaseName("modeldbTestDB")
            .RdbUsername("SA")
            .RdbPassword("MyPass@word")
            .sslEnabled(false)
            .build();
    CommonDBUtil.createDBIfNotExists(config);
    Connection connection = buildSqlServerConnection(config);

    Migrator migrator = new Migrator(connection, "migrations/testing/sqlsvr");

    verifyDdlExecution(connection, migrator);
  }

  @Test
  @Disabled("only run manually to test things against mysql for now")
  void singleMigration_mysql() throws Exception {
    RdbConfig config =
        RdbConfig.builder()
            .RdbUrl("jdbc:mysql://localhost:3306")
            .RdbDriver("org.mariadb.jdbc.Driver")
            .RdbDialect("org.hibernate.dialect.MySQL5Dialect")
            .RdbDatabaseName("modeldbTestDB")
            .RdbUsername("root")
            .RdbPassword("replace me with your password")
            .sslEnabled(false)
            .build();
    CommonDBUtil.createDBIfNotExists(config);
    Connection connection = buildSqlServerConnection(config);

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

  private static Connection buildH2Connection() throws SQLException {
    String connectionString =
        RdbConfig.buildDatabaseConnectionString(
            RdbConfig.builder()
                .DBConnectionURL("jdbc:h2:mem:migratorTestDb")
                .RdbDriver("org.h2.Driver")
                .RdbDialect("org.hibernate.dialect.H2Dialect")
                .RdbDatabaseName("modeldbTestDB")
                .build());
    return DriverManager.getConnection(connectionString, "sa", "password");
  }

  private static Connection buildSqlServerConnection(RdbConfig rdbConfig) throws SQLException {
    String connectionString = RdbConfig.buildDatabaseConnectionString(rdbConfig);
    return DriverManager.getConnection(
        connectionString, rdbConfig.getRdbUsername(), rdbConfig.getRdbPassword());
  }
}
