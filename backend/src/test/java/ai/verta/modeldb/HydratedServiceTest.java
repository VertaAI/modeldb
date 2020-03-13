package ai.verta.modeldb;

import static ai.verta.modeldb.CollaboratorTest.addCollaboratorRequestProject;
import static ai.verta.modeldb.CollaboratorTest.addCollaboratorRequestProjectInterceptor;
import static org.junit.Assert.*;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.KeyValue;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub;
import ai.verta.modeldb.ExperimentServiceGrpc.ExperimentServiceBlockingStub;
import ai.verta.modeldb.OperatorEnum.Operator;
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
import ai.verta.uac.GetUser;
import ai.verta.uac.UACServiceGrpc;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HydratedServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(HydratedServiceTest.class.getName());
  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;
  private ManagedChannel client2Channel = null;
  private ManagedChannel authServiceChannel = null;
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
    if (!client2Channel.isShutdown()) {
      client2Channel.shutdownNow();
    }
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      if (!authServiceChannel.isShutdown()) {
        authServiceChannel.shutdownNow();
      }
    }
  }

  @Before
  public void initializeChannel() throws IOException {
    grpcCleanup.register(serverBuilder.build().start());
    channel = grpcCleanup.register(channelBuilder.maxInboundMessageSize(1024).build());
    client2Channel =
        grpcCleanup.register(client2ChannelBuilder.maxInboundMessageSize(1024).build());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      authServiceChannel =
          ManagedChannelBuilder.forTarget(app.getAuthServerHost() + ":" + app.getAuthServerPort())
              .usePlaintext()
              .intercept(authClientInterceptor.getClient1AuthInterceptor())
              .build();
    }
  }

  @Test
  public void a_getHydratedProjectsTest() {
    LOGGER.info("Get hydrated projects data test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();
    ExperimentRunTest experimentRunTest = new ExperimentRunTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    CommentServiceGrpc.CommentServiceBlockingStub commentServiceBlockingStub =
        CommentServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    List<Project> existingProjectList = new ArrayList<>();
    // Create project1
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_ypcdt_1");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    existingProjectList.add(project);
    LOGGER.info("Project created successfully");

    // Create project2
    createProjectRequest = projectTest.getCreateProjectRequest("project_ypcdt_2");
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    existingProjectList.add(project2);
    LOGGER.info("Project2 created successfully");

    // Create two experiment of above project
    CreateExperiment request =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment1");
    CreateExperiment.Response response = experimentServiceStub.createExperiment(request);
    Experiment experiment1 = response.getExperiment();
    LOGGER.info("Experiment1 created successfully");
    request = experimentTest.getCreateExperimentRequest(project.getId(), "Experiment2");
    response = experimentServiceStub.createExperiment(request);
    Experiment experiment2 = response.getExperiment();
    LOGGER.info("Experiment2 created successfully");

    Map<String, ExperimentRun> experimentRunMap = new HashMap<>();
    // Create four ExperimentRun of above two experiment, each experiment has two experimentRun
    // For ExperiemntRun of Experiment1
    CreateExperimentRun createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment1.getId(), "ExperiemntRun1");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun1 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun1.getId(), experimentRun1);
    LOGGER.info("ExperimentRun1 created successfully");
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment1.getId(), "ExperiemntRun2");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun2 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun2.getId(), experimentRun2);
    LOGGER.info("ExperimentRun2 created successfully");

    // For ExperiemntRun of Experiment2
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment2.getId(), "ExperiemntRun3");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun3 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun3.getId(), experimentRun3);
    LOGGER.info("ExperimentRun3 created successfully");
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment2.getId(), "ExperimentRun4");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun4 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun4.getId(), experimentRun4);
    LOGGER.info("ExperimentRun4 created successfully");

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
      // For Collaborator1
      AddCollaboratorRequest addCollaboratorRequest =
          addCollaboratorRequestProjectInterceptor(
              project, CollaboratorTypeEnum.CollaboratorType.READ_WRITE, authClientInterceptor);
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      LOGGER.info("Collaborator1 added successfully");
    }

    GetHydratedProjects.Response getHydratedProjectsResponse =
        hydratedServiceBlockingStub.getHydratedProjects(GetHydratedProjects.newBuilder().build());

    assertEquals(
        "HydratedProjects count does not match with project count",
        existingProjectList.size(),
        getHydratedProjectsResponse.getHydratedProjectsCount());

    Map<String, HydratedProject> hydratedProjectMap = new HashMap<>();
    for (HydratedProject hydratedProject : getHydratedProjectsResponse.getHydratedProjectsList()) {
      hydratedProjectMap.put(hydratedProject.getProject().getId(), hydratedProject);
    }

    for (Project existingProject : existingProjectList) {
      assertEquals(
          "Expected project does not exist in the hydrated projects",
          existingProject.getName(),
          hydratedProjectMap.get(existingProject.getId()).getProject().getName());
      assertEquals(
          "Expected project owner does not match with the hydratedProject owner",
          existingProject.getOwner(),
          authService.getVertaIdFromUserInfo(
              hydratedProjectMap.get(existingProject.getId()).getOwnerUserInfo()));

      // Delete all data related to project
      DeleteProject deleteProject =
          DeleteProject.newBuilder().setId(existingProject.getId()).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }

    LOGGER.info("Get hydrated projects data test stop................................");
  }

  @Test
  public void b_getHydratedProjectTest() {
    LOGGER.info("Get hydrated project data test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();
    ExperimentRunTest experimentRunTest = new ExperimentRunTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    CommentServiceGrpc.CommentServiceBlockingStub commentServiceBlockingStub =
        CommentServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    // Create project
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_ypcdt");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");

    // Create two experiment of above project
    CreateExperiment request =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment1");
    CreateExperiment.Response response = experimentServiceStub.createExperiment(request);
    Experiment experiment1 = response.getExperiment();
    LOGGER.info("Experiment1 created successfully");
    request = experimentTest.getCreateExperimentRequest(project.getId(), "Experiment2");
    response = experimentServiceStub.createExperiment(request);
    Experiment experiment2 = response.getExperiment();
    LOGGER.info("Experiment2 created successfully");

    Map<String, ExperimentRun> experimentRunMap = new HashMap<>();
    // Create four ExperimentRun of above two experiment, each experiment has two experimentRun
    // For ExperiemntRun of Experiment1
    CreateExperimentRun createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment1.getId(), "ExperiemntRun1");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun1 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun1.getId(), experimentRun1);
    LOGGER.info("ExperimentRun1 created successfully");
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment1.getId(), "ExperiemntRun2");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun2 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun2.getId(), experimentRun2);
    LOGGER.info("ExperimentRun2 created successfully");

    // For ExperiemntRun of Experiment2
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment2.getId(), "ExperiemntRun3");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun3 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun3.getId(), experimentRun3);
    LOGGER.info("ExperimentRun3 created successfully");
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment2.getId(), "ExperimentRun4");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun4 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun4.getId(), experimentRun4);
    LOGGER.info("ExperimentRun4 created successfully");

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
      UACServiceGrpc.UACServiceBlockingStub uaServiceStub =
          UACServiceGrpc.newBlockingStub(authServiceChannel);
      GetUser getUserRequest =
          GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
      // Get the user info by vertaId form the AuthService
      UserInfo shareWithUserInfo = uaServiceStub.getUser(getUserRequest);

      // Create two collaborator for above project
      List<String> collaboratorUsers = new ArrayList<>();
      // For Collaborator1
      AddCollaboratorRequest addCollaboratorRequest =
          addCollaboratorRequestProject(
              project,
              shareWithUserInfo.getEmail(),
              CollaboratorTypeEnum.CollaboratorType.READ_WRITE);
      collaboratorUsers.add(authService.getVertaIdFromUserInfo(shareWithUserInfo));
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      LOGGER.info("Collaborator1 added successfully");

      GetHydratedProjectById.Response getHydratedProjectResponse =
          hydratedServiceBlockingStub.getHydratedProjectById(
              GetHydratedProjectById.newBuilder().setId(project.getId()).build());

      assertEquals(
          "HydratedProject does not match with expected project",
          project.getName(),
          getHydratedProjectResponse.getHydratedProject().getProject().getName());

      assertEquals(
          "Expected project owner does not match with the hydratedProject owner",
          project.getOwner(),
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

    // Delete all data related to project
    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get hydrated project data test stop................................");
  }

  @Test
  public void a_getHydratedExperimentRunsTest() {
    LOGGER.info("Get hydrated ExperimentRuns data test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();
    ExperimentRunTest experimentRunTest = new ExperimentRunTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    CommentServiceGrpc.CommentServiceBlockingStub commentServiceBlockingStub =
        CommentServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    // Create project
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_ypcdt");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");

    // Create two experiment of above project
    CreateExperiment request =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment1");
    CreateExperiment.Response response = experimentServiceStub.createExperiment(request);
    Experiment experiment1 = response.getExperiment();
    LOGGER.info("Experiment1 created successfully");
    request = experimentTest.getCreateExperimentRequest(project.getId(), "Experiment2");
    response = experimentServiceStub.createExperiment(request);
    Experiment experiment2 = response.getExperiment();
    LOGGER.info("Experiment2 created successfully");

    Map<String, ExperimentRun> experimentRunMap = new HashMap<>();
    // Create four ExperimentRun of above two experiment, each experiment has two experimentRun
    // For ExperiemntRun of Experiment1
    CreateExperimentRun createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment1.getId(), "ExperiemntRun1");
    KeyValue metric1 =
        KeyValue.newBuilder()
            .setKey("loss")
            .setValue(Value.newBuilder().setNumberValue(0.012).build())
            .build();
    KeyValue metric2 =
        KeyValue.newBuilder()
            .setKey("accuracy")
            .setValue(Value.newBuilder().setNumberValue(0.99).build())
            .build();
    KeyValue hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("tuning")
            .setValue(Value.newBuilder().setNumberValue(9).build())
            .build();
    createExperimentRunRequest =
        createExperimentRunRequest
            .toBuilder()
            .setCodeVersion("4.0")
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun1 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun1.getId(), experimentRun1);
    LOGGER.info("ExperimentRun1 created successfully");
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment1.getId(), "ExperiemntRun2");
    metric1 =
        KeyValue.newBuilder()
            .setKey("loss")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    metric2 =
        KeyValue.newBuilder()
            .setKey("accuracy")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("tuning")
            .setValue(Value.newBuilder().setNumberValue(7).build())
            .build();
    createExperimentRunRequest =
        createExperimentRunRequest
            .toBuilder()
            .setCodeVersion("3.0")
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun2 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun2.getId(), experimentRun2);
    LOGGER.info("ExperimentRun2 created successfully");

    // For ExperiemntRun of Experiment2
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment2.getId(), "ExperiemntRun3");
    metric1 =
        KeyValue.newBuilder()
            .setKey("loss")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    metric2 =
        KeyValue.newBuilder()
            .setKey("accuracy")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("tuning")
            .setValue(Value.newBuilder().setNumberValue(4.55).build())
            .build();
    createExperimentRunRequest =
        createExperimentRunRequest
            .toBuilder()
            .setCodeVersion("2.0")
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun3 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun3.getId(), experimentRun3);
    LOGGER.info("ExperimentRun3 created successfully");
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment2.getId(), "ExperimentRun4");
    metric1 =
        KeyValue.newBuilder()
            .setKey("loss")
            .setValue(Value.newBuilder().setNumberValue(1.00).build())
            .build();
    metric2 =
        KeyValue.newBuilder()
            .setKey("accuracy")
            .setValue(Value.newBuilder().setNumberValue(0.001212).build())
            .build();
    hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("tuning")
            .setValue(Value.newBuilder().setNumberValue(2.545).build())
            .build();
    createExperimentRunRequest =
        createExperimentRunRequest
            .toBuilder()
            .setCodeVersion("1.0")
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .addTags("test_tag_123")
            .addTags("test_tag_456")
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun4 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun4.getId(), experimentRun4);
    LOGGER.info("ExperimentRun4 created successfully");

    // Create comment for above experimentRun1 & experimentRun3
    // comment for experiment1
    AddComment addCommentRequest =
        AddComment.newBuilder()
            .setEntityId(experimentRun1.getId())
            .setMessage(
                "Hello, this project is interesting." + Calendar.getInstance().getTimeInMillis())
            .build();
    AddComment.Response commentResponse =
        commentServiceBlockingStub.addExperimentRunComment(addCommentRequest);
    Comment experimentRun1Comment = commentResponse.getComment();
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
      // Create two collaborator for above project
      // For Collaborator1
      AddCollaboratorRequest addCollaboratorRequest =
          addCollaboratorRequestProjectInterceptor(
              project, CollaboratorTypeEnum.CollaboratorType.READ_WRITE, authClientInterceptor);
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      LOGGER.info("Collaborator1 added successfully");
    }

    int pageLimit = 2;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      GetHydratedExperimentRunsByProjectId.Response getHydratedExperimentRunsResponse =
          hydratedServiceBlockingStub.getHydratedExperimentRunsInProject(
              GetHydratedExperimentRunsByProjectId.newBuilder()
                  .setProjectId(project.getId())
                  .setPageNumber(pageNumber)
                  .setPageLimit(pageLimit)
                  .setAscending(true)
                  .setSortKey(ModelDBConstants.NAME)
                  .build());

      assertEquals(
          "HydratedExperimentRuns count does not match with existing ExperimentRun count",
          experimentRunMap.size(),
          getHydratedExperimentRunsResponse.getTotalRecords());

      if (getHydratedExperimentRunsResponse.getHydratedExperimentRunsList() != null
          && getHydratedExperimentRunsResponse.getHydratedExperimentRunsList().size() > 0) {
        isExpectedResultFound = true;
        for (HydratedExperimentRun hydratedExperimentRun :
            getHydratedExperimentRunsResponse.getHydratedExperimentRunsList()) {
          assertEquals(
              "ExperimentRun not match with expected experimentRun",
              experimentRunMap.get(hydratedExperimentRun.getExperimentRun().getId()),
              hydratedExperimentRun.getExperimentRun());

          if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
            assertEquals(
                "Expected experimentRun owner does not match with the hydratedExperimentRun owner",
                experimentRunMap.get(hydratedExperimentRun.getExperimentRun().getId()).getOwner(),
                authService.getVertaIdFromUserInfo(hydratedExperimentRun.getOwnerUserInfo()));

            if (hydratedExperimentRun.getExperimentRun().getName().equals("ExperiemntRun1")) {
              assertEquals(
                  "Expected experimentRun owner does not match with the hydratedExperimentRun owner",
                  Collections.singletonList(experimentRun1Comment),
                  hydratedExperimentRun.getCommentsList());
            }
          }
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More ExperimentRun not found in database");
          assertTrue(true);
        } else {
          fail("Expected experimentRun not found in response");
        }
        break;
      }
    }

    GetHydratedExperimentRunsByProjectId.Response getHydratedExperimentRunsResponse =
        hydratedServiceBlockingStub.getHydratedExperimentRunsInProject(
            GetHydratedExperimentRunsByProjectId.newBuilder()
                .setProjectId(project.getId())
                .setPageNumber(1)
                .setPageLimit(1)
                .setAscending(false)
                .setSortKey("metrics.loss")
                .build());

    assertEquals(
        "Total records count not matched with expected records count",
        4,
        getHydratedExperimentRunsResponse.getTotalRecords());
    assertEquals(
        "ExperimentRuns count not match with expected experimentRuns count",
        1,
        getHydratedExperimentRunsResponse.getHydratedExperimentRunsCount());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun4,
        getHydratedExperimentRunsResponse.getHydratedExperimentRuns(0).getExperimentRun());
    assertEquals(
        "Experiment name not match with expected Experiment name",
        experiment2.getName(),
        getHydratedExperimentRunsResponse.getHydratedExperimentRuns(0).getExperiment().getName());

    // Delete all data related to project
    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get hydrated ExperimentRuns data test stop................................");
  }

  @Test
  public void a_getHydratedExperimentRunByIdTest() {
    LOGGER.info("Get hydrated ExperimentRun By ID data test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();
    ExperimentRunTest experimentRunTest = new ExperimentRunTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    CommentServiceGrpc.CommentServiceBlockingStub commentServiceBlockingStub =
        CommentServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    // Create project
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_ypcdt");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");

    // Create two experiment of above project
    CreateExperiment request =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment1");
    CreateExperiment.Response response = experimentServiceStub.createExperiment(request);
    Experiment experiment1 = response.getExperiment();
    LOGGER.info("Experiment1 created successfully");
    request = experimentTest.getCreateExperimentRequest(project.getId(), "Experiment2");
    response = experimentServiceStub.createExperiment(request);
    Experiment experiment2 = response.getExperiment();
    LOGGER.info("Experiment2 created successfully");
    // Create four ExperimentRun of above two experiment, each experiment has two experimentRun
    // For ExperiemntRun of Experiment1
    CreateExperimentRun createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment1.getId(), "ExperiemntRun1");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun1 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun1 created successfully");
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment1.getId(), "ExperiemntRun2");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun2 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun2 created successfully");

    // For ExperiemntRun of Experiment2
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment2.getId(), "ExperiemntRun3");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun3 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun3 created successfully");
    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment2.getId(), "ExperimentRun4");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun4 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun4 created successfully");

    // Create comment for above experimentRun1 & experimentRun3
    // comment for experiment1
    AddComment addCommentRequest =
        AddComment.newBuilder()
            .setEntityId(experimentRun1.getId())
            .setMessage(
                "Hello, this project is interesting." + Calendar.getInstance().getTimeInMillis())
            .build();
    AddComment.Response addCommentResponse =
        commentServiceBlockingStub.addExperimentRunComment(addCommentRequest);
    Comment comment1 = addCommentResponse.getComment();
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

    // Create two collaborator for above project
    // For Collaborator1
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      AddCollaboratorRequest addCollaboratorRequest =
          addCollaboratorRequestProjectInterceptor(
              project, CollaboratorTypeEnum.CollaboratorType.READ_WRITE, authClientInterceptor);
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      LOGGER.info("Collaborator1 added successfully");
    }

    GetHydratedExperimentRunById.Response getHydratedExperimentRunsResponse =
        hydratedServiceBlockingStub.getHydratedExperimentRunById(
            GetHydratedExperimentRunById.newBuilder().setId(experimentRun1.getId()).build());

    assertEquals(
        "ExperimentRun not match with expected ExperimentRun",
        experimentRun1,
        getHydratedExperimentRunsResponse.getHydratedExperimentRun().getExperimentRun());

    assertEquals(
        "Experiment name not match with expected Experiment name",
        experiment1.getName(),
        getHydratedExperimentRunsResponse.getHydratedExperimentRun().getExperiment().getName());

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      assertEquals(
          "Hydrated comments not match with expected ExperimentRun comments",
          Collections.singletonList(comment1),
          getHydratedExperimentRunsResponse.getHydratedExperimentRun().getCommentsList());
    }

    // Delete all data related to project
    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get hydrated ExperimentRun By ID data test stop................................");
  }

  @Test
  public void getHydratedExperimentsTest() {
    LOGGER.info("Get hydrated Experiments data test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    // Create project
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_ypcdt");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");

    Map<String, Experiment> experimentMap = new HashMap<>();
    // Create two experiment of above project
    CreateExperiment request =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment1");
    CreateExperiment.Response response = experimentServiceStub.createExperiment(request);
    Experiment experiment1 = response.getExperiment();
    experimentMap.put(experiment1.getId(), experiment1);
    LOGGER.info("Experiment1 created successfully");
    request = experimentTest.getCreateExperimentRequest(project.getId(), "Experiment2");
    response = experimentServiceStub.createExperiment(request);
    Experiment experiment2 = response.getExperiment();
    experimentMap.put(experiment2.getId(), experiment2);
    LOGGER.info("Experiment2 created successfully");

    GetHydratedExperimentsByProjectId.Response getHydratedExperimentsResponse =
        hydratedServiceBlockingStub.getHydratedExperimentsByProjectId(
            GetHydratedExperimentsByProjectId.newBuilder().setProjectId(project.getId()).build());

    assertEquals(
        "HydratedExperiments count does not match with existing Experiment count",
        experimentMap.size(),
        getHydratedExperimentsResponse.getHydratedExperimentsCount());

    Map<String, HydratedExperiment> hydratedExperimentMap = new HashMap<>();
    for (HydratedExperiment hydratedExperiment :
        getHydratedExperimentsResponse.getHydratedExperimentsList()) {
      hydratedExperimentMap.put(hydratedExperiment.getExperiment().getId(), hydratedExperiment);
    }

    for (Experiment experiment : experimentMap.values()) {
      assertEquals(
          "Expected experimentRun not exist in the hydrated experimentRun",
          experiment,
          hydratedExperimentMap.get(experiment.getId()).getExperiment());
      assertEquals(
          "Expected experimentRun owner not match with the hydratedExperimentRun owner",
          experiment.getOwner(),
          authService.getVertaIdFromUserInfo(
              hydratedExperimentMap.get(experiment.getId()).getOwnerUserInfo()));
    }

    // Delete all data related to project
    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get hydrated ExperimentRuns data test stop................................");
  }

  @Test
  public void findHydratedExperimentRunsTest() {
    LOGGER.info("FindHydratedExperimentRuns test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();
    ExperimentRunTest experimentRunTest = new ExperimentRunTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    // Create project
    CreateProject createProjectRequest =
        projectTest.getCreateProjectRequest("experimentRun_project_ypcdt1");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project.getName());

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_sprt_abc_1");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment1 = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment1.getName());

    Map<String, ExperimentRun> experimentRunMap = new HashMap<>();

    CreateExperimentRun createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment1.getId(), "ExperimentRun_sprt_1");
    KeyValue metric1 =
        KeyValue.newBuilder()
            .setKey("loss")
            .setValue(Value.newBuilder().setNumberValue(0.012).build())
            .build();
    KeyValue metric2 =
        KeyValue.newBuilder()
            .setKey("accuracy")
            .setValue(Value.newBuilder().setNumberValue(0.99).build())
            .build();
    KeyValue hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("tuning")
            .setValue(Value.newBuilder().setNumberValue(9).build())
            .build();
    createExperimentRunRequest =
        createExperimentRunRequest
            .toBuilder()
            .setCodeVersion("4.0")
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun11 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun11.getId(), experimentRun11);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun11.getName());

    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment1.getId(), "ExperimentRun_sprt_2");
    metric1 =
        KeyValue.newBuilder()
            .setKey("loss")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    metric2 =
        KeyValue.newBuilder()
            .setKey("accuracy")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("tuning")
            .setValue(Value.newBuilder().setNumberValue(7).build())
            .build();
    createExperimentRunRequest =
        createExperimentRunRequest
            .toBuilder()
            .setCodeVersion("3.0")
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun12 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun12.getId(), experimentRun12);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun12.getName());

    // experiment2 of above project
    createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_sprt_abc_2");
    createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment2 = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment2.getName());

    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment2.getId(), "ExperimentRun_sprt_2");
    metric1 =
        KeyValue.newBuilder()
            .setKey("loss")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    metric2 =
        KeyValue.newBuilder()
            .setKey("accuracy")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("tuning")
            .setValue(Value.newBuilder().setNumberValue(4.55).build())
            .build();
    createExperimentRunRequest =
        createExperimentRunRequest
            .toBuilder()
            .setCodeVersion("2.0")
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun21 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun21.getId(), experimentRun21);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun21.getName());

    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment2.getId(), "ExperimentRun_sprt_1");
    metric1 =
        KeyValue.newBuilder()
            .setKey("loss")
            .setValue(Value.newBuilder().setNumberValue(1.00).build())
            .build();
    metric2 =
        KeyValue.newBuilder()
            .setKey("accuracy")
            .setValue(Value.newBuilder().setNumberValue(0.001212).build())
            .build();
    hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("tuning")
            .setValue(Value.newBuilder().setNumberValue(2.545).build())
            .build();
    createExperimentRunRequest =
        createExperimentRunRequest
            .toBuilder()
            .setCodeVersion("1.0")
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .addTags("test_tag_123")
            .addTags("test_tag_456")
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun22 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun22.getId(), experimentRun22);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun22.getName());

    // Validate check for predicate value not empty
    List<KeyValueQuery> predicates = new ArrayList<>();
    Value stringValueType = Value.newBuilder().setStringValue("").build();

    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("metrics.loss")
            .setValue(stringValueType)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    FindExperimentRuns findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .setExperimentId(experiment1.getId())
            .addAllPredicates(predicates)
            // .setIdsOnly(true)
            .build();
    try {
      hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // If key is not set in predicate
    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .setExperimentId(experiment1.getId())
            .addPredicates(
                KeyValueQuery.newBuilder()
                    .setValue(Value.newBuilder().setNumberValue(11).build())
                    .build())
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // Validate check for struct Type not implemented
    predicates = new ArrayList<>();
    Value numValue = Value.newBuilder().setNumberValue(17.1716586149719).build();

    Struct.Builder struct = Struct.newBuilder();
    struct.putFields("number_value", numValue);
    struct.build();
    Value structValue = Value.newBuilder().setStructValue(struct).build();

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("metrics.loss")
            .setValue(structValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .setExperimentId(experiment1.getId())
            .addAllPredicates(predicates)
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    // get experimentRun with value of metrics.loss <= 0.6543210
    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("metrics.loss")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQuery)
            .build();

    AdvancedQueryExperimentRunsResponse response =
        hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
    LOGGER.info("FindExperimentRuns Response : " + response.getHydratedExperimentRunsCount());
    List<ExperimentRun> experimentRuns = new ArrayList<>();
    for (HydratedExperimentRun hydratedExperimentRun : response.getHydratedExperimentRunsList()) {
      experimentRuns.add(hydratedExperimentRun.getExperimentRun());
    }
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        3,
        experimentRuns.size());

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());

    for (ExperimentRun fetchedExperimentRun : experimentRuns) {
      boolean doesMetricExist = false;
      for (KeyValue fetchedMetric : fetchedExperimentRun.getMetricsList()) {
        if (fetchedMetric.getKey().equals("loss")) {
          doesMetricExist = true;
          assertTrue(
              "ExperimentRun metrics.loss not match with expected experimentRun metrics.loss",
              fetchedMetric.getValue().getNumberValue() <= 0.6543210);
        }
      }
      if (!doesMetricExist) {
        fail("Expected metric not found in fetched metrics");
      }
    }

    // get experimentRun with value of metrics.loss <= 0.6543210 & metrics.accuracy == 0.31
    predicates = new ArrayList<>();
    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("metrics.loss")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setNumberValue(0.31).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("metrics.accuracy")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery2);

    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .setExperimentId(experiment1.getId())
            .addAllPredicates(predicates)
            .setIdsOnly(true)
            .build();

    response = hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
    LOGGER.info("FindExperimentRuns Response : " + response.getHydratedExperimentRunsCount());
    experimentRuns = new ArrayList<>();
    for (HydratedExperimentRun hydratedExperimentRun : response.getHydratedExperimentRunsList()) {
      experimentRuns.add(hydratedExperimentRun.getExperimentRun());
    }
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        1,
        experimentRuns.size());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun12.getId(),
        experimentRuns.get(0).getId());
    assertNotEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun12,
        experimentRuns.get(0));
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    // get experimentRun with value of metrics.accuracy >= 0.6543210 & hyperparameters.tuning ==
    // 2.545
    predicates = new ArrayList<>();
    numValue = Value.newBuilder().setNumberValue(2.545).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("hyperparameters.tuning")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("metrics.loss")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.GTE)
            .build();
    predicates.add(keyValueQuery2);

    List<String> experimentRunIds = new ArrayList<>();
    experimentRunIds.add(experimentRun11.getId());
    experimentRunIds.add(experimentRun12.getId());
    experimentRunIds.add(experimentRun21.getId());
    experimentRunIds.add(experimentRun22.getId());

    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .addAllExperimentRunIds(experimentRunIds)
            .addAllPredicates(predicates)
            .build();

    response = hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
    LOGGER.info("FindExperimentRuns Response : " + response.getHydratedExperimentRunsCount());
    experimentRuns = new ArrayList<>();
    for (HydratedExperimentRun hydratedExperimentRun : response.getHydratedExperimentRunsList()) {
      experimentRuns.add(hydratedExperimentRun.getExperimentRun());
    }
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        1,
        experimentRuns.size());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun22.getId(),
        experimentRuns.get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());
    assertEquals(
        "Expected experimentRun owner not match with the hydratedExperimentRun owner",
        experimentRun22.getOwner(),
        authService.getVertaIdFromUserInfo(
            response.getHydratedExperimentRunsList().get(0).getOwnerUserInfo()));

    // get experimentRun with value of endTime == 1550837
    Value stringValue =
        Value.newBuilder().setStringValue(String.valueOf(experimentRun22.getEndTime())).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("end_time")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQuery)
            .build();

    response = hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
    LOGGER.info("FindExperimentRuns Response : " + response.getHydratedExperimentRunsCount());
    experimentRuns = new ArrayList<>();
    for (HydratedExperimentRun hydratedExperimentRun : response.getHydratedExperimentRunsList()) {
      experimentRuns.add(hydratedExperimentRun.getExperimentRun());
    }
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        1,
        experimentRuns.size());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun22.getId(),
        experimentRuns.get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());
    assertEquals(
        "Expected experimentRun owner not match with the hydratedExperimentRun owner",
        experimentRun22.getOwner(),
        authService.getVertaIdFromUserInfo(
            response.getHydratedExperimentRunsList().get(0).getOwnerUserInfo()));

    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("metrics.loss")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    int pageLimit = 2;
    int count = 0;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setProjectId(project.getId())
              .addPredicates(keyValueQuery2)
              .setPageLimit(pageLimit)
              .setPageNumber(pageNumber)
              .setAscending(true)
              .setSortKey("code_version")
              .build();

      response = hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
      LOGGER.info("FindExperimentRuns Response : " + response.getHydratedExperimentRunsCount());
      experimentRuns = new ArrayList<>();
      for (HydratedExperimentRun hydratedExperimentRun : response.getHydratedExperimentRunsList()) {
        experimentRuns.add(hydratedExperimentRun.getExperimentRun());
      }

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          response.getTotalRecords());

      if (!experimentRuns.isEmpty()) {
        isExpectedResultFound = true;
        for (int index = 0; index < experimentRuns.size(); index++) {
          ExperimentRun experimentRun = experimentRuns.get(index);
          assertEquals(
              "ExperimentRun not match with expected experimentRun",
              experimentRunMap.get(experimentRun.getId()),
              experimentRun);

          String responseUsername =
              authService.getVertaIdFromUserInfo(
                  response.getHydratedExperimentRunsList().get(index).getOwnerUserInfo());
          if (count == 0) {
            assertEquals(
                "ExperimentRun code version not match with expected experimentRun code version",
                experimentRun21.getCodeVersion(),
                experimentRun.getCodeVersion());
            assertEquals(
                "Expected experimentRun owner not match with the hydratedExperimentRun owner",
                experimentRun21.getOwner(),
                responseUsername);
          } else if (count == 1) {
            assertEquals(
                "ExperimentRun code version not match with expected experimentRun code version",
                experimentRun12.getCodeVersion(),
                experimentRun.getCodeVersion());
            assertEquals(
                "Expected experimentRun owner not match with the hydratedExperimentRun owner",
                experimentRun12.getOwner(),
                responseUsername);
          } else if (count == 2) {
            assertEquals(
                "ExperimentRun code version not match with expected experimentRun code version",
                experimentRun11.getCodeVersion(),
                experimentRun.getCodeVersion());
            assertEquals(
                "Expected experimentRun owner not match with the hydratedExperimentRun owner",
                experimentRun11.getOwner(),
                responseUsername);
          }
          count++;
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More ExperimentRun not found in database");
          assertTrue(true);
        } else {
          fail("Expected experimentRun not found in response");
        }
        break;
      }
    }

    pageLimit = 2;
    count = 0;
    isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setProjectId(project.getId())
              .addPredicates(keyValueQuery2)
              .setPageLimit(pageLimit)
              .setPageNumber(pageNumber)
              .setAscending(false)
              .setSortKey("hyperparameters.tuning")
              .build();

      response = hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
      LOGGER.info("FindExperimentRuns Response : " + response.getHydratedExperimentRunsCount());
      experimentRuns = new ArrayList<>();
      for (HydratedExperimentRun hydratedExperimentRun : response.getHydratedExperimentRunsList()) {
        experimentRuns.add(hydratedExperimentRun.getExperimentRun());
      }

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          response.getTotalRecords());

      if (!experimentRuns.isEmpty()) {
        isExpectedResultFound = true;
        for (int index = 0; index < experimentRuns.size(); index++) {
          ExperimentRun experimentRun = experimentRuns.get(index);
          assertEquals(
              "ExperimentRun not match with expected experimentRun",
              experimentRunMap.get(experimentRun.getId()),
              experimentRun);

          String responseUsername =
              authService.getVertaIdFromUserInfo(
                  response.getHydratedExperimentRunsList().get(index).getOwnerUserInfo());
          if (count == 0) {
            assertEquals(
                "ExperimentRun hyperparameter not match with expected experimentRun hyperparameter",
                experimentRun11.getHyperparametersList(),
                experimentRun.getHyperparametersList());
            assertEquals(
                "Expected experimentRun owner not match with the hydratedExperimentRun owner",
                experimentRun11.getOwner(),
                responseUsername);
          } else if (count == 1) {
            assertEquals(
                "ExperimentRun hyperparameter not match with expected experimentRun hyperparameter",
                experimentRun12.getHyperparametersList(),
                experimentRun.getHyperparametersList());
            assertEquals(
                "Expected experimentRun owner not match with the hydratedExperimentRun owner",
                experimentRun12.getOwner(),
                responseUsername);
          } else if (count == 2) {
            assertEquals(
                "ExperimentRun hyperparameter not match with expected experimentRun hyperparameter",
                experimentRun21.getHyperparametersList(),
                experimentRun.getHyperparametersList());
            assertEquals(
                "Expected experimentRun owner not match with the hydratedExperimentRun owner",
                experimentRun21.getOwner(),
                responseUsername);
          }
          count++;
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More ExperimentRun not found in database");
          assertTrue(true);
        } else {
          fail("Expected experimentRun not found in response");
        }
        break;
      }
    }

    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQuery2)
            .setAscending(false)
            .setSortKey("observations.attribute.attr_1")
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    // get experimentRun with value of tags == test_tag_123
    Value stringValue1 = Value.newBuilder().setStringValue("test_tag_123").build();
    KeyValueQuery keyValueQueryTag1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue1)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    // get experimentRun with value of tags == test_tag_456
    Value stringValue2 = Value.newBuilder().setStringValue("test_tag_456").build();
    KeyValueQuery keyValueQueryTag2 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue2)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQueryTag1)
            .addPredicates(keyValueQueryTag2)
            .build();

    response = hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
    LOGGER.info("FindExperimentRuns Response : " + response.getHydratedExperimentRunsCount());
    experimentRuns = new ArrayList<>();
    for (HydratedExperimentRun hydratedExperimentRun : response.getHydratedExperimentRunsList()) {
      experimentRuns.add(hydratedExperimentRun.getExperimentRun());
    }
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        1,
        experimentRuns.size());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun22.getId(),
        experimentRuns.get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());
    assertEquals(
        "Expected experimentRun owner not match with the hydratedExperimentRun owner",
        experimentRun22.getOwner(),
        authService.getVertaIdFromUserInfo(
            response.getHydratedExperimentRunsList().get(0).getOwnerUserInfo()));

    Value numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQueryLoss =
        KeyValueQuery.newBuilder()
            .setKey("metrics.loss")
            .setValue(numValueLoss)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQueryLoss)
            .setAscending(false)
            .setSortKey("metrics.loss")
            .build();

    response = hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
    LOGGER.info("FindExperimentRuns Response : " + response.getHydratedExperimentRunsCount());
    experimentRuns = new ArrayList<>();
    for (HydratedExperimentRun hydratedExperimentRun : response.getHydratedExperimentRunsList()) {
      experimentRuns.add(hydratedExperimentRun.getExperimentRun());
    }

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        3,
        experimentRuns.size());

    KeyValueQuery keyValueQueryAccuracy =
        KeyValueQuery.newBuilder()
            .setKey("metrics.accuracy")
            .setValue(Value.newBuilder().setNumberValue(0.654321).build())
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQueryLoss)
            .addPredicates(keyValueQueryAccuracy)
            .setAscending(false)
            .setSortKey("metrics.loss")
            .build();
    response = hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
    LOGGER.info("FindExperimentRuns Response : " + response.getHydratedExperimentRunsCount());
    experimentRuns = new ArrayList<>();
    for (HydratedExperimentRun hydratedExperimentRun : response.getHydratedExperimentRunsList()) {
      experimentRuns.add(hydratedExperimentRun.getExperimentRun());
    }

    assertEquals(
        "Total records count not matched with expected records count",
        2,
        response.getTotalRecords());
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        2,
        experimentRuns.size());

    numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQueryLoss =
        KeyValueQuery.newBuilder()
            .setKey("metrics.loss")
            .setValue(numValueLoss)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQueryLoss)
            .setAscending(false)
            .setIdsOnly(true)
            .setSortKey("metrics.loss")
            .build();

    response = hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
    LOGGER.info("FindExperimentRuns Response : " + response.getHydratedExperimentRunsCount());
    experimentRuns = new ArrayList<>();
    for (HydratedExperimentRun hydratedExperimentRun : response.getHydratedExperimentRunsList()) {
      experimentRuns.add(hydratedExperimentRun.getExperimentRun());
    }

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        3,
        experimentRuns.size());

    for (int index = 0; index < experimentRuns.size(); index++) {
      ExperimentRun experimentRun = experimentRuns.get(index);
      if (index == 0) {
        assertNotEquals(
            "ExperimentRun not match with expected experimentRun", experimentRun21, experimentRun);
        assertEquals(
            "ExperimentRun Id not match with expected experimentRun Id",
            experimentRun21.getId(),
            experimentRun.getId());
      } else if (index == 1) {
        assertNotEquals(
            "ExperimentRun not match with expected experimentRun", experimentRun12, experimentRun);
        assertEquals(
            "ExperimentRun Id not match with expected experimentRun Id",
            experimentRun12.getId(),
            experimentRun.getId());
      } else if (index == 2) {
        assertNotEquals(
            "ExperimentRun not match with expected experimentRun", experimentRun11, experimentRun);
        assertEquals(
            "ExperimentRun Id not match with expected experimentRun Id",
            experimentRun11.getId(),
            experimentRun.getId());
      }
    }

    keyValueQueryLoss =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.ID)
            .setValue(Value.newBuilder().setStringValue("xyz").build())
            .setOperator(Operator.EQ)
            .build();

    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQueryLoss)
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    keyValueQueryLoss =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.ID)
            .setValue(Value.newBuilder().setStringValue("xyz").build())
            .setOperator(Operator.NE)
            .build();

    findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQueryLoss)
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedExperimentRuns(findExperimentRuns);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("FindHydratedExperimentRuns test stop................................");
  }

  @Test
  public void getHydratedExperimentsWithPaginationInProject() {
    LOGGER.info(
        "Get Experiment with pagination of project test start................................");

    ExperimentTest experimentTest = new ExperimentTest();
    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    // Create project
    ProjectTest projectTest = new ProjectTest();
    CreateProject createProjectRequest =
        projectTest.getCreateProjectRequest("experiment_project_n_sprt_abc");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project.getName());

    Map<String, Experiment> experimentMap = new HashMap<>();

    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_sprt_abc_1");
    Value intValue = Value.newBuilder().setNumberValue(12345).build();
    KeyValue keyValue1 =
        KeyValue.newBuilder()
            .setKey("attribute_2_2")
            .setValue(intValue)
            .setValueType(ValueTypeEnum.ValueType.NUMBER)
            .build();
    createExperimentRequest = createExperimentRequest.toBuilder().addAttributes(keyValue1).build();
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment1 = createExperimentResponse.getExperiment();
    experimentMap.put(experiment1.getId(), experiment1);
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        createExperimentResponse.getExperiment().getName());

    createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_sprt_abc_2");
    intValue = Value.newBuilder().setNumberValue(9876543).build();
    KeyValue keyValue2 =
        KeyValue.newBuilder()
            .setKey("attribute_2_2")
            .setValue(intValue)
            .setValueType(ValueTypeEnum.ValueType.NUMBER)
            .build();
    createExperimentRequest = createExperimentRequest.toBuilder().addAttributes(keyValue2).build();
    createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment2 = createExperimentResponse.getExperiment();
    experimentMap.put(experiment2.getId(), experiment2);
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        createExperimentResponse.getExperiment().getName());

    int pageLimit = 2;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      GetHydratedExperimentsByProjectId getExperiment =
          GetHydratedExperimentsByProjectId.newBuilder()
              .setProjectId(project.getId())
              .setPageNumber(pageNumber)
              .setPageLimit(pageLimit)
              .setAscending(true)
              .setSortKey(ModelDBConstants.NAME)
              .build();

      GetHydratedExperimentsByProjectId.Response experimentResponse =
          hydratedServiceBlockingStub.getHydratedExperimentsByProjectId(getExperiment);

      assertEquals(
          "Total records count not matched with expected records count",
          2,
          experimentResponse.getTotalRecords());

      List<Experiment> experimentList = new ArrayList<>();
      for (HydratedExperiment hydratedExperiment :
          experimentResponse.getHydratedExperimentsList()) {
        experimentList.add(hydratedExperiment.getExperiment());
      }

      if (!experimentList.isEmpty()) {
        isExpectedResultFound = true;
        LOGGER.info("GetExperimentsInProject Response : " + experimentList.size());
        for (Experiment experiment : experimentList) {
          assertEquals(
              "Experiment not match with expected Experiment",
              experimentMap.get(experiment.getId()),
              experiment);
        }

      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More Experiment not found in database");
          assertTrue(true);
        } else {
          fail("Expected experiment not found in response");
        }
        break;
      }
    }

    pageLimit = 1;
    int count = 0;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      GetHydratedExperimentsByProjectId getExperiment =
          GetHydratedExperimentsByProjectId.newBuilder()
              .setProjectId(project.getId())
              .setPageNumber(pageNumber)
              .setPageLimit(pageLimit)
              .setAscending(true)
              .setSortKey("attributes.attribute_2_2")
              .build();

      GetHydratedExperimentsByProjectId.Response experimentResponse =
          hydratedServiceBlockingStub.getHydratedExperimentsByProjectId(getExperiment);

      List<Experiment> experimentList = new ArrayList<>();
      for (HydratedExperiment hydratedExperiment :
          experimentResponse.getHydratedExperimentsList()) {
        experimentList.add(hydratedExperiment.getExperiment());
      }

      assertEquals(
          "Total records count not matched with expected records count",
          2,
          experimentResponse.getTotalRecords());

      if (!experimentList.isEmpty()) {

        LOGGER.info("GetExperimentsInProject Response : " + experimentList.size());
        for (Experiment experiment : experimentList) {
          assertEquals(
              "Experiment not match with expected Experiment",
              experimentMap.get(experiment.getId()),
              experiment);

          if (count == 0) {
            assertEquals(
                "ExperimentRun code version not match with expected experimentRun code version",
                experiment1.getAttributesList(),
                experiment.getAttributesList());
          } else if (count == 1) {
            assertEquals(
                "ExperimentRun code version not match with expected experimentRun code version",
                experiment2.getAttributesList(),
                experiment.getAttributesList());
          }
          count++;
        }

      } else {
        LOGGER.warn("More Experiment not found in database");
        assertTrue(true);
        break;
      }
    }

    GetHydratedExperimentsByProjectId getExperiment =
        GetHydratedExperimentsByProjectId.newBuilder()
            .setProjectId(project.getId())
            .setPageNumber(1)
            .setPageLimit(1)
            .setAscending(true)
            .setSortKey("observations.attribute.attr_1")
            .build();
    try {
      hydratedServiceBlockingStub.getHydratedExperimentsByProjectId(getExperiment);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info(
        "Get Experiment with pagination of project test stop................................");
  }

  @Test
  public void findHydratedExperimentsTest() {
    LOGGER.info("FindHydratedExperiments test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    // Create project
    CreateProject createProjectRequest =
        projectTest.getCreateProjectRequest("experimentRun_project_ypcdt1");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project.getName());

    Map<String, Experiment> experimentMap = new HashMap<>();

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_sprt_abc_1");
    KeyValue attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.012).build())
            .build();
    KeyValue attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.99).build())
            .build();
    createExperimentRequest =
        createExperimentRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_2")
            .build();
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment1 = createExperimentResponse.getExperiment();
    experimentMap.put(experiment1.getId(), experiment1);
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment1.getName());

    // experiment2 of above project
    createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_sprt_abc_2");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    createExperimentRequest =
        createExperimentRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_3")
            .addTags("Tag_4")
            .build();
    createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment2 = createExperimentResponse.getExperiment();
    experimentMap.put(experiment2.getId(), experiment2);
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment2.getName());

    // experiment3 of above project
    createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_sprt_abc_3");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    createExperimentRequest =
        createExperimentRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_5")
            .addTags("Tag_6")
            .build();
    createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment3 = createExperimentResponse.getExperiment();
    experimentMap.put(experiment3.getId(), experiment3);
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment3.getName());

    // experiment4 of above project
    createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_sprt_abc_4");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(1.00).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.001212).build())
            .build();
    createExperimentRequest =
        createExperimentRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_5")
            .addTags("Tag_7")
            .addTags("Tag_8")
            .build();
    createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment4 = createExperimentResponse.getExperiment();
    experimentMap.put(experiment4.getId(), experiment4);
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment4.getName());

    // Validate check for predicate value not empty
    List<KeyValueQuery> predicates = new ArrayList<>();
    Value stringValueType = Value.newBuilder().setStringValue("").build();

    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(stringValueType)
            .setOperator(Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    FindExperiments findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addAllPredicates(predicates)
            // .setIdsOnly(true)
            .build();
    try {
      hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // If key is not set in predicate
    findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(
                KeyValueQuery.newBuilder()
                    .setValue(Value.newBuilder().setNumberValue(11).build())
                    .build())
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // Validate check for struct Type not implemented
    predicates = new ArrayList<>();
    Value numValue = Value.newBuilder().setNumberValue(17.1716586149719).build();

    Struct.Builder struct = Struct.newBuilder();
    struct.putFields("number_value", numValue);
    struct.build();
    Value structValue = Value.newBuilder().setStructValue(struct).build();

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(structValue)
            .setOperator(Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addAllPredicates(predicates)
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    // get experiment with value of attributes.attribute_1 <= 0.6543210
    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(Operator.LTE)
            .build();

    findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQuery)
            .build();

    AdvancedQueryExperimentsResponse response =
        hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
    List<Experiment> experimentList = new ArrayList<>();
    for (HydratedExperiment hydratedExperiment : response.getHydratedExperimentsList()) {
      experimentList.add(hydratedExperiment.getExperiment());
    }
    LOGGER.info("FindExperiments Response size: " + experimentList.size());
    assertEquals(
        "Experiment count not match with expected experiment count", 3, experimentList.size());

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());

    for (Experiment fetchedExperiment : experimentList) {
      boolean doesAttributeExist = false;
      for (KeyValue fetchedAttribute : fetchedExperiment.getAttributesList()) {
        if (fetchedAttribute.getKey().equals("attribute_1")) {
          doesAttributeExist = true;
          assertTrue(
              "Experiment attributes.attribute_1 not match with expected experiment attributes.attribute_1",
              fetchedAttribute.getValue().getNumberValue() <= 0.6543210);
        }
      }
      if (!doesAttributeExist) {
        fail("Expected attribute not found in fetched attributes");
      }
    }

    // get experiment with value of attributes.attribute_1 <= 0.6543210 & attributes.attribute_2 ==
    // 0.31
    predicates = new ArrayList<>();
    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setNumberValue(0.31).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(numValue)
            .setOperator(Operator.EQ)
            .build();
    predicates.add(keyValueQuery2);

    findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addAllPredicates(predicates)
            .setIdsOnly(true)
            .build();

    response = hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
    experimentList = new ArrayList<>();
    for (HydratedExperiment hydratedExperiment : response.getHydratedExperimentsList()) {
      experimentList.add(hydratedExperiment.getExperiment());
    }
    LOGGER.info("FindExperiments Response : " + experimentList.size());
    assertEquals(
        "Experiment count not match with expected experiment count", 1, experimentList.size());
    assertEquals(
        "Experiment not match with expected experiment",
        experiment2.getId(),
        experimentList.get(0).getId());
    assertNotEquals(
        "Experiment not match with expected experiment", experiment2, experimentList.get(0));
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    // get experimentRun with value of metrics.accuracy >= 0.6543210 & tags == Tag_7
    predicates = new ArrayList<>();
    Value stringValue = Value.newBuilder().setStringValue("Tag_7").build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue)
            .setOperator(Operator.EQ)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(Operator.GTE)
            .build();
    predicates.add(keyValueQuery2);

    List<String> experimentIds = new ArrayList<>();
    experimentIds.add(experiment1.getId());
    experimentIds.add(experiment2.getId());
    experimentIds.add(experiment3.getId());
    experimentIds.add(experiment4.getId());

    findExperiments =
        FindExperiments.newBuilder()
            .addAllExperimentIds(experimentIds)
            .addAllPredicates(predicates)
            .build();

    response = hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
    experimentList = new ArrayList<>();
    for (HydratedExperiment hydratedExperiment : response.getHydratedExperimentsList()) {
      experimentList.add(hydratedExperiment.getExperiment());
    }
    LOGGER.info("FindExperiments Response : " + experimentList.size());
    assertEquals(
        "Experiment count not match with expected experiment count", 1, experimentList.size());
    assertEquals(
        "Experiment not match with expected experiment",
        experiment4.getId(),
        experimentList.get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    // get experiment with value of endTime
    stringValue =
        Value.newBuilder().setStringValue(String.valueOf(experiment4.getDateCreated())).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.DATE_CREATED)
            .setValue(stringValue)
            .setOperator(Operator.EQ)
            .build();

    findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQuery)
            .build();

    response = hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
    experimentList = new ArrayList<>();
    for (HydratedExperiment hydratedExperiment : response.getHydratedExperimentsList()) {
      experimentList.add(hydratedExperiment.getExperiment());
    }
    LOGGER.info("FindExperiments Response : " + experimentList.size());
    assertEquals(
        "Experiment count not match with expected experiment count", 1, experimentList.size());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experiment4.getId(),
        experimentList.get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(Operator.LTE)
            .build();

    int pageLimit = 2;
    int count = 0;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      findExperiments =
          FindExperiments.newBuilder()
              .setProjectId(project.getId())
              .addPredicates(keyValueQuery2)
              .setPageLimit(pageLimit)
              .setPageNumber(pageNumber)
              .setAscending(true)
              .setSortKey("name")
              .build();

      response = hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
      experimentList = new ArrayList<>();
      for (HydratedExperiment hydratedExperiment : response.getHydratedExperimentsList()) {
        experimentList.add(hydratedExperiment.getExperiment());
      }

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          response.getTotalRecords());

      if (!experimentList.isEmpty()) {
        isExpectedResultFound = true;
        for (Experiment experiment : experimentList) {
          assertEquals(
              "Experiment not match with expected experiment",
              experimentMap.get(experiment.getId()),
              experiment);

          if (count == 0) {
            assertEquals(
                "Experiment name not match with expected experiment name",
                experiment1.getName(),
                experiment.getName());
          } else if (count == 1) {
            assertEquals(
                "Experiment name not match with expected experiment name",
                experiment2.getName(),
                experiment.getName());
          } else if (count == 2) {
            assertEquals(
                "Experiment name not match with expected experiment name",
                experiment3.getName(),
                experiment.getName());
          }
          count++;
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More Experiment not found in database");
          assertTrue(true);
        } else {
          fail("Expected experiment not found in response");
        }
        break;
      }
    }

    findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQuery2)
            .setAscending(false)
            .setSortKey("observations.attribute.attr_1")
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    // get experiment with value of tags == test_tag_123
    Value stringValue1 = Value.newBuilder().setStringValue("Tag_1").build();
    KeyValueQuery keyValueQueryTag1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue1)
            .setOperator(Operator.EQ)
            .build();
    // get experimentRun with value of tags == test_tag_456
    Value stringValue2 = Value.newBuilder().setStringValue("Tag_5").build();
    KeyValueQuery keyValueQueryTag2 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue2)
            .setOperator(Operator.EQ)
            .build();

    findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQueryTag1)
            .addPredicates(keyValueQueryTag2)
            .build();

    response = hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
    experimentList = new ArrayList<>();
    for (HydratedExperiment hydratedExperiment : response.getHydratedExperimentsList()) {
      experimentList.add(hydratedExperiment.getExperiment());
    }
    LOGGER.info("FindExperiments Response : " + experimentList.size());
    assertEquals(
        "Experiment count not match with expected experiment count", 1, experimentList.size());
    assertEquals(
        "Experiment not match with expected experiment",
        experiment3.getId(),
        experimentList.get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    Value numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValueLoss)
            .setOperator(Operator.LTE)
            .build();

    findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQueryAttribute_1)
            .setAscending(false)
            .setSortKey("attributes.attribute_1")
            .build();

    response = hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
    experimentList = new ArrayList<>();
    for (HydratedExperiment hydratedExperiment : response.getHydratedExperimentsList()) {
      experimentList.add(hydratedExperiment.getExperiment());
    }

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "Experiment count not match with expected experiment count", 3, experimentList.size());

    KeyValueQuery keyValueQueryAccuracy =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.654321).build())
            .setOperator(Operator.LTE)
            .build();
    findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQueryAttribute_1)
            .addPredicates(keyValueQueryAccuracy)
            .setAscending(false)
            .setSortKey("attributes.attribute_1")
            .build();
    response = hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
    experimentList = new ArrayList<>();
    for (HydratedExperiment hydratedExperiment : response.getHydratedExperimentsList()) {
      experimentList.add(hydratedExperiment.getExperiment());
    }

    assertEquals(
        "Total records count not matched with expected records count",
        2,
        response.getTotalRecords());
    assertEquals(
        "Experiment count not match with expected experiment count", 2, experimentList.size());

    numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValueLoss)
            .setOperator(Operator.LTE)
            .build();

    findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQueryAttribute_1)
            .setAscending(false)
            .setIdsOnly(true)
            .setSortKey("attributes.attribute_1")
            .build();

    response = hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
    experimentList = new ArrayList<>();
    for (HydratedExperiment hydratedExperiment : response.getHydratedExperimentsList()) {
      experimentList.add(hydratedExperiment.getExperiment());
    }

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "Experiment count not match with expected experiment count", 3, experimentList.size());

    for (int index = 0; index < experimentList.size(); index++) {
      Experiment experiment = experimentList.get(index);
      if (index == 0) {
        assertNotEquals("Experiment not match with expected experiment", experiment3, experiment);
        assertEquals(
            "Experiment Id not match with expected experiment Id",
            experiment3.getId(),
            experiment.getId());
      } else if (index == 1) {
        assertNotEquals("Experiment not match with expected experiment", experiment2, experiment);
        assertEquals(
            "Experiment Id not match with expected experiment Id",
            experiment2.getId(),
            experiment.getId());
      } else if (index == 2) {
        assertNotEquals("Experiment not match with expected experiment", experiment1, experiment);
        assertEquals(
            "Experiment Id not match with expected experiment Id",
            experiment1.getId(),
            experiment.getId());
      }
    }

    keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.ID)
            .setValue(Value.newBuilder().setStringValue("xyz").build())
            .setOperator(Operator.EQ)
            .build();

    findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQueryAttribute_1)
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.ID)
            .setValue(Value.newBuilder().setStringValue("xyz").build())
            .setOperator(Operator.NE)
            .build();

    findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(keyValueQueryAttribute_1)
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedExperiments(findExperiments);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("FindExperimentRuns test stop................................");
  }

  @Test
  public void getHydratedProjectsByPagination() {
    LOGGER.info("Get Hydrated Project by pagination test start................................");

    ProjectTest projectTest = new ProjectTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    Map<String, Project> projectsMap = new HashMap<>();
    // Create project1
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_apt_1");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project1 = createProjectResponse.getProject();
    projectsMap.put(project1.getId(), project1);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project1.getName());

    // Create project2
    createProjectRequest = projectTest.getCreateProjectRequest("project_apt_2");
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    projectsMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project2.getName());

    // Create project3
    createProjectRequest = projectTest.getCreateProjectRequest("project_apt_3");
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project3 = createProjectResponse.getProject();
    projectsMap.put(project3.getId(), project3);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project3.getName());

    GetHydratedProjects getHydratedProjects = GetHydratedProjects.newBuilder().build();
    GetHydratedProjects.Response response =
        hydratedServiceBlockingStub.getHydratedProjects(getHydratedProjects);
    LOGGER.info("GetHydratedProjects Count : " + response.getTotalRecords());
    List<Project> projectList = new ArrayList<>();
    for (HydratedProject hydratedProject : response.getHydratedProjectsList()) {
      projectList.add(hydratedProject.getProject());
    }
    assertEquals(
        "HydratedProjects count not match with expected HydratedProjects count",
        projectsMap.size(),
        projectList.size());
    assertEquals(
        "Projects count not match with expected projects count",
        projectsMap.size(),
        response.getTotalRecords());

    for (Project project : projectList) {
      if (projectsMap.get(project.getId()) == null) {
        fail("Project not found in the expected project list");
      }
    }

    int pageLimit = 1;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      getHydratedProjects =
          GetHydratedProjects.newBuilder()
              .setPageNumber(pageNumber)
              .setPageLimit(pageLimit)
              .setAscending(false)
              .setSortKey(ModelDBConstants.NAME)
              .build();

      GetHydratedProjects.Response hydratedProjectsResponse =
          hydratedServiceBlockingStub.getHydratedProjects(getHydratedProjects);

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          hydratedProjectsResponse.getTotalRecords());

      projectList = new ArrayList<>();
      for (HydratedProject hydratedProject : hydratedProjectsResponse.getHydratedProjectsList()) {
        projectList.add(hydratedProject.getProject());
      }

      if (!projectList.isEmpty()) {
        isExpectedResultFound = true;
        LOGGER.info("GetProjects Response : " + projectList.size());
        for (Project project : projectList) {
          assertEquals(
              "Project not match with expected Project", projectsMap.get(project.getId()), project);
        }

        if (pageNumber == 1) {
          assertEquals(
              "Project not match with expected Project",
              projectsMap.get(projectList.get(0).getId()),
              project3);
        } else if (pageNumber == 3) {
          assertEquals(
              "Project not match with expected Project",
              projectsMap.get(projectList.get(0).getId()),
              project1);
        }

      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More Project not found in database");
          assertTrue(true);
        } else {
          fail("Expected project not found in response");
        }
        break;
      }
    }

    for (String projectId : projectsMap.keySet()) {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(projectId).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }

    LOGGER.info("Get Hydrated project by pagination test stop................................");
  }

  @Test
  public void findHydratedProjectsTest() {
    LOGGER.info("FindHydratedProjects test start................................");

    ProjectTest projectTest = new ProjectTest();
    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    Map<String, Project> projectMap = new HashMap<>();

    // Create two project of above project
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("Project_1");
    KeyValue attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.012).build())
            .build();
    KeyValue attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.99).build())
            .build();
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_2")
            .build();
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project1 = createProjectResponse.getProject();
    projectMap.put(project1.getId(), project1);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project1.getName());

    // project2 of above project
    createProjectRequest = projectTest.getCreateProjectRequest("Project_2");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_3")
            .addTags("Tag_4")
            .build();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    projectMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project2.getName());

    // project3 of above project
    createProjectRequest = projectTest.getCreateProjectRequest("Project_3");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_5")
            .addTags("Tag_6")
            .build();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project3 = createProjectResponse.getProject();
    projectMap.put(project3.getId(), project3);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project3.getName());

    // project4 of above project
    createProjectRequest = projectTest.getCreateProjectRequest("Project_4");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(1.00).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.001212).build())
            .build();
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_5")
            .addTags("Tag_7")
            .addTags("Tag_8")
            .setProjectVisibility(ProjectVisibility.PUBLIC)
            .build();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project4 = createProjectResponse.getProject();
    projectMap.put(project4.getId(), project4);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project4.getName());

    // Validate check for predicate value not empty
    List<KeyValueQuery> predicates = new ArrayList<>();
    Value stringValueType = Value.newBuilder().setStringValue("").build();

    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(stringValueType)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    FindProjects findProjects =
        FindProjects.newBuilder()
            .addProjectIds(project1.getId())
            .addAllPredicates(predicates)
            // .setIdsOnly(true)
            .build();
    try {
      hydratedServiceBlockingStub.findHydratedProjects(findProjects);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // If key is not set in predicate
    findProjects =
        FindProjects.newBuilder()
            .addProjectIds(project1.getId())
            .addPredicates(
                KeyValueQuery.newBuilder()
                    .setValue(Value.newBuilder().setNumberValue(11).build())
                    .build())
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedProjects(findProjects);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // Validate check for struct Type not implemented
    predicates = new ArrayList<>();
    Value numValue = Value.newBuilder().setNumberValue(17.1716586149719).build();

    Struct.Builder struct = Struct.newBuilder();
    struct.putFields("number_value", numValue);
    struct.build();
    Value structValue = Value.newBuilder().setStructValue(struct).build();

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(structValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    findProjects =
        FindProjects.newBuilder()
            .addProjectIds(project1.getId())
            .addAllPredicates(predicates)
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedProjects(findProjects);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    // get project with value of attributes.attribute_1 <= 0.6543210
    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findProjects = FindProjects.newBuilder().addPredicates(keyValueQuery).build();

    AdvancedQueryProjectsResponse response =
        hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    List<Project> projectList = new ArrayList<>();
    for (HydratedProject hydratedProject : response.getHydratedProjectsList()) {
      projectList.add(hydratedProject.getProject());
    }
    LOGGER.info("FindProjects Response : " + projectList.size());
    assertEquals("Project count not match with expected project count", 3, projectList.size());

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());

    for (Project fetchedProject : projectList) {
      boolean doesAttributeExist = false;
      for (KeyValue fetchedAttribute : fetchedProject.getAttributesList()) {
        if (fetchedAttribute.getKey().equals("attribute_1")) {
          doesAttributeExist = true;
          assertTrue(
              "Project attributes.attribute_1 not match with expected project attributes.attribute_1",
              fetchedAttribute.getValue().getNumberValue() <= 0.6543210);
        }
      }
      if (!doesAttributeExist) {
        fail("Expected attribute not found in fetched attributes");
      }
    }

    List<String> projectIds = new ArrayList<>();
    projectIds.add(project1.getId());
    projectIds.add(project2.getId());
    projectIds.add(project3.getId());
    projectIds.add(project4.getId());

    // get project with value of attributes.attribute_1 <= 0.6543210 & attributes.attribute_2 ==
    // 0.31
    predicates = new ArrayList<>();
    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setNumberValue(0.31).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery2);

    findProjects =
        FindProjects.newBuilder()
            .addAllProjectIds(projectIds)
            .addAllPredicates(predicates)
            .setIdsOnly(true)
            .build();

    response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    projectList = new ArrayList<>();
    for (HydratedProject hydratedProject : response.getHydratedProjectsList()) {
      projectList.add(hydratedProject.getProject());
    }
    LOGGER.info("FindProjects Response : " + projectList.size());
    assertEquals("Project count not match with expected project count", 1, projectList.size());
    assertEquals(
        "Project not match with expected project", project2.getId(), projectList.get(0).getId());
    assertNotEquals("Project not match with expected project", project2, projectList.get(0));
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    // get projectRun with value of metrics.accuracy >= 0.6543210 & tags == Tag_7
    predicates = new ArrayList<>();
    Value stringValue = Value.newBuilder().setStringValue("Tag_7").build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.GTE)
            .build();
    predicates.add(keyValueQuery2);

    findProjects =
        FindProjects.newBuilder().addAllProjectIds(projectIds).addAllPredicates(predicates).build();

    response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    projectList = new ArrayList<>();
    for (HydratedProject hydratedProject : response.getHydratedProjectsList()) {
      projectList.add(hydratedProject.getProject());
    }
    LOGGER.info("FindProjects Response : " + projectList.size());
    assertEquals("Project count not match with expected project count", 1, projectList.size());
    assertEquals(
        "Project not match with expected project", project4.getId(), projectList.get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    // get project with value of endTime
    stringValue =
        Value.newBuilder().setStringValue(String.valueOf(project4.getDateCreated())).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.DATE_CREATED)
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    findProjects =
        FindProjects.newBuilder().addAllProjectIds(projectIds).addPredicates(keyValueQuery).build();

    response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    projectList = new ArrayList<>();
    for (HydratedProject hydratedProject : response.getHydratedProjectsList()) {
      projectList.add(hydratedProject.getProject());
    }
    LOGGER.info("FindProjects Response : " + projectList.size());
    assertEquals("Project count not match with expected project count", 1, projectList.size());
    assertEquals(
        "ProjectRun not match with expected projectRun",
        project4.getId(),
        projectList.get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    int pageLimit = 2;
    int count = 0;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      findProjects =
          FindProjects.newBuilder()
              .addAllProjectIds(projectIds)
              .addPredicates(keyValueQuery2)
              .setPageLimit(pageLimit)
              .setPageNumber(pageNumber)
              .setAscending(true)
              .setSortKey("name")
              .build();

      response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
      projectList = new ArrayList<>();
      for (HydratedProject hydratedProject : response.getHydratedProjectsList()) {
        projectList.add(hydratedProject.getProject());
      }

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          response.getTotalRecords());

      if (projectList.size() > 0) {
        isExpectedResultFound = true;
        for (Project project : projectList) {
          assertEquals(
              "Project not match with expected project", projectMap.get(project.getId()), project);

          if (count == 0) {
            assertEquals(
                "Project name not match with expected project name",
                project1.getName(),
                project.getName());
          } else if (count == 1) {
            assertEquals(
                "Project name not match with expected project name",
                project2.getName(),
                project.getName());
          } else if (count == 2) {
            assertEquals(
                "Project name not match with expected project name",
                project3.getName(),
                project.getName());
          }
          count++;
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More Project not found in database");
          assertTrue(true);
        } else {
          fail("Expected project not found in response");
        }
        break;
      }
    }

    findProjects =
        FindProjects.newBuilder()
            .addAllProjectIds(projectIds)
            .addPredicates(keyValueQuery2)
            .setAscending(false)
            .setSortKey("observations.attribute.attr_1")
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedProjects(findProjects);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    // get project with value of tags == test_tag_123
    Value stringValue1 = Value.newBuilder().setStringValue("Tag_1").build();
    KeyValueQuery keyValueQueryTag1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue1)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    // get projectRun with value of tags == test_tag_456
    Value stringValue2 = Value.newBuilder().setStringValue("Tag_5").build();
    KeyValueQuery keyValueQueryTag2 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue2)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    findProjects =
        FindProjects.newBuilder()
            .addAllProjectIds(projectIds)
            .addPredicates(keyValueQueryTag1)
            .addPredicates(keyValueQueryTag2)
            .build();

    response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    projectList = new ArrayList<>();
    for (HydratedProject hydratedProject : response.getHydratedProjectsList()) {
      projectList.add(hydratedProject.getProject());
    }
    LOGGER.info("FindProjects Response : " + projectList.size());
    assertEquals("Project count not match with expected project count", 1, projectList.size());
    assertEquals(
        "Project not match with expected project", project3.getId(), projectList.get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    Value numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValueLoss)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findProjects =
        FindProjects.newBuilder()
            .addAllProjectIds(projectIds)
            .addPredicates(keyValueQueryAttribute_1)
            .setAscending(false)
            .setSortKey("attributes.attribute_1")
            .build();

    response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    projectList = new ArrayList<>();
    for (HydratedProject hydratedProject : response.getHydratedProjectsList()) {
      projectList.add(hydratedProject.getProject());
    }
    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals("Project count not match with expected project count", 3, projectList.size());

    KeyValueQuery keyValueQueryAccuracy =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.654321).build())
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    findProjects =
        FindProjects.newBuilder()
            .addAllProjectIds(projectIds)
            .addPredicates(keyValueQueryAttribute_1)
            .addPredicates(keyValueQueryAccuracy)
            .setAscending(false)
            .setSortKey("attributes.attribute_1")
            .build();
    response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    projectList = new ArrayList<>();
    for (HydratedProject hydratedProject : response.getHydratedProjectsList()) {
      projectList.add(hydratedProject.getProject());
    }
    assertEquals(
        "Total records count not matched with expected records count",
        2,
        response.getTotalRecords());
    assertEquals("Project count not match with expected project count", 2, projectList.size());

    numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValueLoss)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findProjects =
        FindProjects.newBuilder()
            .addAllProjectIds(projectIds)
            .addPredicates(keyValueQueryAttribute_1)
            .setAscending(false)
            .setIdsOnly(true)
            .setSortKey("attributes.attribute_1")
            .build();

    response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    projectList = new ArrayList<>();
    for (HydratedProject hydratedProject : response.getHydratedProjectsList()) {
      projectList.add(hydratedProject.getProject());
    }
    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals("Project count not match with expected project count", 3, projectList.size());

    for (int index = 0; index < projectList.size(); index++) {
      Project project = projectList.get(index);
      if (index == 0) {
        assertNotEquals("Project not match with expected project", project3, project);
        assertEquals(
            "Project Id not match with expected project Id", project3.getId(), project.getId());
      } else if (index == 1) {
        assertNotEquals("Project not match with expected project", project2, project);
        assertEquals(
            "Project Id not match with expected project Id", project2.getId(), project.getId());
      } else if (index == 2) {
        assertNotEquals("Project not match with expected project", project1, project);
        assertEquals(
            "Project Id not match with expected project Id", project1.getId(), project.getId());
      }
    }

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.PROJECT_VISIBILITY)
            .setValue(Value.newBuilder().setStringValue("PUBLIC").build())
            .setOperator(Operator.EQ)
            .build();
    findProjects =
        FindProjects.newBuilder()
            .addPredicates(keyValueQuery)
            .setAscending(false)
            .setIdsOnly(false)
            .setSortKey("name")
            .build();

    response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());
    assertEquals(
        "HydratedProject count not match with expected HydratedProject count",
        1,
        response.getHydratedProjectsCount());
    assertEquals(
        "HydratedProject Id not match with expected HydratedProject Id",
        project4.getId(),
        response.getHydratedProjects(0).getProject().getId());

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(Value.newBuilder().setStringValue("_8").build())
            .setOperator(Operator.CONTAIN)
            .build();
    findProjects =
        FindProjects.newBuilder()
            .addPredicates(keyValueQuery)
            .setAscending(false)
            .setIdsOnly(false)
            .setSortKey("name")
            .build();

    response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());
    assertEquals(
        "HydratedProject count not match with expected HydratedProject count",
        1,
        response.getHydratedProjectsCount());
    assertEquals(
        "HydratedProject Id not match with expected HydratedProject Id",
        project4.getId(),
        response.getHydratedProjects(0).getProject().getId());

    KeyValueQuery keyValueQuery1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(Value.newBuilder().setStringValue("_8").build())
            .setOperator(Operator.NOT_CONTAIN)
            .build();
    keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(Value.newBuilder().setStringValue("_x").build())
            .setOperator(Operator.CONTAIN)
            .build();
    findProjects =
        FindProjects.newBuilder()
            .addPredicates(keyValueQuery1)
            .addPredicates(keyValueQuery2)
            .setAscending(false)
            .setIdsOnly(false)
            .setSortKey("name")
            .build();

    response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "HydratedProject count not match with expected HydratedProject count",
        3,
        response.getHydratedProjectsCount());
    assertEquals(
        "HydratedProject Id not match with expected HydratedProject Id",
        project3.getId(),
        response.getHydratedProjects(0).getProject().getId());

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.PROJECT_VISIBILITY)
            .setValue(Value.newBuilder().setStringValue("PUBLIC").build())
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    findProjects =
        FindProjects.newBuilder()
            .addPredicates(keyValueQuery)
            .setAscending(false)
            .setIdsOnly(false)
            .setSortKey("name")
            .build();

    response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.PROJECT_VISIBILITY)
            .setValue(Value.newBuilder().setStringValue("PUBLIC").build())
            .setOperator(OperatorEnum.Operator.NE)
            .build();
    findProjects = FindProjects.newBuilder().addPredicates(keyValueQuery).build();

    response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.ID)
            .setValue(Value.newBuilder().setStringValue("xyz").build())
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    findProjects = FindProjects.newBuilder().addPredicates(keyValueQuery).build();

    try {
      response = hydratedServiceBlockingStub.findHydratedProjects(findProjects);
      if (app.getAuthServerHost() == null || app.getAuthServerPort() == null) {
        assertEquals(0, response.getTotalRecords());
      } else {
        fail();
      }
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.ID)
            .setValue(Value.newBuilder().setStringValue("xyz").build())
            .setOperator(OperatorEnum.Operator.NE)
            .build();
    findProjects = FindProjects.newBuilder().addPredicates(keyValueQuery).build();

    try {
      hydratedServiceBlockingStub.findHydratedProjects(findProjects);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    for (String projectId : projectIds) {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(projectId).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }

    LOGGER.info("FindProjectRuns test stop................................");
  }

  @Test
  public void findHydratedDatasetsTest() {
    LOGGER.info("FindHydratedDatasets test start................................");

    DatasetTest datasetTest = new DatasetTest();
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    Map<String, Dataset> datasetMap = new HashMap<>();

    // Create two dataset of above dataset
    CreateDataset createDatasetRequest = datasetTest.getDatasetRequest("HydratedDataset_1");
    KeyValue attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.012).build())
            .build();
    KeyValue attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.99).build())
            .build();
    createDatasetRequest =
        createDatasetRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_2")
            .build();
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset1 = createDatasetResponse.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected Dataset name",
        createDatasetRequest.getName(),
        dataset1.getName());

    // dataset2 of above dataset
    createDatasetRequest = datasetTest.getDatasetRequest("HydratedDataset_2");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    createDatasetRequest =
        createDatasetRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_3")
            .addTags("Tag_4")
            .build();
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset2 = createDatasetResponse.getDataset();
    datasetMap.put(dataset2.getId(), dataset2);
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected Dataset name",
        createDatasetRequest.getName(),
        dataset2.getName());

    // dataset3 of above dataset
    createDatasetRequest = datasetTest.getDatasetRequest("HydratedDataset_3");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    createDatasetRequest =
        createDatasetRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_5")
            .addTags("Tag_6")
            .build();
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset3 = createDatasetResponse.getDataset();
    datasetMap.put(dataset3.getId(), dataset3);
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected Dataset name",
        createDatasetRequest.getName(),
        dataset3.getName());

    // dataset4 of above dataset
    createDatasetRequest = datasetTest.getDatasetRequest("HydratedDataset_4");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(1.00).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.001212).build())
            .build();
    createDatasetRequest =
        createDatasetRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_5")
            .addTags("Tag_7")
            .addTags("Tag_8")
            .setDatasetVisibility(DatasetVisibilityEnum.DatasetVisibility.PUBLIC)
            .build();
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset4 = createDatasetResponse.getDataset();
    datasetMap.put(dataset4.getId(), dataset4);
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected Dataset name",
        createDatasetRequest.getName(),
        dataset4.getName());

    // Validate check for predicate value not empty
    List<KeyValueQuery> predicates = new ArrayList<>();
    Value stringValueType = Value.newBuilder().setStringValue("").build();

    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(stringValueType)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    FindDatasets findDatasets =
        FindDatasets.newBuilder()
            .addDatasetIds(dataset1.getId())
            .addAllPredicates(predicates)
            // .setIdsOnly(true)
            .build();
    try {
      hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // If key is not set in predicate
    findDatasets =
        FindDatasets.newBuilder()
            .addDatasetIds(dataset1.getId())
            .addPredicates(
                KeyValueQuery.newBuilder()
                    .setValue(Value.newBuilder().setNumberValue(11).build())
                    .build())
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // Validate check for struct Type not implemented
    predicates = new ArrayList<>();
    Value numValue = Value.newBuilder().setNumberValue(17.1716586149719).build();

    Struct.Builder struct = Struct.newBuilder();
    struct.putFields("number_value", numValue);
    struct.build();
    Value structValue = Value.newBuilder().setStructValue(struct).build();

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(structValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    findDatasets =
        FindDatasets.newBuilder()
            .addDatasetIds(dataset1.getId())
            .addAllPredicates(predicates)
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    // get dataset with value of attributes.attribute_1 <= 0.6543210
    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findDatasets = FindDatasets.newBuilder().addPredicates(keyValueQuery).build();

    AdvancedQueryDatasetsResponse response =
        hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
    LOGGER.info("AdvancedQueryDatasetsResponse Response : " + response.getHydratedDatasetsList());
    assertEquals(
        "HydratedDataset count not match with expected dataset count",
        3,
        response.getHydratedDatasetsList().size());

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());

    for (HydratedDataset fetchedHydratedDataset : response.getHydratedDatasetsList()) {
      boolean doesAttributeExist = false;
      for (KeyValue fetchedAttribute : fetchedHydratedDataset.getDataset().getAttributesList()) {
        if (fetchedAttribute.getKey().equals("attribute_1")) {
          doesAttributeExist = true;
          assertTrue(
              "HydratedDataset attributes.attribute_1 not match with expected dataset attributes.attribute_1",
              fetchedAttribute.getValue().getNumberValue() <= 0.6543210);
        }
      }
      if (!doesAttributeExist) {
        fail("Expected attribute not found in fetched attributes");
      }
    }

    List<String> datasetIds = new ArrayList<>();
    datasetIds.add(dataset1.getId());
    datasetIds.add(dataset2.getId());
    datasetIds.add(dataset3.getId());
    datasetIds.add(dataset4.getId());

    // get dataset with value of attributes.attribute_1 <= 0.6543210 & attributes.attribute_2 ==
    // 0.31
    predicates = new ArrayList<>();
    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setNumberValue(0.31).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery2);

    findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetIds)
            .addAllPredicates(predicates)
            .setIdsOnly(true)
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
    LOGGER.info("AdvancedQueryDatasetsResponse Response : " + response.getHydratedDatasetsCount());
    assertEquals(
        "HydratedDataset count not match with expected dataset count",
        1,
        response.getHydratedDatasetsCount());
    assertEquals(
        "HydratedDataset not match with expected dataset",
        dataset2.getId(),
        response.getHydratedDatasetsList().get(0).getDataset().getId());
    assertNotEquals(
        "HydratedDataset not match with expected dataset",
        dataset2,
        response.getHydratedDatasetsList().get(0).getDataset());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    // get datasetRun with value of metrics.accuracy >= 0.6543210 & tags == Tag_7
    predicates = new ArrayList<>();
    Value stringValue = Value.newBuilder().setStringValue("Tag_7").build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.GTE)
            .build();
    predicates.add(keyValueQuery2);

    findDatasets =
        FindDatasets.newBuilder().addAllDatasetIds(datasetIds).addAllPredicates(predicates).build();

    response = hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
    LOGGER.info("AdvancedQueryDatasetsResponse Response : " + response.getHydratedDatasetsCount());
    assertEquals(
        "HydratedDataset count not match with expected dataset count",
        1,
        response.getHydratedDatasetsCount());
    assertEquals(
        "HydratedDataset not match with expected dataset",
        dataset4.getId(),
        response.getHydratedDatasetsList().get(0).getDataset().getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    // get dataset with value of endTime
    stringValue =
        Value.newBuilder().setStringValue(String.valueOf(dataset4.getTimeUpdated())).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.TIME_UPDATED)
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    findDatasets =
        FindDatasets.newBuilder().addAllDatasetIds(datasetIds).addPredicates(keyValueQuery).build();

    response = hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
    LOGGER.info("AdvancedQueryDatasetsResponse Response : " + response.getHydratedDatasetsCount());
    assertEquals(
        "HydratedDataset count not match with expected dataset count",
        1,
        response.getHydratedDatasetsCount());
    assertEquals(
        "HydratedDatasetRun not match with expected datasetRun",
        dataset4.getId(),
        response.getHydratedDatasetsList().get(0).getDataset().getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    int pageLimit = 2;
    int count = 0;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      findDatasets =
          FindDatasets.newBuilder()
              .addAllDatasetIds(datasetIds)
              .addPredicates(keyValueQuery2)
              .setPageLimit(pageLimit)
              .setPageNumber(pageNumber)
              .setAscending(true)
              .setSortKey("name")
              .build();

      response = hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          response.getTotalRecords());

      if (response.getHydratedDatasetsList() != null
          && response.getHydratedDatasetsList().size() > 0) {
        isExpectedResultFound = true;
        for (HydratedDataset hydratedDataset : response.getHydratedDatasetsList()) {
          Dataset dataset = hydratedDataset.getDataset();
          assertEquals(
              "HydratedDataset not match with expected dataset",
              datasetMap.get(dataset.getId()),
              dataset);

          if (count == 0) {
            assertEquals(
                "HydratedDataset name not match with expected dataset name",
                dataset1.getName(),
                dataset.getName());
          } else if (count == 1) {
            assertEquals(
                "HydratedDataset name not match with expected dataset name",
                dataset2.getName(),
                dataset.getName());
          } else if (count == 2) {
            assertEquals(
                "HydratedDataset name not match with expected dataset name",
                dataset3.getName(),
                dataset.getName());
          }
          count++;
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More HydratedDataset not found in database");
          assertTrue(true);
        } else {
          fail("Expected dataset not found in response");
        }
        break;
      }
    }

    findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetIds)
            .addPredicates(keyValueQuery2)
            .setAscending(false)
            .setSortKey("observations.attribute.attr_1")
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    // get dataset with value of tags == test_tag_123
    Value stringValue1 = Value.newBuilder().setStringValue("Tag_1").build();
    KeyValueQuery keyValueQueryTag1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue1)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    // get datasetRun with value of tags == test_tag_456
    Value stringValue2 = Value.newBuilder().setStringValue("Tag_5").build();
    KeyValueQuery keyValueQueryTag2 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue2)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetIds)
            .addPredicates(keyValueQueryTag1)
            .addPredicates(keyValueQueryTag2)
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
    LOGGER.info("AdvancedQueryDatasetsResponse Response : " + response.getHydratedDatasetsCount());
    assertEquals(
        "HydratedDataset count not match with expected dataset count",
        1,
        response.getHydratedDatasetsCount());
    assertEquals(
        "HydratedDataset not match with expected dataset",
        dataset3.getId(),
        response.getHydratedDatasetsList().get(0).getDataset().getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    Value numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValueLoss)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetIds)
            .addPredicates(keyValueQueryAttribute_1)
            .setAscending(false)
            .setSortKey("attributes.attribute_1")
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "HydratedDataset count not match with expected dataset count",
        3,
        response.getHydratedDatasetsCount());

    KeyValueQuery keyValueQueryAccuracy =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.654321).build())
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetIds)
            .addPredicates(keyValueQueryAttribute_1)
            .addPredicates(keyValueQueryAccuracy)
            .setAscending(false)
            .setSortKey("attributes.attribute_1")
            .build();
    response = hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);

    assertEquals(
        "Total records count not matched with expected records count",
        2,
        response.getTotalRecords());
    assertEquals(
        "HydratedDataset count not match with expected dataset count",
        2,
        response.getHydratedDatasetsCount());

    numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValueLoss)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetIds)
            .addPredicates(keyValueQueryAttribute_1)
            .setAscending(false)
            .setIdsOnly(true)
            .setSortKey("attributes.attribute_1")
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "HydratedDataset count not match with expected dataset count",
        3,
        response.getHydratedDatasetsCount());

    for (int index = 0; index < response.getHydratedDatasetsCount(); index++) {
      HydratedDataset hydratedDataset = response.getHydratedDatasetsList().get(index);
      Dataset dataset = hydratedDataset.getDataset();
      if (index == 0) {
        assertNotEquals("HydratedDataset not match with expected dataset", dataset3, dataset);
        assertEquals(
            "HydratedDataset Id not match with expected dataset Id",
            dataset3.getId(),
            dataset.getId());
      } else if (index == 1) {
        assertNotEquals("HydratedDataset not match with expected dataset", dataset2, dataset);
        assertEquals(
            "HydratedDataset Id not match with expected dataset Id",
            dataset2.getId(),
            dataset.getId());
      } else if (index == 2) {
        assertNotEquals("HydratedDataset not match with expected dataset", dataset1, dataset);
        assertEquals(
            "HydratedDataset Id not match with expected dataset Id",
            dataset1.getId(),
            dataset.getId());
      }
    }

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.DATASET_VISIBILITY)
            .setValue(Value.newBuilder().setStringValue("PUBLIC").build())
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    findDatasets =
        FindDatasets.newBuilder()
            .addPredicates(keyValueQuery)
            .setAscending(false)
            .setIdsOnly(false)
            .setSortKey("name")
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());
    assertEquals(
        "HydratedDataset count not match with expected dataset count",
        1,
        response.getHydratedDatasetsCount());
    assertEquals(
        "HydratedDataset Id not match with expected dataset Id",
        dataset4.getId(),
        response.getHydratedDatasets(0).getDataset().getId());

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(Value.newBuilder().setStringValue("_8").build())
            .setOperator(Operator.CONTAIN)
            .build();
    findDatasets =
        FindDatasets.newBuilder()
            .addPredicates(keyValueQuery)
            .setAscending(false)
            .setIdsOnly(false)
            .setSortKey("name")
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());
    assertEquals(
        "HydratedDataset count not match with expected HydratedDataset count",
        1,
        response.getHydratedDatasetsCount());
    assertEquals(
        "HydratedDataset Id not match with expected HydratedDataset Id",
        dataset4.getId(),
        response.getHydratedDatasets(0).getDataset().getId());

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(Value.newBuilder().setStringValue("_8").build())
            .setOperator(Operator.NOT_CONTAIN)
            .build();
    findDatasets =
        FindDatasets.newBuilder()
            .addPredicates(keyValueQuery)
            .setAscending(false)
            .setIdsOnly(false)
            .setSortKey("name")
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "HydratedDataset count not match with expected HydratedDataset count",
        3,
        response.getHydratedDatasetsCount());
    assertEquals(
        "HydratedDataset Id not match with expected HydratedDataset Id",
        dataset3.getId(),
        response.getHydratedDatasets(0).getDataset().getId());

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.ID)
            .setValue(Value.newBuilder().setStringValue("xyz").build())
            .setOperator(Operator.EQ)
            .build();
    findDatasets = FindDatasets.newBuilder().addPredicates(keyValueQuery).build();

    try {
      hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.ID)
            .setValue(Value.newBuilder().setStringValue("xyz").build())
            .setOperator(Operator.NE)
            .build();
    findDatasets = FindDatasets.newBuilder().addPredicates(keyValueQuery).build();

    try {
      hydratedServiceBlockingStub.findHydratedDatasets(findDatasets);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    for (String datasetId : datasetIds) {
      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(datasetId).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());
    }

    LOGGER.info("FindHydratedDatasets test stop................................");
  }

  @Test
  public void findHydratedDatasetVersionsTest() {
    LOGGER.info("FindHydratedDatasetVersions test start................................");

    DatasetTest datasetTest = new DatasetTest();
    DatasetVersionTest datasetVersionTest = new DatasetVersionTest();
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    Map<String, DatasetVersion> datasetVersionMap = new HashMap<>();
    long version = 1L;

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    // Create two datasetVersion of above datasetVersion
    CreateDatasetVersion createDatasetVersionRequest =
        datasetVersionTest.getDatasetVersionRequest(dataset.getId());
    KeyValue attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.012).build())
            .build();
    KeyValue attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.99).build())
            .build();
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_2")
            .build();
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionMap.put(datasetVersion1.getId(), datasetVersion1);
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        version,
        datasetVersion1.getVersion());

    // datasetVersion2 of above datasetVersion
    createDatasetVersionRequest = datasetVersionTest.getDatasetVersionRequest(dataset.getId());
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_3")
            .addTags("Tag_4")
            .setRawDatasetVersionInfo(
                RawDatasetVersionInfo.newBuilder().setSize(1).setNumRecords(1).build())
            .setDatasetType(DatasetTypeEnum.DatasetType.RAW)
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion2 = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionMap.put(datasetVersion2.getId(), datasetVersion2);
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        ++version,
        datasetVersion2.getVersion());

    // datasetVersion3 of above datasetVersion
    createDatasetVersionRequest = datasetVersionTest.getDatasetVersionRequest(dataset.getId());
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_5")
            .addTags("Tag_6")
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion3 = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionMap.put(datasetVersion3.getId(), datasetVersion3);
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        ++version,
        datasetVersion3.getVersion());

    // datasetVersion4 of above datasetVersion
    createDatasetVersionRequest = datasetVersionTest.getDatasetVersionRequest(dataset.getId());
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(1.00).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.001212).build())
            .build();
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_5")
            .addTags("Tag_7")
            .addTags("Tag_8")
            .setRawDatasetVersionInfo(
                RawDatasetVersionInfo.newBuilder().setSize(1).setNumRecords(1).build())
            .setDatasetType(DatasetTypeEnum.DatasetType.RAW)
            .setDatasetVersionVisibility(DatasetVisibilityEnum.DatasetVisibility.PUBLIC)
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion4 = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionMap.put(datasetVersion4.getId(), datasetVersion4);
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        ++version,
        datasetVersion4.getVersion());

    // Validate check for predicate value not empty
    List<KeyValueQuery> predicates = new ArrayList<>();
    Value stringValueType = Value.newBuilder().setStringValue("").build();

    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(stringValueType)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addDatasetVersionIds(datasetVersion1.getId())
            .addAllPredicates(predicates)
            // .setIdsOnly(true)
            .build();
    try {
      hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // If key is not set in predicate
    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addDatasetVersionIds(datasetVersion1.getId())
            .addPredicates(
                KeyValueQuery.newBuilder()
                    .setValue(Value.newBuilder().setNumberValue(11).build())
                    .build())
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // Validate check for struct Type not implemented
    predicates = new ArrayList<>();
    Value numValue = Value.newBuilder().setNumberValue(17.1716586149719).build();

    Struct.Builder struct = Struct.newBuilder();
    struct.putFields("number_value", numValue);
    struct.build();
    Value structValue = Value.newBuilder().setStructValue(struct).build();

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(structValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addDatasetVersionIds(datasetVersion1.getId())
            .addAllPredicates(predicates)
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    // get datasetVersion with value of attributes.attribute_1 <= 0.6543210
    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findDatasetVersions = FindDatasetVersions.newBuilder().addPredicates(keyValueQuery).build();

    AdvancedQueryDatasetVersionsResponse response =
        hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
    LOGGER.info(
        "AdvancedQueryDatasetVersionsResponse Response : "
            + response.getHydratedDatasetVersionsList());
    assertEquals(
        "HydratedDatasetVersion count not match with expected datasetVersion count",
        3,
        response.getHydratedDatasetVersionsList().size());

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());

    for (HydratedDatasetVersion fetchedHydratedDatasetVersion :
        response.getHydratedDatasetVersionsList()) {
      boolean doesAttributeExist = false;
      for (KeyValue fetchedAttribute :
          fetchedHydratedDatasetVersion.getDatasetVersion().getAttributesList()) {
        if (fetchedAttribute.getKey().equals("attribute_1")) {
          doesAttributeExist = true;
          assertTrue(
              "HydratedDatasetVersion attributes.attribute_1 not match with expected datasetVersion attributes.attribute_1",
              fetchedAttribute.getValue().getNumberValue() <= 0.6543210);
        }
      }
      if (!doesAttributeExist) {
        fail("Expected attribute not found in fetched attributes");
      }
    }

    List<String> datasetVersionIds = new ArrayList<>();
    datasetVersionIds.add(datasetVersion1.getId());
    datasetVersionIds.add(datasetVersion2.getId());
    datasetVersionIds.add(datasetVersion3.getId());
    datasetVersionIds.add(datasetVersion4.getId());

    // get datasetVersion with value of attributes.attribute_1 <= 0.6543210 & attributes.attribute_2
    // ==
    // 0.31
    predicates = new ArrayList<>();
    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setNumberValue(0.31).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery2);

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionIds)
            .addAllPredicates(predicates)
            .setIdsOnly(true)
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
    LOGGER.info(
        "AdvancedQueryDatasetVersionsResponse Response : "
            + response.getHydratedDatasetVersionsCount());
    assertEquals(
        "HydratedDatasetVersion count not match with expected datasetVersion count",
        1,
        response.getHydratedDatasetVersionsCount());
    assertEquals(
        "HydratedDatasetVersion not match with expected datasetVersion",
        datasetVersion2.getId(),
        response.getHydratedDatasetVersionsList().get(0).getDatasetVersion().getId());
    assertNotEquals(
        "HydratedDatasetVersion not match with expected datasetVersion",
        datasetVersion2,
        response.getHydratedDatasetVersionsList().get(0).getDatasetVersion());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    // get datasetVersionRun with value of metrics.accuracy >= 0.6543210 & tags == Tag_7
    predicates = new ArrayList<>();
    Value stringValue = Value.newBuilder().setStringValue("Tag_7").build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.GTE)
            .build();
    predicates.add(keyValueQuery2);

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionIds)
            .addAllPredicates(predicates)
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
    LOGGER.info(
        "AdvancedQueryDatasetVersionsResponse Response : "
            + response.getHydratedDatasetVersionsCount());
    assertEquals(
        "HydratedDatasetVersion count not match with expected datasetVersion count",
        1,
        response.getHydratedDatasetVersionsCount());
    assertEquals(
        "HydratedDatasetVersion not match with expected datasetVersion",
        datasetVersion4.getId(),
        response.getHydratedDatasetVersionsList().get(0).getDatasetVersion().getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    // get datasetVersion with value of endTime
    stringValue =
        Value.newBuilder().setStringValue(String.valueOf(datasetVersion4.getTimeUpdated())).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.TIME_UPDATED)
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionIds)
            .addPredicates(keyValueQuery)
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
    LOGGER.info(
        "AdvancedQueryDatasetVersionsResponse Response : "
            + response.getHydratedDatasetVersionsCount());
    assertEquals(
        "HydratedDatasetVersion count not match with expected datasetVersion count",
        1,
        response.getHydratedDatasetVersionsCount());
    assertEquals(
        "HydratedDatasetVersionRun not match with expected datasetVersionRun",
        datasetVersion4.getId(),
        response.getHydratedDatasetVersionsList().get(0).getDatasetVersion().getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    int pageLimit = 2;
    int count = 0;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      findDatasetVersions =
          FindDatasetVersions.newBuilder()
              .addAllDatasetVersionIds(datasetVersionIds)
              .addPredicates(keyValueQuery2)
              .setPageLimit(pageLimit)
              .setPageNumber(pageNumber)
              .setAscending(true)
              .setSortKey("version")
              .build();

      response = hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          response.getTotalRecords());

      if (response.getHydratedDatasetVersionsList() != null
          && response.getHydratedDatasetVersionsList().size() > 0) {
        isExpectedResultFound = true;
        for (HydratedDatasetVersion hydratedDatasetVersion :
            response.getHydratedDatasetVersionsList()) {
          DatasetVersion datasetVersion = hydratedDatasetVersion.getDatasetVersion();
          assertEquals(
              "HydratedDatasetVersion not match with expected datasetVersion",
              datasetVersionMap.get(datasetVersion.getId()),
              datasetVersion);

          if (count == 0) {
            assertEquals(
                "HydratedDatasetVersion version not match with expected datasetVersion version",
                datasetVersion1.getVersion(),
                datasetVersion.getVersion());
          } else if (count == 1) {
            assertEquals(
                "HydratedDatasetVersion version not match with expected datasetVersion version",
                datasetVersion2.getVersion(),
                datasetVersion.getVersion());
          } else if (count == 2) {
            assertEquals(
                "HydratedDatasetVersion version not match with expected datasetVersion version",
                datasetVersion3.getVersion(),
                datasetVersion.getVersion());
          }
          count++;
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More HydratedDatasetVersion not found in database");
          assertTrue(true);
        } else {
          fail("Expected datasetVersion not found in response");
        }
        break;
      }
    }

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionIds)
            .addPredicates(keyValueQuery2)
            .setAscending(false)
            .setSortKey("observations.attribute.attr_1")
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    // get datasetVersion with value of tags == test_tag_123
    Value stringValue1 = Value.newBuilder().setStringValue("Tag_1").build();
    KeyValueQuery keyValueQueryTag1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue1)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    // get datasetVersionRun with value of tags == test_tag_456
    Value stringValue2 = Value.newBuilder().setStringValue("Tag_5").build();
    KeyValueQuery keyValueQueryTag2 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue2)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionIds)
            .addPredicates(keyValueQueryTag1)
            .addPredicates(keyValueQueryTag2)
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
    LOGGER.info(
        "AdvancedQueryDatasetVersionsResponse Response : "
            + response.getHydratedDatasetVersionsCount());
    assertEquals(
        "HydratedDatasetVersion count not match with expected datasetVersion count",
        1,
        response.getHydratedDatasetVersionsCount());
    assertEquals(
        "HydratedDatasetVersion not match with expected datasetVersion",
        datasetVersion3.getId(),
        response.getHydratedDatasetVersionsList().get(0).getDatasetVersion().getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    Value numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValueLoss)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionIds)
            .addPredicates(keyValueQueryAttribute_1)
            .setAscending(false)
            .setSortKey("attributes.attribute_1")
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "HydratedDatasetVersion count not match with expected datasetVersion count",
        3,
        response.getHydratedDatasetVersionsCount());

    KeyValueQuery keyValueQueryAccuracy =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.654321).build())
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionIds)
            .addPredicates(keyValueQueryAttribute_1)
            .addPredicates(keyValueQueryAccuracy)
            .setAscending(false)
            .setSortKey("attributes.attribute_1")
            .build();
    response = hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);

    assertEquals(
        "Total records count not matched with expected records count",
        2,
        response.getTotalRecords());
    assertEquals(
        "HydratedDatasetVersion count not match with expected datasetVersion count",
        2,
        response.getHydratedDatasetVersionsCount());

    numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValueLoss)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionIds)
            .addPredicates(keyValueQueryAttribute_1)
            .setAscending(false)
            .setIdsOnly(true)
            .setSortKey("attributes.attribute_1")
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "HydratedDatasetVersion count not match with expected datasetVersion count",
        3,
        response.getHydratedDatasetVersionsCount());

    for (int index = 0; index < response.getHydratedDatasetVersionsCount(); index++) {
      HydratedDatasetVersion hydratedDatasetVersion =
          response.getHydratedDatasetVersionsList().get(index);
      DatasetVersion datasetVersion = hydratedDatasetVersion.getDatasetVersion();
      if (index == 0) {
        assertNotEquals(
            "HydratedDatasetVersion not match with expected datasetVersion",
            datasetVersion3,
            datasetVersion);
        assertEquals(
            "HydratedDatasetVersion Id not match with expected datasetVersion Id",
            datasetVersion3.getId(),
            datasetVersion.getId());
      } else if (index == 1) {
        assertNotEquals(
            "HydratedDatasetVersion not match with expected datasetVersion",
            datasetVersion2,
            datasetVersion);
        assertEquals(
            "HydratedDatasetVersion Id not match with expected datasetVersion Id",
            datasetVersion2.getId(),
            datasetVersion.getId());
      } else if (index == 2) {
        assertNotEquals(
            "HydratedDatasetVersion not match with expected datasetVersion",
            datasetVersion1,
            datasetVersion);
        assertEquals(
            "HydratedDatasetVersion Id not match with expected datasetVersion Id",
            datasetVersion1.getId(),
            datasetVersion.getId());
      }
    }

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.DATASET_VERSION_VISIBILITY)
            .setValue(Value.newBuilder().setStringValue("PUBLIC").build())
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addPredicates(keyValueQuery)
            .setAscending(false)
            .setIdsOnly(false)
            .setSortKey("version")
            .build();

    response = hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());
    assertEquals(
        "HydratedDatasetVersion count not match with expected datasetVersion count",
        1,
        response.getHydratedDatasetVersionsCount());
    assertEquals(
        "HydratedDatasetVersion Id not match with expected datasetVersion Id",
        datasetVersion4.getId(),
        response.getHydratedDatasetVersions(0).getDatasetVersion().getId());

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addDatasetVersionIds("dsfsd")
            .addPredicates(keyValueQuery)
            .build();

    try {
      hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.ID)
            .setValue(Value.newBuilder().setStringValue("xyz").build())
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    findDatasetVersions = FindDatasetVersions.newBuilder().addPredicates(keyValueQuery).build();

    try {
      hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.ID)
            .setValue(Value.newBuilder().setStringValue("xyz").build())
            .setOperator(OperatorEnum.Operator.NE)
            .build();
    findDatasetVersions = FindDatasetVersions.newBuilder().addPredicates(keyValueQuery).build();

    try {
      hydratedServiceBlockingStub.findHydratedDatasetVersions(findDatasetVersions);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    for (String datasetVersionId : datasetVersionIds) {
      DeleteDatasetVersion deleteDatasetVersion =
          DeleteDatasetVersion.newBuilder().setId(datasetVersionId).build();
      DeleteDatasetVersion.Response deleteDatasetVersionResponse =
          datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
      LOGGER.info("DatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionResponse.toString());
      assertTrue(deleteDatasetVersionResponse.getStatus());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("FindHydratedDatasetVersions test stop................................");
  }

  @Test
  public void getHydratedDatasetByName() {
    LOGGER.info("Get HydratedDataset by name test start................................");

    DatasetTest datasetTest = new DatasetTest();
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    // Create dataset
    CreateDataset createDatasetRequest = datasetTest.getDatasetRequest("dataset_f_apt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    GetHydratedDatasetByName getHydratedDataset =
        GetHydratedDatasetByName.newBuilder().setName(dataset.getName()).build();

    GetHydratedDatasetByName.Response response =
        hydratedServiceBlockingStub.getHydratedDatasetByName(getHydratedDataset);
    LOGGER.info(
        "Response HydratedDatasetByUser of HydratedDataset : "
            + response.getHydratedDatasetByUser());
    LOGGER.info(
        "Response SharedHydratedDatasetsList of HydratedDatasets : "
            + response.getSharedHydratedDatasetsList());
    assertEquals(
        "HydratedDataset name not match",
        dataset.getName(),
        response.getHydratedDatasetByUser().getDataset().getName());
    for (HydratedDataset sharedHydratedDataset : response.getSharedHydratedDatasetsList()) {
      Dataset sharedDataset = sharedHydratedDataset.getDataset();
      assertEquals("Shared dataset name not match", dataset.getName(), sharedDataset.getName());
    }

    try {
      getHydratedDataset = GetHydratedDatasetByName.newBuilder().build();
      hydratedServiceBlockingStub.getHydratedDatasetByName(getHydratedDataset);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Get dataset by name test stop................................");
  }

  @Test
  public void findHydratedProjectsByUserTest() {
    LOGGER.info("FindHydratedProjectsByUser test start................................");
    ProjectTest projectTest = new ProjectTest();
    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    Map<String, Project> projectMap = new HashMap<>();

    // Create two project of above project
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("Project_1");
    KeyValue attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.012).build())
            .build();
    KeyValue attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.99).build())
            .build();
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_2")
            .build();
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project1 = createProjectResponse.getProject();
    projectMap.put(project1.getId(), project1);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project1.getName());

    // project2 of above project
    createProjectRequest = projectTest.getCreateProjectRequest("Project_2");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_3")
            .addTags("Tag_4")
            .build();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    projectMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project2.getName());

    // project3 of above project
    createProjectRequest = projectTest.getCreateProjectRequest("Project_3");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_5")
            .addTags("Tag_6")
            .build();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project3 = createProjectResponse.getProject();
    projectMap.put(project3.getId(), project3);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project3.getName());

    // project4 of above project
    createProjectRequest = projectTest.getCreateProjectRequest("Project_4");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(1.00).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.001212).build())
            .build();
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_5")
            .addTags("Tag_7")
            .addTags("Tag_8")
            .setProjectVisibility(ProjectVisibility.PUBLIC)
            .build();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project4 = createProjectResponse.getProject();
    projectMap.put(project4.getId(), project4);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project4.getName());

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      FindHydratedProjectsByUser findHydratedProjectsByUser =
          FindHydratedProjectsByUser.newBuilder().setVertaId(project4.getOwner()).build();

      AdvancedQueryProjectsResponse response =
          hydratedServiceBlockingStub.findHydratedProjectsByUser(findHydratedProjectsByUser);
      List<Project> projectList = new ArrayList<>();
      for (HydratedProject hydratedProject : response.getHydratedProjectsList()) {
        projectList.add(hydratedProject.getProject());
      }
      LOGGER.info("FindProjects Response : " + projectList.size());
      assertEquals("Project count not match with expected project count", 1, projectList.size());

      assertEquals(
          "Total records count not matched with expected records count",
          1,
          response.getTotalRecords());
    }

    for (String projectId : projectMap.keySet()) {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(projectId).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }
    LOGGER.info("FindHydratedProjectsByUser test stop................................");
  }

  @Test
  public void getHydratedDatasetsByProjectIdTest() {
    LOGGER.info("Get Hydrated Datasets By ProjectId test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();
    ExperimentRunTest experimentRunTest = new ExperimentRunTest();
    DatasetTest datasetTest = new DatasetTest();
    DatasetVersionTest datasetVersionTest = new DatasetVersionTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    List<Project> projectList = new ArrayList<>();
    List<Dataset> datasetList = new ArrayList<>();
    // Create project 1
    CreateProject createProjectRequest =
        projectTest.getCreateProjectRequest("experimentRun_project_ypcdt1");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project1 = createProjectResponse.getProject();
    projectList.add(project1);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project1.getName());

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project1.getId(), "Experiment_sprt_abc_1");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment1 = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment1.getName());

    CreateExperimentRun createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project1.getId(), experiment1.getId(), "ExperimentRun_sprt_1");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun1 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun1.getName());

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset1 = createDatasetResponse.getDataset();
    datasetList.add(dataset1);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset1.getName());

    createDatasetRequest = datasetTest.getDatasetRequest("rental_TEXT_train_data_1.csv");
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset2 = createDatasetResponse.getDataset();
    datasetList.add(dataset2);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset2.getName());

    List<String> datasetVersionIds = new ArrayList<>();
    int version = 1;
    // Create two datasetVersion of above datasetVersion
    CreateDatasetVersion createDatasetVersionRequest =
        datasetVersionTest.getDatasetVersionRequest(dataset1.getId());
    KeyValue attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.012).build())
            .build();
    KeyValue attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.99).build())
            .build();
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_2")
            .build();
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionIds.add(datasetVersion1.getId());
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        version,
        datasetVersion1.getVersion());

    // datasetVersion2 of above datasetVersion
    createDatasetVersionRequest = datasetVersionTest.getDatasetVersionRequest(dataset2.getId());
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_3")
            .addTags("Tag_4")
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion2 = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionIds.add(datasetVersion2.getId());
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        version,
        datasetVersion2.getVersion());

    Map<String, Artifact> artifactMap = new HashMap<>();
    for (Artifact existingDataset : experimentRun1.getDatasetsList()) {
      artifactMap.put(existingDataset.getKey(), existingDataset);
    }
    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact1 =
        Artifact.newBuilder()
            .setKey("Google Pay datasets_1")
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactTypeEnum.ArtifactType.DATA)
            .setLinkedArtifactId(datasetVersion1.getId())
            .build();
    artifacts.add(artifact1);
    artifactMap.put(artifact1.getKey(), artifact1);
    Artifact artifact2 =
        Artifact.newBuilder()
            .setKey("Google Pay datasets_2")
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactTypeEnum.ArtifactType.DATA)
            .setLinkedArtifactId(datasetVersion2.getId())
            .build();
    artifacts.add(artifact2);
    artifactMap.put(artifact2.getKey(), artifact2);

    LogDatasets logDatasetRequest =
        LogDatasets.newBuilder().setId(experimentRun1.getId()).addAllDatasets(artifacts).build();

    LogDatasets.Response response = experimentRunServiceStub.logDatasets(logDatasetRequest);
    LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());

    for (Artifact datasetArtifact : response.getExperimentRun().getDatasetsList()) {
      assertEquals(
          "Experiment datasets not match with expected datasets",
          artifactMap.get(datasetArtifact.getKey()),
          datasetArtifact);
    }

    // Create project 2
    createProjectRequest = projectTest.getCreateProjectRequest("experimentRun_project_ypcdt2");
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    projectList.add(project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project2.getName());

    // Create two experiment of above project
    createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project2.getId(), "Experiment_sprt_abc_2");
    createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment2 = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment2.getName());

    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project2.getId(), experiment2.getId(), "ExperimentRun_sprt_2");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun2 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun2.getName());

    createDatasetRequest = datasetTest.getDatasetRequest("rental_TEXT_train_data3.csv");
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset3 = createDatasetResponse.getDataset();
    datasetList.add(dataset3);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset3.getName());

    createDatasetRequest = datasetTest.getDatasetRequest("rental_TEXT_train_data_4.csv");
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset4 = createDatasetResponse.getDataset();
    datasetList.add(dataset4);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset4.getName());

    GetHydratedDatasetsByProjectId getHydratedDatasetsByProjectIdRequest =
        GetHydratedDatasetsByProjectId.newBuilder().build();
    try {
      hydratedServiceBlockingStub.getHydratedDatasetsByProjectId(
          getHydratedDatasetsByProjectIdRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getHydratedDatasetsByProjectIdRequest =
        getHydratedDatasetsByProjectIdRequest.toBuilder().setProjectId(project1.getId()).build();
    GetHydratedDatasetsByProjectId.Response getHydratedDatasetsByProjectIdResponse =
        hydratedServiceBlockingStub.getHydratedDatasetsByProjectId(
            getHydratedDatasetsByProjectIdRequest);
    assertEquals(
        "HydratedDatasets count not match with expected HydratedDatasets count",
        2,
        getHydratedDatasetsByProjectIdResponse.getTotalRecords());

    assertEquals(
        "HydratedDataset not match with expected HydratedDatasets",
        dataset2.getId(),
        getHydratedDatasetsByProjectIdResponse.getHydratedDatasets(0).getDataset().getId());

    for (int pageNumber = 1; pageNumber <= 2; pageNumber++) {
      getHydratedDatasetsByProjectIdRequest =
          getHydratedDatasetsByProjectIdRequest
              .toBuilder()
              .setProjectId(project1.getId())
              .setPageNumber(pageNumber)
              .setPageLimit(1)
              .build();
      getHydratedDatasetsByProjectIdResponse =
          hydratedServiceBlockingStub.getHydratedDatasetsByProjectId(
              getHydratedDatasetsByProjectIdRequest);

      assertEquals(
          "HydratedDatasets count not match with expected HydratedDatasets count",
          2,
          getHydratedDatasetsByProjectIdResponse.getTotalRecords());

      if (pageNumber == 1) {
        assertEquals(
            "HydratedDataset not match with expected HydratedDatasets",
            dataset2.getId(),
            getHydratedDatasetsByProjectIdResponse.getHydratedDatasets(0).getDataset().getId());
      } else {
        assertEquals(
            "HydratedDataset not match with expected HydratedDatasets",
            dataset1.getId(),
            getHydratedDatasetsByProjectIdResponse.getHydratedDatasets(0).getDataset().getId());
      }
    }

    for (String datasetVersionId : datasetVersionIds) {
      DeleteDatasetVersion deleteDatasetVersion =
          DeleteDatasetVersion.newBuilder().setId(datasetVersionId).build();
      DeleteDatasetVersion.Response deleteDatasetVersionResponse =
          datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
      LOGGER.info("DatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionResponse.toString());
      assertTrue(deleteDatasetVersionResponse.getStatus());
    }

    for (Dataset dataset : datasetList) {
      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());
    }

    for (Project project : projectList) {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }

    LOGGER.info("Get Hydrated Datasets By ProjectId test stop................................");
  }

  @Test
  public void findHydratedProjectsByWorkspaceTest() {
    LOGGER.info("FindHydratedProjectsByWorkspace test start................................");

    if (app.getAuthServerHost() == null || app.getAuthServerPort() == null) {
      assertTrue(true);
      return;
    }

    ProjectTest projectTest = new ProjectTest();
    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ProjectServiceBlockingStub user2ProjectServiceStub =
        ProjectServiceGrpc.newBlockingStub(client2Channel);
    HydratedServiceGrpc.HydratedServiceBlockingStub hydratedServiceBlockingStub =
        HydratedServiceGrpc.newBlockingStub(channel);

    Map<String, Project> secondProjectMap = new HashMap<>();
    Map<String, Project> firstProjectMap = new HashMap<>();

    // Create two project of above project
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("Project_1");
    KeyValue attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.012).build())
            .build();
    KeyValue attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.99).build())
            .build();
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_2")
            .build();
    CreateProject.Response createProjectResponse =
        user2ProjectServiceStub.createProject(createProjectRequest);
    Project project1 = createProjectResponse.getProject();
    secondProjectMap.put(project1.getId(), project1);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project1.getName());

    // project2 of above project
    createProjectRequest = projectTest.getCreateProjectRequest("Project_2");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_3")
            .addTags("Tag_4")
            .build();
    createProjectResponse = user2ProjectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    secondProjectMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project2.getName());

    // project3 of above project
    createProjectRequest = projectTest.getCreateProjectRequest("Project_2");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_5")
            .addTags("Tag_6")
            .build();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project3 = createProjectResponse.getProject();
    firstProjectMap.put(project3.getId(), project3);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project3.getName());

    // project4 of above project
    createProjectRequest = projectTest.getCreateProjectRequest("Project_1");
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(1.00).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.001212).build())
            .build();
    createProjectRequest =
        createProjectRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_5")
            .addTags("Tag_7")
            .addTags("Tag_8")
            .setProjectVisibility(ProjectVisibility.PUBLIC)
            .build();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project4 = createProjectResponse.getProject();
    firstProjectMap.put(project4.getId(), project4);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project4.getName());

    UACServiceGrpc.UACServiceBlockingStub uaServiceStub =
        UACServiceGrpc.newBlockingStub(authServiceChannel);
    GetUser getUserRequest =
        GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo secondUserInfo = uaServiceStub.getUser(getUserRequest);

    FindProjects findProjects =
        FindProjects.newBuilder()
            .addPredicates(
                KeyValueQuery.newBuilder()
                    .setKey(ModelDBConstants.NAME)
                    .setValue(Value.newBuilder().setStringValue(project1.getName()).build())
                    .setOperator(OperatorEnum.Operator.EQ)
                    .build())
            .setWorkspaceName(secondUserInfo.getVertaInfo().getUsername())
            .build();

    AdvancedQueryProjectsResponse response =
        hydratedServiceBlockingStub.findHydratedProjects(findProjects);
    List<Project> projectList = new ArrayList<>();
    for (HydratedProject hydratedProject : response.getHydratedProjectsList()) {
      projectList.add(hydratedProject.getProject());
    }
    LOGGER.info("FindProjects Response : " + projectList.size());
    assertEquals("Project count not match with expected project count", 0, projectList.size());

    assertEquals(
        "Total records count not matched with expected records count",
        0,
        response.getTotalRecords());

    for (String projectId : firstProjectMap.keySet()) {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(projectId).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }
    for (String projectId : secondProjectMap.keySet()) {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(projectId).build();
      DeleteProject.Response deleteProjectResponse =
          user2ProjectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }
    LOGGER.info("FindHydratedProjectsByUser test stop................................");
  }
}
