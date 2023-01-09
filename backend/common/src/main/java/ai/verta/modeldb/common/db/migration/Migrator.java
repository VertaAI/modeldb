package ai.verta.modeldb.common.db.migration;

import ai.verta.modeldb.common.config.RdbConfig;
import com.google.common.io.Resources;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

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

  public void performMigration(Integer desiredVersion) throws SQLException, MigrationException {
    MigrationDatastore migrationDatastore = setupDatastore();
    MigrationTools.lockDatabase(migrationDatastore);
    try {
      MigrationState currentState = findCurrentState();
      if (currentState == null) {
        throw new IllegalStateException(
            "The schema_migrations table contains no records. Migration process cannot start.");
      }
      if (currentState.isDirty()) {
        currentState = cleanUpDirtyDatabase(currentState);
      }
      int versionToTarget = findVersionToTarget(desiredVersion);
      log.info("Starting database migration process to version: " + versionToTarget);

      if (currentState.getVersion() == versionToTarget) {
        log.info("No migrations to perform. Database is already at version " + versionToTarget);
        return;
      }
      SortedSet<Migration> migrationsToPerform =
          gatherMigrationsToPerform(currentState, versionToTarget);
      runMigrations(migrationsToPerform);
    } catch (SQLException | MigrationException e) {
      log.error("Migration process failed", e);
      throw e;
    } finally {
      migrationDatastore.unlock();
    }
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
    updateVersion(false, revertedVersion, currentState.getVersion(), true);
    return new MigrationState(revertedVersion, false);
  }

  private int findVersionToTarget(Integer desiredVersion) throws MigrationException {
    return desiredVersion != null
        ? desiredVersion
        : gatherMigrations(migration -> true, migration -> true).stream()
            .map(Migration::getNumber)
            .max(Integer::compareTo)
            .orElse(0);
  }

  private SortedSet<Migration> gatherMigrationsToPerform(
      MigrationState currentState, int versionToTarget) throws MigrationException {
    // if we're currently below the targeted version, we want up migrations in the right range,
    // otherwise the down ones.
    if (currentState.getVersion() < versionToTarget) {
      return gatherMigrations(
          Migration::isUp,
          migration -> migrationLessThanTargetedVersion(currentState, versionToTarget, migration));
    }
    return gatherMigrations(
            Migration::isDown,
            migration ->
                migrationGreaterThanTargetedVersion(currentState, versionToTarget, migration))
        .descendingSet();
  }

  /**
   * Returns whether the current migration is greater than targeted version, and less than or equal
   * to the current state's version.
   */
  private static boolean migrationGreaterThanTargetedVersion(
      MigrationState currentState, int versionToTarget, Migration migration) {
    return migration.getNumber() > versionToTarget
        && migration.getNumber() <= currentState.getVersion();
  }

  /**
   * Returns whether the current migration is less than or equal to the targeted version, and
   * greater than to the current state's version.
   */
  private static boolean migrationLessThanTargetedVersion(
      MigrationState currentState, int versionToTarget, Migration migration) {
    return (migration.getNumber() > currentState.getVersion())
        && (migration.getNumber() <= versionToTarget);
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

  private void runMigrations(SortedSet<Migration> migrationsToPerform) throws MigrationException {
    for (Migration migration : migrationsToPerform) {
      try {
        updateVersion(
            migration, true, migration.isUp() ? migration.getNumber() - 1 : migration.getNumber());
        executeSingleMigration(migration);
        updateVersion(
            migration, false, migration.isUp() ? migration.getNumber() : migration.getNumber() - 1);
      } catch (IOException | SQLException e) {
        throw new MigrationException("failed migration " + migration + "", e);
      }
    }
  }

  private void updateVersion(Migration pendingMigration, boolean dirty, int expectedCurrentVersion)
      throws SQLException {
    int newVersion =
        pendingMigration.isUp() ? pendingMigration.getNumber() : pendingMigration.getNumber() - 1;

    updateVersion(dirty, newVersion, expectedCurrentVersion, !dirty);
  }

  private void updateVersion(
      boolean dirty, int newVersion, int expectedCurrentVersion, boolean expectedCurrentDirtyState)
      throws SQLException {
    try (PreparedStatement ps =
        connection.prepareStatement(
            "UPDATE schema_migrations set version = ?, dirty = ? WHERE version = ? and dirty = ?")) {
      ps.setInt(1, newVersion);
      ps.setBoolean(2, dirty);
      ps.setInt(3, expectedCurrentVersion);
      ps.setBoolean(4, expectedCurrentDirtyState);
      int rowsUpdated = ps.executeUpdate();
      if (rowsUpdated != 1) {
        throw new IllegalStateException(
            "Failed to update schema_migrations table to '"
                + newVersion
                + "' from '"
                + expectedCurrentVersion
                + "'. rowsUpdated: "
                + rowsUpdated);
      }
    }
  }

  private NavigableSet<Migration> gatherMigrations(
      Predicate<Migration> migrationDirectionFilter, Predicate<Migration> migrationVersionFilter)
      throws MigrationException {
    try {
      // todo: let's not use spring code for this, but figure out how to do it ourselves...
      Resource[] resources =
          new PathMatchingResourcePatternResolver().getResources(resourcesDirectory + "/*.sql");
      List<String> fileNames =
          Arrays.stream(resources).map(Resource::getFilename).collect(Collectors.toList());
      return fileNames.stream()
          .map(Migration::new)
          .filter(migrationDirectionFilter)
          .filter(migrationVersionFilter)
          .collect(Collectors.toCollection(TreeSet::new));
    } catch (Exception e) {
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

  public void executeSingleMigration(Migration migration) throws IOException, SQLException {
    log.info("executing migration: " + migration);
    String sql =
        Resources.toString(
            Resources.getResource(resourcesDirectory + "/" + migration.getFilename()),
            StandardCharsets.UTF_8);
    if (sql.trim().isEmpty()) {
      log.info("Skipping empty migration : " + migration.getFilename());
      return;
    }
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    }
  }

  /**
   * Pre-initializes the schema migrations table, in cases where the database is being converted
   * from a legacy migration tool (i.e. liquibase).
   *
   * <p>_If and only if_ ALL the following conditions are met, then the <code>schema_migrations
   * </code> table will be initialized with the value of <code>assumedCurrentVersion</code>:
   *
   * <ul>
   *   <li>there are legacy migrations present
   *   <li>there is no existing <code>schema_migrations</code> table
   *   <li>an <code>assumedCurrentVersion</code> value is provided
   * </ul>
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
      updateVersion(false, assumedCurrentVersion.get(), 0, false);
    }
  }

  @Value
  private static class MigrationState {
    int version;
    boolean dirty;
  }
}
