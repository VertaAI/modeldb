package ai.verta.modeldb;

import static org.junit.Assert.*;

import ai.verta.modeldb.CommentServiceGrpc.CommentServiceBlockingStub;
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
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.Calendar;
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
public class CommentTest {

  private static final Logger LOGGER = LogManager.getLogger(CommentTest.class);
  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;
  private static String serverName = InProcessServerBuilder.generateName();
  private static InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder channelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static AuthClientInterceptor authClientInterceptor;
  private static App app;

  @SuppressWarnings("unchecked")
  @BeforeClass
  public static void setServerAndService() throws Exception {

    Map<String, Object> propertiesMap =
        ModelDBUtils.readYamlProperties(System.getenv(ModelDBConstants.VERTA_MODELDB_CONFIG));
    Map<String, Object> testPropMap = (Map<String, Object>) propertiesMap.get("test");
    Map<String, Object> databasePropMap = (Map<String, Object>) testPropMap.get("test-database");

    app = App.getInstance();
    AuthService authService = new PublicAuthServiceUtils();
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
  }

  @Before
  public void initializeChannel() throws IOException {
    grpcCleanup.register(serverBuilder.build().start());
    channel = grpcCleanup.register(channelBuilder.maxInboundMessageSize(1024).build());
  }

  @Test
  public void a_addExperimentRunCommentTest() {
    LOGGER.info("Add ExperimentRun comment test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    CommentServiceBlockingStub commentServiceBlockingStub =
        CommentServiceGrpc.newBlockingStub(channel);

    // Create Project
    ProjectTest projectTest = new ProjectTest();
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_e_aert");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project.getName());

    // Create Experiment
    ExperimentTest experimentTest = new ExperimentTest();
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "experiment_e_aertt");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    // Create Experiment Run
    CreateExperimentRun createExperimentRunRequest =
        CreateExperimentRun.newBuilder()
            .setProjectId(project.getId())
            .setExperimentId(experiment.getId())
            .setName("experimentRun_e_aertt")
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    AddComment addComment =
        AddComment.newBuilder()
            .setEntityId(experimentRun.getId())
            .setMessage(
                "Hello, this project is awesome. I am interested to explore it."
                    + Calendar.getInstance().getTimeInMillis())
            .build();
    AddComment.Response response = commentServiceBlockingStub.addExperimentRunComment(addComment);
    LOGGER.info("addProjectComment Response : \n" + response.getComment());
    assertEquals(
        "Comment message not match with expected comment message",
        addComment.getMessage(),
        response.getComment().getMessage());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Add ExperimentRun comment test stop................................");
  }

  @Test
  public void aa_addExperimentRunCommentNegativeTest() {
    LOGGER.info("Add ExperimentRun comment Negative test start................................");
    CommentServiceBlockingStub commentServiceBlockingStub =
        CommentServiceGrpc.newBlockingStub(channel);
    AddComment addComment =
        AddComment.newBuilder()
            .setMessage(
                "Hello, this ExperimentRun is awesome. I am interested to explore it."
                    + Calendar.getInstance().getTimeInMillis())
            .build();
    try {
      commentServiceBlockingStub.addExperimentRunComment(addComment);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
    LOGGER.info("Add ExperimentRun comment Negative test stop................................");
  }

  @Test
  public void b_updateExperimentRunCommentTest() {
    LOGGER.info("Update ExperimentRun comment test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    CommentServiceBlockingStub commentServiceBlockingStub =
        CommentServiceGrpc.newBlockingStub(channel);

    // Create Project
    ProjectTest projectTest = new ProjectTest();
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_e_aert");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project.getName());

    // Create Experiment
    ExperimentTest experimentTest = new ExperimentTest();
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "experiment_e_aertt");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    // Create Experiment Run
    CreateExperimentRun createExperimentRunRequest =
        CreateExperimentRun.newBuilder()
            .setProjectId(project.getId())
            .setExperimentId(experiment.getId())
            .setName("experimentRun_e_aertt")
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    AddComment addComment =
        AddComment.newBuilder()
            .setEntityId(experimentRun.getId())
            .setMessage(
                "Hello, this project is awesome. I am interested to explore it."
                    + Calendar.getInstance().getTimeInMillis())
            .build();
    AddComment.Response addCommentResponse =
        commentServiceBlockingStub.addExperimentRunComment(addComment);
    LOGGER.info("addProjectComment Response : \n" + addCommentResponse.getComment());
    assertEquals(
        "Comment message not match with expected comment message",
        addCommentResponse.getComment().getMessage(),
        addCommentResponse.getComment().getMessage());

    GetComments getCommentsRequest =
        GetComments.newBuilder().setEntityId(experimentRun.getId()).build();
    GetComments.Response getCommentsResponse =
        commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
    if (getCommentsResponse.getCommentsList().isEmpty()) {
      LOGGER.error("Comments not found in database.");
      fail();
      return;
    }

    Comment comment = getCommentsResponse.getCommentsList().get(0);
    LOGGER.info("Existing Comment for update is : \n" + comment);
    String newMessage =
        "Hello, this ExperimentRun is awesome. I am interested to explore it. "
            + Calendar.getInstance().getTimeInMillis();
    UpdateComment updateComment =
        UpdateComment.newBuilder()
            .setId(comment.getId())
            .setEntityId(experimentRun.getId())
            .setMessage(newMessage)
            .build();
    UpdateComment.Response response =
        commentServiceBlockingStub.updateExperimentRunComment(updateComment);
    LOGGER.info("UpdateExperimentRunComment Response : \n" + response.getComment());
    assertEquals(newMessage, response.getComment().getMessage());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Update ExperimentRun comment test stop................................");
  }

  @Test
  public void bb_updateExperimentRunCommentNegativeTest() {
    LOGGER.info("Update ExperimentRun comment Negative test start................................");

    CommentServiceBlockingStub commentServiceBlockingStub =
        CommentServiceGrpc.newBlockingStub(channel);
    String newMessage =
        "Hello, this ExperimentRun is awesome. I am interested to explore it. "
            + Calendar.getInstance().getTimeInMillis();
    UpdateComment updateComment = UpdateComment.newBuilder().setMessage(newMessage).build();
    try {
      commentServiceBlockingStub.updateExperimentRunComment(updateComment);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
    LOGGER.info("Update ExperimentRun comment Negative test stop................................");
  }

  @Test
  public void c_getExperimentRunCommentTest() {
    LOGGER.info("Get ExperimentRun comment test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    CommentServiceBlockingStub commentServiceBlockingStub =
        CommentServiceGrpc.newBlockingStub(channel);

    // Create Project
    ProjectTest projectTest = new ProjectTest();
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_e_aert");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project.getName());

    // Create Experiment
    ExperimentTest experimentTest = new ExperimentTest();
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "experiment_e_aertt");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    // Create Experiment Run
    CreateExperimentRun createExperimentRunRequest =
        CreateExperimentRun.newBuilder()
            .setProjectId(project.getId())
            .setExperimentId(experiment.getId())
            .setName("experimentRun_e_aertt")
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    AddComment addComment =
        AddComment.newBuilder()
            .setEntityId(experimentRun.getId())
            .setMessage(
                "Hello, this project is awesome. I am interested to explore it."
                    + Calendar.getInstance().getTimeInMillis())
            .build();
    AddComment.Response addCommentResponse =
        commentServiceBlockingStub.addExperimentRunComment(addComment);
    LOGGER.info("addProjectComment Response : \n" + addCommentResponse.getComment());
    assertEquals(
        "Comment message not match with expected comment message",
        addComment.getMessage(),
        addCommentResponse.getComment().getMessage());
    Comment expectedComment = addCommentResponse.getComment();

    GetComments getCommentsRequest =
        GetComments.newBuilder().setEntityId(experimentRun.getId()).build();
    GetComments.Response response =
        commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
    LOGGER.info("getExperimentRunComment Response : \n" + response.getCommentsList());
    assertEquals(
        "Comment not match with expected comment",
        expectedComment,
        response.getCommentsList().get(0));

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get ExperimentRun comment test stop................................");
  }

  @Test
  public void cc_getExperimentRunCommentNegativeTest() {
    LOGGER.info("Get ExperimentRun comment Negative test start................................");

    GetComments getCommentsRequest = GetComments.newBuilder().build();
    CommentServiceBlockingStub commentServiceBlockingStub =
        CommentServiceGrpc.newBlockingStub(channel);
    try {
      commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
    LOGGER.info("Get ExperimentRun comment Negative test stop................................");
  }

  @Test
  public void d_deleteExperimentRunCommentTest() {
    LOGGER.info("Delete ExperimentRun comment test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    CommentServiceBlockingStub commentServiceBlockingStub =
        CommentServiceGrpc.newBlockingStub(channel);

    // Create Project
    ProjectTest projectTest = new ProjectTest();
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_e_aert");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project.getName());

    // Create Experiment
    ExperimentTest experimentTest = new ExperimentTest();
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "experiment_e_aertt");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    // Create Experiment Run
    CreateExperimentRun createExperimentRunRequest =
        CreateExperimentRun.newBuilder()
            .setProjectId(project.getId())
            .setExperimentId(experiment.getId())
            .setName("experimentRun_e_aertt")
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    AddComment addComment =
        AddComment.newBuilder()
            .setEntityId(experimentRun.getId())
            .setMessage(
                "Hello, this project is awesome. I am interested to explore it."
                    + Calendar.getInstance().getTimeInMillis())
            .build();
    AddComment.Response addCommentResponse =
        commentServiceBlockingStub.addExperimentRunComment(addComment);
    LOGGER.info("addProjectComment Response : \n" + addCommentResponse.getComment());
    assertEquals(
        "Comment message not match with expected comment message",
        addComment.getMessage(),
        addCommentResponse.getComment().getMessage());

    GetComments getCommentsRequest =
        GetComments.newBuilder().setEntityId(experimentRun.getId()).build();
    GetComments.Response getCommentsResponse =
        commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
    LOGGER.info("getExperimentRunComment Response : \n" + getCommentsResponse.getCommentsList());
    assertEquals(
        "ExperimentRun comments count not match with expected comments count",
        1,
        getCommentsResponse.getCommentsCount());

    Comment comment = getCommentsResponse.getCommentsList().get(0);
    LOGGER.debug("Existing Comment for update is : \n" + comment);

    DeleteComment deleteComment =
        DeleteComment.newBuilder()
            .setId(comment.getId())
            .setEntityId(experimentRun.getId())
            .build();
    DeleteComment.Response response =
        commentServiceBlockingStub.deleteExperimentRunComment(deleteComment);
    LOGGER.info("deleteExperimentRunComment Response : \n" + response.getStatus());
    assertTrue(response.getStatus());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Delete ExperimentRun comment test stop................................");
  }

  @Test
  public void dd_deleteExperimentRunCommentNegativeTest() {
    LOGGER.info("Delete ExperimentRun comment Negative test start................................");
    CommentServiceBlockingStub commentServiceBlockingStub =
        CommentServiceGrpc.newBlockingStub(channel);
    DeleteComment deleteComment = DeleteComment.newBuilder().build();
    try {
      commentServiceBlockingStub.deleteExperimentRunComment(deleteComment);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
    LOGGER.info("Delete ExperimentRun comment Negative test stop................................");
  }
}
