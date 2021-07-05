package ai.verta.modeldb.common;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.DatabaseConfig;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import io.grpc.health.v1.HealthCheckResponse;
import io.prometheus.client.hibernate.HibernateStatisticsCollector;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.LockException;
import liquibase.lockservice.LockServiceFactory;
import liquibase.resource.FileSystemResourceAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import java.sql.*;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.Properties;
import org.postgresql.ds.PGSimpleDataSource;

public abstract class CommonHibernateUtil {
  private static final Logger LOGGER = LogManager.getLogger(CommonHibernateUtil.class);
  private StandardServiceRegistry registry;
  private SessionFactory sessionFactory;
  private Boolean isReady = false;
  protected static Config config;
  protected static DatabaseConfig databaseConfig;
  protected static Class<?>[] entities;
  protected static String liquibaseRootFilePath;

  public Connection getConnection() throws SQLException {
    return sessionFactory
        .getSessionFactoryOptions()
        .getServiceRegistry()
        .getService(ConnectionProvider.class)
        .getConnection();
  }

  public SessionFactory createOrGetSessionFactory(DatabaseConfig config) throws ModelDBException {
    if (sessionFactory == null) {
      LOGGER.info("Fetching sessionFactory");
      try {

        final var rdb = config.RdbConfiguration;
        final var connectionString = RdbConfig.buildDatabaseConnectionString(rdb);
        final var idleTimeoutMillis = Integer.parseInt(config.connectionTimeout) * 1000;
        final var connectionTimeoutMillis = 30000;
        final var connectionMaxLifetimeMillis = idleTimeoutMillis - 5000;
        final var url = RdbConfig.buildDatabaseConnectionString(rdb);
        final var connectionProviderClass = HikariCPConnectionProvider.class.getName();
        final var datasourceClass = getDatasourceClass(rdb);

        // Hibernate settings equivalent to hibernate.cfg.xml's properties
        final var configuration = new Configuration()
                .setProperty("hibernate.hbm2ddl.auto", "validate")
                .setProperty("hibernate.dialect", rdb.RdbDialect)
                .setProperty("hibernate.connection.provider_class", connectionProviderClass)
                .setProperty("hibernate.hikari.dataSourceClassName", datasourceClass)
                .setProperty("hibernate.hikari.dataSource.url", url)
                .setProperty("hibernate.hikari.dataSource.user", rdb.RdbUsername)
                .setProperty("hibernate.hikari.dataSource.password", rdb.RdbPassword)
                .setProperty("hibernate.hikari.idleTimeout", String.valueOf(idleTimeoutMillis))
                .setProperty(
                    "hibernate.hikari.connectionTimeout", String.valueOf(connectionTimeoutMillis))
                .setProperty("hibernate.hikari.minimumIdle", config.minConnectionPoolSize)
                .setProperty("hibernate.hikari.maximumPoolSize", config.maxConnectionPoolSize)
                .setProperty(
                    "hibernate.hikari.maxLifetime", String.valueOf(connectionMaxLifetimeMillis))
                .setProperty("hibernate.hikari.poolName", "hibernate")
                .setProperty("hibernate.hikari.registerMbeans", "true")
                .setProperty("hibernate.generate_statistics", "true")
                .setProperty("hibernate.jmx.enabled", "true")
                .setProperty("hibernate.hbm2ddl.auto", "none")
                .setProperty(Environment.QUERY_PLAN_CACHE_MAX_SIZE, String.valueOf(200))
                .setProperty(
                    Environment.QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE, String.valueOf(20));

        LOGGER.trace("connectionString {}", connectionString);
        // Create registry builder
        StandardServiceRegistryBuilder registryBuilder =
            new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
        registry = registryBuilder.build();
        MetadataSources metaDataSrc = new MetadataSources(registry);
        for (Class<?> entity : entities) {
          metaDataSrc.addAnnotatedClass(entity);
        }

        // Check DB is up or not
        boolean dbConnectionStatus = checkDBConnection(rdb, config.timeout);
        if (!dbConnectionStatus) {
          checkDBConnectionInLoop(true);
        }

        // Create session factory and validate entity
        sessionFactory = metaDataSrc.buildMetadata().buildSessionFactory();
        // Enable JMX metrics collection from hibernate
        new HibernateStatisticsCollector(sessionFactory, "hibernate").register();
        // Export schema
        if (CommonConstants.EXPORT_SCHEMA) {
          exportSchema(metaDataSrc.buildMetadata());
        }

        LOGGER.info(CommonMessages.READY_STATUS, isReady);
        isReady = true;
        return sessionFactory;
      } catch (Exception e) {
        LOGGER.warn(
            "CommonHibernateUtil getSessionFactory() getting error : {}", e.getMessage(), e);
        if (registry != null) {
          StandardServiceRegistryBuilder.destroy(registry);
        }
        throw new ModelDBException(e.getMessage());
      }
    } else {
      return loopBack(sessionFactory);
    }
  }

  private String getDatasourceClass(RdbConfig rdbConfiguration) {
    if (rdbConfiguration.isMssql()) {
      return SQLServerDataSource.class.getName();
    }
    if (rdbConfiguration.isMysql()) {
      return MysqlDataSource.class.getName();
    }
    if (rdbConfiguration.isPostgres()) {
      return PGSimpleDataSource.class.getName();
    }
    throw new ModelDBException("Unrecognized database " + rdbConfiguration.RdbDialect);
  }

  public static void changeCharsetToUtf(JdbcConnection jdbcCon)
      throws DatabaseException, SQLException {
    Statement stmt = jdbcCon.createStatement();
    String dbName = jdbcCon.getCatalog();
    String sql =
        String.format(
            "ALTER DATABASE `%s` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;", dbName);
    int result = stmt.executeUpdate(sql);
    LOGGER.info("ALTER charset execute result: {}", result);
  }

  public SessionFactory getSessionFactory() {
    return createOrGetSessionFactory(config.database);
  }

  private SessionFactory loopBack(SessionFactory sessionFactory) {
    try {
      LOGGER.trace("CommonHibernateUtil checking DB connection");
      boolean dbConnectionLive =
          checkDBConnection(databaseConfig.RdbConfiguration, databaseConfig.timeout);
      if (dbConnectionLive) {
        return sessionFactory;
      }
      // Check DB connection based on the periodic time logic
      checkDBConnectionInLoop(false);
      sessionFactory = resetSessionFactory();
      LOGGER.trace("CommonHibernateUtil getSessionFactory() DB connection got successfully");
      return sessionFactory;
    } catch (Exception ex) {
      LOGGER.warn("CommonHibernateUtil loopBack() getting error ", ex);
      throw new UnavailableException(ex.getMessage());
    }
  }

  public SessionFactory resetSessionFactory() {
    isReady = false;
    sessionFactory = null;
    return getSessionFactory();
  }

  public void checkDBConnectionInLoop(boolean isStartUpTime) throws InterruptedException {
    int loopBackTime = 5;
    int loopIndex = 0;
    boolean dbConnectionLive = false;
    while (!dbConnectionLive) {
      if (loopIndex < 10 || isStartUpTime) {
        Thread.sleep(loopBackTime);
        LOGGER.debug(
            "CommonHibernateUtil getSessionFactory() retrying for DB connection after {} millisecond ",
            loopBackTime);
        loopBackTime = loopBackTime * 2;
        loopIndex = loopIndex + 1;
        dbConnectionLive =
            checkDBConnection(databaseConfig.RdbConfiguration, databaseConfig.timeout);
        if (isStartUpTime && loopBackTime >= 2560) {
          loopBackTime = 2560;
        }
      } else {
        throw new UnavailableException("DB connection not found after 2560 millisecond");
      }
    }
  }

  private void exportSchema(Metadata buildMetadata) {
    String rootPath = System.getProperty(CommonConstants.userDir);
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
      throws LiquibaseException, SQLException, InterruptedException, ClassNotFoundException {
    // Get database connection
    try (Connection con = getDBConnection(config.RdbConfiguration)) {
      boolean existsStatus = tableExists(con, config, "database_change_log_lock");
      if (!existsStatus) {
        LOGGER.info("Table database_change_log_lock does not exists in DB");
        LOGGER.info("Proceeding with liquibase assuming it has never been run");
        return;
      }

      JdbcConnection jdbcCon = new JdbcConnection(con);

      Statement stmt = jdbcCon.createStatement();

      String sql = "SELECT * FROM database_change_log_lock WHERE ID = 1";
      ResultSet rs = stmt.executeQuery(sql);

      long lastLockAcquireTimestamp = 0L;
      boolean locked = false;
      // Extract data from result set
      while (rs.next()) {
        // Retrieve by column name
        int id = rs.getInt("id");
        locked = rs.getBoolean("locked");
        Timestamp lockGrantedTimeStamp = rs.getTimestamp("lockgranted", Calendar.getInstance());
        String lockedBy = rs.getString("lockedby");

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
      stmt.close();

      Calendar currentCalender = Calendar.getInstance();
      long currentLockedTimeDiffSecond =
          (currentCalender.getTimeInMillis() - lastLockAcquireTimestamp) / 1000;
      LOGGER.debug(
          "current liquibase locked time difference in second: {}", currentLockedTimeDiffSecond);
      if (lastLockAcquireTimestamp != 0
          && currentLockedTimeDiffSecond > config.liquibaseLockThreshold) {
        // Initialize Liquibase and run the update
        Database database =
            DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcCon);
        LockServiceFactory.getInstance().getLockService(database).forceReleaseLock();
        locked = false;
        LOGGER.debug("Release database lock executing query from backend");
      }

      if (locked) {
        Thread.sleep(config.liquibaseLockThreshold * 1000); // liquibaseLockThreshold = second
        releaseLiquibaseLock(config);
      }
    } catch (InterruptedException e) {
      LOGGER.error(e.getMessage(), e);
      throw e;
    }
  }

  public void createTablesLiquibaseMigration(
      DatabaseConfig config, String changeSetToRevertUntilTag, String liquibaseRootPath)
      throws LiquibaseException, SQLException, InterruptedException, ClassNotFoundException {
    RdbConfig rdb = config.RdbConfiguration;

    // Get database connection
    try (Connection con = getDBConnection(rdb)) {
      JdbcConnection jdbcCon = new JdbcConnection(con);
      if (config.RdbConfiguration.isMysql()) {
        changeCharsetToUtf(jdbcCon);
      }

      // Overwrite default liquibase table names by custom
      GlobalConfiguration liquibaseConfiguration =
          LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class);
      liquibaseConfiguration.setDatabaseChangeLogLockWaitTime(1L);

      // Initialize Liquibase and run the update
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcCon);
      String rootPath = System.getProperty(CommonConstants.userDir);
      rootPath = rootPath + liquibaseRootPath;
      Liquibase liquibase = new Liquibase(rootPath, new FileSystemResourceAccessor(), database);

      boolean liquibaseExecuted = false;
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
    return checkDBConnection(databaseConfig.RdbConfiguration, databaseConfig.timeout);
  }

  public Connection getDBConnection(RdbConfig rdb) throws SQLException {
    final var connectionString = RdbConfig.buildDatabaseConnectionString(rdb);
    return DriverManager.getConnection(connectionString, rdb.RdbUsername, rdb.RdbPassword);
  }

  public boolean checkDBConnection(RdbConfig rdb, Integer timeout) {
    try (Connection con = getDBConnection(rdb)) {
      return con.isValid(timeout);
    } catch (Exception ex) {
      LOGGER.warn("CommonHibernateUtil checkDBConnection() got error ", ex);
      return false;
    }
  }

  public boolean ping() {
    if (sessionFactory != null) {
      try (Session session = sessionFactory.openSession()) {
        final boolean[] valid = {false};
        session.doWork(
            connection -> {
              if (connection.isValid(databaseConfig.timeout)) {
                valid[0] = true;
              }
            });

        return valid[0];
      } catch (JDBCConnectionException ex) {
        LOGGER.error(
            "CommonHibernateUtil ping() : DB connection not found, got error: {}", ex.getMessage());
        // CommonHibernateUtil.sessionFactory = null;
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
      LOGGER.error("Getting error on health checkReady: " + ex.getMessage(), ex);
      return HealthCheckResponse.ServingStatus.NOT_SERVING;
    }
  }

  public HealthCheckResponse.ServingStatus checkLive() {
    return HealthCheckResponse.ServingStatus.SERVING;
  }

  public static boolean tableExists(Connection conn, DatabaseConfig config, String tableName)
      throws SQLException {
    boolean tExists = false;
    try (ResultSet rs = getTableBasedOnDialect(conn, tableName, config.RdbConfiguration)) {
      while (rs.next()) {
        String tName = rs.getString("TABLE_NAME");
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
      return conn.getMetaData().getTables(rdb.RdbDatabaseName, null, tableName, null);
    }
  }

  protected boolean checkMigrationLockedStatus(String migrationName, RdbConfig rdb)
      throws SQLException, DatabaseException, ClassNotFoundException {
    // Get database connection
    try (Connection con = getDBConnection(rdb)) {

      JdbcConnection jdbcCon = new JdbcConnection(con);

      Statement stmt = jdbcCon.createStatement();

      String sql =
          "SELECT * FROM migration_status ms WHERE ms.migration_name = '" + migrationName + "'";
      ResultSet rs = stmt.executeQuery(sql);

      boolean locked = false;
      // Extract data from result set
      while (rs.next()) {
        // Retrieve by column name
        int id = rs.getInt("id");
        locked = rs.getBoolean("status");
        String migrationNameDB = rs.getString("migration_name");

        // Display values
        LOGGER.debug("Id: {}, Locked: {}, migration_name: {}", id, locked, migrationNameDB);
        LOGGER.debug("migration {} locked: {}", migrationNameDB, locked);
      }
      rs.close();
      stmt.close();

      return locked;
    } catch (DatabaseException e) {
      LOGGER.error(e.getMessage(), e);
      throw e;
    }
  }

  protected void lockedMigration(String migrationName, RdbConfig rdb)
      throws SQLException, DatabaseException, ClassNotFoundException {
    // Get database connection
    try (Connection con = getDBConnection(rdb)) {

      JdbcConnection jdbcCon = new JdbcConnection(con);

      Statement stmt = jdbcCon.createStatement();

      String sql =
          "INSERT INTO migration_status (migration_name, status) VALUES ('"
              + migrationName
              + "', 1);";
      int updatedRowCount = stmt.executeUpdate(sql);
      stmt.close();
      LOGGER.debug("migration {} locked: {}", migrationName, updatedRowCount > 0);
    } catch (DatabaseException e) {
      LOGGER.error(e.getMessage(), e);
      throw e;
    }
  }

  public void runLiquibaseMigration(DatabaseConfig config)
      throws InterruptedException, LiquibaseException, SQLException, ClassNotFoundException {
    runLiquibaseMigration(config, liquibaseRootFilePath);
  }

  public void runLiquibaseMigration(DatabaseConfig config, String liquibaseRootPath)
      throws InterruptedException, LiquibaseException, SQLException, ClassNotFoundException {
    // Change liquibase default table names
    System.getProperties().put("liquibase.databaseChangeLogTableName", "database_change_log");
    System.getProperties()
        .put("liquibase.databaseChangeLogLockTableName", "database_change_log_lock");

    // Lock to RDB for now
    RdbConfig rdb = config.RdbConfiguration;

    createDBIfNotExists(rdb);

    // Check DB is up or not
    boolean dbConnectionStatus = checkDBConnection(rdb, config.timeout);
    if (!dbConnectionStatus) {
      checkDBConnectionInLoop(true);
    }

    releaseLiquibaseLock(config);

    // Run tables liquibase migration
    createTablesLiquibaseMigration(config, config.changeSetToRevertUntilTag, liquibaseRootPath);
  }

  public void createDBIfNotExists(RdbConfig rdb) throws SQLException {
    LOGGER.info("Checking DB " + rdb.RdbUrl);
    Properties properties = new Properties();
    properties.put("user", rdb.RdbUsername);
    properties.put("password", rdb.RdbPassword);
    properties.put("sslMode", rdb.sslMode);
    final var dbUrl = RdbConfig.buildDatabaseServerConnectionString(rdb);
    LOGGER.info("Connecting to DB server url " + dbUrl);
    Connection connection = DriverManager.getConnection(dbUrl, properties);
    ResultSet resultSet = connection.getMetaData().getCatalogs();

    while (resultSet.next()) {
      String databaseNameRes = resultSet.getString(1);
      if (rdb.RdbDatabaseName.equals(databaseNameRes)) {
        System.out.println("the database " + rdb.RdbDatabaseName + " exists");
        return;
      }
    }

    var dbName = RdbConfig.buildDatabaseName(rdb);

    System.out.println("the database " + rdb.RdbDatabaseName + " does not exists");
    Statement statement = connection.createStatement();
    statement.executeUpdate("CREATE DATABASE " + dbName);
    System.out.println("the database " + rdb.RdbDatabaseName + " created successfully");
  }
}
