package ai.verta.modeldb.migration;

import static org.assertj.core.api.Assertions.assertThat;

import ai.verta.modeldb.common.CommonDBUtil;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.db.migration.Migration;
import ai.verta.modeldb.common.db.migration.Migrator;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class MigratorTest {

  @Test
  void release_2022_08_ddl_migration_mysql() throws Exception {
    var dbName = "modeldbTestDB";
    RdbConfig config =
        RdbConfig.builder()
            .RdbUrl("jdbc:mysql://localhost:3306")
            .RdbDriver("org.mariadb.jdbc.Driver")
            .RdbDialect("org.hibernate.dialect.MySQL5Dialect")
            .RdbDatabaseName(dbName)
            .RdbUsername("root")
            .RdbPassword("root")
            .sslEnabled(false)
            .build();
    CommonDBUtil.createDBIfNotExists(config);
    Connection connection = buildStandardDbConnection(config);

    try {
      Migrator migrator = new Migrator(connection, "migration/release_ddl/mysql");

      verifyRelease202208DdlExecution(connection, migrator);
    } finally {
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate(String.format("drop database %s;", dbName));
      }
    }
  }

  private static void verifyRelease202208DdlExecution(Connection connection, Migrator migrator)
      throws IOException, SQLException {
    migrator.executeMigration(new Migration("mysql_release_2022_08.up.sql"));
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

    migrator.executeMigration(new Migration("mysql_release_2022_08.down.sql"));

    try (ResultSet tables = connection.getMetaData().getTables(null, null, "artifact", null)) {
      assertThat(tables.next()).isFalse();
    }

    try (ResultSet tables =
        connection
            .getMetaData()
            .getTables(null, null, "versioning_modeldb_entity_mapping_config_blob", null)) {
      assertThat(tables.next()).isFalse();
    }
  }

  private static Connection buildStandardDbConnection(RdbConfig rdbConfig) throws SQLException {
    String connectionString = RdbConfig.buildDatabaseConnectionString(rdbConfig);
    return DriverManager.getConnection(
        connectionString, rdbConfig.getRdbUsername(), rdbConfig.getRdbPassword());
  }
}