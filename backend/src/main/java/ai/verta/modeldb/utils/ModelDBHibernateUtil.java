package ai.verta.modeldb.utils;

import ai.verta.common.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.batchProcess.*;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.config.DatabaseConfig;
import ai.verta.modeldb.config.MigrationConfig;
import ai.verta.modeldb.config.RdbConfig;
import ai.verta.modeldb.entities.*;
import ai.verta.modeldb.entities.audit_log.AuditLogLocalEntity;
import ai.verta.modeldb.entities.code.GitCodeBlobEntity;
import ai.verta.modeldb.entities.code.NotebookCodeBlobEntity;
import ai.verta.modeldb.entities.config.ConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterElementConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterElementMappingEntity;
import ai.verta.modeldb.entities.config.HyperparameterSetConfigBlobEntity;
import ai.verta.modeldb.entities.dataset.PathDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.dataset.QueryDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.dataset.S3DatasetComponentBlobEntity;
import ai.verta.modeldb.entities.environment.*;
import ai.verta.modeldb.entities.metadata.KeyValuePropertyMappingEntity;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
import ai.verta.modeldb.entities.metadata.MetadataPropertyMappingEntity;
import ai.verta.modeldb.entities.versioning.*;
import ai.verta.modeldb.exceptions.AlreadyExistsException;
import ai.verta.modeldb.exceptions.ModelDBException;
import ai.verta.modeldb.exceptions.UnavailableException;
import com.google.common.base.Joiner;
import io.grpc.health.v1.HealthCheckResponse;
import java.sql.*;
import java.util.*;
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
import org.hibernate.query.Query;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

public class ModelDBHibernateUtil {
  private static final Logger LOGGER = LogManager.getLogger(ModelDBHibernateUtil.class);
  private static StandardServiceRegistry registry;
  private static SessionFactory sessionFactory;
  private static Boolean isReady = false;
  public static DatabaseConfig config;
  private static Class[] entities = {
    ProjectEntity.class,
    ExperimentEntity.class,
    ExperimentRunEntity.class,
    KeyValueEntity.class,
    ArtifactEntity.class,
    ArtifactPartEntity.class,
    FeatureEntity.class,
    TagsMapping.class,
    ObservationEntity.class,
    JobEntity.class,
    GitSnapshotEntity.class,
    CodeVersionEntity.class,
    DatasetEntity.class,
    DatasetVersionEntity.class,
    RawDatasetVersionInfoEntity.class,
    PathDatasetVersionInfoEntity.class,
    DatasetPartInfoEntity.class,
    QueryDatasetVersionInfoEntity.class,
    QueryParameterEntity.class,
    CommentEntity.class,
    UserCommentEntity.class,
    ArtifactStoreMapping.class,
    AttributeEntity.class,
    LineageEntity.class,
    RepositoryEntity.class,
    CommitEntity.class,
    LabelsMappingEntity.class,
    TagsEntity.class,
    PathDatasetComponentBlobEntity.class,
    S3DatasetComponentBlobEntity.class,
    InternalFolderElementEntity.class,
    EnvironmentBlobEntity.class,
    DockerEnvironmentBlobEntity.class,
    PythonEnvironmentBlobEntity.class,
    PythonEnvironmentRequirementBlobEntity.class,
    EnvironmentCommandLineEntity.class,
    EnvironmentVariablesEntity.class,
    BranchEntity.class,
    HyperparameterElementConfigBlobEntity.class,
    HyperparameterSetConfigBlobEntity.class,
    ConfigBlobEntity.class,
    GitCodeBlobEntity.class,
    NotebookCodeBlobEntity.class,
    BranchEntity.class,
    VersioningModeldbEntityMapping.class,
    HyperparameterElementMappingEntity.class,
    MetadataPropertyMappingEntity.class,
    DatasetRepositoryMappingEntity.class,
    UploadStatusEntity.class,
    KeyValuePropertyMappingEntity.class,
    QueryDatasetComponentBlobEntity.class,
    AuditLogLocalEntity.class
  };

  private ModelDBHibernateUtil() {}

  public static Connection getConnection() throws SQLException {
    return sessionFactory
        .getSessionFactoryOptions()
        .getServiceRegistry()
        .getService(ConnectionProvider.class)
        .getConnection();
  }

  public static SessionFactory createOrGetSessionFactory(DatabaseConfig config)
      throws ModelDBException {
    if (sessionFactory == null) {
      LOGGER.info("Fetching sessionFactory");
      try {
        ModelDBHibernateUtil.config = config;

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
                + "?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8";
        settings.put(Environment.DRIVER, rdb.RdbDriver);
        settings.put(Environment.URL, connectionString);
        settings.put(Environment.USER, rdb.RdbUsername);
        settings.put(Environment.PASS, rdb.RdbPassword);
        settings.put(Environment.DIALECT, rdb.RdbDialect);
        settings.put(Environment.HBM2DDL_AUTO, "validate");
        settings.put(Environment.SHOW_SQL, "false");
        settings.put("hibernate.c3p0.testConnectionOnCheckin", "true");
        // Reduce this time period if stale connections still exist
        settings.put("hibernate.c3p0.idleConnectionTestPeriod", "100");
        settings.put("hibernate.c3p0.preferredTestQuery", "Select 1");
        settings.put(Environment.C3P0_MIN_SIZE, config.minConnectionPoolSize);
        settings.put(Environment.C3P0_MAX_SIZE, config.maxConnectionPoolSize);
        settings.put(Environment.C3P0_TIMEOUT, config.connectionTimeout);
        settings.put(Environment.QUERY_PLAN_CACHE_MAX_SIZE, 200);
        settings.put(Environment.QUERY_PLAN_CACHE_PARAMETER_METADATA_MAX_SIZE, 20);
        configuration.setProperties(settings);

        LOGGER.trace("connectionString {}", connectionString);
        // Create registry builder
        StandardServiceRegistryBuilder registryBuilder =
            new StandardServiceRegistryBuilder().applySettings(settings);
        MetadataSources metaDataSrc = new MetadataSources(registryBuilder.build());
        for (Class entity : entities) {
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
        if (ModelDBConstants.EXPORT_SCHEMA) {
          exportSchema(metaDataSrc.buildMetadata());
        }

        LOGGER.info(ModelDBMessages.READY_STATUS, isReady);
        isReady = true;
        return sessionFactory;
      } catch (Exception e) {
        LOGGER.warn(
            "ModelDBHibernateUtil getSessionFactory() getting error : {}", e.getMessage(), e);
        if (registry != null) {
          StandardServiceRegistryBuilder.destroy(registry);
        }
        throw new ModelDBException(e.getMessage());
      }
    } else {
      return loopBack(sessionFactory);
    }
  }

  public static SessionFactory getSessionFactory() {
    return createOrGetSessionFactory(Config.getInstance().database);
  }

  private static SessionFactory loopBack(SessionFactory sessionFactory) {
    try {
      LOGGER.debug("ModelDBHibernateUtil checking DB connection");
      boolean dbConnectionLive = checkDBConnection(config.RdbConfiguration, config.timeout);
      if (dbConnectionLive) {
        return sessionFactory;
      }
      // Check DB connection based on the periodic time logic
      checkDBConnectionInLoop(false);
      sessionFactory = resetSessionFactory();
      LOGGER.debug("ModelDBHibernateUtil getSessionFactory() DB connection got successfully");
      return sessionFactory;
    } catch (Exception ex) {
      LOGGER.warn("ModelDBHibernateUtil loopBack() getting error ", ex);
      throw new UnavailableException(ex.getMessage());
    }
  }

  public static SessionFactory resetSessionFactory() {
    isReady = false;
    ModelDBHibernateUtil.sessionFactory = null;
    return getSessionFactory();
  }

  public static void checkDBConnectionInLoop(boolean isStartUpTime) throws InterruptedException {
    int loopBackTime = 5;
    int loopIndex = 0;
    boolean dbConnectionLive = false;
    while (!dbConnectionLive) {
      if (loopIndex < 10 || isStartUpTime) {
        Thread.sleep(loopBackTime);
        LOGGER.debug(
            "ModelDBHibernateUtil getSessionFactory() retrying for DB connection after {} millisecond ",
            loopBackTime);
        loopBackTime = loopBackTime * 2;
        loopIndex = loopIndex + 1;
        dbConnectionLive = checkDBConnection(config.RdbConfiguration, config.timeout);
        if (isStartUpTime && loopBackTime >= 2560) {
          loopBackTime = 2560;
        }
      } else {
        throw new UnavailableException("DB connection not found after 2560 millisecond");
      }
    }
  }

  private static void exportSchema(Metadata buildMetadata) {
    String rootPath = System.getProperty(ModelDBConstants.userDir);
    rootPath = rootPath + "\\src\\main\\resources\\liquibase\\hibernate-base-db-schema.sql";
    new SchemaExport()
        .setDelimiter(";")
        .setOutputFile(rootPath)
        .create(EnumSet.of(TargetType.SCRIPT), buildMetadata);
  }

  public static void shutdown() {
    if (registry != null) {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }

  public static void releaseLiquibaseLock(DatabaseConfig config)
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

  public static void createTablesLiquibaseMigration(
      DatabaseConfig config, String changeSetToRevertUntilTag)
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
      String rootPath = System.getProperty(ModelDBConstants.userDir);
      rootPath = rootPath + "\\src\\main\\resources\\liquibase\\db-changelog-master.xml";
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
              "ModelDBHibernateUtil createTablesLiquibaseMigration() getting LockException ", ex);
          releaseLiquibaseLock(config);
        }
      }
    }
  }

  public static boolean checkDBConnection() {
    return checkDBConnection(config.RdbConfiguration, config.timeout);
  }

  public static Connection getDBConnection(RdbConfig rdb)
      throws SQLException, ClassNotFoundException {
    String connectionString =
        rdb.RdbUrl
            + "/"
            + rdb.RdbDatabaseName
            + "?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8";
    try {
      Class.forName(rdb.RdbDriver);
    } catch (ClassNotFoundException e) {
      LOGGER.warn("ModelDBHibernateUtil getDBConnection() got error ", e);
      throw e;
    }
    return DriverManager.getConnection(connectionString, rdb.RdbUsername, rdb.RdbPassword);
  }

  public static boolean checkDBConnection(RdbConfig rdb, Integer timeout) {
    try (Connection con = getDBConnection(rdb)) {
      return con.isValid(timeout);
    } catch (Exception ex) {
      LOGGER.warn("ModelDBHibernateUtil checkDBConnection() got error ", ex);
      return false;
    }
  }

  public static boolean ping() {
    if (sessionFactory != null) {
      try (Session session = sessionFactory.openSession()) {
        final boolean[] valid = {false};
        session.doWork(
            connection -> {
              if (connection.isValid(config.timeout)) {
                valid[0] = true;
              }
            });

        return valid[0];
      } catch (JDBCConnectionException ex) {
        LOGGER.error(
            "ModelDBHibernateUtil ping() : DB connection not found, got error: {}",
            ex.getMessage());
        // ModelDBHibernateUtil.sessionFactory = null;
      }
    }
    return false;
  }

  public static HealthCheckResponse.ServingStatus checkReady() {
    if (isReady && ping()) {
      return HealthCheckResponse.ServingStatus.SERVING;
    }
    return HealthCheckResponse.ServingStatus.NOT_SERVING;
  }

  public static HealthCheckResponse.ServingStatus checkLive() {
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

  public static void checkIfEntityAlreadyExists(
      Session session,
      String shortName,
      String command,
      String entityName,
      String fieldName,
      String name,
      String workspaceColumnName,
      String workspaceId,
      WorkspaceType workspaceType,
      Logger logger) {
    Query query =
        getWorkspaceEntityQuery(
            session,
            shortName,
            command,
            fieldName,
            name,
            workspaceColumnName,
            workspaceId,
            workspaceType,
            true,
            null);
    Long count = (Long) query.uniqueResult();

    if (count > 0) {
      // Throw error if it is an insert request and project with same name already exists
      logger.info(entityName + " with name {} already exists", name);
      throw new AlreadyExistsException(entityName + " already exists in database");
    }
  }

  public static Query getWorkspaceEntityQuery(
      Session session,
      String shortName,
      String command,
      String fieldName,
      String name,
      String workspaceColumnName,
      String workspaceId,
      WorkspaceType workspaceType,
      boolean shouldSetName,
      List<String> ordering) {
    StringBuilder stringQueryBuilder = new StringBuilder(command);
    stringQueryBuilder
        .append(" AND ")
        .append(shortName)
        .append(".")
        .append(ModelDBConstants.DELETED)
        .append(" = false ");
    if (workspaceId != null && !workspaceId.isEmpty()) {
      if (shouldSetName) {
        stringQueryBuilder.append(" AND ");
      }
      stringQueryBuilder
          .append(shortName)
          .append(".")
          .append(workspaceColumnName)
          .append(" =: ")
          .append(workspaceColumnName)
          .append(" AND ")
          .append(shortName)
          .append(".")
          .append(ModelDBConstants.WORKSPACE_TYPE)
          .append(" =: ")
          .append(ModelDBConstants.WORKSPACE_TYPE);
    }

    if (ordering != null && !ordering.isEmpty()) {
      stringQueryBuilder.append(" order by ");
      Joiner joiner = Joiner.on(",");
      stringQueryBuilder.append(joiner.join(ordering));
    }
    Query query = session.createQuery(stringQueryBuilder.toString());
    if (shouldSetName) {
      query.setParameter(fieldName, name);
    }
    if (workspaceId != null && !workspaceId.isEmpty()) {
      query.setParameter(workspaceColumnName, workspaceId);
      query.setParameter(ModelDBConstants.WORKSPACE_TYPE, workspaceType.getNumber());
    }
    return query;
  }

  /**
   * If you want to define new migration then add new if check for your migration in `if (migration)
   * {` condition.
   */
  @SuppressWarnings("unchecked")
  public static void runMigration(DatabaseConfig config)
      throws ClassNotFoundException, ModelDBException, DatabaseException, SQLException {
    RdbConfig rdb = config.RdbConfiguration;

    if (config.migrations != null) {
      for (MigrationConfig migrationConfig : config.migrations) {
        if (!migrationConfig.enabled) {
          continue;
        }
        switch (migrationConfig.name) {
          case ModelDBConstants.SUB_ENTITIES_OWNERS_RBAC_MIGRATION:
            OwnerRoleBindingUtils.execute();
            break;
          case ModelDBConstants.SUB_ENTITIES_REPOSITORY_OWNERS_RBAC_MIGRATION:
            OwnerRoleBindingRepositoryUtils.execute();
            break;
          case ModelDBConstants.POPULATE_VERSION_MIGRATION:
            PopulateVersionMigration.execute(migrationConfig.record_update_limit);
            break;
        }
      }
    }

    CollaboratorResourceMigration.execute();
  }

  private static boolean checkMigrationLockedStatus(String migrationName, RdbConfig rdb)
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

  private static void lockedMigration(String migrationName, RdbConfig rdb)
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

  public static void runLiquibaseMigration(DatabaseConfig config)
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
        ModelDBHibernateUtil.createDBIfNotExists(rdb);
      }
    }

    // Check DB is up or not
    boolean dbConnectionStatus = ModelDBHibernateUtil.checkDBConnection(rdb, config.timeout);
    if (!dbConnectionStatus) {
      ModelDBHibernateUtil.checkDBConnectionInLoop(true);
    }

    ModelDBHibernateUtil.releaseLiquibaseLock(config);

    // Run tables liquibase migration
    ModelDBHibernateUtil.createTablesLiquibaseMigration(config, config.changeSetToRevertUntilTag);
  }

  public static void createDBIfNotExists(RdbConfig rdb) throws SQLException {

    Connection connection =
        DriverManager.getConnection(rdb.RdbUrl, rdb.RdbUsername, rdb.RdbPassword);
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
