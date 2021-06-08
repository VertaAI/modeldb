package ai.verta.modeldb;

import ai.verta.artifactstore.ArtifactStoreGrpc;
import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.exceptions.ExceptionInterceptor;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.interceptors.MetadataForwarder;
import ai.verta.modeldb.common.monitoring.AuditLogInterceptor;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.cron_jobs.CronJobUtils;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.monitoring.MonitoringInterceptor;
import ai.verta.modeldb.reconcilers.ReconcilerInitializer;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class TestsInit {

  private static InProcessServerBuilder serverBuilder;
  protected static AuthClientInterceptor authClientInterceptor;

  protected static Config config;
  protected static DeleteEntitiesCron deleteEntitiesCron;
  protected static AuthService authService;

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

    config = Config.getInstance();
    final Executor handleExecutor = FutureGrpc.initializeExecutor(config.grpcServer.threadCount);
    // Initialize services that we depend on
    ServiceSet services = ServiceSet.fromConfig(config);
    authService = services.authService;
    // Initialize data access
    DAOSet daos = DAOSet.fromServices(services, config.getTestJdbi(), handleExecutor, config);
    App.migrate(config.test.database, config.migrations);

    App.initializeBackendServices(serverBuilder, services, daos, handleExecutor);
    serverBuilder.intercept(new MetadataForwarder());
    serverBuilder.intercept(new ExceptionInterceptor());
    serverBuilder.intercept(new MonitoringInterceptor());
    serverBuilder.intercept(new AuthInterceptor());
    serverBuilder.intercept(new AuditLogInterceptor(false));
    // Initialize cron jobs
    CronJobUtils.initializeCronJobs(config, services);
    ReconcilerInitializer.initialize(config, services, config.getTestJdbi(), handleExecutor);

    if (config.test != null) {
      authClientInterceptor = new AuthClientInterceptor(config.test);
      client1ChannelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
      client2ChannelBuilder.intercept(authClientInterceptor.getClient2AuthInterceptor());
    }
    deleteEntitiesCron = new DeleteEntitiesCron(services.authService, services.roleService, 1000);

    if (config.authService != null) {
      ManagedChannel authServiceChannel =
          ManagedChannelBuilder.forTarget(config.authService.host + ":" + config.authService.port)
              .usePlaintext()
              .intercept(authClientInterceptor.getClient1AuthInterceptor())
              .build();
      uacServiceStub = UACServiceGrpc.newBlockingStub(authServiceChannel);
      organizationServiceBlockingStub = OrganizationServiceGrpc.newBlockingStub(authServiceChannel);
      roleServiceBlockingStub = RoleServiceGrpc.newBlockingStub(authServiceChannel);
      collaboratorServiceStubClient1 = CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

      ManagedChannel authServiceChannelClient2 =
          ManagedChannelBuilder.forTarget(config.authService.host + ":" + config.authService.port)
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

    // Remove all entities
    // removeEntities();
    // Delete entities by cron job
    deleteEntitiesCron.run();

    // shutdown test server
    serverBuilder.build().shutdownNow();
  }
}
