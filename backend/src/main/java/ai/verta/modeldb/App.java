package ai.verta.modeldb;

import ai.verta.modeldb.advancedService.AdvancedServiceImpl;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAODisabled;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAORdbImpl;
import ai.verta.modeldb.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.artifactStore.storageservice.nfs.FileStorageProperties;
import ai.verta.modeldb.artifactStore.storageservice.nfs.NFSService;
import ai.verta.modeldb.artifactStore.storageservice.s3.S3Service;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.comment.CommentDAO;
import ai.verta.modeldb.comment.CommentDAORdbImpl;
import ai.verta.modeldb.comment.CommentServiceImpl;
import ai.verta.modeldb.cron_jobs.CronJobUtils;
import ai.verta.modeldb.dataset.DatasetDAO;
import ai.verta.modeldb.dataset.DatasetDAORdbImpl;
import ai.verta.modeldb.dataset.DatasetServiceImpl;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAORdbImpl;
import ai.verta.modeldb.datasetVersion.DatasetVersionServiceImpl;
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
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.project.ProjectDAORdbImpl;
import ai.verta.modeldb.project.ProjectServiceImpl;
import ai.verta.modeldb.telemetry.TelemetryCron;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.BlobDAORdbImpl;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.CommitDAORdbImpl;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryDAORdbImpl;
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
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
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

/** This class is entry point of modeldb server. */
@SpringBootApplication
@EnableAutoConfiguration
@EnableConfigurationProperties({FileStorageProperties.class})
// Remove bracket () code if in future define any @component outside of the defined basePackages.
@ComponentScan(basePackages = "${scan.packages}")
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

  // Authentication service
  private String authServerHost = null;
  private Integer authServerPort = null;

  // Service Account details
  private String serviceUserEmail = null;
  private String serviceUserDevKey = null;

  // S3 Artifact store
  private String cloudAccessKey = null;
  private String cloudSecretKey = null;
  private String minioEndpoint = null;
  private String awsRegion = null;

  // Artifact store
  private boolean disabledArtifactStore = false;
  private Boolean pickArtifactStoreHostFromConfig = null;
  private String artifactStoreServerHost = null;
  private String artifactStoreUrlProtocol = null;
  private String storeArtifactEndpoint = null;
  private String getArtifactEndpoint = null;
  private String storeTypePathPrefix = null;
  private boolean s3presignedURLEnabled = true;

  // Database connection details
  private Map<String, Object> databasePropMap;

  // Control parameter for delayed shutdown
  private Long shutdownTimeout;

  // Feature flags
  private Boolean disabledAuthz = false;
  private Boolean publicSharingEnabled = false;
  private Boolean storeClientCreationTimestamp = false;
  private Integer requestTimeout = 30;

  private Boolean traceEnabled = false;
  private static TracingServerInterceptor tracingInterceptor;
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
  public S3Service getS3Service() throws ModelDBException {
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
      // --------------- End reading properties --------------------------

      // --------------- Start Initialize modelDB gRPC server --------------------------
      Map<String, Object> grpcServerMap =
          (Map<String, Object>) propertiesMap.get(ModelDBConstants.GRPC_SERVER);
      if (grpcServerMap == null) {
        throw new ModelDBException("grpcServer configuration not found in properties.");
      }

      Integer grpcServerPort = (Integer) grpcServerMap.get(ModelDBConstants.PORT);
      LOGGER.trace("grpc server port number found");
      ServerBuilder<?> serverBuilder = ServerBuilder.forPort(grpcServerPort);

      App app = App.getInstance();
      app.requestTimeout =
          (Integer) grpcServerMap.getOrDefault(ModelDBConstants.REQUEST_TIMEOUT, 30);

      Map<String, Object> featureFlagMap =
          (Map<String, Object>) propertiesMap.get(ModelDBConstants.FEATURE_FLAG);
      if (featureFlagMap != null) {
        app.setDisabledAuthz(
            (Boolean) featureFlagMap.getOrDefault(ModelDBConstants.DISABLED_AUTHZ, false));
        app.storeClientCreationTimestamp =
            (Boolean)
                featureFlagMap.getOrDefault(
                    ModelDBConstants.STORE_CLIENT_CREATION_TIMESTAMP, false);
      }

      if (propertiesMap.containsKey("enableTrace") && (Boolean) propertiesMap.get("enableTrace")) {
        app.traceEnabled = true;
        Tracer tracer = Configuration.fromEnv().getTracer();
        app.tracingInterceptor = TracingServerInterceptor.newBuilder().withTracer(tracer).build();
        GlobalTracer.register(tracer);
        io.opentracing.contrib.jdbc.TracingDriver.load();
        io.opentracing.contrib.jdbc.TracingDriver.setInterceptorMode(true);
        TracingDriver.setInterceptorProperty(true);
      }
      AuthService authService = new PublicAuthServiceUtils();
      app.roleService = new PublicRoleServiceUtils(authService);

      Map<String, Object> authServicePropMap =
          (Map<String, Object>) propertiesMap.get(ModelDBConstants.AUTH_SERVICE);
      if (authServicePropMap != null) {
        String authServiceHost = (String) authServicePropMap.get(ModelDBConstants.HOST);
        Integer authServicePort = (Integer) authServicePropMap.get(ModelDBConstants.PORT);
        app.setAuthServerHost(authServiceHost);
        app.setAuthServerPort(authServicePort);

        authService = new AuthServiceUtils();
        app.roleService = new RoleServiceUtils(authService);
      }

      Map<String, Object> databasePropMap =
          (Map<String, Object>) propertiesMap.get(ModelDBConstants.DATABASE);

      HealthStatusManager healthStatusManager = new HealthStatusManager(new HealthServiceImpl());
      serverBuilder.addService(healthStatusManager.getHealthService());
      healthStatusManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING);

      // ----------------- Start Initialize database & modelDB services with DAO ---------
      initializeServicesBaseOnDataBase(
          serverBuilder, databasePropMap, propertiesMap, authService, app.roleService);
      // ----------------- Finish Initialize database & modelDB services with DAO --------

      serverBuilder.intercept(new ModelDBAuthInterceptor());

      Server server = serverBuilder.build();
      // --------------- Finish Initialize modelDB gRPC server --------------------------

      // --------------- Start modelDB gRPC server --------------------------
      server.start();
      up.inc();
      LOGGER.info("Backend server started listening on {}", grpcServerPort);

      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    int activeRequestCount = ModelDBAuthInterceptor.ACTIVE_REQUEST_COUNT.get();
                    while (activeRequestCount > 0) {
                      activeRequestCount = ModelDBAuthInterceptor.ACTIVE_REQUEST_COUNT.get();
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
      Map<String, Object> databasePropMap,
      Map<String, Object> propertiesMap,
      AuthService authService,
      RoleService roleService)
      throws ModelDBException {

    App app = App.getInstance();
    Map<String, Object> serviceUserDetailMap =
        (Map<String, Object>) propertiesMap.get(ModelDBConstants.MDB_SERVICE_USER);
    if (serviceUserDetailMap != null) {
      if (serviceUserDetailMap.containsKey(ModelDBConstants.EMAIL)) {
        app.serviceUserEmail = (String) serviceUserDetailMap.get(ModelDBConstants.EMAIL);
        LOGGER.trace("service user email found");
      }
      if (serviceUserDetailMap.containsKey(ModelDBConstants.DEV_KEY)) {
        app.serviceUserDevKey = (String) serviceUserDetailMap.get(ModelDBConstants.DEV_KEY);
        LOGGER.trace("service user devKey found");
      }
    }

    app.populateConnectionsBasedOnPrivileges =
        (boolean)
            propertiesMap.getOrDefault(
                ModelDBConstants.POPULATE_CONNECTIONS_BASED_ON_PRIVILEGES, false);

    Map<String, Object> featureFlagMap =
        (Map<String, Object>) propertiesMap.get(ModelDBConstants.FEATURE_FLAG);
    if (featureFlagMap != null) {
      app.setDisabledAuthz(
          (Boolean) featureFlagMap.getOrDefault(ModelDBConstants.DISABLED_AUTHZ, false));
      app.setPublicSharingEnabled(
          (Boolean) featureFlagMap.getOrDefault(ModelDBConstants.PUBLIC_SHARING_ENABLED, false));
      app.disabledArtifactStore =
          (Boolean) featureFlagMap.getOrDefault(ModelDBConstants.DISABLED_ARTIFACT_STORE, false);
    }

    Map<String, Object> starterProjectDetail =
        (Map<String, Object>) propertiesMap.get(ModelDBConstants.STARTER_PROJECT);
    if (starterProjectDetail != null) {
      app.starterProjectID = (String) starterProjectDetail.get(ModelDBConstants.STARTER_PROJECT_ID);
    }
    // --------------- Start Initialize Cloud Config ---------------------------------------------
    ArtifactStoreService artifactStoreService = null;
    if (!app.disabledArtifactStore) {
      artifactStoreService = initializeServicesBaseOnArtifactStoreType(propertiesMap);
    }

    // --------------- Start Initialize Database base on configuration --------------------------
    if (databasePropMap.isEmpty()) {
      throw new ModelDBException("database properties not found in config.");
    }
    LOGGER.trace("Database properties found");

    String dbType = (String) databasePropMap.get(ModelDBConstants.DB_TYPE);
    switch (dbType) {
      case ModelDBConstants.RELATIONAL:

        // --------------- Start Initialize relational Database base on configuration
        // ---------------
        app.databasePropMap = databasePropMap;
        app.propertiesMap = propertiesMap;
        ModelDBHibernateUtil.createOrGetSessionFactory();

        LOGGER.trace("RDBMS configured with server");
        // --------------- Finish Initialize relational Database base on configuration
        // --------------

        // -- Start Initialize relational Service and modelDB services --
        initializeRelationalDBServices(
            serverBuilder, artifactStoreService, authService, roleService);
        // -- Start Initialize relational Service and modelDB services --
        break;
      default:
        throw new ModelDBException(
            "Please enter valid database name (DBType) in config.yaml file.");
    }

    // --------------- Finish Initialize Database base on configuration --------------------------

    initializeTelemetryBasedOnConfig(propertiesMap);

    // Initialize cron jobs
    CronJobUtils.initializeBasedOnConfig(propertiesMap, authService, roleService);
  }

  private static void initializeRelationalDBServices(
      ServerBuilder<?> serverBuilder,
      ArtifactStoreService artifactStoreService,
      AuthService authService,
      RoleService roleService) {

    // --------------- Start Initialize DAO --------------------------
    CommitDAO commitDAO = new CommitDAORdbImpl(authService, roleService);
    RepositoryDAO repositoryDAO = new RepositoryDAORdbImpl(authService, roleService);
    BlobDAO blobDAO = new BlobDAORdbImpl(authService, roleService);
    MetadataDAO metadataDAO = new MetadataDAORdbImpl();

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
      AuthService authService,
      RoleService roleService) {
    App app = App.getInstance();
    wrapService(
        serverBuilder,
        new ProjectServiceImpl(
            authService, roleService, projectDAO, experimentRunDAO, artifactStoreDAO));
    LOGGER.trace("Project serviceImpl initialized");
    wrapService(
        serverBuilder,
        new ExperimentServiceImpl(
            authService, roleService, experimentDAO, projectDAO, artifactStoreDAO));
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
            commitDAO));
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
            datasetDAO,
            datasetVersionDAO,
            projectDAO,
            experimentDAO,
            experimentRunDAO,
            repositoryDAO,
            commitDAO,
            metadataDAO));
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
            artifactStoreDAO));
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
            new ModelDBAuthInterceptor(),
            new FileHasher(),
            artifactStoreDAO));
    LOGGER.trace("Versioning serviceImpl initialized");
    wrapService(serverBuilder, new MetadataServiceImpl(metadataDAO));
    LOGGER.trace("Metadata serviceImpl initialized");
    LOGGER.info("All services initialized and resolved dependency before server start");
  }

  private static void wrapService(ServerBuilder<?> serverBuilder, BindableService bindableService) {
    App app = App.getInstance();
    if (app.traceEnabled)
      serverBuilder.addService(app.tracingInterceptor.intercept(bindableService));
    else serverBuilder.addService(bindableService);
  }

  private static ArtifactStoreService initializeServicesBaseOnArtifactStoreType(
      Map<String, Object> propertiesMap) throws ModelDBException {

    Map<String, Object> springServerMap =
        (Map<String, Object>) propertiesMap.get(ModelDBConstants.SPRING_SERVER);
    if (springServerMap == null) {
      throw new ModelDBException("springServer configuration not found in properties.");
    }

    Integer springServerPort = (Integer) springServerMap.get(ModelDBConstants.PORT);
    LOGGER.trace("spring server port number found");
    System.getProperties().put("server.port", String.valueOf(springServerPort));

    Map<String, Object> artifactStoreConfigMap =
        (Map<String, Object>) propertiesMap.get(ModelDBConstants.ARTIFACT_STORE_CONFIG);

    String artifactStoreType =
        (String) artifactStoreConfigMap.get(ModelDBConstants.ARTIFACT_STORE_TYPE);

    // ------------- Start Initialize Cloud storage base on configuration ------------------
    ArtifactStoreService artifactStoreService;
    App app = App.getInstance();

    Object object = springServerMap.get(ModelDBConstants.SHUTDOWN_TIMEOUT);
    if (object instanceof Integer) {
      app.shutdownTimeout = ((Integer) object).longValue();
    } else {
      app.shutdownTimeout = ModelDBConstants.DEFAULT_SHUTDOWN_TIMEOUT;
    }

    app.pickArtifactStoreHostFromConfig =
        (Boolean)
            artifactStoreConfigMap.getOrDefault(
                ModelDBConstants.PICK_ARTIFACT_STORE_HOST_FROM_CONFIG, false);
    LOGGER.trace(
        "ArtifactStore pick host from config flag : {}", app.pickArtifactStoreHostFromConfig);
    app.artifactStoreServerHost =
        (String)
            artifactStoreConfigMap.getOrDefault(ModelDBConstants.ARTIFACT_STORE_SERVER_HOST, "");
    LOGGER.trace("ArtifactStore server host URL found : {}", app.artifactStoreServerHost);
    app.artifactStoreUrlProtocol =
        (String)
            artifactStoreConfigMap.getOrDefault(
                ModelDBConstants.ARTIFACT_STORE_URL_PROTOCOL, ModelDBConstants.HTTPS_STR);
    LOGGER.debug("ArtifactStore URL protocol found : {}", app.artifactStoreUrlProtocol);

    Map<String, Object> artifactStoreEndpointConfigMap =
        (Map<String, Object>) artifactStoreConfigMap.get(ModelDBConstants.ARTIFACT_ENDPOINT);
    if (artifactStoreEndpointConfigMap != null && !artifactStoreEndpointConfigMap.isEmpty()) {
      app.getArtifactEndpoint =
          (String) artifactStoreEndpointConfigMap.get(ModelDBConstants.GET_ARTIFACT_ENDPOINT);
      LOGGER.trace("ArtifactStore Get artifact endpoint found : {}", app.getArtifactEndpoint);
      app.storeArtifactEndpoint =
          (String) artifactStoreEndpointConfigMap.get(ModelDBConstants.STORE_ARTIFACT_ENDPOINT);
      LOGGER.trace("ArtifactStore Store artifact endpoint found : {}", app.storeArtifactEndpoint);

      System.getProperties().put("artifactEndpoint.storeArtifact", app.storeArtifactEndpoint);
      System.getProperties().put("artifactEndpoint.getArtifact", app.getArtifactEndpoint);
    }
    switch (artifactStoreType) {
      case ModelDBConstants.S3:
        Map<String, Object> s3ConfigMap =
            (Map<String, Object>) artifactStoreConfigMap.get(ModelDBConstants.S3);
        app.cloudAccessKey = (String) s3ConfigMap.get(ModelDBConstants.CLOUD_ACCESS_KEY);
        app.cloudSecretKey = (String) s3ConfigMap.get(ModelDBConstants.CLOUD_SECRET_KEY);
        app.minioEndpoint = (String) s3ConfigMap.get(ModelDBConstants.MINIO_ENDPOINT);
        app.awsRegion =
            (String)
                s3ConfigMap.getOrDefault(
                    ModelDBConstants.AWS_REGION, ModelDBConstants.DEFAULT_AWS_REGION);
        String cloudBucketName = (String) s3ConfigMap.get(ModelDBConstants.CLOUD_BUCKET_NAME);
        app.storeTypePathPrefix = "s3://" + cloudBucketName + ModelDBConstants.PATH_DELIMITER;

        if (s3ConfigMap.containsKey(ModelDBConstants.S3_PRESIGNED_URL_ENABLED)
            && !((boolean) s3ConfigMap.get(ModelDBConstants.S3_PRESIGNED_URL_ENABLED))) {
          app.s3presignedURLEnabled = false;

          System.setProperty(ModelDBConstants.CLOUD_BUCKET_NAME, cloudBucketName);
          System.getProperties()
              .put("scan.packages", "ai.verta.modeldb.artifactStore.storageservice.s3");
          SpringApplication.run(App.class);
          artifactStoreService = app.applicationContext.getBean(S3Service.class);
        } else {
          artifactStoreService = new S3Service(cloudBucketName);
          System.getProperties().put("scan.packages", "dummyPackageName");
          SpringApplication.run(App.class);
        }
        break;
      case ModelDBConstants.NFS:
        Map<String, Object> nfsConfigMap =
            (Map<String, Object>) artifactStoreConfigMap.get(ModelDBConstants.NFS);
        String rootDir = (String) nfsConfigMap.get(ModelDBConstants.NFS_ROOT_PATH);
        LOGGER.trace("NFS server root path {}", rootDir);
        app.storeTypePathPrefix = "nfs://" + rootDir + ModelDBConstants.PATH_DELIMITER;

        if (!artifactStoreConfigMap.containsKey(
            ModelDBConstants.PICK_ARTIFACT_STORE_HOST_FROM_CONFIG)) {
          app.pickArtifactStoreHostFromConfig =
              (Boolean)
                  nfsConfigMap.getOrDefault(ModelDBConstants.PICK_NFS_HOST_FROM_CONFIG, false);
          LOGGER.trace("NFS pick host from config flag : {}", app.pickArtifactStoreHostFromConfig);
        }
        if (!artifactStoreConfigMap.containsKey(ModelDBConstants.ARTIFACT_STORE_SERVER_HOST)) {
          app.artifactStoreServerHost =
              (String) nfsConfigMap.getOrDefault(ModelDBConstants.NFS_SERVER_HOST, "");
          LOGGER.trace("NFS server host URL found : {}", app.artifactStoreServerHost);
        }
        if (!artifactStoreConfigMap.containsKey(ModelDBConstants.ARTIFACT_STORE_URL_PROTOCOL)) {
          app.artifactStoreUrlProtocol =
              (String)
                  nfsConfigMap.getOrDefault(
                      ModelDBConstants.NFS_URL_PROTOCOL, ModelDBConstants.HTTPS_STR);
          LOGGER.debug("NFS URL protocol found : {}", app.artifactStoreUrlProtocol);
        }

        if (artifactStoreEndpointConfigMap == null || artifactStoreEndpointConfigMap.isEmpty()) {
          Map<String, Object> artifactEndpointConfigMap =
              (Map<String, Object>) nfsConfigMap.get(ModelDBConstants.ARTIFACT_ENDPOINT);
          if (artifactEndpointConfigMap != null && !artifactEndpointConfigMap.isEmpty()) {
            app.getArtifactEndpoint =
                (String) artifactEndpointConfigMap.get(ModelDBConstants.GET_ARTIFACT_ENDPOINT);
            LOGGER.trace("Get artifact endpoint found : {}", app.getArtifactEndpoint);
            app.storeArtifactEndpoint =
                (String) artifactEndpointConfigMap.get(ModelDBConstants.STORE_ARTIFACT_ENDPOINT);
            LOGGER.trace("Store artifact endpoint found : {}", app.storeArtifactEndpoint);
          }
          System.getProperties().put("artifactEndpoint.storeArtifact", app.storeArtifactEndpoint);
          System.getProperties().put("artifactEndpoint.getArtifact", app.getArtifactEndpoint);
        }
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

  public static void initializeTelemetryBasedOnConfig(Map<String, Object> propertiesMap) {
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

  public String getAuthServerHost() {
    return authServerHost;
  }

  public void setAuthServerHost(String authServerHost) {
    this.authServerHost = authServerHost;
  }

  public Integer getAuthServerPort() {
    return authServerPort;
  }

  public void setAuthServerPort(Integer authServerPort) {
    this.authServerPort = authServerPort;
  }

  public boolean isS3presignedURLEnabled() {
    return s3presignedURLEnabled;
  }

  public Boolean getPickArtifactStoreHostFromConfig() {
    return pickArtifactStoreHostFromConfig;
  }

  public String getArtifactStoreServerHost() {
    return artifactStoreServerHost;
  }

  public String getArtifactStoreUrlProtocol() {
    return artifactStoreUrlProtocol;
  }

  public String getStoreArtifactEndpoint() {
    return storeArtifactEndpoint;
  }

  public String getGetArtifactEndpoint() {
    return getArtifactEndpoint;
  }

  public String getStoreTypePathPrefix() {
    return storeTypePathPrefix;
  }

  public Map<String, Object> getDatabasePropMap() {
    return databasePropMap;
  }

  public Map<String, Object> getPropertiesMap() {
    return propertiesMap;
  }

  public Boolean getDisabledAuthz() {
    return disabledAuthz;
  }

  public void setDisabledAuthz(Boolean disabledAuthz) {
    this.disabledAuthz = disabledAuthz;
  }

  public Boolean getPublicSharingEnabled() {
    return publicSharingEnabled;
  }

  public void setPublicSharingEnabled(Boolean publicSharingEnabled) {
    this.publicSharingEnabled = publicSharingEnabled;
  }

  public String getCloudAccessKey() {
    return cloudAccessKey;
  }

  public String getCloudSecretKey() {
    return cloudSecretKey;
  }

  public String getMinioEndpoint() {
    return minioEndpoint;
  }

  public String getAwsRegion() {
    return awsRegion;
  }

  public Boolean getStoreClientCreationTimestamp() {
    return storeClientCreationTimestamp;
  }

  public String getServiceUserEmail() {
    return serviceUserEmail;
  }

  public String getServiceUserDevKey() {
    return serviceUserDevKey;
  }

  public Boolean getTraceEnabled() {
    return traceEnabled;
  }

  public Integer getRequestTimeout() {
    return requestTimeout;
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
