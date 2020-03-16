package ai.verta.modeldb;

import static ai.verta.modeldb.CollaboratorTest.addCollaboratorRequestProjectInterceptor;
import static org.junit.Assert.*;

import ai.verta.common.TernaryEnum.Ternary;
import ai.verta.modeldb.ArtifactTypeEnum.ArtifactType;
import ai.verta.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub;
import ai.verta.modeldb.ExperimentServiceGrpc.ExperimentServiceBlockingStub;
import ai.verta.modeldb.OperatorEnum.Operator;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.ValueTypeEnum.ValueType;
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
import ai.verta.uac.CollaboratorTypeEnum.CollaboratorType;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
public class ExperimentRunTest {

  private static final Logger LOGGER = LogManager.getLogger(ExperimentRunTest.class);
  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;
  private ManagedChannel client2Channel = null;
  private static String serverName = InProcessServerBuilder.generateName();
  private static InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder client1ChannelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder client2ChannelBuilder =
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
      client1ChannelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
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
  }

  @Before
  public void initializeChannel() throws IOException {
    grpcCleanup.register(serverBuilder.build().start());
    channel = grpcCleanup.register(client1ChannelBuilder.maxInboundMessageSize(1024).build());
    client2Channel =
        grpcCleanup.register(client2ChannelBuilder.maxInboundMessageSize(1024).build());
  }

  private void checkEqualsAssert(StatusRuntimeException e) {
    Status status = Status.fromThrowable(e);
    LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    } else {
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }
  }

  public CreateExperimentRun getCreateExperimentRunRequestSimple(
      String projectId, String experimentId, String experimentRunName) {
    return CreateExperimentRun.newBuilder()
        .setProjectId(projectId)
        .setExperimentId(experimentId)
        .setName(experimentRunName)
        .build();
  }

  public CreateExperimentRun getCreateExperimentRunRequest(
      String projectId, String experimentId, String experimentRunName) {

    List<String> tags = new ArrayList<>();
    tags.add("Tag_x");
    tags.add("Tag_y");

    int rangeMax = 20;
    int rangeMin = 1;
    Random randomNum = new Random();

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
            .setValueType(ValueType.STRING)
            .build());

    double randomValue = rangeMin + (rangeMax - rangeMin) * randomNum.nextDouble();
    List<KeyValue> hyperparameters = new ArrayList<>();
    intValue = Value.newBuilder().setNumberValue(randomValue).build();
    hyperparameters.add(
        KeyValue.newBuilder()
            .setKey("tuning_" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build());
    stringValue =
        Value.newBuilder()
            .setStringValue("hyperparameters_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    hyperparameters.add(
        KeyValue.newBuilder()
            .setKey("hyperparameters_" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build());

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

    List<Artifact> datasets = new ArrayList<>();
    datasets.add(
        Artifact.newBuilder()
            .setKey("Google developer datasets")
            .setPath("This is data artifact type in Google developer datasets")
            .setArtifactType(ArtifactType.MODEL)
            .build());
    datasets.add(
        Artifact.newBuilder()
            .setKey("Google Pay datasets")
            .setPath("This is data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactType.DATA)
            .build());

    List<KeyValue> metrics = new ArrayList<>();
    randomValue = rangeMin + (rangeMax - rangeMin) * randomNum.nextDouble();
    intValue = Value.newBuilder().setNumberValue(randomValue).build();
    metrics.add(
        KeyValue.newBuilder()
            .setKey("accuracy_" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build());
    randomValue = rangeMin + (rangeMax - rangeMin) * randomNum.nextDouble();
    intValue = Value.newBuilder().setNumberValue(randomValue).build();
    metrics.add(
        KeyValue.newBuilder()
            .setKey("loss_" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build());
    randomValue = rangeMin + (rangeMax - rangeMin) * randomNum.nextDouble();
    Value listValue =
        Value.newBuilder()
            .setListValue(
                ListValue.newBuilder()
                    .addValues(intValue)
                    .addValues(Value.newBuilder().setNumberValue(randomValue).build()))
            .build();
    metrics.add(
        KeyValue.newBuilder()
            .setKey("profit_" + Calendar.getInstance().getTimeInMillis())
            .setValue(listValue)
            .setValueType(ValueType.LIST)
            .build());

    List<Observation> observations = new ArrayList<>();
    observations.add(
        Observation.newBuilder()
            .setArtifact(
                Artifact.newBuilder()
                    .setKey("Google developer Observation artifact")
                    .setPath("This is data artifact type in Google developer Observation artifact")
                    .setArtifactType(ArtifactType.DATA)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .build());
    stringValue =
        Value.newBuilder()
            .setStringValue("Observation_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    observations.add(
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("Observation Key " + Calendar.getInstance().getTimeInMillis())
                    .setValue(stringValue)
                    .setValueType(ValueType.STRING))
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .build());

    List<Feature> features = new ArrayList<>();
    features.add(Feature.newBuilder().setName("ExperimentRun Test case feature 1").build());
    features.add(Feature.newBuilder().setName("ExperimentRun Test case feature 2").build());

    return CreateExperimentRun.newBuilder()
        .setProjectId(projectId)
        .setExperimentId(experimentId)
        .setName(experimentRunName)
        .setDescription("this is a ExperimentRun description")
        .setDateCreated(Calendar.getInstance().getTimeInMillis())
        .setDateUpdated(Calendar.getInstance().getTimeInMillis())
        .setStartTime(Calendar.getInstance().getTime().getTime())
        .setEndTime(Calendar.getInstance().getTime().getTime())
        .setCodeVersion("1.0")
        .addAllTags(tags)
        .addAllAttributes(attributeList)
        .addAllHyperparameters(hyperparameters)
        .addAllArtifacts(artifactList)
        .addAllDatasets(datasets)
        .addAllMetrics(metrics)
        .addAllObservations(observations)
        .addAllFeatures(features)
        .build();
  }

  @Test
  public void a_experimentRunCreateTest() {
    LOGGER.info("Create ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());
    experiment = getExperimentByIdResponse.getExperiment();

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    try {
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    try {
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder().setProjectId("xyz").build();
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      fail();
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    try {
      createExperimentRunRequest =
          createExperimentRunRequest
              .toBuilder()
              .setProjectId(project.getId())
              .setExperimentId("xyz")
              .build();
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    try {
      String tag52 = "Human Activity Recognition using Smartphone Dataset";
      createExperimentRunRequest =
          createExperimentRunRequest
              .toBuilder()
              .setProjectId(project.getId())
              .setExperimentId(experiment.getId())
              .addTags(tag52)
              .build();
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder().clearTags().addTags("").build();
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      String name =
          "Experiment of Human Activity Recognition using Smartphone Dataset Human Activity Recognition using Smartphone Dataset Human Activity Recognition using Smartphone Dataset Human Activity Recognition using Smartphone Dataset Human Activity Recognition using Smartphone Dataset";
      createExperimentRunRequest = createExperimentRunRequest.toBuilder().setName(name).build();
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
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

    LOGGER.info("Create ExperimentRun test stop................................");
  }

  @Test
  public void a_experimentRunCreateNegativeTest() {
    LOGGER.info("Create ExperimentRun Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    CreateExperimentRun request = getCreateExperimentRunRequest("", "abcd", "ExperiemntRun_xyz");

    try {
      experimentRunServiceStub.createExperimentRun(request);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.info("CreateExperimentRun Response : \n" + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Create ExperimentRun Negative test stop................................");
  }

  @Test
  public void b_getExperimentRunFromProjectRunTest() {
    LOGGER.info("Get ExperimentRun from Project test start................................");
    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    Map<String, ExperimentRun> experimentRunMap = new HashMap<>();
    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_sprt_1");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_sprt_2");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentRunsInProject getExperimentRunRequest =
        GetExperimentRunsInProject.newBuilder().setProjectId(project.getId()).build();

    GetExperimentRunsInProject.Response experimentRunResponse =
        experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunRequest);
    assertEquals(
        "ExperimentRuns count not match with expected experimentRun count",
        experimentRunMap.size(),
        experimentRunResponse.getExperimentRunsList().size());

    if (experimentRunResponse.getExperimentRunsList() != null) {
      for (ExperimentRun experimentRun1 : experimentRunResponse.getExperimentRunsList()) {
        assertEquals(
            "ExperimentRun not match with expected experimentRun",
            experimentRunMap.get(experimentRun1.getId()),
            experimentRun1);
      }
    } else {
      LOGGER.warn("More ExperimentRun not found in database");
      assertTrue(true);
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get ExperimentRun from Project test stop................................");
  }

  @Test
  public void b_getExperimentRunWithPaginationFromProjectRunTest() {
    LOGGER.info(
        "Get ExperimentRun using pagination from Project test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    Map<String, ExperimentRun> experimentRunMap = new HashMap<>();
    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_sprt_1");
    KeyValue metric1 =
        KeyValue.newBuilder()
            .setKey("loss")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    KeyValue metric2 =
        KeyValue.newBuilder()
            .setKey("accuracy")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    KeyValue hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("tuning")
            .setValue(Value.newBuilder().setNumberValue(7).build())
            .build();
    createExperimentRunRequest =
        createExperimentRunRequest
            .toBuilder()
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .setDateCreated(123456)
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun1 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun1.getId(), experimentRun1);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun1.getName());

    createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_sprt_2");
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
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun2 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun2.getId(), experimentRun2);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun2.getName());

    int pageLimit = 2;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      GetExperimentRunsInProject getExperimentRunRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(project.getId())
              .setPageNumber(pageNumber)
              .setPageLimit(pageLimit)
              .setAscending(true)
              .setSortKey(ModelDBConstants.NAME)
              .build();

      GetExperimentRunsInProject.Response experimentRunResponse =
          experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunRequest);

      assertEquals(
          "Total records count not matched with expected records count",
          2,
          experimentRunResponse.getTotalRecords());

      if (experimentRunResponse.getExperimentRunsList() != null
          && experimentRunResponse.getExperimentRunsList().size() > 0) {
        isExpectedResultFound = true;
        for (ExperimentRun experimentRun : experimentRunResponse.getExperimentRunsList()) {
          assertEquals(
              "ExperimentRun not match with expected experimentRun",
              experimentRunMap.get(experimentRun.getId()),
              experimentRun);
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

    GetExperimentRunsInProject getExperiment =
        GetExperimentRunsInProject.newBuilder()
            .setProjectId(project.getId())
            .setPageNumber(1)
            .setPageLimit(1)
            .setAscending(false)
            .setSortKey("metrics.loss")
            .build();

    GetExperimentRunsInProject.Response experimentRunResponse =
        experimentRunServiceStub.getExperimentRunsInProject(getExperiment);
    assertEquals(
        "Total records count not matched with expected records count",
        2,
        experimentRunResponse.getTotalRecords());
    assertEquals(
        "ExperimentRuns count not match with expected experimentRuns count",
        1,
        experimentRunResponse.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun2,
        experimentRunResponse.getExperimentRuns(0));

    getExperiment =
        GetExperimentRunsInProject.newBuilder()
            .setProjectId(project.getId())
            .setPageNumber(1)
            .setPageLimit(1)
            .setAscending(true)
            .setSortKey("")
            .build();

    experimentRunResponse = experimentRunServiceStub.getExperimentRunsInProject(getExperiment);
    assertEquals(
        "ExperimentRuns count not match with expected experimentRuns count",
        1,
        experimentRunResponse.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun1,
        experimentRunResponse.getExperimentRuns(0));

    getExperiment =
        GetExperimentRunsInProject.newBuilder()
            .setProjectId(project.getId())
            .setPageNumber(1)
            .setPageLimit(1)
            .setAscending(true)
            .setSortKey("observations.attribute.attr_1")
            .build();

    try {
      experimentRunServiceStub.getExperimentRunsInProject(getExperiment);
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
        "Get ExperimentRun using pagination from Project test stop................................");
  }

  @Test
  public void b_getExperimentFromProjectRunNegativeTest() {
    LOGGER.info(
        "Get ExperimentRun from Project Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    GetExperimentRunsInProject getExperiment = GetExperimentRunsInProject.newBuilder().build();
    try {
      experimentRunServiceStub.getExperimentRunsInProject(getExperiment);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getExperiment = GetExperimentRunsInProject.newBuilder().setProjectId("sdfdsfsd").build();
    try {
      experimentRunServiceStub.getExperimentRunsInProject(getExperiment);
      fail();
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    LOGGER.info(
        "Get ExperimentRun from Project Negative test stop................................");
  }

  @Test
  public void bb_getExperimentRunFromExperimentTest() {
    LOGGER.info("Get ExperimentRun from Experiment test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    Map<String, ExperimentRun> experimentRunMap = new HashMap<>();
    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_sprt_1");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_sprt_2");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentRunsInExperiment getExperimentRunsInExperiment =
        GetExperimentRunsInExperiment.newBuilder().setExperimentId(experiment.getId()).build();

    GetExperimentRunsInExperiment.Response experimentRunResponse =
        experimentRunServiceStub.getExperimentRunsInExperiment(getExperimentRunsInExperiment);
    assertEquals(
        "ExperimentRuns count not match with expected experimentRun count",
        experimentRunMap.size(),
        experimentRunResponse.getExperimentRunsList().size());

    if (experimentRunResponse.getExperimentRunsList() != null) {
      for (ExperimentRun experimentRun1 : experimentRunResponse.getExperimentRunsList()) {
        assertEquals(
            "ExperimentRun not match with expected experimentRun",
            experimentRunMap.get(experimentRun1.getId()),
            experimentRun1);
      }
    } else {
      LOGGER.warn("More ExperimentRun not found in database");
      assertTrue(true);
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get ExperimentRun from Experiment test stop................................");
  }

  @Test
  public void bb_getExperimentRunWithPaginationFromExperimentTest() {
    LOGGER.info(
        "Get ExperimentRun using pagination from Experiment test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    Map<String, ExperimentRun> experimentRunMap = new HashMap<>();
    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_sprt_1");
    KeyValue metric1 =
        KeyValue.newBuilder()
            .setKey("loss")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    KeyValue metric2 =
        KeyValue.newBuilder()
            .setKey("accuracy")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    KeyValue hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("tuning")
            .setValue(Value.newBuilder().setNumberValue(7).build())
            .build();
    createExperimentRunRequest =
        createExperimentRunRequest
            .toBuilder()
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .setDateCreated(123456)
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun1 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun1.getId(), experimentRun1);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun1.getName());

    createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_sprt_2");
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
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun2 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun2.getId(), experimentRun2);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun2.getName());

    int pageLimit = 1;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      GetExperimentRunsInExperiment getExperimentRunsInExperiment =
          GetExperimentRunsInExperiment.newBuilder()
              .setExperimentId(experiment.getId())
              .setPageNumber(pageNumber)
              .setPageLimit(pageLimit)
              .setAscending(true)
              .setSortKey(ModelDBConstants.NAME)
              .build();

      GetExperimentRunsInExperiment.Response experimentRunResponse =
          experimentRunServiceStub.getExperimentRunsInExperiment(getExperimentRunsInExperiment);
      assertEquals(
          "Total records count not matched with expected records count",
          2,
          experimentRunResponse.getTotalRecords());

      if (experimentRunResponse.getExperimentRunsList() != null) {
        isExpectedResultFound = true;
        for (ExperimentRun experimentRun : experimentRunResponse.getExperimentRunsList()) {
          assertEquals(
              "ExperimentRun not match with expected experimentRun",
              experimentRunMap.get(experimentRun.getId()),
              experimentRun);
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More ExperimentRun not found in database");
          assertTrue(true);
        } else {
          fail("Expected experimentRun not found in response");
        }
      }
    }

    GetExperimentRunsInExperiment getExperimentRunsInExperiment =
        GetExperimentRunsInExperiment.newBuilder()
            .setExperimentId(experiment.getId())
            .setPageNumber(1)
            .setPageLimit(1)
            .setAscending(false)
            .setSortKey("metrics.loss")
            .build();

    GetExperimentRunsInExperiment.Response experimentRunResponse =
        experimentRunServiceStub.getExperimentRunsInExperiment(getExperimentRunsInExperiment);
    assertEquals(
        "Total records count not matched with expected records count",
        2,
        experimentRunResponse.getTotalRecords());
    assertEquals(
        "ExperimentRuns count not match with expected experimentRuns count",
        1,
        experimentRunResponse.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun2,
        experimentRunResponse.getExperimentRuns(0));

    getExperimentRunsInExperiment =
        GetExperimentRunsInExperiment.newBuilder()
            .setExperimentId(experiment.getId())
            .setPageNumber(1)
            .setPageLimit(1)
            .setAscending(true)
            .setSortKey("")
            .build();

    experimentRunResponse =
        experimentRunServiceStub.getExperimentRunsInExperiment(getExperimentRunsInExperiment);
    assertEquals(
        "ExperimentRuns count not match with expected experimentRuns count",
        1,
        experimentRunResponse.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun1,
        experimentRunResponse.getExperimentRuns(0));
    assertEquals(
        "Total records count not matched with expected records count",
        2,
        experimentRunResponse.getTotalRecords());

    getExperimentRunsInExperiment =
        GetExperimentRunsInExperiment.newBuilder()
            .setExperimentId(experiment.getId())
            .setPageNumber(1)
            .setPageLimit(1)
            .setAscending(true)
            .setSortKey("observations.attribute.attr_1")
            .build();

    try {
      experimentRunServiceStub.getExperimentRunsInExperiment(getExperimentRunsInExperiment);
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
        "Get ExperimentRun using pagination from Experiment test stop................................");
  }

  @Test
  public void bb_getExperimentFromExperimentNegativeTest() {
    LOGGER.info(
        "Get ExperimentRun from Experiment Negative test start................................");

    ExperimentRunServiceBlockingStub experimentServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    GetExperimentRunsInExperiment getExperiment =
        GetExperimentRunsInExperiment.newBuilder().build();
    try {
      experimentServiceStub.getExperimentRunsInExperiment(getExperiment);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getExperiment = GetExperimentRunsInExperiment.newBuilder().setExperimentId("sdfdsfsd").build();
    try {
      experimentServiceStub.getExperimentRunsInExperiment(getExperiment);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    LOGGER.info(
        "Get ExperimentRun from Experiment Negative test stop................................");
  }

  @Test
  public void c_getExperimentRunByIdTest() {
    LOGGER.info("Get ExperimentRunById test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentRunById request =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();

    GetExperimentRunById.Response response = experimentRunServiceStub.getExperimentRunById(request);

    LOGGER.info("getExperimentRunById Response : \n" + response.getExperimentRun());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun,
        response.getExperimentRun());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get ExperimentRunById test stop................................");
  }

  @Test
  public void c_getExperimentRunByIdNegativeTest() {
    LOGGER.info("Get ExperimentRunById Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    GetExperimentRunById request = GetExperimentRunById.newBuilder().build();

    try {
      experimentRunServiceStub.getExperimentRunById(request);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    request = GetExperimentRunById.newBuilder().setId("fdsfd").build();

    try {
      experimentRunServiceStub.getExperimentRunById(request);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info("Get ExperimentRunById Negative test stop................................");
  }

  @Test
  public void c_getExperimentRunByNameTest() {
    LOGGER.info("Get ExperimentRunByName test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentRunByName request =
        GetExperimentRunByName.newBuilder()
            .setName(experimentRun.getName())
            .setExperimentId(experimentRun.getExperimentId())
            .build();

    GetExperimentRunByName.Response response =
        experimentRunServiceStub.getExperimentRunByName(request);

    LOGGER.info("getExperimentRunByName Response : \n" + response.getExperimentRun());
    assertEquals(
        "ExperimentRun name not match with expected experimentRun name ",
        experimentRun.getName(),
        response.getExperimentRun().getName());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get ExperimentRunByName test stop................................");
  }

  @Test
  public void c_getExperimentRunByNameNegativeTest() {
    LOGGER.info("Get ExperimentRunByName Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    GetExperimentRunByName request = GetExperimentRunByName.newBuilder().build();

    try {
      experimentRunServiceStub.getExperimentRunByName(request);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Get ExperimentRunByName Negative test stop................................");
  }

  @Test
  public void d_updateExperimentRunNameOrDescription() {
    LOGGER.info(
        "Update ExperimentRun Name & Description test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());
    experiment = getExperimentByIdResponse.getExperiment();

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    UpdateExperimentRunName request =
        UpdateExperimentRunName.newBuilder()
            .setId(experimentRun.getId())
            .setName("ExperimentRun Name updated " + Calendar.getInstance().getTimeInMillis())
            .build();

    UpdateExperimentRunName.Response response =
        experimentRunServiceStub.updateExperimentRunName(request);
    LOGGER.info("UpdateExperimentRunName Response : " + response.getExperimentRun());
    assertEquals(
        "ExperimentRun name not match with expected experimentRun name",
        request.getName(),
        response.getExperimentRun().getName());

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());
    experiment = getExperimentByIdResponse.getExperiment();

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    UpdateExperimentRunDescription request2 =
        UpdateExperimentRunDescription.newBuilder()
            .setId(experimentRun.getId())
            .setDescription(
                "this is a ExperimentRun description updated "
                    + Calendar.getInstance().getTimeInMillis())
            .build();

    UpdateExperimentRunDescription.Response response2 =
        experimentRunServiceStub.updateExperimentRunDescription(request2);
    assertEquals(
        "ExperimentRun Description do not match with expected experimentRun name",
        request2.getDescription(),
        response2.getExperimentRun().getDescription());

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response2.getExperimentRun().getDateUpdated());

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());
    experiment = getExperimentByIdResponse.getExperiment();

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    try {
      String name =
          "Experiment of Human Activity Recognition using Smartphone Dataset Human Activity Recognition using Smartphone Dataset Human Activity Recognition using Smartphone Dataset Human Activity Recognition using Smartphone Dataset Human Activity Recognition using Smartphone Dataset";
      request = request.toBuilder().setName(name).build();
      experimentRunServiceStub.updateExperimentRunName(request);
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
        "Update ExperimentRun Name & Description test stop................................");
  }

  @Test
  public void d_updateExperimentRunNameOrDescriptionNegativeTest() {
    LOGGER.info(
        "Update ExperimentRun Name & Description Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    UpdateExperimentRunDescription request =
        UpdateExperimentRunDescription.newBuilder()
            .setDescription(
                "this is a ExperimentRun description updated "
                    + Calendar.getInstance().getTimeInMillis())
            .build();

    try {
      experimentRunServiceStub.updateExperimentRunDescription(request);
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
        "Update ExperimentRun Name & Description Negative test stop................................");
  }

  @Test
  public void e_addExperimentRunTags() {
    LOGGER.info("Add ExperimentRun tags test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());
    experiment = getExperimentByIdResponse.getExperiment();

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    List<String> tags = new ArrayList<>();
    tags.add("Test Added tag");
    tags.add("Test Added tag 2");

    AddExperimentRunTags request =
        AddExperimentRunTags.newBuilder().setId(experimentRun.getId()).addAllTags(tags).build();

    AddExperimentRunTags.Response aertResponse =
        experimentRunServiceStub.addExperimentRunTags(request);
    LOGGER.info("AddExperimentRunTags Response : \n" + aertResponse.getExperimentRun());
    assertEquals(4, aertResponse.getExperimentRun().getTagsCount());

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        aertResponse.getExperimentRun().getDateUpdated());
    experimentRun = aertResponse.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    tags = new ArrayList<>();
    tags.add("Test Added tag 3");
    tags.add("Test Added tag 2");

    request =
        AddExperimentRunTags.newBuilder().setId(experimentRun.getId()).addAllTags(tags).build();

    aertResponse = experimentRunServiceStub.addExperimentRunTags(request);
    LOGGER.info("AddExperimentRunTags Response : \n" + aertResponse.getExperimentRun());
    assertEquals(5, aertResponse.getExperimentRun().getTagsCount());

    try {
      String tag52 = "Human Activity Recognition using Smartphone Dataset";
      request = request.toBuilder().addTags(tag52).build();
      experimentRunServiceStub.addExperimentRunTags(request);
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

    LOGGER.info("Add ExperimentRun tags test stop................................");
  }

  @Test
  public void ea_addExperimentRunTagsNegativeTest() {
    LOGGER.info("Add ExperimentRun tags Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    List<String> tags = new ArrayList<>();
    tags.add("Test Added tag " + Calendar.getInstance().getTimeInMillis());
    tags.add("Test Added tag 2 " + Calendar.getInstance().getTimeInMillis());

    AddExperimentRunTags request = AddExperimentRunTags.newBuilder().addAllTags(tags).build();

    try {
      experimentRunServiceStub.addExperimentRunTags(request);
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

    LOGGER.info("Add ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void eb_addExperimentRunTag() {
    LOGGER.info("Add ExperimentRun tag test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());
    experiment = getExperimentByIdResponse.getExperiment();

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    AddExperimentRunTag request =
        AddExperimentRunTag.newBuilder()
            .setId(experimentRun.getId())
            .setTag("Added new tag 1")
            .build();

    AddExperimentRunTag.Response aertResponse =
        experimentRunServiceStub.addExperimentRunTag(request);
    LOGGER.info("AddExperimentRunTag Response : \n" + aertResponse.getExperimentRun());
    assertEquals(
        "ExperimentRun tags not match with expected experimentRun tags",
        3,
        aertResponse.getExperimentRun().getTagsCount());

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        aertResponse.getExperimentRun().getDateUpdated());

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    try {
      String tag52 = "Human Activity Recognition using Smartphone Dataset";
      request = request.toBuilder().setTag(tag52).build();
      experimentRunServiceStub.addExperimentRunTag(request);
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

    LOGGER.info("Add ExperimentRun tags test stop................................");
  }

  @Test
  public void ec_addExperimentRunTagNegativeTest() {
    LOGGER.info("Add ExperimentRun tag Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    AddExperimentRunTag request = AddExperimentRunTag.newBuilder().setTag("Tag_xyz").build();

    try {
      experimentRunServiceStub.addExperimentRunTag(request);
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

    LOGGER.info("Add ExperimentRun tag Negative test stop................................");
  }

  @Test
  public void ee_getExperimentRunTags() {
    LOGGER.info("Get ExperimentRun tags test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetTags request = GetTags.newBuilder().setId(experimentRun.getId()).build();

    GetTags.Response response = experimentRunServiceStub.getExperimentRunTags(request);
    LOGGER.info("GetExperimentRunTags Response : \n" + response.getTagsList());
    assertEquals(
        "ExperimentRun tags not match with expected experimentRun tags",
        experimentRun.getTagsList(),
        response.getTagsList());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get ExperimentRun tags test stop................................");
  }

  @Test
  public void eea_getExperimentRunTagsNegativeTest() {
    LOGGER.info("Get ExperimentRun tags Negative test start................................");

    GetTags request = GetTags.newBuilder().build();
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    try {
      experimentRunServiceStub.getExperimentRunTags(request);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Get ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void f_deleteExperimentRunTags() {
    LOGGER.info("Delete ExperimentRun tags test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());
    experiment = getExperimentByIdResponse.getExperiment();

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    List<String> removableTagList = new ArrayList<>();
    if (experimentRun.getTagsList().size() > 1) {
      removableTagList =
          experimentRun.getTagsList().subList(0, experimentRun.getTagsList().size() - 1);
    }
    DeleteExperimentRunTags request =
        DeleteExperimentRunTags.newBuilder()
            .setId(experimentRun.getId())
            .addAllTags(removableTagList)
            .build();

    DeleteExperimentRunTags.Response response =
        experimentRunServiceStub.deleteExperimentRunTags(request);
    LOGGER.info(
        "DeleteExperimentRunTags Response : \n" + response.getExperimentRun().getTagsList());
    assertTrue(response.getExperimentRun().getTagsList().size() <= 1);

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    if (response.getExperimentRun().getTagsList().size() > 0) {
      request =
          DeleteExperimentRunTags.newBuilder()
              .setId(experimentRun.getId())
              .setDeleteAll(true)
              .build();

      response = experimentRunServiceStub.deleteExperimentRunTags(request);
      LOGGER.info(
          "DeleteExperimentRunTags Response : \n" + response.getExperimentRun().getTagsList());
      assertEquals(0, response.getExperimentRun().getTagsList().size());
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Delete ExperimentRun tags test stop................................");
  }

  @Test
  public void fa_deleteExperimentRunTagsNegativeTest() {
    LOGGER.info("Delete ExperimentRun tags Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    DeleteExperimentRunTags request = DeleteExperimentRunTags.newBuilder().build();

    try {
      experimentRunServiceStub.deleteExperimentRunTags(request);
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

    LOGGER.info("Delete ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void fb_deleteExperimentRunTag() {
    LOGGER.info("Delete ExperimentRun tag test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteExperimentRunTag request =
        DeleteExperimentRunTag.newBuilder().setId(experimentRun.getId()).setTag("Tag_1").build();

    DeleteExperimentRunTag.Response response =
        experimentRunServiceStub.deleteExperimentRunTag(request);
    LOGGER.info("DeleteExperimentRunTag Response : \n" + response.getExperimentRun().getTagsList());
    assertFalse(response.getExperimentRun().getTagsList().contains("tag_abc"));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Delete ExperimentRun tags test stop................................");
  }

  /**
   * This test tests comparision predicates on numeric Key Values (Hyperparameters in this case) It
   * creates a project with two Experiments , each with two experiment runs. E1 ER1 hyperparameter.C
   * = 0.0001 E1 ER2 no hyperparameters E2 ER1 hyperparameter.C = 0.0001 E2 ER1 hyperparameter.C =
   * 1E-6
   *
   * <p>It then filters on C >= 0.0001 and expects 2 ExperimentRuns in results
   */
  @Test
  public void findExperimentRunsHyperparameter() {
    LOGGER.info("FindExperimentRuns test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    // Create project
    CreateProject createProjectRequest =
        projectTest.getCreateProjectRequest("experimentRun_project_ferh");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_ferh_1");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment1 = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");

    Map<String, ExperimentRun> experimentRunMap = new HashMap<>();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequestSimple(
            project.getId(), experiment1.getId(), "ExperimentRun_ferh_1");
    KeyValue hyperparameter1 = generateNumericKeyValue("C", 0.0001);
    createExperimentRunRequest =
        createExperimentRunRequest.toBuilder().addHyperparameters(hyperparameter1).build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun11 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun11.getId(), experimentRun11);
    LOGGER.info("ExperimentRun created successfully");
    createExperimentRunRequest =
        getCreateExperimentRunRequestSimple(
            project.getId(), experiment1.getId(), "ExperimentRun_ferh_2");
    createExperimentRunRequest = createExperimentRunRequest.toBuilder().build();
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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_ferh_2");
    createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment2 = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");

    createExperimentRunRequest =
        getCreateExperimentRunRequestSimple(
            project.getId(), experiment2.getId(), "ExperimentRun_ferh_2");
    hyperparameter1 = generateNumericKeyValue("C", 0.0001);
    createExperimentRunRequest =
        createExperimentRunRequest.toBuilder().addHyperparameters(hyperparameter1).build();
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
        getCreateExperimentRunRequestSimple(
            project.getId(), experiment2.getId(), "ExperimentRun_ferh_1");
    hyperparameter1 = generateNumericKeyValue("C", 1e-6);
    createExperimentRunRequest =
        createExperimentRunRequest.toBuilder().addHyperparameters(hyperparameter1).build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun22 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun22.getId(), experimentRun22);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun22.getName());

    Value hyperparameterFilter = Value.newBuilder().setNumberValue(0.0001).build();
    KeyValueQuery CGTE0_0001 =
        KeyValueQuery.newBuilder()
            .setKey("hyperparameters.C")
            .setValue(hyperparameterFilter)
            .setOperator(Operator.GTE)
            .setValueType(ValueType.NUMBER)
            .build();

    FindExperimentRuns findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(project.getId())
            .addPredicates(CGTE0_0001)
            .setAscending(false)
            .setIdsOnly(false)
            .build();

    FindExperimentRuns.Response response =
        experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

    assertEquals(
        "Total records count not matched with expected records count",
        2,
        response.getTotalRecords());
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        2,
        response.getExperimentRunsCount());
    for (ExperimentRun exprRun : response.getExperimentRunsList()) {
      for (KeyValue kv : exprRun.getHyperparametersList()) {
        if (kv.getKey() == "C") {
          assertEquals(
              "Value should be GTE 0.0001 " + kv, true, kv.getValue().getNumberValue() > 0.0001);
        }
      }
    }
    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("FindExperimentRuns test stop................................");
  }

  private KeyValue generateNumericKeyValue(String key, Double value) {
    return KeyValue.newBuilder()
        .setKey(key)
        .setValue(Value.newBuilder().setNumberValue(value).build())
        .build();
  }

  @Test
  public void g_logObservationTest() {
    LOGGER.info(" Log Observation in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    Observation observation =
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("New Added Key " + Calendar.getInstance().getTimeInMillis())
                    .setValue(intValue)
                    .setValueType(ValueType.NUMBER)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .build();

    LogObservation logObservationRequest =
        LogObservation.newBuilder()
            .setId(experimentRun.getId())
            .setObservation(observation)
            .build();

    LogObservation.Response response =
        experimentRunServiceStub.logObservation(logObservationRequest);

    LOGGER.info("LogObservation Response : \n" + response.getExperimentRun());
    assertTrue(response.getExperimentRun().getObservationsList().contains(observation));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Log Observation in ExperimentRun tags test stop................................");
  }

  @Test
  public void g_logObservationNegativeTest() {
    LOGGER.info(
        " Log Observation in ExperimentRun Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    Observation observation =
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("New Added Key " + Calendar.getInstance().getTimeInMillis())
                    .setValue(intValue)
                    .setValueType(ValueType.NUMBER)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .build();

    LogObservation logObservationRequest =
        LogObservation.newBuilder().setObservation(observation).build();

    try {
      experimentRunServiceStub.logObservation(logObservationRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logObservationRequest =
        LogObservation.newBuilder()
            .setId("sdfsd")
            .setObservation(experimentRun.getObservations(0))
            .build();

    try {
      experimentRunServiceStub.logObservation(logObservationRequest);
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

    LOGGER.info(
        "Log Observation in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void g_logObservationsTest() {
    LOGGER.info(" Log Observations in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    List<Observation> observations = new ArrayList<>();
    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    Observation observation1 =
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("New Added Key " + Calendar.getInstance().getTimeInMillis())
                    .setValue(intValue)
                    .setValueType(ValueType.NUMBER)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .build();
    observations.add(observation1);

    Value stringValue =
        Value.newBuilder()
            .setStringValue("new Observation " + Calendar.getInstance().getTimeInMillis())
            .build();
    Observation observation2 =
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("New Added Key " + Calendar.getInstance().getTimeInMillis())
                    .setValue(stringValue)
                    .setValueType(ValueType.STRING)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .build();
    observations.add(observation2);

    LogObservations logObservationRequest =
        LogObservations.newBuilder()
            .setId(experimentRun.getId())
            .addAllObservations(observations)
            .build();

    LogObservations.Response response =
        experimentRunServiceStub.logObservations(logObservationRequest);

    LOGGER.info("LogObservation Response : \n" + response.getExperimentRun());
    assertTrue(
        "ExperimentRun observations not match with expected ExperimentRun observation",
        response.getExperimentRun().getObservationsList().containsAll(observations));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    logObservationRequest =
        LogObservations.newBuilder()
            .setId(experimentRun.getId())
            .addAllObservations(experimentRun.getObservationsList())
            .build();

    response = experimentRunServiceStub.logObservations(logObservationRequest);

    LOGGER.info(
        "Duplicate LogObservation Response : \n"
            + response.getExperimentRun().getObservationsList());
    assertEquals(
        "Existing observations count not match with expected observations count",
        experimentRun.getObservationsList().size() + experimentRun.getObservationsList().size(),
        response.getExperimentRun().getObservationsList().size());

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Log Observations in ExperimentRun tags test stop................................");
  }

  @Test
  public void g_logObservationsNegativeTest() {
    LOGGER.info(
        " Log Observations in ExperimentRun Negative test start................................");
    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    List<Observation> observations = new ArrayList<>();
    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    Observation observation1 =
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("New Added Key " + Calendar.getInstance().getTimeInMillis())
                    .setValue(intValue)
                    .setValueType(ValueType.NUMBER)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .build();
    observations.add(observation1);

    Value stringValue =
        Value.newBuilder()
            .setStringValue("new Observation " + Calendar.getInstance().getTimeInMillis())
            .build();
    Observation observation2 =
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("New Added Key " + Calendar.getInstance().getTimeInMillis())
                    .setValue(stringValue)
                    .setValueType(ValueType.STRING)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .build();
    observations.add(observation2);

    LogObservations logObservationRequest =
        LogObservations.newBuilder().addAllObservations(observations).build();

    try {
      experimentRunServiceStub.logObservations(logObservationRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logObservationRequest =
        LogObservations.newBuilder()
            .setId("sdfsd")
            .addAllObservations(experimentRun.getObservationsList())
            .build();

    try {
      experimentRunServiceStub.logObservations(logObservationRequest);
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

    LOGGER.info(
        "Log Observations in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void h_getLogObservationTest() {
    LOGGER.info("Get Observation from ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetObservations getLogObservationRequest =
        GetObservations.newBuilder()
            .setId(experimentRun.getId())
            .setObservationKey("Google developer Observation artifact")
            .build();

    GetObservations.Response response =
        experimentRunServiceStub.getObservations(getLogObservationRequest);

    LOGGER.info("GetObservations Response : " + response.getObservationsCount());
    for (Observation observation : response.getObservationsList()) {
      if (observation.hasAttribute()) {
        assertEquals(
            "ExperimentRun observations not match with expected observations ",
            "Google developer Observation artifact",
            observation.getAttribute().getKey());
      } else if (observation.hasArtifact()) {
        assertEquals(
            "ExperimentRun observations not match with expected observations ",
            "Google developer Observation artifact",
            observation.getArtifact().getKey());
      }
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info(
        "Get Observation from ExperimentRun tags test stop................................");
  }

  @Test
  public void h_getLogObservationNegativeTest() {
    LOGGER.info(
        "Get Observation from ExperimentRun Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    GetObservations getLogObservationRequest =
        GetObservations.newBuilder()
            .setObservationKey("Google developer Observation artifact")
            .build();

    try {
      experimentRunServiceStub.getObservations(getLogObservationRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getLogObservationRequest =
        GetObservations.newBuilder()
            .setId("dfsdfs")
            .setObservationKey("Google developer Observation artifact")
            .build();

    try {
      experimentRunServiceStub.getObservations(getLogObservationRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info(
        "Get Observation from ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void i_logMetricTest() {
    LOGGER.info(" Log Metric in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    KeyValue keyValue =
        KeyValue.newBuilder()
            .setKey("New Added Metric " + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build();

    LogMetric logMetricRequest =
        LogMetric.newBuilder().setId(experimentRun.getId()).setMetric(keyValue).build();

    LogMetric.Response response = experimentRunServiceStub.logMetric(logMetricRequest);

    LOGGER.info("LogMetric Response : \n" + response.getExperimentRun());
    assertTrue(
        "ExperimentRun metric not match with expected experimentRun metric",
        response.getExperimentRun().getMetricsList().contains(keyValue));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Log Metric in ExperimentRun tags test stop................................");
  }

  @Test
  public void i_logMetricNegativeTest() {
    LOGGER.info(" Log Metric in ExperimentRun Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    KeyValue keyValue =
        KeyValue.newBuilder()
            .setKey("New Added Metric " + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build();

    LogMetric logMetricRequest = LogMetric.newBuilder().setMetric(keyValue).build();

    try {
      experimentRunServiceStub.logMetric(logMetricRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logMetricRequest = LogMetric.newBuilder().setId("dfsdfsd").setMetric(keyValue).build();

    try {
      experimentRunServiceStub.logMetric(logMetricRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    logMetricRequest =
        LogMetric.newBuilder()
            .setId(experimentRun.getId())
            .setMetric(experimentRun.getMetricsList().get(0))
            .build();

    try {
      experimentRunServiceStub.logMetric(logMetricRequest);
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

    LOGGER.info(
        "Log Metric in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void i_logMetricsTest() {
    LOGGER.info(" Log Metrics in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    List<KeyValue> keyValues = new ArrayList<>();
    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    KeyValue keyValue1 =
        KeyValue.newBuilder()
            .setKey("New Added Metric " + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build();
    keyValues.add(keyValue1);
    Value stringValue =
        Value.newBuilder()
            .setStringValue("New Added Metric " + Calendar.getInstance().getTimeInMillis())
            .build();
    KeyValue keyValue2 =
        KeyValue.newBuilder()
            .setKey("New Added Metric " + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build();
    keyValues.add(keyValue2);

    LogMetrics logMetricRequest =
        LogMetrics.newBuilder().setId(experimentRun.getId()).addAllMetrics(keyValues).build();

    LogMetrics.Response response = experimentRunServiceStub.logMetrics(logMetricRequest);

    LOGGER.info("LogMetrics Response : \n" + response.getExperimentRun());
    assertTrue(
        "ExperimentRun metrics not match with expected experimentRun metrics",
        response.getExperimentRun().getMetricsList().containsAll(keyValues));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Log Metrics in ExperimentRun tags test stop................................");
  }

  @Test
  public void i_logMetricsNegativeTest() {
    LOGGER.info(
        " Log Metrics in ExperimentRun Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    List<KeyValue> keyValues = new ArrayList<>();
    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    KeyValue keyValue1 =
        KeyValue.newBuilder()
            .setKey("New Added Metric " + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build();
    keyValues.add(keyValue1);
    Value stringValue =
        Value.newBuilder()
            .setStringValue("New Added Metric " + Calendar.getInstance().getTimeInMillis())
            .build();
    KeyValue keyValue2 =
        KeyValue.newBuilder()
            .setKey("New Added Metric " + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build();
    keyValues.add(keyValue2);

    LogMetrics logMetricRequest = LogMetrics.newBuilder().addAllMetrics(keyValues).build();

    try {
      experimentRunServiceStub.logMetrics(logMetricRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logMetricRequest = LogMetrics.newBuilder().setId("dfsdfsd").addAllMetrics(keyValues).build();

    try {
      experimentRunServiceStub.logMetrics(logMetricRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
    }

    logMetricRequest =
        LogMetrics.newBuilder()
            .setId(experimentRun.getId())
            .addAllMetrics(experimentRun.getMetricsList())
            .build();

    try {
      experimentRunServiceStub.logMetrics(logMetricRequest);
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

    LOGGER.info(
        "Log Metrics in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void j_getMetricsTest() {
    LOGGER.info("Get Metrics from ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetMetrics getMetricsRequest = GetMetrics.newBuilder().setId(experimentRun.getId()).build();

    GetMetrics.Response response = experimentRunServiceStub.getMetrics(getMetricsRequest);
    LOGGER.info("GetMetrics Response : " + response.getMetricsCount());
    assertEquals(
        "ExperimentRun metrics not match with expected experimentRun metrics",
        experimentRun.getMetricsList(),
        response.getMetricsList());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get Metrics from ExperimentRun tags test stop................................");
  }

  @Test
  public void j_getMetricsNegativeTest() {
    LOGGER.info(
        "Get Metrics from ExperimentRun Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    GetMetrics getMetricsRequest = GetMetrics.newBuilder().build();

    try {
      experimentRunServiceStub.getMetrics(getMetricsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getMetricsRequest = GetMetrics.newBuilder().setId("sdfdsfsd").build();

    try {
      experimentRunServiceStub.getMetrics(getMetricsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
    }

    LOGGER.info(
        "Get Metrics from ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void k_logDatasetTest() {
    LOGGER.info(" Log Dataset in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    DatasetTest datasetTest = new DatasetTest();
    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        createDatasetResponse.getDataset().getName());

    Artifact artifact =
        Artifact.newBuilder()
            .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactType.DATA)
            .setLinkedArtifactId(createDatasetResponse.getDataset().getId())
            .build();

    LogDataset logDatasetRequest =
        LogDataset.newBuilder().setId(experimentRun.getId()).setDataset(artifact).build();

    LogDataset.Response response = experimentRunServiceStub.logDataset(logDatasetRequest);

    LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());
    assertTrue(
        "Experiment dataset not match with expected dataset",
        response.getExperimentRun().getDatasetsList().contains(artifact));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());

    artifact =
        artifact
            .toBuilder()
            .setPath(
                "Overwritten data, This is overwritten data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactType.DATA)
            .build();

    logDatasetRequest =
        LogDataset.newBuilder().setId(experimentRun.getId()).setDataset(artifact).build();

    try {
      experimentRunServiceStub.logDataset(logDatasetRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    logDatasetRequest =
        LogDataset.newBuilder()
            .setId(experimentRun.getId())
            .setDataset(artifact)
            .setOverwrite(true)
            .build();
    response = experimentRunServiceStub.logDataset(logDatasetRequest);
    assertTrue(
        "Experiment dataset not match with expected dataset",
        response.getExperimentRun().getDatasetsList().contains(artifact));

    artifact =
        Artifact.newBuilder()
            .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactType.MODEL)
            .setLinkedArtifactId(createDatasetResponse.getDataset().getId())
            .build();

    logDatasetRequest =
        LogDataset.newBuilder().setId(experimentRun.getId()).setDataset(artifact).build();

    try {
      experimentRunServiceStub.logDataset(logDatasetRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteDataset deleteDataset =
        DeleteDataset.newBuilder().setId(createDatasetResponse.getDataset().getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Log Dataset in ExperimentRun tags test stop................................");
  }

  @Test
  public void k_logDatasetNegativeTest() {
    LOGGER.info(
        " Log Dataset in ExperimentRun Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    Artifact artifact =
        Artifact.newBuilder()
            .setKey("Google Pay datasets")
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactType.MODEL)
            .setLinkedArtifactId("dadasdadaasd")
            .build();

    LogDataset logDatasetRequest = LogDataset.newBuilder().setDataset(artifact).build();

    try {
      experimentRunServiceStub.logDataset(logDatasetRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    artifact = artifact.toBuilder().setArtifactType(ArtifactType.DATA).build();
    logDatasetRequest = LogDataset.newBuilder().setId("sdsdsa").setDataset(artifact).build();

    try {
      experimentRunServiceStub.logDataset(logDatasetRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    artifact =
        Artifact.newBuilder()
            .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactType.DATA)
            .setLinkedArtifactId("fsdfsdfsdfds")
            .build();

    logDatasetRequest =
        LogDataset.newBuilder().setId(experimentRun.getId()).setDataset(artifact).build();

    LogDataset.Response response = experimentRunServiceStub.logDataset(logDatasetRequest);

    LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());
    assertTrue(
        "Experiment dataset not match with expected dataset",
        response.getExperimentRun().getDatasetsList().contains(artifact));

    logDatasetRequest =
        LogDataset.newBuilder().setId(experimentRun.getId()).setDataset(artifact).build();
    try {
      experimentRunServiceStub.logDataset(logDatasetRequest);
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

    LOGGER.info(
        "Log Dataset in ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void k_logDatasetsTest() {
    LOGGER.info(" Log Datasets in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    DatasetTest datasetTest = new DatasetTest();
    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    Map<String, Artifact> artifactMap = new HashMap<>();
    for (Artifact existingDataset : experimentRun.getDatasetsList()) {
      artifactMap.put(existingDataset.getKey(), existingDataset);
    }
    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact1 =
        Artifact.newBuilder()
            .setKey("Google Pay datasets_1")
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactType.DATA)
            .setLinkedArtifactId(dataset.getId())
            .build();
    artifacts.add(artifact1);
    artifactMap.put(artifact1.getKey(), artifact1);
    Artifact artifact2 =
        Artifact.newBuilder()
            .setKey("Google Pay datasets_2")
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactType.DATA)
            .setLinkedArtifactId(dataset.getId())
            .build();
    artifacts.add(artifact2);
    artifactMap.put(artifact2.getKey(), artifact2);

    LogDatasets logDatasetRequest =
        LogDatasets.newBuilder().setId(experimentRun.getId()).addAllDatasets(artifacts).build();

    LogDatasets.Response response = experimentRunServiceStub.logDatasets(logDatasetRequest);
    LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());

    for (Artifact datasetArtifact : response.getExperimentRun().getDatasetsList()) {
      assertEquals(
          "Experiment datasets not match with expected datasets",
          artifactMap.get(datasetArtifact.getKey()),
          datasetArtifact);
    }

    logDatasetRequest =
        LogDatasets.newBuilder().setId(experimentRun.getId()).addAllDatasets(artifacts).build();

    try {
      experimentRunServiceStub.logDatasets(logDatasetRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    artifact1 =
        artifact1
            .toBuilder()
            .setPath(
                "Overwritten data 1, This is overwritten data artifact type in Google Pay datasets")
            .build();
    artifactMap.put(artifact1.getKey(), artifact1);
    artifact2 =
        artifact2
            .toBuilder()
            .setPath(
                "Overwritten data 2, This is overwritten data artifact type in Google Pay datasets")
            .build();
    artifactMap.put(artifact2.getKey(), artifact2);

    logDatasetRequest =
        LogDatasets.newBuilder()
            .setId(experimentRun.getId())
            .setOverwrite(true)
            .addDatasets(artifact1)
            .addDatasets(artifact2)
            .build();
    response = experimentRunServiceStub.logDatasets(logDatasetRequest);
    LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());

    for (Artifact datasetArtifact : response.getExperimentRun().getDatasetsList()) {
      assertEquals(
          "Experiment datasets not match with expected datasets",
          artifactMap.get(datasetArtifact.getKey()),
          datasetArtifact);
    }

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteDataset deleteDataset =
        DeleteDataset.newBuilder().setId(createDatasetResponse.getDataset().getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Log Datasets in ExperimentRun tags test stop................................");
  }

  @Test
  public void k_logDatasetsNegativeTest() {
    LOGGER.info(
        " Log Datasets in ExperimentRun Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    List<Artifact> artifacts = new ArrayList<>();
    Artifact artifact1 =
        Artifact.newBuilder()
            .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactType.MODEL)
            .build();
    artifacts.add(artifact1);
    Artifact artifact2 =
        Artifact.newBuilder()
            .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactType.DATA)
            .build();
    artifacts.add(artifact2);

    LogDatasets logDatasetRequest = LogDatasets.newBuilder().addAllDatasets(artifacts).build();

    try {
      experimentRunServiceStub.logDatasets(logDatasetRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logDatasetRequest = LogDatasets.newBuilder().setId("sdsdsa").addAllDatasets(artifacts).build();

    try {
      experimentRunServiceStub.logDatasets(logDatasetRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    logDatasetRequest =
        LogDatasets.newBuilder()
            .setId(experimentRun.getId())
            .addAllDatasets(experimentRun.getDatasetsList())
            .build();

    try {
      experimentRunServiceStub.logDatasets(logDatasetRequest);
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

    LOGGER.info(
        "Log Datasets in ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void l_getDatasetsTest() {
    LOGGER.info("Get Datasets from ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetDatasets getDatasetsRequest = GetDatasets.newBuilder().setId(experimentRun.getId()).build();

    GetDatasets.Response response = experimentRunServiceStub.getDatasets(getDatasetsRequest);
    LOGGER.info("GetDatasets Response : " + response.getDatasetsCount());
    assertEquals(
        "Experiment datasets not match with expected datasets",
        experimentRun.getDatasetsList(),
        response.getDatasetsList());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get Datasets from ExperimentRun tags test stop................................");
  }

  @Test
  public void l_getDatasetsNegativeTest() {
    LOGGER.info(
        "Get Datasets from ExperimentRun Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    GetDatasets getDatasetsRequest = GetDatasets.newBuilder().build();

    try {
      experimentRunServiceStub.getDatasets(getDatasetsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getDatasetsRequest = GetDatasets.newBuilder().setId("sdfsdfdsf").build();

    try {
      experimentRunServiceStub.getDatasets(getDatasetsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
    }

    LOGGER.info(
        "Get Datasets from ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void m_logArtifactTest() {
    LOGGER.info(" Log Artifact in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    Artifact artifact =
        Artifact.newBuilder()
            .setKey("Google Pay Artifact " + Calendar.getInstance().getTimeInMillis())
            .setPath("46513216546" + Calendar.getInstance().getTimeInMillis())
            .setArtifactType(ArtifactType.TENSORBOARD)
            .build();

    LogArtifact logArtifactRequest =
        LogArtifact.newBuilder().setId(experimentRun.getId()).setArtifact(artifact).build();

    LogArtifact.Response response = experimentRunServiceStub.logArtifact(logArtifactRequest);
    LOGGER.info("LogArtifact Response : " + response.getExperimentRun().getArtifactsCount());
    assertEquals(
        "Experiment artifact count not match with expected artifact count",
        experimentRun.getArtifactsCount() + 1,
        response.getExperimentRun().getArtifactsCount());

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Log Artifact in ExperimentRun tags test stop................................");
  }

  @Test
  public void m_logArtifactNegativeTest() {
    LOGGER.info(
        " Log Artifact in ExperimentRun Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    Artifact artifact =
        Artifact.newBuilder()
            .setKey("Google Pay Artifact")
            .setPath("46513216546" + Calendar.getInstance().getTimeInMillis())
            .setArtifactType(ArtifactType.TENSORBOARD)
            .build();

    LogArtifact logArtifactRequest = LogArtifact.newBuilder().setArtifact(artifact).build();
    try {
      experimentRunServiceStub.logArtifact(logArtifactRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logArtifactRequest = LogArtifact.newBuilder().setId("asda").setArtifact(artifact).build();
    try {
      experimentRunServiceStub.logArtifact(logArtifactRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    logArtifactRequest =
        LogArtifact.newBuilder()
            .setId(experimentRun.getId())
            .setArtifact(experimentRun.getArtifactsList().get(0))
            .build();
    try {
      experimentRunServiceStub.logArtifact(logArtifactRequest);
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

    LOGGER.info(
        "Log Artifact in ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void m_logArtifactsTest() {
    LOGGER.info(" Log Artifacts in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

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

    LogArtifacts logArtifactRequest =
        LogArtifacts.newBuilder().setId(experimentRun.getId()).addAllArtifacts(artifacts).build();

    LogArtifacts.Response response = experimentRunServiceStub.logArtifacts(logArtifactRequest);

    LOGGER.info("LogArtifact Response : \n" + response.getExperimentRun());
    assertEquals(
        "ExperimentRun artifacts not match with expected artifacts",
        (experimentRun.getArtifactsCount() + artifacts.size()),
        response.getExperimentRun().getArtifactsList().size());

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Log Artifacts in ExperimentRun tags test stop................................");
  }

  @Test
  public void m_logArtifactsNegativeTest() {
    LOGGER.info(
        " Log Artifacts in ExperimentRun Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

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

    LogArtifacts logArtifactRequest = LogArtifacts.newBuilder().addAllArtifacts(artifacts).build();
    try {
      experimentRunServiceStub.logArtifacts(logArtifactRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logArtifactRequest = LogArtifacts.newBuilder().setId("asda").addAllArtifacts(artifacts).build();
    try {
      experimentRunServiceStub.logArtifacts(logArtifactRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    logArtifactRequest =
        LogArtifacts.newBuilder()
            .setId(experimentRun.getId())
            .addAllArtifacts(experimentRun.getArtifactsList())
            .build();
    try {
      experimentRunServiceStub.logArtifacts(logArtifactRequest);
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

    LOGGER.info(
        "Log Artifacts in ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void n_getArtifactsTest() {
    LOGGER.info("Get Artifacts from ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetArtifacts getArtifactsRequest =
        GetArtifacts.newBuilder().setId(experimentRun.getId()).build();

    GetArtifacts.Response response = experimentRunServiceStub.getArtifacts(getArtifactsRequest);

    LOGGER.info("GetArtifacts Response : " + response.getArtifactsCount());
    assertEquals(
        "ExperimentRun artifacts not matched with expected artifacts",
        experimentRun.getArtifactsList(),
        response.getArtifactsList());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get Artifacts from ExperimentRun tags test stop................................");
  }

  @Test
  public void n_getArtifactsNegativeTest() {
    LOGGER.info(
        "Get Artifacts from ExperimentRun Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    GetArtifacts getArtifactsRequest = GetArtifacts.newBuilder().build();

    try {
      experimentRunServiceStub.getArtifacts(getArtifactsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getArtifactsRequest = GetArtifacts.newBuilder().setId("dssaa").build();

    try {
      experimentRunServiceStub.getArtifacts(getArtifactsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info(
        "Get Artifacts from ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void o_logHyperparameterTest() {
    LOGGER.info(" Log Hyperparameter in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    Value blobValue = Value.newBuilder().setStringValue("this is a blob data example").build();
    KeyValue hyperparameter =
        KeyValue.newBuilder()
            .setKey("Log new hyperparameter " + Calendar.getInstance().getTimeInMillis())
            .setValue(blobValue)
            .setValueType(ValueType.BLOB)
            .build();

    LogHyperparameter logHyperparameterRequest =
        LogHyperparameter.newBuilder()
            .setId(experimentRun.getId())
            .setHyperparameter(hyperparameter)
            .build();

    LogHyperparameter.Response response =
        experimentRunServiceStub.logHyperparameter(logHyperparameterRequest);

    LOGGER.info("LogHyperparameter Response : \n" + response.getExperimentRun());
    assertTrue(
        "ExperimentRun hyperparameter not match with expected hyperparameter",
        response.getExperimentRun().getHyperparametersList().contains(hyperparameter));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info(
        "Log Hyperparameter in ExperimentRun tags test stop................................");
  }

  @Test
  public void o_logHyperparameterNegativeTest() {
    LOGGER.info(
        " Log Hyperparameter in ExperimentRun Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    Value blobValue = Value.newBuilder().setStringValue("this is a blob data example").build();
    KeyValue hyperparameter =
        KeyValue.newBuilder()
            .setKey("Log new hyperparameter " + Calendar.getInstance().getTimeInMillis())
            .setValue(blobValue)
            .setValueType(ValueType.BLOB)
            .build();

    LogHyperparameter logHyperparameterRequest =
        LogHyperparameter.newBuilder().setHyperparameter(hyperparameter).build();

    try {
      experimentRunServiceStub.logHyperparameter(logHyperparameterRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logHyperparameterRequest =
        LogHyperparameter.newBuilder().setId("dsdsfs").setHyperparameter(hyperparameter).build();

    try {
      experimentRunServiceStub.logHyperparameter(logHyperparameterRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
    }

    logHyperparameterRequest =
        LogHyperparameter.newBuilder()
            .setId(experimentRun.getId())
            .setHyperparameter(experimentRun.getHyperparametersList().get(0))
            .build();

    try {
      experimentRunServiceStub.logHyperparameter(logHyperparameterRequest);
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

    LOGGER.info(
        "Log Hyperparameter in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void o_logHyperparametersTest() {
    LOGGER.info(" Log Hyperparameters in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    List<KeyValue> hyperparameters = new ArrayList<>();
    Value blobValue = Value.newBuilder().setStringValue("this is a blob data example").build();
    KeyValue hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("Log new hyperparameter " + Calendar.getInstance().getTimeInMillis())
            .setValue(blobValue)
            .setValueType(ValueType.BLOB)
            .build();
    hyperparameters.add(hyperparameter1);

    Value numValue = Value.newBuilder().setNumberValue(12.02125212).build();
    KeyValue hyperparameter2 =
        KeyValue.newBuilder()
            .setKey("Log new hyperparameter " + Calendar.getInstance().getTimeInMillis())
            .setValue(numValue)
            .setValueType(ValueType.NUMBER)
            .build();
    hyperparameters.add(hyperparameter2);

    LogHyperparameters logHyperparameterRequest =
        LogHyperparameters.newBuilder()
            .setId(experimentRun.getId())
            .addAllHyperparameters(hyperparameters)
            .build();

    LogHyperparameters.Response response =
        experimentRunServiceStub.logHyperparameters(logHyperparameterRequest);

    LOGGER.info("LogHyperparameters Response : \n" + response.getExperimentRun());
    assertTrue(
        "ExperimentRun hyperparameters not match with expected hyperparameters",
        response.getExperimentRun().getHyperparametersList().containsAll(hyperparameters));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info(
        "Log Hyperparameters in ExperimentRun tags test stop................................");
  }

  @Test
  public void o_logHyperparametersNegativeTest() {
    LOGGER.info(
        " Log Hyperparameters in ExperimentRun Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    List<KeyValue> hyperparameters = new ArrayList<>();
    Value blobValue = Value.newBuilder().setStringValue("this is a blob data example").build();
    KeyValue hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("Log new hyperparameter " + Calendar.getInstance().getTimeInMillis())
            .setValue(blobValue)
            .setValueType(ValueType.BLOB)
            .build();
    hyperparameters.add(hyperparameter1);

    Value numValue = Value.newBuilder().setNumberValue(12.02125212).build();
    KeyValue hyperparameter2 =
        KeyValue.newBuilder()
            .setKey("Log new hyperparameter " + Calendar.getInstance().getTimeInMillis())
            .setValue(numValue)
            .setValueType(ValueType.NUMBER)
            .build();
    hyperparameters.add(hyperparameter2);

    LogHyperparameters logHyperparameterRequest =
        LogHyperparameters.newBuilder().addAllHyperparameters(hyperparameters).build();

    try {
      experimentRunServiceStub.logHyperparameters(logHyperparameterRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logHyperparameterRequest =
        LogHyperparameters.newBuilder()
            .setId("dsdsfs")
            .addAllHyperparameters(hyperparameters)
            .build();

    try {
      experimentRunServiceStub.logHyperparameters(logHyperparameterRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    logHyperparameterRequest =
        LogHyperparameters.newBuilder()
            .setId(experimentRun.getId())
            .addAllHyperparameters(experimentRun.getHyperparametersList())
            .build();

    try {
      experimentRunServiceStub.logHyperparameters(logHyperparameterRequest);
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

    LOGGER.info(
        "Log Hyperparameters in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void p_getHyperparametersTest() {
    LOGGER.info(
        "Get Hyperparameters from ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetHyperparameters getHyperparametersRequest =
        GetHyperparameters.newBuilder().setId(experimentRun.getId()).build();

    GetHyperparameters.Response response =
        experimentRunServiceStub.getHyperparameters(getHyperparametersRequest);

    LOGGER.info("GetHyperparameters Response : " + response.getHyperparametersCount());
    assertEquals(
        "ExperimentRun Hyperparameters not match with expected hyperparameters",
        experimentRun.getHyperparametersList(),
        response.getHyperparametersList());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info(
        "Get Hyperparameters from ExperimentRun tags test stop................................");
  }

  @Test
  public void p_getHyperparametersNegativeTest() {
    LOGGER.info(
        "Get Hyperparameters from ExperimentRun Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    GetHyperparameters getHyperparametersRequest = GetHyperparameters.newBuilder().build();

    try {
      experimentRunServiceStub.getHyperparameters(getHyperparametersRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getHyperparametersRequest = GetHyperparameters.newBuilder().setId("sdsssd").build();

    try {
      experimentRunServiceStub.getHyperparameters(getHyperparametersRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
    }

    LOGGER.info(
        "Get Hyperparameters from ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void q_logAttributeTest() {
    LOGGER.info(" Log Attribute in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    Value blobValue =
        Value.newBuilder().setStringValue("this is a blob data example of attribute").build();
    KeyValue attribute =
        KeyValue.newBuilder()
            .setKey("Log new attribute " + Calendar.getInstance().getTimeInMillis())
            .setValue(blobValue)
            .setValueType(ValueType.BLOB)
            .build();

    LogAttribute logAttributeRequest =
        LogAttribute.newBuilder().setId(experimentRun.getId()).setAttribute(attribute).build();

    LogAttribute.Response response = experimentRunServiceStub.logAttribute(logAttributeRequest);

    LOGGER.info("LogAttribute Response : \n" + response.getExperimentRun());
    assertTrue(
        "ExperimentRun attribute not match with expected attribute",
        response.getExperimentRun().getAttributesList().contains(attribute));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Log Attribute in ExperimentRun tags test stop................................");
  }

  @Test
  public void q_logAttributeNegativeTest() {
    LOGGER.info(
        " Log Attribute in ExperimentRun Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    Value blobValue =
        Value.newBuilder().setStringValue("this is a blob data example of attribute").build();
    KeyValue attribute =
        KeyValue.newBuilder()
            .setKey("Log new attribute " + Calendar.getInstance().getTimeInMillis())
            .setValue(blobValue)
            .setValueType(ValueType.BLOB)
            .build();

    LogAttribute logAttributeRequest = LogAttribute.newBuilder().setAttribute(attribute).build();

    try {
      experimentRunServiceStub.logAttribute(logAttributeRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logAttributeRequest = LogAttribute.newBuilder().setId("sdsds").setAttribute(attribute).build();

    try {
      experimentRunServiceStub.logAttribute(logAttributeRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
    }

    logAttributeRequest =
        LogAttribute.newBuilder()
            .setId(experimentRun.getId())
            .setAttribute(experimentRun.getAttributesList().get(0))
            .build();

    try {
      experimentRunServiceStub.logAttribute(logAttributeRequest);
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

    LOGGER.info(
        "Log Attribute in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void q_logAttributesTest() {
    LOGGER.info(" Log Attributes in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    List<KeyValue> attributes = new ArrayList<>();
    Value blobValue =
        Value.newBuilder().setStringValue("this is a blob data example of attribute").build();
    KeyValue attribute1 =
        KeyValue.newBuilder()
            .setKey("Log new attribute " + Calendar.getInstance().getTimeInMillis())
            .setValue(blobValue)
            .setValueType(ValueType.BLOB)
            .build();
    attributes.add(attribute1);
    Value stringValue =
        Value.newBuilder().setStringValue("this is a blob data example of attribute").build();
    KeyValue attribute2 =
        KeyValue.newBuilder()
            .setKey("Log new attribute " + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build();
    attributes.add(attribute2);

    LogAttributes logAttributeRequest =
        LogAttributes.newBuilder()
            .setId(experimentRun.getId())
            .addAllAttributes(attributes)
            .build();

    LogAttributes.Response response = experimentRunServiceStub.logAttributes(logAttributeRequest);

    LOGGER.info("LogAttributes Response : \n" + response.getExperimentRun());
    assertTrue(
        "ExperimentRun attributes not match with expected attributes",
        response.getExperimentRun().getAttributesList().containsAll(attributes));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Log Attributes in ExperimentRun tags test stop................................");
  }

  @Test
  public void q_logAttributesNegativeTest() {
    LOGGER.info(
        " Log Attributes in ExperimentRun Negative test start................................");
    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    List<KeyValue> attributes = new ArrayList<>();
    Value blobValue =
        Value.newBuilder().setStringValue("this is a blob data example of attribute").build();
    KeyValue attribute1 =
        KeyValue.newBuilder()
            .setKey("Log new attribute " + Calendar.getInstance().getTimeInMillis())
            .setValue(blobValue)
            .setValueType(ValueType.BLOB)
            .build();
    attributes.add(attribute1);
    Value stringValue =
        Value.newBuilder().setStringValue("this is a blob data example of attribute").build();
    KeyValue attribute2 =
        KeyValue.newBuilder()
            .setKey("Log new attribute " + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build();
    attributes.add(attribute2);

    LogAttributes logAttributeRequest =
        LogAttributes.newBuilder().addAllAttributes(attributes).build();

    try {
      experimentRunServiceStub.logAttributes(logAttributeRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logAttributeRequest =
        LogAttributes.newBuilder().setId("sdsds").addAllAttributes(attributes).build();

    try {
      experimentRunServiceStub.logAttributes(logAttributeRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    logAttributeRequest =
        LogAttributes.newBuilder()
            .setId(experimentRun.getId())
            .addAllAttributes(experimentRun.getAttributesList())
            .build();

    try {
      experimentRunServiceStub.logAttributes(logAttributeRequest);
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

    LOGGER.info(
        "Log Attributes in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void qq_addExperimentRunAttributes() {
    LOGGER.info("Add ExperimentRun Attributes test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

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

    AddExperimentRunAttributes request =
        AddExperimentRunAttributes.newBuilder()
            .setId(experimentRun.getId())
            .addAllAttributes(attributeList)
            .build();

    AddExperimentRunAttributes.Response response =
        experimentRunServiceStub.addExperimentRunAttributes(request);
    LOGGER.info("AddExperimentRunAttributes Response : \n" + response.getExperimentRun());
    assertTrue(
        "ExperimentRun attributes not match with expected attributes",
        response.getExperimentRun().getAttributesList().containsAll(attributeList));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Add ExperimentRun Attributes test stop................................");
  }

  @Test
  public void qq_addExperimentRunAttributesNegativeTest() {
    LOGGER.info("Add ExperimentRun attributes Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    AddExperimentRunAttributes request =
        AddExperimentRunAttributes.newBuilder().setId("xyz").build();

    try {
      experimentRunServiceStub.addExperimentRunAttributes(request);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    AddExperimentRunAttributes addAttributesRequest =
        AddExperimentRunAttributes.newBuilder().build();
    try {
      experimentRunServiceStub.addExperimentRunAttributes(addAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Add ExperimentRun attributes Negative test stop................................");
  }

  @Test
  public void r_getExperimentRunAttributesTest() {
    LOGGER.info("Get Attributes from ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    List<KeyValue> attributes = experimentRun.getAttributesList();
    LOGGER.info("Attributes size : " + attributes.size());

    if (attributes.size() == 0) {
      LOGGER.warn("Experiment Attributes not found in database ");
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

    GetAttributes getAttributesRequest =
        GetAttributes.newBuilder().setId(experimentRun.getId()).addAllAttributeKeys(keys).build();

    GetAttributes.Response response =
        experimentRunServiceStub.getExperimentRunAttributes(getAttributesRequest);

    LOGGER.info("GetAttributes Response : " + response.getAttributesCount());
    for (KeyValue attributeKeyValue : response.getAttributesList()) {
      assertTrue(
          "Experiment attribute not match with expected attribute",
          keys.contains(attributeKeyValue.getKey()));
    }

    getAttributesRequest =
        GetAttributes.newBuilder().setId(experimentRun.getId()).setGetAll(true).build();

    response = experimentRunServiceStub.getExperimentRunAttributes(getAttributesRequest);

    LOGGER.info("GetAttributes Response : " + response.getAttributesCount());
    assertEquals(attributes.size(), response.getAttributesList().size());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get Attributes from ExperimentRun tags test stop................................");
  }

  @Test
  public void r_getExperimentRunAttributesNegativeTest() {
    LOGGER.info(
        "Get Attributes from ExperimentRun Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    GetAttributes getAttributesRequest = GetAttributes.newBuilder().build();

    try {
      experimentRunServiceStub.getExperimentRunAttributes(getAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getAttributesRequest = GetAttributes.newBuilder().setId("dssdds").setGetAll(true).build();

    try {
      experimentRunServiceStub.getExperimentRunAttributes(getAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info(
        "Get Attributes from ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void rrr_deleteExperimentRunAttributes() {
    LOGGER.info("Delete ExperimentRun Attributes test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    List<KeyValue> attributes = experimentRun.getAttributesList();
    LOGGER.info("Attributes size : " + attributes.size());
    List<String> keys = new ArrayList<>();
    for (int index = 0; index < attributes.size() - 1; index++) {
      KeyValue keyValue = attributes.get(index);
      keys.add(keyValue.getKey());
    }
    LOGGER.info("Attributes key size : " + keys.size());

    DeleteExperimentRunAttributes request =
        DeleteExperimentRunAttributes.newBuilder()
            .setId(experimentRun.getId())
            .addAllAttributeKeys(keys)
            .build();

    DeleteExperimentRunAttributes.Response response =
        experimentRunServiceStub.deleteExperimentRunAttributes(request);
    LOGGER.info(
        "DeleteExperimentRunAttributes Response : \n"
            + response.getExperimentRun().getAttributesList());
    assertTrue(response.getExperimentRun().getAttributesList().size() <= 1);

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    if (response.getExperimentRun().getAttributesList().size() != 0) {
      request =
          DeleteExperimentRunAttributes.newBuilder()
              .setId(experimentRun.getId())
              .setDeleteAll(true)
              .build();

      response = experimentRunServiceStub.deleteExperimentRunAttributes(request);
      LOGGER.info(
          "DeleteExperimentRunAttributes Response : \n"
              + response.getExperimentRun().getAttributesList());
      assertEquals(0, response.getExperimentRun().getAttributesList().size());

      assertNotEquals(
          "ExperimentRun date_updated field not update on database",
          experimentRun.getDateUpdated(),
          response.getExperimentRun().getDateUpdated());
      experimentRun = response.getExperimentRun();

      getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
      getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
      assertNotEquals(
          "Experiment date_updated field not update on database",
          experiment.getDateUpdated(),
          getExperimentByIdResponse.getExperiment().getDateUpdated());

      getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
      getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
      assertNotEquals(
          "Project date_updated field not update on database",
          project.getDateUpdated(),
          getProjectByIdResponse.getProject().getDateUpdated());
      project = getProjectByIdResponse.getProject();
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Delete ExperimentRun Attributes test stop................................");
  }

  @Test
  public void rrr_deleteExperimentRunAttributesNegativeTest() {
    LOGGER.info(
        "Delete ExperimentRun Attributes Negative test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    DeleteExperimentRunAttributes request = DeleteExperimentRunAttributes.newBuilder().build();

    try {
      experimentRunServiceStub.deleteExperimentRunAttributes(request);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    request =
        DeleteExperimentRunAttributes.newBuilder()
            .setId(experimentRun.getId())
            .setDeleteAll(true)
            .build();

    DeleteExperimentRunAttributes.Response response =
        experimentRunServiceStub.deleteExperimentRunAttributes(request);
    LOGGER.info(
        "DeleteExperimentRunAttributes Response : \n"
            + response.getExperimentRun().getAttributesList());
    assertEquals(0, response.getExperimentRun().getAttributesList().size());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info(
        "Delete ExperimentRun Attributes Negative test stop................................");
  }

  @Test
  public void t_sortExperimentRunsTest() {
    LOGGER.info("SortExperimentRuns test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment1.getId(), "ExperimentRun_sprt_1");
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
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun11 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun11.getName());

    createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment1.getId(), "ExperimentRun_sprt_2");
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
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun12 = createExperimentRunResponse.getExperimentRun();
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
        getCreateExperimentRunRequest(project.getId(), experiment2.getId(), "ExperimentRun_sprt_2");
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
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun21 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun21.getName());

    createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment2.getId(), "ExperimentRun_sprt_1");
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
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun22 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun22.getName());

    List<String> experimentRunIds = new ArrayList<>();
    experimentRunIds.add(experimentRun11.getId());
    experimentRunIds.add(experimentRun12.getId());
    experimentRunIds.add(experimentRun21.getId());
    experimentRunIds.add(experimentRun22.getId());

    SortExperimentRuns sortExperimentRuns =
        SortExperimentRuns.newBuilder()
            .addAllExperimentRunIds(experimentRunIds)
            .setSortKey("metrics.accuracy")
            .setAscending(true)
            .build();

    SortExperimentRuns.Response response =
        experimentRunServiceStub.sortExperimentRuns(sortExperimentRuns);
    LOGGER.info("SortExperimentRuns Response : " + response.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        4,
        response.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun22,
        response.getExperimentRunsList().get(0));
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun11,
        response.getExperimentRunsList().get(3));

    try {
      sortExperimentRuns =
          SortExperimentRuns.newBuilder()
              .addAllExperimentRunIds(experimentRunIds)
              .setSortKey("observations.attribute.attr_1")
              .setAscending(true)
              .build();

      experimentRunServiceStub.sortExperimentRuns(sortExperimentRuns);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    sortExperimentRuns =
        SortExperimentRuns.newBuilder()
            .addAllExperimentRunIds(experimentRunIds)
            .setSortKey("metrics.accuracy")
            .setAscending(true)
            .setIdsOnly(true)
            .build();

    response = experimentRunServiceStub.sortExperimentRuns(sortExperimentRuns);
    LOGGER.info("SortExperimentRuns Response : " + response.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        4,
        response.getExperimentRunsCount());

    for (int index = 0; index < response.getExperimentRunsCount(); index++) {
      ExperimentRun experimentRun = response.getExperimentRunsList().get(index);
      if (index == 0) {
        assertNotEquals(
            "ExperimentRun not match with expected experimentRun", experimentRun22, experimentRun);
        assertEquals(
            "ExperimentRun Id not match with expected experimentRun Id",
            experimentRun22.getId(),
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
            "ExperimentRun not match with expected experimentRun", experimentRun21, experimentRun);
        assertEquals(
            "ExperimentRun Id not match with expected experimentRun Id",
            experimentRun21.getId(),
            experimentRun.getId());
      } else if (index == 3) {
        assertNotEquals(
            "ExperimentRun not match with expected experimentRun", experimentRun11, experimentRun);
        assertEquals(
            "ExperimentRun Id not match with expected experimentRun Id",
            experimentRun11.getId(),
            experimentRun.getId());
      }
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("SortExperimentRuns test stop................................");
  }

  @Test
  public void t_sortExperimentRunsNegativeTest() {
    LOGGER.info("SortExperimentRuns Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    SortExperimentRuns sortExperimentRuns =
        SortExperimentRuns.newBuilder().setSortKey("end_time").setIdsOnly(true).build();

    try {
      experimentRunServiceStub.sortExperimentRuns(sortExperimentRuns);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      List<String> experimentRunIds = new ArrayList<>();
      experimentRunIds.add("abc");
      experimentRunIds.add("xyz");
      sortExperimentRuns =
          SortExperimentRuns.newBuilder()
              .addAllExperimentRunIds(experimentRunIds)
              .setSortKey("end_time")
              .build();

      experimentRunServiceStub.sortExperimentRuns(sortExperimentRuns);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    LOGGER.info("SortExperimentRuns Negative test stop................................");
  }

  @Test
  public void u_getTopExperimentRunsTest() {
    LOGGER.info("TopExperimentRuns test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment1.getId(), "ExperimentRun_sprt_1");
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
            .setCodeVersion("1")
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun11 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun11.getName());

    createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment1.getId(), "ExperimentRun_sprt_2");
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
            .setCodeVersion("2")
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun12 = createExperimentRunResponse.getExperimentRun();
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
        getCreateExperimentRunRequest(project.getId(), experiment2.getId(), "ExperimentRun_sprt_2");
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
            .setCodeVersion("3")
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun21 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun21.getName());

    createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment2.getId(), "ExperimentRun_sprt_1");
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
            .setCodeVersion("4")
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun22 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun22.getName());

    List<String> experimentRunIds = new ArrayList<>();
    experimentRunIds.add(experimentRun11.getId());
    experimentRunIds.add(experimentRun12.getId());
    experimentRunIds.add(experimentRun21.getId());
    experimentRunIds.add(experimentRun22.getId());

    TopExperimentRunsSelector topExperimentRunsSelector =
        TopExperimentRunsSelector.newBuilder()
            .setProjectId(project.getId())
            .setSortKey("metrics.accuracy")
            .setTopK(3)
            .setAscending(true)
            .build();

    TopExperimentRunsSelector.Response response =
        experimentRunServiceStub.getTopExperimentRuns(topExperimentRunsSelector);
    LOGGER.info("TopExperimentRunsSelector Response : " + response.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        3,
        response.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun22,
        response.getExperimentRunsList().get(0));
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun21,
        response.getExperimentRunsList().get(2));

    topExperimentRunsSelector =
        TopExperimentRunsSelector.newBuilder()
            .setProjectId(project.getId())
            .setExperimentId(experiment1.getId())
            .setSortKey("hyperparameters.tuning")
            .setTopK(20000)
            .setAscending(true)
            .setIdsOnly(true)
            .build();

    response = experimentRunServiceStub.getTopExperimentRuns(topExperimentRunsSelector);
    LOGGER.info("TopExperimentRunsSelector Response : " + response.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        2,
        response.getExperimentRunsCount());
    assertNotEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun12,
        response.getExperimentRunsList().get(0));
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun12.getId(),
        response.getExperimentRunsList().get(0).getId());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun11.getId(),
        response.getExperimentRunsList().get(1).getId());

    topExperimentRunsSelector =
        TopExperimentRunsSelector.newBuilder()
            .addAllExperimentRunIds(experimentRunIds)
            .setSortKey("code_version")
            .setTopK(3)
            .setIdsOnly(true)
            .build();

    response = experimentRunServiceStub.getTopExperimentRuns(topExperimentRunsSelector);
    LOGGER.info("TopExperimentRunsSelector Response : " + response.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun count not match with expected experimentRun count",
        3,
        response.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun22.getId(),
        response.getExperimentRunsList().get(0).getId());
    assertNotEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun12,
        response.getExperimentRunsList().get(2));
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        experimentRun12.getId(),
        response.getExperimentRunsList().get(2).getId());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("TopExperimentRuns test stop................................");
  }

  @Test
  public void u_getTopExperimentRunsNegativeTest() {
    LOGGER.info("TopExperimentRuns Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    TopExperimentRunsSelector topExperimentRunsSelector =
        TopExperimentRunsSelector.newBuilder().setTopK(4).setAscending(true).build();

    try {
      experimentRunServiceStub.getTopExperimentRuns(topExperimentRunsSelector);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      topExperimentRunsSelector =
          TopExperimentRunsSelector.newBuilder()
              .setProjectId("12321")
              .setSortKey("endTime")
              .build();
      experimentRunServiceStub.getTopExperimentRuns(topExperimentRunsSelector);
      fail();
    } catch (StatusRuntimeException ex) {
      checkEqualsAssert(ex);
    }

    topExperimentRunsSelector =
        TopExperimentRunsSelector.newBuilder()
            .setSortKey("endTime")
            .setTopK(4)
            .setAscending(true)
            .build();

    try {
      experimentRunServiceStub.getTopExperimentRuns(topExperimentRunsSelector);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      List<String> experimentRunIds = new ArrayList<>();
      experimentRunIds.add("abc");
      experimentRunIds.add("xyz");
      topExperimentRunsSelector =
          TopExperimentRunsSelector.newBuilder()
              .addAllExperimentRunIds(experimentRunIds)
              .setSortKey("end_time")
              .setTopK(3)
              .build();

      experimentRunServiceStub.getTopExperimentRuns(topExperimentRunsSelector);
      fail();
    } catch (StatusRuntimeException exce) {
      Status status = Status.fromThrowable(exce);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    }

    LOGGER.info("TopExperimentRuns Negative test stop................................");
  }

  @Test
  public void v_logJobIdTest() {
    LOGGER.info(" Log Job Id in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    String jobId = "xyz";
    LogJobId logJobIdRequest =
        LogJobId.newBuilder().setId(experimentRun.getId()).setJobId(jobId).build();

    LogJobId.Response response = experimentRunServiceStub.logJobId(logJobIdRequest);

    LOGGER.info("LogJobId Response : \n" + response.getExperimentRun());
    assertEquals(
        "Job Id not match with expected job Id", jobId, response.getExperimentRun().getJobId());

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());
    experimentRun = response.getExperimentRun();

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Log Job Id in ExperimentRun test stop................................");
  }

  @Test
  public void v_logJobIdNegativeTest() {
    LOGGER.info(" Log Job Id in ExperimentRun Negative test start................................");

    String jobId = "xyz";
    LogJobId logJobIdRequest = LogJobId.newBuilder().setJobId(jobId).build();

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    try {

      experimentRunServiceStub.logJobId(logJobIdRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      logJobIdRequest = LogJobId.newBuilder().setId("abc").build();
      experimentRunServiceStub.logJobId(logJobIdRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Log Job Id in ExperimentRun Negative test stop................................");
  }

  @Test
  public void w_getJobIdTest() {
    LOGGER.info(" Get Job Id in ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    String jobId = "xyz";
    LogJobId logJobIdRequest =
        LogJobId.newBuilder().setId(experimentRun.getId()).setJobId(jobId).build();

    LogJobId.Response logJobIdResponse = experimentRunServiceStub.logJobId(logJobIdRequest);
    LOGGER.info("LogJobId Response : \n" + logJobIdResponse.getExperimentRun());
    assertEquals(
        "Job Id not match with expected job Id",
        jobId,
        logJobIdResponse.getExperimentRun().getJobId());

    GetJobId getJobIdRequest = GetJobId.newBuilder().setId(experimentRun.getId()).build();

    GetJobId.Response response = experimentRunServiceStub.getJobId(getJobIdRequest);

    LOGGER.info("GetJobId Response : \n" + response.getJobId());
    assertEquals("Job Id not match with expected job Id", "xyz", response.getJobId());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get Job Id in ExperimentRun test stop................................");
  }

  @Test
  public void w_getJobIdNegativeTest() {
    LOGGER.info(" Get Job Id in ExperimentRun Negative test start................................");

    GetJobId getJobIdRequest = GetJobId.newBuilder().build();

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    try {
      experimentRunServiceStub.getJobId(getJobIdRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getJobIdRequest = GetJobId.newBuilder().setId("dssdds").build();

    try {
      experimentRunServiceStub.getJobId(getJobIdRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertTrue(
          Status.NOT_FOUND.getCode().equals(status.getCode())
              || Status.UNAUTHENTICATED.getCode().equals(status.getCode()));
    }

    LOGGER.info("Get Job Id in ExperimentRun Negative test stop................................");
  }

  /*@Test
  public void x_getURLForArtifact() {
    LOGGER.info("Get Url for Artifact test start................................");
    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    // Create project
    CreateProject createProjectRequest =
        projectTest.getCreateProjectRequest("experimentRun_project_ypcdt11");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_zys");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_zys");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    Artifact artifact = experimentRun.getArtifacts(0);

    GetUrlForArtifact getUrlForArtifact =
        GetUrlForArtifact.newBuilder()
            .setId(experiment.getId())
            .setKey(artifact.getKey())
            .setMethod("put")
            .build();
    GetUrlForArtifact.Response getUrlForArtifactResponse =
        experimentRunServiceStub.getUrlForArtifact(getUrlForArtifact);
    String url = getUrlForArtifactResponse.getUrl();
    assertEquals("Artifact Url not match with expected artifact Url", url, url);

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get Url for Artifact test stop................................");
  }*/

  @Test
  public void z_deleteExperimentRunTest() {
    LOGGER.info("Delete ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteExperimentRun deleteExperimentRun =
        DeleteExperimentRun.newBuilder().setId(experimentRun.getId()).build();
    DeleteExperimentRun.Response deleteExperimentRunResponse =
        experimentRunServiceStub.deleteExperimentRun(deleteExperimentRun);
    assertTrue(deleteExperimentRunResponse.getStatus());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Delete ExperimentRun test stop................................");
  }

  @Test
  public void z_deleteExperimentRunNegativeTest() {
    LOGGER.info("Delete ExperimentRun Negative test start................................");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    DeleteExperimentRun request = DeleteExperimentRun.newBuilder().build();

    try {
      experimentRunServiceStub.deleteExperimentRun(request);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    request = DeleteExperimentRun.newBuilder().setId("ddsdfds").build();

    try {
      experimentRunServiceStub.deleteExperimentRun(request);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertTrue(Status.PERMISSION_DENIED.getCode().equals(status.getCode()));
    }

    LOGGER.info("Delete ExperimentRun Negative test stop................................");
  }

  @Test
  public void createParentChildExperimentRunTest() {
    LOGGER.info("Create Parent Children ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun_sprt_parent_1");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    // Children experimentRun 1
    createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun_sprt_children_1");
    // Add Parent Id to children
    createExperimentRunRequest =
        createExperimentRunRequest.toBuilder().setParentId(experimentRun.getId()).build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun childrenExperimentRun1 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun1 created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        childrenExperimentRun1.getName());

    // Children experimentRun 2
    createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun_sprt_children_2");
    // Add Parent Id to children
    createExperimentRunRequest =
        createExperimentRunRequest.toBuilder().setParentId(experimentRun.getId()).build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun childrenExperimentRun2 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun1 created successfully");
    assertEquals(
        "ExperimentRun2 name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        childrenExperimentRun2.getName());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Create Parent Children ExperimentRun test stop................................");
  }

  @Test
  public void getChildExperimentRunWithPaginationTest() {
    LOGGER.info(
        "Get Children ExperimentRun using pagination test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    // Create parent experimentRun
    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun_sprt_parent_1");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun parentExperimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        parentExperimentRun.getName());

    Map<String, ExperimentRun> childrenExperimentRunMap = new HashMap<>();

    // Children experimentRun 1
    createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun_sprt_children_1");
    // Add Parent Id to children
    KeyValue metric1 =
        KeyValue.newBuilder()
            .setKey("loss")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    KeyValue metric2 =
        KeyValue.newBuilder()
            .setKey("accuracy")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    KeyValue hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("tuning")
            .setValue(Value.newBuilder().setNumberValue(7).build())
            .build();
    createExperimentRunRequest =
        createExperimentRunRequest
            .toBuilder()
            .setParentId(parentExperimentRun.getId())
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .setDateCreated(123456)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun childrenExperimentRun1 = createExperimentRunResponse.getExperimentRun();
    childrenExperimentRunMap.put(childrenExperimentRun1.getId(), childrenExperimentRun1);
    LOGGER.info("ExperimentRun1 created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        childrenExperimentRun1.getName());

    // Children experimentRun 2
    createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun_sprt_children_2");
    // Add Parent Id to children
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
            .setParentId(parentExperimentRun.getId())
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun childrenExperimentRun2 = createExperimentRunResponse.getExperimentRun();
    childrenExperimentRunMap.put(childrenExperimentRun2.getId(), childrenExperimentRun2);
    LOGGER.info("ExperimentRun1 created successfully");
    assertEquals(
        "ExperimentRun2 name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        childrenExperimentRun2.getName());

    int pageLimit = 1;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      GetChildrenExperimentRuns getChildrenExperimentRunsRequest =
          GetChildrenExperimentRuns.newBuilder()
              .setExperimentRunId(parentExperimentRun.getId())
              .setPageNumber(pageNumber)
              .setPageLimit(pageLimit)
              .setAscending(true)
              .setSortKey(ModelDBConstants.NAME)
              .build();

      GetChildrenExperimentRuns.Response experimentRunResponse =
          experimentRunServiceStub.getChildrenExperimentRuns(getChildrenExperimentRunsRequest);
      assertEquals(
          "Total records count not matched with expected records count",
          2,
          experimentRunResponse.getTotalRecords());

      if (experimentRunResponse.getExperimentRunsList() != null) {
        isExpectedResultFound = true;
        for (ExperimentRun experimentRun : experimentRunResponse.getExperimentRunsList()) {
          assertEquals(
              "ExperimentRun not match with expected experimentRun",
              childrenExperimentRunMap.get(experimentRun.getId()),
              experimentRun);
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More ExperimentRun not found in database");
          assertTrue(true);
        } else {
          fail("Expected experimentRun not found in response");
        }
      }
    }

    GetChildrenExperimentRuns getChildrenExperimentRunRequest =
        GetChildrenExperimentRuns.newBuilder()
            .setExperimentRunId(parentExperimentRun.getId())
            .setPageNumber(1)
            .setPageLimit(1)
            .setAscending(false)
            .setSortKey("metrics.loss")
            .build();

    GetChildrenExperimentRuns.Response experimentRunResponse =
        experimentRunServiceStub.getChildrenExperimentRuns(getChildrenExperimentRunRequest);
    assertEquals(
        "Total records count not matched with expected records count",
        2,
        experimentRunResponse.getTotalRecords());
    assertEquals(
        "ExperimentRuns count not match with expected experimentRuns count",
        1,
        experimentRunResponse.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        childrenExperimentRun2,
        experimentRunResponse.getExperimentRuns(0));

    getChildrenExperimentRunRequest =
        GetChildrenExperimentRuns.newBuilder()
            .setExperimentRunId(parentExperimentRun.getId())
            .setPageNumber(1)
            .setPageLimit(1)
            .setAscending(true)
            .setSortKey("")
            .build();

    experimentRunResponse =
        experimentRunServiceStub.getChildrenExperimentRuns(getChildrenExperimentRunRequest);
    assertEquals(
        "ExperimentRuns count not match with expected experimentRuns count",
        1,
        experimentRunResponse.getExperimentRunsCount());
    assertEquals(
        "ExperimentRun not match with expected experimentRun",
        childrenExperimentRun1,
        experimentRunResponse.getExperimentRuns(0));
    assertEquals(
        "Total records count not matched with expected records count",
        2,
        experimentRunResponse.getTotalRecords());

    getChildrenExperimentRunRequest =
        GetChildrenExperimentRuns.newBuilder()
            .setExperimentRunId(parentExperimentRun.getId())
            .setPageNumber(1)
            .setPageLimit(1)
            .setAscending(true)
            .setSortKey("observations.attribute.attr_1")
            .build();

    try {
      experimentRunServiceStub.getChildrenExperimentRuns(getChildrenExperimentRunRequest);
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
        "Get Children ExperimentRun using pagination test stop................................");
  }

  @Test
  public void setParentIdOnChildExperimentRunTest() {
    LOGGER.info(
        "Set Parent ID on Children ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun_sprt_parent_1");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    // Children experimentRun 1
    createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun_sprt_children_1");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun childrenExperimentRun1 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun1 created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        childrenExperimentRun1.getName());

    SetParentExperimentRunId setParentExperimentRunIdRequest =
        SetParentExperimentRunId.newBuilder()
            .setExperimentRunId(childrenExperimentRun1.getId())
            .setParentId(experimentRun.getId())
            .build();
    SetParentExperimentRunId.Response setParentExperimentRunIdResponse =
        experimentRunServiceStub.setParentExperimentRunId(setParentExperimentRunIdRequest);
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        childrenExperimentRun1.getName(),
        setParentExperimentRunIdResponse.getExperimentRun().getName());
    assertEquals(
        "ExperimentRun parent ID not match with expected ExperimentRun parent ID",
        experimentRun.getId(),
        setParentExperimentRunIdResponse.getExperimentRun().getParentId());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info(
        "Set Parent ID on Children ExperimentRun test stop................................");
  }

  @Test
  public void logExperimentRunCodeVersionTest() {
    LOGGER.info("Log ExperimentRun code version test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());
    experiment = getExperimentByIdResponse.getExperiment();

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    LogExperimentRunCodeVersion logExperimentRunCodeVersionRequest =
        LogExperimentRunCodeVersion.newBuilder()
            .setId(experimentRun.getId())
            .setCodeVersion(
                CodeVersion.newBuilder()
                    .setCodeArchive(
                        Artifact.newBuilder()
                            .setKey("code_version_image")
                            .setPath("https://xyz_path_string.com/image.png")
                            .setArtifactType(ArtifactTypeEnum.ArtifactType.CODE)
                            .build())
                    .build())
            .build();
    LogExperimentRunCodeVersion.Response logExperimentRunCodeVersionResponse =
        experimentRunServiceStub.logExperimentRunCodeVersion(logExperimentRunCodeVersionRequest);
    CodeVersion codeVersion =
        logExperimentRunCodeVersionResponse.getExperimentRun().getCodeVersionSnapshot();
    assertEquals(
        "ExperimentRun codeVersion not match with expected ExperimentRun codeVersion",
        logExperimentRunCodeVersionRequest.getCodeVersion(),
        codeVersion);

    try {
      experimentRunServiceStub.logExperimentRunCodeVersion(logExperimentRunCodeVersionRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    logExperimentRunCodeVersionRequest =
        logExperimentRunCodeVersionRequest
            .toBuilder()
            .setCodeVersion(
                CodeVersion.newBuilder()
                    .setCodeArchive(
                        Artifact.newBuilder()
                            .setKey("code_version_image_1")
                            .setPath("https://xyz_path_string.com/image.png")
                            .setArtifactType(ArtifactType.IMAGE)
                            .build())
                    .build())
            .setOverwrite(true)
            .build();
    logExperimentRunCodeVersionResponse =
        experimentRunServiceStub.logExperimentRunCodeVersion(logExperimentRunCodeVersionRequest);
    codeVersion = logExperimentRunCodeVersionResponse.getExperimentRun().getCodeVersionSnapshot();
    assertEquals(
        "ExperimentRun codeVersion not match with expected ExperimentRun codeVersion",
        logExperimentRunCodeVersionRequest.getCodeVersion(),
        codeVersion);

    logExperimentRunCodeVersionRequest =
        LogExperimentRunCodeVersion.newBuilder()
            .setId(experimentRun.getId())
            .setCodeVersion(
                CodeVersion.newBuilder()
                    .setGitSnapshot(
                        GitSnapshot.newBuilder()
                            .setHash("code_version_image_1_hash")
                            .setRepo("code_version_image_1_repo")
                            .addFilepaths("https://xyz_path_string.com/image.png")
                            .setIsDirty(Ternary.TRUE)
                            .build())
                    .build())
            .setOverwrite(true)
            .build();
    logExperimentRunCodeVersionResponse =
        experimentRunServiceStub.logExperimentRunCodeVersion(logExperimentRunCodeVersionRequest);
    codeVersion = logExperimentRunCodeVersionResponse.getExperimentRun().getCodeVersionSnapshot();
    assertEquals(
        "ExperimentRun codeVersion not match with expected ExperimentRun codeVersion",
        logExperimentRunCodeVersionRequest.getCodeVersion(),
        codeVersion);

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Log ExperimentRun code version test stop................................");
  }

  @Test
  public void getExperimentRunCodeVersionTest() {
    LOGGER.info("Get ExperimentRun code version test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());
    experiment = getExperimentByIdResponse.getExperiment();

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    LogExperimentRunCodeVersion logExperimentRunCodeVersionRequest =
        LogExperimentRunCodeVersion.newBuilder()
            .setId(experimentRun.getId())
            .setCodeVersion(
                CodeVersion.newBuilder()
                    .setCodeArchive(
                        Artifact.newBuilder()
                            .setKey("code_version_image")
                            .setPath("https://xyz_path_string.com/image.png")
                            .setArtifactType(ArtifactTypeEnum.ArtifactType.CODE)
                            .build())
                    .build())
            .build();
    LogExperimentRunCodeVersion.Response logExperimentRunCodeVersionResponse =
        experimentRunServiceStub.logExperimentRunCodeVersion(logExperimentRunCodeVersionRequest);
    CodeVersion codeVersion =
        logExperimentRunCodeVersionResponse.getExperimentRun().getCodeVersionSnapshot();
    assertEquals(
        "ExperimentRun codeVersion not match with expected ExperimentRun codeVersion",
        logExperimentRunCodeVersionRequest.getCodeVersion(),
        codeVersion);

    GetExperimentRunCodeVersion getExperimentRunCodeVersionRequest =
        GetExperimentRunCodeVersion.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunCodeVersion.Response getExperimentRunCodeVersionResponse =
        experimentRunServiceStub.getExperimentRunCodeVersion(getExperimentRunCodeVersionRequest);
    assertEquals(
        "ExperimentRun codeVersion not match with expected experimentRun codeVersion",
        codeVersion,
        getExperimentRunCodeVersionResponse.getCodeVersion());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get ExperimentRun code version test stop................................");
  }

  @Test
  public void deleteExperimentRunArtifacts() {
    LOGGER.info("Delete ExperimentRun Artifacts test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    GetProjectById getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    GetProjectById.Response getProjectByIdResponse =
        projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    List<Artifact> artifacts = experimentRun.getArtifactsList();
    LOGGER.info("Artifacts size : " + artifacts.size());
    if (artifacts.isEmpty()) {
      fail("Artifacts not found");
    }

    DeleteArtifact request =
        DeleteArtifact.newBuilder()
            .setId(experimentRun.getId())
            .setKey(artifacts.get(0).getKey())
            .build();

    DeleteArtifact.Response response = experimentRunServiceStub.deleteArtifact(request);
    LOGGER.info(
        "DeleteExperimentRunArtifacts Response : \n"
            + response.getExperimentRun().getArtifactsList());
    assertFalse(response.getExperimentRun().getArtifactsList().contains(artifacts.get(0)));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());

    getExperimentById = GetExperimentById.newBuilder().setId(experiment.getId()).build();
    getExperimentByIdResponse = experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    getProjectById = GetProjectById.newBuilder().setId(project.getId()).build();
    getProjectByIdResponse = projectServiceStub.getProjectById(getProjectById);
    assertNotEquals(
        "Project date_updated field not update on database",
        project.getDateUpdated(),
        getProjectByIdResponse.getProject().getDateUpdated());
    project = getProjectByIdResponse.getProject();

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Delete ExperimentRun Artifacts test stop................................");
  }

  @Test
  public void batchDeleteExperimentRunTest() {
    LOGGER.info("Batch Delete ExperimentRun test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

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
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());

    List<String> experimentRunIds = new ArrayList<>();
    for (int count = 0; count < 5; count++) {
      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project.getId(), experiment.getId(), "ExperimentRun_n_sprt_" + count);
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(experimentRun.getId());
      LOGGER.info("ExperimentRun created successfully");
      assertEquals(
          "ExperimentRun name not match with expected ExperimentRun name",
          createExperimentRunRequest.getName(),
          experimentRun.getName());
    }

    GetExperimentById getExperimentById =
        GetExperimentById.newBuilder().setId(experiment.getId()).build();
    GetExperimentById.Response getExperimentByIdResponse =
        experimentServiceStub.getExperimentById(getExperimentById);
    assertNotEquals(
        "Experiment date_updated field not update on database",
        experiment.getDateUpdated(),
        getExperimentByIdResponse.getExperiment().getDateUpdated());

    DeleteExperimentRuns deleteExperimentRuns =
        DeleteExperimentRuns.newBuilder().addAllIds(experimentRunIds).build();
    DeleteExperimentRuns.Response deleteExperimentRunsResponse =
        experimentRunServiceStub.deleteExperimentRuns(deleteExperimentRuns);
    assertTrue(deleteExperimentRunsResponse.getStatus());

    GetExperimentRunsInExperiment getExperimentRunsInExperiment =
        GetExperimentRunsInExperiment.newBuilder().setExperimentId(experiment.getId()).build();

    GetExperimentRunsInExperiment.Response experimentRunResponse =
        experimentRunServiceStub.getExperimentRunsInExperiment(getExperimentRunsInExperiment);
    assertEquals(
        "ExperimentRuns count not match with expected experimentRun count",
        0,
        experimentRunResponse.getExperimentRunsCount());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Batch Delete ExperimentRun test stop................................");
  }

  @Test
  public void deleteExperimentRunByParentEntitiesOwnerTest() {
    LOGGER.info(
        "Delete ExperimentRun by parent entities owner test start.........................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(channel);

    ExperimentRunServiceBlockingStub experimentRunServiceStubClient2 =
        ExperimentRunServiceGrpc.newBlockingStub(client2Channel);

    // Create project
    CreateProject createProjectRequest =
        projectTest.getCreateProjectRequest("experimentRun_project_ypcdt1");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      AddCollaboratorRequest addCollaboratorRequest =
          addCollaboratorRequestProjectInterceptor(
              project, CollaboratorType.READ_ONLY, authClientInterceptor);

      AddCollaboratorRequest.Response addCollaboratorResponse =
          collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
      assertTrue(addCollaboratorResponse.getStatus());
    }

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");

    List<String> experimentRunIds = new ArrayList<>();
    for (int count = 0; count < 5; count++) {
      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project.getId(), experiment.getId(), "ExperimentRun_n_sprt_" + count);
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(experimentRun.getId());
      LOGGER.info("ExperimentRun created successfully");
    }

    DeleteExperimentRuns deleteExperimentRuns =
        DeleteExperimentRuns.newBuilder()
            .addAllIds(experimentRunIds.subList(0, experimentRunIds.size() - 1))
            .build();

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      try {
        experimentRunServiceStubClient2.deleteExperimentRuns(deleteExperimentRuns);
      } catch (StatusRuntimeException e) {
        checkEqualsAssert(e);
      }

      AddCollaboratorRequest addCollaboratorRequest =
          addCollaboratorRequestProjectInterceptor(
              project, CollaboratorType.READ_WRITE, authClientInterceptor);

      AddCollaboratorRequest.Response addCollaboratorResponse =
          collaboratorServiceStub.addOrUpdateProjectCollaborator(addCollaboratorRequest);
      LOGGER.info("Collaborator updated in server : " + addCollaboratorResponse.getStatus());
      assertTrue(addCollaboratorResponse.getStatus());

      DeleteExperimentRuns.Response deleteExperimentRunsResponse =
          experimentRunServiceStubClient2.deleteExperimentRuns(deleteExperimentRuns);
      assertTrue(deleteExperimentRunsResponse.getStatus());

      DeleteExperimentRun deleteExperimentRun =
          DeleteExperimentRun.newBuilder().setId(experimentRunIds.get(4)).build();

      DeleteExperimentRun.Response deleteExperimentRunResponse =
          experimentRunServiceStubClient2.deleteExperimentRun(deleteExperimentRun);
      assertTrue(deleteExperimentRunResponse.getStatus());

    } else {
      deleteExperimentRuns = DeleteExperimentRuns.newBuilder().addAllIds(experimentRunIds).build();

      DeleteExperimentRuns.Response deleteExperimentRunResponse =
          experimentRunServiceStub.deleteExperimentRuns(deleteExperimentRuns);
      assertTrue(deleteExperimentRunResponse.getStatus());
    }

    GetExperimentRunsInExperiment getExperimentRunsInExperiment =
        GetExperimentRunsInExperiment.newBuilder().setExperimentId(experiment.getId()).build();

    GetExperimentRunsInExperiment.Response experimentRunResponse =
        experimentRunServiceStub.getExperimentRunsInExperiment(getExperimentRunsInExperiment);
    assertEquals(
        "ExperimentRuns count not match with expected experimentRun count",
        0,
        experimentRunResponse.getExperimentRunsCount());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info(
        "Delete ExperimentRun by parent entities owner test stop................................");
  }
}
