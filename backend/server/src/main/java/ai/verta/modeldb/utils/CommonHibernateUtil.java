package ai.verta.modeldb.utils;

import ai.verta.modeldb.common.CommonDBUtil;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.DatabaseConfig;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import io.grpc.health.v1.HealthCheckResponse;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.mariadb.jdbc.MariaDbDataSource;

public abstract class CommonHibernateUtil extends CommonDBUtil {
  private static final Logger LOGGER = LogManager.getLogger(CommonHibernateUtil.class);
  private StandardServiceRegistry registry;
  private SessionFactory sessionFactory;
  private boolean isReady = false;
  protected Config config;
  protected DatabaseConfig databaseConfig;
  protected static Class<?>[] entities;
  protected static String liquibaseRootFilePath;

  public Connection getConnection() throws SQLException {
    return getSessionFactory()
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
      final var idleTimeoutMillis = 90_000L;
      final var connectionTimeoutMillis = config.getConnectionTimeoutMillis();
      final var connectionMaxLifetimeMillis = 300_000L;
      final var url = RdbConfig.buildDatabaseConnectionString(rdb);
      final var connectionProviderClass = HikariCPConnectionProvider.class.getName();
      final var datasourceClass = getDatasourceClass(rdb);

      // Hibernate settings equivalent to hibernate.cfg.xml properties
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
                  String.valueOf(config.getLeakDetectionThresholdMs()));

      LOGGER.trace("connectionString {}", connectionString);
      // Create registry builder
      StandardServiceRegistryBuilder registryBuilder =
          new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
      registry = registryBuilder.build();
      var metaDataSrc = new MetadataSources(registry);
      for (Class<?> entity : entities) {
        metaDataSrc.addAnnotatedClass(entity);
      }

      LOGGER.info("Checking DB Connection");
      boolean dbConnectionStatus = checkDBConnection(rdb, config.getTimeout());
      if (!dbConnectionStatus) {
        checkDBConnectionInLoop(config, true);
      }

      // Create session factory and validate entity
      sessionFactory = metaDataSrc.buildMetadata().buildSessionFactory();

      LOGGER.info(CommonMessages.READY_STATUS, isReady);
      isReady = true;
      return sessionFactory;
    } catch (Exception e) {
      LOGGER.warn("CommonHibernateUtil getSessionFactory() getting error : {}", e.getMessage(), e);
      if (registry != null) {
        StandardServiceRegistryBuilder.destroy(registry);
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
    if (rdbConfiguration.isH2()) {
      return JdbcDataSource.class.getName();
    }
    throw new ModelDBException("Unrecognized database " + rdbConfiguration.getRdbDialect());
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
        checkDBConnectionInLoop(databaseConfig, false);
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
}
