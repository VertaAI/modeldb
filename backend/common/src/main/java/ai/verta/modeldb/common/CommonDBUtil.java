package ai.verta.modeldb.common;

import ai.verta.modeldb.common.config.DatabaseConfig;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import java.sql.*;
import java.util.Calendar;
import java.util.Locale;
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
    try (var con = getDBConnection(rdb)) {
      return con.isValid(timeout);
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
    if (rdb.isPostgres()) {
      // TODO: make postgres implementation multitenant as well.
      return conn.getMetaData().getTables(null, null, tableName, null);
    } else {
      return conn.getMetaData().getTables(RdbConfig.buildDatabaseName(rdb), null, tableName, null);
    }
  }

  protected void createTablesLiquibaseMigration(
      DatabaseConfig config,
      String changeSetToRevertUntilTag,
      String liquibaseRootPath,
      ResourceAccessor resourceAccessor)
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
        String trimOperation;
        if (config.getRdbConfiguration().isMssql()) {
          LOGGER.info("MSSQL detected. Using custom update to liquibase filename records.");
          // looks like sql server doesn't support the "length" function, so hardcode it here.
          trimOperation = "substring(FILENAME, 1, 19)";
        } else {
          trimOperation = "substring(FILENAME, length('/src/main/resources/'))";
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

      var liquibase =
          new Liquibase(
              liquibaseRootPath,
              new CompositeResourceAccessor(resourceAccessor, new ClassLoaderResourceAccessor()),
              database);

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

  protected void runLiquibaseMigration(
      DatabaseConfig config, String liquibaseRootPath, ResourceAccessor resourceAccessor)
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
        config, config.getChangeSetToRevertUntilTag(), liquibaseRootPath, resourceAccessor);
  }

  protected static void createDBIfNotExists(RdbConfig rdbConfiguration) throws SQLException {
    final var databaseName = RdbConfig.buildDatabaseName(rdbConfiguration);
    final var dbUrl = RdbConfig.buildDatabaseServerConnectionString(rdbConfiguration);
    LOGGER.debug("Connecting to DB URL " + dbUrl);
    Connection connection =
        DriverManager.getConnection(
            dbUrl, rdbConfiguration.getRdbUsername(), rdbConfiguration.getRdbPassword());

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
    Statement statement = connection.createStatement();
    statement.executeUpdate("CREATE DATABASE " + databaseName);
    LOGGER.debug("the database " + databaseName + " was created successfully");
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
