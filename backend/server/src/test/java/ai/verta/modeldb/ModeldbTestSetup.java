package ai.verta.modeldb;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.DatasetServiceGrpc.DatasetServiceBlockingStub;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.common.authservice.AuthServiceChannel;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.interceptors.MetadataForwarder;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.modeldb.configuration.ReconcilerInitializer;
import ai.verta.modeldb.metadata.MetadataServiceGrpc;
import ai.verta.modeldb.reconcilers.SoftDeleteExperimentRuns;
import ai.verta.modeldb.reconcilers.SoftDeleteExperiments;
import ai.verta.modeldb.reconcilers.SoftDeleteProjects;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.VersioningServiceGrpc;
import ai.verta.uac.Action;
import ai.verta.uac.Actions;
import ai.verta.uac.AuthzServiceGrpc;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.DeleteResources;
import ai.verta.uac.Entities;
import ai.verta.uac.GetAllowedEntities;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetSelfAllowedActionsBatch;
import ai.verta.uac.GetSelfAllowedResources;
import ai.verta.uac.GetUser;
import ai.verta.uac.GetUsers;
import ai.verta.uac.GetUsersFuzzy;
import ai.verta.uac.GetWorkspaceById;
import ai.verta.uac.GetWorkspaceByName;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ListMyOrganizations;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.OrganizationServiceGrpc;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Resources;
import ai.verta.uac.RoleServiceGrpc;
import ai.verta.uac.ServiceEnum.Service;
import ai.verta.uac.SetResource;
import ai.verta.uac.SetRoleBinding;
import ai.verta.uac.UACServiceGrpc;
import ai.verta.uac.UserInfo;
import ai.verta.uac.VertaUserInfo;
import ai.verta.uac.Workspace;
import ai.verta.uac.WorkspaceServiceGrpc;
import com.google.common.util.concurrent.Futures;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public abstract class ModeldbTestSetup {
  public static final Logger LOGGER = LogManager.getLogger(ModeldbTestSetup.class);

  @Autowired UAC uac;
  @Autowired TestConfig testConfig;

  @Qualifier("grpcExecutor")
  @Autowired
  Executor executor;

  @Autowired ReconcilerInitializer reconcilerInitializer;

  protected UserInfo testUser1;
  protected UserInfo testUser2;

  protected ProjectServiceGrpc.ProjectServiceBlockingStub projectServiceStub;
  protected ProjectServiceGrpc.ProjectServiceBlockingStub client2ProjectServiceStub;
  protected ProjectServiceBlockingStub serviceUserProjectServiceStub;
  protected ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub;
  protected ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub experimentRunServiceStub;
  protected ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub
      experimentRunServiceStubClient2;
  protected CommentServiceGrpc.CommentServiceBlockingStub commentServiceBlockingStub;
  protected DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub;
  protected DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStubClient2;
  protected DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub;
  protected DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub
      datasetVersionServiceStubClient2;
  protected static LineageServiceGrpc.LineageServiceBlockingStub lineageServiceStub;
  protected VersioningServiceGrpc.VersioningServiceBlockingStub versioningServiceBlockingStub;
  protected VersioningServiceGrpc.VersioningServiceBlockingStub
      versioningServiceBlockingStubClient2;
  protected MetadataServiceGrpc.MetadataServiceBlockingStub metadataServiceBlockingStub;
  protected DatasetServiceBlockingStub datasetServiceStubServiceAccount;
  protected UACServiceGrpc.UACServiceBlockingStub uacServiceStub;
  protected CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceStubClient1;
  protected CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceStubClient2;
  protected OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationServiceBlockingStub;

  protected AuthClientInterceptor authClientInterceptor;
  protected final String random = String.valueOf((long) (Math.random() * 1_000_000));
  private static boolean runningIsolated;

  protected CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorBlockingMock =
      mock(CollaboratorServiceGrpc.CollaboratorServiceBlockingStub.class);
  protected AuthzServiceGrpc.AuthzServiceBlockingStub authzBlockingMock =
      mock(AuthzServiceGrpc.AuthzServiceBlockingStub.class);
  protected UACServiceGrpc.UACServiceBlockingStub uacBlockingMock =
      mock(UACServiceGrpc.UACServiceBlockingStub.class);
  protected WorkspaceServiceGrpc.WorkspaceServiceBlockingStub workspaceBlockingMock =
      mock(WorkspaceServiceGrpc.WorkspaceServiceBlockingStub.class);
  protected RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingMock =
      mock(RoleServiceGrpc.RoleServiceBlockingStub.class);
  protected OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationBlockingMock =
      mock(OrganizationServiceGrpc.OrganizationServiceBlockingStub.class);
  private ManagedChannel channel;
  private ManagedChannel channelUser2;
  private ManagedChannel channelServiceUser;

  /**
   * Whether the tests should contain all of their external dependencies as mocks, or should use
   * "real" external dependencies.
   *
   * <p>Note: currently synonymous with running against an H2 database.
   */
  public static boolean isRunningIsolated() {
    return runningIsolated;
  }

  @AfterEach
  public void tearDown() {
    channel.shutdown();
    channelUser2.shutdown();
    channelServiceUser.shutdown();
  }

  public void initializeChannelBuilderAndExternalServiceStubs() {
    runningIsolated = testConfig.testsShouldRunIsolatedFromDependencies();
    authClientInterceptor = new AuthClientInterceptor(testConfig);
    channel =
        ManagedChannelBuilder.forAddress("localhost", testConfig.getGrpcServer().getPort())
            .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
            .intercept(authClientInterceptor.getClient1AuthInterceptor())
            .usePlaintext()
            .executor(executor)
            .build();
    channelUser2 =
        ManagedChannelBuilder.forAddress("localhost", testConfig.getGrpcServer().getPort())
            .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
            .intercept(authClientInterceptor.getClient2AuthInterceptor())
            .usePlaintext()
            .executor(executor)
            .build();
    channelServiceUser =
        ManagedChannelBuilder.forAddress("localhost", testConfig.getGrpcServer().getPort())
            .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
            .intercept(authClientInterceptor.getServiceAccountClientAuthInterceptor())
            .usePlaintext()
            .executor(executor)
            .build();

    // Create all service blocking stub
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    client2ProjectServiceStub = ProjectServiceGrpc.newBlockingStub(channelUser2);
    serviceUserProjectServiceStub = ProjectServiceGrpc.newBlockingStub(channelServiceUser);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStubClient2 = ExperimentRunServiceGrpc.newBlockingStub(channelUser2);
    versioningServiceBlockingStub = VersioningServiceGrpc.newBlockingStub(channel);
    versioningServiceBlockingStubClient2 = VersioningServiceGrpc.newBlockingStub(channelUser2);
    metadataServiceBlockingStub = MetadataServiceGrpc.newBlockingStub(channel);
    commentServiceBlockingStub = CommentServiceGrpc.newBlockingStub(channel);
    datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);
    datasetServiceStubClient2 = DatasetServiceGrpc.newBlockingStub(channelUser2);
    datasetServiceStubServiceAccount = DatasetServiceGrpc.newBlockingStub(channelServiceUser);
    datasetVersionServiceStub = DatasetVersionServiceGrpc.newBlockingStub(channel);
    datasetVersionServiceStubClient2 = DatasetVersionServiceGrpc.newBlockingStub(channel);
    lineageServiceStub = LineageServiceGrpc.newBlockingStub(channel);

    if (!runningIsolated) {
      var authServiceChannel =
          ManagedChannelBuilder.forTarget(
                  testConfig.getAuthService().getHost()
                      + ":"
                      + testConfig.getAuthService().getPort())
              .usePlaintext()
              .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
              .intercept(authClientInterceptor.getClient1AuthInterceptor())
              .build();
      var authServiceChannelClient2 =
          ManagedChannelBuilder.forTarget(
                  testConfig.getAuthService().getHost()
                      + ":"
                      + testConfig.getAuthService().getPort())
              .usePlaintext()
              .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
              .intercept(authClientInterceptor.getClient2AuthInterceptor())
              .build();
      uacServiceStub = UACServiceGrpc.newBlockingStub(authServiceChannel);
      collaboratorServiceStubClient1 = CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);
      collaboratorServiceStubClient2 =
          CollaboratorServiceGrpc.newBlockingStub(authServiceChannelClient2);
      organizationServiceBlockingStub = OrganizationServiceGrpc.newBlockingStub(authServiceChannel);

      GetUser getUserRequest =
          GetUser.newBuilder().setEmail(authClientInterceptor.getClient1Email()).build();
      // Get the user info by vertaId form the AuthService
      testUser1 = uacServiceStub.getUser(getUserRequest);
      getUserRequest =
          GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
      testUser2 = uacServiceStub.getUser(getUserRequest);
    } else {
      testUser1 =
          UserInfo.newBuilder()
              .setEmail(authClientInterceptor.getClient1Email())
              .setVertaInfo(
                  VertaUserInfo.newBuilder()
                      .setUserId(String.valueOf(authClientInterceptor.getClient1WorkspaceId()))
                      .setUsername(authClientInterceptor.getClient1UserName())
                      .setDefaultWorkspaceId(authClientInterceptor.getClient1WorkspaceId())
                      .setWorkspaceId(String.valueOf(authClientInterceptor.getClient1WorkspaceId()))
                      .build())
              .build();

      testUser2 =
          UserInfo.newBuilder()
              .setEmail(authClientInterceptor.getClient2Email())
              .setVertaInfo(
                  VertaUserInfo.newBuilder()
                      .setUserId(String.valueOf(authClientInterceptor.getClient2WorkspaceId()))
                      .setUsername(authClientInterceptor.getClient2UserName())
                      .setDefaultWorkspaceId(authClientInterceptor.getClient2WorkspaceId())
                      .setWorkspaceId(String.valueOf(authClientInterceptor.getClient2WorkspaceId()))
                      .build())
              .build();
    }

    LOGGER.trace("Test service infrastructure config complete.");
  }

  protected void setupMockUacEndpoints(UAC uac) {
    Context.current().withValue(MetadataForwarder.METADATA_INFO, new Metadata()).attach();
    AuthServiceChannel authChannelMock = uac.getBlockingAuthServiceChannel();
    when(authChannelMock.getAuthzServiceBlockingStub()).thenReturn(authzBlockingMock);
    when(authChannelMock.getUacServiceBlockingStub()).thenReturn(uacBlockingMock);
    when(authChannelMock.getWorkspaceServiceBlockingStub()).thenReturn(workspaceBlockingMock);
    when(authChannelMock.getCollaboratorServiceBlockingStub()).thenReturn(collaboratorBlockingMock);
    when(authChannelMock.getRoleServiceBlockingStubForServiceUser())
        .thenReturn(roleServiceBlockingMock);
    when(authChannelMock.getOrganizationServiceBlockingStub()).thenReturn(organizationBlockingMock);
    when(authChannelMock.getCollaboratorServiceBlockingStubForServiceUser())
        .thenReturn(collaboratorBlockingMock);

    UACServiceGrpc.UACServiceFutureStub uacMock = uac.getUACService();
    when(uacMock.getCurrentUser(any())).thenReturn(Futures.immediateFuture(testUser1));
    when(uacMock.getUser(any())).thenReturn(Futures.immediateFuture(testUser1));
    when(uacMock.getUsersFuzzy(any()))
        .thenReturn(
            Futures.immediateFuture(
                GetUsersFuzzy.Response.newBuilder()
                    .addAllUserInfos(List.of(testUser1, testUser2))
                    .build()));
    when(uac.getAuthzService().isSelfAllowed(any()))
        .thenReturn(
            Futures.immediateFuture(IsSelfAllowed.Response.newBuilder().setAllowed(true).build()));
    when(authzBlockingMock.isSelfAllowed(any()))
        .thenReturn(IsSelfAllowed.Response.newBuilder().setAllowed(true).build());
    when(authzBlockingMock.getSelfAllowedActionsBatch(any()))
        .thenReturn(
            GetSelfAllowedActionsBatch.Response.newBuilder()
                .putActions(
                    "READ",
                    Actions.newBuilder()
                        .addActions(
                            Action.newBuilder()
                                .setModeldbServiceAction(ModelDBServiceActions.READ)
                                .setService(Service.MODELDB_SERVICE)
                                .build())
                        .build())
                .build());
    when(uacBlockingMock.getCurrentUser(any())).thenReturn(testUser1);
    when(uacBlockingMock.getUsers(any()))
        .thenReturn(
            GetUsers.Response.newBuilder().addAllUserInfos(List.of(testUser1, testUser2)).build());
    when(workspaceBlockingMock.getWorkspaceByName(any()))
        .thenReturn(
            Workspace.newBuilder()
                .setId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                .setUsername(testUser1.getVertaInfo().getUsername())
                .build());
    when(workspaceBlockingMock.getWorkspaceById(any()))
        .thenReturn(
            Workspace.newBuilder()
                .setId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                .setUsername(testUser1.getVertaInfo().getUsername())
                .build());
    when(collaboratorBlockingMock.setResource(any()))
        .thenReturn(SetResource.Response.newBuilder().build());
    when(collaboratorBlockingMock.deleteResources(any()))
        .thenReturn(DeleteResources.Response.newBuilder().build());
    // allow any SetResource call
    when(uac.getCollaboratorService().setResource(any()))
        .thenReturn(Futures.immediateFuture(SetResource.Response.newBuilder().build()));
    when(uac.getWorkspaceService()
            .getWorkspaceById(
                GetWorkspaceById.newBuilder()
                    .setId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                    .build()))
        .thenReturn(
            Futures.immediateFuture(
                Workspace.newBuilder()
                    .setId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                    .build()));
    when(uac.getWorkspaceService()
            .getWorkspaceByName(
                GetWorkspaceByName.newBuilder()
                    .setName(testUser1.getVertaInfo().getUsername())
                    .build()))
        .thenReturn(
            Futures.immediateFuture(
                Workspace.newBuilder()
                    .setId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                    .build()));
    when(uac.getCollaboratorService().getResourcesSpecialPersonalWorkspace(any()))
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
    when(uac.getServiceAccountRoleServiceFutureStub().setRoleBinding(any()))
        .thenReturn(Futures.immediateFuture(SetRoleBinding.Response.newBuilder().build()));
    when(organizationBlockingMock.listMyOrganizations(any()))
        .thenReturn(ListMyOrganizations.Response.newBuilder().build());
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

  protected void updateTimestampOfResources() throws Exception {
    var updateTimestampRepo = reconcilerInitializer.getUpdateRepositoryTimestampReconcile();
    updateTimestampRepo.resync();
    await().until(updateTimestampRepo::isEmpty);
    var updateTimestampExp = reconcilerInitializer.getUpdateExperimentTimestampReconcile();
    updateTimestampExp.resync();
    await().until(updateTimestampExp::isEmpty);
    var updateTimestampProject = reconcilerInitializer.getUpdateProjectTimestampReconcile();
    updateTimestampProject.resync();
    await().until(updateTimestampProject::isEmpty);
  }

  protected void mockGetResourcesForAllProjects(
      Map<String, Project> projectMap, UserInfo userInfo) {
    var projectIdNameMap =
        projectMap.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> String.valueOf(entry.getKey()), entry -> entry.getValue().getName()));
    mockGetResources(projectIdNameMap, userInfo);
    when(uac.getCollaboratorService().getResourcesSpecialPersonalWorkspace(any()))
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
    mockGetSelfAllowedResources(
        projectMap.keySet(), ModelDBServiceResourceTypes.PROJECT, ModelDBServiceActions.READ);
  }

  protected void mockGetSelfAllowedResources(
      Set<String> resourceIds,
      ModelDBServiceResourceTypes resourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    var allowedResourcesResponse =
        resourceIds.stream()
            .map(
                resourceId ->
                    Resources.newBuilder()
                        .addResourceIds(resourceId)
                        .setResourceType(
                            ResourceType.newBuilder()
                                .setModeldbServiceResourceType(resourceTypes)
                                .build())
                        .setService(Service.MODELDB_SERVICE)
                        .build())
            .collect(Collectors.toList());
    var getSelfAllowedResourcesRequest =
        GetSelfAllowedResources.newBuilder()
            .addActions(
                Action.newBuilder()
                    .setModeldbServiceAction(modelDBServiceActions)
                    .setService(Service.MODELDB_SERVICE))
            .setService(Service.MODELDB_SERVICE)
            .setResourceType(ResourceType.newBuilder().setModeldbServiceResourceType(resourceTypes))
            .build();
    var response = GetSelfAllowedResources.Response.newBuilder();
    if (!allowedResourcesResponse.isEmpty()) {
      response.addAllResources(allowedResourcesResponse);
    }
    when(uac.getAuthzService().getSelfAllowedResources(getSelfAllowedResourcesRequest))
        .thenReturn(Futures.immediateFuture(response.build()));
    when(authzBlockingMock.getSelfAllowedResources(getSelfAllowedResourcesRequest))
        .thenReturn(response.build());
    when(authzBlockingMock.getAllowedEntities(any()))
        .thenReturn(
            GetAllowedEntities.Response.newBuilder()
                .addEntities(
                    Entities.newBuilder()
                        .addAllUserIds(
                            List.of(
                                testUser1.getVertaInfo().getUserId(),
                                testUser2.getVertaInfo().getUserId()))
                        .build())
                .build());
  }

  protected void mockGetResources(Map<String, String> resourceIdNameMap, UserInfo userInfo) {
    var resourcesResponse =
        GetResources.Response.newBuilder()
            .addAllItem(
                resourceIdNameMap.entrySet().stream()
                    .map(
                        resourceIdNameEntry ->
                            GetResourcesResponseItem.newBuilder()
                                .setResourceId(resourceIdNameEntry.getKey())
                                .setResourceName(resourceIdNameEntry.getValue())
                                .setWorkspaceId(userInfo.getVertaInfo().getDefaultWorkspaceId())
                                .setOwnerId(userInfo.getVertaInfo().getDefaultWorkspaceId())
                                .build())
                    .collect(Collectors.toList()))
            .build();
    when(uac.getCollaboratorService().getResources(any()))
        .thenReturn(Futures.immediateFuture(resourcesResponse));
    when(collaboratorBlockingMock.getResources(any())).thenReturn(resourcesResponse);
  }

  protected void mockGetResourcesForAllDatasets(
      Map<String, Dataset> datasetMap, UserInfo userInfo) {
    var datasetIdNameMap =
        datasetMap.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> String.valueOf(entry.getKey()), entry -> entry.getValue().getName()));
    mockGetResources(datasetIdNameMap, userInfo);
    when(uac.getCollaboratorService().getResourcesSpecialPersonalWorkspace(any()))
        .thenReturn(
            Futures.immediateFuture(
                GetResources.Response.newBuilder()
                    .addAllItem(
                        datasetMap.values().stream()
                            .map(
                                dataset ->
                                    GetResourcesResponseItem.newBuilder()
                                        .setVisibility(ResourceVisibility.PRIVATE)
                                        .setResourceId(dataset.getId())
                                        .setResourceName(dataset.getName())
                                        .setResourceType(
                                            ResourceType.newBuilder()
                                                .setModeldbServiceResourceType(
                                                    ModelDBServiceResourceTypes.DATASET)
                                                .build())
                                        .setOwnerId(dataset.getWorkspaceServiceId())
                                        .setWorkspaceId(dataset.getWorkspaceServiceId())
                                        .build())
                            .collect(Collectors.toList()))
                    .build()));
    mockGetSelfAllowedResources(
        datasetIdNameMap.keySet(), ModelDBServiceResourceTypes.DATASET, ModelDBServiceActions.READ);
  }

  protected void mockGetResourcesForAllRepositories(
      Map<Long, Repository> repositoryMap, UserInfo userInfo) {
    var repoIdNameMap =
        repositoryMap.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> String.valueOf(entry.getKey()), entry -> entry.getValue().getName()));
    mockGetResources(repoIdNameMap, userInfo);
    when(collaboratorMock.getResourcesSpecialPersonalWorkspace(any()))
        .thenReturn(
            Futures.immediateFuture(
                GetResources.Response.newBuilder()
                    .addAllItem(
                        repositoryMap.values().stream()
                            .map(
                                repository ->
                                    GetResourcesResponseItem.newBuilder()
                                        .setVisibility(ResourceVisibility.PRIVATE)
                                        .setResourceId(String.valueOf(repository.getId()))
                                        .setResourceName(repository.getName())
                                        .setResourceType(
                                            ResourceType.newBuilder()
                                                .setModeldbServiceResourceType(
                                                    ModelDBServiceResourceTypes.REPOSITORY)
                                                .build())
                                        .setOwnerId(repository.getWorkspaceServiceId())
                                        .setWorkspaceId(repository.getWorkspaceServiceId())
                                        .build())
                            .collect(Collectors.toList()))
                    .build()));
    mockGetSelfAllowedResources(
        repoIdNameMap.keySet(), ModelDBServiceResourceTypes.REPOSITORY, ModelDBServiceActions.READ);
  }
}
