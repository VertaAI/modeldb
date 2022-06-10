package ai.verta.modeldb;

import ai.verta.artifactstore.ArtifactStoreGrpc;
import ai.verta.modeldb.DatasetServiceGrpc.DatasetServiceBlockingStub;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.artifactStore.storageservice.nfs.FileStorageProperties;
import ai.verta.modeldb.artifactStore.storageservice.nfs.NFSService;
import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.configuration.AppContext;
import ai.verta.modeldb.common.exceptions.ExceptionInterceptor;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.interceptors.MetadataForwarder;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.modeldb.configuration.AppConfigBeans;
import ai.verta.modeldb.configuration.CronJobUtils;
import ai.verta.modeldb.configuration.Migration;
import ai.verta.modeldb.configuration.ReconcilerInitializer;
import ai.verta.modeldb.metadata.MetadataServiceGrpc;
import ai.verta.modeldb.monitoring.MonitoringInterceptor;
import ai.verta.modeldb.reconcilers.SoftDeleteExperimentRuns;
import ai.verta.modeldb.reconcilers.SoftDeleteExperiments;
import ai.verta.modeldb.reconcilers.SoftDeleteProjects;
import ai.verta.modeldb.versioning.VersioningServiceGrpc;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.OrganizationServiceGrpc;
import ai.verta.uac.RoleServiceGrpc;
import ai.verta.uac.UACServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
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
  protected static ProjectServiceBlockingStub serviceUserProjectServiceStub;
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
  protected static DatasetServiceBlockingStub serviceUserDatasetServiceStub;
  protected static DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub
      datasetVersionServiceStub;
  protected static DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub
      datasetVersionServiceStubClient2;
  protected static LineageServiceGrpc.LineageServiceBlockingStub lineageServiceStub;
  protected static HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub;
  protected static HydratedServiceGrpc.HydratedServiceBlockingStub
      hydratedServiceBlockingStubClient2;
  protected static ArtifactStoreGrpc.ArtifactStoreBlockingStub artifactStoreBlockingStub;
  private static AtomicBoolean setServerAndServiceRan = new AtomicBoolean(false);

  protected static void init() throws Exception {
    if (setServerAndServiceRan.get()) {
      return;
    }

    String serverName = InProcessServerBuilder.generateName();
    serverBuilder = InProcessServerBuilder.forName(serverName).directExecutor();
    InProcessChannelBuilder serviceAccountClientChannelBuilder =
        InProcessChannelBuilder.forName(serverName).directExecutor();
    InProcessChannelBuilder client1ChannelBuilder =
        InProcessChannelBuilder.forName(serverName).directExecutor();
    InProcessChannelBuilder client2ChannelBuilder =
        InProcessChannelBuilder.forName(serverName).directExecutor();

    testConfig = TestConfig.getInstance();
    handleExecutor = FutureGrpc.initializeExecutor(testConfig.getGrpcServer().getThreadCount());

    // TODO: FIXME: fix init flow as per spring bean initialization

    //  Initialize services that we depend on
    services = ServiceSet.fromConfig(testConfig, new NFSService(new FileStorageProperties()));
    authService = services.authService;
    // Initialize data access
    daos = DAOSet.fromServices(services, testConfig.getJdbi(), handleExecutor, testConfig);
    Migration.migrate(testConfig);

    new AppConfigBeans(new AppContext())
        .initializeBackendServices(serverBuilder, services, daos, handleExecutor);
    serverBuilder.intercept(new MetadataForwarder());
    serverBuilder.intercept(new ExceptionInterceptor());
    serverBuilder.intercept(new MonitoringInterceptor());
    serverBuilder.intercept(new AuthInterceptor());
    // Initialize cron jobs
    new CronJobUtils().initializeCronJobs(testConfig, services);
    new ReconcilerInitializer().initialize(testConfig, services, daos, handleExecutor);

    if (testConfig.testUsers != null && !testConfig.testUsers.isEmpty()) {
      authClientInterceptor = new AuthClientInterceptor(testConfig);
      serviceAccountClientChannelBuilder.intercept(
          authClientInterceptor.getServiceAccountClientAuthInterceptor());
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

    ManagedChannel channelServiceUser =
        serviceAccountClientChannelBuilder.maxInboundMessageSize(1024).build();
    ManagedChannel channel = client1ChannelBuilder.maxInboundMessageSize(1024).build();
    ManagedChannel client2Channel = client2ChannelBuilder.maxInboundMessageSize(1024).build();

    // Create all service blocking stub
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    client2ProjectServiceStub = ProjectServiceGrpc.newBlockingStub(client2Channel);
    serviceUserProjectServiceStub = ProjectServiceGrpc.newBlockingStub(channelServiceUser);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStubClient2 = ExperimentRunServiceGrpc.newBlockingStub(client2Channel);
    commentServiceBlockingStub = CommentServiceGrpc.newBlockingStub(channel);
    versioningServiceBlockingStub = VersioningServiceGrpc.newBlockingStub(channel);
    versioningServiceBlockingStubClient2 = VersioningServiceGrpc.newBlockingStub(client2Channel);
    metadataServiceBlockingStub = MetadataServiceGrpc.newBlockingStub(channel);
    datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);
    datasetServiceStubClient2 = DatasetServiceGrpc.newBlockingStub(client2Channel);
    serviceUserDatasetServiceStub = DatasetServiceGrpc.newBlockingStub(channelServiceUser);
    datasetVersionServiceStub = DatasetVersionServiceGrpc.newBlockingStub(channel);
    datasetVersionServiceStubClient2 = DatasetVersionServiceGrpc.newBlockingStub(client2Channel);
    lineageServiceStub = LineageServiceGrpc.newBlockingStub(channel);
    artifactStoreBlockingStub = ArtifactStoreGrpc.newBlockingStub(channel);
    hydratedServiceBlockingStub = HydratedServiceGrpc.newBlockingStub(channel);
    hydratedServiceBlockingStubClient2 = HydratedServiceGrpc.newBlockingStub(client2Channel);

    serverBuilder.build().start();
    setServerAndServiceRan.set(true);
  }

  @BeforeClass
  public static void setServerAndService() throws Exception {
    init();
  }

  @AfterClass
  public static void removeServerAndService() throws InterruptedException {
    cleanUpResources();

    // shutdown test server
    serverBuilder.build().shutdownNow();
  }

  protected static void cleanUpResources() throws InterruptedException {
    // Remove all entities
    // removeEntities();
    // Delete entities by cron job
    SoftDeleteProjects softDeleteProjects = ReconcilerInitializer.softDeleteProjects;
    SoftDeleteExperiments softDeleteExperiments = ReconcilerInitializer.softDeleteExperiments;
    SoftDeleteExperimentRuns softDeleteExperimentRuns =
        ReconcilerInitializer.softDeleteExperimentRuns;

    softDeleteProjects.resync();
    while (!softDeleteProjects.isEmpty()) {
      LOGGER.trace("Project deletion is still in progress");
      Thread.sleep(10);
    }
    softDeleteExperiments.resync();
    while (!softDeleteExperiments.isEmpty()) {
      LOGGER.trace("Experiment deletion is still in progress");
      Thread.sleep(10);
    }
    softDeleteExperimentRuns.resync();
    while (!softDeleteExperimentRuns.isEmpty()) {
      LOGGER.trace("ExperimentRun deletion is still in progress");
      Thread.sleep(10);
    }

    ReconcilerInitializer.softDeleteDatasets.resync();
    ReconcilerInitializer.softDeleteRepositories.resync();
  }

  protected static void updateTimestampOfResources() throws InterruptedException {
    var updateTimestampRepo = ReconcilerInitializer.updateRepositoryTimestampReconcile;
    updateTimestampRepo.resync();
    while (!updateTimestampRepo.isEmpty()) {
      LOGGER.trace("Update repository timestamp is still in progress");
      Thread.sleep(10);
    }
    var updateTimestampExp = ReconcilerInitializer.updateExperimentTimestampReconcile;
    updateTimestampExp.resync();
    while (!updateTimestampExp.isEmpty()) {
      LOGGER.trace("Update experiment timestamp is still in progress");
      Thread.sleep(10);
    }
    var updateTimestampProject = ReconcilerInitializer.updateProjectTimestampReconcile;
    updateTimestampProject.resync();
    while (!updateTimestampProject.isEmpty()) {
      LOGGER.trace("Update project timestamp is still in progress");
      Thread.sleep(10);
    }
  }
}
