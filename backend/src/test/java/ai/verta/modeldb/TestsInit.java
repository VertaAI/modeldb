package ai.verta.modeldb;

import ai.verta.artifactstore.ArtifactStoreGrpc;
import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.exceptions.ExceptionInterceptor;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.interceptors.MetadataForwarder;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.modeldb.cron_jobs.CronJobUtils;
import ai.verta.modeldb.metadata.MetadataServiceGrpc;
import ai.verta.modeldb.monitoring.MonitoringInterceptor;
import ai.verta.modeldb.reconcilers.ReconcilerInitializer;
import ai.verta.modeldb.reconcilers.SoftDeleteExperimentRuns;
import ai.verta.modeldb.reconcilers.SoftDeleteExperiments;
import ai.verta.modeldb.reconcilers.SoftDeleteProjects;
import ai.verta.modeldb.reconcilers.SoftDeleteRepositories;
import ai.verta.modeldb.reconcilers.UpdateExperimentTimestampReconcile;
import ai.verta.modeldb.reconcilers.UpdateProjectTimestampReconcile;
import ai.verta.modeldb.reconcilers.UpdateRepositoryTimestampReconcile;
import ai.verta.modeldb.versioning.VersioningServiceGrpc;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.OrganizationServiceGrpc;
import ai.verta.uac.RoleServiceGrpc;
import ai.verta.uac.UACServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class TestsInit {
  private static final Logger LOGGER = LogManager.getLogger(TestsInit.class);
  private static InProcessServerBuilder serverBuilder;
  protected static AuthClientInterceptor authClientInterceptor;

  protected static TestConfig testConfig;
  protected static AuthService authService;
  protected static Executor handleExecutor;
  protected static ServiceSet services;
  protected static DAOSet daos;

  // all service stubs
  protected static UACServiceGrpc.UACServiceBlockingStub uacServiceStub;
  protected static CollaboratorServiceGrpc.CollaboratorServiceBlockingStub
      collaboratorServiceStubClient1;
  protected static CollaboratorServiceGrpc.CollaboratorServiceBlockingStub
      collaboratorServiceStubClient2;
  protected static ProjectServiceGrpc.ProjectServiceBlockingStub projectServiceStub;
  protected static ProjectServiceGrpc.ProjectServiceBlockingStub client2ProjectServiceStub;
  protected static ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub;
  protected static ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub
      experimentRunServiceStub;
  protected static ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub
      experimentRunServiceStubClient2;
  protected static CommentServiceGrpc.CommentServiceBlockingStub commentServiceBlockingStub;
  protected static OrganizationServiceGrpc.OrganizationServiceBlockingStub
      organizationServiceBlockingStub;
  protected static RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingStub;
  protected static VersioningServiceGrpc.VersioningServiceBlockingStub
      versioningServiceBlockingStub;
  protected static VersioningServiceGrpc.VersioningServiceBlockingStub
      versioningServiceBlockingStubClient2;
  protected static MetadataServiceGrpc.MetadataServiceBlockingStub metadataServiceBlockingStub;
  protected static DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub;
  protected static DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStubClient2;
  protected static DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub
      datasetVersionServiceStub;
  protected static DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub
      datasetVersionServiceStubClient2;
  protected static LineageServiceGrpc.LineageServiceBlockingStub lineageServiceStub;
  protected static HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub;
  protected static HydratedServiceGrpc.HydratedServiceBlockingStub
      hydratedServiceBlockingStubClient2;
  protected static ArtifactStoreGrpc.ArtifactStoreBlockingStub artifactStoreBlockingStub;

  protected static void init() throws Exception {
    String serverName = InProcessServerBuilder.generateName();
    serverBuilder = InProcessServerBuilder.forName(serverName).directExecutor();
    InProcessChannelBuilder client1ChannelBuilder =
        InProcessChannelBuilder.forName(serverName).directExecutor();
    InProcessChannelBuilder client2ChannelBuilder =
        InProcessChannelBuilder.forName(serverName).directExecutor();

    testConfig = TestConfig.getInstance();
    handleExecutor = FutureGrpc.initializeExecutor(testConfig.getGrpcServer().getThreadCount());
    // Initialize services that we depend on
    services = ServiceSet.fromConfig(testConfig, testConfig.artifactStoreConfig);
    authService = services.authService;
    // Initialize data access
    daos = DAOSet.fromServices(services, testConfig.getJdbi(), handleExecutor, testConfig);
    App.migrate(testConfig.getDatabase(), testConfig.migrations);

    App.initializeBackendServices(serverBuilder, services, daos, handleExecutor);
    serverBuilder.intercept(new MetadataForwarder());
    serverBuilder.intercept(new ExceptionInterceptor());
    serverBuilder.intercept(new MonitoringInterceptor());
    serverBuilder.intercept(new AuthInterceptor());
    // Initialize cron jobs
    CronJobUtils.initializeCronJobs(testConfig, services);
    ReconcilerInitializer.initialize(
        testConfig, services, daos, testConfig.getJdbi(), handleExecutor);

    if (testConfig.testUsers != null && !testConfig.testUsers.isEmpty()) {
      authClientInterceptor = new AuthClientInterceptor(testConfig.testUsers);
      client1ChannelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
      client2ChannelBuilder.intercept(authClientInterceptor.getClient2AuthInterceptor());
    }

    if (testConfig.getAuthService() != null) {
      ManagedChannel authServiceChannel =
          ManagedChannelBuilder.forTarget(
                  testConfig.getAuthService().getHost()
                      + ":"
                      + testConfig.getAuthService().getPort())
              .usePlaintext()
              .intercept(authClientInterceptor.getClient1AuthInterceptor())
              .build();
      uacServiceStub = UACServiceGrpc.newBlockingStub(authServiceChannel);
      organizationServiceBlockingStub = OrganizationServiceGrpc.newBlockingStub(authServiceChannel);
      roleServiceBlockingStub = RoleServiceGrpc.newBlockingStub(authServiceChannel);
      collaboratorServiceStubClient1 = CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

      ManagedChannel authServiceChannelClient2 =
          ManagedChannelBuilder.forTarget(
                  testConfig.getAuthService().getHost()
                      + ":"
                      + testConfig.getAuthService().getPort())
              .usePlaintext()
              .intercept(authClientInterceptor.getClient2AuthInterceptor())
              .build();
      collaboratorServiceStubClient2 =
          CollaboratorServiceGrpc.newBlockingStub(authServiceChannelClient2);
    }

    ManagedChannel channel = client1ChannelBuilder.maxInboundMessageSize(1024).build();
    ManagedChannel client2Channel = client2ChannelBuilder.maxInboundMessageSize(1024).build();

    // Create all service blocking stub
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    client2ProjectServiceStub = ProjectServiceGrpc.newBlockingStub(client2Channel);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStubClient2 = ExperimentRunServiceGrpc.newBlockingStub(client2Channel);
    commentServiceBlockingStub = CommentServiceGrpc.newBlockingStub(channel);
    versioningServiceBlockingStub = VersioningServiceGrpc.newBlockingStub(channel);
    versioningServiceBlockingStubClient2 = VersioningServiceGrpc.newBlockingStub(client2Channel);
    metadataServiceBlockingStub = MetadataServiceGrpc.newBlockingStub(channel);
    datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);
    datasetServiceStubClient2 = DatasetServiceGrpc.newBlockingStub(client2Channel);
    datasetVersionServiceStub = DatasetVersionServiceGrpc.newBlockingStub(channel);
    datasetVersionServiceStubClient2 = DatasetVersionServiceGrpc.newBlockingStub(client2Channel);
    lineageServiceStub = LineageServiceGrpc.newBlockingStub(channel);
    artifactStoreBlockingStub = ArtifactStoreGrpc.newBlockingStub(channel);
    hydratedServiceBlockingStub = HydratedServiceGrpc.newBlockingStub(channel);
    hydratedServiceBlockingStubClient2 = HydratedServiceGrpc.newBlockingStub(client2Channel);

    serverBuilder.build().start();
  }

  @BeforeClass
  public static void setServerAndService() throws Exception {
    init();
  }

  @AfterClass
  public static void removeServerAndService() {
    App.initiateShutdown(0);

    // cleanUpResources();

    // shutdown test server
    serverBuilder.build().shutdownNow();
  }

  protected static void cleanUpResources() {
    // Remove all entities
    // removeEntities();
    // Delete entities by cron job
    cleanUpProjects();
    cleanUpExperiments();
    cleanUpExperimentRuns();
    // clean up datasets
    cleanUpRepositories(true);
    // clean up repositories
    cleanUpRepositories(false);
  }

  protected static void updateTimestampOfResources() {
    ReconcilerConfig reconcilerConfig = new ReconcilerConfig(true);
    var repoTimestampReconcile =
        new UpdateRepositoryTimestampReconcile(
            reconcilerConfig, testConfig.getJdbi(), handleExecutor);
    repoTimestampReconcile.reconcile(
        new HashSet<>(repoTimestampReconcile.getEntriesForDateUpdate()));

    var expTimestampReconcile =
        new UpdateExperimentTimestampReconcile(
            reconcilerConfig, testConfig.getJdbi(), handleExecutor);
    expTimestampReconcile.reconcile(
        new HashSet<>(expTimestampReconcile.getEntitiesForDateUpdate()));

    var projTimestampReconcile =
        new UpdateProjectTimestampReconcile(reconcilerConfig, testConfig.getJdbi(), handleExecutor);
    projTimestampReconcile.reconcile(
        new HashSet<>(projTimestampReconcile.getEntitiesForDateUpdate()));
  }

  private static void cleanUpProjects() {
    var queryString =
        "select id from project where deleted=:deleted OR (created=:created AND date_created < :dateCreated) ";

    testConfig
        .getJdbi()
        .withHandle(
            handle ->
                handle
                    .createQuery(queryString)
                    .bind("deleted", true)
                    .bind("created", false)
                    .bind("dateCreated", new Date().getTime() - 60000L) // before 1 min
                    .mapTo(String.class)
                    .list())
        .thenApply(
            deletedProjects -> {
              ReconcilerConfig reconcilerConfig = new ReconcilerConfig(true);
              var softDeleteProjects =
                  new SoftDeleteProjects(
                      reconcilerConfig,
                      services.mdbRoleService,
                      testConfig.getJdbi(),
                      handleExecutor);
              return softDeleteProjects.reconcile(new HashSet<>(deletedProjects));
            },
            handleExecutor)
        .get();
  }

  private static void cleanUpExperiments() {
    var queryString = "select id from experiment where deleted=:deleted ";

    testConfig
        .getJdbi()
        .withHandle(
            handle ->
                handle.createQuery(queryString).bind("deleted", true).mapTo(String.class).list())
        .thenApply(
            deletedExperiments -> {
              ReconcilerConfig reconcilerConfig = new ReconcilerConfig(true);
              var softDeleteExperiments =
                  new SoftDeleteExperiments(
                      reconcilerConfig,
                      services.mdbRoleService,
                      testConfig.getJdbi(),
                      handleExecutor);
              return softDeleteExperiments.reconcile(new HashSet<>(deletedExperiments));
            },
            handleExecutor)
        .get();
  }

  private static void cleanUpExperimentRuns() {
    var queryString = "select id from experiment_run where deleted=:deleted ";

    testConfig
        .getJdbi()
        .withHandle(
            handle ->
                handle.createQuery(queryString).bind("deleted", true).mapTo(String.class).list())
        .thenApply(
            deletedERs -> {
              ReconcilerConfig reconcilerConfig = new ReconcilerConfig(true);
              var softDeleteExperimentRuns =
                  new SoftDeleteExperimentRuns(
                      reconcilerConfig,
                      services.mdbRoleService,
                      testConfig.getJdbi(),
                      handleExecutor);
              return softDeleteExperimentRuns.reconcile(new HashSet<>(deletedERs));
            },
            handleExecutor)
        .get();
  }

  private static void cleanUpRepositories(boolean isDataset) {
    String queryString;
    if (isDataset) {
      queryString =
          "select rp.id from repository rp where rp.deleted=:deleted AND rp.repository_access_modifier = 2";
    } else {
      queryString =
          "select rp.id from repository rp where rp.deleted=:deleted AND rp.repository_access_modifier = 1";
    }

    testConfig
        .getJdbi()
        .withHandle(
            handle ->
                handle.createQuery(queryString).bind("deleted", true).mapTo(String.class).list())
        .thenApply(
            deletedRepositories -> {
              ReconcilerConfig reconcilerConfig = new ReconcilerConfig(true);
              var softDeleteRepositories =
                  new SoftDeleteRepositories(
                      reconcilerConfig,
                      services.mdbRoleService,
                      isDataset,
                      testConfig.getJdbi(),
                      handleExecutor);
              return softDeleteRepositories.reconcile(new HashSet<>(deletedRepositories));
            },
            handleExecutor)
        .get();
  }
}
