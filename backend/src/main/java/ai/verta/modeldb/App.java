package ai.verta.modeldb;

import ai.verta.modeldb.advancedService.AdvancedServiceImpl;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAODisabled;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAORdbImpl;
import ai.verta.modeldb.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.artifactStore.storageservice.nfs.FileStorageProperties;
import ai.verta.modeldb.artifactStore.storageservice.nfs.NFSService;
import ai.verta.modeldb.artifactStore.storageservice.s3.S3Service;
import ai.verta.modeldb.audit_log.AuditLogLocalDAO;
import ai.verta.modeldb.audit_log.AuditLogLocalDAODisabled;
import ai.verta.modeldb.audit_log.AuditLogLocalDAORdbImpl;
import ai.verta.modeldb.authservice.*;
import ai.verta.modeldb.comment.CommentDAO;
import ai.verta.modeldb.comment.CommentDAORdbImpl;
import ai.verta.modeldb.comment.CommentServiceImpl;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.config.DatabaseConfig;
import ai.verta.modeldb.config.InvalidConfigException;
import ai.verta.modeldb.cron_jobs.CronJobUtils;
import ai.verta.modeldb.dataset.DatasetDAO;
import ai.verta.modeldb.dataset.DatasetDAORdbImpl;
import ai.verta.modeldb.dataset.DatasetServiceImpl;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAORdbImpl;
import ai.verta.modeldb.datasetVersion.DatasetVersionServiceImpl;
import ai.verta.modeldb.exceptions.ModelDBException;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.experiment.ExperimentDAORdbImpl;
import ai.verta.modeldb.experiment.ExperimentServiceImpl;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAORdbImpl;
import ai.verta.modeldb.experimentRun.ExperimentRunServiceImpl;
import ai.verta.modeldb.health.HealthServiceImpl;
import ai.verta.modeldb.health.HealthStatusManager;
import ai.verta.modeldb.lineage.LineageDAO;
import ai.verta.modeldb.lineage.LineageDAORdbImpl;
import ai.verta.modeldb.lineage.LineageServiceImpl;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.metadata.MetadataDAORdbImpl;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.monitoring.MonitoringInterceptor;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.project.ProjectDAORdbImpl;
import ai.verta.modeldb.project.ProjectServiceImpl;
import ai.verta.modeldb.telemetry.TelemetryCron;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.*;
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
import java.util.Collections;
import java.util.Map;
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
@SuppressWarnings("unchecked")
public class App implements ApplicationContextAware {

  private ApplicationContext applicationContext;

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

  // Over all map of properties
  private Map<String, Object> propertiesMap;

  // project which can be use for deep copying on user login
  private String starterProjectID = null;

  // Control parameter for delayed shutdown
  private Long shutdownTimeout;

  private boolean populateConnectionsBasedOnPrivileges = false;
  private RoleService roleService;

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
    app = App.getInstance();
    return new GracefulShutdown(app.shutdownTimeout);
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

  public static void main(String[] args) {
    try {
      LOGGER.info("Backend server starting.");
      final java.util.logging.Logger logger =
          java.util.logging.Logger.getLogger("io.grpc.netty.NettyServerTransport.connections");
      logger.setLevel(Level.WARNING);
      // --------------- Start reading properties --------------------------
      Map<String, Object> propertiesMap =
          ModelDBUtils.readYamlProperties(System.getenv(ModelDBConstants.VERTA_MODELDB_CONFIG));
      App app = App.getInstance();
      app.propertiesMap = propertiesMap;
      Config config = Config.getInstance();
      // --------------- End reading properties --------------------------

      // Initialize database configuration and maybe run migration
      boolean liquibaseMigration =
          Boolean.parseBoolean(
              Optional.ofNullable(System.getenv(ModelDBConstants.LIQUIBASE_MIGRATION))
                  .orElse("false"));
      if (liquibaseMigration) {
        LOGGER.info("Liquibase migration starting");
        ModelDBHibernateUtil.runLiquibaseMigration(config.database);
        LOGGER.info("Liquibase migration done");

        ModelDBHibernateUtil.createOrGetSessionFactory(config.database);

        LOGGER.info("Code migration starting");
        ModelDBHibernateUtil.runMigration(config.database);
        LOGGER.info("Code migration done");

        boolean runLiquibaseSeparate =
            Boolean.parseBoolean(
                Optional.ofNullable(System.getenv(ModelDBConstants.RUN_LIQUIBASE_SEPARATE))
                    .orElse("false"));
        if (runLiquibaseSeparate) {
          return;
        }
      }

      ModelDBHibernateUtil.createOrGetSessionFactory(config.database);

      // --------------- Start Initialize modelDB gRPC server --------------------------
      ServerBuilder<?> serverBuilder = ServerBuilder.forPort(config.grpcServer.port);

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
      AuthService authService = AuthServiceUtils.FromConfig(config);
      app.roleService = RoleServiceUtils.FromConfig(config, authService);

      HealthStatusManager healthStatusManager = new HealthStatusManager(new HealthServiceImpl());
      serverBuilder.addService(healthStatusManager.getHealthService());
      healthStatusManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING);

      // ----------------- Start Initialize database & modelDB services with DAO ---------
      initializeServicesBaseOnDataBase(
          serverBuilder, config, propertiesMap, authService, app.roleService);
      // ----------------- Finish Initialize database & modelDB services with DAO --------

      serverBuilder.intercept(new MonitoringInterceptor());
      serverBuilder.intercept(new AuthInterceptor());

      Server server = serverBuilder.build();
      // --------------- Finish Initialize modelDB gRPC server --------------------------

      // --------------- Start modelDB gRPC server --------------------------
      server.start();
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

  public static void initializeServicesBaseOnDataBase(
      ServerBuilder<?> serverBuilder,
      Config config,
      Map<String, Object> propertiesMap,
      AuthService authService,
      RoleService roleService)
      throws ModelDBException, IOException, InvalidConfigException {

    App app = App.getInstance();

    app.populateConnectionsBasedOnPrivileges =
        (boolean)
            propertiesMap.getOrDefault(
                ModelDBConstants.POPULATE_CONNECTIONS_BASED_ON_PRIVILEGES, false);

    Map<String, Object> starterProjectDetail =
        (Map<String, Object>) propertiesMap.get(ModelDBConstants.STARTER_PROJECT);
    if (starterProjectDetail != null) {
      app.starterProjectID = (String) starterProjectDetail.get(ModelDBConstants.STARTER_PROJECT_ID);
    }
    // --------------- Start Initialize Cloud Config ---------------------------------------------
    Map<String, Object> springServerMap =
        (Map<String, Object>) propertiesMap.get(ModelDBConstants.SPRING_SERVER);
    if (springServerMap == null) {
      throw new ModelDBException("springServer configuration not found in properties.");
    }

    Integer springServerPort = (Integer) springServerMap.get(ModelDBConstants.PORT);
    LOGGER.trace("spring server port number found");
    System.getProperties().put("server.port", String.valueOf(springServerPort));

    Object object = springServerMap.get(ModelDBConstants.SHUTDOWN_TIMEOUT);
    if (object instanceof Integer) {
      app.shutdownTimeout = ((Integer) object).longValue();
    } else {
      app.shutdownTimeout = ModelDBConstants.DEFAULT_SHUTDOWN_TIMEOUT;
    }

    ArtifactStoreService artifactStoreService = null;
    if (config.artifactStoreConfig.enabled) {
      artifactStoreService = initializeArtifactStore(config);
    } else {
      System.getProperties().put("scan.packages", "dummyPackageName");
      SpringApplication.run(App.class);
    }

    HealthStatusManager healthStatusManager =
        new HealthStatusManager(app.applicationContext.getBean(HealthServiceImpl.class));
    healthStatusManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING);

    initializeRelationalDBServices(serverBuilder, artifactStoreService, authService, roleService);

    initializeTelemetryBasedOnConfig(propertiesMap);

    // Initialize cron jobs
    CronJobUtils.initializeBasedOnConfig(config, authService, roleService, artifactStoreService);
  }

  private static void initializeRelationalDBServices(
      ServerBuilder<?> serverBuilder,
      ArtifactStoreService artifactStoreService,
      AuthService authService,
      RoleService roleService) {

    // --------------- Start Initialize DAO --------------------------
    MetadataDAO metadataDAO = new MetadataDAORdbImpl();
    CommitDAO commitDAO = new CommitDAORdbImpl(authService, roleService);
    RepositoryDAO repositoryDAO =
        new RepositoryDAORdbImpl(authService, roleService, commitDAO, metadataDAO);
    BlobDAO blobDAO = new BlobDAORdbImpl(authService, roleService);

    ExperimentDAO experimentDAO = new ExperimentDAORdbImpl(authService, roleService);
    ExperimentRunDAO experimentRunDAO =
        new ExperimentRunDAORdbImpl(
            authService, roleService, repositoryDAO, commitDAO, blobDAO, metadataDAO);
    ProjectDAO projectDAO =
        new ProjectDAORdbImpl(authService, roleService, experimentDAO, experimentRunDAO);
    ArtifactStoreDAO artifactStoreDAO;
    if (artifactStoreService != null) {
      artifactStoreDAO = new ArtifactStoreDAORdbImpl(artifactStoreService);
    } else {
      artifactStoreDAO = new ArtifactStoreDAODisabled();
    }

    CommentDAO commentDAO = new CommentDAORdbImpl(authService);
    DatasetDAO datasetDAO = new DatasetDAORdbImpl(authService, roleService);
    LineageDAO lineageDAO = new LineageDAORdbImpl();
    DatasetVersionDAO datasetVersionDAO = new DatasetVersionDAORdbImpl(authService, roleService);
    AuditLogLocalDAO auditLogLocalDAO;
    if (authService instanceof PublicAuthServiceUtils) {
      auditLogLocalDAO = new AuditLogLocalDAODisabled();
    } else {
      auditLogLocalDAO = new AuditLogLocalDAORdbImpl();
    }
    LOGGER.info("All DAO initialized");
    // --------------- Finish Initialize DAO --------------------------
    initializeBackendServices(
        serverBuilder,
        projectDAO,
        experimentDAO,
        experimentRunDAO,
        datasetDAO,
        datasetVersionDAO,
        artifactStoreDAO,
        commentDAO,
        lineageDAO,
        metadataDAO,
        repositoryDAO,
        commitDAO,
        blobDAO,
        auditLogLocalDAO,
        authService,
        roleService);
  }

  private static void initializeBackendServices(
      ServerBuilder<?> serverBuilder,
      ProjectDAO projectDAO,
      ExperimentDAO experimentDAO,
      ExperimentRunDAO experimentRunDAO,
      DatasetDAO datasetDAO,
      DatasetVersionDAO datasetVersionDAO,
      ArtifactStoreDAO artifactStoreDAO,
      CommentDAO commentDAO,
      LineageDAO lineageDAO,
      MetadataDAO metadataDAO,
      RepositoryDAO repositoryDAO,
      CommitDAO commitDAO,
      BlobDAO blobDAO,
      AuditLogLocalDAO auditLogLocalDAO,
      AuthService authService,
      RoleService roleService) {
    wrapService(
        serverBuilder,
        new ProjectServiceImpl(
            authService,
            roleService,
            projectDAO,
            experimentRunDAO,
            artifactStoreDAO,
            auditLogLocalDAO));
    LOGGER.trace("Project serviceImpl initialized");
    wrapService(
        serverBuilder,
        new ExperimentServiceImpl(
            authService,
            roleService,
            experimentDAO,
            projectDAO,
            artifactStoreDAO,
            auditLogLocalDAO));
    LOGGER.trace("Experiment serviceImpl initialized");
    wrapService(
        serverBuilder,
        new ExperimentRunServiceImpl(
            authService,
            roleService,
            experimentRunDAO,
            projectDAO,
            experimentDAO,
            artifactStoreDAO,
            datasetVersionDAO,
            repositoryDAO,
            commitDAO,
            auditLogLocalDAO));
    LOGGER.trace("ExperimentRun serviceImpl initialized");
    wrapService(
        serverBuilder,
        new CommentServiceImpl(authService, commentDAO, experimentRunDAO, roleService));
    LOGGER.trace("Comment serviceImpl initialized");
    wrapService(
        serverBuilder,
        new DatasetServiceImpl(
            authService,
            roleService,
            projectDAO,
            experimentDAO,
            experimentRunDAO,
            repositoryDAO,
            commitDAO,
            metadataDAO,
            auditLogLocalDAO));
    LOGGER.trace("Dataset serviceImpl initialized");
    wrapService(
        serverBuilder,
        new DatasetVersionServiceImpl(
            authService,
            roleService,
            repositoryDAO,
            commitDAO,
            blobDAO,
            metadataDAO,
            artifactStoreDAO,
            auditLogLocalDAO));
    LOGGER.trace("Dataset Version serviceImpl initialized");
    wrapService(
        serverBuilder,
        new AdvancedServiceImpl(
            authService,
            roleService,
            projectDAO,
            experimentRunDAO,
            commentDAO,
            experimentDAO,
            artifactStoreDAO,
            datasetDAO,
            datasetVersionDAO));
    LOGGER.trace("Hydrated serviceImpl initialized");
    wrapService(serverBuilder, new LineageServiceImpl(lineageDAO, experimentRunDAO, commitDAO));
    LOGGER.trace("Lineage serviceImpl initialized");

    wrapService(
        serverBuilder,
        new VersioningServiceImpl(
            authService,
            roleService,
            repositoryDAO,
            commitDAO,
            blobDAO,
            projectDAO,
            experimentDAO,
            experimentRunDAO,
            new FileHasher(),
            artifactStoreDAO));
    LOGGER.trace("Versioning serviceImpl initialized");
    wrapService(serverBuilder, new MetadataServiceImpl(metadataDAO));
    LOGGER.trace("Metadata serviceImpl initialized");
    LOGGER.info("All services initialized and resolved dependency before server start");
  }

  private static void wrapService(ServerBuilder<?> serverBuilder, BindableService bindableService) {
    serverBuilder.addService(bindableService);
  }

  private static ArtifactStoreService initializeArtifactStore(Config config)
      throws ModelDBException, IOException {

    App app = App.getInstance();

    // ------------- Start Initialize Cloud storage base on configuration ------------------
    ArtifactStoreService artifactStoreService;

    if (config.artifactStoreConfig.artifactEndpoint != null) {
      System.getProperties()
          .put(
              "artifactEndpoint.storeArtifact",
              config.artifactStoreConfig.artifactEndpoint.storeArtifact);
      System.getProperties()
          .put(
              "artifactEndpoint.getArtifact",
              config.artifactStoreConfig.artifactEndpoint.getArtifact);
    }

    if (config.artifactStoreConfig.NFS != null
        && config.artifactStoreConfig.NFS.artifactEndpoint != null) {
      System.getProperties()
          .put(
              "artifactEndpoint.storeArtifact",
              config.artifactStoreConfig.NFS.artifactEndpoint.storeArtifact);
      System.getProperties()
          .put(
              "artifactEndpoint.getArtifact",
              config.artifactStoreConfig.NFS.artifactEndpoint.getArtifact);
    }

    switch (config.artifactStoreConfig.artifactStoreType) {
      case "S3":
        if (!config.artifactStoreConfig.S3.s3presignedURLEnabled) {
          System.setProperty(
              ModelDBConstants.CLOUD_BUCKET_NAME, config.artifactStoreConfig.S3.cloudBucketName);
          System.getProperties()
              .put("scan.packages", "ai.verta.modeldb.artifactStore.storageservice.s3");
          SpringApplication.run(App.class);
          artifactStoreService = app.applicationContext.getBean(S3Service.class);
        } else {
          artifactStoreService = new S3Service(config.artifactStoreConfig.S3.cloudBucketName);
          System.getProperties().put("scan.packages", "dummyPackageName");
          SpringApplication.run(App.class);
        }
        break;
      case "NFS":
        String rootDir = config.artifactStoreConfig.NFS.nfsRootPath;
        LOGGER.trace("NFS server root path {}", rootDir);

        System.getProperties().put("file.upload-dir", rootDir);
        System.getProperties()
            .put("scan.packages", "ai.verta.modeldb.artifactStore.storageservice.nfs");
        SpringApplication.run(App.class, new String[0]);

        artifactStoreService = app.applicationContext.getBean(NFSService.class);
        break;
      default:
        throw new ModelDBException("Configure valid artifact store name in config.yaml file.");
    }
    // ------------- Finish Initialize Cloud storage base on configuration ------------------

    LOGGER.info(
        "ArtifactStore service initialized and resolved storage dependency before server start");
    return artifactStoreService;
  }

  public static void initializeTelemetryBasedOnConfig(Map<String, Object> propertiesMap)
      throws FileNotFoundException, InvalidConfigException {
    boolean optIn = true;
    int frequency = 1;
    String consumer = null;
    if (propertiesMap.containsKey(ModelDBConstants.TELEMETRY)) {
      Map<String, Object> telemetryMap =
          (Map<String, Object>) propertiesMap.get(ModelDBConstants.TELEMETRY);
      if (telemetryMap != null) {
        optIn = (boolean) telemetryMap.getOrDefault(ModelDBConstants.OPT_IN, true);
        frequency = (int) telemetryMap.getOrDefault(ModelDBConstants.TELEMENTRY_FREQUENCY, 1);
        if (telemetryMap.containsKey(ModelDBConstants.TELEMETRY_CONSUMER)) {
          consumer = (String) telemetryMap.get(ModelDBConstants.TELEMETRY_CONSUMER);
        }
      }
    }

    if (optIn) {
      // creating an instance of task to be scheduled
      TimerTask task = new TelemetryCron(consumer);
      ModelDBUtils.scheduleTask(task, frequency, frequency, TimeUnit.HOURS);
      LOGGER.info("Telemetry scheduled successfully");
    } else {
      LOGGER.info("Telemetry opt out by user");
    }
  }

  public String getStarterProjectID() {
    return starterProjectID;
  }

  public Map<String, Object> getPropertiesMap() {
    return propertiesMap;
  }

  public void setRoleService(RoleService roleService) {
    this.roleService = roleService;
  }

  public RoleService getRoleService() {
    return roleService;
  }

  public boolean isPopulateConnectionsBasedOnPrivileges() {
    return populateConnectionsBasedOnPrivileges;
  }
}
