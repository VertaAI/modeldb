package ai.verta.modeldb;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.Action;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetSelfAllowedResources;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ServiceEnum;
import com.google.common.util.concurrent.Futures;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = App.class, webEnvironment = DEFINED_PORT)
@ContextConfiguration(classes = {ModeldbTestConfigurationBeans.class})
public class CommentTest extends ModeldbTestSetup {

  private static final Logger LOGGER = LogManager.getLogger(CommentTest.class);
  // Project Entities
  private Project project;

  // Experiment Entities
  private Experiment experiment;

  // ExperimentRun Entities
  private ExperimentRun experimentRun;
  private List<Comment> commentList = new ArrayList<>();

  @BeforeEach
  public void createEntities() {
    initializeChannelBuilderAndExternalServiceStubs();

    if (isRunningIsolated()) {
      setupMockUacEndpoints(uac);
    }

    createProjectEntities();
    createExperimentEntities();
    createExperimentRunEntities();
  }

  @AfterEach
  public void removeEntities() {
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

    if (isRunningIsolated()) {
      when(uacBlockingMock.getCurrentUser(any())).thenReturn(testUser1);
      mockGetSelfAllowedResources(
          Set.of(project.getId()),
          ModelDBServiceResourceTypes.PROJECT,
          ModelDBServiceActions.DELETE);
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());
    project = null;
    commentList = new ArrayList<>();
  }

  private void createProjectEntities() {
    if (isRunningIsolated()) {
      var resourcesResponse =
          GetResources.Response.newBuilder()
              .addItem(
                  GetResourcesResponseItem.newBuilder()
                      .setWorkspaceId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                      .setOwnerId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                      .build())
              .build();
      when(collaboratorBlockingMock.getResources(any())).thenReturn(resourcesResponse);
    }

    // Create two project of above project
    CreateProject createProjectRequest =
        ProjectTest.getCreateProjectRequest("project-" + new Date().getTime());
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project.getName());

    if (isRunningIsolated()) {
      mockGetResourcesForAllProjects(Map.of(project.getId(), project), testUser1);
      when(uac.getAuthzService()
              .getSelfAllowedResources(
                  GetSelfAllowedResources.newBuilder()
                      .addActions(
                          Action.newBuilder()
                              .setModeldbServiceAction(ModelDBServiceActions.READ)
                              .setService(ServiceEnum.Service.MODELDB_SERVICE))
                      .setService(ServiceEnum.Service.MODELDB_SERVICE)
                      .setResourceType(
                          ResourceType.newBuilder()
                              .setModeldbServiceResourceType(
                                  ModelDBServiceResourceTypes.REPOSITORY))
                      .build()))
          .thenReturn(
              Futures.immediateFuture(GetSelfAllowedResources.Response.newBuilder().build()));
    }
  }

  private void createExperimentEntities() {
    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        ExperimentTest.getCreateExperimentRequestForOtherTests(
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

  private void createExperimentRunEntities() {
    CreateExperimentRun createExperimentRunRequest =
        ExperimentRunTest.getCreateExperimentRunRequestForOtherTests(
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
