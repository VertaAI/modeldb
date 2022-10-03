package ai.verta.modeldb.config;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.common.config.ServiceUserConfig;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
@Getter
@Setter(AccessLevel.NONE)
public class TestConfig extends MDBConfig {
  @JsonProperty private static TestConfig config = null;

  @JsonProperty private final Map<String, ServiceUserConfig> testUsers = new HashMap<>();

  public static TestConfig getInstance() throws InternalErrorException {
    if (config == null) {
      config = getInstance(TestConfig.class, ModelDBConstants.VERTA_MODELDB_TEST_CONFIG);
      config.validate();
    }
    return config;
  }

  @Override
  public void validate() throws InvalidConfigException {
    if (getDatabase() == null)
      throw new InvalidConfigException("database", CommonMessages.MISSING_REQUIRED);
    getDatabase().validate("database");

    if (getService_user() != null) {
      getService_user().validate("service_user");
    }

    if (config.hasAuth() && testUsers.isEmpty()) {
      throw new InvalidConfigException("testUsers", CommonMessages.MISSING_REQUIRED);
    }

    for (Map.Entry<String, ServiceUserConfig> entry : testUsers.entrySet()) {
      entry.getValue().validate(entry.getKey());
    }

    if (getArtifactStoreConfig() == null) {
      throw new InvalidConfigException("artifactStoreConfig", CommonMessages.MISSING_REQUIRED);
    }
    getArtifactStoreConfig().validate("artifactStoreConfig");

    if (getMigrations() != null) {
      for (MigrationConfig migrationConfig : getMigrations()) {
        migrationConfig.validate("migration");
      }
    }
  }

  public boolean hasAuth() {
    return getAuthService() != null;
  }

  @Override
  public boolean hasServiceAccount() {
    return getService_user() != null;
  }

  @Override
  public FutureJdbi getJdbi() {
    if (this.jdbi == null) {
      // Initialize HikariCP and jdbi
      final var databaseConfig = config.getDatabase();
      this.jdbi = initializeFutureJdbi(databaseConfig, "modeldb-test");
    }
    return this.jdbi;
  }

  public boolean testsShouldRunIsolatedFromDependencies() {
    return getDatabase().getRdbConfiguration().isH2();
  }
}
