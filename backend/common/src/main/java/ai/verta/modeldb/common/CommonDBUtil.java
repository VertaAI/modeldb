package ai.verta.modeldb.common;

import ai.verta.modeldb.common.config.DatabaseConfig;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.db.migration.MigrationException;
import ai.verta.modeldb.common.db.migration.Migrator;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.result.ResultSetException;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

public abstract class CommonDBUtil {
  private static final Logger LOGGER = LogManager.getLogger(CommonDBUtil.class);

  protected static void checkDBConnectionInLoop(
      DatabaseConfig databaseConfig, boolean isStartUpTime) throws InterruptedException {
    var loopBackTime = 5;
    var loopIndex = 0;
    var dbConnectionLive = false;
    while (!dbConnectionLive) {
      if (loopIndex < 10 || (isStartUpTime && loopBackTime < 2560)) {
        Thread.sleep(loopBackTime);
        LOGGER.warn(
            "CommonDBUtil checkDBConnectionInLoop() retrying for DB connection after {} millisecond ",
            loopBackTime);
        loopBackTime = loopBackTime * 2;
        loopIndex = loopIndex + 1;
        dbConnectionLive =
            checkDBConnection(databaseConfig.getRdbConfiguration(), databaseConfig.getTimeout());
        // While backend will start up and DB connection is still not accessible then backend will
        // retry continuously for DB connection
        // And if it is from the user call then it will retry continuously till 2560 millisecond and
        // then return UnavailableException.
        if (isStartUpTime && loopBackTime >= 2560) {
          loopBackTime = 2560;
        }
      } else {
        LOGGER.error("DB connection not found after 2560 millisecond");
        throw new UnavailableException("Backend is unable to access database");
      }
    }
  }

  protected static Connection getDBConnection(RdbConfig rdb) throws SQLException {
    final var connectionString = RdbConfig.buildDatabaseConnectionString(rdb);
    return DriverManager.getConnection(
        connectionString, rdb.getRdbUsername(), rdb.getRdbPassword());
  }

  protected static boolean checkDBConnection(RdbConfig rdb, Integer timeout) {
    LOGGER.debug("Checking DB connection with timeout [{}]...", timeout);
    try (var con = getDBConnection(rdb)) {
      LOGGER.debug("Got connection: {}", con);
      boolean valid = con.isValid(timeout);
      LOGGER.debug("DB Connection valid? : {}", valid);
      return valid;
    } catch (Exception ex) {
      LOGGER.error("JdbiUtil checkDBConnection() got error ", ex);
      return false;
    }
  }

  public static boolean tableExists(Connection conn, DatabaseConfig config, String tableName)
      throws SQLException {
    var tExists = false;
    try (ResultSet rs = getTableBasedOnDialect(conn, tableName, config.getRdbConfiguration())) {
      while (rs.next()) {
        var tName = rs.getString("TABLE_NAME");
        if (tName != null && tName.equals(tableName)) {
          tExists = true;
          break;
        }
      }
    }
    return tExists;
  }

  private static ResultSet getTableBasedOnDialect(Connection conn, String tableName, RdbConfig rdb)
      throws SQLException {
    return conn.getMetaData().getTables(RdbConfig.buildDatabaseName(rdb), null, tableName, null);
  }

  /**
   * This method will use the {@link Migrator} tool to run migrations against the database. It does
   * not assume that the database currently exists, and will create it if necessary.
   *
   * <p>The currentVersion parameter is used during the transition from liquibase to the new
   * migrator. The table belows explains when it will be used. That is, it will only be used if the
   * database already exists, the liquibase changelog table exists, and, the new migration table has
   * not been initialized. In that specific case, the new migration table will be initialized with
   * the provided value.
   *
   * <pre>
   *     ________________________________________________________________________________________________________
   *     | Database exists? | Liquibase changelog table exists? | migration table exists? | currentVersion used |
   *     |       N          |              N/A                  |         N/A             |         N           |
   *     |       Y          |               N                   |          Y              |         N           |
   *     |       Y          |               N                   |          N              |         N           |
   *     |       Y          |               Y                   |          Y              |         N           |
   *     |       Y          |               Y                   |          N              |         Y           |
   *     --------------------------------------------------------------------------------------------------------
   * </pre>
   *
   * @param config The database configuration to use.
   * @param migrationResourcesRootDirectory The subdirectory under resources to pull migrations
   *     from.
   * @param currentVersion The version that the database is assumed to be in, already, if converting
   *     from liquibase.
   */
  public void runMigrations(
      DatabaseConfig config,
      String migrationResourcesRootDirectory,
      Optional<Integer> currentVersion)
      throws SQLException, MigrationException {
    RdbConfig rdbConfiguration = config.getRdbConfiguration();
    createDBIfNotExists(rdbConfiguration);
    try (Connection connection = acquireDatabaseConnection(config, rdbConfiguration)) {
      Migrator migrator =
          new Migrator(
              connection,
              findResourcesDirectory(migrationResourcesRootDirectory, rdbConfiguration),
              rdbConfiguration);
      boolean liquibaseTableExists = tableExists(connection, config, "database_change_log");
      migrator.preInitializeIfRequired(liquibaseTableExists, currentVersion);
      migrator.performMigration();
      connection.commit();
    }
  }

  private String findResourcesDirectory(String rootDirectory, RdbConfig rdbConfiguration) {
    if (rdbConfiguration.isMysql()) {
      return rootDirectory + "/mysql";
    }
    if (rdbConfiguration.isMssql()) {
      return rootDirectory + "/sqlsvr";
    }
    if (rdbConfiguration.isH2()) {
      return rootDirectory + "/h2";
    }
    throw new IllegalArgumentException("Unsupported database type for migrations");
  }

  private static Connection acquireDatabaseConnection(
      DatabaseConfig config, RdbConfig rdbConfiguration) throws SQLException {
    // Check DB is up or not
    boolean dbConnectionStatus =
        checkDBConnection(config.getRdbConfiguration(), config.getTimeout());
    if (!dbConnectionStatus) {
      try {
        checkDBConnectionInLoop(config, true);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted while getting a database connection.");
      }
    }
    return getDBConnection(rdbConfiguration);
  }

  public static void createDBIfNotExists(RdbConfig rdbConfiguration) throws SQLException {
    final var databaseName = RdbConfig.buildDatabaseName(rdbConfiguration);
    final var dbUrl = RdbConfig.buildDatabaseServerConnectionString(rdbConfiguration);
    LOGGER.info("Connecting to DB URL {}", dbUrl);
    try (Connection connection =
        DriverManager.getConnection(
            dbUrl, rdbConfiguration.getRdbUsername(), rdbConfiguration.getRdbPassword())) {
      if (rdbConfiguration.isH2()) {
        LOGGER.debug("database driver is H2...skipping creation");
        return;
      }
      ResultSet resultSet = connection.getMetaData().getCatalogs();

      while (resultSet.next()) {
        String databaseNameRes = resultSet.getString(1);
        var dbName = RdbConfig.buildDatabaseName(rdbConfiguration);
        if (dbName.equals(databaseNameRes)) {
          LOGGER.debug("the database {} exists", databaseName);
          return;
        }
      }

      LOGGER.debug("the database {} does not exist", databaseName);
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate("CREATE DATABASE " + databaseName);
        LOGGER.debug("the database {} was created successfully", databaseName);
      }
    }
  }

  public static Throwable logError(Throwable e) {
    if (e instanceof ExecutionException || e instanceof CompletionException) {
      return logError(e.getCause());
    }
    return e;
  }

  public static boolean needToRetry(Throwable e) {
    Throwable cause = logError(e);
    if (cause instanceof UnableToCreateStatementException
        || cause instanceof UnableToExecuteStatementException) {
      return cause.getMessage().toLowerCase(Locale.ROOT).contains("deadlock");
    } else if (cause instanceof ResultSetException) {
      return cause.getMessage().toLowerCase(Locale.ROOT).contains("unable to advance");
    }
    return false;
  }
}
