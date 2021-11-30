package ai.verta.modeldb.common;

import ai.verta.modeldb.common.config.DatabaseConfig;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import io.grpc.health.v1.HealthCheckResponse;
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
import liquibase.resource.FileSystemResourceAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.JDBCConnectionException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Properties;

public abstract class CommonJdbiUtil {
  private static final Logger LOGGER = LogManager.getLogger(CommonJdbiUtil.class);

  private static void checkDBConnectionInLoop(DatabaseConfig databaseConfig, boolean isStartUpTime)
      throws InterruptedException {
    var loopBackTime = 5;
    var loopIndex = 0;
    var dbConnectionLive = false;
    while (!dbConnectionLive) {
      if (loopIndex < 10 || isStartUpTime) {
        Thread.sleep(loopBackTime);
        LOGGER.debug(
            "CommonJdbiUtil checkDBConnectionInLoop() retrying for DB connection after {} millisecond ",
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

  private static Connection getDBConnection(RdbConfig rdb) throws SQLException {
    final var connectionString = RdbConfig.buildDatabaseConnectionString(rdb);
    return DriverManager.getConnection(
        connectionString, rdb.getRdbUsername(), rdb.getRdbPassword());
  }

  private static boolean checkDBConnection(RdbConfig rdb, Integer timeout) {
    try (var con = getDBConnection(rdb)) {
      return con.isValid(timeout);
    } catch (Exception ex) {
      LOGGER.warn("JdbiUtil checkDBConnection() got error ", ex);
      return false;
    }
  }

  public void releaseLiquibaseLock(DatabaseConfig config)
      throws InterruptedException, SQLException, DatabaseException, LockException {
    // Get database connection
    try (var con = getDBConnection(config.getRdbConfiguration())) {
      var jdbcCon = new JdbcConnection(con);
      var existsStatus = tableExists(con, config, "database_change_log_lock");
      if (!existsStatus) {
        LOGGER.info("Table database_change_log_lock does not exists in DB");
        LOGGER.info("Proceeding with liquibase assuming it has never been run");
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
                "current liquibase locked time difference in second: {}",
                currentLockedTimeDiffSecond);
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
      return conn.getMetaData().getTables(rdb.getRdbDatabaseName(), null, tableName, null);
    }
  }

  public void createTablesLiquibaseMigration(
      DatabaseConfig config, String changeSetToRevertUntilTag, String liquibaseRootPath)
      throws LiquibaseException, SQLException, InterruptedException {
    var rdb = config.getRdbConfiguration();

    // Get database connection
    try (var con = getDBConnection(rdb)) {
      var jdbcCon = new JdbcConnection(con);
      if (config.getRdbConfiguration().isMysql()) {
        changeCharsetToUtf(jdbcCon);
      }

      // Overwrite default liquibase table names by custom
      GlobalConfiguration liquibaseConfiguration =
          LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class);
      liquibaseConfiguration.setDatabaseChangeLogLockWaitTime(1L);

      // Initialize Liquibase and run the update
      var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcCon);
      String rootPath = System.getProperty(CommonConstants.USER_DIR);
      rootPath = rootPath + liquibaseRootPath;
      var liquibase = new Liquibase(rootPath, new FileSystemResourceAccessor(), database);

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
          LOGGER.warn(
              "CommonJdbiUtil createTablesLiquibaseMigration() getting LockException ", ex);
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

  protected boolean checkMigrationLockedStatus(FutureJdbi dbFutureJdbi, String migrationName)
      throws SQLException, DatabaseException {
    // Get database connection
    return dbFutureJdbi
        .withHandle(
            handle -> {
              try (var con = handle.getConnection()) {
                var jdbcCon = new JdbcConnection(con);
                try (var stmt = jdbcCon.createStatement()) {
                  String sql = String.format("SELECT * FROM migration_status ms WHERE ms.migration_name = '%s'", migrationName);
                  ResultSet rs = stmt.executeQuery(sql);

                  var locked = false;
                  // Extract data from result set
                  while (rs.next()) {
                    // Retrieve by column name
                    var id = rs.getInt("id");
                    locked = rs.getBoolean("status");
                    var migrationNameDB = rs.getString("migration_name");

                    // Display values
                    LOGGER.debug(
                        "Id: {}, Locked: {}, migration_name: {}", id, locked, migrationNameDB);
                    LOGGER.debug("migration {} locked: {}", migrationNameDB, locked);
                  }
                  rs.close();
                  return locked;
                }
              } catch (DatabaseException e) {
                LOGGER.error(e.getMessage(), e);
                throw e;
              }
            })
        .get();
  }

  protected void lockedMigration(String migrationName, FutureJdbi dbFutureJdbi)
      throws SQLException, DatabaseException {
    // Get database connection
    dbFutureJdbi
        .useHandle(
            handle -> {
              try (var con = handle.getConnection()) {
                var jdbcCon = new JdbcConnection(con);
                try (var stmt = jdbcCon.createStatement()) {
                  String sql =
                          String.format("INSERT INTO migration_status (migration_name, status) VALUES ('%s', 1);", migrationName);
                  int updatedRowCount = stmt.executeUpdate(sql);
                  LOGGER.debug("migration {} locked: {}", migrationName, updatedRowCount > 0);
                }
              } catch (DatabaseException e) {
                LOGGER.error(e.getMessage(), e);
                throw e;
              }
            })
        .get();
  }

  private void runLiquibaseMigration(DatabaseConfig config, String liquibaseRootPath)
      throws InterruptedException, LiquibaseException, SQLException {
    // Change liquibase default table names
    System.getProperties().put("liquibase.databaseChangeLogTableName", "database_change_log");
    System.getProperties()
        .put("liquibase.databaseChangeLogLockTableName", "database_change_log_lock");

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
        config, config.getChangeSetToRevertUntilTag(), liquibaseRootPath);
  }

  private static void createDBIfNotExists(RdbConfig rdb) throws SQLException {
    LOGGER.info("Checking DB: {}", rdb.getRdbUrl());
    var properties = new Properties();
    properties.put("user", rdb.getRdbUsername());
    properties.put("password", rdb.getRdbPassword());
    properties.put("sslMode", rdb.getSslMode());
    final var dbUrl = RdbConfig.buildDatabaseServerConnectionString(rdb);
    LOGGER.info("Connecting to DB server url: {} ", dbUrl);
    try (var connection = DriverManager.getConnection(dbUrl, properties)) {
      var resultSet = connection.getMetaData().getCatalogs();

      while (resultSet.next()) {
        var databaseNameRes = resultSet.getString(1);
        if (rdb.getRdbDatabaseName().equals(databaseNameRes)) {
          LOGGER.info("the database {} exists", rdb.getRdbDatabaseName());
          return;
        }
      }

      var dbName = RdbConfig.buildDatabaseName(rdb);

      LOGGER.info("the database {} does not exists", rdb.getRdbDatabaseName());
      try (var statement = connection.createStatement()) {
        statement.executeUpdate(String.format("CREATE DATABASE %s", dbName));
        LOGGER.info("the database {} created successfully", rdb.getRdbDatabaseName());
      }
    }
  }
}
