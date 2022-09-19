package ai.verta.modeldb;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.modeldb.configuration.ReconcilerInitializer;
import ai.verta.modeldb.reconcilers.SoftDeleteExperimentRuns;
import ai.verta.modeldb.reconcilers.SoftDeleteExperiments;
import ai.verta.modeldb.reconcilers.SoftDeleteProjects;
import ai.verta.uac.AuthzServiceGrpc;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.CollaboratorServiceGrpc.CollaboratorServiceFutureStub;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetWorkspaceById;
import ai.verta.uac.GetWorkspaceByName;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.SetResource;
import ai.verta.uac.UACServiceGrpc;
import ai.verta.uac.UserInfo;
import ai.verta.uac.VertaUserInfo;
import ai.verta.uac.Workspace;
import ai.verta.uac.WorkspaceServiceGrpc;
import com.google.common.util.concurrent.Futures;
import io.grpc.ManagedChannelBuilder;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ModeldbTestSetup extends TestCase {
  public static final Logger LOGGER = LogManager.getLogger(ModeldbTestSetup.class);

  @Autowired UAC uac;
  @Autowired TestConfig testConfig;
  @Autowired Executor executor;
  @Autowired ReconcilerInitializer reconcilerInitializer;

  protected static ProjectServiceGrpc.ProjectServiceBlockingStub projectServiceStub;
  protected static ProjectServiceGrpc.ProjectServiceBlockingStub client2ProjectServiceStub;
  protected static ProjectServiceBlockingStub serviceUserProjectServiceStub;
  protected static ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub;
  protected static ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub
      experimentRunServiceStub;
  protected static CommentServiceGrpc.CommentServiceBlockingStub commentServiceBlockingStub;
  protected static DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub;
  protected static DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub
      datasetVersionServiceStub;
  protected static UACServiceGrpc.UACServiceBlockingStub uacServiceStub;
  protected static CollaboratorServiceGrpc.CollaboratorServiceBlockingStub
      collaboratorServiceStubClient1;
  protected static CollaboratorServiceGrpc.CollaboratorServiceBlockingStub
      collaboratorServiceStubClient2;

  protected static AuthClientInterceptor authClientInterceptor;
  protected final String random = String.valueOf((long) (Math.random() * 1_000_000));
  private static boolean runningIsolated;

  /**
   * Whether the tests should contain all of their external dependencies as mocks, or should use
   * "real" external dependencies.
   *
   * <p>Note: currently synonymous with running against an H2 database.
   */
  public static boolean isRunningIsolated() {
    return runningIsolated;
  }

  protected void initializedChannelBuilderAndExternalServiceStubs() {
    runningIsolated = testConfig.testsShouldRunIsolatedFromDependencies();
    authClientInterceptor = new AuthClientInterceptor(testConfig);
    var channel =
        ManagedChannelBuilder.forAddress("localhost", testConfig.getGrpcServer().getPort())
            .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
            .intercept(authClientInterceptor.getClient1AuthInterceptor())
            .usePlaintext()
            .executor(executor)
            .build();
    var channelUser2 =
        ManagedChannelBuilder.forAddress("localhost", testConfig.getGrpcServer().getPort())
            .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
            .intercept(authClientInterceptor.getClient2AuthInterceptor())
            .usePlaintext()
            .executor(executor)
            .build();
    var channelServiceUser =
        ManagedChannelBuilder.forAddress("localhost", testConfig.getGrpcServer().getPort())
            .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
            .intercept(authClientInterceptor.getServiceAccountClientAuthInterceptor())
            .usePlaintext()
            .executor(executor)
            .build();
    var authServiceChannel =
        ManagedChannelBuilder.forTarget(
                testConfig.getAuthService().getHost() + ":" + testConfig.getAuthService().getPort())
            .usePlaintext()
            .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
            .intercept(authClientInterceptor.getClient1AuthInterceptor())
            .build();
    var authServiceChannelClient2 =
        ManagedChannelBuilder.forTarget(
                testConfig.getAuthService().getHost() + ":" + testConfig.getAuthService().getPort())
            .usePlaintext()
            .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
            .intercept(authClientInterceptor.getClient2AuthInterceptor())
            .build();

    // Create all service blocking stub
    // Create all service blocking stub
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    client2ProjectServiceStub = ProjectServiceGrpc.newBlockingStub(channelUser2);
    serviceUserProjectServiceStub = ProjectServiceGrpc.newBlockingStub(channelServiceUser);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);
    commentServiceBlockingStub = CommentServiceGrpc.newBlockingStub(channel);
    datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);
    datasetVersionServiceStub = DatasetVersionServiceGrpc.newBlockingStub(channel);
    uacServiceStub = UACServiceGrpc.newBlockingStub(authServiceChannel);
    collaboratorServiceStubClient1 = CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);
    collaboratorServiceStubClient2 =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannelClient2);

    LOGGER.info("Test service infrastructure config complete.");
  }

  protected void setupMockUacEndpoints(UAC uac, CollaboratorServiceFutureStub collaboratorMock) {
    var uacMock = mock(UACServiceGrpc.UACServiceFutureStub.class);
    when(uac.getUACService()).thenReturn(uacMock);
    var testUser1 =
        UserInfo.newBuilder()
            .setEmail(authClientInterceptor.getClient1Email())
            .setVertaInfo(
                VertaUserInfo.newBuilder()
                    .setUserId(authClientInterceptor.getClient1UserName())
                    .setUsername(authClientInterceptor.getClient1UserName())
                    .setDefaultWorkspaceId(authClientInterceptor.getClient1WorkspaceId())
                    .setWorkspaceId(String.valueOf(authClientInterceptor.getClient1WorkspaceId()))
                    .build())
            .build();
    when(uacMock.getCurrentUser(any())).thenReturn(Futures.immediateFuture(testUser1));
    when(uacMock.getUser(any())).thenReturn(Futures.immediateFuture(testUser1));

    var authzMock = mock(AuthzServiceGrpc.AuthzServiceFutureStub.class);
    when(uac.getAuthzService()).thenReturn(authzMock);
    when(authzMock.isSelfAllowed(any()))
        .thenReturn(
            Futures.immediateFuture(IsSelfAllowed.Response.newBuilder().setAllowed(true).build()));

    // allow any SetResource call
    when(collaboratorMock.setResource(any()))
        .thenReturn(Futures.immediateFuture(SetResource.Response.newBuilder().build()));
    var workspaceMock = mock(WorkspaceServiceGrpc.WorkspaceServiceFutureStub.class);
    when(uac.getWorkspaceService()).thenReturn(workspaceMock);
    when(workspaceMock.getWorkspaceById(
            GetWorkspaceById.newBuilder()
                .setId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                .build()))
        .thenReturn(
            Futures.immediateFuture(
                Workspace.newBuilder()
                    .setId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                    .build()));
    when(workspaceMock.getWorkspaceByName(
            GetWorkspaceByName.newBuilder()
                .setName(testUser1.getVertaInfo().getUsername())
                .build()))
        .thenReturn(
            Futures.immediateFuture(
                Workspace.newBuilder()
                    .setId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                    .build()));

    when(collaboratorMock.getResourcesSpecialPersonalWorkspace(any()))
        .thenReturn(
            Futures.immediateFuture(
                GetResources.Response.newBuilder()
                    .addItem(
                        GetResourcesResponseItem.newBuilder()
                            .setVisibility(ResourceVisibility.PRIVATE)
                            .setResourceType(
                                ResourceType.newBuilder()
                                    .setModeldbServiceResourceType(
                                        ModelDBServiceResourceTypes.PROJECT)
                                    .build())
                            .setOwnerId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                            .setWorkspaceId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                            .build())
                    .build()));
  }

  protected void cleanUpResources() {
    // Delete entities by cron job
    SoftDeleteProjects softDeleteProjects = reconcilerInitializer.getSoftDeleteProjects();
    SoftDeleteExperiments softDeleteExperiments = reconcilerInitializer.getSoftDeleteExperiments();
    SoftDeleteExperimentRuns softDeleteExperimentRuns =
        reconcilerInitializer.getSoftDeleteExperimentRuns();

    softDeleteProjects.resync();
    await().until(softDeleteProjects::isEmpty);

    softDeleteExperiments.resync();
    await().until(softDeleteExperiments::isEmpty);

    softDeleteExperimentRuns.resync();
    await().until(softDeleteExperimentRuns::isEmpty);

    reconcilerInitializer.getSoftDeleteDatasets().resync();
    reconcilerInitializer.getSoftDeleteRepositories().resync();
  }

  public void mockGetResourcesForProject(
      Project project, CollaboratorServiceFutureStub collaboratorMock) {
    when(collaboratorMock.getResourcesSpecialPersonalWorkspace(any()))
        .thenReturn(
            Futures.immediateFuture(
                GetResources.Response.newBuilder()
                    .addItem(
                        GetResourcesResponseItem.newBuilder()
                            .setVisibility(ResourceVisibility.PRIVATE)
                            .setResourceId(project.getId())
                            .setResourceName(project.getName())
                            .setResourceType(
                                ResourceType.newBuilder()
                                    .setModeldbServiceResourceType(
                                        ModelDBServiceResourceTypes.PROJECT)
                                    .build())
                            .setOwnerId(project.getWorkspaceServiceId())
                            .setWorkspaceId(project.getWorkspaceServiceId())
                            .build())
                    .build()));
  }

  public void mockGetResourcesForAllEntity(
      CollaboratorServiceFutureStub collaboratorMock, Map<Long, Project> projectMap) {
    when(collaboratorMock.getResources(any()))
        .thenReturn(
            Futures.immediateFuture(
                GetResources.Response.newBuilder()
                    .addAllItem(
                        projectMap.values().stream()
                            .map(
                                project ->
                                    GetResourcesResponseItem.newBuilder()
                                        .setResourceId(project.getId())
                                        .setWorkspaceId(
                                            authClientInterceptor.getClient1WorkspaceId())
                                        .build())
                            .collect(Collectors.toList()))
                    .build()));

    when(collaboratorMock.getResourcesSpecialPersonalWorkspace(any()))
        .thenReturn(
            Futures.immediateFuture(
                GetResources.Response.newBuilder()
                    .addAllItem(
                        projectMap.values().stream()
                            .map(
                                project ->
                                    GetResourcesResponseItem.newBuilder()
                                        .setVisibility(ResourceVisibility.PRIVATE)
                                        .setResourceId(project.getId())
                                        .setResourceName(project.getName())
                                        .setResourceType(
                                            ResourceType.newBuilder()
                                                .setModeldbServiceResourceType(
                                                    ModelDBServiceResourceTypes.PROJECT)
                                                .build())
                                        .setOwnerId(project.getWorkspaceServiceId())
                                        .setWorkspaceId(project.getWorkspaceServiceId())
                                        .build())
                            .collect(Collectors.toList()))
                    .build()));
  }
}
