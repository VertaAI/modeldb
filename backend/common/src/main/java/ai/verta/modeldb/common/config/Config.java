package ai.verta.modeldb.common.config;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.FutureUtil;
import ai.verta.modeldb.common.futures.InternalJdbi;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.extension.resources.ContainerResource;
import io.opentelemetry.sdk.extension.resources.HostResource;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.ActiveSpanContextSource;
import io.opentracing.contrib.grpc.ActiveSpanSource;
import io.opentracing.contrib.grpc.TracingClientInterceptor;
import io.opentracing.contrib.grpc.TracingServerInterceptor;
import io.opentracing.contrib.jdbc.TracingDriver;
import io.opentracing.util.GlobalTracer;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;

@SuppressWarnings({"squid:S116", "squid:S100"})
public abstract class Config {
  public static final String MISSING_REQUIRED = "required field is missing";

  private ServiceConfig authService;
  private Map<String, CronJobConfig> cron_job = new HashMap<>();
  private boolean populateConnectionsBasedOnPrivileges = false;
  private DatabaseConfig database;
  private boolean enableTrace = false;
  private GrpcServerConfig grpcServer;
  private SpringServerConfig springServer;
  private ServiceUserConfig service_user;
  private int jdbi_retry_time = 100; // Time in ms
  private boolean event_system_enabled = false;
  private TracingServerInterceptor tracingServerInterceptor = null;
  private TracingClientInterceptor tracingClientInterceptor = null;
  private volatile OpenTelemetry openTelemetry;
  private ArtifactStoreConfig artifactStoreConfig;

  public static <T> T getInstance(Class<T> configType, String configFile)
      throws InternalErrorException {
    try {
      var yaml = new Yaml(new Constructor(configType));
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
      throw new InvalidConfigException("database", MISSING_REQUIRED);
    }
    database.validate("database");

    if (grpcServer == null) {
      throw new InvalidConfigException("grpcServer", MISSING_REQUIRED);
    }
    grpcServer.validate("grpcServer");

    if (springServer == null) {
      throw new InvalidConfigException("springServer", MISSING_REQUIRED);
    }
    springServer.validate("springServer");

    if (artifactStoreConfig == null) {
      throw new InvalidConfigException("artifactStoreConfig", MISSING_REQUIRED);
    }
    artifactStoreConfig.validate("artifactStoreConfig");
  }

  public boolean hasAuth() {
    return authService != null;
  }

  public abstract boolean hasServiceAccount();

  // todo: move all tracing related things to a class that exposes spring @Beans, rather than doing
  // all of this here.
  private void initializeTracing() {
    if (!enableTrace) {
      return;
    }

    if (tracingServerInterceptor == null) {
      OpenTelemetry openTelemetry = getOpenTelemetry();
      Tracer tracerShim = OpenTracingShim.createTracerShim(openTelemetry);

      tracingServerInterceptor =
          TracingServerInterceptor.newBuilder().withTracer(tracerShim).build();
      GlobalTracer.registerIfAbsent(tracerShim);
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

  private OpenTelemetry initializeOpenTelemetry() {
    if (!enableTrace) {
      return OpenTelemetry.noop();
    }
    JaegerThriftSpanExporter spanExporter =
        JaegerThriftSpanExporter.builder().setEndpoint(System.getenv("JAEGER_ENDPOINT")).build();
    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .setSampler(createOpenTelemetrySampler())
            .setResource(
                Resource.getDefault()
                    .merge(
                        Resource.create(
                            Attributes.of(
                                stringKey("service.name"),
                                System.getenv("JAEGER_SERVICE_NAME"),
                                stringKey("kubernetes.namespace"),
                                System.getenv("POD_NAMESPACE"))))
                    .merge(HostResource.get())
                    .merge(ContainerResource.get()))
            .build();
    return OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .setPropagators(
            ContextPropagators.create(
                TextMapPropagator.composite(
                    W3CTraceContextPropagator.getInstance(), JaegerPropagator.getInstance())))
        .buildAndRegisterGlobal();
  }

  /**
   * Override this method to provide a custom sampler implementation for spans.
   *
   * <p>todo: figure out how to do this without requiring a subclass.
   */
  protected Sampler createOpenTelemetrySampler() {
    return Sampler.alwaysOn();
  }

  public Optional<TracingServerInterceptor> getTracingServerInterceptor() {
    initializeTracing();
    return Optional.ofNullable(tracingServerInterceptor);
  }

  public Optional<TracingClientInterceptor> getTracingClientInterceptor() {
    initializeTracing();
    return Optional.ofNullable(tracingClientInterceptor);
  }

  public FutureJdbi initializeFutureJdbi(DatabaseConfig databaseConfig, String poolName) {
    final var jdbi = initializeJdbi(databaseConfig, poolName);
    final var dbExecutor = FutureUtil.initializeExecutor(databaseConfig.getThreadCount());
    // wrap the executor in the OpenTelemetry context wrapper to make sure the context propagates
    // into any jdbi threads.
    return new FutureJdbi(jdbi, Context.taskWrapping(dbExecutor));
  }

  public InternalJdbi initializeJdbi(DatabaseConfig databaseConfig, String poolName) {
    initializeTracing();
    final var hikariDataSource = new HikariDataSource();
    final var dbUrl = RdbConfig.buildDatabaseConnectionString(databaseConfig.getRdbConfiguration());
    hikariDataSource.setJdbcUrl(dbUrl);
    hikariDataSource.setUsername(databaseConfig.getRdbConfiguration().getRdbUsername());
    hikariDataSource.setPassword(databaseConfig.getRdbConfiguration().getRdbPassword());
    hikariDataSource.setMinimumIdle(Integer.parseInt(databaseConfig.getMinConnectionPoolSize()));
    hikariDataSource.setMaximumPoolSize(
        Integer.parseInt(databaseConfig.getMaxConnectionPoolSize()));
    hikariDataSource.setRegisterMbeans(true);
    hikariDataSource.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
    hikariDataSource.setPoolName(poolName);
    hikariDataSource.setLeakDetectionThreshold(databaseConfig.getLeakDetectionThresholdMs());

    return new InternalJdbi(Jdbi.create(hikariDataSource));
  }

  public ServiceConfig getAuthService() {
    return authService;
  }

  public Map<String, CronJobConfig> getCron_job() {
    return cron_job;
  }

  public boolean isPopulateConnectionsBasedOnPrivileges() {
    return populateConnectionsBasedOnPrivileges;
  }

  public DatabaseConfig getDatabase() {
    return database;
  }

  public GrpcServerConfig getGrpcServer() {
    return grpcServer;
  }

  public SpringServerConfig getSpringServer() {
    return springServer;
  }

  public ServiceUserConfig getService_user() {
    return service_user;
  }

  public int getJdbi_retry_time() {
    return jdbi_retry_time;
  }

  public boolean isEvent_system_enabled() {
    return event_system_enabled;
  }

  public OpenTelemetry getOpenTelemetry() {
    if (openTelemetry == null) {
      openTelemetry = initializeOpenTelemetry();
    }
    return openTelemetry;
  }

  public ArtifactStoreConfig getArtifactStoreConfig() {
    return artifactStoreConfig;
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
}
