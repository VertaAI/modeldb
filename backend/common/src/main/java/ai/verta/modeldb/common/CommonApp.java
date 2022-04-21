package ai.verta.modeldb.common;

import ai.verta.modeldb.common.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.common.artifactStore.storageservice.NoopArtifactStoreService;
import ai.verta.modeldb.common.artifactStore.storageservice.nfs.FileStorageProperties;
import ai.verta.modeldb.common.artifactStore.storageservice.nfs.NFSController;
import ai.verta.modeldb.common.artifactStore.storageservice.nfs.NFSService;
import ai.verta.modeldb.common.artifactStore.storageservice.s3.S3Controller;
import ai.verta.modeldb.common.artifactStore.storageservice.s3.S3Service;
import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.exceptions.ExceptionInterceptor;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.health.HealthServiceImpl;
import ai.verta.modeldb.common.health.HealthStatusManager;
import ai.verta.modeldb.common.interceptors.MetadataForwarder;
import ai.verta.modeldb.common.monitoring.MonitoringInterceptor;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.health.v1.HealthCheckResponse;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.jmx.BuildInfoCollector;
import io.prometheus.jmx.JmxCollector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.annotation.PreDestroy;
import javax.management.MalformedObjectNameException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

public abstract class CommonApp implements ApplicationContextAware {
  private static final Logger LOGGER = LogManager.getLogger(CommonApp.class);

  protected ApplicationContext applicationContext;
  private Optional<Server> server = Optional.empty();
  protected Config config;

  // Export all JMX metrics to Prometheus
  private static final String JMX_RULES = "---\n" + "rules:\n" + "  - pattern: \".*\"";
  private static final AtomicBoolean metricsInitialized = new AtomicBoolean(false);

  // metric for prometheus monitoring
  private static final Gauge up =
      Gauge.build()
          .name(System.getProperties().getProperty("BACKEND_SERVICE_NAME") + "_up")
          .help("Binary signal indicating that the service is up and working.")
          .register();

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Configuration
  public class SpringServerCustomizer
      implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
    private final Config config;

    public SpringServerCustomizer(Config config) {
      this.config = config;
    }

    public void customize(ConfigurableServletWebServerFactory factory) {
      factory.setPort(this.config.getSpringServer().getPort());
    }
  }

  /**
   * Shut down the spring boot server
   *
   * @param returnCode : for system exit - 0
   */
  public void initiateShutdown(int returnCode) {
    SpringApplication.exit(this.applicationContext, () -> returnCode);
  }

  @Bean
  public ServletRegistrationBean<MetricsServlet> servletRegistrationBean()
      throws MalformedObjectNameException {
    if (!metricsInitialized.getAndSet(true)) {
      DefaultExports.initialize();
      new BuildInfoCollector().register();
      new JmxCollector(JMX_RULES).register();
    }
    return new ServletRegistrationBean<>(new MetricsServlet(), "/metrics");
  }

  @Bean
  protected abstract Config initConfig();

  @Bean
  public GracefulShutdown gracefulShutdown(Config config) {
    if (config == null || config.getSpringServer().getShutdownTimeout() == null) {
      return new GracefulShutdown(30L);
    }
    return new GracefulShutdown(config.getSpringServer().getShutdownTimeout());
  }

  @Bean
  public ServletWebServerFactory servletContainer(final GracefulShutdown gracefulShutdown) {
    var factory = new TomcatServletWebServerFactory();
    factory.addConnectorCustomizers(gracefulShutdown);
    return factory;
  }

  @Bean
  Executor executor(Config config) {
    // Initialize executor so we don't lose context using Futures
    return FutureGrpc.initializeExecutor(config.getGrpcServer().getThreadCount());
  }

  protected void registeredBean(
      ApplicationContext applicationContext, String controllerBeanName, Class<?> className) {
    AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;

    // Remove nfsController bean if exists
    if (registry.containsBeanDefinition(controllerBeanName)) {
      registry.removeBeanDefinition(controllerBeanName);
    }
    // create nfsController bean based on condition
    GenericBeanDefinition gbd = new GenericBeanDefinition();
    gbd.setBeanClass(className);
    registry.registerBeanDefinition(controllerBeanName, gbd);
  }

  protected static void resolvePortCollisionIfExists(String path) throws Exception {
    File pidFile = new File(path);
    if (pidFile.exists()) {
      try (BufferedReader reader = new BufferedReader(new FileReader(pidFile))) {
        String pidString = reader.readLine();
        var pid = Long.parseLong(pidString);
        var process = ProcessHandle.of(pid);
        if (process.isPresent()) {
          var processHandle = process.get();
          LOGGER.warn("Port is already used by backend PID: {}", pid);
          boolean destroyed = processHandle.destroy();
          LOGGER.warn("Process kill completed `{}` for PID: {}", destroyed, pid);
          processHandle = processHandle.onExit().get();
          LOGGER.warn("Process is alive after kill: `{}`", processHandle.isAlive());
        }
      }
    }
  }

  protected void cleanUpPIDFile() {
    var path = System.getProperty(CommonConstants.USER_DIR) + "/" + CommonConstants.BACKEND_PID;
    File pidFile = new File(path);
    if (pidFile.exists()) {
      pidFile.deleteOnExit();
      LOGGER.trace(CommonConstants.BACKEND_PID + " file is deleted: {}", pidFile.exists());
    }
  }

  @Bean
  protected abstract CommonDBUtil initJdbiUtil() throws SQLException, InterruptedException;

  protected abstract boolean runMigration(
      Config config, CommonDBUtil jdbiUtil, Executor handleExecutor) throws Exception;

  @Bean
  public ArtifactStoreService artifactStoreService(
      ApplicationContext applicationContext, final Config config) throws IOException {

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
          if (!artifactStoreConfig.getS3().getS3presignedURLEnabled()) {
            registeredBean(applicationContext, "s3Controller", S3Controller.class);
          }
          artifactStoreService =
              new S3Service(artifactStoreConfig, artifactStoreConfig.getS3().getCloudBucketName());
          break;
        case "NFS":
          registeredBean(applicationContext, "nfsController", NFSController.class);
          String rootDir = artifactStoreConfig.getNFS().getNfsRootPath();
          LOGGER.trace("NFS server root path {}", rootDir);
          final var props = new FileStorageProperties(rootDir);
          artifactStoreService = new NFSService(props, artifactStoreConfig);
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
      return new NoopArtifactStoreService();
    }
  }

  @Bean
  protected abstract CommonServiceSet initServiceSet(
      Config config, ArtifactStoreService artifactStoreService, Executor handleExecutor)
      throws IOException;

  @Bean
  protected abstract CommonDAOSet initDAOSet(
      Config config, CommonServiceSet serviceSet, Executor handleExecutor);

  protected abstract void initReconcilers(
      Config config, CommonServiceSet services, CommonDAOSet daos, Executor handleExecutor);

  @Bean
  protected HealthServiceImpl healthService(CommonDBUtil commonDBUtil) {
    return new HealthServiceImpl(commonDBUtil);
  }

  @Bean
  protected CommandLineRunner commandLineRunner(ApplicationContext ctx) {
    return args -> {
      try {
        LOGGER.info("Backend server starting.");
        final var logger =
            java.util.logging.Logger.getLogger("io.grpc.netty.NettyServerTransport.connections");
        logger.setLevel(Level.WARNING);

        final var config = ctx.getBean(Config.class);

        final var jdbiUtil = ctx.getBean(CommonDBUtil.class);

        // Initialize executor so we don't lose context using Futures
        final var handleExecutor = ctx.getBean(Executor.class);

        // Initialize database configuration and maybe run migration
        if (runMigration(config, jdbiUtil, handleExecutor)) {
          LOGGER.info("Migrations have completed.  System exiting.");
          initiateShutdown(0);
          return;
        }

        LOGGER.info("Migration disabled, starting server.");
        final var services = ctx.getBean(CommonServiceSet.class);
        final var daos = ctx.getBean(CommonDAOSet.class);

        // Initialize reconciler or cron jobs
        initReconcilers(config, services, daos, handleExecutor);

        // Initialize service specific things like Telemetry service etc.
        initServiceSpecificThings(config);

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
        serverBuilder.intercept(new AuthInterceptor());
        serverBuilder.intercept(new MonitoringInterceptor());

        // Add APIs
        LOGGER.info("Initializing backend services.");
        initGRPCServices(serverBuilder, services, daos, handleExecutor);

        // Create the server
        final var server = serverBuilder.build();

        // --------------- Start modelDB gRPC server --------------------------
        server.start();
        this.server = Optional.of(server);
        healthStatusManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING);
        up.inc();
        LOGGER.info("Current PID: {}", ProcessHandle.current().pid());
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

  protected abstract void initServiceSpecificThings(Config config) throws Exception;

  protected abstract void initGRPCServices(
      ServerBuilder<?> serverBuilder,
      CommonServiceSet services,
      CommonDAOSet daos,
      Executor handleExecutor);

  protected void wrapService(ServerBuilder<?> serverBuilder, BindableService bindableService) {
    serverBuilder.addService(bindableService);
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

        long pollInterval = 5L;
        long timeoutRemaining = config.getGrpcServer().getRequestTimeout();
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

    cleanUpPIDFile();
  }
}
