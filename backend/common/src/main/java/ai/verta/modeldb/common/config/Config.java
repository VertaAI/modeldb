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
import io.opentracing.contrib.jdbi3.OpentracingJdbi3Plugin;
import io.opentracing.util.GlobalTracer;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.jdbi.v3.core.Jdbi;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

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

  private Tracer tracer = null;
  private TracingServerInterceptor tracingServerInterceptor = null;
  private TracingClientInterceptor tracingClientInterceptor = null;

  private void initializeTracing() {
    if (!enableTrace) return;

    if (tracingServerInterceptor == null) {
      tracer = Configuration.fromEnv().getTracer();
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

  public FutureJdbi initializeFutureJdbi(DatabaseConfig databaseConfig, String poolName) {
    final Jdbi jdbi = initializeJdbi(databaseConfig, poolName);
    final Executor dbExecutor = FutureGrpc.initializeExecutor(databaseConfig.threadCount);
    return new FutureJdbi(jdbi, dbExecutor);
  }

  public Jdbi initializeJdbi(DatabaseConfig databaseConfig, String poolName) {
    final var hikariDataSource = new HikariDataSource();
    final var dbUrl = RdbConfig.buildDatabaseConnectionString(databaseConfig.RdbConfiguration);
    hikariDataSource.setJdbcUrl(dbUrl);
    hikariDataSource.setUsername(databaseConfig.RdbConfiguration.RdbUsername);
    hikariDataSource.setPassword(databaseConfig.RdbConfiguration.RdbPassword);
    hikariDataSource.setMinimumIdle(Integer.parseInt(databaseConfig.minConnectionPoolSize));
    hikariDataSource.setMaximumPoolSize(Integer.parseInt(databaseConfig.maxConnectionPoolSize));
    hikariDataSource.setRegisterMbeans(true);
    hikariDataSource.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
    hikariDataSource.setPoolName(poolName);

    Jdbi jdbi = Jdbi.create(hikariDataSource);
    if (enableTrace) {
      initializeTracing();
      jdbi = jdbi.installPlugin(new OpentracingJdbi3Plugin(tracer));
    }
    return jdbi;
  }

}
