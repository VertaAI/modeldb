package ai.verta.modeldb;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.DatasetServiceGrpc.DatasetServiceBlockingStub;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
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
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ServiceEnum.Service;
import com.google.common.util.concurrent.Futures;
import io.grpc.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
  protected UserInfo serviceAccountUser;

  protected WorkspaceV2 testUser1Workspace;

  protected String organizationId;
  private OrganizationV2 organizationV2;
  protected String groupIdUser1;
  protected String roleIdUser1;

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
  private OrganizationServiceV2Grpc.OrganizationServiceV2BlockingStub
      organizationServiceV2BlockingStub;

  protected AuthClientInterceptor authClientInterceptor;
  protected final String random = String.valueOf((long) (Math.random() * 1_000_000));
  private static boolean runningIsolated;
  protected ManagedChannel authServiceChannelServiceUser;

  protected CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorBlockingMock;
  protected AuthzServiceGrpc.AuthzServiceBlockingStub authzBlockingMock;
  protected UACServiceGrpc.UACServiceBlockingStub uacBlockingMock;
  protected WorkspaceServiceGrpc.WorkspaceServiceBlockingStub workspaceBlockingMock;
  protected RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingMock;
  protected OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationBlockingMock;
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

  @BeforeEach
  void setUp() {
    runningIsolated = testConfig.testsShouldRunIsolatedFromDependencies();

    if (runningIsolated) {
      ModeldbTestConfigurationBeans.resetUacMocks(uac);
      collaboratorBlockingMock =
          uac.getBlockingAuthServiceChannel().getCollaboratorServiceBlockingStub();
      authzBlockingMock = uac.getBlockingAuthServiceChannel().getAuthzServiceBlockingStub();
      uacBlockingMock = uac.getBlockingAuthServiceChannel().getUacServiceBlockingStub();
      workspaceBlockingMock = uac.getBlockingAuthServiceChannel().getWorkspaceServiceBlockingStub();
      roleServiceBlockingMock = uac.getBlockingAuthServiceChannel().getRoleServiceBlockingStub();
      organizationBlockingMock =
          uac.getBlockingAuthServiceChannel().getOrganizationServiceBlockingStub();
    }
  }

  @AfterEach
  public void tearDown() {
    channel.shutdown();
    channelUser2.shutdown();
    channelServiceUser.shutdown();
  }

  public void initializeChannelBuilderAndExternalServiceStubs() {
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

    if (runningIsolated) {
      serviceAccountUser =
          UserInfo.newBuilder()
              .setEmail(testConfig.getService_user().getEmail())
              .setVertaInfo(
                  VertaUserInfo.newBuilder()
                      .setUserId("-111")
                      .setUsername(testConfig.getService_user().getEmail())
                      .setDefaultWorkspaceId(-111)
                      .setWorkspaceId("-111")
                      .build())
              .build();

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
      testUser1Workspace =
          WorkspaceV2.newBuilder()
              .setId(Long.parseLong(testUser1.getVertaInfo().getWorkspaceId()))
              .setName(testUser1.getVertaInfo().getUsername())
              .setOrgId("-1")
              .setNamespace("namespace")
              .addPermissions(Permission.newBuilder().setGroupId("-1").setRoleId("-1").build())
              .build();
    } else {
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

      authServiceChannelServiceUser =
          ManagedChannelBuilder.forTarget(
                  testConfig.getAuthService().getHost()
                      + ":"
                      + testConfig.getAuthService().getPort())
              .usePlaintext()
              .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
              .intercept(authClientInterceptor.getServiceAccountClientAuthInterceptor())
              .usePlaintext()
              .executor(executor)
              .build();
      var superUserUacServiceStub = UACServiceGrpc.newBlockingStub(authServiceChannelServiceUser);

      GetUser getUserRequest =
          GetUser.newBuilder().setEmail(authClientInterceptor.getClient1Email()).build();
      // Get the user info by vertaId from the AuthService
      testUser1 = superUserUacServiceStub.getUser(getUserRequest);
      getUserRequest =
          GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
      testUser2 = superUserUacServiceStub.getUser(getUserRequest);
      getUserRequest =
          GetUser.newBuilder().setEmail(testConfig.getService_user().getEmail()).build();
      serviceAccountUser = superUserUacServiceStub.getUser(getUserRequest);

      organizationServiceV2BlockingStub =
          OrganizationServiceV2Grpc.newBlockingStub(authServiceChannelServiceUser);
      organizationV2 = createAndGetOrganization();
      organizationId = organizationV2.getId();

      addTestUsersInOrganization(authServiceChannelServiceUser, organizationId);

      groupIdUser1 = createAndGetGroup(authServiceChannelServiceUser, organizationId, testUser1);

      roleIdUser1 =
          createAndGetRole(
                  authServiceChannelServiceUser,
                  organizationId,
                  Optional.empty(),
                  Set.of(ResourceTypeV2.PROJECT, ResourceTypeV2.DATASET))
              .getRole()
              .getId();

      testUser1Workspace =
          createWorkspaceAndRoleForUser(
              authServiceChannelServiceUser,
              organizationId,
              groupIdUser1,
              roleIdUser1,
              testUser1.getVertaInfo().getUsername());
    }

    LOGGER.trace("Test service infrastructure config complete.");
  }

  protected OrganizationV2 createAndGetOrganization() {
    if (!isRunningIsolated()) {
      try {
        GetOrganizationByNameV2.Response existingVertaOrg =
            organizationServiceV2BlockingStub.getOrganizationByName(
                GetOrganizationByNameV2.newBuilder().setOrgName("Verta").build());
        return existingVertaOrg.getOrganization();
      } catch (Exception e) {
        LOGGER.info("Verta org does not exist...creating new organization to test with");
      }
    }

    var organizationResponse =
        organizationServiceV2BlockingStub.setOrganization(
            SetOrganizationV2.newBuilder()
                .setOrganization(
                    OrganizationV2.newBuilder()
                        .setName("modeldb-test-org" + new Date().getTime())
                        .addAdmins(
                            OrgAdminV2.newBuilder()
                                .setEmail(authClientInterceptor.getClient1Email())
                                .build())
                        .build())
                .build());
    return organizationResponse.getOrganization();
  }

  private void removeOrganizationFromUAC() {
    if (!organizationV2.getName().equals("Verta")) {
      organizationServiceV2BlockingStub.deleteOrganization(
          DeleteOrganizationV2.newBuilder().setOrgId(organizationId).build());
    }
  }

  private void addTestUsersInOrganization(
      ManagedChannel authServiceChannelServiceUser, String organizationId) {
    var userStub = UserServiceV2Grpc.newBlockingStub(authServiceChannelServiceUser);
    userStub.addUser(
        AddUserV2.newBuilder().setOrgId(organizationId).setEmail(testUser1.getEmail()).build());
    userStub.addUser(
        AddUserV2.newBuilder().setOrgId(organizationId).setEmail(testUser2.getEmail()).build());
  }

  protected String createAndGetGroup(
      ManagedChannel authServiceChannelServiceUser, String organizationId, UserInfo userInfo) {
    var groupStub = GroupServiceGrpc.newBlockingStub(authServiceChannelServiceUser);
    GroupV2 group =
        groupStub
            .setGroup(
                SetGroup.newBuilder()
                    .setGroup(
                        GroupV2.newBuilder()
                            .setName("Test-Group-" + new Date().getTime())
                            .setOrgId(organizationId)
                            .addMemberIds(userInfo.getVertaInfo().getUserId()))
                    .build())
            .getGroup();
    LOGGER.info("Created group : " + group);
    return group.getId();
  }

  private void deleteGroup(String organizationId, String groupId) {
    var groupStub = GroupServiceGrpc.newBlockingStub(authServiceChannelServiceUser);
    groupStub.deleteGroup(
        DeleteGroup.newBuilder().setGroupId(groupId).setOrgId(organizationId).build());
  }

  private void deleteWorkspace(String organizationId, long workspaceId) {
    WorkspaceServiceV2Grpc.newBlockingStub(authServiceChannelServiceUser)
        .deleteWorkspace(
            DeleteWorkspaceV2.newBuilder()
                .setWorkspaceId(workspaceId)
                .setOrgId(organizationId)
                .build());
  }

  private void deleteRole(String organizationId, String roleId) {
    var roleStub = RoleServiceV2Grpc.newBlockingStub(authServiceChannelServiceUser);
    roleStub.deleteRole(
        DeleteRoleV2.newBuilder().setRoleId(roleId).setOrgId(organizationId).build());
  }

  protected SetRoleV2.Response createAndGetRole(
      ManagedChannel authServiceChannelServiceUser,
      String organizationId,
      Optional<String> roleId,
      Set<ResourceTypeV2> resourceTypeV2s) {

    var roleBuilder =
        RoleV2.newBuilder().setName("Test-Role-" + new Date().getTime()).setOrgId(organizationId);

    roleId.ifPresent(roleBuilder::setId);
    resourceTypeV2s.forEach(
        resourceTypeV2 ->
            roleBuilder.addResourceActions(
                RoleResourceActions.newBuilder()
                    .setResourceType(resourceTypeV2)
                    .addAllowedActions(ActionTypeV2.UPDATE)
                    .addAllowedActions(ActionTypeV2.READ)
                    .addAllowedActions(ActionTypeV2.CREATE)
                    .addAllowedActions(ActionTypeV2.DELETE)
                    .build()));

    var roleStub = RoleServiceV2Grpc.newBlockingStub(authServiceChannelServiceUser);
    return roleStub.setRole(SetRoleV2.newBuilder().setRole(roleBuilder.build()).build());
  }

  protected WorkspaceV2 createWorkspaceAndRoleForUser(
      ManagedChannel authServiceChannelServiceUser,
      String organizationId,
      String groupId,
      String roleId,
      String username) {
    WorkspaceV2 workspaceRequest =
        WorkspaceV2.newBuilder()
            .setName(username)
            .setOrgId(organizationId)
            .setNamespace("namespace")
            .addPermissions(Permission.newBuilder().setGroupId(groupId).setRoleId(roleId).build())
            .build();
    var workspaceStub = WorkspaceServiceV2Grpc.newBlockingStub(authServiceChannelServiceUser);
    Optional<WorkspaceV2> existingWorkspace =
        workspaceStub
            .searchWorkspaces(SearchWorkspacesV2.newBuilder().setOrgId(organizationId).build())
            .getWorkspacesList()
            .stream()
            .filter(workspaceV2 -> workspaceV2.getName().equals(username))
            .findFirst();

    if (existingWorkspace.isPresent()) {
      WorkspaceV2 workspace = existingWorkspace.get();
      workspaceRequest =
          workspace.toBuilder()
              .addPermissions(Permission.newBuilder().setGroupId(groupId).setRoleId(roleId).build())
              .build();
    }

    var testUserWorkspace =
        workspaceStub
            .setWorkspace(SetWorkspaceV2.newBuilder().setWorkspace(workspaceRequest).build())
            .getWorkspace();
    LOGGER.debug("WorkspaceResult: {}", testUserWorkspace);
    return testUserWorkspace;
  }

  public String getWorkspaceNameUser1() {
    return testUser1Workspace.getOrgId() + "/" + testUser1Workspace.getName();
  }

  protected void setupMockUacEndpoints(UAC uac) {
    Context.current().withValue(MetadataForwarder.METADATA_INFO, new Metadata()).attach();

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
    when(authzBlockingMock.isSelfAllowed(any(IsSelfAllowed.class)))
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
    var getResources =
        GetResources.Response.newBuilder()
            .addItem(
                GetResourcesResponseItem.newBuilder()
                    .setVisibility(ResourceVisibility.PRIVATE)
                    .setResourceType(
                        ResourceType.newBuilder()
                            .setModeldbServiceResourceType(ModelDBServiceResourceTypes.PROJECT)
                            .build())
                    .setOwnerId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                    .setWorkspaceId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                    .build())
            .build();
    when(uac.getCollaboratorService().getResourcesSpecialPersonalWorkspace(any()))
        .thenReturn(Futures.immediateFuture(getResources));
    when(uac.getCollaboratorService().getResources(any()))
        .thenReturn(Futures.immediateFuture(getResources));
    when(uac.getServiceAccountRoleServiceFutureStub().setRoleBinding(any()))
        .thenReturn(Futures.immediateFuture(SetRoleBinding.Response.newBuilder().build()));
    when(organizationBlockingMock.listMyOrganizations(any()))
        .thenReturn(ListMyOrganizations.Response.newBuilder().build());
  }

  protected void cleanUpResources() {
    // Delete entities via reconcilers
    SoftDeleteProjects softDeleteProjects = reconcilerInitializer.getSoftDeleteProjects();
    SoftDeleteExperiments softDeleteExperiments = reconcilerInitializer.getSoftDeleteExperiments();
    SoftDeleteExperimentRuns softDeleteExperimentRuns =
        reconcilerInitializer.getSoftDeleteExperimentRuns();

    softDeleteProjects.resync();
    await().until(softDeleteProjects::isEmpty);

    softDeleteExperiments.resync();
    await().until(softDeleteExperiments::isEmpty);

    softDeleteExperimentRuns.resync();
    await().atMost(30, TimeUnit.SECONDS).until(softDeleteExperimentRuns::isEmpty);

    reconcilerInitializer.getSoftDeleteDatasets().resync();
    reconcilerInitializer.getSoftDeleteRepositories().resync();

    if (!runningIsolated) {
      removeOrganizationFromUAC();
      deleteGroup(organizationId, groupIdUser1);
      deleteWorkspace(organizationId, testUser1Workspace.getId());
      deleteRole(organizationId, roleIdUser1);
    }
  }

  protected void updateTimestampOfResources() throws Exception {
    var updateTimestampRepo = reconcilerInitializer.getUpdateRepositoryTimestampReconciler();
    updateTimestampRepo.resync();
    await().until(updateTimestampRepo::isEmpty);
    var updateTimestampExp = reconcilerInitializer.getUpdateExperimentTimestampReconciler();
    updateTimestampExp.resync();
    await().until(updateTimestampExp::isEmpty);
    var updateTimestampProject = reconcilerInitializer.getUpdateProjectTimestampReconciler();
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
                                .setVisibility(ResourceVisibility.PRIVATE)
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
    when(uac.getCollaboratorService().getResourcesSpecialPersonalWorkspace(any()))
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
