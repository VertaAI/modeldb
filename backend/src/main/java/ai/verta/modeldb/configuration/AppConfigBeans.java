package ai.verta.modeldb.configuration;

import ai.verta.modeldb.App;
import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.advancedService.AdvancedServiceImpl;
import ai.verta.modeldb.comment.CommentServiceImpl;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.common.artifactStore.storageservice.nfs.FileStorageProperties;
import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.SpringServerConfig;
import ai.verta.modeldb.common.configuration.AppContext;
import ai.verta.modeldb.common.configuration.EnabledMigration;
import ai.verta.modeldb.common.configuration.RunLiquibaseSeparately;
import ai.verta.modeldb.common.configuration.RunLiquibaseSeparately.RunLiquibaseWithMainService;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.ExceptionInterceptor;
import ai.verta.modeldb.common.futures.FutureUtil;
import ai.verta.modeldb.common.interceptors.MetadataForwarder;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.dataset.DatasetServiceImpl;
import ai.verta.modeldb.datasetVersion.DatasetVersionServiceImpl;
import ai.verta.modeldb.experiment.FutureExperimentServiceImpl;
import ai.verta.modeldb.experimentRun.FutureExperimentRunServiceImpl;
import ai.verta.modeldb.health.HealthServiceImpl;
import ai.verta.modeldb.health.HealthStatusManager;
import ai.verta.modeldb.lineage.LineageServiceImpl;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.monitoring.MonitoringInterceptor;
import ai.verta.modeldb.project.FutureProjectServiceImpl;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.VersioningServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.health.v1.HealthCheckResponse;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.webmvc.SpringWebMvcTelemetry;
import io.prometheus.client.Gauge;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.PreDestroy;
import javax.servlet.Filter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({FileStorageProperties.class})
public class AppConfigBeans {
  private static final Logger LOGGER = LogManager.getLogger(AppConfigBeans.class);
  private final AppContext appContext;
  private Optional<Server> server = Optional.empty();

  // metric for prometheus monitoring
  private static final Gauge up =
      Gauge.build()
          .name("verta_backend_up")
          .help("Binary signal indicating that the service is up and working.")
          .register();

  public AppConfigBeans(AppContext appContext) {
    this.appContext = appContext;
  }

  @Bean
  public Filter webMvcTracingFilter(OpenTelemetry openTelemetry) {
    return SpringWebMvcTelemetry.builder(openTelemetry).build().newServletFilter();
  }

  @Bean
  public OpenTelemetry openTelemetry(Config config) {
    return config.getOpenTelemetry();
  }

  @Bean
  public MDBConfig config() {
    var config = MDBConfig.getInstance();
    App.getInstance().mdbConfig = config;
    initializeSystemProperties(config);
    return config;
  }

  private static void initializeSystemProperties(MDBConfig config) {
    // Configure spring HTTP server
    var webPort = config.getSpringServer().getPort();
    if (webPort == 0) {
      var grpcServerConfig = config.getGrpcServer();
      var grpcServerPort = grpcServerConfig.getPort();
      webPort = grpcServerPort + 1;
    }
    System.getProperties().put("server.port", webPort);
    LOGGER.info("Configuring spring HTTP traffic on port: {}", webPort);
  }

  @Bean
  public SpringServerConfig springServerConfig(MDBConfig config) {
    return config.getSpringServer();
  }

  @Bean
  Executor grpcExecutor(Config config) {
    // Initialize executor so we don't lose context using Futures
    return FutureUtil.initializeExecutor(config.getGrpcServer().getThreadCount());
  }

  @Bean
  UAC uac(Config config) {
    return UAC.FromConfig(config);
  }

  @Bean
  ServiceSet serviceSet(MDBConfig config, ArtifactStoreService artifactStoreService, UAC uac)
      throws IOException {
    // Initialize services that we depend on
    return ServiceSet.fromConfig(config, artifactStoreService, uac);
  }

  @Bean
  @Conditional(RunLiquibaseWithMainService.class)
  public DAOSet daoSet(
      MDBConfig config,
      ServiceSet services,
      Executor grpcExecutor,
      ReconcilerInitializer reconcilerInitializer) {
    return DAOSet.fromServices(
        services, config.getJdbi(), grpcExecutor, config, reconcilerInitializer);
  }

  @Bean
  public HealthStatusManager healthStatusManager(HealthServiceImpl healthService) {
    // Initialize health check
    return new HealthStatusManager(healthService);
  }

  @Bean
  public ServerBuilder<?> serverBuilder(
      MDBConfig config, Executor grpcExecutor, HealthStatusManager healthStatusManager) {
    // Initialize grpc server
    ServerBuilder<?> serverBuilder =
        ServerBuilder.forPort(config.getGrpcServer().getPort()).executor(grpcExecutor);
    if (config.getGrpcServer().getMaxInboundMessageSize() != null) {
      serverBuilder.maxInboundMessageSize(config.getGrpcServer().getMaxInboundMessageSize());
    }

    serverBuilder.addService(healthStatusManager.getHealthService());

    // Add middleware/interceptors
    LOGGER.info(
        "Tracing is " + (config.getTracingServerInterceptor().isEmpty() ? "disabled" : "enabled"));
    config.getTracingServerInterceptor().ifPresent(serverBuilder::intercept);
    serverBuilder.intercept(new MetadataForwarder());
    serverBuilder.intercept(new ExceptionInterceptor());
    serverBuilder.intercept(new MonitoringInterceptor());
    serverBuilder.intercept(new AuthInterceptor());

    return serverBuilder;
  }

  @Bean
  @Conditional(EnabledMigration.class)
  public Migration migration(MDBConfig mdbConfig) throws Exception {
    var migration = new Migration(mdbConfig);
    LOGGER.info("Migrations have completed");
    return migration;
  }

  @Bean
  public ModelDBHibernateUtil hibernateUtil(MDBConfig mdbConfig) {
    // Initialize hibernate session factory
    var modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
    modelDBHibernateUtil.initializedConfigAndDatabase(mdbConfig, mdbConfig.getDatabase());
    modelDBHibernateUtil.createOrGetSessionFactory(mdbConfig.getDatabase());
    return modelDBHibernateUtil;
  }

  @Bean
  public CommandLineRunner commandLineRunner(
      ServerBuilder<?> serverBuilder,
      HealthStatusManager healthStatusManager,
      MDBConfig config,
      ServiceSet services,
      DAOSet daos,
      Executor grpcExecutor) {
    return args -> {
      try {
        LOGGER.info("Backend server starting.");
        final var logger =
            java.util.logging.Logger.getLogger("io.grpc.netty.NettyServerTransport.connections");
        logger.setLevel(Level.WARNING);

        // Add APIs
        LOGGER.info("Initializing backend services.");
        initializeBackendServices(serverBuilder, services, daos, grpcExecutor);

        // Create the server
        final var server = serverBuilder.build();

        // --------------- Start modelDB gRPC server --------------------------
        server.start();
        this.server = Optional.of(server);
        healthStatusManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING);
        up.inc();
        LOGGER.info("Current PID: {}", ProcessHandle.current().pid());
        LOGGER.info("Backend server started listening on {}", config.getGrpcServer().getPort());
      } catch (Exception ex) {
        CommonUtils.printStackTrace(LOGGER, ex);
        appContext.initiateShutdown(0);
        System.exit(1);
        // Restore interrupted state...
        Thread.currentThread().interrupt();
      }
    };
  }

  @Bean
  @Conditional({RunLiquibaseSeparately.class})
  public CommandLineRunner commandLineRunner() {
    return args -> {
      LOGGER.trace("Liquibase run separate: done");
      LOGGER.info("System exiting");
      this.appContext.initiateShutdown(0);
      System.exit(0);
    };
  }

  public void initializeBackendServices(
      ServerBuilder<?> serverBuilder, ServiceSet services, DAOSet daos, Executor grpcExecutor) {
    serverBuilder.addService(new FutureProjectServiceImpl(daos, grpcExecutor));
    LOGGER.trace("Project serviceImpl initialized");
    serverBuilder.addService(new FutureExperimentServiceImpl(daos, grpcExecutor));
    LOGGER.trace("Experiment serviceImpl initialized");
    serverBuilder.addService(new FutureExperimentRunServiceImpl(daos, grpcExecutor));
    LOGGER.trace("ExperimentRun serviceImpl initialized");
    serverBuilder.addService(new CommentServiceImpl(services, daos));
    LOGGER.trace("Comment serviceImpl initialized");
    serverBuilder.addService(new DatasetServiceImpl(services, daos));
    LOGGER.trace("Dataset serviceImpl initialized");
    serverBuilder.addService(new DatasetVersionServiceImpl(services, daos));
    LOGGER.trace("Dataset Version serviceImpl initialized");
    serverBuilder.addService(new AdvancedServiceImpl(services, daos, grpcExecutor));
    LOGGER.trace("Hydrated serviceImpl initialized");
    serverBuilder.addService(new LineageServiceImpl(daos));
    LOGGER.trace("Lineage serviceImpl initialized");

    serverBuilder.addService(new VersioningServiceImpl(services, daos, new FileHasher()));
    LOGGER.trace("Versioning serviceImpl initialized");
    serverBuilder.addService(new MetadataServiceImpl(daos));
    LOGGER.trace("Metadata serviceImpl initialized");
    LOGGER.info("All services initialized and dependencies resolved");
  }

  @PreDestroy
  public void onShutDown() {
    if (this.server.isPresent()) {
      var server = this.server.get();
      try {
        // Use stderr here since the logger may have been reset by its JVM shutdown
        // hook.
        LOGGER.info("*** Shutting down gRPC server since JVM is shutting down ***");
        server.shutdown();

        var mdbConfig = appContext.getApplicationContext().getBean(MDBConfig.class);
        long pollInterval = 5L;
        long timeoutRemaining = mdbConfig.getGrpcServer().getRequestTimeout();
        while (timeoutRemaining > pollInterval
            && !server.awaitTermination(pollInterval, TimeUnit.SECONDS)) {
          int activeRequestCount = MonitoringInterceptor.ACTIVE_REQUEST_COUNT.get();
          LOGGER.info("Active Request Count in while:{} ", activeRequestCount);

          timeoutRemaining -= pollInterval;
        }

        server.awaitTermination(pollInterval, TimeUnit.SECONDS);
        LOGGER.info("*** Server Shutdown ***");
      } catch (InterruptedException e) {
        LOGGER.error("Getting error while graceful shutdown", e);
        // Restore interrupted state...
        Thread.currentThread().interrupt();
      }
    }

    appContext.initiateShutdown(0);

    CommonUtils.cleanUpPIDFile();
  }
}
