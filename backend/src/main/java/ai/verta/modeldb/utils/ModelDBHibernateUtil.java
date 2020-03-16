package ai.verta.modeldb.utils;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.entities.ArtifactEntity;
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
import ai.verta.modeldb.entities.UserCommentEntity;
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
import java.util.Map;
import java.util.Properties;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
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
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

public class ModelDBHibernateUtil {
  private static final Logger LOGGER = LogManager.getLogger(ModelDBHibernateUtil.class);
  private static StandardServiceRegistry registry;
  private static SessionFactory sessionFactory;
  private static String databaseName;
  private static String rDBDriver;
  private static String rDBUrl;
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
    LineageEntity.class
  };

  private ModelDBHibernateUtil() {}

  public static SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      LOGGER.info("Fetching sessionFactory");
      try {
        App app = App.getInstance();
        Map<String, Object> databasePropMap = app.getDatabasePropMap();
        Map<String, Object> propertiesMap = app.getPropertiesMap();

        Map<String, Object> rDBPropMap =
            (Map<String, Object>) databasePropMap.get("RdbConfiguration");

        databaseName = (String) rDBPropMap.get("RdbDatabaseName");
        rDBDriver = (String) rDBPropMap.get("RdbDriver");
        rDBUrl = (String) rDBPropMap.get("RdbUrl");
        String rDBDialect = (String) rDBPropMap.get("RdbDialect");
        configUsername = (String) rDBPropMap.get("RdbUsername");
        configPassword = (String) rDBPropMap.get("RdbPassword");
        if (databasePropMap.containsKey("timeout")) {
          timeout = (Integer) databasePropMap.get("timeout");
        }
        liquibaseLockThreshold =
            Long.parseLong(databasePropMap.getOrDefault("liquibaseLockThreshold", "60").toString());

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
        configuration.setProperties(settings);

        LOGGER.trace("connectionString {}", connectionString);
        // Create registry builder
        StandardServiceRegistryBuilder registryBuilder =
            new StandardServiceRegistryBuilder().applySettings(settings);
        MetadataSources metaDataSrc = new MetadataSources(registryBuilder.build());
        for (Class entity : entities) {
          metaDataSrc.addAnnotatedClass(entity);
        }

        releaseLiquibaseLock(metaDataSrc);

        // Run tables liquibase migration
        createTablesLiquibaseMigration(metaDataSrc);

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
        LOGGER.warn("ModelDBHibernateUtil getSessionFactory() getting error ", e);
        if (registry != null) {
          StandardServiceRegistryBuilder.destroy(registry);
        }
        Status status =
            Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(e.getMessage()).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    } else {
      return loopBack(sessionFactory);
    }
  }

  private static SessionFactory loopBack(SessionFactory sessionFactory) {
    try {
      boolean dbConnectionLive = ping();
      if (dbConnectionLive) {
        return sessionFactory;
      }
      int loopBackTime = 5;
      int loopIndex = 0;
      while (!dbConnectionLive) {
        if (loopIndex < 10) {
          Thread.sleep(loopBackTime);
          LOGGER.debug(
              "ModelDBHibernateUtil getSessionFactory() retrying for DB connection after {} millisecond ",
              loopBackTime);
          loopBackTime = loopBackTime * 2;
          loopIndex = loopIndex + 1;
          dbConnectionLive =
              checkDBConnection(
                  rDBDriver, rDBUrl, databaseName, configUsername, configPassword, timeout);
        } else {
          Status status =
              Status.newBuilder()
                  .setCode(Code.UNAVAILABLE_VALUE)
                  .setMessage("DB connection not found after 2560 millisecond")
                  .build();
          throw StatusProto.toStatusRuntimeException(status);
        }
      }
      ModelDBHibernateUtil.sessionFactory = null;
      sessionFactory = getSessionFactory();
      LOGGER.debug("ModelDBHibernateUtil getSessionFactory() DB connection got successfully");
      return sessionFactory;
    } catch (Exception ex) {
      LOGGER.warn("ModelDBHibernateUtil loopBack() getting error ", ex);
      Status status =
          Status.newBuilder().setCode(Code.UNAVAILABLE_VALUE).setMessage(ex.getMessage()).build();
      throw StatusProto.toStatusRuntimeException(status);
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

  private static void releaseLiquibaseLock(MetadataSources metaDataSrc)
      throws LiquibaseException, SQLException {
    // Get database connection
    try (Connection con =
        metaDataSrc.getServiceRegistry().getService(ConnectionProvider.class).getConnection()) {
      JdbcConnection jdbcCon = new JdbcConnection(con);

      Statement stmt = jdbcCon.createStatement();

      // FIXME: Verify if this is the bestway to check if database exists
      // This might not work on non Postgres RDBMS
      String existsSql =
          "SELECT EXISTS (   SELECT 1   FROM   information_schema.tables    WHERE  table_schema = 'public'  AND    table_name = 'databasechangeloglock');";
      ResultSet existTable = stmt.executeQuery(existsSql);
      if (existTable.next()) {
        if (!existTable.getBoolean(1)) {
          LOGGER.info("Table databasechangeloglock does not exists in DB");
          LOGGER.info("Proceeding with liquibase assuming it has never been run");
          existTable.close();
          stmt.close();
          return;
        }
      }

      String sql = "SELECT * FROM DATABASECHANGELOGLOCK WHERE ID = 1";
      ResultSet rs = stmt.executeQuery(sql);

      long lastLockAcquireTimestamp = 0L;
      // Extract data from result set
      while (rs.next()) {
        // Retrieve by column name
        int id = rs.getInt("id");
        boolean locked = rs.getBoolean("locked");
        Timestamp lockGrantedTimeStamp = rs.getTimestamp("lockgranted");
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
        LOGGER.debug("Release database lock executing query froom backend");
      }
    }
  }

  private static void createTablesLiquibaseMigration(MetadataSources metaDataSrc)
      throws LiquibaseException, SQLException {
    // Get database connection
    try (Connection con =
        metaDataSrc.getServiceRegistry().getService(ConnectionProvider.class).getConnection()) {
      JdbcConnection jdbcCon = new JdbcConnection(con);

      // Overwrite default liquibase table names by custom
      GlobalConfiguration liquibaseConfiguration =
          LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class);
      liquibaseConfiguration.setDatabaseChangeLogLockWaitTime(1L);

      // Initialize Liquibase and run the update
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcCon);
      String rootPath = System.getProperty(ModelDBConstants.userDir);
      rootPath = rootPath + "\\src\\main\\resources\\liquibase\\db-changelog-1.0.xml";
      Liquibase liquibase = new Liquibase(rootPath, new FileSystemResourceAccessor(), database);

      boolean liquibaseExecuted = false;
      while (!liquibaseExecuted) {
        try {
          liquibase.update(new Contexts(), new LabelExpression());
          liquibaseExecuted = true;
        } catch (LockException ex) {
          LOGGER.warn(
              "ModelDBHibernateUtil createTablesLiquibaseMigration() getting LockException ", ex);
          releaseLiquibaseLock(metaDataSrc);
        }
      }
    }
  }

  private static void runLiquibaseMigration(MetadataSources metaDataSrc)
      throws LiquibaseException, SQLException {
    // Get database connection
    try (Connection con =
        metaDataSrc.getServiceRegistry().getService(ConnectionProvider.class).getConnection()) {
      JdbcConnection jdbcCon = new JdbcConnection(con);

      // Initialize Liquibase and run the update
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcCon);
      String rootPath = System.getProperty(ModelDBConstants.userDir);
      rootPath = rootPath + "\\src\\main\\resources\\liquibase\\db-changelog-master.xml";
      Liquibase liquibase = new Liquibase(rootPath, new FileSystemResourceAccessor(), database);
      liquibase.update(new Contexts(), new LabelExpression());
    }
  }

  private static boolean checkDBConnection(
      String rDBDriver,
      String rDBUrl,
      String databaseName,
      String configUsername,
      String configPassword,
      Integer timeout) {
    String connectionString =
        rDBUrl
            + "/"
            + databaseName
            + "?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8";

    try {
      Class.forName(rDBDriver);
    } catch (ClassNotFoundException e) {
      LOGGER.warn("ModelDBHibernateUtil checkDBConnection() got error ", e);
      return false;
    }
    try (Connection con =
        DriverManager.getConnection(connectionString, configUsername, configPassword)) {

      return con.isValid(timeout);
    } catch (Exception ex) {
      LOGGER.warn("ModelDBHibernateUtil checkDBConnection() got error ", ex);
      return false;
    }
  }

  private static boolean ping() {
    try (Session session = sessionFactory.openSession()) {
      final boolean[] valid = {false};
      session.doWork(
          connection -> {
            if (connection.isValid(timeout)) {
              valid[0] = true;
            }
          });

      return valid[0];
    }
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
}
