package ai.verta.modeldb.utils;

import ai.verta.common.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.batchProcess.DatasetToRepositoryMigration;
import ai.verta.modeldb.batchProcess.OwnerRoleBindingRepositoryUtils;
import ai.verta.modeldb.batchProcess.OwnerRoleBindingUtils;
import ai.verta.modeldb.batchProcess.PopulateVersionMigration;
import ai.verta.modeldb.entities.ArtifactEntity;
import ai.verta.modeldb.entities.ArtifactPartEntity;
import ai.verta.modeldb.entities.ArtifactStoreMapping;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.CodeVersionEntity;
import ai.verta.modeldb.entities.CommentEntity;
import ai.verta.modeldb.entities.DatasetEntity;
import ai.verta.modeldb.entities.DatasetPartInfoEntity;
import ai.verta.modeldb.entities.DatasetVersionEntity;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.entities.FeatureEntity;
import ai.verta.modeldb.entities.GitSnapshotEntity;
import ai.verta.modeldb.entities.JobEntity;
import ai.verta.modeldb.entities.KeyValueEntity;
import ai.verta.modeldb.entities.LineageEntity;
import ai.verta.modeldb.entities.ObservationEntity;
import ai.verta.modeldb.entities.PathDatasetVersionInfoEntity;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.QueryDatasetVersionInfoEntity;
import ai.verta.modeldb.entities.QueryParameterEntity;
import ai.verta.modeldb.entities.RawDatasetVersionInfoEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.entities.UploadStatusEntity;
import ai.verta.modeldb.entities.UserCommentEntity;
import ai.verta.modeldb.entities.code.GitCodeBlobEntity;
import ai.verta.modeldb.entities.code.NotebookCodeBlobEntity;
import ai.verta.modeldb.entities.config.ConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterElementConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterElementMappingEntity;
import ai.verta.modeldb.entities.config.HyperparameterSetConfigBlobEntity;
import ai.verta.modeldb.entities.dataset.PathDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.dataset.QueryDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.dataset.S3DatasetComponentBlobEntity;
import ai.verta.modeldb.entities.environment.DockerEnvironmentBlobEntity;
import ai.verta.modeldb.entities.environment.EnvironmentBlobEntity;
import ai.verta.modeldb.entities.environment.EnvironmentCommandLineEntity;
import ai.verta.modeldb.entities.environment.EnvironmentVariablesEntity;
import ai.verta.modeldb.entities.environment.PythonEnvironmentBlobEntity;
import ai.verta.modeldb.entities.environment.PythonEnvironmentRequirementBlobEntity;
import ai.verta.modeldb.entities.metadata.KeyValuePropertyMappingEntity;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
import ai.verta.modeldb.entities.metadata.MetadataPropertyMappingEntity;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.DatasetRepositoryMappingEntity;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.TagsEntity;
import ai.verta.modeldb.entities.versioning.VersioningModeldbEntityMapping;
import com.google.common.base.Joiner;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.StatusProto;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
  private static String databaseName;
  private static String rDBDriver;
  private static String rDBUrl;
  public static String rDBDialect;
  private static String configUsername;
  private static String configPassword;
  private static Integer timeout = 4;
  private static Long liquibaseLockThreshold = 0L;
  private static Boolean isReady = false;
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
    QueryDatasetComponentBlobEntity.class
  };
  private static Integer minConnectionPoolSize;
  private static Integer maxConnectionPoolSize;
  private static Integer connectionTimeout;

  private ModelDBHibernateUtil() {}

  public static Connection getConnection() throws SQLException {
    return sessionFactory
        .getSessionFactoryOptions()
        .getServiceRegistry()
        .getService(ConnectionProvider.class)
        .getConnection();
  }

  public static SessionFactory createOrGetSessionFactory() throws ModelDBException {
    if (sessionFactory == null) {
      LOGGER.info("Fetching sessionFactory");
      try {
        App app = App.getInstance();
        Map<String, Object> databasePropMap = app.getDatabasePropMap();

        setDatabaseProperties(app, databasePropMap);

        // Initialize background utils count
        ModelDBUtils.initializeBackgroundUtilsCount();

        // Hibernate settings equivalent to hibernate.cfg.xml's properties
        Configuration configuration = new Configuration();

        Properties settings = new Properties();

        String connectionString =
            rDBUrl
                + "/"
                + databaseName
                + "?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8";
        settings.put(Environment.DRIVER, rDBDriver);
        settings.put(Environment.URL, connectionString);
        settings.put(Environment.USER, configUsername);
        settings.put(Environment.PASS, configPassword);
        settings.put(Environment.DIALECT, rDBDialect);
        settings.put(Environment.HBM2DDL_AUTO, "validate");
        settings.put(Environment.SHOW_SQL, "false");
        settings.put("hibernate.c3p0.testConnectionOnCheckin", "true");
        // Reduce this time period if stale connections still exist
        settings.put("hibernate.c3p0.idleConnectionTestPeriod", "100");
        settings.put("hibernate.c3p0.preferredTestQuery", "Select 1");
        settings.put(Environment.C3P0_MIN_SIZE, minConnectionPoolSize);
        settings.put(Environment.C3P0_MAX_SIZE, maxConnectionPoolSize);
        settings.put(Environment.C3P0_TIMEOUT, connectionTimeout);
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
        boolean dbConnectionStatus =
            checkDBConnection(
                rDBDriver, rDBUrl, databaseName, configUsername, configPassword, timeout);
        if (!dbConnectionStatus) {
          checkDBConnectionInLoop(true);
        }

        // Create session factory and validate entity
        sessionFactory = metaDataSrc.buildMetadata().buildSessionFactory();

        // Export schema
        if (ModelDBConstants.EXPORT_SCHEMA) {
          exportSchema(metaDataSrc.buildMetadata());
        }

        // Check if any migration need to be run or not and based on the migration status flag
        runMigration();

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

  public static void setDatabaseProperties(App app, Map<String, Object> databasePropMap) {
    Map<String, Object> rDBPropMap = (Map<String, Object>) databasePropMap.get("RdbConfiguration");

    databaseName = (String) rDBPropMap.get("RdbDatabaseName");
    if (!app.getTraceEnabled()) {
      rDBDriver = (String) rDBPropMap.get("RdbDriver");
    } else {
      rDBDriver = "io.opentracing.contrib.jdbc.TracingDriver";
    }
    rDBUrl = (String) rDBPropMap.get("RdbUrl");
    rDBDialect = (String) rDBPropMap.get("RdbDialect");
    configUsername = (String) rDBPropMap.get("RdbUsername");
    configPassword = (String) rDBPropMap.get("RdbPassword");
    if (databasePropMap.containsKey("timeout")) {
      timeout = (Integer) databasePropMap.get("timeout");
    }
    minConnectionPoolSize =
        (Integer)
            databasePropMap.getOrDefault(
                ModelDBConstants.MIN_CONNECTION_POOL_SIZE,
                ModelDBConstants.MIN_CONNECTION_SIZE_DEFAULT);
    maxConnectionPoolSize =
        (Integer)
            databasePropMap.getOrDefault(
                ModelDBConstants.MAX_CONNECTION_POOL_SIZE,
                ModelDBConstants.MAX_CONNECTION_SIZE_DEFAULT);
    connectionTimeout =
        (Integer)
            databasePropMap.getOrDefault(
                ModelDBConstants.CONNECTION_TIMEOUT, ModelDBConstants.CONNECTION_TIMEOUT_DEFAULT);
    liquibaseLockThreshold =
        Long.parseLong(databasePropMap.getOrDefault("liquibaseLockThreshold", "60").toString());

    // Change liquibase default table names
    System.getProperties().put("liquibase.databaseChangeLogTableName", "database_change_log");
    System.getProperties()
        .put("liquibase.databaseChangeLogLockTableName", "database_change_log_lock");
  }

  public static SessionFactory getSessionFactory() {
    try {
      return createOrGetSessionFactory();
    } catch (Exception e) {
      Status status =
          Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(e.getMessage()).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  private static SessionFactory loopBack(SessionFactory sessionFactory) {
    try {
      LOGGER.debug("ModelDBHibernateUtil checking DB connection");
      boolean dbConnectionLive =
          checkDBConnection(
              rDBDriver, rDBUrl, databaseName, configUsername, configPassword, timeout);
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
      Status status =
          Status.newBuilder().setCode(Code.UNAVAILABLE_VALUE).setMessage(ex.getMessage()).build();
      throw StatusProto.toStatusRuntimeException(status);
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
        dbConnectionLive =
            checkDBConnection(
                rDBDriver, rDBUrl, databaseName, configUsername, configPassword, timeout);
        if (isStartUpTime && loopBackTime >= 2560) {
          loopBackTime = 2560;
        }
      } else {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("DB connection not found after 2560 millisecond")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
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

  public static void releaseLiquibaseLock(
      String rDBDriver,
      String rDBUrl,
      String databaseName,
      String configUsername,
      String configPassword)
      throws LiquibaseException, SQLException, InterruptedException, ClassNotFoundException {
    // Get database connection
    try (Connection con =
        getDBConnection(rDBDriver, rDBUrl, databaseName, configUsername, configPassword)) {
      boolean existsStatus = tableExists(con, "database_change_log_lock");
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
      if (lastLockAcquireTimestamp != 0 && currentLockedTimeDiffSecond > liquibaseLockThreshold) {
        // Initialize Liquibase and run the update
        Database database =
            DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcCon);
        LockServiceFactory.getInstance().getLockService(database).forceReleaseLock();
        locked = false;
        LOGGER.debug("Release database lock executing query from backend");
      }

      if (locked) {
        Thread.sleep(liquibaseLockThreshold * 1000); // liquibaseLockThreshold = second
        releaseLiquibaseLock(rDBDriver, rDBUrl, databaseName, configUsername, configPassword);
      }
    } catch (InterruptedException e) {
      LOGGER.error(e.getMessage(), e);
      throw e;
    }
  }

  public static void createTablesLiquibaseMigration(
      String rDBDriver,
      String rDBUrl,
      String databaseName,
      String configUsername,
      String configPassword,
      String changeSetToRevertUntilTag)
      throws LiquibaseException, SQLException, InterruptedException, ClassNotFoundException {
    // Get database connection
    try (Connection con =
        getDBConnection(rDBDriver, rDBUrl, databaseName, configUsername, configPassword)) {
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
          releaseLiquibaseLock(rDBDriver, rDBUrl, databaseName, configUsername, configPassword);
        }
      }
    }
  }

  public static boolean checkDBConnection() {
    return checkDBConnection(
        rDBDriver, rDBUrl, databaseName, configUsername, configPassword, timeout);
  }

  public static Connection getDBConnection(
      String rDBDriver,
      String rDBUrl,
      String databaseName,
      String configUsername,
      String configPassword)
      throws SQLException, ClassNotFoundException {
    String connectionString =
        rDBUrl
            + "/"
            + databaseName
            + "?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8";
    try {
      Class.forName(rDBDriver);
    } catch (ClassNotFoundException e) {
      LOGGER.warn("ModelDBHibernateUtil getDBConnection() got error ", e);
      throw e;
    }
    return DriverManager.getConnection(connectionString, configUsername, configPassword);
  }

  public static boolean checkDBConnection(
      String rDBDriver,
      String rDBUrl,
      String databaseName,
      String configUsername,
      String configPassword,
      Integer timeout) {
    try (Connection con =
        getDBConnection(rDBDriver, rDBUrl, databaseName, configUsername, configPassword)) {
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
              if (connection.isValid(timeout)) {
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

  public static boolean tableExists(Connection conn, String tableName) throws SQLException {
    boolean tExists = false;
    try (ResultSet rs = getTableBasedOnDialect(conn, tableName, databaseName, rDBDialect)) {
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

  private static ResultSet getTableBasedOnDialect(
      Connection conn, String tableName, String dbName, String rDBDialect) throws SQLException {
    if (rDBDialect.equals(ModelDBConstants.POSTGRES_DB_DIALECT)) {
      // TODO: make postgres implementation multitenant as well.
      return conn.getMetaData().getTables(null, null, tableName, null);
    } else {
      return conn.getMetaData().getTables(dbName, null, tableName, null);
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
      Status status =
          Status.newBuilder()
              .setCode(Code.ALREADY_EXISTS_VALUE)
              .setMessage(entityName + " already exists in database")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
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
  private static void runMigration() throws ClassNotFoundException {
    App app = App.getInstance();
    Map<String, Map<String, Object>> migrationTypeMap =
        (Map<String, Map<String, Object>>) app.getPropertiesMap().get(ModelDBConstants.MIGRATION);
    if (migrationTypeMap != null && migrationTypeMap.size() > 0) {
      new Thread(
              () -> {
                ModelDBUtils.registeredBackgroundUtilsCount();
                int index = 0;
                try {
                  CompletableFuture<Boolean>[] completableFutures =
                      new CompletableFuture[migrationTypeMap.size()];
                  for (String migrationName : migrationTypeMap.keySet()) {
                    Map<String, Object> migrationDetailMap = migrationTypeMap.get(migrationName);
                    if ((boolean) migrationDetailMap.get(ModelDBConstants.ENABLE)) {
                      if (migrationName.equals(
                          ModelDBConstants.SUB_ENTITIES_OWNERS_RBAC_MIGRATION)) {
                        // Manually migration for populate RoleBinding of experiment, experimentRun
                        // &
                        // datasetVersion owner
                        CompletableFuture<Boolean> futureTask =
                            CompletableFuture.supplyAsync(
                                () -> {
                                  OwnerRoleBindingUtils.execute();
                                  return true;
                                });
                        completableFutures[index] = futureTask;
                        index = index + 1;
                      }
                      if (migrationName.equals(
                          ModelDBConstants.SUB_ENTITIES_REPOSITORY_OWNERS_RBAC_MIGRATION)) {
                        // Manual migration for populate RoleBinding of repository
                        CompletableFuture<Boolean> futureTask =
                            CompletableFuture.supplyAsync(
                                () -> {
                                  OwnerRoleBindingRepositoryUtils.execute();
                                  return true;
                                });
                        completableFutures[index] = futureTask;
                        index = index + 1;
                      }
                      if (migrationName.equals(ModelDBConstants.POPULATE_VERSION_MIGRATION)) {
                        // Manual migration for populate RoleBinding of repository
                        CompletableFuture<Boolean> futureTask =
                            CompletableFuture.supplyAsync(
                                () -> {
                                  int recordUpdateLimit =
                                      (int)
                                          migrationDetailMap.getOrDefault(
                                              ModelDBConstants.RECORD_UPDATE_LIMIT, 100);
                                  PopulateVersionMigration.execute(recordUpdateLimit);
                                  return true;
                                });
                        completableFutures[index] = futureTask;
                        index = index + 1;
                      }
                      // add if here for the new migration type
                    }
                  }
                  if (index > 0) {
                    CompletableFuture<Void> combinedFuture =
                        CompletableFuture.allOf(completableFutures);
                    combinedFuture.get();
                    LOGGER.info("Finished all the future tasks");
                  }
                } catch (InterruptedException | ExecutionException e) {
                  LOGGER.warn(
                      "ModelDBHibernateUtil runMigration() getting error : {}", e.getMessage(), e);
                } finally {
                  ModelDBUtils.unregisteredBackgroundUtilsCount();
                }
              })
          .start();

      // Blocking migration
      String migrationName = ModelDBConstants.DATASET_VERSIONING_MIGRATION;
      if (migrationTypeMap.containsKey(migrationName)) {
        Map<String, Object> migrationDetailMap = migrationTypeMap.get(migrationName);
        if ((boolean) migrationDetailMap.get(ModelDBConstants.ENABLE)) {
          try {
            ModelDBUtils.registeredBackgroundUtilsCount();
            boolean isLocked =
                checkMigrationLockedStatus(
                    migrationName, rDBDriver, rDBUrl, databaseName, configUsername, configPassword);
            if (!isLocked) {
              LOGGER.debug("Obtaingin migration lock");
              lockedMigration(
                  migrationName, rDBDriver, rDBUrl, databaseName, configUsername, configPassword);
              int recordUpdateLimit =
                  (int) migrationDetailMap.getOrDefault(ModelDBConstants.RECORD_UPDATE_LIMIT, 100);
              DatasetToRepositoryMigration.execute(recordUpdateLimit);
            } else {
              LOGGER.debug("Migration already locked");
            }
          } catch (SQLException | DatabaseException e) {
            LOGGER.error("Error on migration: {}", e.getMessage());
          } finally {
            ModelDBUtils.unregisteredBackgroundUtilsCount();
          }
        }
      }
    }
  }

  private static boolean checkMigrationLockedStatus(
      String migrationName,
      String rDBDriver,
      String rDBUrl,
      String databaseName,
      String configUsername,
      String configPassword)
      throws SQLException, DatabaseException, ClassNotFoundException {
    // Get database connection
    try (Connection con =
        getDBConnection(rDBDriver, rDBUrl, databaseName, configUsername, configPassword)) {

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

  private static void lockedMigration(
      String migrationName,
      String rDBDriver,
      String rDBUrl,
      String databaseName,
      String configUsername,
      String configPassword)
      throws SQLException, DatabaseException, ClassNotFoundException {
    // Get database connection
    try (Connection con =
        getDBConnection(rDBDriver, rDBUrl, databaseName, configUsername, configPassword)) {

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

  public static boolean runLiquibaseMigration(Map<String, Object> databasePropMap)
      throws InterruptedException, LiquibaseException, SQLException, ClassNotFoundException {
    setDatabaseProperties(App.getInstance(), databasePropMap);

    // Check DB is up or not
    boolean dbConnectionStatus =
        ModelDBHibernateUtil.checkDBConnection(
            rDBDriver, rDBUrl, databaseName, configUsername, configPassword, timeout);
    if (!dbConnectionStatus) {
      ModelDBHibernateUtil.checkDBConnectionInLoop(true);
    }

    ModelDBHibernateUtil.releaseLiquibaseLock(
        rDBDriver, rDBUrl, databaseName, configUsername, configPassword);

    String changeSetToRevertUntilTag = (String) databasePropMap.get("changeSetToRevertUntilTag");
    // Run tables liquibase migration
    ModelDBHibernateUtil.createTablesLiquibaseMigration(
        rDBDriver, rDBUrl, databaseName, configUsername, configPassword, changeSetToRevertUntilTag);

    LOGGER.info("Liquibase validation stop");

    boolean runLiquibaseSeparate =
        Boolean.parseBoolean(System.getenv(ModelDBConstants.RUN_LIQUIBASE_SEPARATE));
    if (runLiquibaseSeparate) {
      return true;
    }
    return false;
  }
}
