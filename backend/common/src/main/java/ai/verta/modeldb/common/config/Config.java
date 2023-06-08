package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import lombok.*;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter(AccessLevel.NONE)
public abstract class Config {
  private ServiceConfig authService;
  private Map<String, CronJobConfig> cron_job = new HashMap<>();
  private boolean populateConnectionsBasedOnPrivileges = false;
  private DatabaseConfig database;
  private boolean enableTrace = false;
  private GrpcServerConfig grpcServer;
  private SpringServerConfig springServer;
  private ServiceUserConfig service_user;
  private int jdbi_retry_time = 100; // Time in ms
  private ServerInterceptor tracingServerInterceptor = null;
  private ClientInterceptor tracingClientInterceptor = null;
  private OpenTelemetry openTelemetry;
  private ArtifactStoreConfig artifactStoreConfig;

  private boolean permissionV2Enabled = false;

  public static <T> T getInstance(Class<T> configType, String configFile)
      throws InternalErrorException {
    try {
      var yaml = new Yaml(new Constructor(configType, new LoaderOptions()));
      String filePath = System.getenv(configFile);
      filePath = CommonUtils.appendOptionalTelepresencePath(filePath);
      InputStream inputStream = new FileInputStream(filePath);
      yaml.setBeanAccess(BeanAccess.FIELD);
      return yaml.loadAs(inputStream, configType);
    } catch (ModelDBException | NullPointerException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new InternalErrorException(ex.getMessage());
    }
  }

  /** If you're overriding this method, don't forget to call super.validate(). */
  public void validate() throws InvalidConfigException {
    if (authService != null) {
      authService.validate("authService");
    }

    if (cron_job != null) {
      for (Map.Entry<String, CronJobConfig> cronJob : cron_job.entrySet()) {
        cronJob.getValue().validate("cron_job." + cronJob.getKey());
      }
    }

    if (database == null) {
      throw new InvalidConfigException("database", CommonMessages.MISSING_REQUIRED);
    }
    database.validate("database");

    if (grpcServer == null) {
      throw new InvalidConfigException("grpcServer", CommonMessages.MISSING_REQUIRED);
    }
    grpcServer.validate("grpcServer");

    if (springServer == null) {
      throw new InvalidConfigException("springServer", CommonMessages.MISSING_REQUIRED);
    }
    springServer.validate("springServer");

    if (artifactStoreConfig == null) {
      throw new InvalidConfigException("artifactStoreConfig", CommonMessages.MISSING_REQUIRED);
    }
    artifactStoreConfig.validate("artifactStoreConfig");
  }

  public boolean hasAuth() {
    return authService != null;
  }

  public abstract boolean hasServiceAccount();

  public void setArtifactStoreConfig(ArtifactStoreConfig artifactStoreConfig) {
    this.artifactStoreConfig = artifactStoreConfig;
  }

  public void setSpringServer(SpringServerConfig springServer) {
    this.springServer = springServer;
  }

  public void setGrpcServer(GrpcServerConfig grpcServer) {
    this.grpcServer = grpcServer;
  }

  public boolean tracingEnabled() {
    return enableTrace;
  }

  public void setAuthService(ServiceConfig authService) {
    this.authService = authService;
  }
}
