package ai.verta.modeldb.common.config;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalJdbi;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.jaeger.thrift.JaegerThriftSpanExporter;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import io.opentelemetry.instrumentation.resources.ContainerResource;
import io.opentelemetry.instrumentation.resources.HostResource;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentracing.Tracer;
import io.opentracing.contrib.jdbc.TracingDriver;
import io.opentracing.util.GlobalTracer;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.*;
import org.jdbi.v3.core.Jdbi;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter(AccessLevel.NONE)
@SuppressWarnings({"squid:S116", "squid:S100"})
public abstract class Config {
  @JsonProperty private ServiceConfig authService;
  @JsonProperty private Map<String, CronJobConfig> cron_job = new HashMap<>();
  @JsonProperty private boolean populateConnectionsBasedOnPrivileges = false;
  @JsonProperty private DatabaseConfig database;
  @JsonProperty private boolean enableTrace = false;
  @JsonProperty private GrpcServerConfig grpcServer;
  @JsonProperty private SpringServerConfig springServer;
  @JsonProperty private ServiceUserConfig service_user;
  @JsonProperty private int jdbi_retry_time = 100; // Time in ms
  @JsonProperty private boolean event_system_enabled = false;
  @JsonProperty private ServerInterceptor tracingServerInterceptor = null;
  @JsonProperty private ClientInterceptor tracingClientInterceptor = null;
  @JsonProperty private volatile OpenTelemetry openTelemetry;
  @JsonProperty private ArtifactStoreConfig artifactStoreConfig;

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

  // todo: move all tracing related things to a class that exposes spring @Beans, rather than doing
  // all of this here.
  private void initializeTracingInterceptors() {
    if (!enableTrace) {
      return;
    }

    OpenTelemetry openTelemetry = getOpenTelemetry();
    GrpcTelemetry grpcTelemetry = GrpcTelemetry.create(openTelemetry);
    if (tracingServerInterceptor == null) {
      tracingServerInterceptor = grpcTelemetry.newServerInterceptor();
    }
    if (tracingClientInterceptor == null) {
      tracingClientInterceptor = grpcTelemetry.newClientInterceptor();
    }
  }

  private void initializeOpenTracingShim(OpenTelemetry openTelemetry) {
    Tracer tracerShim = OpenTracingShim.createTracerShim(openTelemetry);
    GlobalTracer.registerIfAbsent(tracerShim);
    TracingDriver.load();
    TracingDriver.setInterceptorMode(true);
    TracingDriver.setInterceptorProperty(true);
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
    OpenTelemetrySdk openTelemetry =
        OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(
                ContextPropagators.create(
                    TextMapPropagator.composite(
                        W3CTraceContextPropagator.getInstance(), JaegerPropagator.getInstance())))
            .buildAndRegisterGlobal();
    initializeOpenTracingShim(openTelemetry);
    return openTelemetry;
  }

  /**
   * Override this method to provide a custom sampler implementation for spans.
   *
   * <p>todo: figure out how to do this without requiring a subclass.
   */
  protected Sampler createOpenTelemetrySampler() {
    return Sampler.alwaysOn();
  }

  public Optional<ServerInterceptor> getTracingServerInterceptor() {
    initializeTracingInterceptors();
    return Optional.ofNullable(tracingServerInterceptor);
  }

  public Optional<ClientInterceptor> getTracingClientInterceptor() {
    initializeTracingInterceptors();
    return Optional.ofNullable(tracingClientInterceptor);
  }

  public FutureJdbi initializeFutureJdbi(DatabaseConfig databaseConfig, String poolName) {
    final var jdbi = initializeJdbi(databaseConfig, poolName);
    final var dbExecutor = FutureExecutor.initializeExecutor(databaseConfig.getThreadCount());
    // wrap the executor in the OpenTelemetry context wrapper to make sure the context propagates
    // into any jdbi threads.
    return new FutureJdbi(jdbi, dbExecutor);
  }

  public InternalJdbi initializeJdbi(DatabaseConfig databaseConfig, String poolName) {
    initializeTracingInterceptors();
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

  public OpenTelemetry getOpenTelemetry() {
    if (openTelemetry == null) {
      openTelemetry = initializeOpenTelemetry();
    }
    return openTelemetry;
  }

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
}
