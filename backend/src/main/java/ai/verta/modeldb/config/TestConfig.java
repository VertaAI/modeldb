package ai.verta.modeldb.config;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.common.config.ServiceUserConfig;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import java.util.HashMap;
import java.util.Map;

public class TestConfig extends MDBConfig {
  private static TestConfig config = null;

  public Map<String, ServiceUserConfig> testUsers = new HashMap<>();

  public static TestConfig getInstance() throws InternalErrorException {
    if (config == null) {
      config = getInstance(TestConfig.class, ModelDBConstants.VERTA_MODELDB_TEST_CONFIG);
      config.Validate();
    }
    return config;
  }

  public void Validate() throws InvalidConfigException {
    if (database == null) throw new InvalidConfigException("database", MISSING_REQUIRED);
    database.Validate("database");

    if (service_user != null) {
      service_user.Validate("service_user");
    }

    if (config.hasAuth() && testUsers == null) {
      throw new InvalidConfigException("testUsers", MISSING_REQUIRED);
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

  public FutureJdbi getJdbi() {
    if (this.jdbi == null) {
      // Initialize HikariCP and jdbi
      final var databaseConfig = config.database;
      this.jdbi = initializeFutureJdbi(databaseConfig, "modeldb-test");
    }
    return this.jdbi;
  }
}
