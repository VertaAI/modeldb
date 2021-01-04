package ai.verta.modeldb.config;

import static ai.verta.modeldb.utils.ModelDBUtils.appendOptionalTelepresencePath;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.exceptions.InternalErrorException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class Config {
  public static String MISSING_REQUIRED = "required field is missing";

  private static Config config = null;

  public ArtifactStoreConfig artifactStoreConfig;
  public ServiceConfig authService;
  public Map<String, CronJobConfig> cron_job = new HashMap<>();
  public DatabaseConfig database;
  public boolean enableTrace = false;
  public GrpcServerConfig grpcServer;
  public ServiceUserConfig mdb_service_user;
  public boolean populateConnectionsBasedOnPrivileges = false;
  public SpringServerConfig springServer;
  public String starterProject;
  public TelemetryConfig telemetry;
  public TestConfig test;
  public TrialConfig trial;

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
    artifactStoreConfig.Validate("database");

    if (authService != null) {
      authService.Validate("authService");
    }

    for (Map.Entry<String, CronJobConfig> cronJob : cron_job.entrySet()) {
      cronJob.getValue().Validate("cron_job." + cronJob.getKey());
    }

    if (database == null) throw new InvalidConfigException("database", MISSING_REQUIRED);
    database.Validate("database");

    if (grpcServer == null) throw new InvalidConfigException("grpcServer", MISSING_REQUIRED);
    grpcServer.Validate("grpcServer");

    if (mdb_service_user != null) {
      mdb_service_user.Validate("mdb_service_user");
    }

    if (springServer == null) throw new InvalidConfigException("springServer", MISSING_REQUIRED);
    springServer.Validate("springServer");

    if (telemetry == null) telemetry = new TelemetryConfig();
    telemetry.Validate("telemetry");

    if (test != null) {
      test.Validate("test");
    }

    if (trial != null) {
      trial.Validate("trial");
    }
  }

  public boolean hasAuth() {
    return authService != null;
  }

  public boolean hasServiceAccount() {
    return mdb_service_user != null;
  }
}
