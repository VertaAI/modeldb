package ai.verta.modeldb.common.config;

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.ActiveSpanContextSource;
import io.opentracing.contrib.grpc.ActiveSpanSource;
import io.opentracing.contrib.grpc.TracingClientInterceptor;
import io.opentracing.contrib.grpc.TracingServerInterceptor;
import io.opentracing.contrib.jdbc.TracingDriver;
import io.opentracing.util.GlobalTracer;

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
  public TestConfig test;
  public ServiceUserConfig service_user;

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

    if (test != null) {
      test.Validate(this, "test");
    }
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
