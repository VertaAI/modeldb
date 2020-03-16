package ai.verta.modeldb;

import static ai.verta.modeldb.CollaboratorTest.addCollaboratorRequestProject;
import static ai.verta.modeldb.CollaboratorTest.addCollaboratorRequestProjectInterceptor;
import static org.junit.Assert.*;

import ai.verta.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub;
import ai.verta.modeldb.ExperimentServiceGrpc.ExperimentServiceBlockingStub;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.CollaboratorServiceGrpc.CollaboratorServiceBlockingStub;
import ai.verta.uac.CollaboratorTypeEnum;
import ai.verta.uac.GetUser;
import ai.verta.uac.UACServiceGrpc;
import ai.verta.uac.UACServiceGrpc.UACServiceBlockingStub;
import ai.verta.uac.UserInfo;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FindHydratedServiceTest {

  private static final Logger LOGGER =
      LogManager.getLogger(FindHydratedServiceTest.class.getName());

  private static String serverName = InProcessServerBuilder.generateName();
  private static InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder channelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder client2ChannelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static AuthClientInterceptor authClientInterceptor;
  private static AuthService authService;
  private static App app;

  // all service stubs
  private static UACServiceBlockingStub uacServiceStub;
  private static ProjectServiceBlockingStub projectServiceStub;
  private static ExperimentServiceBlockingStub experimentServiceStub;
  private static ExperimentRunServiceBlockingStub experimentRunServiceStub;
  private static CommentServiceGrpc.CommentServiceBlockingStub commentServiceBlockingStub;
  private static CollaboratorServiceBlockingStub collaboratorServiceStub;
  private static HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub;

  // Project Entities
  private static Project project1;
  private static Project project2;
  private static Map<String, Project> projectMap = new HashMap<>();

  // Experiment Entities
  private static Experiment experiment1;
  private static Experiment experiment2;

  // ExperimentRun Entities
  private static ExperimentRun experimentRun1;
  private static ExperimentRun experimentRun2;
  private static ExperimentRun experimentRun3;
  private static ExperimentRun experimentRun4;
  private static Map<String, ExperimentRun> experimentRunMap = new HashMap<>();

  @SuppressWarnings("unchecked")
  @BeforeClass
  public static void setServerAndService() throws Exception {

    Map<String, Object> propertiesMap =
        ModelDBUtils.readYamlProperties(System.getenv(ModelDBConstants.VERTA_MODELDB_CONFIG));
    Map<String, Object> testPropMap = (Map<String, Object>) propertiesMap.get("test");
    Map<String, Object> databasePropMap = (Map<String, Object>) testPropMap.get("test-database");

    app = App.getInstance();
    authService = new PublicAuthServiceUtils();
    RoleService roleService = new PublicRoleServiceUtils(authService);

    Map<String, Object> authServicePropMap =
        (Map<String, Object>) propertiesMap.get(ModelDBConstants.AUTH_SERVICE);
    if (authServicePropMap != null) {
      String authServiceHost = (String) authServicePropMap.get(ModelDBConstants.HOST);
      Integer authServicePort = (Integer) authServicePropMap.get(ModelDBConstants.PORT);
      app.setAuthServerHost(authServiceHost);
      app.setAuthServerPort(authServicePort);

      authService = new AuthServiceUtils();
      roleService = new RoleServiceUtils(authService);
    }

    App.initializeServicesBaseOnDataBase(
        serverBuilder, databasePropMap, propertiesMap, authService, roleService);
    serverBuilder.intercept(new ModelDBAuthInterceptor());

    Map<String, Object> testUerPropMap = (Map<String, Object>) testPropMap.get("testUsers");
    if (testUerPropMap != null && testUerPropMap.size() > 0) {
      authClientInterceptor = new AuthClientInterceptor(testPropMap);
      channelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
      client2ChannelBuilder.intercept(authClientInterceptor.getClient2AuthInterceptor());
    }

    serverBuilder.build().start();
    ManagedChannel channel = channelBuilder.maxInboundMessageSize(1024).build();
    ManagedChannel client2Channel = client2ChannelBuilder.maxInboundMessageSize(1024).build();
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      ManagedChannel authServiceChannel =
          ManagedChannelBuilder.forTarget(app.getAuthServerHost() + ":" + app.getAuthServerPort())
              .usePlaintext()
              .intercept(authClientInterceptor.getClient1AuthInterceptor())
              .build();
      uacServiceStub = UACServiceGrpc.newBlockingStub(authServiceChannel);
    }

    // Create all service blocking stub
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);
    commentServiceBlockingStub = CommentServiceGrpc.newBlockingStub(channel);
    collaboratorServiceStub = CollaboratorServiceGrpc.newBlockingStub(channel);
    hydratedServiceBlockingStub = HydratedServiceGrpc.newBlockingStub(channel);

    // Create all entities
    createProjectEntities();
    createExperimentEntities();
    createExperimentRun();
  }

  @AfterClass
  public static void removeServerAndService() {
    App.initiateShutdown(0);

    // Remove all entities
    removeEntities();

    // shutdown test server
    serverBuilder.build().shutdownNow();
  }

  private static void removeEntities() {
    // Delete all data related to project
    for (Project project : projectMap.values()) {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }
  }

  private static void createProjectEntities() {
    ProjectTest projectTest = new ProjectTest();

    // Create project1
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_1");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    project1 = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");

    // Create project2
    createProjectRequest = projectTest.getCreateProjectRequest("project_2");
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    project2 = createProjectResponse.getProject();
    LOGGER.info("Project2 created successfully");

    projectMap.put(project1.getId(), project1);
    projectMap.put(project2.getId(), project2);
  }

  private static void createExperimentEntities() {
    ExperimentTest experimentTest = new ExperimentTest();

    // Create two experiment of above project
    CreateExperiment request =
        experimentTest.getCreateExperimentRequest(project1.getId(), "Experiment1");
    CreateExperiment.Response response = experimentServiceStub.createExperiment(request);
    experiment1 = response.getExperiment();
    LOGGER.info("Experiment1 created successfully");
    request = experimentTest.getCreateExperimentRequest(project1.getId(), "Experiment2");
    response = experimentServiceStub.createExperiment(request);
    experiment2 = response.getExperiment();
    LOGGER.info("Experiment2 created successfully");
  }

  private static void createExperimentRun() {
    ExperimentRunTest experimentRunTest = new ExperimentRunTest();

    CreateExperimentRun createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project1.getId(), experiment1.getId(), "ExperiemntRun1");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun1 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun1 created successfully");
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project1.getId(), experiment1.getId(), "ExperiemntRun2");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun2 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun2 created successfully");

    // For ExperiemntRun of Experiment2
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project1.getId(), experiment2.getId(), "ExperiemntRun3");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun3 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun3 created successfully");
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project1.getId(), experiment2.getId(), "ExperimentRun4");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun4 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun4 created successfully");

    experimentRunMap.put(experimentRun1.getId(), experimentRun1);
    experimentRunMap.put(experimentRun2.getId(), experimentRun2);
    experimentRunMap.put(experimentRun3.getId(), experimentRun3);
    experimentRunMap.put(experimentRun4.getId(), experimentRun4);
  }

  /** FindHydratedProjects with single user collaborator */
  @Test
  public void findHydratedProjectsWithSingleUserCollaboratorTest() {
    LOGGER.info("FindHydratedProjects with single user collaborator test start............");

    // Create comment for above experimentRun1 & experimentRun3
    // comment for experiment1
    AddComment addCommentRequest =
        AddComment.newBuilder()
            .setEntityId(experimentRun1.getId())
            .setMessage(
                "Hello, this project is interesting." + Calendar.getInstance().getTimeInMillis())
            .build();
    commentServiceBlockingStub.addExperimentRunComment(addCommentRequest);
    LOGGER.info("Comment added successfully for ExperimentRun1");
    // comment for experimentRun3
    addCommentRequest =
        AddComment.newBuilder()
            .setEntityId(experimentRun3.getId())
            .setMessage(
                "Hello, this project is interesting." + Calendar.getInstance().getTimeInMillis())
            .build();
    commentServiceBlockingStub.addExperimentRunComment(addCommentRequest);
    LOGGER.info("Comment added successfully for ExperimentRun3");

    // For Collaborator1
    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestProjectInterceptor(
            project1, CollaboratorTypeEnum.CollaboratorType.READ_WRITE, authClientInterceptor);
    AddCollaboratorRequest.Response addCollaboratorResponse =
        collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator1 added successfully");

    GetHydratedProjects.Response getHydratedProjectsResponse =
        hydratedServiceBlockingStub.getHydratedProjects(GetHydratedProjects.newBuilder().build());

    assertEquals(
        "HydratedProjects count does not match with project count",
        projectMap.size(),
        getHydratedProjectsResponse.getHydratedProjectsCount());

    Map<String, HydratedProject> hydratedProjectMap = new HashMap<>();
    for (HydratedProject hydratedProject : getHydratedProjectsResponse.getHydratedProjectsList()) {
      Project project = hydratedProject.getProject();
      hydratedProjectMap.put(project.getId(), hydratedProject);

      if (project1.getId().equals(project.getId())) {
        assertEquals(
            "HydratedProjects collaborator count does not match with added collaborator count",
            1,
            hydratedProject.getCollaboratorUserInfosCount());
        assertEquals(
            hydratedProject.getCollaboratorUserInfosList().get(0).getCollaboratorUserInfo(),
            addCollaboratorResponse.getCollaboratorUserInfo());
      }
    }

    for (Project existingProject : projectMap.values()) {
      assertEquals(
          "Expected project does not exist in the hydrated projects",
          existingProject.getName(),
          hydratedProjectMap.get(existingProject.getId()).getProject().getName());
      assertEquals(
          "Expected project owner does not match with the hydratedProject owner",
          existingProject.getOwner(),
          authService.getVertaIdFromUserInfo(
              hydratedProjectMap.get(existingProject.getId()).getOwnerUserInfo()));
    }

    LOGGER.info("FindHydratedProjects with single user collaborator test stop............");
  }

  /** FindHydratedProjects with multiple user collaborator */
  @Test
  public void findHydratedProjectsWithMultipleUserCollaboratorTest() {
    LOGGER.info("FindHydratedProjects with multiple user collaborators test start............");

    // Create comment for above experimentRun1 & experimentRun3
    // comment for experiment1
    AddComment addCommentRequest =
        AddComment.newBuilder()
            .setEntityId(experimentRun1.getId())
            .setMessage(
                "Hello, this project is interesting." + Calendar.getInstance().getTimeInMillis())
            .build();
    commentServiceBlockingStub.addExperimentRunComment(addCommentRequest);
    LOGGER.info("Comment added successfully for ExperimentRun1");
    // comment for experimentRun3
    addCommentRequest =
        AddComment.newBuilder()
            .setEntityId(experimentRun3.getId())
            .setMessage(
                "Hello, this project is interesting." + Calendar.getInstance().getTimeInMillis())
            .build();
    commentServiceBlockingStub.addExperimentRunComment(addCommentRequest);
    LOGGER.info("Comment added successfully for ExperimentRun3");

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      GetUser getUserRequest =
          GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
      // Get the user info by vertaId form the AuthService
      UserInfo shareWithUserInfo = uacServiceStub.getUser(getUserRequest);

      // Create two collaborator for above project
      List<String> collaboratorUsers = new ArrayList<>();
      // For Collaborator1
      AddCollaboratorRequest addCollaboratorRequest =
          addCollaboratorRequestProject(
              project1,
              shareWithUserInfo.getEmail(),
              CollaboratorTypeEnum.CollaboratorType.READ_WRITE);
      collaboratorUsers.add(authService.getVertaIdFromUserInfo(shareWithUserInfo));
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      LOGGER.info("Collaborator1 added successfully");

      GetHydratedProjectById.Response getHydratedProjectResponse =
          hydratedServiceBlockingStub.getHydratedProjectById(
              GetHydratedProjectById.newBuilder().setId(project1.getId()).build());

      assertEquals(
          "HydratedProject does not match with expected project",
          project1.getName(),
          getHydratedProjectResponse.getHydratedProject().getProject().getName());

      assertEquals(
          "Expected project owner does not match with the hydratedProject owner",
          project1.getOwner(),
          authService.getVertaIdFromUserInfo(
              getHydratedProjectResponse.getHydratedProject().getOwnerUserInfo()));

      assertEquals(
          "Expected shared project user count does not match with the hydratedProject shared project user count",
          collaboratorUsers.size(),
          getHydratedProjectResponse.getHydratedProject().getCollaboratorUserInfosCount());

      LOGGER.info("existing project collaborator count: " + collaboratorUsers.size());
      for (String existingUserId : collaboratorUsers) {
        boolean match = false;
        for (CollaboratorUserInfo collaboratorUserInfo :
            getHydratedProjectResponse.getHydratedProject().getCollaboratorUserInfosList()) {
          if (existingUserId.equals(
              collaboratorUserInfo.getCollaboratorUserInfo().getVertaInfo().getUserId())) {
            LOGGER.info("existing project collborator : " + existingUserId);
            LOGGER.info(
                "Hydrated project collborator : "
                    + authService.getVertaIdFromUserInfo(
                        collaboratorUserInfo.getCollaboratorUserInfo()));
            match = true;
            break;
          }
        }
        if (!match) {
          LOGGER.warn("Hydrated collaborator user not match with existing collaborator user");
          fail();
        }
      }
    }

    LOGGER.info("FindHydratedProjects with multiple user collaborators test stop............");
  }
}
