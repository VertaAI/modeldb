package ai.verta.modeldb.common;

import ai.verta.modeldb.common.config.DatabaseConfig;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.db.migration.MigrationException;
import ai.verta.modeldb.common.db.migration.Migrator;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.LockException;
import liquibase.lockservice.LockServiceFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.ResourceAccessor;
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
    LOGGER.info("Checking DB connection with timeout [" + timeout + "]...");
    try (var con = getDBConnection(rdb)) {
      LOGGER.debug("Got connection: " + con);
      boolean valid = con.isValid(timeout);
      LOGGER.info("DB Connection valid? " + valid);
      return valid;
    } catch (Exception ex) {
      LOGGER.error("JdbiUtil checkDBConnection() got error ", ex);
      return false;
    }
  }

  protected void releaseLiquibaseLock(DatabaseConfig config)
      throws InterruptedException, SQLException, DatabaseException, LockException {
    // Get database connection
    try (var con = getDBConnection(config.getRdbConfiguration())) {
      var jdbcCon = new JdbcConnection(con);
      var existsStatus = tableExists(con, config, "database_change_log_lock");
      if (!existsStatus) {
        LOGGER.info("Table database_change_log_lock does not exists in DB");
        LOGGER.info("Proceeding with liquibase assuming it has never been run");
        return;
      }

      try (var stmt = jdbcCon.createStatement()) {

        var sql = "SELECT * FROM database_change_log_lock WHERE ID = 1";
        ResultSet rs = stmt.executeQuery(sql);

        var lastLockAcquireTimestamp = 0L;
        var locked = false;
        // Extract data from result set
        while (rs.next()) {
          // Retrieve by column name
          var id = rs.getInt("id");
          locked = rs.getBoolean("locked");
          var lockGrantedTimeStamp = rs.getTimestamp("lockgranted", Calendar.getInstance());
          var lockedBy = rs.getString("lockedby");

          // Display values
          LOGGER.debug(
              "Id: {}, Locked: {}, LockGrantedTimeStamp: {}, LockedBy: {}",
              id,
              locked,
              lockGrantedTimeStamp,
              lockedBy);

          if (lockGrantedTimeStamp != null) {
            lastLockAcquireTimestamp = lockGrantedTimeStamp.getTime();
          }
          LOGGER.debug("database locked by Liquibase: {}", locked);
        }
        rs.close();

        var currentCalender = Calendar.getInstance();
        long currentLockedTimeDiffSecond =
            (currentCalender.getTimeInMillis() - lastLockAcquireTimestamp) / 1000;
        LOGGER.debug(
            "current liquibase locked time difference in second: {}", currentLockedTimeDiffSecond);
        if (lastLockAcquireTimestamp != 0
            && currentLockedTimeDiffSecond > config.getLiquibaseLockThreshold()) {
          // Initialize Liquibase and run the update
          var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcCon);
          LockServiceFactory.getInstance().getLockService(database).forceReleaseLock();
          locked = false;
          LOGGER.debug("Release database lock executing query from backend");
        }

        if (locked) {
          Thread.sleep(
              config.getLiquibaseLockThreshold().longValue()
                  * 1000L); // liquibaseLockThreshold = second
          releaseLiquibaseLock(config);
        }
      }
    } catch (InterruptedException e) {
      LOGGER.error(e.getMessage(), e);
      throw e;
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

  protected void createTablesLiquibaseMigration(
      DatabaseConfig config,
      String changeSetToRevertUntilTag,
      String liquibaseRootPath,
      ResourceAccessor resourceAccessor,
      Optional<String> changeSetRemappingFile)
      throws LiquibaseException, SQLException, InterruptedException {
    var rdb = config.getRdbConfiguration();

    // Get database connection
    try (var con = getDBConnection(rdb)) {
      var jdbcCon = new JdbcConnection(con);
      if (config.getRdbConfiguration().isMysql()) {
        changeCharsetToUtf(jdbcCon);
      }

      var changeLogTableName =
          System.getProperties().getProperty("liquibase.databaseChangeLogTableName");

      if (tableExists(con, config, changeLogTableName)) {
        changeSetRemappingFile.ifPresent(
            filename -> resetChangeSetLogData(jdbcCon, changeLogTableName, filename));

        String trimOperation;
        if (config.getRdbConfiguration().isMssql()) {
          LOGGER.info("MSSQL detected. Using custom update to liquibase filename records.");
          // looks like sql server doesn't support the "length" function, so hardcode it here.
          trimOperation = "REPLACE(FILENAME, 'src/main/resources/', '')";
        } else {
          // note: we add 1 to the length because substring is 1-based instead of 0-based
          trimOperation = "substring(FILENAME, length('src/main/resources/')+1)";
        }
        var updateQuery = "update %s set FILENAME=" + trimOperation + " WHERE FILENAME LIKE ?";
        try (var statement =
            jdbcCon.prepareStatement(String.format(updateQuery, changeLogTableName))) {
          statement.setString(1, "%src/main/resources/liquibase%");
          statement.executeUpdate();
        } catch (Exception e) {
          LOGGER.warn("Updating the changelog table name failed.", e);
          // make this fail so it doesn't look like we've gotten properly migrated when we haven't.
          throw e;
        }
      }

      // Overwrite default liquibase table names by custom
      GlobalConfiguration liquibaseConfiguration =
          LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class);
      liquibaseConfiguration.setDatabaseChangeLogLockWaitTime(1L);

      // Initialize Liquibase and run the update
      var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcCon);

      // LiquiBase no longer supports absolute paths. Try to save people from themselves.
      if (liquibaseRootPath.startsWith("/") || liquibaseRootPath.startsWith("\\")) {
        liquibaseRootPath = liquibaseRootPath.substring(1);
      }

      try (var liquibase =
          new Liquibase(
              liquibaseRootPath,
              new CompositeResourceAccessor(resourceAccessor, new ClassLoaderResourceAccessor()),
              database)) {
        var liquibaseExecuted = false;
        while (!liquibaseExecuted) {
          try {
            if (changeSetToRevertUntilTag == null || changeSetToRevertUntilTag.isEmpty()) {
              liquibase.update(new Contexts(), new LabelExpression());
            } else {
              liquibase.rollback(changeSetToRevertUntilTag, new Contexts(), new LabelExpression());
            }
            liquibaseExecuted = true;
          } catch (LockException ex) {
            LOGGER.warn("CommonDBUtil createTablesLiquibaseMigration() getting LockException ", ex);
            releaseLiquibaseLock(config);
          }
        }
      }
    }
  }

  private static void resetChangeSetLogData(
      JdbcConnection jdbcCon, String changeLogTableName, String filename) {
    try {
      LOGGER.info("Resetting changeset data using file {}", filename);
      URL url = Resources.getResource(filename);
      List<String> lines = Resources.readLines(url, StandardCharsets.UTF_8);
      String json = String.join("", lines);
      Gson gson = new Gson();
      JsonReader reader = new JsonReader(new StringReader(json));
      ChangeSetId[] changeSetIdArray = gson.fromJson(reader, ChangeSetId[].class);
      var updateQuery = "update %s set FILENAME=? WHERE ID=?";
      int totalUpdated = 0;
      try (var statement =
          jdbcCon.prepareStatement(String.format(updateQuery, changeLogTableName))) {
        for (var changeSetId : changeSetIdArray) {
          statement.setString(1, changeSetId.getFileName());
          statement.setString(2, changeSetId.getId());
          int numberUpdated = statement.executeUpdate();
          if (numberUpdated > 0) {
            LOGGER.info(
                "Updated changelog for: filename '{}', ID '{}'",
                changeSetId.fileName,
                changeSetId.id);
          }
          totalUpdated += numberUpdated;
        }
        LOGGER.info("Reset database_change_log file path entries: {}", totalUpdated);
      }
    } catch (Exception ex) {
      throw new ModelDBException(ex);
    }
  }

  public static void changeCharsetToUtf(JdbcConnection jdbcCon)
      throws DatabaseException, SQLException {
    try (var stmt = jdbcCon.createStatement()) {
      String dbName = jdbcCon.getCatalog();
      var sql =
          String.format(
              "ALTER DATABASE `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;", dbName);
      int result = stmt.executeUpdate(sql);
      LOGGER.info("ALTER charset execute result: {}", result);
    }
  }

  protected boolean checkMigrationLockedStatus(String migrationName, RdbConfig rdbConfig)
      throws SQLException, DatabaseException {
    // Get database connection
    try (var con = getDBConnection(rdbConfig)) {
      var jdbcCon = new JdbcConnection(con);
      try (var stmt = jdbcCon.createStatement()) {
        String sql =
            String.format(
                "SELECT * FROM migration_status ms WHERE ms.migration_name = '%s'", migrationName);
        ResultSet rs = stmt.executeQuery(sql);

        var locked = false;
        // Extract data from result set
        while (rs.next()) {
          // Retrieve by column name
          var id = rs.getInt("id");
          locked = rs.getBoolean("status");
          var migrationNameDB = rs.getString("migration_name");

          // Display values
          LOGGER.debug("Id: {}, Locked: {}, migration_name: {}", id, locked, migrationNameDB);
          LOGGER.debug("migration {} locked: {}", migrationNameDB, locked);
        }
        rs.close();
        return locked;
      }
    } catch (DatabaseException e) {
      LOGGER.error(e.getMessage(), e);
      throw e;
    }
  }

  protected void lockedMigration(String migrationName, RdbConfig rdbConfig)
      throws SQLException, DatabaseException {
    // Get database connection
    try (var con = getDBConnection(rdbConfig)) {
      var jdbcCon = new JdbcConnection(con);
      try (var stmt = jdbcCon.createStatement()) {
        String sql =
            String.format(
                "INSERT INTO migration_status (migration_name, status) VALUES ('%s', 1);",
                migrationName);
        int updatedRowCount = stmt.executeUpdate(sql);
        LOGGER.debug("migration {} locked: {}", migrationName, updatedRowCount > 0);
      }
    } catch (DatabaseException e) {
      LOGGER.error(e.getMessage(), e);
      throw e;
    }
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

  protected void runLiquibaseMigration(
      DatabaseConfig config, String liquibaseRootPath, ResourceAccessor resourceAccessor)
      throws InterruptedException, LiquibaseException, SQLException {
    runLiquibaseMigration(config, liquibaseRootPath, resourceAccessor, Optional.empty());
  }

  protected void runLiquibaseMigration(
      DatabaseConfig config,
      String liquibaseRootPath,
      ResourceAccessor resourceAccessor,
      Optional<String> changeSetRemappingFile)
      throws InterruptedException, LiquibaseException, SQLException {
    // Change liquibase default table names
    String changeLogTableName = "database_change_log";
    String changeLogLockTableName = "database_change_log_lock";

    if (config.getRdbConfiguration().isH2()) {
      // H2 upper cases all table names, and liquibase has issues if you don't make this also upper
      // case.
      changeLogTableName = changeLogTableName.toUpperCase();
      changeLogLockTableName = changeLogLockTableName.toUpperCase();
    }

    System.getProperties().put("liquibase.databaseChangeLogTableName", changeLogTableName);
    System.getProperties().put("liquibase.databaseChangeLogLockTableName", changeLogLockTableName);

    // Lock to RDB for now
    var rdb = config.getRdbConfiguration();

    createDBIfNotExists(rdb);

    // Check DB is up or not
    boolean dbConnectionStatus =
        checkDBConnection(config.getRdbConfiguration(), config.getTimeout());
    if (!dbConnectionStatus) {
      checkDBConnectionInLoop(config, true);
    }

    releaseLiquibaseLock(config);

    // Run tables liquibase migration
    createTablesLiquibaseMigration(
        config,
        config.getChangeSetToRevertUntilTag(),
        liquibaseRootPath,
        resourceAccessor,
        changeSetRemappingFile);
  }

  public static void createDBIfNotExists(RdbConfig rdbConfiguration) throws SQLException {
    final var databaseName = RdbConfig.buildDatabaseName(rdbConfiguration);
    final var dbUrl = RdbConfig.buildDatabaseServerConnectionString(rdbConfiguration);
    LOGGER.info("Connecting to DB URL " + dbUrl);
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
          LOGGER.debug("the database " + databaseName + " exists");
          return;
        }
      }

      LOGGER.debug("the database " + databaseName + " does not exist");
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate("CREATE DATABASE " + databaseName);
        LOGGER.debug("the database " + databaseName + " was created successfully");
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

  protected static class ChangeSetId {
    private String id;
    private String fileName;

    public String getId() {
      return id;
    }

    public String getFileName() {
      return fileName;
    }
  }
}
