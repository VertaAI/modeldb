package ai.verta.modeldb.config;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.common.config.ServiceUserConfig;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;

public class TestConfig extends MDBConfig {
  private static TestConfig config = null;

  public Map<String, ServiceUserConfig> testUsers = new HashMap<>();

  public static TestConfig getInstance() throws InternalErrorException {
    if (config == null) {
      config = getInstance(TestConfig.class, ModelDBConstants.VERTA_MODELDB_TEST_CONFIG);
      config.validate();
    }
    return config;
  }

  public static TestConfig getInstance(String path) throws InternalErrorException {
    if (config == null) {
      config = readConfig(TestConfig.class, path);
      config.validate();
    }
    return config;
  }

  private static <T> T readConfig(Class<T> configType, String configFile)
      throws InternalErrorException {
    try {
      var yaml = new Yaml(new Constructor(configType));
      configFile = CommonUtils.appendOptionalTelepresencePath(configFile);
      InputStream inputStream = new FileInputStream(configFile);
      yaml.setBeanAccess(BeanAccess.FIELD);
      return yaml.loadAs(inputStream, configType);
    } catch (ModelDBException | NullPointerException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new InternalErrorException(ex.getMessage());
    }
  }

  @Override
  public void validate() throws InvalidConfigException {
    if (getDatabase() == null) throw new InvalidConfigException("database", MISSING_REQUIRED);
    getDatabase().validate("database");

    if (getService_user() != null) {
      getService_user().validate("service_user");
    }

    if (config.hasAuth() && testUsers == null) {
      throw new InvalidConfigException("testUsers", MISSING_REQUIRED);
    }

    for (Map.Entry<String, ServiceUserConfig> entry : testUsers.entrySet()) {
      entry.getValue().validate(entry.getKey());
    }

    if (getArtifactStoreConfig() == null) {
      throw new InvalidConfigException("artifactStoreConfig", MISSING_REQUIRED);
    }
    getArtifactStoreConfig().validate("artifactStoreConfig");

    if (migrations != null) {
      for (MigrationConfig migrationConfig : migrations) {
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

  public FutureJdbi getJdbi() {
    if (this.jdbi == null) {
      // Initialize HikariCP and jdbi
      final var databaseConfig = config.getDatabase();
      this.jdbi = initializeFutureJdbi(databaseConfig, "modeldb-test");
    }
    return this.jdbi;
  }
}
