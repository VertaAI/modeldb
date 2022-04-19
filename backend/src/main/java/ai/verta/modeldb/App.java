package ai.verta.modeldb;

import ai.verta.modeldb.advancedService.AdvancedServiceImpl;
import ai.verta.modeldb.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.artifactStore.storageservice.nfs.FileStorageProperties;
import ai.verta.modeldb.artifactStore.storageservice.nfs.NFSController;
import ai.verta.modeldb.artifactStore.storageservice.nfs.NFSService;
import ai.verta.modeldb.artifactStore.storageservice.s3.S3Controller;
import ai.verta.modeldb.artifactStore.storageservice.s3.S3Service;
import ai.verta.modeldb.comment.CommentServiceImpl;
import ai.verta.modeldb.common.CommonApp;
import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.common.exceptions.ExceptionInterceptor;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.interceptors.MetadataForwarder;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.cron_jobs.CronJobUtils;
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
import ai.verta.modeldb.reconcilers.ReconcilerInitializer;
import ai.verta.modeldb.telemetry.TelemetryCron;
import ai.verta.modeldb.utils.JdbiUtil;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.VersioningServiceImpl;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.health.v1.HealthCheckResponse;
import io.prometheus.client.Gauge;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.lang.NonNull;

/** This class is entry point of modeldb server. */
@SpringBootApplication
@EnableAutoConfiguration
@EnableConfigurationProperties({FileStorageProperties.class})
// Remove bracket () code if in future define any @component outside of the defined basePackages.
@ComponentScan(basePackages = "ai.verta.modeldb.config, ai.verta.modeldb.health")
public class App extends CommonApp {
  private static final Logger LOGGER = LogManager.getLogger(App.class);

  private static App app = null;
  public MDBConfig mdbConfig;
  private Optional<Server> server = Optional.empty();

  // metric for prometheus monitoring
  private static final Gauge up =
      Gauge.build()
          .name("verta_backend_up")
          .help("Binary signal indicating that the service is up and working.")
          .register();

  @Bean
  public S3Service getS3Service() throws ModelDBException, IOException {
    String bucketName = System.getProperty(ModelDBConstants.CLOUD_BUCKET_NAME);
    if (bucketName != null && !bucketName.isEmpty()) {
      return new S3Service(System.getProperty(ModelDBConstants.CLOUD_BUCKET_NAME));
    }
    return null;
  }

  public static App getInstance() {
    if (app == null) {
      app = new App();
    }
    return app;
  }

  /**
   * Shut down the spring boot server
   *
   * @param returnCode : for system exit - 0
   */
  public static void initiateShutdown(int returnCode) {
    SpringApplication.exit(App.getInstance().applicationContext, () -> returnCode);
  }

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
    App app = App.getInstance();
    app.applicationContext = applicationContext;
  }

  @Bean
  public MDBConfig config() {
    var config = MDBConfig.getInstance();
    App.getInstance().mdbConfig = config;

    // Configure spring HTTP server
    LOGGER.info("Configuring spring HTTP traffic on port: {}", config.getSpringServer().getPort());
    System.getProperties().put("server.port", config.getSpringServer().getPort());

    return config;
  }

  @Bean
  public JdbiUtil jdbiUtil(MDBConfig config) throws SQLException, InterruptedException {
    return JdbiUtil.getInstance(config);
  }

  @Bean
  ServiceSet serviceSet(MDBConfig config, ArtifactStoreService artifactStoreService)
      throws IOException {
    // Initialize services that we depend on
    return ServiceSet.fromConfig(config, artifactStoreService);
  }

  @Bean
  public DAOSet daoSet(MDBConfig config, ServiceSet services, Executor handleExecutor) {
    if (config.isMigration()) {
      return null;
    }

    // Initialize data access
    return DAOSet.fromServices(services, config.getJdbi(), handleExecutor, config);
  }

  @Bean
  public ArtifactStoreService artifactStoreService(MDBConfig config) throws IOException {

    final var artifactStoreConfig = config.artifactStoreConfig;
    if (artifactStoreConfig.isEnabled()) {
      //
      // TODO: This is backwards, these values can be extracted from the environment or injected
      // using profiles instead
      // ------------- Start Initialize Cloud storage base on configuration ------------------
      ArtifactStoreService artifactStoreService;

      if (artifactStoreConfig.getArtifactEndpoint() != null) {
        System.getProperties()
            .put(
                "artifactEndpoint.storeArtifact",
                artifactStoreConfig.getArtifactEndpoint().getStoreArtifact());
        System.getProperties()
            .put(
                "artifactEndpoint.getArtifact",
                artifactStoreConfig.getArtifactEndpoint().getGetArtifact());
      }

      if (artifactStoreConfig.getArtifactStoreType().equals("NFS")
          && artifactStoreConfig.getNFS() != null
          && artifactStoreConfig.getNFS().getArtifactEndpoint() != null) {
        System.getProperties()
            .put(
                "artifactEndpoint.storeArtifact",
                artifactStoreConfig.getNFS().getArtifactEndpoint().getStoreArtifact());
        System.getProperties()
            .put(
                "artifactEndpoint.getArtifact",
                artifactStoreConfig.getNFS().getArtifactEndpoint().getGetArtifact());
      }

      switch (artifactStoreConfig.getArtifactStoreType()) {
        case "S3":
          if (!artifactStoreConfig.S3.getS3presignedURLEnabled()) {
            registeredBean(app.applicationContext, "s3Controller", S3Controller.class);
          }
          artifactStoreService = new S3Service(artifactStoreConfig.S3.getCloudBucketName());
          break;
        case "NFS":
          registeredBean(app.applicationContext, "nfsController", NFSController.class);
          String rootDir = artifactStoreConfig.getNFS().getNfsRootPath();
          LOGGER.trace("NFS server root path {}", rootDir);
          final var props = new FileStorageProperties();
          props.setUploadDir(rootDir);
          artifactStoreService = new NFSService(props);
          break;
        default:
          throw new ModelDBException("Configure valid artifact store name in config.yaml file.");
      }
      // ------------- Finish Initialize Cloud storage base on configuration ------------------

      LOGGER.info(
          "ArtifactStore service initialized and resolved storage dependency before server start");
      return artifactStoreService;
    } else {
      LOGGER.info("Artifact store service is disabled.");
      return null;
    }
  }

  public static boolean migrate(MDBConfig mdbConfig, JdbiUtil jdbiUtil, Executor handleExecutor)
      throws Exception {
    var liquibaseMigration =
        Boolean.parseBoolean(
            Optional.ofNullable(System.getenv(CommonConstants.LIQUIBASE_MIGRATION))
                .orElse("false"));

    var modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
    modelDBHibernateUtil.initializedConfigAndDatabase(mdbConfig, mdbConfig.getDatabase());

    if (liquibaseMigration) {
      LOGGER.info("Liquibase migration starting");
      jdbiUtil.runLiquibaseMigration();
      LOGGER.info("Liquibase migration done");

      modelDBHibernateUtil.createOrGetSessionFactory(mdbConfig.getDatabase());
      LOGGER.info("Old code migration using hibernate starting");
      modelDBHibernateUtil.runMigration(mdbConfig.getDatabase(), mdbConfig.migrations);
      LOGGER.info("Old code migration using hibernate done");

      LOGGER.info("Code migration starting");
      jdbiUtil.runMigration(handleExecutor);
      LOGGER.info("Code migration done");

      boolean runLiquibaseSeparate =
          Boolean.parseBoolean(
              Optional.ofNullable(System.getenv(CommonConstants.RUN_LIQUIBASE_SEPARATE))
                  .orElse("false"));
      if (runLiquibaseSeparate) {
        return true;
      }
    }

    modelDBHibernateUtil.createOrGetSessionFactory(mdbConfig.getDatabase());
    jdbiUtil.setIsReady();

    return false;
  }

  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }

  @Bean
  public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
    return args -> {
      try {
        LOGGER.info("Backend server starting.");
        final var logger =
            java.util.logging.Logger.getLogger("io.grpc.netty.NettyServerTransport.connections");
        logger.setLevel(Level.WARNING);

        final var config = ctx.getBean(MDBConfig.class);

        // Configure server
        final var services = ctx.getBean(ServiceSet.class);
        final var jdbiUtil = ctx.getBean(JdbiUtil.class);

        // Initialize executor so we don't lose context using Futures
        final var handleExecutor = ctx.getBean(Executor.class);

        // Initialize database configuration and maybe run migration
        if (migrate(config, jdbiUtil, handleExecutor)) {
          LOGGER.info("Migrations have completed.  System exiting.");
          initiateShutdown(0);
          return;
        }

        LOGGER.info("Migration disabled, starting server.");
        final var daos = ctx.getBean(DAOSet.class);

        // Initialize telemetry
        initializeTelemetryBasedOnConfig(config);

        // Initialize cron jobs
        CronJobUtils.initializeCronJobs(config, services);
        ReconcilerInitializer.initialize(config, services, daos, config.getJdbi(), handleExecutor);

        // Initialize grpc server
        ServerBuilder<?> serverBuilder =
            ServerBuilder.forPort(config.getGrpcServer().getPort()).executor(handleExecutor);
        if (config.getGrpcServer().getMaxInboundMessageSize() != null) {
          serverBuilder.maxInboundMessageSize(config.getGrpcServer().getMaxInboundMessageSize());
        }

        // Initialize health check
        HealthServiceImpl healthService = ctx.getBean(HealthServiceImpl.class);
        HealthStatusManager healthStatusManager = new HealthStatusManager(healthService);
        serverBuilder.addService(healthStatusManager.getHealthService());

        // Add middleware/interceptors
        LOGGER.info(
            "Tracing is "
                + (config.getTracingServerInterceptor().isEmpty() ? "disabled" : "enabled"));
        config.getTracingServerInterceptor().map(serverBuilder::intercept);
        serverBuilder.intercept(new MetadataForwarder());
        serverBuilder.intercept(new ExceptionInterceptor());
        serverBuilder.intercept(new MonitoringInterceptor());
        serverBuilder.intercept(new AuthInterceptor());

        // Add APIs
        LOGGER.info("Initializing backend services.");
        initializeBackendServices(serverBuilder, services, daos, handleExecutor);

        // Create the server
        final var server = serverBuilder.build();

        // --------------- Start modelDB gRPC server --------------------------
        server.start();
        this.server = Optional.of(server);
        healthStatusManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING);
        up.inc();
        LOGGER.info("Backend server started listening on {}", config.getGrpcServer().getPort());

        // ----------- Don't exit the main thread. Wait until server is terminated -----------
        server.awaitTermination();
        up.dec();
      } catch (Exception ex) {
        CommonUtils.printStackTrace(LOGGER, ex);
        initiateShutdown(0);
        System.exit(1);
        // Restore interrupted state...
        Thread.currentThread().interrupt();
      }
    };
  }

  public static void initializeBackendServices(
      ServerBuilder<?> serverBuilder, ServiceSet services, DAOSet daos, Executor executor) {
    wrapService(serverBuilder, new FutureProjectServiceImpl(daos, executor));
    LOGGER.trace("Project serviceImpl initialized");
    wrapService(serverBuilder, new FutureExperimentServiceImpl(daos, executor));
    LOGGER.trace("Experiment serviceImpl initialized");
    wrapService(serverBuilder, new FutureExperimentRunServiceImpl(daos, executor));
    LOGGER.trace("ExperimentRun serviceImpl initialized");
    wrapService(serverBuilder, new CommentServiceImpl(services, daos));
    LOGGER.trace("Comment serviceImpl initialized");
    wrapService(serverBuilder, new DatasetServiceImpl(services, daos));
    LOGGER.trace("Dataset serviceImpl initialized");
    wrapService(serverBuilder, new DatasetVersionServiceImpl(services, daos));
    LOGGER.trace("Dataset Version serviceImpl initialized");
    wrapService(serverBuilder, new AdvancedServiceImpl(services, daos, executor));
    LOGGER.trace("Hydrated serviceImpl initialized");
    wrapService(serverBuilder, new LineageServiceImpl(daos));
    LOGGER.trace("Lineage serviceImpl initialized");

    wrapService(serverBuilder, new VersioningServiceImpl(services, daos, new FileHasher()));
    LOGGER.trace("Versioning serviceImpl initialized");
    wrapService(serverBuilder, new MetadataServiceImpl(daos));
    LOGGER.trace("Metadata serviceImpl initialized");
    LOGGER.info("All services initialized and dependencies resolved");
  }

  private static void wrapService(ServerBuilder<?> serverBuilder, BindableService bindableService) {
    serverBuilder.addService(bindableService);
  }

  public static void initializeTelemetryBasedOnConfig(MDBConfig mdbConfig)
      throws FileNotFoundException, InvalidConfigException {
    if (!mdbConfig.telemetry.opt_out) {
      // creating an instance of task to be scheduled
      TimerTask task = new TelemetryCron(mdbConfig.telemetry.consumer);
      ModelDBUtils.scheduleTask(
          task, mdbConfig.telemetry.frequency, mdbConfig.telemetry.frequency, TimeUnit.HOURS);
      LOGGER.info("Telemetry scheduled successfully");
    } else {
      LOGGER.info("Telemetry opt out by user");
    }
  }

  @PreDestroy
  public void shutdown() {
    LOGGER.info("*** Beginning Server Shutdown ***");
    int activeRequestCount = MonitoringInterceptor.ACTIVE_REQUEST_COUNT.get();
    while (activeRequestCount > 0) {
      activeRequestCount = MonitoringInterceptor.ACTIVE_REQUEST_COUNT.get();
      System.err.println("Active Request Count in while: " + activeRequestCount);
      try {
        Thread.sleep(1000); // wait for 1s
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    // Use stderr here since the logger may have been reset by its JVM shutdown
    // hook.
    if (server.isPresent()) {
      LOGGER.info("*** Shutting down gRPC server since JVM is shutting down ***");
      final var s = server.get();
      s.shutdown();
      try {
        s.awaitTermination();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    LOGGER.info("*** Server Shutdown ***");
    up.dec();
  }
}
