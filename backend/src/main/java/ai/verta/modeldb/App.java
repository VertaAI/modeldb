package ai.verta.modeldb;

import ai.verta.modeldb.advancedService.AdvancedServiceImpl;
import ai.verta.modeldb.artifactStore.storageservice.nfs.FileStorageProperties;
import ai.verta.modeldb.artifactStore.storageservice.s3.S3Service;
import ai.verta.modeldb.comment.CommentServiceImpl;
import ai.verta.modeldb.common.GracefulShutdown;
import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.common.exceptions.ExceptionInterceptor;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.reconcilers.ReconcilerUtils;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.cron_jobs.CronJobUtils;
import ai.verta.modeldb.dataset.DatasetServiceImpl;
import ai.verta.modeldb.datasetVersion.DatasetVersionServiceImpl;
import ai.verta.modeldb.experiment.ExperimentServiceImpl;
import ai.verta.modeldb.experimentRun.ExperimentRunServiceImpl;
import ai.verta.modeldb.health.HealthServiceImpl;
import ai.verta.modeldb.health.HealthStatusManager;
import ai.verta.modeldb.lineage.LineageServiceImpl;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.monitoring.MonitoringInterceptor;
import ai.verta.modeldb.project.ProjectServiceImpl;
import ai.verta.modeldb.telemetry.TelemetryCron;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.VersioningServiceImpl;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.health.v1.HealthCheckResponse;
import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.TracingServerInterceptor;
import io.opentracing.contrib.jdbc.TracingDriver;
import io.opentracing.util.GlobalTracer;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import liquibase.exception.LiquibaseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** This class is entry point of modeldb server. */
@SpringBootApplication
@EnableAutoConfiguration
@EnableConfigurationProperties({FileStorageProperties.class})
// Remove bracket () code if in future define any @component outside of the defined basePackages.
@ComponentScan(basePackages = "${scan.packages}, ai.verta.modeldb.health")
public class App implements ApplicationContextAware {
  public ApplicationContext applicationContext;

  /**
   * Shut down the spring boot server
   *
   * @param returnCode : for system exit - 0
   */
  public static void initiateShutdown(int returnCode) {
    App app = App.getInstance();
    SpringApplication.exit(app.applicationContext, () -> returnCode);
  }

  private static final Logger LOGGER = LogManager.getLogger(App.class);

  private static App app = null;

  // metric for prometheus monitoring
  private static final Gauge up =
      Gauge.build()
          .name("verta_backend_up")
          .help("Binary signal indicating that the service is up and working.")
          .register();

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    app.applicationContext = applicationContext;
  }

  public static ApplicationContext getContext() {
    return app.applicationContext;
  }

  @Bean
  public ServletRegistrationBean<MetricsServlet> servletRegistrationBean() {
    DefaultExports.initialize();
    return new ServletRegistrationBean<>(new MetricsServlet(), "/metrics");
  }

  @Bean
  public GracefulShutdown gracefulShutdown() {
    Config config = Config.getInstance();
    return new GracefulShutdown(config.springServer.shutdownTimeout);
  }

  @Bean
  public ServletWebServerFactory servletContainer(final GracefulShutdown gracefulShutdown) {
    TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
    factory.addConnectorCustomizers(gracefulShutdown);
    return factory;
  }

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

  public static boolean migrate(Config config)
      throws SQLException, LiquibaseException, ClassNotFoundException, InterruptedException {
    boolean liquibaseMigration =
        Boolean.parseBoolean(
            Optional.ofNullable(System.getenv(ModelDBConstants.LIQUIBASE_MIGRATION))
                .orElse("false"));
    ModelDBHibernateUtil modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
    if (liquibaseMigration) {
      LOGGER.info("Liquibase migration starting");
      modelDBHibernateUtil.runLiquibaseMigration(config.database);
      LOGGER.info("Liquibase migration done");

      modelDBHibernateUtil.createOrGetSessionFactory(config.database);

      LOGGER.info("Code migration starting");
      modelDBHibernateUtil.runMigration(config);
      LOGGER.info("Code migration done");

      boolean runLiquibaseSeparate =
          Boolean.parseBoolean(
              Optional.ofNullable(System.getenv(ModelDBConstants.RUN_LIQUIBASE_SEPARATE))
                  .orElse("false"));
      if (runLiquibaseSeparate) {
        return true;
      }
    }

    modelDBHibernateUtil.createOrGetSessionFactory(config.database);

    return false;
  }

  public static void main(String[] args) {
    try {
      LOGGER.info("Backend server starting.");
      final java.util.logging.Logger logger =
          java.util.logging.Logger.getLogger("io.grpc.netty.NettyServerTransport.connections");
      logger.setLevel(Level.WARNING);
      // --------------- Start reading properties --------------------------
      Config config = Config.getInstance();

      // Initialize database configuration and maybe run migration
      if (migrate(config)) return;

      // Configure server
      System.getProperties().put("server.port", config.springServer.port);

      // Initialize services that we depend on
      ServiceSet services = ServiceSet.fromConfig(config);

      // Initialize data access
      DAOSet daos = DAOSet.fromServices(services);

      // Initialize telemetry
      initializeTelemetryBasedOnConfig(config);

      // Initialize cron jobs
      CronJobUtils.initializeCronJobs(config, services);
      ReconcilerUtils.initialize(config, services);

      // Initialize grpc server
      ServerBuilder<?> serverBuilder = ServerBuilder.forPort(config.grpcServer.port);

      // Initialize health check
      HealthServiceImpl healthService = getContext().getBean(HealthServiceImpl.class);
      HealthStatusManager healthStatusManager = new HealthStatusManager(healthService);
      serverBuilder.addService(healthStatusManager.getHealthService());

      // Add middleware/interceptors
      if (config.enableTrace) {
        Tracer tracer = Configuration.fromEnv().getTracer();
        TracingServerInterceptor tracingInterceptor =
            TracingServerInterceptor.newBuilder().withTracer(tracer).build();
        GlobalTracer.register(tracer);
        TracingDriver.load();
        TracingDriver.setInterceptorMode(true);
        TracingDriver.setInterceptorProperty(true);
        serverBuilder.intercept(tracingInterceptor);
      }

      serverBuilder.intercept(new ExceptionInterceptor());
      serverBuilder.intercept(new MonitoringInterceptor());
      serverBuilder.intercept(new AuthInterceptor());

      // Add APIs
      initializeBackendServices(serverBuilder, services, daos);

      // Create the server
      Server server = serverBuilder.build();

      // --------------- Start modelDB gRPC server --------------------------
      server.start();
      healthStatusManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING);
      up.inc();
      LOGGER.info("Backend server started listening on {}", config.grpcServer.port);

      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
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
                    System.err.println(
                        "*** Shutting down gRPC server since JVM is shutting down ***");
                    server.shutdown();
                    try {
                      server.awaitTermination();
                    } catch (InterruptedException e) {
                      e.printStackTrace();
                    }
                    System.err.println("*** Server Shutdown ***");
                  }));

      // ----------- Don't exit the main thread. Wait until server is terminated -----------
      server.awaitTermination();
      up.dec();
    } catch (Exception ex) {
      ex.printStackTrace();
      initiateShutdown(0);
      System.exit(0);
    }
  }

  public static void initializeBackendServices(
      ServerBuilder<?> serverBuilder, ServiceSet services, DAOSet daos) {
    wrapService(serverBuilder, new ProjectServiceImpl(services, daos));
    LOGGER.trace("Project serviceImpl initialized");
    wrapService(serverBuilder, new ExperimentServiceImpl(services, daos));
    LOGGER.trace("Experiment serviceImpl initialized");
    wrapService(serverBuilder, new ExperimentRunServiceImpl(services, daos));
    LOGGER.trace("ExperimentRun serviceImpl initialized");
    wrapService(serverBuilder, new CommentServiceImpl(services, daos));
    LOGGER.trace("Comment serviceImpl initialized");
    wrapService(serverBuilder, new DatasetServiceImpl(services, daos));
    LOGGER.trace("Dataset serviceImpl initialized");
    wrapService(serverBuilder, new DatasetVersionServiceImpl(services, daos));
    LOGGER.trace("Dataset Version serviceImpl initialized");
    wrapService(serverBuilder, new AdvancedServiceImpl(services, daos));
    LOGGER.trace("Hydrated serviceImpl initialized");
    wrapService(serverBuilder, new LineageServiceImpl(daos));
    LOGGER.trace("Lineage serviceImpl initialized");

    wrapService(serverBuilder, new VersioningServiceImpl(services, daos, new FileHasher()));
    LOGGER.trace("Versioning serviceImpl initialized");
    wrapService(serverBuilder, new MetadataServiceImpl(daos));
    LOGGER.trace("Metadata serviceImpl initialized");
    LOGGER.info("All services initialized and resolved dependency before server start");
  }

  private static void wrapService(ServerBuilder<?> serverBuilder, BindableService bindableService) {
    serverBuilder.addService(bindableService);
  }

  public static void initializeTelemetryBasedOnConfig(Config config)
      throws FileNotFoundException, InvalidConfigException {
    if (!config.telemetry.opt_out) {
      // creating an instance of task to be scheduled
      TimerTask task = new TelemetryCron(config.telemetry.consumer);
      ModelDBUtils.scheduleTask(
          task, config.telemetry.frequency, config.telemetry.frequency, TimeUnit.HOURS);
      LOGGER.info("Telemetry scheduled successfully");
    } else {
      LOGGER.info("Telemetry opt out by user");
    }
  }
}
