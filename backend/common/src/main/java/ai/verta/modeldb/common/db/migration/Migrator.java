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
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Migrator {
  private final Connection connection;
  private final String resourcesDirectory;
  private final RdbConfig rdbConfig;

  public Migrator(
      Connection connection, String resourcesDirectory, RdbConfig databaseConfiguration) {
    this.connection = connection;
    this.resourcesDirectory = resourcesDirectory;
    this.rdbConfig = databaseConfiguration;
  }

  /** Assumes that the underlying database has already been created. */
  public void performMigration() throws SQLException, MigrationException {
    log.info("Starting database migration process");
    performMigration(null);
  }

  void performMigration(Integer desiredVersion) throws SQLException, MigrationException {
    MigrationDatastore migrationDatastore = setupDatastore();
    MigrationState currentState = findCurrentState();

    if (currentState.isDirty()) {
      currentState = cleanUpDirtyDatabase(currentState);
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

  private MigrationState cleanUpDirtyDatabase(MigrationState currentState)
      throws MigrationException, SQLException {
    if (currentState.getVersion() <= 1) {
      // todo: are we really comfortable implementing the database drop functionality at this point?
      throw new MigrationException(
          "Database is in a dirty state at version "
              + currentState.getVersion()
              + ". Bailing out until dropping the database is implemented.");
    }
    // We assume if a migration failed, that it wasn't applied, so it should be safe to simply
    // revert the number and unset the dirty flag.
    int revertedVersion = currentState.getVersion() - 1;
    updateVersion(false, revertedVersion);
    return new MigrationState(revertedVersion, false);
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

  private MigrationDatastore setupDatastore() throws SQLException {
    MigrationDatastore migrationDatastore = MigrationDatastore.create(rdbConfig, connection);
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
    int newVersion =
        pendingMigration.isUp() ? pendingMigration.getNumber() : pendingMigration.getNumber() - 1;
    updateVersion(dirty, newVersion);
  }

  private void updateVersion(boolean dirty, int newVersion) throws SQLException {
    try (PreparedStatement ps =
        connection.prepareStatement("UPDATE schema_migrations set version = ?, dirty = ?")) {
      ps.setInt(1, newVersion);
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

  /**
   * Method to pre-initialize the schema migrations table, in cases where the database is being
   * converted from a legacy migration tool (i.e. liquibase).
   *
   * <p>_If and only if_ there are legacy migrations in place, there is no existing
   * schema_migrations table, AND an assumed current version is provided will the schema_versions
   * table be initialized to the provided value.
   *
   * <p>If legacy migrations are in place, and you do not provide an assumed current version, this
   * method will throw an {@link IllegalStateException}.
   *
   * @param legacyMigrationsInPlace Whether legacy migrations have already been applied to this
   *     database.
   * @param assumedCurrentVersion The version that the legacy migrations should correspond to.
   */
  public void preInitializeIfRequired(
      boolean legacyMigrationsInPlace, Optional<Integer> assumedCurrentVersion)
      throws SQLException {
    if (!legacyMigrationsInPlace) {
      return;
    }
    if (assumedCurrentVersion.isEmpty()) {
      throw new IllegalStateException(
          "If legacy migrations are in place, you *must* provide a version to initialize the schema migrations to.");
    }
    try {
      MigrationState currentState = findCurrentState();
      log.info(
          "schema_versions table already exists. Skipping pre-initialization step. current state: "
              + currentState);
    } catch (SQLException e) {
      log.info(
          "No schema_versions table found. Initializing to version " + assumedCurrentVersion.get());
      setupDatastore();
      updateVersion(false, assumedCurrentVersion.get());
    }
  }

  @Value
  private static class MigrationState {
    int version;
    boolean dirty;
  }
}
