package ai.verta.modeldb.config;

import static ai.verta.modeldb.utils.ModelDBUtils.appendOptionalTelepresencePath;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.exceptions.InternalErrorException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class Config {
  public static String MISSING_REQUIRED = "required field is missing";

  private static Config config = null;

  public ArtifactStoreConfig artifactStoreConfig;
  public ServiceConfig authService;
  public DatabaseConfig database;
  public boolean enableTrace = false;
  public GrpcServerConfig grpcServer;
  public TestConfig test;

  // FIXME

  public Object artifactStore_grpcServer;

  public Object cron_job;

  public Object mdb_service_user;
  public Object populateConnectionsBasedOnPrivileges;
  public Object springServer;
  public Object telemetry;
  public Object starterProject;
  public Object migration;
  public Object trial;
  public Object feature_flag;

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
    if (artifactStoreConfig == null)
      throw new InvalidConfigException("artifactStoreConfig", MISSING_REQUIRED);
    artifactStoreConfig.Validate("artifactStoreConfig");

    if (authService != null) {
      authService.Validate("authService");
    }

    if (database == null) throw new InvalidConfigException("database", MISSING_REQUIRED);
    database.Validate("database");

    if (grpcServer == null) throw new InvalidConfigException("grpcServer", MISSING_REQUIRED);
    grpcServer.Validate("grpcServer");

    if (test != null) {
      test.Validate("test");
    }
  }

  public boolean hasAuth() {
    return authService != null;
  }
}
