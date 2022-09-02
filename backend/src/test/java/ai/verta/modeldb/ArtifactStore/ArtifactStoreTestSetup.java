package ai.verta.modeldb.ArtifactStore;

import ai.verta.modeldb.App;
import ai.verta.modeldb.AuthClientInterceptor;
import ai.verta.modeldb.Empty;
import ai.verta.modeldb.ExperimentRunServiceGrpc;
import ai.verta.modeldb.ExperimentServiceGrpc;
import ai.verta.modeldb.ProjectServiceGrpc;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.common.config.GrpcServerConfig;
import ai.verta.modeldb.common.config.SpringServerConfig;
import ai.verta.modeldb.common.configuration.AppContext;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.modeldb.configuration.ReconcilerInitializer;
import ai.verta.modeldb.reconcilers.SoftDeleteExperimentRuns;
import ai.verta.modeldb.reconcilers.SoftDeleteExperiments;
import ai.verta.modeldb.reconcilers.SoftDeleteProjects;
import io.grpc.ManagedChannelBuilder;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class ArtifactStoreTestSetup {
  private static final Logger LOGGER = LogManager.getLogger(ArtifactStoreTestSetup.class);
  private static final int RANDOM_GRPC_PORT = new Random().nextInt(10000) + 1024;
  private static final int RANDOM_WEB_PORT = RANDOM_GRPC_PORT + 1;
  public static final int USE_RANDOM_PORTS_PORT = 99999;
  protected static TestConfig testConfig;
  protected static Executor executor;
  private static ReconcilerInitializer reconcilerInitializer;
  protected static ProjectServiceBlockingStub projectServiceStub;
  protected static ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub;
  protected static ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub
      experimentRunServiceStub;
  private static final AtomicBoolean appStarted = new AtomicBoolean(false);

  protected static void init() {
    if (appStarted.get()) {
      return;
    }

    if (appIsRunning()) {
      appStarted.set(true);
      return;
    }

    testConfig = initializeTestConfig();
    System.getProperties().put("modeldb.test", true);
    System.getProperties().put("LIQUIBASE_MIGRATION", true);
    System.getProperties().put("RUN_LIQUIBASE_SEPARATE", false);
    App app = App.initialize(testConfig);

    try {
      App.main(new String[0]);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    var applicationContext = app.getApplicationContext();
    executor = applicationContext.getBean(Executor.class);
    reconcilerInitializer =
        (ReconcilerInitializer) applicationContext.getBean("reconcilerInitializer");
    var services = applicationContext.getBean(ServiceSet.class);
    var appContext = applicationContext.getBean(AppContext.class);

    // FIXME: Add mock uac logic
    /*var uac = Mockito.mock(UAC.class);
    appContext.registerBean("uac", uac);

    when(uac.getAuthzService()
        .isSelfAllowed(any()))
        .then(invocationOnMock -> true);*/

    initializedChannelBuilderAndExternalServiceStubs();
    appStarted.set(true);
  }

  private static TestConfig initializeTestConfig() {
    var config =
        TestConfig.getInstance(
            "src/test/java/ai/verta/modeldb/ArtifactStore/nfs-config-test-h2.yaml");
    SpringServerConfig springServerConfig = config.getSpringServer();
    if (springServerConfig.getPort() == USE_RANDOM_PORTS_PORT) {
      springServerConfig.setPort(RANDOM_WEB_PORT);
      config.setSpringServer(springServerConfig);
    }

    GrpcServerConfig grpcServerConfig = config.getGrpcServer();
    if (config.getGrpcServer().getPort() == USE_RANDOM_PORTS_PORT) {
      grpcServerConfig.setPort(RANDOM_GRPC_PORT);
      config.setGrpcServer(grpcServerConfig);
    }

    var artifactStoreConfig = config.getArtifactStoreConfig();
    artifactStoreConfig.setHost("localhost:" + config.getSpringServer().getPort());
    config.setArtifactStoreConfig(artifactStoreConfig);
    return config;
  }

  private static void initializedChannelBuilderAndExternalServiceStubs() {
    var authClientInterceptor = new AuthClientInterceptor(testConfig);
    var channel =
        ManagedChannelBuilder.forAddress("localhost", testConfig.getGrpcServer().getPort())
            .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
            .intercept(authClientInterceptor.getClient1AuthInterceptor())
            .usePlaintext()
            .executor(executor)
            .build();

    // Create all service blocking stub
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);

    LOGGER.info("Test service infrastructure config complete.");
  }

  private static boolean appIsRunning() {
    try {
      var response = projectServiceStub.verifyConnection(Empty.newBuilder().build());
      return response.getStatus();
    } catch (Exception e) {
      return false;
    }
  }

  @BeforeClass
  public static void setServerAndService() throws Exception {
    init();
  }

  @AfterClass
  public static void removeServerAndService() throws InterruptedException {
    cleanUpResources();
  }

  protected static void cleanUpResources() throws InterruptedException {
    // Remove all entities
    // removeEntities();
    // Delete entities by cron job
    SoftDeleteProjects softDeleteProjects = reconcilerInitializer.softDeleteProjects;
    SoftDeleteExperiments softDeleteExperiments = reconcilerInitializer.softDeleteExperiments;
    SoftDeleteExperimentRuns softDeleteExperimentRuns =
        reconcilerInitializer.softDeleteExperimentRuns;

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
}
