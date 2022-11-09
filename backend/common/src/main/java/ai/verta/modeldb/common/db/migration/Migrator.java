package ai.verta.modeldb.common.db.migration;

import ai.verta.modeldb.common.config.RdbConfig;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Value;
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
    performMigration(config, null);
  }

  void performMigration(RdbConfig config, Integer desiredVersion)
      throws SQLException, MigrationException {
    MigrationDatastore migrationDatastore = setupDatastore(config);
    MigrationState currentState = findCurrentState();
    if (currentState == null) {
      throw new IllegalStateException(
          "The schema_migrations table contains no records. Migration process cannot start.");
    }
    if (currentState.isDirty()) {
      // todo: implement dirty handling
      throw new MigrationException(
          "Database is in a dirty state. Bailing out until dirty handling is implemented.");
    }
    int versionToTarget =
        desiredVersion != null
            ? desiredVersion
            : gatherMigrations(migration -> true, migration -> true).stream()
                .map(Migration::getNumber)
                .max(Integer::compareTo)
                .orElse(0);
    log.info("Starting database migration process to version: " + versionToTarget);

    if (currentState.getVersion() == versionToTarget) {
      log.info("No migrations to perform. Database is already at version " + versionToTarget);
      return;
    }
    NavigableSet<Migration> migrationsToPerform =
        gatherMigrationsToPerform(currentState, versionToTarget);
    runMigrations(migrationDatastore, migrationsToPerform);
  }

  private NavigableSet<Migration> gatherMigrationsToPerform(
      MigrationState currentState, int versionToTarget) throws MigrationException {
    NavigableSet<Migration> migrationsToPerform;
    if (currentState.getVersion() < versionToTarget) {
      migrationsToPerform =
          gatherMigrations(
              Migration::isUp,
              migration ->
                  (migration.getNumber() > currentState.getVersion())
                      && (migration.getNumber() <= versionToTarget));
    } else {
      migrationsToPerform =
          gatherMigrations(
                  Migration::isDown,
                  migration ->
                      migration.getNumber() > versionToTarget
                          && migration.getNumber() <= currentState.getVersion())
              .descendingSet();
    }
    return migrationsToPerform;
  }

  private MigrationDatastore setupDatastore(RdbConfig config) throws SQLException {
    MigrationDatastore migrationDatastore = MigrationDatastore.create(config, connection);
    migrationDatastore.ensureMigrationTableExists();
    MigrationState currentState = findCurrentState();
    if (currentState == null) {
      try (PreparedStatement ps =
          connection.prepareStatement(
              "INSERT INTO schema_migrations (version, dirty) values (?, ?) ")) {
        ps.setInt(1, 0);
        ps.setBoolean(2, false);
        int rowsInserted = ps.executeUpdate();
        if (rowsInserted != 1) {
          throw new IllegalStateException(
              "Failed to insert initial row into the schema_migrations table. rowsInserted: "
                  + rowsInserted);
        }
      }
    }
    return migrationDatastore;
  }

  private void runMigrations(
      MigrationDatastore migrationDatastore, NavigableSet<Migration> migrationsToPerform)
      throws MigrationException, SQLException {
    MigrationTools.lockDatabase(migrationDatastore);
    try {
      for (Migration migration : migrationsToPerform) {
        try {
          updateVersion(migration, true);
          executeSingleMigration(migration);
          updateVersion(migration, false);
        } catch (IOException | SQLException e) {
          throw new MigrationException("failed migration " + migration + "", e);
        }
      }
    } finally {
      migrationDatastore.unlock();
    }
  }

  private void updateVersion(Migration pendingMigration, boolean dirty) throws SQLException {
    try (PreparedStatement ps =
        connection.prepareStatement("UPDATE schema_migrations set version = ?, dirty = ?")) {
      ps.setInt(
          1,
          pendingMigration.isUp()
              ? pendingMigration.getNumber()
              : pendingMigration.getNumber() - 1);
      ps.setBoolean(2, dirty);
      int rowsUpdated = ps.executeUpdate();
      if (rowsUpdated != 1) {
        throw new IllegalStateException(
            "Failed to update schema_migrations table. rowsUpdated: " + rowsUpdated);
      }
    }
  }

  private NavigableSet<Migration> gatherMigrations(
      Predicate<Migration> upOrDown, Predicate<Migration> versionPredicate)
      throws MigrationException {
    try {
      URI uri = Resources.getResource(resourcesDirectory).toURI();
      Path directory = Paths.get(uri);
      return Arrays.stream(directory.toFile().listFiles())
          .map(File::getName)
          .map(Migration::new)
          .filter(upOrDown)
          .filter(versionPredicate)
          .collect(Collectors.toCollection(TreeSet::new));
    } catch (URISyntaxException e) {
      throw new MigrationException("Failed to read migration files.", e);
    }
  }

  private MigrationState findCurrentState() throws SQLException {
    try (PreparedStatement ps =
        connection.prepareStatement(
            "select version, dirty from " + MigrationDatastore.SCHEMA_MIGRATIONS_TABLE)) {
      ResultSet resultSet = ps.executeQuery();
      if (!resultSet.next()) {
        return null;
      }
      return new MigrationState(resultSet.getInt("version"), resultSet.getBoolean("dirty"));
    }
  }

  void executeSingleMigration(Migration migration) throws IOException, SQLException {
    log.info("executing migration: " + migration);
    String sql =
        Resources.toString(
            Resources.getResource(resourcesDirectory + "/" + migration.getFilename()),
            StandardCharsets.UTF_8);
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    }
  }

  @Value
  private static class MigrationState {
    int version;
    boolean dirty;
  }
}
