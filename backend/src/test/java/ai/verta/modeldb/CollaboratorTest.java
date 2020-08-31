package ai.verta.modeldb;

import static org.junit.Assert.*;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.CollaboratorTypeEnum.CollaboratorType;
import ai.verta.common.EntitiesEnum.EntitiesTypes;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.cron_jobs.CronJobUtils;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.CollaboratorServiceGrpc.CollaboratorServiceBlockingStub;
import ai.verta.uac.GetCollaborator;
import ai.verta.uac.GetCollaboratorResponse;
import ai.verta.uac.GetUser;
import ai.verta.uac.RemoveCollaborator;
import ai.verta.uac.UACServiceGrpc;
import ai.verta.uac.UserInfo;
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
public class CollaboratorTest {

  private static final Logger LOGGER = LogManager.getLogger(CollaboratorTest.class);
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
  private static AuthService authService;
  private static DeleteEntitiesCron deleteEntitiesCron;

  @SuppressWarnings("unchecked")
  @BeforeClass
  public static void setServerAndService() throws Exception {

    Map<String, Object> propertiesMap =
        ModelDBUtils.readYamlProperties(System.getenv(ModelDBConstants.VERTA_MODELDB_CONFIG));
    Map<String, Object> testPropMap = (Map<String, Object>) propertiesMap.get("test");
    Map<String, Object> databasePropMap = (Map<String, Object>) testPropMap.get("test-database");

    App app = App.getInstance();
    authService = new PublicAuthServiceUtils();
    RoleService roleService = new PublicRoleServiceUtils(authService);

    Map<String, Object> authServicePropMap =
        (Map<String, Object>) propertiesMap.get(ModelDBConstants.AUTH_SERVICE);
    if (authServicePropMap != null) {
      String authServiceHost = (String) authServicePropMap.get(ModelDBConstants.HOST);
      Integer authServicePort = (Integer) authServicePropMap.get(ModelDBConstants.PORT);
      app.setAuthServerHost(authServiceHost);
      app.setAuthServerPort(authServicePort);
      authHost = authServiceHost;
      authPort = authServicePort;

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
    deleteEntitiesCron =
        new DeleteEntitiesCron(authService, roleService, CronJobUtils.deleteEntitiesFrequency);
  }

  @AfterClass
  public static void removeServerAndService() {
    // Delete entities by cron job
    deleteEntitiesCron.run();
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
  public void a_collaboratorCreateTest() {
    LOGGER.info("Create Collaborator test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    ProjectTest projectTest = new ProjectTest();
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

    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestProjectInterceptor(
            project, CollaboratorType.READ_WRITE, authClientInterceptor);

    AddCollaboratorRequest.Response response =
        collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + response.getStatus());
    assertTrue(response.getStatus());

    addCollaboratorRequest =
        addCollaboratorRequestProject(
            project, "github|1234", CollaboratorTypeEnum.CollaboratorType.READ_WRITE);

    try {
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    addCollaboratorRequest =
        addCollaboratorRequestProject(
            project, "google-oauth2|12345678", CollaboratorTypeEnum.CollaboratorType.READ_WRITE);

    try {
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    addCollaboratorRequest =
        addCollaboratorRequestProject(
            project, "bitbucket|12345678", CollaboratorTypeEnum.CollaboratorType.READ_WRITE);

    try {
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Create Collaborator test stop................................");
  }

  @Test
  public void a_collaboratorCreateNegativeTest() {
    LOGGER.info("Create Collaborator Negative test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    ProjectTest projectTest = new ProjectTest();
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

    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestProject(project, "", CollaboratorType.READ_ONLY);
    try {
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      addCollaboratorRequest =
          addCollaboratorRequestProject(project, project.getOwner(), CollaboratorType.READ_WRITE);
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Create Collaborator Negative test stop................................");
  }

  @Test
  public void aa_collaboratorCreateWithEmailTest() {
    LOGGER.info("Create Collaborator with email test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    ProjectTest projectTest = new ProjectTest();
    // Create project
    CreateProject createProjectRequest =
        projectTest.getCreateProjectRequest("experimentRun_project1_ypcdt1");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project.getName());

    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestProject(
            project, authClientInterceptor.getClient2Email(), CollaboratorType.READ_ONLY);

    AddCollaboratorRequest.Response response =
        collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);

    LOGGER.info("Collaborator added in server : " + response.getStatus());
    assertTrue(response.getStatus());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Create Collaborator with email test stop................................");
  }

  @Test
  public void aa_collaboratorCreateWithEmailNegativeTest() {
    LOGGER.info(
        "Create Collaborator with email Negative test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    ProjectTest projectTest = new ProjectTest();
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

    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestProject(project, "", CollaboratorType.READ_ONLY);
    try {
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info(
        "Create Collaborator with email Negative test stop................................");
  }

  @Test
  public void b_collaboratorUpdateTest() {
    LOGGER.info("Update Collaborator test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    ProjectTest projectTest = new ProjectTest();
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

    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestProjectInterceptor(
            project, CollaboratorType.READ_WRITE, authClientInterceptor);

    AddCollaboratorRequest.Response response =
        collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + response.getStatus());
    assertTrue(response.getStatus());

    addCollaboratorRequest =
        addCollaboratorRequestMessage(
            project.getId(),
            authClientInterceptor.getClient2Email(),
            CollaboratorType.READ_ONLY,
            "Now you have "
                + CollaboratorType.READ_ONLY
                + " permission, Please refer shared project for your invention");
    response = collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);

    LOGGER.info("Collaborator updated in server : " + response.getStatus());
    assertTrue(response.getStatus());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Update Collaborator test stop................................");
  }

  @Test
  public void b_collaboratorUpdateNegativeTest() {
    LOGGER.info("Update Collaborator Negative test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    ProjectTest projectTest = new ProjectTest();
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

    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestMessage(
            project.getId(),
            "",
            CollaboratorType.READ_ONLY,
            "Now you have "
                + CollaboratorType.READ_ONLY
                + " permission, Please refer shared project for your invention");
    try {
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Update Collaborator Negative test stop................................");
  }

  @Test
  public void c_GetCollaboratorTest() {
    LOGGER.info("Get Collaborator test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    ProjectTest projectTest = new ProjectTest();
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

    UACServiceGrpc.UACServiceBlockingStub uaServiceStub =
        UACServiceGrpc.newBlockingStub(authServiceChannel);
    GetUser getUserRequest =
        GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo shareWithUserInfo = uaServiceStub.getUser(getUserRequest);

    List<String> sharedUsers = new ArrayList<>();
    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestProject(
            project,
            shareWithUserInfo.getEmail(),
            CollaboratorTypeEnum.CollaboratorType.READ_WRITE);
    sharedUsers.add(authService.getVertaIdFromUserInfo(shareWithUserInfo));

    AddCollaboratorRequest.Response addCollaboratorResponse =
        collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
    assertTrue(addCollaboratorResponse.getStatus());

    /*addCollaboratorRequest =
        AddCollaboratorRequest.newBuilder()
            .addEntityIds(project.getId())
            .setShareWith("github|87654321")
            .setCollaboratorType(CollaboratorType.READ_WRITE)
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .setMessage("Please refer shared project for your invention")
            .build();
    sharedUsers.add("github|87654321");

    addCollaboratorResponse =
        collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
    assertTrue(addCollaboratorResponse.getStatus());*/

    GetCollaborator getCollaboratorRequest =
        GetCollaborator.newBuilder().setEntityId(project.getId()).build();
    GetCollaborator.Response getCollaboratorResponse =
        collaboratorServiceStub.getProjectCollaborators(getCollaboratorRequest);

    List<GetCollaboratorResponse> sharedUserList = getCollaboratorResponse.getSharedUsersList();
    LOGGER.info(
        "Founded collaborator users count : " + getCollaboratorResponse.getSharedUsersCount());
    assertEquals(
        "Collaborator count not match with expected collaborator",
        1,
        getCollaboratorResponse.getSharedUsersCount());
    sharedUserList.forEach(
        sharedUserDetail -> {
          assertTrue(
              "Collaborator user not match with expected collaborator user",
              sharedUsers.contains(sharedUserDetail.getVertaId()));
        });

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get Collaborator test stop................................");
  }

  @Test
  public void c_GetCollaboratorNegativeTest() {
    LOGGER.info("Get Collaborator Negative test start................................");

    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);
    GetCollaborator getCollaboratorRequest = GetCollaborator.newBuilder().build();

    try {
      collaboratorServiceStub.getProjectCollaborators(getCollaboratorRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Get Collaborator Negative test stop................................");
  }

  @Test
  public void z_collaboratorRemoveNegativeTest() {
    LOGGER.info("Remove Collaborator Negative test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    ProjectTest projectTest = new ProjectTest();
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

    RemoveCollaborator removeProjectCollaborator =
        RemoveCollaborator.newBuilder()
            .setEntityId(project.getId())
            .setAuthzEntityType(EntitiesTypes.USER)
            .setDateDeleted(Calendar.getInstance().getTimeInMillis())
            .build();
    try {
      collaboratorServiceStub.removeProjectCollaborator(removeProjectCollaborator);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    removeProjectCollaborator =
        RemoveCollaborator.newBuilder()
            .setAuthzEntityType(EntitiesTypes.USER)
            .setEntityId(project.getId())
            .setShareWith("2019")
            .setDateDeleted(Calendar.getInstance().getTimeInMillis())
            .build();
    try {
      collaboratorServiceStub.removeProjectCollaborator(removeProjectCollaborator);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Remove Collaborator Negative test stop................................");
  }

  @Test
  public void z_collaboratorRemoveTest() {
    LOGGER.info("Remove Collaborator test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    ProjectTest projectTest = new ProjectTest();
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

    UACServiceGrpc.UACServiceBlockingStub uaServiceStub =
        UACServiceGrpc.newBlockingStub(authServiceChannel);
    GetUser getUserRequest =
        GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo shareWithUserInfo = uaServiceStub.getUser(getUserRequest);

    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestProject(
            project,
            shareWithUserInfo.getEmail(),
            CollaboratorTypeEnum.CollaboratorType.READ_WRITE);

    AddCollaboratorRequest.Response addCollaboratorResponse =
        collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
    assertTrue(addCollaboratorResponse.getStatus());

    RemoveCollaborator removeProjectCollaborator =
        RemoveCollaborator.newBuilder()
            .setAuthzEntityType(EntitiesTypes.USER)
            .setEntityId(project.getId())
            .setShareWith(authService.getVertaIdFromUserInfo(shareWithUserInfo))
            .setDateDeleted(Calendar.getInstance().getTimeInMillis())
            .build();

    RemoveCollaborator.Response response =
        collaboratorServiceStub.removeProjectCollaborator(removeProjectCollaborator);

    LOGGER.info("Collaborator remove in server : " + response.getStatus());
    assertTrue(response.getStatus());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Remove Collaborator test stop................................");
  }

  @Test
  public void datasetCollaboratorCreateTest() {
    LOGGER.info("Create Dataset Collaborator test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    CreateDataset createDatasetRequest =
        CreateDataset.newBuilder()
            .setName("rental_TEXT_train_data.csv")
            .setDatasetVisibility(DatasetVisibilityEnum.DatasetVisibility.PUBLIC)
            .build();
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "DatasetInfo name not match with expected datasetInfo name",
        createDatasetRequest.getName(),
        createDatasetResponse.getDataset().getName());

    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestDatasetInterceptor(
            createDatasetResponse.getDataset(), CollaboratorType.READ_WRITE);

    AddCollaboratorRequest.Response response =
        collaboratorServiceStub.addOrUpdateRepositoryCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + response.getStatus());
    assertTrue(response.getStatus());

    DeleteDataset deleteDataset =
        DeleteDataset.newBuilder().setId(createDatasetResponse.getDataset().getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Create Dataset Collaborator test stop................................");
  }

  @Test
  public void getDatasetCollaboratorTest() {
    LOGGER.info("Get Dataset Collaborator test start................................");

    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        CreateDataset.newBuilder()
            .setName("rental_TEXT_train_data.csv")
            .setDatasetVisibility(DatasetVisibilityEnum.DatasetVisibility.PUBLIC)
            .build();
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "DatasetInfo name not match with expected datasetInfo name",
        createDatasetRequest.getName(),
        createDatasetResponse.getDataset().getName());
    Dataset dataset = createDatasetResponse.getDataset();

    UACServiceGrpc.UACServiceBlockingStub uaServiceStub =
        UACServiceGrpc.newBlockingStub(authServiceChannel);
    GetUser getUserRequest =
        GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo shareWithUserInfo = uaServiceStub.getUser(getUserRequest);

    List<String> sharedUsers = new ArrayList<>();
    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestUser(
            dataset.getId(),
            shareWithUserInfo.getEmail(),
            CollaboratorType.READ_WRITE,
            "Please refer shared project for your invention");
    sharedUsers.add(authService.getVertaIdFromUserInfo(shareWithUserInfo));

    AddCollaboratorRequest.Response addCollaboratorResponse =
        collaboratorServiceStub.addOrUpdateRepositoryCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
    assertTrue(addCollaboratorResponse.getStatus());

    /*addCollaboratorRequest =
        AddCollaboratorRequest.newBuilder()
            .addEntityIds(dataset.getId())
            .setShareWith("github|87654321")
            .setCollaboratorType(CollaboratorType.READ_WRITE)
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .setMessage("Please refer shared dataset for your invention")
            .build();
    sharedUsers.add("github|87654321");

    addCollaboratorResponse =
        collaboratorServiceStub.addOrUpdateRepositoryCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
    assertTrue(addCollaboratorResponse.getStatus());*/

    GetCollaborator getCollaboratorRequest =
        GetCollaborator.newBuilder().setEntityId(dataset.getId()).build();
    GetCollaborator.Response getCollaboratorResponse =
        collaboratorServiceStub.getRepositoryCollaborators(getCollaboratorRequest);

    List<GetCollaboratorResponse> sharedUserList = getCollaboratorResponse.getSharedUsersList();
    LOGGER.info(
        "Found collaborator users count : " + getCollaboratorResponse.getSharedUsersCount());
    assertEquals(
        "Collaborator count not match with expected collaborator",
        1,
        getCollaboratorResponse.getSharedUsersCount());
    sharedUserList.forEach(
        sharedUserDetail -> {
          assertTrue(
              "Collaborator user not match with expected collaborator user",
              sharedUsers.contains(sharedUserDetail.getVertaId()));
        });

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Get Dataset Collaborator test stop................................");
  }

  @Test
  public void datasetCollaboratorRemoveTest() {
    LOGGER.info("Remove Dataset Collaborator test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    CreateDataset createDatasetRequest =
        CreateDataset.newBuilder()
            .setName("rental_TEXT_train_data.csv")
            .setDatasetVisibility(DatasetVisibilityEnum.DatasetVisibility.PUBLIC)
            .build();
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "DatasetInfo name not match with expected datasetInfo name",
        createDatasetRequest.getName(),
        dataset.getName());

    UACServiceGrpc.UACServiceBlockingStub uaServiceStub =
        UACServiceGrpc.newBlockingStub(authServiceChannel);
    GetUser getUserRequest =
        GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo shareWithUserInfo = uaServiceStub.getUser(getUserRequest);

    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestDataset(
            dataset, shareWithUserInfo.getEmail(), CollaboratorType.READ_WRITE);

    AddCollaboratorRequest.Response addCollaboratorResponse =
        collaboratorServiceStub.addOrUpdateRepositoryCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
    assertTrue(addCollaboratorResponse.getStatus());

    RemoveCollaborator removeRepositoryCollaborator =
        RemoveCollaborator.newBuilder()
            .setAuthzEntityType(EntitiesTypes.USER)
            .setEntityId(dataset.getId())
            .setShareWith(authService.getVertaIdFromUserInfo(shareWithUserInfo))
            .setDateDeleted(Calendar.getInstance().getTimeInMillis())
            .build();

    RemoveCollaborator.Response response =
        collaboratorServiceStub.removeRepositoryCollaborator(removeRepositoryCollaborator);

    LOGGER.info("Collaborator remove in server : " + response.getStatus());
    assertTrue(response.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Remove Dataset Collaborator test stop................................");
  }

  @Test
  public void datasetCollaboratorBatchCreateTest() {
    LOGGER.info("Batch Create Dataset Collaborator test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    List<String> datasetIds = new ArrayList<>();
    for (int index = 0; index < 5; index++) {
      CreateDataset createDatasetRequest =
          CreateDataset.newBuilder()
              .setName("rental_TEXT_train_data" + index + ".csv")
              .setDatasetVisibility(DatasetVisibilityEnum.DatasetVisibility.PUBLIC)
              .build();
      CreateDataset.Response createDatasetResponse =
          datasetServiceStub.createDataset(createDatasetRequest);
      LOGGER.info("Dataset created successfully");
      assertEquals(
          "DatasetInfo name not match with expected datasetInfo name",
          createDatasetRequest.getName(),
          createDatasetResponse.getDataset().getName());
      datasetIds.add(createDatasetResponse.getDataset().getId());
    }

    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestProjectInterceptor(
            datasetIds, CollaboratorType.READ_WRITE, authClientInterceptor);

    AddCollaboratorRequest.Response response =
        collaboratorServiceStub.addOrUpdateRepositoryCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + response.getStatus());
    assertTrue(response.getStatus());

    addCollaboratorRequest =
        addCollaboratorRequestProjectInterceptor(
            datasetIds.subList(1, 4), CollaboratorType.READ_ONLY, authClientInterceptor);

    response = collaboratorServiceStub.addOrUpdateRepositoryCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + response.getStatus());
    assertTrue(response.getStatus());

    for (String datasetId : datasetIds) {
      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(datasetId).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());
    }

    LOGGER.info("Batch Dataset Collaborator test stop................................");
  }

  @Test
  public void projectCollaboratorBatchCreateTest() {
    LOGGER.info("Batch Create Collaborator test start................................");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    ProjectTest projectTest = new ProjectTest();

    List<String> projectIds = new ArrayList<>();
    List<Project> projects = new ArrayList<>();
    for (int index = 0; index < 5; index++) {
      // Create project
      CreateProject createProjectRequest =
          projectTest.getCreateProjectRequest("project_ypcdt_" + index);
      CreateProject.Response createProjectResponse =
          projectServiceStub.createProject(createProjectRequest);
      Project project = createProjectResponse.getProject();
      projects.add(project);
      LOGGER.info("Project created successfully");
      assertEquals(
          "Project name not match with expected project name",
          createProjectRequest.getName(),
          project.getName());
      projectIds.add(project.getId());
    }

    AddCollaboratorRequest addCollaboratorRequest =
        addCollaboratorRequestProjectInterceptor(
            projectIds, CollaboratorTypeEnum.CollaboratorType.READ_WRITE, authClientInterceptor);

    AddCollaboratorRequest.Response response =
        collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + response.getStatus());
    assertTrue(response.getStatus());

    addCollaboratorRequest =
        addCollaboratorRequestProjectInterceptor(
            projectIds.subList(1, 4), CollaboratorType.READ_ONLY, authClientInterceptor);

    response = collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
    LOGGER.info("Collaborator added in server : " + response.getStatus());
    assertTrue(response.getStatus());

    addCollaboratorRequest =
        addCollaboratorRequest(
            projectIds.subList(1, 4), projects.get(2).getOwner(), CollaboratorType.READ_ONLY);

    try {
      collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    for (String projectId : projectIds) {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(projectId).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }

    LOGGER.info("Batch Create Collaborator test stop................................");
  }

  private AddCollaboratorRequest addCollaboratorRequestDatasetInterceptor(
      Dataset dataset, CollaboratorType collaboratorType) {
    return addCollaboratorRequestDataset(
        dataset, authClientInterceptor.getClient2Email(), collaboratorType);
  }

  public static AddCollaboratorRequest addCollaboratorRequestDataset(
      Dataset dataset, String email, CollaboratorType collaboratorType) {
    return addCollaboratorRequestUser(
        dataset.getId(), email, collaboratorType, "Please refer shared dataset for your invention");
  }

  public static AddCollaboratorRequest addCollaboratorRequestProjectInterceptor(
      Project project,
      CollaboratorType collaboratorType,
      AuthClientInterceptor authClientInterceptor0) {
    return addCollaboratorRequestProject(
        project, authClientInterceptor0.getClient2Email(), collaboratorType);
  }

  public static AddCollaboratorRequest addCollaboratorRequestProjectInterceptor(
      List<String> ids,
      CollaboratorType collaboratorType,
      AuthClientInterceptor authClientInterceptor0) {
    return addCollaboratorRequest(ids, authClientInterceptor0.getClient2Email(), collaboratorType);
  }

  private AddCollaboratorRequest addCollaboratorRequestMessage(
      String id, String email, CollaboratorType collaboratorType, String message) {
    return addCollaboratorRequestUser(id, email, collaboratorType, message);
  }

  public static AddCollaboratorRequest addCollaboratorRequest(
      List<String> ids, String shareWith, CollaboratorType collaboratorType) {
    return addCollaboratorRequestUser(
        ids, shareWith, collaboratorType, "Please refer shared project for your invention");
  }

  static AddCollaboratorRequest addCollaboratorRequestProject(
      Project project, String email, CollaboratorType collaboratorType) {
    return addCollaboratorRequest(
        Collections.singletonList(project.getId()), email, collaboratorType);
  }

  static AddCollaboratorRequest addCollaboratorRequestUser(
      String entityId, String email, CollaboratorType collaboratorType, String message) {
    return addCollaboratorRequestUser(
        Collections.singletonList(entityId), email, collaboratorType, message);
  }

  static AddCollaboratorRequest addCollaboratorRequestUser(
      List<String> ids, String email, CollaboratorType collaboratorType, String message) {
    return AddCollaboratorRequest.newBuilder()
        .addAllEntityIds(ids)
        .setShareWith(email)
        .setAuthzEntityType(EntitiesTypes.USER)
        .setCollaboratorType(collaboratorType)
        .setDateCreated(Calendar.getInstance().getTimeInMillis())
        .setMessage(message)
        .build();
  }
}
