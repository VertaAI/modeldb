package ai.verta.modeldb.config;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.DatabaseConfig;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.config.ServiceUserConfig;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.jdbi.v3.core.Jdbi;

public class TestConfig extends Config {
  private static TestConfig config = null;

  public Map<String, ServiceUserConfig> testUsers = new HashMap<>();

  public TrialConfig trial;
  public ArtifactStoreConfig artifactStoreConfig;
  public List<MigrationConfig> migrations;
  private FutureJdbi jdbi;
  public int jdbi_retry_time = 100; // Time in ms

  public static TestConfig getInstance() throws InternalErrorException {
    if (config == null) {
      config = getInstance(TestConfig.class, ModelDBConstants.VERTA_MODELDB_TEST_CONFIG);
      config.Validate();
    }
    return config;
  }

  public void Validate() throws InvalidConfigException {
    if (database == null) throw new InvalidConfigException("database", TestConfig.MISSING_REQUIRED);
    database.Validate("database");

    if (service_user != null) {
      service_user.Validate("service_user");
    }

    if (config.hasAuth() && testUsers == null) {
      throw new InvalidConfigException("testUsers", TestConfig.MISSING_REQUIRED);
    }

    for (Map.Entry<String, ServiceUserConfig> entry : testUsers.entrySet()) {
      entry.getValue().Validate(entry.getKey());
    }

    if (artifactStoreConfig == null)
      throw new InvalidConfigException("artifactStoreConfig", MISSING_REQUIRED);
    artifactStoreConfig.Validate("artifactStoreConfig");

    if (trial != null) {
      trial.Validate("trial");
    }

    if (migrations != null) {
      for (MigrationConfig migrationConfig : migrations) {
        migrationConfig.Validate("migration");
      }
    }
  }

  public boolean hasAuth() {
    return authService != null;
  }

  @Override
  public boolean hasServiceAccount() {
    return service_user != null;
  }

  public FutureJdbi getTestJdbi() {
    if (this.jdbi == null) {
      // Initialize HikariCP and jdbi
      final var databaseConfig = config.database;
      this.jdbi = initializeJdbi(databaseConfig);
    }
    return this.jdbi;
  }

  private FutureJdbi initializeJdbi(DatabaseConfig databaseConfig) {
    final var hikariDataSource = new HikariDataSource();
    final var dbUrl = RdbConfig.buildDatabaseConnectionString(databaseConfig.RdbConfiguration);
    hikariDataSource.setJdbcUrl(dbUrl);
    hikariDataSource.setUsername(databaseConfig.RdbConfiguration.RdbUsername);
    hikariDataSource.setPassword(databaseConfig.RdbConfiguration.RdbPassword);
    hikariDataSource.setMinimumIdle(Integer.parseInt(databaseConfig.minConnectionPoolSize));
    hikariDataSource.setMaximumPoolSize(Integer.parseInt(databaseConfig.maxConnectionPoolSize));
    hikariDataSource.setRegisterMbeans(true);
    hikariDataSource.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
    hikariDataSource.setPoolName("modeldb_test");

    final Jdbi jdbi = Jdbi.create(hikariDataSource);
    final Executor dbExecutor = FutureGrpc.initializeExecutor(databaseConfig.threadCount);
    return new FutureJdbi(jdbi, dbExecutor);
  }
}
