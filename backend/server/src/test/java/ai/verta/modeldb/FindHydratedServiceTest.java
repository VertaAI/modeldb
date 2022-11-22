package ai.verta.modeldb;

import static ai.verta.modeldb.CollaboratorUtils.addCollaboratorRequestProject;
import static ai.verta.modeldb.CollaboratorUtils.addCollaboratorRequestProjectInterceptor;
import static org.junit.Assert.*;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.GetUser;
import ai.verta.uac.UserInfo;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FindHydratedServiceTest extends TestsInit {

  private static final Logger LOGGER =
      LogManager.getLogger(FindHydratedServiceTest.class.getName());

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

  @Before
  public void createEntities() {
    // Create all entities
    createProjectEntities();
    createExperimentEntities();
    createExperimentRun();
  }

  @After
  public void removeEntities() {
    // Delete all data related to project
    DeleteProjects deleteProjects =
        DeleteProjects.newBuilder().addAllIds(projectMap.keySet()).build();
    DeleteProjects.Response deleteProjectsResponse =
        projectServiceStub.deleteProjects(deleteProjects);
    LOGGER.info("Projects deleted successfully");
    LOGGER.info(deleteProjectsResponse.toString());
    assertTrue(deleteProjectsResponse.getStatus());

    projectMap = new HashMap<>();
    experimentRunMap = new HashMap<>();
  }

  private static void createProjectEntities() {
    ProjectTest projectTest = new ProjectTest();

    // Create project1
    CreateProject createProjectRequest =
        projectTest.getCreateProjectRequest("project-1-" + new Date().getTime());
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    project1 = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");

    // Create project2
    createProjectRequest = projectTest.getCreateProjectRequest("project-2-" + new Date().getTime());
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    project2 = createProjectResponse.getProject();
    LOGGER.info("Project2 created successfully");

    projectMap.put(project1.getId(), project1);
    projectMap.put(project2.getId(), project2);
  }

  private static void createExperimentEntities() {
    // Create two experiment of above project
    CreateExperiment request =
        ExperimentTest.getCreateExperimentRequestForOtherTests(
            project1.getId(), "Experiment-1-" + new Date().getTime());
    CreateExperiment.Response response = experimentServiceStub.createExperiment(request);
    experiment1 = response.getExperiment();
    LOGGER.info("Experiment1 created successfully");
    request =
        ExperimentTest.getCreateExperimentRequestForOtherTests(
            project1.getId(), "Experiment-2-" + new Date().getTime());
    response = experimentServiceStub.createExperiment(request);
    experiment2 = response.getExperiment();
    LOGGER.info("Experiment2 created successfully");
  }

  private static void createExperimentRun() {
    CreateExperimentRun createExperimentRunRequest =
        ExperimentRunTest.getCreateExperimentRunRequest(
            project1.getId(), experiment1.getId(), "ExperiemntRun-1-" + new Date().getTime());
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun1 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun1 created successfully");
    createExperimentRunRequest =
        ExperimentRunTest.getCreateExperimentRunRequest(
            project1.getId(), experiment1.getId(), "ExperiemntRun-2-" + new Date().getTime());
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun2 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun2 created successfully");

    // For ExperiemntRun of Experiment2
    createExperimentRunRequest =
        ExperimentRunTest.getCreateExperimentRunRequest(
            project1.getId(), experiment2.getId(), "ExperiemntRun-3-" + new Date().getTime());
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun3 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun3 created successfully");
    createExperimentRunRequest =
        ExperimentRunTest.getCreateExperimentRunRequest(
            project1.getId(), experiment2.getId(), "ExperimentRun-4-" + new Date().getTime());
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

    if (!testConfig.hasAuth()) {
      assertTrue(true);
      return;
    }

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
        collaboratorServiceStubClient1.addOrUpdateProjectCollaborator(addCollaboratorRequest);
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

    if (testConfig.hasAuth()) {
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
      collaboratorServiceStubClient1.addOrUpdateProjectCollaborator(addCollaboratorRequest);
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
