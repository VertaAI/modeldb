package ai.verta.modeldb.common;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.DatabaseConfig;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import io.grpc.health.v1.HealthCheckResponse;
import java.util.Optional;
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
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import java.sql.*;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.Properties;

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

        // Initialize background utils count
        CommonUtils.initializeBackgroundUtilsCount();

        // Hibernate settings equivalent to hibernate.cfg.xml's properties
        Configuration configuration = new Configuration();

        Properties settings = new Properties();
        RdbConfig rdb = config.RdbConfiguration;

        String connectionString =
            rdb.RdbUrl
                + "/"
                + rdb.RdbDatabaseName
                + "?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8"
                + "&sslMode="
                + rdb.sslMode;
        settings.put(Environment.DRIVER, rdb.RdbDriver);
        settings.put(Environment.URL, connectionString);
        settings.put(Environment.USER, rdb.RdbUsername);
        settings.put(Environment.PASS, rdb.RdbPassword);
        settings.put(Environment.DIALECT, rdb.RdbDialect);
        settings.put(Environment.HBM2DDL_AUTO, "validate");
        settings.put(Environment.SHOW_SQL, "false");
        settings.put("hibernate.hikari.maxLifetime", config.maxLifetime);
        settings.put("hibernate.hikari.idleTimeout", config.idleTimeout);
        settings.put("hibernate.hikari.minimumIdle", config.minConnectionPoolSize);
        settings.put("hibernate.hikari.maximumPoolSize", config.maxConnectionPoolSize);
        settings.put("hibernate.hikari.connectionTimeout", config.connectionTimeout);
        settings.put(Environment.QUERY_PLAN_CACHE_MAX_SIZE, 200);
        settings.put(Environment.QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE, 20);
        configuration.setProperties(settings);

        LOGGER.trace("connectionString {}", connectionString);
        // Create registry builder
        StandardServiceRegistryBuilder registryBuilder =
            new StandardServiceRegistryBuilder().applySettings(settings);
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

  public void setMaxAllowedPacket(JdbcConnection jdbcCon, Integer maxAllowedPacket)
      throws DatabaseException, SQLException {
    if (maxAllowedPacket != null) {
      Statement stmt = jdbcCon.createStatement();
      ResultSet rsMaxAllowedPacket = stmt.executeQuery("SHOW VARIABLES LIKE 'max_allowed_packet'");
      boolean shouldChangeMaxAllowedPacket = true;
      Optional<Integer> maxAllowedPacketFromDatabase;
      if (rsMaxAllowedPacket.next()) {
        maxAllowedPacketFromDatabase = Optional.of(rsMaxAllowedPacket.getInt(2));
        rsMaxAllowedPacket.close();
        shouldChangeMaxAllowedPacket =
            maxAllowedPacketFromDatabase.get() != maxAllowedPacket / 1024 * 1024;
      } else {
        maxAllowedPacketFromDatabase = Optional.empty();
      }
      if (shouldChangeMaxAllowedPacket) {
        LOGGER.info("Changing maxAllowedPacket. Old value: {}, new value: {}",
            maxAllowedPacketFromDatabase.map(Object::toString).orElse("N/A"),
            maxAllowedPacket);
        ResultSet rs = stmt.executeQuery(maxAllowedPacketQuery(maxAllowedPacket));
        rs.close();
      }
      stmt.close();
      if (shouldChangeMaxAllowedPacket) {
        resetSessionFactory();
      }
    }
  }

  public static String maxAllowedPacketQuery(Integer maxAllowedPacket) {
    return String.format("SET GLOBAL max_allowed_packet=%d",
        maxAllowedPacket);
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
      setMaxAllowedPacket(jdbcCon, databaseConfig.RdbConfiguration.maxAllowedPacket);

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

  public Connection getDBConnection(RdbConfig rdb) throws SQLException, ClassNotFoundException {
    String connectionString =
        rdb.RdbUrl
            + "/"
            + rdb.RdbDatabaseName
            + "?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8"
            + "&sslMode="
            + rdb.sslMode;
    try {
      Class.forName(rdb.RdbDriver);
    } catch (ClassNotFoundException e) {
      LOGGER.warn("CommonHibernateUtil getDBConnection() got error ", e);
      throw e;
    }
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

  public boolean tableExists(Connection conn, DatabaseConfig config, String tableName)
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

  private ResultSet getTableBasedOnDialect(Connection conn, String tableName, RdbConfig rdb)
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

    if (rdb.RdbDatabaseName.contains("-")) {
      if (rdb.isPostgres()) {
        throw new InterruptedException("Postgres doesn't allow '-' in the database name");
      } else {
        createDBIfNotExists(rdb);
      }
    }

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
    Connection connection =
        DriverManager.getConnection(rdb.RdbUrl, properties);
    ResultSet resultSet = connection.getMetaData().getCatalogs();

    while (resultSet.next()) {
      String databaseNameRes = resultSet.getString(1);
      if (rdb.RdbDatabaseName.equals(databaseNameRes)) {
        System.out.println("the database " + rdb.RdbDatabaseName + " exists");
        return;
      }
    }

    String quotedDBName = String.format("`%s`", rdb.RdbDatabaseName);

    System.out.println("the database " + rdb.RdbDatabaseName + " does not exists");
    Statement statement = connection.createStatement();
    statement.executeUpdate("CREATE DATABASE " + quotedDBName);
    System.out.println("the database " + rdb.RdbDatabaseName + " created successfully");
  }
}
