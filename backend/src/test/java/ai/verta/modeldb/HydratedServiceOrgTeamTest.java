package ai.verta.modeldb;

import static ai.verta.modeldb.CollaboratorTest.addCollaboratorRequestProjectInterceptor;
import static ai.verta.modeldb.utils.TestConstants.RESOURCE_OWNER_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ai.verta.common.CollaboratorTypeEnum.CollaboratorType;
import ai.verta.common.TernaryEnum;
import ai.verta.common.TernaryEnum.Ternary;
import ai.verta.modeldb.HydratedServiceGrpc.HydratedServiceBlockingStub;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorOrg;
import ai.verta.modeldb.collaborator.CollaboratorTeam;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.Action;
import ai.verta.uac.Actions;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.AddCollaboratorRequest.Response;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.CollaboratorServiceGrpc.CollaboratorServiceBlockingStub;
import ai.verta.uac.EntitiesEnum.EntitiesTypes;
import ai.verta.uac.GetCollaboratorResponse;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.Organization;
import ai.verta.uac.ServiceEnum.Service;
import ai.verta.uac.Team;
import ai.verta.uac.UserInfo;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HydratedServiceOrgTeamTest {

  private static final Logger LOGGER =
      LogManager.getLogger(HydratedServiceOrgTeamTest.class.getName());
  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;
  private ManagedChannel authServiceChannel = null;
  private static String serverName = InProcessServerBuilder.generateName();
  private static InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder channelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static String authHost;
  private static Integer authPort;
  private static AuthClientInterceptor authClientInterceptor;
  private static final String ORG_ID = "1234";
  private static final String TEAM_ID = "12";
  private static final String ORG_NAME = "org name";
  private static final String TEAM_NAME = "team_name";

  private AuthService authService = new AuthServiceUtils();
  @Mock private RoleService roleService;

  @SuppressWarnings("unchecked")
  @Before
  public void setServerAndService() throws Exception {

    Map<String, Object> propertiesMap =
        ModelDBUtils.readYamlProperties(System.getenv(ModelDBConstants.VERTA_MODELDB_CONFIG));
    Map<String, Object> testPropMap = (Map<String, Object>) propertiesMap.get("test");
    Map<String, Object> databasePropMap = (Map<String, Object>) testPropMap.get("test-database");

    App app = App.getInstance();

    Map<String, Object> authServicePropMap =
        (Map<String, Object>) propertiesMap.get(ModelDBConstants.AUTH_SERVICE);
    if (authServicePropMap != null) {
      String authServiceHost = (String) authServicePropMap.get(ModelDBConstants.HOST);
      Integer authServicePort = (Integer) authServicePropMap.get(ModelDBConstants.PORT);
      app.setAuthServerHost(authServiceHost);
      app.setAuthServerPort(authServicePort);
      authHost = authServiceHost;
      authPort = authServicePort;
    }

    App.initializeServicesBaseOnDataBase(
        serverBuilder, databasePropMap, propertiesMap, authService, roleService);
    serverBuilder.intercept(new ModelDBAuthInterceptor());

    Map<String, Object> testUerPropMap = (Map<String, Object>) testPropMap.get("testUsers");
    if (testUerPropMap != null && testUerPropMap.size() > 0) {
      authClientInterceptor = new AuthClientInterceptor(testPropMap);
      channelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
    }
  }

  @AfterClass
  public static void removeServerAndService() {
    App.initiateShutdown(0);
  }

  @After
  public void clientClose() {
    if (!channel.isShutdown()) {
      channel.shutdownNow();
    }
    if (!authServiceChannel.isShutdown()) {
      authServiceChannel.shutdownNow();
    }
  }

  @Before
  public void initializeChannel() throws IOException {
    grpcCleanup.register(serverBuilder.build().start());
    channel = grpcCleanup.register(channelBuilder.maxInboundMessageSize(1024).build());
    authServiceChannel =
        ManagedChannelBuilder.forTarget(authHost + ":" + authPort)
            .usePlaintext()
            .intercept(authClientInterceptor.getClient1AuthInterceptor())
            .build();
  }

  @Test
  public void findHydratedProjectsByOrganizationTest() {

    LOGGER.info("FindHydratedProjectsByOrganization test start................................");
    ProjectTest projectTest = new ProjectTest();
    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);
    Map<String, Project> projectMap = new HashMap<>();

    Organization org = Organization.newBuilder().setId(ORG_ID).setName(ORG_NAME).build();
    Mockito.when(roleService.getWorkspaceDTOByWorkspaceName(Mockito.any(), Mockito.anyString()))
        .thenAnswer(
            (t) -> {
              WorkspaceDTO workspaceDTO = new WorkspaceDTO();
              final String workspaceName = (String) t.getArguments()[1];
              workspaceDTO.setWorkspaceName(workspaceName);
              if (workspaceName.equals("")) {
                final UserInfo userInfo = (UserInfo) t.getArguments()[0];
                final boolean isCurrentUser =
                    authClientInterceptor.getClient1Email().equals(userInfo.getEmail());
                Assert.assertTrue("Wrong user", isCurrentUser);
                workspaceDTO.setWorkspaceId(userInfo.getVertaInfo().getUserId());
                workspaceDTO.setWorkspaceType(WorkspaceType.USER);
              } else {
                workspaceDTO.setWorkspaceId(org.getId());
                workspaceDTO.setWorkspaceType(WorkspaceType.ORGANIZATION);
              }
              return workspaceDTO;
            });
    Mockito.doReturn(org).when(roleService).getOrgById(org.getId());

    try {

      CreateProject createProjectRequest = projectTest.getCreateProjectRequest("Project_1");
      createProjectRequest = createProjectRequest.toBuilder().setWorkspaceName(org.getId()).build();
      CreateProject.Response createProjectResponse =
          projectServiceStub.createProject(createProjectRequest);
      Project project1 = createProjectResponse.getProject();
      projectMap.put(project1.getId(), project1);
      LOGGER.info("Project created successfully");
      assertEquals(
          "Project name not match with expected Project name",
          createProjectRequest.getName(),
          project1.getName());

      CreateProject createProjectRequest2 = projectTest.getCreateProjectRequest("Project_2");
      CreateProject.Response createProjectResponse2 =
          projectServiceStub.createProject(createProjectRequest2);
      Project project2 = createProjectResponse2.getProject();
      projectMap.put(project2.getId(), project2);
      LOGGER.info("Project created successfully");
      assertEquals(
          "Project name not match with expected Project name",
          createProjectRequest2.getName(),
          project2.getName());

      Mockito.doReturn(Collections.singletonList(project1.getId()))
          .when(roleService)
          .getAccessibleResourceIdsByActions(
              ModelDBServiceResourceTypes.PROJECT,
              ModelDBServiceActions.DELETE,
              Collections.singletonList(project1.getId()));
      Mockito.doReturn(Collections.singletonList(project2.getId()))
          .when(roleService)
          .getAccessibleResourceIdsByActions(
              ModelDBServiceResourceTypes.PROJECT,
              ModelDBServiceActions.DELETE,
              Collections.singletonList(project2.getId()));

      Map<String, Actions> actions = new HashMap<>();
      actions.put(
          project1.getId(),
          Actions.newBuilder()
              .addActions(
                  Action.newBuilder()
                      .setService(Service.MODELDB_SERVICE)
                      .setModeldbServiceAction(ModelDBServiceActions.UPDATE))
              .build());

      Mockito.doReturn(actions)
          .when(roleService)
          .getSelfAllowedActionsBatch(
              Collections.singletonList(project1.getId()), ModelDBServiceResourceTypes.PROJECT);
      actions = new HashMap<>();
      actions.put(
          project2.getId(),
          Actions.newBuilder()
              .addActions(
                  Action.newBuilder()
                      .setService(Service.MODELDB_SERVICE)
                      .setModeldbServiceAction(ModelDBServiceActions.UPDATE))
              .build());
      Mockito.doReturn(actions)
          .when(roleService)
          .getSelfAllowedActionsBatch(
              Collections.singletonList(project2.getId()), ModelDBServiceResourceTypes.PROJECT);

      CollaboratorServiceBlockingStub collaboratorServiceStub =
          CollaboratorServiceGrpc.newBlockingStub(channel);
      // For Collaborator1
      addCollaboratorRequestProjectInterceptor(
          project1, CollaboratorType.READ_WRITE, authClientInterceptor);
      LOGGER.info("Collaborator1 added successfully");
      Mockito.doReturn(org).when(roleService).getOrgById(ORG_ID);
      Team team = Team.newBuilder().setId(TEAM_ID).setOrgId(ORG_ID).setName(TEAM_NAME).build();
      Mockito.doReturn(team).when(roleService).getTeamById(TEAM_ID);
      AddCollaboratorRequest addCollaboratorRequestOrg =
          AddCollaboratorRequest.newBuilder()
              .setCanDeploy(TernaryEnum.Ternary.TRUE)
              .setShareWith(ORG_ID)
              .setAuthzEntityType(EntitiesTypes.ORGANIZATION)
              .addEntityIds(project1.getId())
              .setCollaboratorType(CollaboratorType.READ_WRITE)
              .build();
      Response collaboratorResponseOrg =
          collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequestOrg);
      assertEquals(ORG_ID, collaboratorResponseOrg.getCollaboratorOrganization().getId());
      FindHydratedProjectsByOrganization findHydratedProjectsByOrganization =
          FindHydratedProjectsByOrganization.newBuilder().setId(ORG_ID).build();
      AddCollaboratorRequest addCollaboratorRequestTeam =
          AddCollaboratorRequest.newBuilder()
              .setCanDeploy(Ternary.TRUE)
              .setShareWith(TEAM_ID)
              .setAuthzEntityType(EntitiesTypes.TEAM)
              .addEntityIds(project2.getId())
              .setCollaboratorType(CollaboratorType.READ_WRITE)
              .build();
      Response collaboratorResponseTeam =
          collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequestTeam);
      assertEquals(TEAM_ID, collaboratorResponseTeam.getCollaboratorTeam().getId());
      FindHydratedProjectsByTeam findHydratedProjectsByTeam =
          FindHydratedProjectsByTeam.newBuilder().setId(TEAM_ID).build();

      GetCollaboratorResponse.Builder response1 = GetCollaboratorResponse.newBuilder();
      GetCollaboratorResponse.Builder response2 = GetCollaboratorResponse.newBuilder();
      response1
          .setVertaId(org.getId())
          .setCollaboratorType(CollaboratorType.READ_WRITE)
          .setAuthzEntityType(EntitiesTypes.ORGANIZATION)
          .setCanDeploy(Ternary.TRUE);
      response2
          .setVertaId(team.getId())
          .setCollaboratorType(CollaboratorType.READ_WRITE)
          .setAuthzEntityType(EntitiesTypes.TEAM)
          .setCanDeploy(Ternary.TRUE);
      Mockito.doAnswer(
              (arg) -> {
                Assert.assertEquals(ModelDBServiceResourceTypes.PROJECT, arg.getArgument(0));
                Assert.assertEquals(RESOURCE_OWNER_ID, arg.getArgument(2));
                if (project1.getId().equals(arg.getArgument(1))) {
                  return Collections.singletonList(response1.build());
                } else if (project2.getId().equals(arg.getArgument(1))) {
                  return Collections.singletonList(response2.build());
                }
                Assert.fail("project id unknown");
                return null;
              })
          .when(roleService)
          .getResourceCollaborators(
              Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.any());
      Mockito.doReturn(Collections.singletonList(project1.getId()))
          .when(roleService)
          .getAccessibleResourceIds(
              new CollaboratorOrg(org),
              new CollaboratorUser(authService, (GeneratedMessageV3) null),
              ProjectVisibility.PRIVATE,
              ModelDBServiceResourceTypes.PROJECT,
              Collections.emptyList());
      Mockito.doReturn(Collections.singletonList(project2.getId()))
          .when(roleService)
          .getAccessibleResourceIds(
              new CollaboratorTeam(team),
              new CollaboratorUser(authService, (GeneratedMessageV3) null),
              ProjectVisibility.PRIVATE,
              ModelDBServiceResourceTypes.PROJECT,
              Collections.emptyList());

      AdvancedQueryProjectsResponse responseOrg =
          hydratedServiceBlockingStub.findHydratedProjectsByOrganization(
              findHydratedProjectsByOrganization);
      AdvancedQueryProjectsResponse responseTeam =
          hydratedServiceBlockingStub.findHydratedProjectsByTeam(findHydratedProjectsByTeam);
      LOGGER.info("FindProjectsOrg Response : " + responseOrg.getHydratedProjectsList());
      assertEquals(
          "Project count not match with expected project count",
          1,
          responseOrg.getHydratedProjectsList().size());
      LOGGER.info("FindProjectsTeam Response : " + responseTeam.getHydratedProjectsList());
      assertEquals(
          "Project count not match with expected project count",
          1,
          responseTeam.getHydratedProjectsList().size());

      assertEquals(
          1, responseOrg.getHydratedProjectsList().get(0).getCollaboratorUserInfosList().size());
      assertEquals(
          1, responseTeam.getHydratedProjectsList().get(0).getCollaboratorUserInfosList().size());
      assertEquals(
          CollaboratorUserInfo.newBuilder()
              .setEntityType(EntitiesTypes.ORGANIZATION)
              .setCollaboratorOrganization(org)
              .setCollaboratorType(CollaboratorType.READ_WRITE)
              .setCanDeploy(Ternary.TRUE)
              .build(),
          responseOrg.getHydratedProjectsList().get(0).getCollaboratorUserInfosList().get(0));
      assertEquals(
          CollaboratorUserInfo.newBuilder()
              .setEntityType(EntitiesTypes.TEAM)
              .setCollaboratorTeam(team)
              .setCollaboratorType(CollaboratorType.READ_WRITE)
              .setCanDeploy(Ternary.TRUE)
              .build(),
          responseTeam.getHydratedProjectsList().get(0).getCollaboratorUserInfosList().get(0));

      assertEquals(
          "Total records count not matched with expected records count",
          1,
          responseOrg.getTotalRecords());
      assertEquals(
          "Total records count not matched with expected records count",
          1,
          responseTeam.getTotalRecords());
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    } finally {
      for (String projectId : projectMap.keySet()) {
        DeleteProject deleteProject = DeleteProject.newBuilder().setId(projectId).build();
        DeleteProject.Response deleteProjectResponse =
            projectServiceStub.deleteProject(deleteProject);
        LOGGER.info("Project deleted successfully");
        LOGGER.info(deleteProjectResponse.toString());
        assertTrue(deleteProjectResponse.getStatus());
      }
    }
    LOGGER.info("FindHydratedProjectsByOrganization test stop................................");
  }
}
