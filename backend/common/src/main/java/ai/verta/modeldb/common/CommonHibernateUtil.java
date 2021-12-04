package ai.verta.modeldb.common;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.DatabaseConfig;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import io.grpc.health.v1.HealthCheckResponse;
import java.sql.*;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.Properties;
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
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.mariadb.jdbc.MariaDbDataSource;
import org.postgresql.ds.PGSimpleDataSource;

public abstract class CommonHibernateUtil {
  private static final Logger LOGGER = LogManager.getLogger(CommonHibernateUtil.class);
  private StandardServiceRegistry registry;
  private SessionFactory sessionFactory;
  private boolean isReady = false;
  protected Config config;
  protected DatabaseConfig databaseConfig;
  protected static Class<?>[] entities;
  protected static String liquibaseRootFilePath;
  // private HibernateStatisticsCollector hibernateStatisticsCollector;

  public Connection getConnection() throws SQLException {
    return sessionFactory
        .getSessionFactoryOptions()
        .getServiceRegistry()
        .getService(ConnectionProvider.class)
        .getConnection();
  }

  public SessionFactory createOrGetSessionFactory(DatabaseConfig config) throws ModelDBException {
    if (sessionFactory != null) {
      return validateConnectionAndFetchExistingSessionFactory(sessionFactory);
    }

    LOGGER.info("Fetching sessionFactory");
    try {

      final var rdb = config.getRdbConfiguration();
      final var connectionString = RdbConfig.buildDatabaseConnectionString(rdb);
      final var idleTimeoutMillis = Integer.parseInt(config.getConnectionTimeout()) * 1000;
      final var connectionTimeoutMillis = 30000;
      final var connectionMaxLifetimeMillis = idleTimeoutMillis - 5000;
      final var url = RdbConfig.buildDatabaseConnectionString(rdb);
      final var connectionProviderClass = HikariCPConnectionProvider.class.getName();
      final var datasourceClass = getDatasourceClass(rdb);

      // Hibernate settings equivalent to hibernate.cfg.xml's properties
      final var configuration =
          new Configuration()
              .setProperty("hibernate.hbm2ddl.auto", "validate")
              .setProperty("hibernate.dialect", rdb.getRdbDialect())
              .setProperty("hibernate.connection.provider_class", connectionProviderClass)
              .setProperty("hibernate.hikari.dataSourceClassName", datasourceClass)
              .setProperty("hibernate.hikari.dataSource.url", url)
              .setProperty("hibernate.hikari.dataSource.user", rdb.getRdbUsername())
              .setProperty("hibernate.hikari.dataSource.password", rdb.getRdbPassword())
              .setProperty("hibernate.hikari.idleTimeout", String.valueOf(idleTimeoutMillis))
              .setProperty(
                  "hibernate.hikari.connectionTimeout", String.valueOf(connectionTimeoutMillis))
              .setProperty("hibernate.hikari.minimumIdle", config.getMinConnectionPoolSize())
              .setProperty("hibernate.hikari.maximumPoolSize", config.getMaxConnectionPoolSize())
              .setProperty(
                  "hibernate.hikari.maxLifetime", String.valueOf(connectionMaxLifetimeMillis))
              .setProperty("hibernate.hikari.poolName", "hibernate")
              .setProperty("hibernate.hikari.registerMbeans", "true")
              .setProperty("hibernate.generate_statistics", "true")
              .setProperty("hibernate.jmx.enabled", "true")
              .setProperty("hibernate.hbm2ddl.auto", "none")
              .setProperty(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, String.valueOf(200))
              .setProperty(
                  AvailableSettings.QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE,
                  String.valueOf(20))
              .setProperty(
                  "hibernate.hikari.leakDetectionThreshold",
                  String.valueOf(config.getLiquibaseLockThreshold()));

      LOGGER.trace("connectionString {}", connectionString);
      // Create registry builder
      StandardServiceRegistryBuilder registryBuilder =
          new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
      registry = registryBuilder.build();
      var metaDataSrc = new MetadataSources(registry);
      for (Class<?> entity : entities) {
        metaDataSrc.addAnnotatedClass(entity);
      }

      // Check DB is up or not
      boolean dbConnectionStatus = checkDBConnection(rdb, config.getTimeout());
      if (!dbConnectionStatus) {
        checkDBConnectionInLoop(true);
      }

      // Create session factory and validate entity
      sessionFactory = metaDataSrc.buildMetadata().buildSessionFactory();
      // Enable JMX metrics collection from hibernate
      // FIXME: Identify right way for how to re-initialize hibernateStatisticsCollector
      /*if (hibernateStatisticsCollector != null) {
        hibernateStatisticsCollector.add(sessionFactory, "hibernate");
      } else {
        hibernateStatisticsCollector = new HibernateStatisticsCollector(sessionFactory, "hibernate").register();
      }*/

      // Export schema
      if (CommonConstants.EXPORT_SCHEMA) {
        exportSchema(metaDataSrc.buildMetadata());
      }

      LOGGER.info(CommonMessages.READY_STATUS, isReady);
      isReady = true;
      return sessionFactory;
    } catch (Exception e) {
      LOGGER.warn("CommonHibernateUtil getSessionFactory() getting error : {}", e.getMessage(), e);
      if (registry != null) {
        StandardServiceRegistryBuilder.destroy(registry);
        // If registry will destroy then session factory also useless and have stale reference of
        // registry so need to clean it as well.
        sessionFactory = null;
      }
      if (e instanceof InterruptedException) {
        // Restore interrupted state...
        Thread.currentThread().interrupt();
      }
      throw new ModelDBException(e.getMessage(), e);
    }
  }

  private String getDatasourceClass(RdbConfig rdbConfiguration) {
    if (rdbConfiguration.isMssql()) {
      return SQLServerDataSource.class.getName();
    }
    if (rdbConfiguration.isMysql()) {
      return MariaDbDataSource.class.getName();
    }
    if (rdbConfiguration.isPostgres()) {
      return PGSimpleDataSource.class.getName();
    }
    throw new ModelDBException("Unrecognized database " + rdbConfiguration.getRdbDialect());
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

  public SessionFactory getSessionFactory() {
    return createOrGetSessionFactory(config.getDatabase());
  }

  private SessionFactory validateConnectionAndFetchExistingSessionFactory(
      SessionFactory sessionFactory) {
    try {
      LOGGER.trace("CommonHibernateUtil checking DB connection");
      boolean dbConnectionLive =
          checkDBConnection(databaseConfig.getRdbConfiguration(), databaseConfig.getTimeout());
      if (!dbConnectionLive) {
        LOGGER.warn(
            "CommonHibernateUtil validateConnectionAndFetchExistingSessionFactory() DB connection isValid: {}",
            false);
        // If DB is not live then backend is not ready yet
        isReady = false;
        // Check DB connection based on the periodic time logic
        checkDBConnectionInLoop(false);
      }
      // If DB is live then backend is not ready yet
      isReady = true;
      LOGGER.trace(
          "CommonHibernateUtil validateConnectionAndFetchExistingSessionFactory() DB connection got successfully");
      return sessionFactory;
    } catch (Exception ex) {
      LOGGER.warn(
          "CommonHibernateUtil validateConnectionAndFetchExistingSessionFactory() getting error ",
          ex);
      if (ex instanceof InterruptedException) {
        // Restore interrupted state...
        Thread.currentThread().interrupt();
      }
      throw new UnavailableException(ex.getMessage());
    }
  }

  public void checkDBConnectionInLoop(boolean isStartUpTime) throws InterruptedException {
    var loopBackTime = 5;
    var loopIndex = 0;
    var dbConnectionLive = false;
    while (!dbConnectionLive) {
      if (loopIndex < 10 || isStartUpTime) {
        Thread.sleep(loopBackTime);
        LOGGER.debug(
            "CommonHibernateUtil checkDBConnectionInLoop() retrying for DB connection after {} millisecond ",
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

  private void exportSchema(Metadata buildMetadata) {
    String rootPath = System.getProperty(CommonConstants.USER_DIR);
    rootPath = rootPath + "\\src\\main\\resources\\liquibase\\hibernate-base-db-schema.sql";
    new SchemaExport()
        .setDelimiter(";")
        .setOutputFile(rootPath)
        .create(EnumSet.of(TargetType.SCRIPT), buildMetadata);
  }

  public void shutdown() {
    if (registry != null) {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }

  public void releaseLiquibaseLock(DatabaseConfig config)
      throws LiquibaseException, SQLException, InterruptedException {
    // Get database connection
    try (var con = getDBConnection(config.getRdbConfiguration())) {
      var existsStatus = tableExists(con, config, "database_change_log_lock");
      if (!existsStatus) {
        LOGGER.info("Table database_change_log_lock does not exists in DB");
        LOGGER.info("Proceeding with liquibase assuming it has never been run");
        return;
      }

      var jdbcCon = new JdbcConnection(con);
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
              "CommonHibernateUtil createTablesLiquibaseMigration() getting LockException ", ex);
          releaseLiquibaseLock(config);
        }
      }
    }
  }

  public boolean checkDBConnection() {
    return checkDBConnection(databaseConfig.getRdbConfiguration(), databaseConfig.getTimeout());
  }

  public Connection getDBConnection(RdbConfig rdb) throws SQLException {
    final var connectionString = RdbConfig.buildDatabaseConnectionString(rdb);
    return DriverManager.getConnection(
        connectionString, rdb.getRdbUsername(), rdb.getRdbPassword());
  }

  public boolean checkDBConnection(RdbConfig rdb, Integer timeout) {
    try (var con = getDBConnection(rdb)) {
      return con.isValid(timeout);
    } catch (Exception ex) {
      LOGGER.warn("CommonHibernateUtil checkDBConnection() got error ", ex);
      return false;
    }
  }

  public boolean ping() {
    if (sessionFactory != null) {
      try (var session = sessionFactory.openSession()) {
        final var valid = new boolean[] {false};
        session.doWork(
            connection -> {
              if (connection.isValid(databaseConfig.getTimeout())) {
                valid[0] = true;
              }
            });

        return valid[0];
      } catch (JDBCConnectionException ex) {
        LOGGER.error(
            "CommonHibernateUtil ping() : DB connection not found, got error: {}", ex.getMessage());
      }
    }
    return false;
  }

  public HealthCheckResponse.ServingStatus checkReady() {
    try {
      if (isReady && ping()) {
        return HealthCheckResponse.ServingStatus.SERVING;
      }
      return HealthCheckResponse.ServingStatus.NOT_SERVING;
    } catch (Exception ex) {
      LOGGER.error("Getting error on health checkReady: {}", ex.getMessage(), ex);
      return HealthCheckResponse.ServingStatus.NOT_SERVING;
    }
  }

  public HealthCheckResponse.ServingStatus checkLive() {
    return HealthCheckResponse.ServingStatus.SERVING;
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

  protected boolean checkMigrationLockedStatus(String migrationName, RdbConfig rdb)
      throws SQLException, DatabaseException {
    // Get database connection
    try (var con = getDBConnection(rdb)) {

      var jdbcCon = new JdbcConnection(con);

      try (var stmt = jdbcCon.createStatement()) {

        var sql =
            new StringBuilder("SELECT * FROM migration_status ms WHERE ms.migration_name = '")
                .append(migrationName)
                .append("'");
        ResultSet rs = stmt.executeQuery(sql.toString());

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

  protected void lockedMigration(String migrationName, RdbConfig rdb)
      throws SQLException, DatabaseException {
    // Get database connection
    try (var con = getDBConnection(rdb)) {

      var jdbcCon = new JdbcConnection(con);

      try (var stmt = jdbcCon.createStatement()) {

        var sql =
            new StringBuilder("INSERT INTO migration_status (migration_name, status) VALUES ('")
                .append(migrationName)
                .append("', 1);");
        int updatedRowCount = stmt.executeUpdate(sql.toString());
        LOGGER.debug("migration {} locked: {}", migrationName, updatedRowCount > 0);
      }
    } catch (DatabaseException e) {
      LOGGER.error(e.getMessage(), e);
      throw e;
    }
  }

  public void runLiquibaseMigration(DatabaseConfig config)
      throws InterruptedException, LiquibaseException, SQLException {
    runLiquibaseMigration(config, liquibaseRootFilePath);
  }

  public void runLiquibaseMigration(DatabaseConfig config, String liquibaseRootPath)
      throws InterruptedException, LiquibaseException, SQLException {
    // Change liquibase default table names
    System.getProperties().put("liquibase.databaseChangeLogTableName", "database_change_log");
    System.getProperties()
        .put("liquibase.databaseChangeLogLockTableName", "database_change_log_lock");

    // Lock to RDB for now
    var rdb = config.getRdbConfiguration();

    createDBIfNotExists(rdb);

    // Check DB is up or not
    boolean dbConnectionStatus = checkDBConnection(rdb, config.getTimeout());
    if (!dbConnectionStatus) {
      checkDBConnectionInLoop(true);
    }

    releaseLiquibaseLock(config);

    // Run tables liquibase migration
    createTablesLiquibaseMigration(
        config, config.getChangeSetToRevertUntilTag(), liquibaseRootPath);
  }

  public void createDBIfNotExists(RdbConfig rdb) throws SQLException {
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
        var queryBuilder = new StringBuilder("CREATE DATABASE " + dbName);
        statement.executeUpdate(queryBuilder.toString());
        LOGGER.info("the database {} created successfully", rdb.getRdbDatabaseName());
      }
    }
  }
}
