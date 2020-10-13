package ai.verta.modeldb;

import static org.junit.Assert.*;

import ai.verta.common.Artifact;
import ai.verta.common.ArtifactTypeEnum.ArtifactType;
import ai.verta.common.CollaboratorTypeEnum.CollaboratorType;
import ai.verta.common.KeyValue;
import ai.verta.common.ValueTypeEnum.ValueType;
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
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.CollaboratorServiceGrpc.CollaboratorServiceBlockingStub;
import ai.verta.uac.DeleteOrganization;
import ai.verta.uac.GetCollaborator;
import ai.verta.uac.GetRoleByName;
import ai.verta.uac.GetUser;
import ai.verta.uac.Organization;
import ai.verta.uac.OrganizationServiceGrpc;
import ai.verta.uac.RoleScope;
import ai.verta.uac.RoleServiceGrpc;
import ai.verta.uac.SetOrganization;
import ai.verta.uac.UACServiceGrpc;
import ai.verta.uac.UserInfo;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProjectTest {

  private static final Logger LOGGER = LogManager.getLogger(ProjectTest.class);

  private static String serverName = InProcessServerBuilder.generateName();
  private static InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder client1ChannelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder client2ChannelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static AuthClientInterceptor authClientInterceptor;

  private static App app;
  private static DeleteEntitiesCron deleteEntitiesCron;

  // Project Entities
  private static Project project;
  private static Project project2;
  private static Project project3;
  private static Map<String, Project> projectMap = new HashMap<>();

  // Experiment Entities
  private static Experiment experiment;

  // ExperimentRun Entities
  private static ExperimentRun experimentRun;

  // all service stubs
  private static UACServiceGrpc.UACServiceBlockingStub uacServiceStub;
  private static CollaboratorServiceBlockingStub collaboratorServiceStubClient1;
  private static CollaboratorServiceBlockingStub collaboratorServiceStub;
  private static ProjectServiceBlockingStub projectServiceStub;
  private static ProjectServiceBlockingStub client2ProjectServiceStub;
  private static ExperimentServiceBlockingStub experimentServiceStub;
  private static ExperimentRunServiceBlockingStub experimentRunServiceStub;
  private static CommentServiceBlockingStub commentServiceBlockingStub;
  private static OrganizationServiceGrpc.OrganizationServiceBlockingStub
      organizationServiceBlockingStub;
  private static RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingStub;

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

    ModelDBHibernateUtil.runLiquibaseMigration(databasePropMap);
    App.initializeServicesBaseOnDataBase(
        serverBuilder, databasePropMap, propertiesMap, authService, roleService);
    serverBuilder.intercept(new ModelDBAuthInterceptor());

    Map<String, Object> testUerPropMap = (Map<String, Object>) testPropMap.get("testUsers");
    if (testUerPropMap != null && testUerPropMap.size() > 0) {
      authClientInterceptor = new AuthClientInterceptor(testPropMap);
      client1ChannelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
      client2ChannelBuilder.intercept(authClientInterceptor.getClient2AuthInterceptor());
    }

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      ManagedChannel authServiceChannel =
          ManagedChannelBuilder.forTarget(app.getAuthServerHost() + ":" + app.getAuthServerPort())
              .usePlaintext()
              .intercept(authClientInterceptor.getClient1AuthInterceptor())
              .build();
      uacServiceStub = UACServiceGrpc.newBlockingStub(authServiceChannel);
      organizationServiceBlockingStub = OrganizationServiceGrpc.newBlockingStub(authServiceChannel);
      roleServiceBlockingStub = RoleServiceGrpc.newBlockingStub(authServiceChannel);
      collaboratorServiceStubClient1 = CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

      ManagedChannel authServiceChannelClient2 =
          ManagedChannelBuilder.forTarget(app.getAuthServerHost() + ":" + app.getAuthServerPort())
              .usePlaintext()
              .intercept(authClientInterceptor.getClient2AuthInterceptor())
              .build();
      collaboratorServiceStub = CollaboratorServiceGrpc.newBlockingStub(authServiceChannelClient2);
    }

    serverBuilder.build().start();
    ManagedChannel channel = client1ChannelBuilder.maxInboundMessageSize(1024).build();
    ManagedChannel client2Channel = client2ChannelBuilder.maxInboundMessageSize(1024).build();
    deleteEntitiesCron = new DeleteEntitiesCron(authService, roleService, 100);

    // Create all service blocking stub
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    client2ProjectServiceStub = ProjectServiceGrpc.newBlockingStub(client2Channel);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);
    commentServiceBlockingStub = CommentServiceGrpc.newBlockingStub(channel);
  }

  @AfterClass
  public static void removeServerAndService() {
    App.initiateShutdown(0);

    // Remove all entities
    // removeEntities();
    // Delete entities by cron job
    deleteEntitiesCron.run();

    // shutdown test server
    serverBuilder.build().shutdownNow();
  }

  @Before
  public void createEntities() {
    // Create all entities
    createProjectEntities();
    createExperimentEntities();
    createExperimentRunEntities();
  }

  @After
  public void removeEntities() {
    for (String projectId : projectMap.keySet()) {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(projectId).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }
    project = null;
    project2 = null;
    project3 = null;

    // Experiment Entities
    experiment = null;

    // ExperimentRun Entities
    experimentRun = null;

    projectMap = new HashMap<>();
  }

  private static void createProjectEntities() {
    ProjectTest projectTest = new ProjectTest();

    // Create two project of above project
    CreateProject createProjectRequest =
        projectTest.getCreateProjectRequest("project-" + new Date().getTime());
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    project = createProjectResponse.getProject();
    projectMap.put(project.getId(), project);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project.getName());

    // Create project2
    createProjectRequest = projectTest.getCreateProjectRequest("project-" + new Date().getTime());
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    project2 = createProjectResponse.getProject();
    projectMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project2.getName());

    // Create project3
    createProjectRequest = projectTest.getCreateProjectRequest("project-" + new Date().getTime());
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    project3 = createProjectResponse.getProject();
    projectMap.put(project3.getId(), project3);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project3.getName());
  }

  private static void createExperimentEntities() {

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        ExperimentTest.getCreateExperimentRequest(project.getId(), "Experiment_1");
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
    experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());
  }

  private static void createExperimentRunEntities() {
    ExperimentRunTest experimentRunTest = new ExperimentRunTest();

    CreateExperimentRun createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun_sprt_1");
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
    experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());
  }

  private void checkEqualsAssert(StatusRuntimeException e) {
    Status status = Status.fromThrowable(e);
    LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      assertTrue(
          Status.PERMISSION_DENIED.getCode() == status.getCode()
              || Status.NOT_FOUND.getCode()
                  == status.getCode()); // because of shadow delete the response could be 403 or 404
    } else {
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }
  }

  @Test
  public void a_aVerifyConnection() {
    LOGGER.info("Verify connection test start................................");

    VerifyConnectionResponse response =
        projectServiceStub.verifyConnection(Empty.newBuilder().build());
    assertTrue(response.getStatus());
    LOGGER.info("Verify connection Successfully..");

    LOGGER.info("Verify connection test stop................................");
  }

  public CreateProject getCreateProjectRequest(String projectName) {
    List<KeyValue> metadataList = new ArrayList<>();
    Value stringValue =
        Value.newBuilder()
            .setStringValue("attribute_" + Calendar.getInstance().getTimeInMillis() + "_value")
            .build();
    KeyValue keyValue =
        KeyValue.newBuilder()
            .setKey("attribute_1_" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .build();
    metadataList.add(keyValue);

    Value intValue = Value.newBuilder().setNumberValue(12345).build();
    keyValue =
        KeyValue.newBuilder()
            .setKey("attribute_2_" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build();
    metadataList.add(keyValue);

    Value listValue =
        Value.newBuilder()
            .setListValue(ListValue.newBuilder().addValues(intValue).addValues(stringValue).build())
            .build();
    keyValue =
        KeyValue.newBuilder()
            .setKey("attribute_3_" + Calendar.getInstance().getTimeInMillis())
            .setValue(listValue)
            .setValueType(ValueType.LIST)
            .build();
    metadataList.add(keyValue);

    List<Artifact> artifactList = new ArrayList<>();
    artifactList.add(
        Artifact.newBuilder()
            .setKey("Google developer Artifact")
            .setPath(
                "https://www.google.co.in/imgres?imgurl=https%3A%2F%2Flh3.googleusercontent.com%2FFyZA5SbKPJA7Y3XCeb9-uGwow8pugxj77Z1xvs8vFS6EI3FABZDCDtA9ScqzHKjhU8av_Ck95ET-P_rPJCbC2v_OswCN8A%3Ds688&imgrefurl=https%3A%2F%2Fdevelopers.google.com%2F&docid=1MVaWrOPIjYeJM&tbnid=I7xZkRN5m6_z-M%3A&vet=10ahUKEwjr1OiS0ufeAhWNbX0KHXpFAmQQMwhyKAMwAw..i&w=688&h=387&bih=657&biw=1366&q=google&ved=0ahUKEwjr1OiS0ufeAhWNbX0KHXpFAmQQMwhyKAMwAw&iact=mrc&uact=8")
            .setArtifactType(ArtifactType.BLOB)
            .build());
    artifactList.add(
        Artifact.newBuilder()
            .setKey("Google Pay Artifact")
            .setPath(
                "https://www.google.co.in/imgres?imgurl=https%3A%2F%2Fpay.google.com%2Fabout%2Fstatic%2Fimages%2Fsocial%2Fknowledge_graph_logo.png&imgrefurl=https%3A%2F%2Fpay.google.com%2Fabout%2F&docid=zmoE9BrSKYr4xM&tbnid=eCL1Y6f9xrPtDM%3A&vet=10ahUKEwjr1OiS0ufeAhWNbX0KHXpFAmQQMwhwKAIwAg..i&w=1200&h=630&bih=657&biw=1366&q=google&ved=0ahUKEwjr1OiS0ufeAhWNbX0KHXpFAmQQMwhwKAIwAg&iact=mrc&uact=8")
            .setArtifactType(ArtifactType.IMAGE)
            .build());

    return CreateProject.newBuilder()
        .setName(projectName)
        .setDescription("This is a project description.")
        .addTags("tag_x")
        .addTags("tag_y")
        .addAllAttributes(metadataList)
        .addAllArtifacts(artifactList)
        .build();
  }

  @Test
  public void a_projectCreateTest() {
    LOGGER.info("Create Project test start................................");

    CreateProject createProjectRequest = getCreateProjectRequest(project.getName());

    try {
      projectServiceStub.createProject(createProjectRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    try {
      createProjectRequest = createProjectRequest.toBuilder().addTags("").build();
      projectServiceStub.createProject(createProjectRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      String tag52 = "Human Activity Recognition using Smartphone Dataset";
      createProjectRequest = createProjectRequest.toBuilder().addTags(tag52).build();
      projectServiceStub.createProject(createProjectRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      String name259 =
          "Human Activity Recognition using Smartphone Dataset Human Activity Recognition using Smartphone Dataset Human Activity Recognition using Smartphone Dataset Human Activity Recognition using Smartphone Dataset Human Activity Recognition using Smartphone Dataset";
      createProjectRequest = getCreateProjectRequest(name259);
      projectServiceStub.createProject(createProjectRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Create Project test stop................................");
  }

  @Test
  public void a_projectCreateWithMd5LogicTest() {
    LOGGER.info("Create Project with MD5 logic test start................................");

    List<Project> projects = new ArrayList<>();
    try {
      String projectName = "project_1234567890";
      CreateProject createProjectRequest = getCreateProjectRequest(projectName);
      createProjectRequest = createProjectRequest.toBuilder().build();

      CreateProject.Response response = projectServiceStub.createProject(createProjectRequest);
      assertEquals(projectName, response.getProject().getName());
      assertEquals(projectName, response.getProject().getShortName());
      LOGGER.info("Project Created Successfully");
      projects.add(response.getProject());

      projectName = "project_01234567891234567891234567891234567";
      createProjectRequest = getCreateProjectRequest(projectName);
      createProjectRequest = createProjectRequest.toBuilder().build();
      response = projectServiceStub.createProject(createProjectRequest);
      String projectShortName = projectName;
      projectShortName = ModelDBUtils.convertToProjectShortName(projectShortName);
      assertEquals(projectName, response.getProject().getName());
      assertEquals(projectShortName, response.getProject().getShortName());
      LOGGER.info("Project Created Successfully");
      projects.add(response.getProject());

      projectName = "project URLEncoder 01234567891234567891234567891234567";
      createProjectRequest = getCreateProjectRequest(projectName);
      createProjectRequest = createProjectRequest.toBuilder().build();
      response = projectServiceStub.createProject(createProjectRequest);
      projectShortName = projectName;
      projectShortName = ModelDBUtils.convertToProjectShortName(projectShortName);
      assertEquals(projectName, response.getProject().getName());
      assertEquals(projectShortName, response.getProject().getShortName());
      LOGGER.info("Project Created Successfully");
      projects.add(response.getProject());

      projectName = "Code Ver##_ver-1.1 (((none; git--child)";
      createProjectRequest = getCreateProjectRequest(projectName);
      createProjectRequest = createProjectRequest.toBuilder().build();
      response = projectServiceStub.createProject(createProjectRequest);
      assertEquals(projectName, response.getProject().getName());
      assertEquals("Code-Ver-_ver-1.1-none-git--child-", response.getProject().getShortName());
      LOGGER.info("Project Created Successfully");
      projects.add(response.getProject());

      projectName = "Code Versioning_ver-1.1 (none; git children)";
      createProjectRequest = getCreateProjectRequest(projectName);
      createProjectRequest = createProjectRequest.toBuilder().build();
      response = projectServiceStub.createProject(createProjectRequest);
      projectShortName = projectName;
      projectShortName = ModelDBUtils.convertToProjectShortName(projectShortName);
      assertEquals(projectName, response.getProject().getName());
      assertEquals(projectShortName, response.getProject().getShortName());
      LOGGER.info("Project Created Successfully");
      projects.add(response.getProject());

      createProjectRequest = getCreateProjectRequest("");
      createProjectRequest = createProjectRequest.toBuilder().build();
      try {
        projectServiceStub.createProject(createProjectRequest);
        fail();
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.warn(
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
      }
    } finally {
      for (Project project : projects) {
        // Delete all data related to project
        DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
        DeleteProject.Response deleteProjectResponse =
            projectServiceStub.deleteProject(deleteProject);
        LOGGER.info("Project delete successfully. Status : {}", deleteProjectResponse.toString());
        assertTrue(deleteProjectResponse.getStatus());
      }
    }

    LOGGER.info("Create Project with MD5 logic test stop................................");
  }

  @Test
  public void b_projectCreateNegativeTest() {
    LOGGER.info("Create Project Negative test start................................");

    List<KeyValue> metadataList = new ArrayList<>();
    for (int count = 0; count < 5; count++) {
      Value stringValue =
          Value.newBuilder()
              .setStringValue(
                  "attribute_" + count + "_" + Calendar.getInstance().getTimeInMillis() + "_value")
              .build();
      KeyValue keyValue =
          KeyValue.newBuilder()
              .setKey("attribute_" + count + "_" + Calendar.getInstance().getTimeInMillis())
              .setValue(stringValue)
              .build();
      metadataList.add(keyValue);
    }

    CreateProject request =
        CreateProject.newBuilder()
            .setDescription("This is a project description.")
            .addTags("tag_" + Calendar.getInstance().getTimeInMillis())
            .addTags("tag_" + +Calendar.getInstance().getTimeInMillis())
            .addAllAttributes(metadataList)
            .build();

    try {
      projectServiceStub.createProject(request);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
  }

  @Test
  public void c_updateProjectName() {
    LOGGER.info("Update Project Name test start................................");

    UpdateProjectName updateProjectNameRequest =
        UpdateProjectName.newBuilder()
            .setId(project.getId())
            .setName("Project Name Update 1")
            .build();

    UpdateProjectName.Response response =
        projectServiceStub.updateProjectName(updateProjectNameRequest);
    LOGGER.info("UpdateProjectName Response : " + response.getProject());
    assertEquals(
        "Project name not match with expected project name",
        updateProjectNameRequest.getName(),
        response.getProject().getName());
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        response.getProject().getDateUpdated());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    updateProjectNameRequest =
        UpdateProjectName.newBuilder()
            .setId(project.getId())
            .setName("Project Name Update 2")
            .build();

    response = projectServiceStub.updateProjectName(updateProjectNameRequest);
    LOGGER.info("UpdateProjectName Response : " + response.getProject());
    assertEquals(
        "Project name not match with expected project name",
        updateProjectNameRequest.getName(),
        response.getProject().getName());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    LOGGER.info("Update Project Name test stop................................");
  }

  @Test
  public void cc_updateProjectNameNegativeTest() {
    LOGGER.info("Update Project Name Negative test start................................");

    UpdateProjectName updateProjectNameRequest =
        UpdateProjectName.newBuilder().setName("Update Project Name 1 ").build();

    try {
      projectServiceStub.updateProjectName(updateProjectNameRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Project Id : " + project.getId());
    updateProjectNameRequest =
        UpdateProjectName.newBuilder().setId(project.getId()).setName(project.getName()).build();
    try {
      projectServiceStub.updateProjectName(updateProjectNameRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    LOGGER.info("Update Project Name test stop................................");
  }

  @Test
  public void d_updateProjectDescription() {
    LOGGER.info("Update Project Description test start................................");

    UpdateProjectDescription updateDescriptionRequest =
        UpdateProjectDescription.newBuilder()
            .setId(project.getId())
            .setDescription("Project Description Update 1")
            .build();

    UpdateProjectDescription.Response response =
        projectServiceStub.updateProjectDescription(updateDescriptionRequest);
    LOGGER.info("UpdateProjectDescription Response : " + response.getProject());
    assertEquals(
        "Project description not match with expected project description",
        updateDescriptionRequest.getDescription(),
        response.getProject().getDescription());
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        response.getProject().getDateUpdated());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    updateDescriptionRequest =
        UpdateProjectDescription.newBuilder()
            .setId(project.getId())
            .setDescription("Project Description Update 2")
            .build();

    response = projectServiceStub.updateProjectDescription(updateDescriptionRequest);
    LOGGER.info("UpdateProjectDescription Response : " + response.getProject());
    assertEquals(
        "Project description not match with expected project description",
        updateDescriptionRequest.getDescription(),
        response.getProject().getDescription());
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        response.getProject().getDateUpdated());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    LOGGER.info("Update Project Description test stop................................");
  }

  @Test
  public void dd_updateProjectDescriptionNegativeTest() {
    LOGGER.info("Update Project Description Negative test start................................");

    UpdateProjectDescription updateDescriptionRequest =
        UpdateProjectDescription.newBuilder()
            .setDescription(
                "This is update from UpdateProjectDescription."
                    + Calendar.getInstance().getTimeInMillis())
            .build();

    try {
      projectServiceStub.updateProjectDescription(updateDescriptionRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Update Project Description test stop................................");
  }

  @Test
  public void e_addProjectAttributes() {
    LOGGER.info("Add Project Attributes test start................................");

    List<KeyValue> attributeList = new ArrayList<>();
    Value intValue = Value.newBuilder().setNumberValue(1.1).build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_1" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build());
    Value stringValue =
        Value.newBuilder()
            .setStringValue("attributes_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_2" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.BLOB)
            .build());

    AddProjectAttributes addProjectAttributesRequest =
        AddProjectAttributes.newBuilder()
            .setId(project.getId())
            .addAllAttributes(attributeList)
            .build();

    AddProjectAttributes.Response response =
        projectServiceStub.addProjectAttributes(addProjectAttributesRequest);
    LOGGER.info("Added Project Attributes: \n" + response.getProject());
    assertTrue(response.getProject().getAttributesList().containsAll(attributeList));
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        response.getProject().getDateUpdated());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    LOGGER.info("Add Project Attributes test stop................................");
  }

  @Test
  public void e_addProjectAttributesNegativeTest() {
    LOGGER.info("Add Project Attributes Negative test start................................");

    List<KeyValue> attributeList = new ArrayList<>();
    Value intValue = Value.newBuilder().setNumberValue(1.1).build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build());
    Value stringValue =
        Value.newBuilder()
            .setStringValue("attributes_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.BLOB)
            .build());

    AddProjectAttributes addProjectAttributesRequest =
        AddProjectAttributes.newBuilder().addAllAttributes(attributeList).build();

    try {
      projectServiceStub.addProjectAttributes(addProjectAttributesRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Add Project Attributes Negative test stop................................");
  }

  @Test
  public void e_updateProjectAttributes() {
    LOGGER.info("Update Project Attributes test start................................");

    List<KeyValue> attributes = project.getAttributesList();
    Value stringValue =
        Value.newBuilder()
            .setStringValue(
                "attribute_1542193772147_updated_test_value"
                    + Calendar.getInstance().getTimeInMillis())
            .build();
    KeyValue keyValue =
        KeyValue.newBuilder()
            .setKey(attributes.get(1).getKey())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build();
    UpdateProjectAttributes updateProjectAttributesRequest =
        UpdateProjectAttributes.newBuilder().setId(project.getId()).setAttribute(keyValue).build();

    UpdateProjectAttributes.Response response =
        projectServiceStub.updateProjectAttributes(updateProjectAttributesRequest);
    LOGGER.info("Updated Project : \n" + response.getProject());
    assertTrue(response.getProject().getAttributesList().contains(keyValue));
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        response.getProject().getDateUpdated());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    keyValue =
        KeyValue.newBuilder()
            .setKey(attributes.get(1).getKey())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build();
    updateProjectAttributesRequest =
        UpdateProjectAttributes.newBuilder().setId(project.getId()).setAttribute(keyValue).build();

    response = projectServiceStub.updateProjectAttributes(updateProjectAttributesRequest);
    LOGGER.info("Updated Project : \n" + response.getProject());
    assertTrue(response.getProject().getAttributesList().contains(keyValue));
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        response.getProject().getDateUpdated());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    Value listValue =
        Value.newBuilder()
            .setListValue(ListValue.newBuilder().addValues(intValue).addValues(stringValue).build())
            .build();
    keyValue =
        KeyValue.newBuilder()
            .setKey(attributes.get(0).getKey())
            .setValue(listValue)
            .setValueType(ValueType.LIST)
            .build();
    updateProjectAttributesRequest =
        UpdateProjectAttributes.newBuilder().setId(project.getId()).setAttribute(keyValue).build();

    response = projectServiceStub.updateProjectAttributes(updateProjectAttributesRequest);
    LOGGER.info("Updated Project : \n" + response.getProject());
    assertTrue(response.getProject().getAttributesList().contains(keyValue));
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        response.getProject().getDateUpdated());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    LOGGER.info("Update Project Attributes test stop................................");
  }

  @Test
  public void e_updateProjectAttributesNegativeTest() {
    LOGGER.info("Update Project Attributes Negative test start................................");

    List<KeyValue> attributes = project.getAttributesList();
    Value stringValue = Value.newBuilder().setStringValue("attribute_updated_test_value").build();
    KeyValue keyValue =
        KeyValue.newBuilder()
            .setKey(attributes.get(0).getKey())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build();
    UpdateProjectAttributes updateProjectAttributesRequest =
        UpdateProjectAttributes.newBuilder().setAttribute(keyValue).build();

    try {
      projectServiceStub.updateProjectAttributes(updateProjectAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    updateProjectAttributesRequest =
        UpdateProjectAttributes.newBuilder()
            .setId("sfds")
            .setAttribute(project.getAttributesList().get(0))
            .build();
    try {
      projectServiceStub.updateProjectAttributes(updateProjectAttributesRequest);
      fail();
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    updateProjectAttributesRequest =
        UpdateProjectAttributes.newBuilder().setId(project.getId()).clearAttribute().build();

    try {
      projectServiceStub.updateProjectAttributes(updateProjectAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Update Project Attributes Negative test stop................................");
  }

  @Test
  public void f_addProjectTags() {
    LOGGER.info("Add Project Tags test start................................");

    List<String> tagsList = new ArrayList<>();
    tagsList.add("Add Test Tag1");
    tagsList.add("Add Test Tag2");
    AddProjectTags addProjectTagsRequest =
        AddProjectTags.newBuilder().setId(project.getId()).addAllTags(tagsList).build();

    AddProjectTags.Response response = projectServiceStub.addProjectTags(addProjectTagsRequest);

    Project checkProject = response.getProject();
    assertEquals(4, checkProject.getTagsCount());
    assertEquals(4, checkProject.getTagsList().size());
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        checkProject.getDateUpdated());

    tagsList = new ArrayList<>();
    tagsList.add("Add Test Tag3");
    tagsList.add("Add Test Tag2");
    addProjectTagsRequest =
        AddProjectTags.newBuilder().setId(project.getId()).addAllTags(tagsList).build();

    response = projectServiceStub.addProjectTags(addProjectTagsRequest);

    assertNotEquals(
        "Project date_updated field not update on database",
        checkProject.getDateUpdated(),
        response.getProject().getDateUpdated());

    checkProject = response.getProject();
    assertEquals(5, checkProject.getTagsCount());
    assertEquals(5, checkProject.getTagsList().size());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    try {
      String tag52 = "Human Activity Recognition using Smartphone Dataset";
      addProjectTagsRequest =
          AddProjectTags.newBuilder().setId(project.getId()).addTags(tag52).build();
      projectServiceStub.addProjectTags(addProjectTagsRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Add Project tags test stop................................");
  }

  @Test
  public void ff_addProjectNegativeTags() {
    LOGGER.info("Add Project Tags Negative test start................................");

    List<String> tagsList = new ArrayList<>();
    tagsList.add("Add Test Tag " + Calendar.getInstance().getTimeInMillis());
    tagsList.add("Add Test Tag 2 " + Calendar.getInstance().getTimeInMillis());
    AddProjectTags addProjectTagsRequest = AddProjectTags.newBuilder().addAllTags(tagsList).build();

    try {
      projectServiceStub.addProjectTags(addProjectTagsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    addProjectTagsRequest =
        AddProjectTags.newBuilder().setId("sdasd").addAllTags(project.getTagsList()).build();

    try {
      projectServiceStub.addProjectTags(addProjectTagsRequest);
      fail();
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    LOGGER.info("Add Project tags Negative test stop................................");
  }

  @Test
  public void fff_addProjectTag() {
    LOGGER.info("Add Project Tag test start................................");

    AddProjectTag addProjectTagRequest =
        AddProjectTag.newBuilder().setId(project.getId()).setTag("New added tag").build();

    AddProjectTag.Response response = projectServiceStub.addProjectTag(addProjectTagRequest);

    Project checkProject = response.getProject();
    assertEquals(project.getTagsCount() + 1, checkProject.getTagsCount());
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        checkProject.getDateUpdated());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    try {
      String tag52 = "Human Activity Recognition using Smartphone Dataset";
      addProjectTagRequest =
          AddProjectTag.newBuilder().setId(project.getId()).setTag(tag52).build();
      projectServiceStub.addProjectTag(addProjectTagRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Add Project tags test stop................................");
  }

  @Test
  public void ffff_addProjectTagNegativeTest() {
    LOGGER.info("Add Project Tag negative test start................................");

    try {
      AddProjectTag addProjectTagRequest =
          AddProjectTag.newBuilder().setTag("New added tag").build();
      projectServiceStub.addProjectTag(addProjectTagRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      AddProjectTag addProjectTagRequest = AddProjectTag.newBuilder().setId("xzy").build();
      projectServiceStub.addProjectTag(addProjectTagRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Add Project tags negative test stop................................");
  }

  @Test
  public void g_getProjectTags() {
    LOGGER.info("Get Project Tags test start................................");

    GetTags deleteProjectTagsRequest = GetTags.newBuilder().setId(project.getId()).build();
    GetTags.Response response = projectServiceStub.getProjectTags(deleteProjectTagsRequest);
    LOGGER.info("Tags deleted in server : " + response.getTagsList());
    assertTrue(project.getTagsList().containsAll(response.getTagsList()));

    LOGGER.info("Get Project tags test stop................................");
  }

  @Test
  public void gg_getProjectTagsNegativeTest() {
    LOGGER.info("Get Project Tags Negative test start................................");

    GetTags deleteProjectTagsRequest = GetTags.newBuilder().build();

    try {
      projectServiceStub.getProjectTags(deleteProjectTagsRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Get Project tags Negative test stop................................");
  }

  @Test
  public void h_deleteProjectTags() {
    LOGGER.info("Delete Project Tags test start................................");

    try {
      List<String> removableTags = project.getTagsList();
      if (removableTags.size() == 0) {
        LOGGER.info("Project Tags not found in database ");
        fail();
        return;
      }
      if (project.getTagsList().size() > 1) {
        removableTags = project.getTagsList().subList(0, project.getTagsList().size() - 1);
      }
      DeleteProjectTags deleteProjectTagsRequest =
          DeleteProjectTags.newBuilder().setId(project.getId()).addAllTags(removableTags).build();

      DeleteProjectTags.Response response =
          projectServiceStub.deleteProjectTags(deleteProjectTagsRequest);
      LOGGER.info("Tags deleted in server : " + response.getProject().getTagsList());
      assertTrue(response.getProject().getTagsList().size() <= 1);
      assertNotEquals(
          "Project date_updated field not update on database",
          project.getDateUpdated(),
          response.getProject().getDateUpdated());
      project = response.getProject();
      projectMap.put(project.getId(), project);

      if (response.getProject().getTagsList().size() > 0) {
        deleteProjectTagsRequest =
            DeleteProjectTags.newBuilder().setId(project.getId()).setDeleteAll(true).build();

        response = projectServiceStub.deleteProjectTags(deleteProjectTagsRequest);
        LOGGER.info("Tags deleted in server : " + response.getProject().getTagsList());
        assertEquals(0, response.getProject().getTagsList().size());
        assertNotEquals(
            "Project date_updated field not update on database",
            project.getDateUpdated(),
            response.getProject().getDateUpdated());
        project = response.getProject();
        projectMap.put(project.getId(), project);
      }
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      fail();
    }

    LOGGER.info("Delete Project tags test stop................................");
  }

  @Test
  public void h_deleteProjectTagsNegativeTest() {
    LOGGER.info("Delete Project Tags Negative test start................................");

    DeleteProjectTags deleteProjectTagsRequest = DeleteProjectTags.newBuilder().build();

    try {
      projectServiceStub.deleteProjectTags(deleteProjectTagsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    deleteProjectTagsRequest =
        DeleteProjectTags.newBuilder().setId(project.getId()).setDeleteAll(true).build();

    DeleteProjectTags.Response response =
        projectServiceStub.deleteProjectTags(deleteProjectTagsRequest);
    LOGGER.info("Tags deleted in server : " + response.getProject().getTagsList());
    assertEquals(0, response.getProject().getTagsList().size());

    LOGGER.info("Delete Project tags Negative test stop................................");
  }

  @Test
  public void hhh_deleteProjectTag() {
    LOGGER.info("Delete Project Tag test start................................");

    DeleteProjectTag deleteProjectTagRequest =
        DeleteProjectTag.newBuilder()
            .setId(project.getId())
            .setTag(project.getTagsList().get(0))
            .build();

    DeleteProjectTag.Response response =
        projectServiceStub.deleteProjectTag(deleteProjectTagRequest);
    LOGGER.info("Tag deleted in server : " + response.getProject().getTagsList());
    assertEquals(project.getTagsCount() - 1, response.getProject().getTagsList().size());
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        response.getProject().getDateUpdated());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    LOGGER.info("Delete Project tag test stop................................");
  }

  @Test
  public void hhhh_deleteProjectTagNegativeTest() {
    LOGGER.info("Delete Project Tag negative test start................................");

    try {
      DeleteProjectTag deleteProjectTagRequest =
          DeleteProjectTag.newBuilder().setTag("Tag 1").build();
      projectServiceStub.deleteProjectTag(deleteProjectTagRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      DeleteProjectTag deleteProjectTagRequest = DeleteProjectTag.newBuilder().setId("xyz").build();
      projectServiceStub.deleteProjectTag(deleteProjectTagRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Delete Project tag negative test stop................................");
  }

  @Test
  public void i_getProjectAttributes() {
    LOGGER.info("Get Project Attributes test start................................");

    List<KeyValue> attributes = project.getAttributesList();
    LOGGER.info("Attributes size : " + attributes.size());

    if (attributes.size() == 0) {
      LOGGER.warn("Project Attributes not found in database ");
      fail();
      return;
    }

    List<String> keys = new ArrayList<>();
    if (attributes.size() > 1) {
      for (int index = 0; index < attributes.size() - 1; index++) {
        KeyValue keyValue = attributes.get(index);
        keys.add(keyValue.getKey());
      }
    } else {
      keys.add(attributes.get(0).getKey());
    }
    LOGGER.info("Attributes key size : " + keys.size());

    GetAttributes getProjectAttributesRequest =
        GetAttributes.newBuilder().setId(project.getId()).addAllAttributeKeys(keys).build();

    GetAttributes.Response response =
        projectServiceStub.getProjectAttributes(getProjectAttributesRequest);
    LOGGER.info(response.getAttributesList().toString());
    assertEquals(keys.size(), response.getAttributesList().size());

    getProjectAttributesRequest =
        GetAttributes.newBuilder().setId(project.getId()).setGetAll(true).build();

    response = projectServiceStub.getProjectAttributes(getProjectAttributesRequest);
    LOGGER.info(response.getAttributesList().toString());
    assertEquals(project.getAttributesList().size(), response.getAttributesList().size());

    LOGGER.info("Get Project Attributes test stop................................");
  }

  @Test
  public void i_getProjectAttributesNegativeTest() {
    LOGGER.info("Get Project Attributes Negative test start................................");

    GetAttributes getProjectAttributesRequest = GetAttributes.newBuilder().build();

    try {
      projectServiceStub.getProjectAttributes(getProjectAttributesRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getProjectAttributesRequest =
        GetAttributes.newBuilder().setId("jfhdsjfhdsfjk").setGetAll(true).build();
    try {
      projectServiceStub.getProjectAttributes(getProjectAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      checkEqualsAssert(ex);
    }

    LOGGER.info("Get Project Attributes Negative test stop................................");
  }

  @Test
  public void iii_deleteProjectAttributesTest() {
    LOGGER.info("Delete Project Attributes test start................................");

    List<KeyValue> attributes = project.getAttributesList();
    LOGGER.info("Attributes size : " + attributes.size());
    assertEquals(
        "Attribute list size not match with expected attribute list size", 3, attributes.size());
    List<String> keys = new ArrayList<>();
    if (attributes.size() > 1) {
      for (int index = 0; index < attributes.size() - 1; index++) {
        KeyValue keyValue = attributes.get(index);
        keys.add(keyValue.getKey());
      }
    } else {
      keys.add(attributes.get(0).getKey());
    }
    LOGGER.info("Attributes key size : " + keys.size());

    DeleteProjectAttributes deleteProjectAttributesRequest =
        DeleteProjectAttributes.newBuilder()
            .setId(project.getId())
            .addAllAttributeKeys(keys)
            .build();

    DeleteProjectAttributes.Response response =
        projectServiceStub.deleteProjectAttributes(deleteProjectAttributesRequest);
    LOGGER.info("Attributes deleted in server : " + response.getProject());
    assertEquals(1, response.getProject().getAttributesList().size());
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        response.getProject().getDateUpdated());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    if (response.getProject().getAttributesList().size() != 0) {
      deleteProjectAttributesRequest =
          DeleteProjectAttributes.newBuilder().setId(project.getId()).setDeleteAll(true).build();
      response = projectServiceStub.deleteProjectAttributes(deleteProjectAttributesRequest);
      LOGGER.info(
          "All the Attributes deleted from server. Attributes count : "
              + response.getProject().getAttributesCount());
      assertEquals(0, response.getProject().getAttributesList().size());
      assertNotEquals(
          "Project date_updated field not update on database",
          project.getDateUpdated(),
          response.getProject().getDateUpdated());
      project = response.getProject();
      projectMap.put(project.getId(), project);
    }

    LOGGER.info("Delete Project Attributes test stop................................");
  }

  @Test
  public void iii_deleteProjectAttributesNegativeTest() {
    LOGGER.info("Delete Project Attributes Negative test start................................");

    DeleteProjectAttributes deleteProjectAttributesRequest =
        DeleteProjectAttributes.newBuilder().build();

    try {
      projectServiceStub.deleteProjectAttributes(deleteProjectAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Delete Project Attributes Negative test stop................................");
  }

  @Test
  public void j_getProjectById() {
    LOGGER.info("Get Project by ID test start................................");

    GetProjectById getProject = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response response = projectServiceStub.getProjectById(getProject);
    LOGGER.info("Response List : " + response.getProject());
    assertEquals(
        "Project not match with expected project", project.getId(), response.getProject().getId());

    LOGGER.info("Get project by ID test stop................................");
  }

  @Test
  public void j_getProjectByIdNegativeTest() {
    LOGGER.info("Get Project by ID negative test start................................");

    try {
      GetProjectById getProject = GetProjectById.newBuilder().build();
      projectServiceStub.getProjectById(getProject);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      GetProjectById getProject = GetProjectById.newBuilder().setId("xyz").build();
      projectServiceStub.getProjectById(getProject);
      fail();
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    LOGGER.info("Get project by ID negative test stop................................");
  }

  @Test
  public void k_getProjectByName() {
    LOGGER.info("Get Project by name test start................................");

    Project project = null;
    try {
      // Create project
      CreateProject createProjectRequest =
          getCreateProjectRequest("project-" + new Date().getTime());
      CreateProject.Response createProjectResponse =
          client2ProjectServiceStub.createProject(createProjectRequest);
      project = createProjectResponse.getProject();
      LOGGER.info("Project created successfully");
      assertEquals(
          "Project name not match with expected project name",
          createProjectRequest.getName(),
          project.getName());

      GetProjectByName getProject =
          GetProjectByName.newBuilder().setName(project.getName()).build();

      GetProjectByName.Response response = client2ProjectServiceStub.getProjectByName(getProject);
      LOGGER.info("Response ProjectByUser of Project : " + response.getProjectByUser());
      LOGGER.info("Response SharedProjectsList of Projects : " + response.getSharedProjectsList());
      assertEquals(
          "Project name not match", project.getName(), response.getProjectByUser().getName());
      for (Project sharedProject : response.getSharedProjectsList()) {
        assertEquals("Shared project name not match", project.getName(), sharedProject.getName());
      }

      if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
        AddCollaboratorRequest addCollaboratorRequest =
            CollaboratorTest.addCollaboratorRequestProject(
                project, authClientInterceptor.getClient1Email(), CollaboratorType.READ_WRITE);

        AddCollaboratorRequest.Response addOrUpdateProjectCollaboratorResponse =
            collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
        LOGGER.info(
            "Collaborator added in server : " + addOrUpdateProjectCollaboratorResponse.getStatus());
        assertTrue(addOrUpdateProjectCollaboratorResponse.getStatus());

        GetProjectByName.Response getProjectByNameResponse =
            projectServiceStub.getProjectByName(getProject);
        LOGGER.info(
            "Response ProjectByUser of Project : " + getProjectByNameResponse.getProjectByUser());
        LOGGER.info(
            "Response SharedProjectsList of Projects : "
                + getProjectByNameResponse.getSharedProjectsList());
        assertTrue(
            "Project name not match",
            getProjectByNameResponse.getProjectByUser() == null
                || getProjectByNameResponse.getProjectByUser().getId().isEmpty());
        for (Project sharedProject : getProjectByNameResponse.getSharedProjectsList()) {
          assertEquals("Shared project name not match", project.getName(), sharedProject.getName());
        }

        Project selfProject = null;
        try {
          // Create project
          createProjectRequest = getCreateProjectRequest("project-" + new Date().getTime());
          createProjectResponse = projectServiceStub.createProject(createProjectRequest);
          selfProject = createProjectResponse.getProject();
          LOGGER.info("Project created successfully");
          assertEquals(
              "Project name not match with expected project name",
              createProjectRequest.getName(),
              selfProject.getName());

          getProjectByNameResponse = projectServiceStub.getProjectByName(getProject);
          LOGGER.info(
              "Response ProjectByUser of Project : " + getProjectByNameResponse.getProjectByUser());
          LOGGER.info(
              "Response SharedProjectsList of Projects : "
                  + getProjectByNameResponse.getSharedProjectsList());
          assertEquals(
              "Project name not match",
              selfProject.getName(),
              getProjectByNameResponse.getProjectByUser().getName());
          for (Project sharedProject : getProjectByNameResponse.getSharedProjectsList()) {
            assertEquals(
                "Shared project name not match", selfProject.getName(), sharedProject.getName());
          }

        } finally {
          if (selfProject != null) {
            DeleteProject deleteProject =
                DeleteProject.newBuilder().setId(selfProject.getId()).build();
            DeleteProject.Response deleteProjectResponse =
                projectServiceStub.deleteProject(deleteProject);
            LOGGER.info("Project deleted successfully");
            LOGGER.info(deleteProjectResponse.toString());
            assertTrue(deleteProjectResponse.getStatus());
          }
        }
      }

    } finally {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
      DeleteProject.Response deleteProjectResponse =
          client2ProjectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }

    LOGGER.info("Get project by name test stop................................");
  }

  @Test
  public void k_getProjectByNameWithWorkspace() {
    LOGGER.info("Get Project by name with workspace test start................................");
    if (app.getAuthServerHost() == null || app.getAuthServerPort() == null) {
      assertTrue(true);
      return;
    }

    Project selfProject = null;
    Project project = null;
    try {
      GetUser getUserRequest =
          GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
      // Get the user info by vertaId form the AuthService
      UserInfo secondUserInfo = uacServiceStub.getUser(getUserRequest);

      getUserRequest =
          GetUser.newBuilder().setEmail(authClientInterceptor.getClient1Email()).build();

      // Create project
      CreateProject createProjectRequest =
          getCreateProjectRequest("project-" + new Date().getTime());
      createProjectRequest =
          createProjectRequest
              .toBuilder()
              .setWorkspaceName(secondUserInfo.getVertaInfo().getUsername())
              .build();
      CreateProject.Response createProjectResponse =
          client2ProjectServiceStub.createProject(createProjectRequest);
      project = createProjectResponse.getProject();
      LOGGER.info("Project created successfully");
      assertEquals(
          "Project name not match with expected project name",
          createProjectRequest.getName(),
          project.getName());

      AddCollaboratorRequest addCollaboratorRequest =
          CollaboratorTest.addCollaboratorRequestProject(
              project, authClientInterceptor.getClient1Email(), CollaboratorType.READ_WRITE);

      AddCollaboratorRequest.Response addOrUpdateProjectCollaboratorResponse =
          collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      LOGGER.info(
          "Collaborator added in server : " + addOrUpdateProjectCollaboratorResponse.getStatus());
      assertTrue(addOrUpdateProjectCollaboratorResponse.getStatus());

      // Create project
      createProjectRequest = getCreateProjectRequest("project-" + new Date().getTime());
      createProjectResponse = projectServiceStub.createProject(createProjectRequest);
      selfProject = createProjectResponse.getProject();
      LOGGER.info("Project created successfully");
      assertEquals(
          "Project name not match with expected project name",
          createProjectRequest.getName(),
          selfProject.getName());

      GetProjectByName getProject =
          GetProjectByName.newBuilder()
              .setName(selfProject.getName())
              .setWorkspaceName(secondUserInfo.getVertaInfo().getUsername())
              .build();
      GetProjectByName.Response getProjectByNameResponse =
          projectServiceStub.getProjectByName(getProject);
      LOGGER.info(
          "Response ProjectByUser of Project : " + getProjectByNameResponse.getProjectByUser());
      LOGGER.info(
          "Response SharedProjectsList of Projects : "
              + getProjectByNameResponse.getSharedProjectsList());
      assertTrue(
          "Project name not match",
          getProjectByNameResponse.getProjectByUser() == null
              || getProjectByNameResponse.getProjectByUser().getId().isEmpty());
      for (Project sharedProject : getProjectByNameResponse.getSharedProjectsList()) {
        assertEquals("Shared project name not match", project.getName(), sharedProject.getName());
      }
    } finally {
      if (selfProject != null) {
        DeleteProject deleteProject = DeleteProject.newBuilder().setId(selfProject.getId()).build();
        DeleteProject.Response deleteProjectResponse =
            projectServiceStub.deleteProject(deleteProject);
        LOGGER.info("Project deleted successfully");
        LOGGER.info(deleteProjectResponse.toString());
        assertTrue(deleteProjectResponse.getStatus());
      }

      if (project != null) {
        DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
        DeleteProject.Response deleteProjectResponse =
            client2ProjectServiceStub.deleteProject(deleteProject);
        LOGGER.info("Project deleted successfully");
        LOGGER.info(deleteProjectResponse.toString());
        assertTrue(deleteProjectResponse.getStatus());
      }
    }

    LOGGER.info(
        "Get project by name with Email or Username test stop................................");
  }

  @Test
  public void k_getProjectByNameNegativeTest() {
    LOGGER.info("Get Project by name negative test start................................");

    try {
      GetProjectByName getProject = GetProjectByName.newBuilder().build();
      projectServiceStub.getProjectByName(getProject);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Get project by name negative test stop................................");
  }

  @Test
  public void k_getProjectByNameNotFoundTest() {
    LOGGER.info("Get Project by name NOT_FOUND test start................................");

    try {
      GetProjectByName getProject = GetProjectByName.newBuilder().setName("test").build();
      projectServiceStub.getProjectByName(getProject);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info("Get project by name NOT_FOUND test stop................................");
  }

  @Test
  public void l_getProjects() {
    LOGGER.info("Get Project test start................................");

    GetProjects getProjects = GetProjects.newBuilder().build();
    GetProjects.Response response = projectServiceStub.getProjects(getProjects);
    long alreadyExistsProjCount = response.getTotalRecords();

    Map<String, Project> projectsMap = new HashMap<>();
    // Create project1
    CreateProject createProjectRequest = getCreateProjectRequest("project-" + new Date().getTime());
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
    createProjectRequest = getCreateProjectRequest("project-" + new Date().getTime());
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    projectsMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project2.getName());

    getProjects = GetProjects.newBuilder().build();
    response = projectServiceStub.getProjects(getProjects);
    List<Project> responseList = new ArrayList<>();
    for (Project project : response.getProjectsList()) {
      if (projectsMap.containsKey(project.getId())) {
        responseList.add(project);
      }
    }
    LOGGER.info("GetProjects Count : " + responseList.size());
    assertEquals(
        "Projects count not match with expected projects count",
        projectsMap.size(),
        responseList.size());

    for (String projectId : projectsMap.keySet()) {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(projectId).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }

    LOGGER.info("Get project test stop................................");
  }

  @Test
  public void getProjectsOnDescendingOrder() {
    LOGGER.info("Get Project on descending order test start................................");

    Map<String, Project> projectsMap = new HashMap<>();
    // Create project1
    CreateProject createProjectRequest = getCreateProjectRequest("project-" + new Date().getTime());
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
    createProjectRequest = getCreateProjectRequest("project-" + new Date().getTime());
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    projectsMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project2.getName());

    GetProjects getProjects = GetProjects.newBuilder().build();
    GetProjects.Response response = projectServiceStub.getProjects(getProjects);
    LOGGER.info("GetProjects Count : " + response.getProjectsCount());
    List<Project> responseList = new ArrayList<>();
    for (Project project : response.getProjectsList()) {
      if (projectsMap.containsKey(project.getId())) {
        responseList.add(project);
      }
    }

    assertEquals(
        "Projects count not match with expected projects count",
        projectsMap.size(),
        responseList.size());

    assertEquals(
        "Projects order not match with expected projects order", project2, responseList.get(0));

    assertEquals(
        "Projects order not match with expected projects order", project1, responseList.get(1));

    for (String projectId : projectsMap.keySet()) {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(projectId).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }
    LOGGER.info("Get project on descending order test stop................................");
  }

  @Test
  public void m_getProjectSummary() throws IOException {
    LOGGER.info("Get Project summary test start................................");

    GetSummary getSummaryRequest = GetSummary.newBuilder().setEntityId(project.getId()).build();
    GetSummary.Response response = projectServiceStub.getSummary(getSummaryRequest);
    LOGGER.info("Response Project Summary : " + ModelDBUtils.getStringFromProtoObject(response));

    GetExperimentsInProject getExperiment =
        GetExperimentsInProject.newBuilder().setProjectId(project.getId()).build();
    GetExperimentsInProject.Response experimentResponse =
        experimentServiceStub.getExperimentsInProject(getExperiment);
    long existingExperimentCount = (long) experimentResponse.getExperimentsList().size();

    GetExperimentRunsInProject getExperimentRun =
        GetExperimentRunsInProject.newBuilder().setProjectId(project.getId()).build();

    GetExperimentRunsInProject.Response experimentRunResponse =
        experimentRunServiceStub.getExperimentRunsInProject(getExperimentRun);
    long existingExperimentRunCount = (long) experimentRunResponse.getExperimentRunsList().size();

    // In double[], Index 0 = minValue, Index 1 = maxValue
    Map<String, Double[]> minMaxMetricsValueMap = new HashMap<>();
    ExperimentRun lastModifiedExperimentRun = null;
    Set<String> keySet = new HashSet<>();
    for (ExperimentRun experimentRunOfSummary : experimentRunResponse.getExperimentRunsList()) {
      if (lastModifiedExperimentRun == null
          || lastModifiedExperimentRun.getDateUpdated() < experimentRunOfSummary.getDateUpdated()) {
        lastModifiedExperimentRun = experimentRunOfSummary;
      }
      for (KeyValue keyValue : experimentRunOfSummary.getMetricsList()) {
        keySet.add(keyValue.getKey());
        minMaxMetricsValueMap = getMinMaxMetricsValueMap(minMaxMetricsValueMap, keyValue);
      }
    }

    if (lastModifiedExperimentRun != null) {
      LastModifiedExperimentRunSummary lastModifiedExperimentRunSummary =
          response.getLastModifiedExperimentRunSummary();
      assertEquals("Project name does not match", project.getName(), response.getName());
      assertEquals(
          "Experiment count does not match",
          existingExperimentCount,
          response.getTotalExperiment());
      assertEquals(
          "Experiment count does not match",
          existingExperimentRunCount,
          response.getTotalExperimentRuns());

      assertEquals(
          "ExperimentRun does not match with expected experimentRun",
          lastModifiedExperimentRun,
          experimentRun);

      assertTrue(
          "last modified experimentRun summary does not match",
          lastModifiedExperimentRun.getName().equals(lastModifiedExperimentRunSummary.getName())
              && lastModifiedExperimentRun.getDateUpdated()
                  == lastModifiedExperimentRunSummary.getLastUpdatedTime());
      assertEquals("Total metrics does not match", keySet.size(), response.getMetricsCount());
    }

    for (MetricsSummary metricsSummary : response.getMetricsList()) {
      // In double[], Index 0 = minValue, Index 1 = maxValue
      Double[] minMaxValueArray = minMaxMetricsValueMap.get(metricsSummary.getKey());
      assertEquals(
          "Metrics minimum value does not match, Key : " + metricsSummary.getKey(),
          minMaxValueArray[0],
          (Double) metricsSummary.getMinValue());
      assertEquals(
          "Metrics maximum value does not match, Key : " + metricsSummary.getKey(),
          minMaxValueArray[1],
          (Double) metricsSummary.getMaxValue());
    }
    LOGGER.info("Get project summary test stop................................");
  }

  private Map<String, Double[]> getMinMaxMetricsValueMap(
      Map<String, Double[]> minMaxMetricsValueMap, KeyValue keyValue) {
    Double value = keyValue.getValue().getNumberValue();
    Double[] minMaxValueArray = minMaxMetricsValueMap.get(keyValue.getKey());
    if (minMaxValueArray == null) {
      minMaxValueArray = new Double[2]; // Index 0 = minValue, Index 1 = maxValue
    }
    if (minMaxValueArray[0] == null || minMaxValueArray[0] > value) {
      minMaxValueArray[0] = value;
    }
    if (minMaxValueArray[1] == null || minMaxValueArray[1] < value) {
      minMaxValueArray[1] = value;
    }
    minMaxMetricsValueMap.put(keyValue.getKey(), minMaxValueArray);
    return minMaxMetricsValueMap;
  }

  @Test
  public void m_getProjectSummaryNegativeTest() {
    LOGGER.info("Get Project summary Negative test start................................");

    try {
      GetSummary getSummaryRequest = GetSummary.newBuilder().build();
      projectServiceStub.getSummary(getSummaryRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
    LOGGER.info("Get project summary Negative test stop................................");
  }

  @Test
  public void n_setProjectReadMeTest() throws IOException {
    LOGGER.info("Set Project ReadMe test start................................");
    try {
      String rootPath = System.getProperty("user.dir");
      rootPath = rootPath + "/src/test/java/ai/verta/modeldb/setProjectReadMe.md";
      String readMeText = ModelDBTestUtils.readFile(rootPath);
      SetProjectReadme setProjectReadMe =
          SetProjectReadme.newBuilder().setId(project.getId()).setReadmeText(readMeText).build();
      SetProjectReadme.Response response = projectServiceStub.setProjectReadme(setProjectReadMe);
      assertEquals(
          "Project ID not match with expected project ID",
          project.getId(),
          response.getProject().getId());
      assertEquals(
          "Project ReadMe text not match with expected ReadMe text",
          readMeText,
          response.getProject().getReadmeText());
      assertNotEquals(
          "Project date_updated field not update on database",
          project.getDateUpdated(),
          response.getProject().getDateUpdated());
      project = response.getProject();
      projectMap.put(project.getId(), project);
    } catch (StatusRuntimeException e) {
      Status status2 = Status.fromThrowable(e);
      fail();
      LOGGER.info("Error Code : " + status2.getCode() + " Error : " + status2.getDescription());
    }

    LOGGER.info("Set Project ReadMe test stop................................");
  }

  @Test
  public void nn_updateProjectReadMeTest() throws IOException {
    LOGGER.info("Update Project ReadMe test start................................");
    try {
      String rootPath = System.getProperty("user.dir");
      rootPath = rootPath + "/src/test/java/ai/verta/modeldb/setProjectReadMe.md";
      String readMeText = ModelDBTestUtils.readFile(rootPath);
      SetProjectReadme setProjectReadMe =
          SetProjectReadme.newBuilder().setId(project.getId()).setReadmeText(readMeText).build();
      SetProjectReadme.Response response = projectServiceStub.setProjectReadme(setProjectReadMe);
      assertEquals(
          "Project ID not match with expected project ID",
          project.getId(),
          response.getProject().getId());
      assertEquals(
          "Project ReadMe text not match with expected setProjectReadMe.md file text",
          readMeText,
          response.getProject().getReadmeText());
      assertNotEquals(
          "Project date_updated field not update on database",
          project.getDateUpdated(),
          response.getProject().getDateUpdated());
      project = response.getProject();
      projectMap.put(project.getId(), project);

      rootPath = System.getProperty("user.dir");
      rootPath = rootPath + "/src/test/java/ai/verta/modeldb/updateProjectReadMe.md";
      readMeText = ModelDBTestUtils.readFile(rootPath);
      setProjectReadMe =
          SetProjectReadme.newBuilder().setId(project.getId()).setReadmeText(readMeText).build();
      response = projectServiceStub.setProjectReadme(setProjectReadMe);
      assertEquals(
          "Project ID not match with expected project ID",
          project.getId(),
          response.getProject().getId());
      assertEquals(
          "Project ReadMe text not match with expected updateProjectReadMe.md file text",
          readMeText,
          response.getProject().getReadmeText());
      assertNotEquals(
          "Project date_updated field not update on database",
          project.getDateUpdated(),
          response.getProject().getDateUpdated());
      project = response.getProject();
      projectMap.put(project.getId(), project);
    } catch (StatusRuntimeException e) {
      Status status2 = Status.fromThrowable(e);
      fail();
      LOGGER.info("Error Code : " + status2.getCode() + " Error : " + status2.getDescription());
    }

    LOGGER.info("Update Project ReadMe test stop................................");
  }

  @Test
  public void nnn_getProjectReadMeTest() throws IOException {
    LOGGER.info("Get Project ReadMe test start................................");
    try {
      String rootPath = System.getProperty("user.dir");
      rootPath = rootPath + "/src/test/java/ai/verta/modeldb/setProjectReadMe.md";
      String readMeText = ModelDBTestUtils.readFile(rootPath);
      SetProjectReadme setProjectReadMe =
          SetProjectReadme.newBuilder().setId(project.getId()).setReadmeText(readMeText).build();
      SetProjectReadme.Response response = projectServiceStub.setProjectReadme(setProjectReadMe);
      assertEquals(
          "Project ID not match with expected project ID",
          project.getId(),
          response.getProject().getId());
      assertEquals(
          "Project ReadMe text not match with expected setProjectReadMe.md file text",
          readMeText,
          response.getProject().getReadmeText());

      GetProjectReadme getProjectReadMe =
          GetProjectReadme.newBuilder().setId(project.getId()).build();
      GetProjectReadme.Response getProjectReadMeResponse =
          projectServiceStub.getProjectReadme(getProjectReadMe);
      assertEquals(
          "Project ReadMe text not match with expected setProjectReadMe.md file text",
          readMeText,
          getProjectReadMeResponse.getReadmeText());

      // Get the file reference
      Path path = Paths.get("target/outputProjectReadMe.md");
      try (BufferedWriter writer = Files.newBufferedWriter(path)) {
        writer.write(getProjectReadMeResponse.getReadmeText());
      }
    } catch (StatusRuntimeException e) {
      Status status2 = Status.fromThrowable(e);
      fail();
      LOGGER.info("Error Code : " + status2.getCode() + " Error : " + status2.getDescription());
    }

    LOGGER.info("Get Project ReadMe test stop................................");
  }

  @Test
  public void nnnn_setEmptyProjectReadMeTest() throws IOException {
    LOGGER.info("Get Project ReadMe test start................................");
    try {
      String readMeText = "";
      SetProjectReadme setProjectReadMe =
          SetProjectReadme.newBuilder().setId(project.getId()).setReadmeText(readMeText).build();
      SetProjectReadme.Response response = projectServiceStub.setProjectReadme(setProjectReadMe);
      assertEquals(
          "Project ID not match with expected project ID",
          project.getId(),
          response.getProject().getId());
      assertEquals(
          "Project ReadMe text not match with expected setProjectReadMe.md file text",
          readMeText,
          response.getProject().getReadmeText());
      assertNotEquals(
          "Project date_updated field not update on database",
          project.getDateUpdated(),
          response.getProject().getDateUpdated());

      GetProjectReadme getProjectReadMe =
          GetProjectReadme.newBuilder().setId(project.getId()).build();
      GetProjectReadme.Response getProjectReadMeResponse =
          projectServiceStub.getProjectReadme(getProjectReadMe);
      assertEquals(
          "Project ReadMe text not match with expected setProjectReadMe.md file text",
          readMeText,
          getProjectReadMeResponse.getReadmeText());
      project = response.getProject();
      projectMap.put(project.getId(), project);

      // Get the file reference
      Path path = Paths.get("target/outputProjectReadMe.md");
      try (BufferedWriter writer = Files.newBufferedWriter(path)) {
        writer.write(getProjectReadMeResponse.getReadmeText());
      }

    } catch (StatusRuntimeException e) {
      Status status2 = Status.fromThrowable(e);
      fail();
      LOGGER.info("Error Code : " + status2.getCode() + " Error : " + status2.getDescription());
    }

    LOGGER.info("Get Project ReadMe test stop................................");
  }

  @Test
  public void p_SetProjectVisibility() {
    LOGGER.info("Set Project visibility test start................................");

    Project project = null;
    try {
      // Create public project
      // Public project 1
      CreateProject createProjectRequest =
          getCreateProjectRequest("project-" + new Date().getTime());
      createProjectRequest =
          createProjectRequest.toBuilder().setProjectVisibility(ProjectVisibility.PUBLIC).build();
      CreateProject.Response createProjectResponse =
          projectServiceStub.createProject(createProjectRequest);
      project = createProjectResponse.getProject();
      assertEquals(
          "Project name not match with expected project name",
          createProjectRequest.getName(),
          project.getName());
      assertEquals(
          "Project visibility not match with expected project visibility",
          ProjectVisibility.PUBLIC,
          project.getProjectVisibility());
      LOGGER.info("Project created successfully");

      SetProjectVisibilty setProjectVisibilty =
          SetProjectVisibilty.newBuilder()
              .setId(project.getId())
              .setProjectVisibility(ProjectVisibility.PRIVATE)
              .build();
      SetProjectVisibilty.Response response =
          projectServiceStub.setProjectVisibility(setProjectVisibilty);
      Project visibilityProject = response.getProject();
      assertEquals(
          "Project name not match with expected project name",
          project.getName(),
          visibilityProject.getName());
      assertEquals(
          "Project visibility not match with updated project visibility",
          ProjectVisibility.PRIVATE,
          visibilityProject.getProjectVisibility());
      LOGGER.info("Set project visibility successfully");
      assertNotEquals(
          "Project date_updated field not update on database",
          project.getDateUpdated(),
          response.getProject().getDateUpdated());
      project = response.getProject();
    } finally {
      if (project != null) {
        DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
        DeleteProject.Response deleteProjectResponse =
            projectServiceStub.deleteProject(deleteProject);
        LOGGER.info("Project deleted successfully");
        LOGGER.info(deleteProjectResponse.toString());
        assertTrue(deleteProjectResponse.getStatus());
      }
    }

    LOGGER.info("Set Project visibility test stop................................");
  }

  @Test
  public void q_setProjectShortNameTest() {
    LOGGER.info("Set Project short name test start................................");
    try {
      try {
        SetProjectShortName setProjectShortName =
            SetProjectShortName.newBuilder()
                .setShortName("project xyz")
                .setId(project.getId())
                .build();
        projectServiceStub.setProjectShortName(setProjectShortName);
        fail();
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.info("Error Code : " + status.getCode() + " Error : " + status.getDescription());
        assertEquals(Status.ABORTED.getCode(), status.getCode());
      }

      String shortName = "project-sprt_app";
      SetProjectShortName setProjectShortName =
          SetProjectShortName.newBuilder().setShortName(shortName).setId(project.getId()).build();
      SetProjectShortName.Response response =
          projectServiceStub.setProjectShortName(setProjectShortName);
      assertEquals(
          "Project ID not match with expected project ID",
          project.getId(),
          response.getProject().getId());
      assertEquals(
          "Project short name not match with expected short name",
          shortName,
          response.getProject().getShortName());
      assertNotEquals(
          "Project date_updated field not update on database",
          project.getDateUpdated(),
          response.getProject().getDateUpdated());
      project = response.getProject();
      projectMap.put(project.getId(), project);

      try {
        setProjectShortName =
            SetProjectShortName.newBuilder().setShortName(shortName).setId(project.getId()).build();
        projectServiceStub.setProjectShortName(setProjectShortName);
        fail();
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.info("Error Code : " + status.getCode() + " Error : " + status.getDescription());
        assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
      }
    } catch (StatusRuntimeException e) {
      Status status2 = Status.fromThrowable(e);
      fail();
      LOGGER.error("Error Code : " + status2.getCode() + " Error : " + status2.getDescription());
    }

    LOGGER.info("Set Project short name test stop................................");
  }

  @Test
  public void qq_setProjectShortNameNegativeTest() {
    LOGGER.info("Set Project short name negative test start................................");
    try {
      SetProjectShortName setProjectShortName =
          SetProjectShortName.newBuilder().setShortName("project xyz").build();
      projectServiceStub.setProjectShortName(setProjectShortName);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.info("Error Code : " + status.getCode() + " Error : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      SetProjectShortName setProjectShortName =
          SetProjectShortName.newBuilder().setId("abc").build();
      projectServiceStub.setProjectShortName(setProjectShortName);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.info("Error Code : " + status.getCode() + " Error : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
    LOGGER.info("Set Project short name negative test stop................................");
  }

  @Test
  public void qqq_getProjectShortNameTest() {
    LOGGER.info("Get Project short name test start................................");
    Project project = null;
    try {
      // Create project
      String shortName = "project-sprt_app";
      CreateProject createProjectRequest = getCreateProjectRequest(shortName);
      CreateProject.Response createProjectResponse =
          projectServiceStub.createProject(createProjectRequest);
      project = createProjectResponse.getProject();
      LOGGER.info("Project created successfully");
      assertEquals(
          "Project name not match with expected project name",
          createProjectRequest.getName(),
          project.getName());

      GetProjectShortName setProjectShortName =
          GetProjectShortName.newBuilder().setId(project.getId()).build();
      GetProjectShortName.Response response =
          projectServiceStub.getProjectShortName(setProjectShortName);
      assertEquals(
          "Project short name not match with expected short name",
          shortName,
          response.getShortName());
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      fail();
      LOGGER.error("Error Code : " + status.getCode() + " Error : " + status.getDescription());
    } finally {
      if (project != null) {
        DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
        DeleteProject.Response deleteProjectResponse =
            projectServiceStub.deleteProject(deleteProject);
        LOGGER.info("Project deleted successfully");
        LOGGER.info(deleteProjectResponse.toString());
        assertTrue(deleteProjectResponse.getStatus());
      }
    }

    LOGGER.info("Get Project short name test stop................................");
  }

  @Test
  public void qqqq_getProjectShortNameNegativeTest() {
    LOGGER.info("Get Project short name Negative test start................................");
    try {
      GetProjectShortName setProjectShortName = GetProjectShortName.newBuilder().build();
      projectServiceStub.getProjectShortName(setProjectShortName);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.info("Error Code : " + status.getCode() + " Error : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
    LOGGER.info("Get Project short name Negative test stop................................");
  }

  @Test
  public void x_projectDeleteNegativeTest() {
    LOGGER.info("Project delete Negative test start................................");

    DeleteProject deleteProject = DeleteProject.newBuilder().build();
    try {
      projectServiceStub.deleteProject(deleteProject);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.info("Error Code : " + status.getCode() + " Error : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Project delete Negative test stop................................");
  }

  @Test
  public void y_projectCascadeDeleteTest() {
    LOGGER.info("Project delete with cascading test start................................");
    try {
      deleteEntitiesCron.run();

      ExperimentRunTest experimentRunTest = new ExperimentRunTest();

      Project project = null;
      ExperimentRun experimentRun1;
      ExperimentRun experimentRun3;
      try {
        // Create project
        CreateProject createProjectRequest =
            getCreateProjectRequest("project-" + new Date().getTime());
        CreateProject.Response createProjectResponse =
            projectServiceStub.createProject(createProjectRequest);
        project = createProjectResponse.getProject();
        LOGGER.info("\n Project created successfully \n");

        // Create two experiment of above project
        CreateExperiment request =
            ExperimentTest.getCreateExperimentRequest(
                project.getId(), "Experiment-1-" + new Date().getTime());
        CreateExperiment.Response response = experimentServiceStub.createExperiment(request);
        Experiment experiment1 = response.getExperiment();
        LOGGER.info("\n Experiment1 created successfully \n");
        request =
            ExperimentTest.getCreateExperimentRequest(
                project.getId(), "Experiment-2-" + new Date().getTime());
        response = experimentServiceStub.createExperiment(request);
        Experiment experiment2 = response.getExperiment();
        LOGGER.info("\n Experiment2 created successfully \n");

        // Create four ExperimentRun of above two experiment, each experiment has two experimentRun
        // For ExperiemntRun of Experiment1
        CreateExperimentRun createExperimentRunRequest =
            experimentRunTest.getCreateExperimentRunRequest(
                project.getId(), experiment1.getId(), "ExperiemntRun-1-" + new Date().getTime());
        CreateExperimentRun.Response createExperimentRunResponse =
            experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
        experimentRun1 = createExperimentRunResponse.getExperimentRun();
        LOGGER.info("\n ExperimentRun1 created successfully \n");
        createExperimentRunRequest =
            experimentRunTest.getCreateExperimentRunRequest(
                project.getId(), experiment1.getId(), "ExperiemntRun-2-" + new Date().getTime());
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
        LOGGER.info("\n ExperimentRun2 created successfully \n");

        // For ExperiemntRun of Experiment2
        createExperimentRunRequest =
            experimentRunTest.getCreateExperimentRunRequest(
                project.getId(), experiment2.getId(), "ExperiemntRun-3-" + new Date().getTime());
        createExperimentRunResponse =
            experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
        experimentRun3 = createExperimentRunResponse.getExperimentRun();
        LOGGER.info("\n ExperimentRun3 created successfully \n");
        createExperimentRunRequest =
            experimentRunTest.getCreateExperimentRunRequest(
                project.getId(), experiment2.getId(), "ExperimentRun-4-" + new Date().getTime());
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
        LOGGER.info("\n ExperimentRun4 created successfully \n");

        // Create comment for above experimentRun1 & experimentRun3
        // comment for experiment1
        AddComment addCommentRequest =
            AddComment.newBuilder()
                .setEntityId(experimentRun1.getId())
                .setMessage(
                    "Hello, this project is intreasting."
                        + Calendar.getInstance().getTimeInMillis())
                .build();
        commentServiceBlockingStub.addExperimentRunComment(addCommentRequest);
        LOGGER.info("\n Comment added successfully for ExperimentRun1 \n");
        // comment for experimentRun3
        addCommentRequest =
            AddComment.newBuilder()
                .setEntityId(experimentRun3.getId())
                .setMessage(
                    "Hello, this project is intreasting."
                        + Calendar.getInstance().getTimeInMillis())
                .build();
        commentServiceBlockingStub.addExperimentRunComment(addCommentRequest);
        LOGGER.info("\n Comment added successfully for ExperimentRun3 \n");

        // Create two collaborator for above project
        // For Collaborator1
        if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
          AddCollaboratorRequest addCollaboratorRequest =
              CollaboratorTest.addCollaboratorRequestProjectInterceptor(
                  project, CollaboratorType.READ_WRITE, authClientInterceptor);
          collaboratorServiceStubClient1.addOrUpdateProjectCollaborator(addCollaboratorRequest);
          LOGGER.info("\n Collaborator1 added successfully \n");
        }
      } finally {
        if (project != null) {
          // Delete all data related to project
          DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
          DeleteProject.Response deleteProjectResponse =
              projectServiceStub.deleteProject(deleteProject);
          LOGGER.info("Project deleted successfully");
          LOGGER.info(deleteProjectResponse.toString());
          assertTrue(deleteProjectResponse.getStatus());
        }
      }

      // Delete entities by cron job
      // 3 calls to ensure all P, E and ER are deleted.
      deleteEntitiesCron.run();
      deleteEntitiesCron.run();
      deleteEntitiesCron.run();

      // Start cross-checking of deleted the project all data from DB from here.
      try {
        GetProjectById getProject = GetProjectById.newBuilder().setId(project.getId()).build();
        projectServiceStub.getProjectById(getProject);
        fail();
      } catch (StatusRuntimeException e) {
        checkEqualsAssert(e);
      }

      // Start cross-checking for experiment

      try {
        GetExperimentsInProject getExperiment =
            GetExperimentsInProject.newBuilder().setProjectId(project.getId()).build();
        experimentServiceStub.getExperimentsInProject(getExperiment);
        if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
          fail();
        }
      } catch (StatusRuntimeException ex) {
        checkEqualsAssert(ex);
      }

      // Start cross-checking for experimentRun
      try {
        GetExperimentRunsInProject getExperimentRuns =
            GetExperimentRunsInProject.newBuilder().setProjectId(project.getId()).build();
        GetExperimentRunsInProject.Response runResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRuns);
        if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
          assertEquals(0, runResponse.getExperimentRunsCount());
          assertEquals(0, runResponse.getTotalRecords());
        }
      } catch (StatusRuntimeException e) {
        checkEqualsAssert(e);
      }

      // Start cross-checking for comment of experimentRun
      // For experimentRun1
      GetComments getCommentsRequest =
          GetComments.newBuilder().setEntityId(experimentRun1.getId()).build();
      GetComments.Response getCommentsResponse;
      try {
        commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
        if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
          fail();
        }
      } catch (StatusRuntimeException e) {
        checkEqualsAssert(e);
      }
      // For experimentRun3
      getCommentsRequest = GetComments.newBuilder().setEntityId(experimentRun3.getId()).build();
      try {
        getCommentsResponse =
            commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
        assertTrue(getCommentsResponse.getCommentsList().isEmpty());
      } catch (StatusRuntimeException e) {
        checkEqualsAssert(e);
      }

      // Start cross-checking for project collaborator
      if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
        GetCollaborator getCollaboratorRequest =
            GetCollaborator.newBuilder().setEntityId(project.getId()).build();
        try {
          collaboratorServiceStubClient1.getProjectCollaborators(getCollaboratorRequest);
          fail();
        } catch (StatusRuntimeException e) {
          checkEqualsAssert(e);
        }
      }

      List<String> projectIds = new ArrayList<>();
      try {
        for (int count = 0; count < 5; count++) {
          // Create project
          CreateProject createProjectRequest =
              getCreateProjectRequest("project-" + new Date().getTime() + "-" + count);
          CreateProject.Response createProjectResponse =
              projectServiceStub.createProject(createProjectRequest);
          projectIds.add(createProjectResponse.getProject().getId());
          project = createProjectResponse.getProject();
          LOGGER.info("\n Project created successfully \n");

          // Create two experiment of above project
          CreateExperiment request =
              ExperimentTest.getCreateExperimentRequest(project.getId(), "Experiment1_" + count);
          CreateExperiment.Response response = experimentServiceStub.createExperiment(request);
          Experiment experiment1 = response.getExperiment();
          LOGGER.info("\n Experiment1 created successfully \n");
          request =
              ExperimentTest.getCreateExperimentRequest(project.getId(), "Experiment2_" + count);
          response = experimentServiceStub.createExperiment(request);
          Experiment experiment2 = response.getExperiment();
          LOGGER.info("\n Experiment2 created successfully \n");

          // Create four ExperimentRun of above two experiment, each experiment has two
          // experimentRun
          // For ExperiemntRun of Experiment1
          CreateExperimentRun createExperimentRunRequest =
              experimentRunTest.getCreateExperimentRunRequest(
                  project.getId(), experiment1.getId(), "ExperiemntRun1_" + count);
          CreateExperimentRun.Response createExperimentRunResponse =
              experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
          experimentRun1 = createExperimentRunResponse.getExperimentRun();
          LOGGER.info("\n ExperimentRun1 created successfully \n");
          createExperimentRunRequest =
              experimentRunTest.getCreateExperimentRunRequest(
                  project.getId(), experiment1.getId(), "ExperiemntRun2_" + count);
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
          LOGGER.info("\n ExperimentRun2 created successfully \n");

          // For ExperiemntRun of Experiment2
          createExperimentRunRequest =
              experimentRunTest.getCreateExperimentRunRequest(
                  project.getId(), experiment2.getId(), "ExperiemntRun3_" + count);
          createExperimentRunResponse =
              experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
          experimentRun3 = createExperimentRunResponse.getExperimentRun();
          LOGGER.info("\n ExperimentRun3 created successfully \n");
          createExperimentRunRequest =
              experimentRunTest.getCreateExperimentRunRequest(
                  project.getId(), experiment2.getId(), "ExperimentRun4_" + count);
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
          LOGGER.info("\n ExperimentRun4 created successfully \n");

          // Create comment for above experimentRun1 & experimentRun3
          // comment for experiment1
          AddComment addCommentRequest =
              AddComment.newBuilder()
                  .setEntityId(experimentRun1.getId())
                  .setMessage(
                      "Hello, this project is intreasting."
                          + Calendar.getInstance().getTimeInMillis())
                  .build();
          commentServiceBlockingStub.addExperimentRunComment(addCommentRequest);
          LOGGER.info("\n Comment added successfully for ExperimentRun1 \n");
          // comment for experimentRun3
          addCommentRequest =
              AddComment.newBuilder()
                  .setEntityId(experimentRun3.getId())
                  .setMessage(
                      "Hello, this project is intreasting."
                          + Calendar.getInstance().getTimeInMillis())
                  .build();
          commentServiceBlockingStub.addExperimentRunComment(addCommentRequest);
          LOGGER.info("\n Comment added successfully for ExperimentRun3 \n");

          // Create two collaborator for above project
          // For Collaborator1
          if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
            AddCollaboratorRequest addCollaboratorRequest =
                CollaboratorTest.addCollaboratorRequestProjectInterceptor(
                    project, CollaboratorType.READ_WRITE, authClientInterceptor);
            collaboratorServiceStubClient1.addOrUpdateProjectCollaborator(addCollaboratorRequest);
            LOGGER.info("\n Collaborator1 added successfully \n");
          }
        }
      } finally {
        // Delete all data related to project
        DeleteProjects deleteProjects = DeleteProjects.newBuilder().addAllIds(projectIds).build();
        DeleteProjects.Response deleteProjectsResponse =
            projectServiceStub.deleteProjects(deleteProjects);
        LOGGER.info("Project deleted successfully");
        LOGGER.info(deleteProjectsResponse.toString());
        assertTrue(deleteProjectsResponse.getStatus());
      }

      // Delete entities by cron job
      deleteEntitiesCron.run();
      deleteEntitiesCron.run();
      deleteEntitiesCron.run();

      for (String projectId : projectIds) {
        // Start cross-checking of deleted the project all data from DB from here.
        try {
          GetProjectById getProject = GetProjectById.newBuilder().setId(projectId).build();
          projectServiceStub.getProjectById(getProject);
          fail();
        } catch (StatusRuntimeException e) {
          Status status = Status.fromThrowable(e);
          LOGGER.info("Error Code : " + status.getCode() + " Error : " + status.getDescription());
          checkEqualsAssert(e);
        }

        // Start cross-checking for experiment
        try {
          GetExperimentsInProject getExperiment =
              GetExperimentsInProject.newBuilder().setProjectId(project.getId()).build();
          experimentServiceStub.getExperimentsInProject(getExperiment);
          if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
            fail();
          }
        } catch (StatusRuntimeException ex) {
          checkEqualsAssert(ex);
        }

        // Start cross-checking for experimentRun
        try {
          GetExperimentRunsInProject getExperimentRuns =
              GetExperimentRunsInProject.newBuilder().setProjectId(project.getId()).build();
          experimentRunServiceStub.getExperimentRunsInProject(getExperimentRuns);
          if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
            fail();
          }
        } catch (StatusRuntimeException e) {
          checkEqualsAssert(e);
        }

        // Start cross-checking for comment of experimentRun
        // For experimentRun1
        getCommentsRequest = GetComments.newBuilder().setEntityId(experimentRun1.getId()).build();
        try {
          commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
          if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
            fail();
          }
        } catch (StatusRuntimeException e) {
          checkEqualsAssert(e);
        }

        // For experimentRun3
        getCommentsRequest = GetComments.newBuilder().setEntityId(experimentRun3.getId()).build();
        try {
          commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
          if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
            fail();
          }
        } catch (StatusRuntimeException e) {
          checkEqualsAssert(e);
        }

        // Start cross-checking for project collaborator
        if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
          GetCollaborator getCollaboratorRequest =
              GetCollaborator.newBuilder().setEntityId(project.getId()).build();
          try {
            GetCollaborator.Response getCollaboratorResponse =
                collaboratorServiceStubClient1.getProjectCollaborators(getCollaboratorRequest);
            assertTrue(getCollaboratorResponse.getSharedUsersList().isEmpty());
            fail();
          } catch (StatusRuntimeException e) {
            checkEqualsAssert(e);
          }
        }
      }

    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.info("Error Code : " + status.getCode() + " Error : " + status.getDescription());
      fail();
    }

    LOGGER.info("Project delete with cascading test stop................................");
  }

  @Test
  public void getProjectsByPagination() {
    LOGGER.info("Get Project by pagination test start................................");

    GetProjects getProjects = GetProjects.newBuilder().build();
    GetProjects.Response response = projectServiceStub.getProjects(getProjects);
    long alreadyExistsProjCount = response.getTotalRecords();

    Map<String, Project> projectsMap = new HashMap<>();
    // Create project1
    CreateProject createProjectRequest = getCreateProjectRequest("project-" + new Date().getTime());
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
    createProjectRequest = getCreateProjectRequest("project-" + new Date().getTime());
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    projectsMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project2.getName());

    // Create project3
    createProjectRequest = getCreateProjectRequest("project-" + new Date().getTime());
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project3 = createProjectResponse.getProject();
    projectsMap.put(project3.getId(), project3);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project3.getName());

    getProjects = GetProjects.newBuilder().build();
    response = projectServiceStub.getProjects(getProjects);
    List<Project> responseList = new ArrayList<>();
    for (Project project : response.getProjectsList()) {
      if (projectsMap.containsKey(project.getId())) {
        responseList.add(project);
      }
    }
    LOGGER.info("GetProjects Count : " + responseList.size());
    assertEquals(
        "Projects count not match with expected projects count",
        projectsMap.size(),
        responseList.size());
    assertEquals(
        "Projects count not match with expected projects count",
        projectsMap.size(),
        response.getTotalRecords() - alreadyExistsProjCount);

    int pageLimit = 1;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      getProjects =
          GetProjects.newBuilder()
              .setPageNumber(pageNumber)
              .setPageLimit(pageLimit)
              .setAscending(false)
              .setSortKey(ModelDBConstants.NAME)
              .build();

      GetProjects.Response projectResponse = projectServiceStub.getProjects(getProjects);
      responseList = new ArrayList<>();
      for (Project project : projectResponse.getProjectsList()) {
        if (projectsMap.containsKey(project.getId())) {
          responseList.add(project);
        }
      }
      if (responseList.size() == 0) {
        continue;
      }

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          projectResponse.getTotalRecords() - alreadyExistsProjCount);

      if (responseList.size() > 0) {
        isExpectedResultFound = true;
        LOGGER.info("GetProjects Response : " + (responseList.size() - alreadyExistsProjCount));
        for (Project project : responseList) {
          assertEquals(
              "Project not match with expected Project",
              projectsMap.get(project.getId()).getName(),
              project.getName());
        }

        if (pageNumber == 1) {
          assertEquals(
              "Project not match with expected Project",
              projectsMap.get(responseList.get(0).getId()),
              project3);
        } else if (pageNumber == 3) {
          assertEquals(
              "Project not match with expected Project",
              projectsMap.get(responseList.get(0).getId()),
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

    LOGGER.info("Get project by pagination test stop................................");
  }

  @Test
  public void logProjectCodeVersionTest() {
    LOGGER.info("Log Project code version test start................................");

    Project project = null;
    try {
      // Create project
      CreateProject createProjectRequest =
          getCreateProjectRequest("project-" + new Date().getTime());
      CreateProject.Response createProjectResponse =
          projectServiceStub.createProject(createProjectRequest);
      project = createProjectResponse.getProject();
      LOGGER.info("Project created successfully");
      assertEquals(
          "Project name not match with expected project name",
          createProjectRequest.getName(),
          project.getName());

      LogProjectCodeVersion logProjectCodeVersionRequest =
          LogProjectCodeVersion.newBuilder()
              .setId(project.getId())
              .setCodeVersion(
                  CodeVersion.newBuilder()
                      .setCodeArchive(
                          Artifact.newBuilder()
                              .setKey("code_version_image")
                              .setPath("https://xyz_path_string.com/image.png")
                              .setArtifactType(ArtifactType.CODE)
                              .build())
                      .build())
              .build();
      LogProjectCodeVersion.Response logProjectCodeVersionResponse =
          projectServiceStub.logProjectCodeVersion(logProjectCodeVersionRequest);
      CodeVersion codeVersion = logProjectCodeVersionResponse.getProject().getCodeVersionSnapshot();
      assertEquals(
          "Project codeVersion not match with expected project codeVersion",
          logProjectCodeVersionRequest.getCodeVersion(),
          codeVersion);
      project = logProjectCodeVersionResponse.getProject();

      try {
        logProjectCodeVersionRequest =
            LogProjectCodeVersion.newBuilder()
                .setId(project.getId())
                .setCodeVersion(
                    CodeVersion.newBuilder()
                        .setCodeArchive(
                            Artifact.newBuilder()
                                .setKey("code_version_image")
                                .setPath("https://xyz_path_string.com/image.png")
                                .setArtifactType(ArtifactType.CODE)
                                .build())
                        .build())
                .build();
        logProjectCodeVersionResponse =
            projectServiceStub.logProjectCodeVersion(logProjectCodeVersionRequest);
        fail();
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.warn(
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
      }
    } finally {
      if (project != null) {
        DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
        DeleteProject.Response deleteProjectResponse =
            projectServiceStub.deleteProject(deleteProject);
        LOGGER.info("Project deleted successfully");
        LOGGER.info(deleteProjectResponse.toString());
        assertTrue(deleteProjectResponse.getStatus());
      }
    }

    LOGGER.info("Log Project code version test stop................................");
  }

  @Test
  public void getProjectCodeVersionTest() {
    LOGGER.info("Get Project code version test start................................");
    LogProjectCodeVersion logProjectCodeVersionRequest =
        LogProjectCodeVersion.newBuilder()
            .setId(project.getId())
            .setCodeVersion(
                CodeVersion.newBuilder()
                    .setCodeArchive(
                        Artifact.newBuilder()
                            .setKey("code_version_image")
                            .setPath("https://xyz_path_string.com/image.png")
                            .setArtifactType(ArtifactType.CODE)
                            .build())
                    .build())
            .build();
    LogProjectCodeVersion.Response logProjectCodeVersionResponse =
        projectServiceStub.logProjectCodeVersion(logProjectCodeVersionRequest);
    project = logProjectCodeVersionResponse.getProject();
    assertEquals(
        "Project codeVersion not match with expected project codeVersion",
        logProjectCodeVersionRequest.getCodeVersion(),
        project.getCodeVersionSnapshot());
    project = logProjectCodeVersionResponse.getProject();
    projectMap.put(project.getId(), project);

    GetProjectCodeVersion getProjectCodeVersionRequest =
        GetProjectCodeVersion.newBuilder().setId(project.getId()).build();
    GetProjectCodeVersion.Response getProjectCodeVersionResponse =
        projectServiceStub.getProjectCodeVersion(getProjectCodeVersionRequest);
    CodeVersion codeVersion = getProjectCodeVersionResponse.getCodeVersion();
    assertEquals(
        "Project codeVersion not match with expected project codeVersion",
        project.getCodeVersionSnapshot(),
        codeVersion);

    LOGGER.info("Get Project code version test stop................................");
  }

  @Test
  public void logArtifactsTest() {
    LOGGER.info(" Log Artifacts in Project test start................................");

    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact1 =
        Artifact.newBuilder()
            .setKey("Google Pay Artifact " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay artifact")
            .setArtifactType(ArtifactType.MODEL)
            .build();
    artifacts.add(artifact1);
    Artifact artifact2 =
        Artifact.newBuilder()
            .setKey("Google Pay Artifact " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay artifact")
            .setArtifactType(ArtifactType.DATA)
            .build();
    artifacts.add(artifact2);

    LogProjectArtifacts logArtifactRequest =
        LogProjectArtifacts.newBuilder().setId(project.getId()).addAllArtifacts(artifacts).build();

    LogProjectArtifacts.Response response = projectServiceStub.logArtifacts(logArtifactRequest);

    LOGGER.info("LogArtifact Response : \n" + response.getProject());
    assertEquals(
        "Project artifacts not match with expected artifacts",
        (project.getArtifactsCount() + artifacts.size()),
        response.getProject().getArtifactsList().size());

    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        response.getProject().getDateUpdated());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    LOGGER.info("Log Artifacts in Project tags test stop................................");
  }

  @Test
  public void logArtifactsNegativeTest() {
    LOGGER.info(" Log Artifacts in Project Negative test start................................");

    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact1 =
        Artifact.newBuilder()
            .setKey("Google Pay Artifact " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay artifact")
            .setArtifactType(ArtifactType.MODEL)
            .build();
    artifacts.add(artifact1);
    Artifact artifact2 =
        Artifact.newBuilder()
            .setKey("Google Pay Artifact " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay artifact")
            .setArtifactType(ArtifactType.DATA)
            .build();
    artifacts.add(artifact2);

    LogProjectArtifacts logArtifactRequest =
        LogProjectArtifacts.newBuilder().addAllArtifacts(artifacts).build();
    try {
      projectServiceStub.logArtifacts(logArtifactRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logArtifactRequest =
        LogProjectArtifacts.newBuilder().setId("asda").addAllArtifacts(artifacts).build();
    try {
      projectServiceStub.logArtifacts(logArtifactRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      checkEqualsAssert(ex);
    }

    logArtifactRequest =
        LogProjectArtifacts.newBuilder()
            .setId(project.getId())
            .addAllArtifacts(project.getArtifactsList())
            .build();
    try {
      projectServiceStub.logArtifacts(logArtifactRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    LOGGER.info("Log Artifacts in Project tags Negative test stop................................");
  }

  @Test
  public void getArtifactsTest() {
    LOGGER.info("Get Artifacts from Project test start................................");

    GetArtifacts getArtifactsRequest = GetArtifacts.newBuilder().setId(project.getId()).build();

    GetArtifacts.Response response = projectServiceStub.getArtifacts(getArtifactsRequest);

    LOGGER.info("GetArtifacts Response : " + response.getArtifactsCount());
    assertEquals(
        "Project artifacts not matched with expected artifacts",
        project.getArtifactsList(),
        response.getArtifactsList());

    LOGGER.info("Get Artifacts from Project tags test stop................................");
  }

  @Test
  public void n_getArtifactsNegativeTest() {
    LOGGER.info("Get Artifacts from Project Negative test start................................");

    GetArtifacts getArtifactsRequest = GetArtifacts.newBuilder().build();

    try {
      projectServiceStub.getArtifacts(getArtifactsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getArtifactsRequest = GetArtifacts.newBuilder().setId("dssaa").build();

    try {
      projectServiceStub.getArtifacts(getArtifactsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      checkEqualsAssert(ex);
    }

    LOGGER.info(
        "Get Artifacts from Project Negative tags test stop................................");
  }

  @Test
  public void deleteProjectArtifacts() {
    LOGGER.info("Delete Project Artifacts test start................................");

    List<Artifact> artifacts = project.getArtifactsList();
    LOGGER.info("Artifacts size : " + artifacts.size());
    if (artifacts.isEmpty()) {
      fail("Artifacts not found");
    }

    DeleteProjectArtifact request =
        DeleteProjectArtifact.newBuilder()
            .setId(project.getId())
            .setKey(artifacts.get(0).getKey())
            .build();

    DeleteProjectArtifact.Response response = projectServiceStub.deleteArtifact(request);
    LOGGER.info("DeleteProjectArtifacts Response : \n" + response.getProject().getArtifactsList());
    assertFalse(response.getProject().getArtifactsList().contains(artifacts.get(0)));

    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        response.getProject().getDateUpdated());
    project = response.getProject();
    projectMap.put(project.getId(), project);

    LOGGER.info("Delete Project Artifacts test stop................................");
  }

  @Test
  public void createProjectWithGlobalSharingOrganization() {
    LOGGER.info("Global organization Project test start................................");

    if (app.getAuthServerHost() == null || app.getAuthServerPort() == null) {
      Assert.assertTrue(true);
      return;
    }

    String orgName = "Org-test-verta";
    SetOrganization setOrganization =
        SetOrganization.newBuilder()
            .setOrganization(
                Organization.newBuilder()
                    .setName(orgName)
                    .setDescription("This is the verta test organization")
                    .build())
            .build();
    SetOrganization.Response orgResponse =
        organizationServiceBlockingStub.setOrganization(setOrganization);
    Organization organization = orgResponse.getOrganization();
    assertEquals(
        "Organization name not matched with expected organization name",
        orgName,
        organization.getName());

    String orgRoleName = "O_" + organization.getId() + "_GLOBAL_SHARING";
    GetRoleByName getRoleByName =
        GetRoleByName.newBuilder()
            .setName(orgRoleName)
            .setScope(RoleScope.newBuilder().setOrgId(organization.getId()).build())
            .build();
    GetRoleByName.Response getRoleByNameResponse =
        roleServiceBlockingStub.getRoleByName(getRoleByName);
    assertEquals(
        "Expected role name not found in DB",
        orgRoleName,
        getRoleByNameResponse.getRole().getName());

    Project orgProject = null;
    try {
      // Create project
      CreateProject createProjectRequest =
          getCreateProjectRequest("project-" + new Date().getTime());
      createProjectRequest =
          createProjectRequest
              .toBuilder()
              .setWorkspaceName(organization.getName())
              .setProjectVisibility(ProjectVisibility.ORG_SCOPED_PUBLIC)
              .build();
      CreateProject.Response createProjectResponse =
          projectServiceStub.createProject(createProjectRequest);
      orgProject = createProjectResponse.getProject();
      LOGGER.info("Project created successfully");
    } finally {
      if (orgProject != null) {
        DeleteProject deleteProject = DeleteProject.newBuilder().setId(orgProject.getId()).build();
        DeleteProject.Response deleteProjectResponse =
            projectServiceStub.deleteProject(deleteProject);
        LOGGER.info("Project deleted successfully");
        LOGGER.info(deleteProjectResponse.toString());
        assertTrue(deleteProjectResponse.getStatus());
      }
    }

    DeleteOrganization.Response deleteOrganization =
        organizationServiceBlockingStub.deleteOrganization(
            DeleteOrganization.newBuilder().setOrgId(organization.getId()).build());
    assertTrue(deleteOrganization.getStatus());

    LOGGER.info("Global organization Project test stop................................");
  }
}
