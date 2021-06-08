package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.ActiveSpanContextSource;
import io.opentracing.contrib.grpc.ActiveSpanSource;
import io.opentracing.contrib.grpc.TracingClientInterceptor;
import io.opentracing.contrib.grpc.TracingServerInterceptor;
import io.opentracing.contrib.jdbc.TracingDriver;
import io.opentracing.util.GlobalTracer;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class Config {
  public static String MISSING_REQUIRED = "required field is missing";

  public ServiceConfig authService;
  public Map<String, CronJobConfig> cron_job = new HashMap<>();
  public boolean populateConnectionsBasedOnPrivileges = false;
  public DatabaseConfig database;
  public boolean enableTrace = false;
  public GrpcServerConfig grpcServer;
  public SpringServerConfig springServer;
  public ServiceUserConfig service_user;
  public boolean disabled_audits = false;
  public int jdbi_retry_time = 100; // Time in ms

  public static <T> T getInstance(Class<T> configType, String configFile) throws InternalErrorException {
    try {
      Yaml yaml = new Yaml(new Constructor(configType));
      String filePath = System.getenv(configFile);
      filePath = CommonUtils.appendOptionalTelepresencePath(filePath);
      InputStream inputStream = new FileInputStream(filePath);
      return yaml.loadAs(inputStream, configType);
    } catch (ModelDBException ex) {
      throw ex;
    } catch (NullPointerException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new InternalErrorException(ex.getMessage());
    }
  }

  public void Validate() throws InvalidConfigException {

    if (authService != null) {
      authService.Validate("authService");
    }

    if (cron_job != null) {
      for (Map.Entry<String, CronJobConfig> cronJob : cron_job.entrySet()) {
        cronJob.getValue().Validate("cron_job." + cronJob.getKey());
      }
    }

    if (database == null) throw new InvalidConfigException("database", MISSING_REQUIRED);
    database.Validate("database");

    if (grpcServer == null) throw new InvalidConfigException("grpcServer", MISSING_REQUIRED);
    grpcServer.Validate("grpcServer");

    if (springServer == null) throw new InvalidConfigException("springServer", MISSING_REQUIRED);
    springServer.Validate("springServer");
  }

  public boolean hasAuth() {
    return authService != null;
  }

  public abstract boolean hasServiceAccount();

  private TracingServerInterceptor tracingServerInterceptor = null;
  private TracingClientInterceptor tracingClientInterceptor = null;

  private void initializeTracing() {
    if (!enableTrace) return;

    if (tracingServerInterceptor == null) {
      Tracer tracer = Configuration.fromEnv().getTracer();
      tracingServerInterceptor = TracingServerInterceptor.newBuilder().withTracer(tracer).build();
      GlobalTracer.register(tracer);
      TracingDriver.load();
      TracingDriver.setInterceptorMode(true);
      TracingDriver.setInterceptorProperty(true);
    }
    if (tracingClientInterceptor == null) {
      tracingClientInterceptor =
          TracingClientInterceptor.newBuilder()
              .withTracer(GlobalTracer.get())
              .withActiveSpanContextSource(ActiveSpanContextSource.GRPC_CONTEXT)
              .withActiveSpanSource(ActiveSpanSource.GRPC_CONTEXT)
              .build();
    }
  }

  public Optional<TracingServerInterceptor> getTracingServerInterceptor() {
    initializeTracing();
    return Optional.ofNullable(tracingServerInterceptor);
  }

  public Optional<TracingClientInterceptor> getTracingClientInterceptor() {
    initializeTracing();
    return Optional.ofNullable(tracingClientInterceptor);
  }
}
