package ai.verta.modeldb.config;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.exceptions.InternalErrorException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static ai.verta.modeldb.utils.ModelDBUtils.appendOptionalTelepresencePath;

public class Config {
  public static String MISSING_REQUIRED = "required field is missing";

  private static Config config = null;

  public DatabaseConfig database;
  public TestConfig test;

  // FIXME
  public Object artifactStoreConfig;
  public Object artifactStore_grpcServer;
  public Object authService;
  public Object cron_job;
  public Object enableTrace;
  public Object grpcServer;
  public Object mdb_service_user;
  public Object populateConnectionsBasedOnPrivileges;
  public Object springServer;
  public Object telemetry;

  public static Config getInstance() throws InternalErrorException {
    if (config == null) {
      try {
        Yaml yaml = new Yaml(new Constructor(Config.class));
        String filePath = System.getenv(ModelDBConstants.VERTA_MODELDB_CONFIG);
        filePath = appendOptionalTelepresencePath(filePath);
        InputStream inputStream = new FileInputStream(new File(filePath));
        config = yaml.load(inputStream);
        config.Validate();
      } catch (Exception ex) {
        throw new InternalErrorException(ex.getMessage());
      }
    }
    return config;
  }

  public void Validate() throws InvalidConfigException {
    if (database == null) throw new InvalidConfigException("database", MISSING_REQUIRED);
    database.Validate("database");

    if (test != null) {
      test.Validate("test");
    }
  }
}
