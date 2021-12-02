package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.ActiveSpanContextSource;
import io.opentracing.contrib.grpc.ActiveSpanSource;
import io.opentracing.contrib.grpc.TracingClientInterceptor;
import io.opentracing.contrib.grpc.TracingServerInterceptor;
import io.opentracing.contrib.jdbc.TracingDriver;
import io.opentracing.contrib.jdbi3.OpentracingSqlLogger;
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
      GlobalTracer.registerIfAbsent(tracer);
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

  public FutureJdbi initializeFutureJdbi(DatabaseConfig databaseConfig, String poolName) {
    final var jdbi = initializeJdbi(databaseConfig, poolName);
    final var dbExecutor = FutureGrpc.initializeExecutor(databaseConfig.getThreadCount());
    return new FutureJdbi(jdbi, dbExecutor);
  }

  public Jdbi initializeJdbi(DatabaseConfig databaseConfig, String poolName) {
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

    return Jdbi.create(hikariDataSource).setSqlLogger(new OpentracingSqlLogger(GlobalTracer.get()));
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
}
