package ai.verta.modeldb.common.db.migration;

import ai.verta.modeldb.common.config.RdbConfig;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Migrator {
  private final Connection connection;
  private final String resourcesDirectory;

  public Migrator(Connection connection, String resourcesDirectory) {
    this.connection = connection;
    this.resourcesDirectory = resourcesDirectory;
  }

  /** Assumes that the underlying database has already been created. */
  public void performMigration(RdbConfig config) throws SQLException, MigrationException {
    log.info("Starting database migration process");
    MigrationDatastore migrationDatastore = MigrationDatastore.create(config, connection);

    migrationDatastore.ensureMigrationTableExists();

    MigrationTools.lockDatabase(migrationDatastore);
    try {
      // todo: actually do some migration work
    } finally {
      migrationDatastore.unlock();
    }
  }

  public void executeMigration(Migration migration) throws IOException, SQLException {
    String sql =
        Resources.toString(
            Resources.getResource(resourcesDirectory + "/" + migration.getFilename()),
            StandardCharsets.UTF_8);
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    }
  }
}
