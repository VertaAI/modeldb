package ai.verta.modeldb;

import static org.junit.Assert.*;

import ai.verta.modeldb.CommentServiceGrpc.CommentServiceBlockingStub;
import ai.verta.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub;
import ai.verta.modeldb.ExperimentServiceGrpc.ExperimentServiceBlockingStub;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.authservice.*;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
public class CommentTest {

  private static final Logger LOGGER = LogManager.getLogger(CommentTest.class);
  private static String serverName = InProcessServerBuilder.generateName();
  private static InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder channelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static DeleteEntitiesCron deleteEntitiesCron;

  // Project Entities
  private static Project project;

  // Experiment Entities
  private static Experiment experiment;

  // ExperimentRun Entities
  private static ExperimentRun experimentRun;
  private static List<Comment> commentList;

  // all service stubs
  private static ProjectServiceBlockingStub projectServiceStub;
  private static ExperimentServiceBlockingStub experimentServiceStub;
  private static ExperimentRunServiceBlockingStub experimentRunServiceStub;
  private static CommentServiceBlockingStub commentServiceBlockingStub;

  @SuppressWarnings("unchecked")
  @BeforeClass
  public static void setServerAndService() throws Exception {

    Map<String, Object> propertiesMap =
        ModelDBUtils.readYamlProperties(System.getenv(ModelDBConstants.VERTA_MODELDB_CONFIG));
    Map<String, Object> testPropMap = (Map<String, Object>) propertiesMap.get("test");

    App app = App.getInstance();
    // Set user credentials to App class
    app.setServiceUser(propertiesMap, app);
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

    ModelDBHibernateUtil.runLiquibaseMigration(Config.getInstance().test.database);
    ModelDBHibernateUtil.createOrGetSessionFactory(Config.getInstance().test.database);
    App.initializeServicesBaseOnDataBase(
        serverBuilder, Config.getInstance().test.database, propertiesMap, authService, roleService);
    serverBuilder.intercept(new AuthInterceptor());

    Map<String, Object> testUerPropMap = (Map<String, Object>) testPropMap.get("testUsers");
    if (testUerPropMap != null && testUerPropMap.size() > 0) {
      AuthClientInterceptor authClientInterceptor = new AuthClientInterceptor(testPropMap);
      channelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
    }

    serverBuilder.build().start();
    ManagedChannel channel = channelBuilder.maxInboundMessageSize(1024).build();
    deleteEntitiesCron = new DeleteEntitiesCron(authService, roleService, 1000);

    // Create all service blocking stub
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);
    commentServiceBlockingStub = CommentServiceGrpc.newBlockingStub(channel);

    // Create all entities
    commentList = new ArrayList<>();
    createEntities();
  }

  @AfterClass
  public static void removeServerAndService() {
    App.initiateShutdown(0);

    removeEntities();

    // Delete entities by cron job
    deleteEntitiesCron.run();

    // shutdown test server
    serverBuilder.build().shutdownNow();
  }

  private static void createEntities() {
    createProjectEntities();
    createExperimentEntities();
    createExperimentRunEntities();
  }

  private static void removeEntities() {
    DeleteExperimentRun deleteExperimentRun =
        DeleteExperimentRun.newBuilder().setId(experimentRun.getId()).build();
    DeleteExperimentRun.Response deleteExperimentRunResponse =
        experimentRunServiceStub.deleteExperimentRun(deleteExperimentRun);
    assertTrue(deleteExperimentRunResponse.getStatus());

    // ExperimentRun Entities
    experimentRun = null;

    DeleteExperiment deleteExperiment =
        DeleteExperiment.newBuilder().setId(experiment.getId()).build();
    DeleteExperiment.Response deleteExperimentResponse =
        experimentServiceStub.deleteExperiment(deleteExperiment);
    assertTrue(deleteExperimentResponse.getStatus());
    experiment = null;

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());
    project = null;
    commentList = new ArrayList<>();
  }

  private static void createProjectEntities() {
    ProjectTest projectTest = new ProjectTest();

    // Create two project of above project
    CreateProject createProjectRequest =
        projectTest.getCreateProjectRequest("project-" + new Date().getTime());
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project.getName());
  }

  private static void createExperimentEntities() {

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        ExperimentTest.getCreateExperimentRequest(
            project.getId(), "Experiment-" + new Date().getTime());
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());
  }

  private static void createExperimentRunEntities() {
    CreateExperimentRun createExperimentRunRequest =
        ExperimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());
  }

  @Test
  public void a_addExperimentRunCommentTest() {
    LOGGER.info("Add ExperimentRun comment test start................................");
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
    commentList.add(response.getComment());

    LOGGER.info("Add ExperimentRun comment test stop................................");
  }

  @Test
  public void aa_addExperimentRunCommentNegativeTest() {
    LOGGER.info("Add ExperimentRun comment Negative test start................................");
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
    commentList.add(addCommentResponse.getComment());

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

    LOGGER.info("Update ExperimentRun comment test stop................................");
  }

  @Test
  public void bb_updateExperimentRunCommentNegativeTest() {
    LOGGER.info("Update ExperimentRun comment Negative test start................................");

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
    commentList.add(expectedComment);

    GetComments getCommentsRequest =
        GetComments.newBuilder().setEntityId(experimentRun.getId()).build();
    GetComments.Response response =
        commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
    LOGGER.info("getExperimentRunComment Response : \n" + response.getCommentsList());
    assertTrue(
        "Comment not match with expected comment",
        response.getCommentsList().contains(expectedComment));
    LOGGER.info("Get ExperimentRun comment test stop................................");
  }

  @Test
  public void cc_getExperimentRunCommentNegativeTest() {
    LOGGER.info("Get ExperimentRun comment Negative test start................................");

    GetComments getCommentsRequest = GetComments.newBuilder().build();
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
    commentList.add(addCommentResponse.getComment());

    GetComments getCommentsRequest =
        GetComments.newBuilder().setEntityId(experimentRun.getId()).build();
    GetComments.Response getCommentsResponse =
        commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
    LOGGER.info("getExperimentRunComment Response : \n" + getCommentsResponse.getCommentsList());
    assertEquals(
        "ExperimentRun comments count not match with expected comments count",
        commentList.size(),
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

    LOGGER.info("Delete ExperimentRun comment test stop................................");
  }

  @Test
  public void dd_deleteExperimentRunCommentNegativeTest() {
    LOGGER.info("Delete ExperimentRun comment Negative test start................................");
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
