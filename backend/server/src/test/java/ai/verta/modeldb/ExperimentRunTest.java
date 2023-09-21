package ai.verta.modeldb;

import static ai.verta.modeldb.CollaboratorUtils.addCollaboratorRequestProjectInterceptor;
import static ai.verta.modeldb.RepositoryTest.createRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import ai.verta.common.*;
import ai.verta.common.ArtifactTypeEnum.ArtifactType;
import ai.verta.common.CollaboratorTypeEnum.CollaboratorType;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum.Operator;
import ai.verta.common.TernaryEnum.Ternary;
import ai.verta.common.ValueTypeEnum.ValueType;
import ai.verta.modeldb.GetExperimentRunById.Response;
import ai.verta.modeldb.common.config.ArtifactStoreConfig;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.metadata.GenerateRandomNameRequest;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.CodeBlob;
import ai.verta.modeldb.versioning.Commit;
import ai.verta.modeldb.versioning.CreateCommitRequest;
import ai.verta.modeldb.versioning.DeleteCommitRequest;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.GetBranchRequest;
import ai.verta.modeldb.versioning.GitCodeBlob;
import ai.verta.modeldb.versioning.PythonEnvironmentBlob;
import ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.VersionEnvironmentBlob;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ServiceEnum.Service;
import com.google.common.util.concurrent.Futures;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.protobuf.Value.KindCase;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = App.class, webEnvironment = DEFINED_PORT)
@ContextConfiguration(classes = {ModeldbTestConfigurationBeans.class})
public class ExperimentRunTest extends ModeldbTestSetup {

  private static final Logger LOGGER = LogManager.getLogger(ExperimentRunTest.class);

  // Project Entities
  private Project project;
  private Project project2;
  private Project project3;
  private final Map<String, Project> projectMap = new HashMap<>();

  // Experiment Entities
  private Experiment experiment;
  private Experiment experiment2;

  // ExperimentRun Entities
  private ExperimentRun experimentRun;
  private ExperimentRun experimentRun2;
  private final Map<String, ExperimentRun> experimentRunMap = new HashMap<>();

  @BeforeEach
  @Override
  public void setUp() {
    super.setUp();
    initializeChannelBuilderAndExternalServiceStubs();

    if (isRunningIsolated()) {
      setupMockUacEndpoints(uac);
    }

    // Create all entities
    createProjectEntities();
    createExperimentEntities();
    createExperimentRunEntities();
  }

  @AfterEach
  @Override
  public void tearDown() {
    DeleteExperimentRuns deleteExperimentRun =
        DeleteExperimentRuns.newBuilder().addAllIds(experimentRunMap.keySet()).build();
    DeleteExperimentRuns.Response deleteExperimentRunResponse =
        experimentRunServiceStub.deleteExperimentRuns(deleteExperimentRun);
    assertTrue(deleteExperimentRunResponse.getStatus());

    if (isRunningIsolated()) {
      when(uacBlockingMock.getCurrentUser(any())).thenReturn(testUser1);
      mockGetSelfAllowedResources(
          projectMap.keySet(), ModelDBServiceResourceTypes.PROJECT, ModelDBServiceActions.DELETE);
    }

    DeleteProjects deleteProjects =
        DeleteProjects.newBuilder().addAllIds(projectMap.keySet()).build();
    DeleteProjects.Response deleteProjectsResponse =
        projectServiceStub.deleteProjects(deleteProjects);
    LOGGER.info("Projects deleted successfully");
    LOGGER.info(deleteProjectsResponse.toString());
    assertTrue(deleteProjectsResponse.getStatus());

    project = null;
    project2 = null;
    project3 = null;
    projectMap.clear();

    // Experiment Entities
    experiment = null;
    experiment2 = null;

    // ExperimentRun Entities
    experimentRun = null;
    experimentRun2 = null;

    experimentRunMap.clear();

    cleanUpResources();
    super.tearDown();
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

      if (testConfig.isPermissionV2Enabled()) {
        when(uac.getWorkspaceService().getWorkspaceByName(any()))
            .thenReturn(
                Futures.immediateFuture(
                    Workspace.newBuilder()
                        .setId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                        .setUsername(testUser1.getVertaInfo().getUsername())
                        .build()));
      }
    }

    // Create two project of above project
    CreateProject createProjectRequest =
        ProjectTest.getCreateProjectRequest("project-" + new Date().getTime());
    if (testConfig.isPermissionV2Enabled()) {
      createProjectRequest =
          createProjectRequest.toBuilder().setWorkspaceName(getWorkspaceNameUser1()).build();
    }
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    project = createProjectResponse.getProject();
    projectMap.put(project.getId(), project);
    LOGGER.info("Project created successfully");
    assertEquals(
        createProjectRequest.getName(),
        project.getName(),
        "Project name not match with expected Project name");

    // Create project2
    createProjectRequest = ProjectTest.getCreateProjectRequest("project-" + new Date().getTime());
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    project2 = createProjectResponse.getProject();
    projectMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        createProjectRequest.getName(),
        project2.getName(),
        "Project name not match with expected project name");

    // Create project3
    createProjectRequest = ProjectTest.getCreateProjectRequest("project-" + new Date().getTime());
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    project3 = createProjectResponse.getProject();
    projectMap.put(project3.getId(), project3);
    LOGGER.info("Project created successfully");
    assertEquals(
        createProjectRequest.getName(),
        project3.getName(),
        "Project name not match with expected project name");

    if (isRunningIsolated()) {
      mockGetResourcesForAllProjects(projectMap, testUser1);
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
        createExperimentRequest.toBuilder()
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
        createExperimentRequest.getName(),
        experiment.getName(),
        "Experiment name not match with expected Experiment name");

    // Create two experiment of above project
    createExperimentRequest =
        ExperimentTest.getCreateExperimentRequestForOtherTests(
            project.getId(), "Experiment-" + new Date().getTime());
    createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
    experiment2 = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        createExperimentRequest.getName(),
        experiment2.getName(),
        "Experiment name not match with expected Experiment name");
  }

  private void createExperimentRunEntities() {
    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
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
        createExperimentRunRequest.toBuilder()
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        createExperimentRunRequest.getName(),
        experimentRun.getName(),
        "ExperimentRun name not match with expected ExperimentRun name");
    checkValidArtifactPath(
        experimentRun.getId(), "ExperimentRunEntity", experimentRun.getArtifactsList());

    createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
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
        createExperimentRunRequest.toBuilder()
            .addMetrics(metric1)
            .addMetrics(metric2)
            .addHyperparameters(hyperparameter1)
            .build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun2 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun2.getId(), experimentRun2);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        createExperimentRunRequest.getName(),
        experimentRun2.getName(),
        "ExperimentRun name not match with expected ExperimentRun name");
    checkValidArtifactPath(
        experimentRun2.getId(), "ExperimentRunEntity", experimentRun2.getArtifactsList());
  }

  private void checkEqualsAssert(StatusRuntimeException e) {
    Status status = Status.fromThrowable(e);
    LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
    if (testConfig.hasAuth()) {
      assertTrue(
          Status.PERMISSION_DENIED.getCode() == status.getCode()
              || Status.NOT_FOUND.getCode()
                  == status.getCode()); // because of shadow delete the response could be 403 or 404
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

  public static CreateExperimentRun getCreateExperimentRunRequestForOtherTests(
      String projectId, String experimentId, String experimentRunName) {
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
        .build();
  }

  private CreateExperimentRun getCreateExperimentRunRequest(
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
            .setKey("attribute_1_" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build());
    Value stringValue =
        Value.newBuilder()
            .setStringValue("attributes_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_2_" + Calendar.getInstance().getTimeInMillis())
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
            .setUploadCompleted(
                !testConfig
                    .getArtifactStoreConfig()
                    .getArtifactStoreType()
                    .equals(ArtifactStoreConfig.S3_TYPE_STORE))
            .build());
    artifactList.add(
        Artifact.newBuilder()
            .setKey("Google Pay Artifact")
            .setPath(
                "https://www.google.co.in/imgres?imgurl=https%3A%2F%2Fpay.google.com%2Fabout%2Fstatic%2Fimages%2Fsocial%2Fknowledge_graph_logo.png&imgrefurl=https%3A%2F%2Fpay.google.com%2Fabout%2F&docid=zmoE9BrSKYr4xM&tbnid=eCL1Y6f9xrPtDM%3A&vet=10ahUKEwjr1OiS0ufeAhWNbX0KHXpFAmQQMwhwKAIwAg..i&w=1200&h=630&bih=657&biw=1366&q=google&ved=0ahUKEwjr1OiS0ufeAhWNbX0KHXpFAmQQMwhwKAIwAg&iact=mrc&uact=8")
            .setArtifactType(ArtifactType.IMAGE)
            .setUploadCompleted(
                !testConfig
                    .getArtifactStoreConfig()
                    .getArtifactStoreType()
                    .equals(ArtifactStoreConfig.S3_TYPE_STORE))
            .setFilenameExtension("png")
            .build());

    List<Artifact> datasets = new ArrayList<>();
    datasets.add(
        Artifact.newBuilder()
            .setKey("Google developer datasets")
            .setPath("This is data artifact type in Google developer datasets")
            .setArtifactType(ArtifactType.MODEL)
            .setUploadCompleted(
                !testConfig
                    .getArtifactStoreConfig()
                    .getArtifactStoreType()
                    .equals(ArtifactStoreConfig.S3_TYPE_STORE))
            .setFilenameExtension("pkl")
            .build());
    datasets.add(
        Artifact.newBuilder()
            .setKey("Google Pay datasets")
            .setPath("This is data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactType.DATA)
            .setUploadCompleted(
                !testConfig
                    .getArtifactStoreConfig()
                    .getArtifactStoreType()
                    .equals(ArtifactStoreConfig.S3_TYPE_STORE))
            .setFilenameExtension("json")
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
    // TODO: uncomment after supporting artifact on observation
    /*observations.add(
    Observation.newBuilder()
        .setArtifact(
            Artifact.newBuilder()
                .setKey("Google developer Observation artifact")
                .setPath("This is data artifact type in Google developer Observation artifact")
                .setArtifactType(ArtifactType.DATA))
        .setTimestamp(Calendar.getInstance().getTimeInMillis() + 1)
        .setEpochNumber(Value.newBuilder().setNumberValue(1))
        .build());*/
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
            .setTimestamp(Calendar.getInstance().getTimeInMillis() + 2)
            .setEpochNumber(Value.newBuilder().setNumberValue(123))
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

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), experimentRun.getName());

    try {
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    try {
      if (isRunningIsolated()) {
        when(uac.getAuthzService().isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(false).build()));
      }
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder().setProjectId("xyz").build();
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    try {
      if (isRunningIsolated()) {
        when(uac.getAuthzService().isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(true).build()));
      }
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setProjectId(project.getId())
              .setExperimentId("xyz")
              .build();
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    try {
      String tag52 = "Human Activity Recognition using Smartphone Dataset";
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setProjectId(project.getId())
              .setExperimentId(experiment.getId())
              .addTags(tag52)
              .build();
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    try {
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder().clearTags().addTags("").build();
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      Assertions.fail();
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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Create ExperimentRun test stop................................");
  }

  @Test
  public void a_experimentRunCreateNegativeTest() {
    LOGGER.info("Create ExperimentRun Negative test start................................");

    CreateExperimentRun request = getCreateExperimentRunRequest("", "abcd", "ExperiemntRun_xyz");

    try {
      experimentRunServiceStub.createExperimentRun(request);
      Assertions.fail();
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

    GetExperimentRunsInProject getExperimentRunRequest =
        GetExperimentRunsInProject.newBuilder().setProjectId(project.getId()).build();

    GetExperimentRunsInProject.Response experimentRunResponse =
        experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunRequest);
    assertEquals(
        experimentRunMap.size(),
        experimentRunResponse.getExperimentRunsList().size(),
        "ExperimentRuns count not match with expected experimentRun count");

    for (ExperimentRun experimentRun1 : experimentRunResponse.getExperimentRunsList()) {
      assertEquals(
          experimentRunMap.get(experimentRun1.getId()).getId(),
          experimentRun1.getId(),
          "ExperimentRun not match with expected experimentRun");
    }

    LOGGER.info("Get ExperimentRun from Project test stop................................");
  }

  @Test
  public void b_getExperimentRunWithPaginationFromProjectRunTest() {
    LOGGER.info(
        "Get ExperimentRun using pagination from Project test start................................");

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
          2,
          experimentRunResponse.getTotalRecords(),
          "Total records count not matched with expected records count");

      if (!experimentRunResponse.getExperimentRunsList().isEmpty()) {
        isExpectedResultFound = true;
        for (ExperimentRun experimentRun : experimentRunResponse.getExperimentRunsList()) {
          assertEquals(
              experimentRunMap.get(experimentRun.getId()).getId(),
              experimentRun.getId(),
              "ExperimentRun not match with expected experimentRun");
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More ExperimentRun not found in database");
          assertTrue(true);
        } else {
          Assertions.fail("Expected experimentRun not found in response");
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
        2,
        experimentRunResponse.getTotalRecords(),
        "Total records count not matched with expected records count");
    assertEquals(
        1,
        experimentRunResponse.getExperimentRunsCount(),
        "ExperimentRuns count not match with expected experimentRuns count");
    assertEquals(
        experimentRun2.getId(),
        experimentRunResponse.getExperimentRuns(0).getId(),
        "ExperimentRun not match with expected experimentRun");

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
        1,
        experimentRunResponse.getExperimentRunsCount(),
        "ExperimentRuns count not match with expected experimentRuns count");
    assertEquals(
        experimentRun.getId(),
        experimentRunResponse.getExperimentRuns(0).getId(),
        "ExperimentRun not match with expected experimentRun");

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
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info(
        "Get ExperimentRun using pagination from Project test stop................................");
  }

  @Test
  public void b_getExperimentFromProjectRunNegativeTest() {
    LOGGER.info(
        "Get ExperimentRun from Project Negative test start................................");

    GetExperimentRunsInProject getExperiment = GetExperimentRunsInProject.newBuilder().build();
    try {
      experimentRunServiceStub.getExperimentRunsInProject(getExperiment);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getExperiment = GetExperimentRunsInProject.newBuilder().setProjectId("sdfdsfsd").build();
    GetExperimentRunsInProject.Response response =
        experimentRunServiceStub.getExperimentRunsInProject(getExperiment);
    assertEquals(0, response.getExperimentRunsCount(), "Expected response not found");

    LOGGER.info(
        "Get ExperimentRun from Project Negative test stop................................");
  }

  @Test
  public void bb_getExperimentRunFromExperimentTest() {
    LOGGER.info("Get ExperimentRun from Experiment test start................................");

    GetExperimentRunsInExperiment getExperimentRunsInExperiment =
        GetExperimentRunsInExperiment.newBuilder().setExperimentId(experiment.getId()).build();

    GetExperimentRunsInExperiment.Response experimentRunResponse =
        experimentRunServiceStub.getExperimentRunsInExperiment(getExperimentRunsInExperiment);
    assertEquals(
        experimentRunMap.size(),
        experimentRunResponse.getExperimentRunsList().size(),
        "ExperimentRuns count not match with expected experimentRun count");

    for (ExperimentRun experimentRun1 : experimentRunResponse.getExperimentRunsList()) {
      assertEquals(
          experimentRunMap.get(experimentRun1.getId()).getId(),
          experimentRun1.getId(),
          "ExperimentRun not match with expected experimentRun");
    }

    LOGGER.info("Get ExperimentRun from Experiment test stop................................");
  }

  @Test
  public void bb_getExperimentRunWithPaginationFromExperimentTest() {
    LOGGER.info(
        "Get ExperimentRun using pagination from Experiment test start................................");

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
          2,
          experimentRunResponse.getTotalRecords(),
          "Total records count not matched with expected records count");

      if (!experimentRunResponse.getExperimentRunsList().isEmpty()) {
        isExpectedResultFound = true;
        for (ExperimentRun experimentRun : experimentRunResponse.getExperimentRunsList()) {
          assertEquals(
              experimentRunMap.get(experimentRun.getId()).getId(),
              experimentRun.getId(),
              "ExperimentRun not match with expected experimentRun");
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More ExperimentRun not found in database");
          assertTrue(true);
          break;
        } else {
          Assertions.fail("Expected experimentRun not found in response");
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
        2,
        experimentRunResponse.getTotalRecords(),
        "Total records count not matched with expected records count");
    assertEquals(
        1,
        experimentRunResponse.getExperimentRunsCount(),
        "ExperimentRuns count not match with expected experimentRuns count");
    assertEquals(
        experimentRun2.getId(),
        experimentRunResponse.getExperimentRuns(0).getId(),
        "ExperimentRun not match with expected experimentRun");

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
        1,
        experimentRunResponse.getExperimentRunsCount(),
        "ExperimentRuns count not match with expected experimentRuns count");
    assertEquals(
        experimentRun.getId(),
        experimentRunResponse.getExperimentRuns(0).getId(),
        "ExperimentRun not match with expected experimentRun");
    assertEquals(
        2,
        experimentRunResponse.getTotalRecords(),
        "Total records count not matched with expected records count");

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
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info(
        "Get ExperimentRun using pagination from Experiment test stop................................");
  }

  @Test
  public void bb_getExperimentFromExperimentNegativeTest() {
    LOGGER.info(
        "Get ExperimentRun from Experiment Negative test start................................");

    GetExperimentRunsInExperiment getExperiment =
        GetExperimentRunsInExperiment.newBuilder().build();
    try {
      experimentRunServiceStub.getExperimentRunsInExperiment(getExperiment);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getExperiment = GetExperimentRunsInExperiment.newBuilder().setExperimentId("sdfdsfsd").build();
    try {
      experimentRunServiceStub.getExperimentRunsInExperiment(getExperiment);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info(
        "Get ExperimentRun from Experiment Negative test stop................................");
  }

  @Test
  public void c_getExperimentRunByIdTest() {
    LOGGER.info("Get ExperimentRunById test start................................");

    GetExperimentRunById request =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();

    GetExperimentRunById.Response response = experimentRunServiceStub.getExperimentRunById(request);

    LOGGER.info("getExperimentRunById Response : \n" + response.getExperimentRun());
    assertEquals(
        experimentRun.getId(),
        response.getExperimentRun().getId(),
        "ExperimentRun not match with expected experimentRun");

    LOGGER.info("Get ExperimentRunById test stop................................");
  }

  @Test
  public void c_getExperimentRunByIdNegativeTest() {
    LOGGER.info("Get ExperimentRunById Negative test start................................");

    GetExperimentRunById request = GetExperimentRunById.newBuilder().build();

    try {
      experimentRunServiceStub.getExperimentRunById(request);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    request = GetExperimentRunById.newBuilder().setId("fdsfd").build();

    try {
      experimentRunServiceStub.getExperimentRunById(request);
      Assertions.fail();
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

    GetExperimentRunByName request =
        GetExperimentRunByName.newBuilder()
            .setName(experimentRun.getName())
            .setExperimentId(experimentRun.getExperimentId())
            .build();

    GetExperimentRunByName.Response response =
        experimentRunServiceStub.getExperimentRunByName(request);

    LOGGER.info("getExperimentRunByName Response : \n" + response.getExperimentRun());
    assertEquals(
        experimentRun.getName(),
        response.getExperimentRun().getName(),
        "ExperimentRun name not match with expected experimentRun name ");

    LOGGER.info("Get ExperimentRunByName test stop................................");
  }

  @Test
  public void c_getExperimentRunByNameNegativeTest() {
    LOGGER.info("Get ExperimentRunByName Negative test start................................");

    GetExperimentRunByName request = GetExperimentRunByName.newBuilder().build();

    try {
      experimentRunServiceStub.getExperimentRunByName(request);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Get ExperimentRunByName Negative test stop................................");
  }

  @Test
  public void e_addExperimentRunTags() {
    LOGGER.info("Add ExperimentRun tags test start................................");

    List<String> tags = new ArrayList<>();
    tags.add("Test Added tag");
    tags.add("Test Added tag 2");

    AddExperimentRunTags request =
        AddExperimentRunTags.newBuilder().setId(experimentRun.getId()).addAllTags(tags).build();

    AddExperimentRunTags.Response aertResponse =
        experimentRunServiceStub.addExperimentRunTags(request);
    LOGGER.info("AddExperimentRunTags Response : \n" + aertResponse.getExperimentRun());
    assertEquals(
        experimentRun.getTagsCount() + tags.size(), aertResponse.getExperimentRun().getTagsCount());

    assertNotEquals(
        experimentRun.getDateUpdated(),
        aertResponse.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");
    experimentRun = aertResponse.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    tags = new ArrayList<>();
    tags.add("Test Added tag 3");
    tags.add("Test Added tag 2");

    request =
        AddExperimentRunTags.newBuilder().setId(experimentRun.getId()).addAllTags(tags).build();

    aertResponse = experimentRunServiceStub.addExperimentRunTags(request);
    LOGGER.info("AddExperimentRunTags Response : \n" + aertResponse.getExperimentRun());
    assertEquals(experimentRun.getTagsCount() + 1, aertResponse.getExperimentRun().getTagsCount());
    experimentRun = aertResponse.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    try {
      String tag52 = "Human Activity Recognition using Smartphone Dataset";
      request = request.toBuilder().addTags(tag52).build();
      experimentRunServiceStub.addExperimentRunTags(request);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Add ExperimentRun tags test stop................................");
  }

  @Test
  public void ea_addExperimentRunTagsNegativeTest() {
    LOGGER.info("Add ExperimentRun tags Negative test start................................");

    List<String> tags = new ArrayList<>();
    tags.add("Test Added tag " + Calendar.getInstance().getTimeInMillis());
    tags.add("Test Added tag 2 " + Calendar.getInstance().getTimeInMillis());

    AddExperimentRunTags request = AddExperimentRunTags.newBuilder().addAllTags(tags).build();

    try {
      experimentRunServiceStub.addExperimentRunTags(request);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Add ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void eb_addExperimentRunTag() {
    LOGGER.info("Add ExperimentRun tag test start................................");

    AddExperimentRunTag request =
        AddExperimentRunTag.newBuilder()
            .setId(experimentRun.getId())
            .setTag("Added new tag 1")
            .build();

    AddExperimentRunTag.Response aertResponse =
        experimentRunServiceStub.addExperimentRunTag(request);
    LOGGER.info("AddExperimentRunTag Response : \n" + aertResponse.getExperimentRun());
    assertEquals(
        experimentRun.getTagsCount() + 1,
        aertResponse.getExperimentRun().getTagsCount(),
        "ExperimentRun tags not match with expected experimentRun tags");

    assertNotEquals(
        experimentRun.getDateUpdated(),
        aertResponse.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");
    experimentRun = aertResponse.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    try {
      String tag52 = "Human Activity Recognition using Smartphone Dataset";
      request = request.toBuilder().setTag(tag52).build();
      experimentRunServiceStub.addExperimentRunTag(request);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Add ExperimentRun tags test stop................................");
  }

  @Test
  public void ec_addExperimentRunTagNegativeTest() {
    LOGGER.info("Add ExperimentRun tag Negative test start................................");

    AddExperimentRunTag request = AddExperimentRunTag.newBuilder().setTag("Tag_xyz").build();

    try {
      experimentRunServiceStub.addExperimentRunTag(request);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Add ExperimentRun tag Negative test stop................................");
  }

  @Test
  public void ee_getExperimentRunTags() {
    LOGGER.info("Get ExperimentRun tags test start................................");

    GetTags request = GetTags.newBuilder().setId(experimentRun.getId()).build();

    GetTags.Response response = experimentRunServiceStub.getExperimentRunTags(request);
    LOGGER.info("GetExperimentRunTags Response : \n" + response.getTagsList());
    assertEquals(
        experimentRun.getTagsList(),
        response.getTagsList(),
        "ExperimentRun tags not match with expected experimentRun tags");

    LOGGER.info("Get ExperimentRun tags test stop................................");
  }

  @Test
  public void eea_getExperimentRunTagsNegativeTest() {
    LOGGER.info("Get ExperimentRun tags Negative test start................................");

    GetTags request = GetTags.newBuilder().build();
    try {
      experimentRunServiceStub.getExperimentRunTags(request);
      Assertions.fail();
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

    e_addExperimentRunTags();
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
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");
    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    if (!response.getExperimentRun().getTagsList().isEmpty()) {
      request =
          DeleteExperimentRunTags.newBuilder()
              .setId(experimentRun.getId())
              .setDeleteAll(true)
              .build();

      response = experimentRunServiceStub.deleteExperimentRunTags(request);
      LOGGER.info(
          "DeleteExperimentRunTags Response : \n" + response.getExperimentRun().getTagsList());
      assertEquals(0, response.getExperimentRun().getTagsList().size());
      experimentRun = response.getExperimentRun();
      experimentRunMap.put(experimentRun.getId(), experimentRun);
    }

    LOGGER.info("Delete ExperimentRun tags test stop................................");
  }

  @Test
  public void fa_deleteExperimentRunTagsNegativeTest() {
    LOGGER.info("Delete ExperimentRun tags Negative test start................................");

    DeleteExperimentRunTags request = DeleteExperimentRunTags.newBuilder().build();

    try {
      experimentRunServiceStub.deleteExperimentRunTags(request);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Delete ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void fb_deleteExperimentRunTag() {
    LOGGER.info("Delete ExperimentRun tag test start................................");

    e_addExperimentRunTags();
    DeleteExperimentRunTag request =
        DeleteExperimentRunTag.newBuilder()
            .setId(experimentRun.getId())
            .setTag(experimentRun.getTags(0))
            .build();

    DeleteExperimentRunTag.Response response =
        experimentRunServiceStub.deleteExperimentRunTag(request);
    LOGGER.info("DeleteExperimentRunTag Response : \n" + response.getExperimentRun().getTagsList());
    Assertions.assertFalse(
        response.getExperimentRun().getTagsList().contains(experimentRun.getTags(0)));

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");

    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);
    LOGGER.info("Delete ExperimentRun tags test stop................................");
  }

  /**
   * This test tests comparison predicates on numeric Key Values (Hyperparameters in this case) It
   * creates a project with two Experiments , each with two experiment runs. E1 ER1 hyperparameter.C
   * = 0.0001 E1 ER2 no hyperparameters E2 ER1 hyperparameter.C = 0.0001 E2 ER1 hyperparameter.C =
   * 1E-6
   *
   * <p>It then filters on C >= 0.0001 and expects 2 ExperimentRuns in results
   */
  @Test
  public void findExperimentRunsHyperparameter() {
    LOGGER.info("FindExperimentRuns test start................................");

    Map<String, ExperimentRun> experimentRunMap = new HashMap<>();

    try {
      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment.getId(), "ExperimentRun_ferh_1");
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
              project.getId(), experiment.getId(), "ExperimentRun_ferh_2");
      createExperimentRunRequest = createExperimentRunRequest.toBuilder().build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun12 = createExperimentRunResponse.getExperimentRun();
      experimentRunMap.put(experimentRun12.getId(), experimentRun12);
      LOGGER.info("ExperimentRun created successfully");
      assertEquals(
          createExperimentRunRequest.getName(),
          experimentRun12.getName(),
          "ExperimentRun name not match with expected ExperimentRun name");

      // experiment2 of above project
      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment2.getId(), "ExperimentRun_ferh_2");
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder().addHyperparameters(hyperparameter1).build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun21 = createExperimentRunResponse.getExperimentRun();
      experimentRunMap.put(experimentRun21.getId(), experimentRun21);
      LOGGER.info("ExperimentRun created successfully");
      assertEquals(
          createExperimentRunRequest.getName(),
          experimentRun21.getName(),
          "ExperimentRun name not match with expected ExperimentRun name");

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
          createExperimentRunRequest.getName(),
          experimentRun22.getName(),
          "ExperimentRun name not match with expected ExperimentRun name");

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
          2,
          response.getTotalRecords(),
          "Total records count not matched with expected records count");
      assertEquals(
          2,
          response.getExperimentRunsCount(),
          "ExperimentRun count not match with expected experimentRun count");
      for (ExperimentRun exprRun : response.getExperimentRunsList()) {
        for (KeyValue kv : exprRun.getHyperparametersList()) {
          if (kv.getKey().equals("C")) {
            assertThat(kv.getValue().getNumberValue()).isGreaterThanOrEqualTo(0.0001);
          }
        }
      }
    } finally {
      for (String runId : experimentRunMap.keySet()) {
        DeleteExperimentRun deleteExperimentRun =
            DeleteExperimentRun.newBuilder().setId(runId).build();
        DeleteExperimentRun.Response deleteExperimentRunResponse =
            experimentRunServiceStub.deleteExperimentRun(deleteExperimentRun);
        assertTrue(deleteExperimentRunResponse.getStatus());
      }
    }

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
            .setEpochNumber(Value.newBuilder().setNumberValue(2L))
            .build();

    LogObservation logObservationRequest =
        LogObservation.newBuilder()
            .setId(experimentRun.getId())
            .setObservation(observation)
            .build();

    experimentRunServiceStub.logObservation(logObservationRequest);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info("LogObservation Response : \n" + response.getExperimentRun());
    assertTrue(response.getExperimentRun().getObservationsList().contains(observation));

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");
    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info("Log Observation in ExperimentRun tags test stop................................");
  }

  @Test
  public void g_logObservationNegativeTest() {
    LOGGER.info(
        " Log Observation in ExperimentRun Negative test start................................");

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
      Assertions.fail();
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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info(
        "Log Observation in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void g_logObservationsTest() {
    LOGGER.info(" Log Observations in ExperimentRun test start................................");

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
            .setEpochNumber(Value.newBuilder().setNumberValue(234L))
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
            .setEpochNumber(Value.newBuilder().setNumberValue(2134L))
            .build();
    observations.add(observation2);

    LogObservations logObservationRequest =
        LogObservations.newBuilder()
            .setId(experimentRun.getId())
            .addAllObservations(observations)
            .build();

    experimentRunServiceStub.logObservations(logObservationRequest);
    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);

    LOGGER.info("LogObservation Response : \n" + response.getExperimentRun());
    assertTrue(
        response.getExperimentRun().getObservationsList().containsAll(observations),
        "ExperimentRun observations not match with expected ExperimentRun observation");

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");
    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    logObservationRequest =
        LogObservations.newBuilder()
            .setId(experimentRun.getId())
            .addAllObservations(experimentRun.getObservationsList())
            .build();

    experimentRunServiceStub.logObservations(logObservationRequest);

    getExperimentRunById = GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info(
        "Duplicate LogObservation Response : \n"
            + response.getExperimentRun().getObservationsList());
    assertEquals(
        experimentRun.getObservationsList().size() + experimentRun.getObservationsList().size(),
        response.getExperimentRun().getObservationsList().size(),
        "Existing observations count not match with expected observations count");

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");
    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info("Log Observations in ExperimentRun tags test stop................................");
  }

  @Test
  public void g_logObservationsNegativeTest() {
    LOGGER.info(
        " Log Observations in ExperimentRun Negative test start................................");

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
      Assertions.fail();
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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info(
        "Log Observations in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void h_getObservationTest() {
    LOGGER.info("Get Observation from ExperimentRun test start................................");

    g_logObservationsTest();
    GetObservations getObservationRequest =
        GetObservations.newBuilder()
            .setId(experimentRun.getId())
            .setObservationKey("Google developer Observation artifact")
            .build();

    GetObservations.Response response =
        experimentRunServiceStub.getObservations(getObservationRequest);

    LOGGER.info("GetObservations Response : " + response.getObservationsCount());
    for (Observation observation : response.getObservationsList()) {
      if (observation.hasAttribute()) {
        assertEquals(
            "Google developer Observation artifact",
            observation.getAttribute().getKey(),
            "ExperimentRun observations not match with expected observations ");
      } else if (observation.hasArtifact()) {
        assertEquals(
            "Google developer Observation artifact",
            observation.getArtifact().getKey(),
            "ExperimentRun observations not match with expected observations ");
      }
    }

    LOGGER.info(
        "Get Observation from ExperimentRun tags test stop................................");
  }

  @Test
  public void h_getObservationNegativeTest() {
    LOGGER.info(
        "Get Observation from ExperimentRun Negative test start................................");

    GetObservations getObservationRequest =
        GetObservations.newBuilder()
            .setObservationKey("Google developer Observation artifact")
            .build();

    try {
      experimentRunServiceStub.getObservations(getObservationRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getObservationRequest =
        GetObservations.newBuilder()
            .setId("dfsdfs")
            .setObservationKey("Google developer Observation artifact")
            .build();

    try {
      experimentRunServiceStub.getObservations(getObservationRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info(
        "Get Observation from ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void h_LogObservationEpochTest() {
    LOGGER.info("Get Observation from ExperimentRun test start................................");

    DeleteObservations request =
        DeleteObservations.newBuilder().setId(experimentRun.getId()).setDeleteAll(true).build();
    experimentRunServiceStub.deleteObservations(request);

    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    // First Observation without epoch should get epoch number to zero
    Observation observation =
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("key1")
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

    experimentRunServiceStub.logObservation(logObservationRequest);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.debug("observation epoch should be 0 Response: {}", response);
    assertEquals(
        1,
        response.getExperimentRun().getObservationsCount(),
        "there should be exactly one observation");
    Observation responseObservation = response.getExperimentRun().getObservations(0);
    assertEquals(
        0,
        responseObservation.getEpochNumber().getNumberValue(),
        0.0,
        "observation epoch should be 0");

    // observation with epoch should set the value passed
    observation =
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("key1")
                    .setValue(intValue)
                    .setValueType(ValueType.NUMBER)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .setEpochNumber(Value.newBuilder().setNumberValue(123))
            .build();

    logObservationRequest =
        LogObservation.newBuilder()
            .setId(experimentRun.getId())
            .setObservation(observation)
            .build();

    experimentRunServiceStub.logObservation(logObservationRequest);

    getExperimentRunById = GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.debug("observation epoch should be 123 Response: {}", response);
    assertEquals(
        2, response.getExperimentRun().getObservationsCount(), "there should be two observations");
    // Observations are sorted by auto incr id so the observation of interest is on index 1
    responseObservation = response.getExperimentRun().getObservations(1);
    assertEquals(
        123,
        (long) responseObservation.getEpochNumber().getNumberValue(),
        "observation epoch should be 123");

    // Subsequent call to log observation without epoch should set value to old max +1
    observation =
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("key1")
                    .setValue(intValue)
                    .setValueType(ValueType.NUMBER)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .build();

    logObservationRequest =
        LogObservation.newBuilder()
            .setId(experimentRun.getId())
            .setObservation(observation)
            .build();

    experimentRunServiceStub.logObservation(logObservationRequest);

    getExperimentRunById = GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.debug("observation epoch should be 123 + 1 Response: {}", response);
    assertEquals(
        3,
        response.getExperimentRun().getObservationsCount(),
        "there should be three observations");
    // Observations are sorted by auto incr id so the observation of interest is on index 2
    responseObservation = response.getExperimentRun().getObservations(2);
    assertEquals(
        124,
        (long) responseObservation.getEpochNumber().getNumberValue(),
        "observation epoch should be 123 + 1");

    // call to log observation with epoch but o not a different key should set value to 0
    observation =
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("key2")
                    .setValue(intValue)
                    .setValueType(ValueType.NUMBER)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .build();

    logObservationRequest =
        LogObservation.newBuilder()
            .setId(experimentRun.getId())
            .setObservation(observation)
            .build();

    experimentRunServiceStub.logObservation(logObservationRequest);

    getExperimentRunById = GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.debug("observation epoch should be 0 again Response: {}", response);
    assertEquals(
        4, response.getExperimentRun().getObservationsCount(), "there should be four observations");
    // Observations are sorted by auto incr id so the observation of interest is on index 3
    responseObservation = response.getExperimentRun().getObservations(3);
    assertEquals(
        0,
        (long) responseObservation.getEpochNumber().getNumberValue(),
        "observation epoch should be 0");

    // same epoch_number, same key stores duplicate
    observation =
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("key1")
                    .setValue(Value.newBuilder().setNumberValue(1))
                    .setValueType(ValueType.NUMBER)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .setEpochNumber(Value.newBuilder().setNumberValue(123))
            .build();

    logObservationRequest =
        LogObservation.newBuilder()
            .setId(experimentRun.getId())
            .setObservation(observation)
            .build();

    experimentRunServiceStub.logObservation(logObservationRequest);

    getExperimentRunById = GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    assertEquals(
        5, response.getExperimentRun().getObservationsCount(), "there should be five observations");
    // Observations are sorted by auto incr id so the observation of interest is on index 3
    responseObservation = response.getExperimentRun().getObservations(1);
    Observation responseObservation2 = response.getExperimentRun().getObservations(3);
    assertEquals(
        responseObservation.getAttribute().getKey(),
        responseObservation2.getAttribute().getKey(),
        "observations have same key");
    assertEquals(
        responseObservation.getEpochNumber(),
        responseObservation2.getEpochNumber(),
        "observations have same epoch number");

    // call to log observation with non numeric epoch throws error
    observation =
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("key2")
                    .setValue(intValue)
                    .setValueType(ValueType.NUMBER)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .setEpochNumber(Value.newBuilder().setStringValue("125"))
            .build();

    logObservationRequest =
        LogObservation.newBuilder()
            .setId(experimentRun.getId())
            .setObservation(observation)
            .build();

    try {
      experimentRunServiceStub.logObservation(logObservationRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info(
        "Get Observation from ExperimentRun tags test stop................................");
  }

  @Test
  public void i_logMetricTest() {
    LOGGER.info(" Log Metric in ExperimentRun test start................................");

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

    experimentRunServiceStub.logMetric(logMetricRequest);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info("LogMetric Response : \n" + response.getExperimentRun());
    assertTrue(
        response.getExperimentRun().getMetricsList().contains(keyValue),
        "ExperimentRun metric not match with expected experimentRun metric");

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");
    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info("Log Metric in ExperimentRun tags test stop................................");
  }

  @Test
  public void i_logMetricNegativeTest() {
    LOGGER.info(" Log Metric in ExperimentRun Negative test start................................");

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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logMetricRequest = LogMetric.newBuilder().setId("dfsdfsd").setMetric(keyValue).build();

    try {
      experimentRunServiceStub.logMetric(logMetricRequest);
      Assertions.fail();
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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    LOGGER.info(
        "Log Metric in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void i_logMetricsTest() {
    LOGGER.info(" Log Metrics in ExperimentRun test start................................");

    List<KeyValue> keyValues = new ArrayList<>();
    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    KeyValue keyValue1 =
        KeyValue.newBuilder()
            .setKey("New Added Metric 1 " + Calendar.getInstance().getTimeInMillis())
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
            .setKey("New Added Metric 2 " + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build();
    keyValues.add(keyValue2);

    LogMetrics logMetricRequest =
        LogMetrics.newBuilder().setId(experimentRun.getId()).addAllMetrics(keyValues).build();

    experimentRunServiceStub.logMetrics(logMetricRequest);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info("LogMetrics Response : \n" + response.getExperimentRun());
    assertTrue(
        response.getExperimentRun().getMetricsList().containsAll(keyValues),
        "ExperimentRun metrics not match with expected experimentRun metrics");

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");

    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info("Log Metrics in ExperimentRun tags test stop................................");
  }

  @Test
  public void i_logMetricsNegativeTest() {
    LOGGER.info(
        " Log Metrics in ExperimentRun Negative test start................................");

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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logMetricRequest = LogMetrics.newBuilder().setId("dfsdfsd").addAllMetrics(keyValues).build();

    try {
      experimentRunServiceStub.logMetrics(logMetricRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    logMetricRequest =
        LogMetrics.newBuilder()
            .setId(experimentRun.getId())
            .addAllMetrics(experimentRun.getMetricsList())
            .build();

    try {
      experimentRunServiceStub.logMetrics(logMetricRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    LOGGER.info(
        "Log Metrics in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void j_getMetricsTest() {
    LOGGER.info("Get Metrics from ExperimentRun test start................................");

    i_logMetricsTest();
    GetMetrics getMetricsRequest = GetMetrics.newBuilder().setId(experimentRun.getId()).build();

    GetMetrics.Response response = experimentRunServiceStub.getMetrics(getMetricsRequest);
    LOGGER.info("GetMetrics Response : " + response.getMetricsCount());
    assertEquals(
        experimentRun.getMetricsList(),
        response.getMetricsList(),
        "ExperimentRun metrics not match with expected experimentRun metrics");

    LOGGER.info("Get Metrics from ExperimentRun tags test stop................................");
  }

  @Test
  public void j_getMetricsNegativeTest() {
    LOGGER.info(
        "Get Metrics from ExperimentRun Negative test start................................");

    GetMetrics getMetricsRequest = GetMetrics.newBuilder().build();

    try {
      experimentRunServiceStub.getMetrics(getMetricsRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getMetricsRequest = GetMetrics.newBuilder().setId("sdfdsfsd").build();

    try {
      experimentRunServiceStub.getMetrics(getMetricsRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info(
        "Get Metrics from ExperimentRun tags Negative test stop................................");
  }

  /* commented till dataset int code is in
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
          experimentTest.getCreateExperimentRequestForOtherTests(project.getId(), "Experiment_n_sprt_abc");
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

      experimentRunServiceStub.logDataset(logDatasetRequest);

      GetExperimentRunById getExperimentRunById =
          GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
      GetExperimentRunById.Response response =
          experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
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
      experimentRunServiceStub.logDataset(logDatasetRequest);

      response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
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
          experimentTest.getCreateExperimentRequestForOtherTests(project.getId(), "Experiment_n_sprt_abc");
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

      experimentRunServiceStub.logDataset(logDatasetRequest);

      GetExperimentRunById getExperimentRunById =
          GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
      GetExperimentRunById.Response response =
          experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
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
          experimentTest.getCreateExperimentRequestForOtherTests(project.getId(), "Experiment_n_sprt_abc");
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

      experimentRunServiceStub.logDatasets(logDatasetRequest);

      GetExperimentRunById getExperimentRunById =
          GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
      GetExperimentRunById.Response response =
          experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
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
      experimentRunServiceStub.logDatasets(logDatasetRequest);

      response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
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
  */
  @Test
  public void k_logDatasetsNegativeTest() {
    LOGGER.info(
        " Log Datasets in ExperimentRun Negative test start................................");

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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logDatasetRequest = LogDatasets.newBuilder().setId("sdsdsa").addAllDatasets(artifacts).build();

    try {
      experimentRunServiceStub.logDatasets(logDatasetRequest);
      Assertions.fail();
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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    LOGGER.info(
        "Log Datasets in ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void l_getDatasetsTest() {
    LOGGER.info("Get Datasets from ExperimentRun test start................................");

    GetDatasets getDatasetsRequest = GetDatasets.newBuilder().setId(experimentRun.getId()).build();

    GetDatasets.Response response = experimentRunServiceStub.getDatasets(getDatasetsRequest);
    LOGGER.info("GetDatasets Response : " + response.getDatasetsCount());
    assertEquals(
        experimentRun.getDatasetsList().stream().map(Artifact::getKey).collect(Collectors.toList()),
        response.getDatasetsList().stream().map(Artifact::getKey).collect(Collectors.toList()),
        "Experiment datasets not match with expected datasets");

    LOGGER.info("Get Datasets from ExperimentRun tags test stop................................");
  }

  @Test
  public void l_getDatasetsNegativeTest() {
    LOGGER.info(
        "Get Datasets from ExperimentRun Negative test start................................");

    GetDatasets getDatasetsRequest = GetDatasets.newBuilder().build();

    try {
      experimentRunServiceStub.getDatasets(getDatasetsRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getDatasetsRequest = GetDatasets.newBuilder().setId("sdfsdfdsf").build();

    try {
      experimentRunServiceStub.getDatasets(getDatasetsRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info(
        "Get Datasets from ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void m_logArtifactTest() {
    LOGGER.info(" Log Artifact in ExperimentRun test start................................");

    Artifact artifact =
        Artifact.newBuilder()
            .setKey("Google Pay Artifact " + Calendar.getInstance().getTimeInMillis())
            .setPath("46513216546" + Calendar.getInstance().getTimeInMillis())
            .setArtifactType(ArtifactType.TENSORBOARD)
            .setFilenameExtension("tf")
            .build();

    LogArtifact logArtifactRequest =
        LogArtifact.newBuilder().setId(experimentRun.getId()).setArtifact(artifact).build();

    experimentRunServiceStub.logArtifact(logArtifactRequest);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info("LogArtifact Response : " + response.getExperimentRun().getArtifactsCount());
    assertEquals(
        experimentRun.getArtifactsCount() + 1,
        response.getExperimentRun().getArtifactsCount(),
        "Experiment artifact count not match with expected artifact count");

    checkValidArtifactPath(
        response.getExperimentRun().getId(),
        "ExperimentRunEntity",
        response.getExperimentRun().getArtifactsList());

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");

    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info("Log Artifact in ExperimentRun tags test stop................................");
  }

  private void checkValidArtifactPath(
      String entityId, String entityName, List<Artifact> artifacts) {
    for (var responseArtifact : artifacts) {
      var validPrefix =
          testConfig.getArtifactStoreConfig().getPathPrefixWithSeparator() + entityName;
      var path = validPrefix + "/" + entityId + "/" + responseArtifact.getKey();

      var filenameExtension = responseArtifact.getFilenameExtension();
      if (!filenameExtension.isEmpty() && !filenameExtension.endsWith("." + filenameExtension)) {
        path += "." + filenameExtension;
      }

      assertEquals(
          path,
          responseArtifact.getPath(),
          "ExperimentRun artifact path not match with expected artifact path");
    }
  }

  @Test
  public void m_logArtifactNegativeTest() {
    LOGGER.info(
        " Log Artifact in ExperimentRun Negative test start................................");

    Artifact artifact =
        Artifact.newBuilder()
            .setKey("Google Pay Artifact")
            .setPath("46513216546" + Calendar.getInstance().getTimeInMillis())
            .setArtifactType(ArtifactType.TENSORBOARD)
            .build();

    LogArtifact logArtifactRequest = LogArtifact.newBuilder().setArtifact(artifact).build();
    try {
      experimentRunServiceStub.logArtifact(logArtifactRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logArtifactRequest = LogArtifact.newBuilder().setId("asda").setArtifact(artifact).build();
    try {
      experimentRunServiceStub.logArtifact(logArtifactRequest);
      Assertions.fail();
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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    LOGGER.info(
        "Log Artifact in ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void m_logArtifactsTest() {
    LOGGER.info(" Log Artifacts in ExperimentRun test start................................");

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

    experimentRunServiceStub.logArtifacts(logArtifactRequest);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info("LogArtifact Response : \n" + response.getExperimentRun());
    assertEquals(
        (experimentRun.getArtifactsCount() + artifacts.size()),
        response.getExperimentRun().getArtifactsList().size(),
        "ExperimentRun artifacts not match with expected artifacts");

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");

    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info("Log Artifacts in ExperimentRun tags test stop................................");
  }

  @Test
  public void m_logArtifactsNegativeTest() {
    LOGGER.info(
        " Log Artifacts in ExperimentRun Negative test start................................");

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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logArtifactRequest = LogArtifacts.newBuilder().setId("asda").addAllArtifacts(artifacts).build();
    try {
      experimentRunServiceStub.logArtifacts(logArtifactRequest);
      Assertions.fail();
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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    LOGGER.info(
        "Log Artifacts in ExperimentRun tags Negative test stop................................");
  }

  @Test
  public void n_getArtifactsTest() {
    LOGGER.info("Get Artifacts from ExperimentRun test start................................");

    m_logArtifactsTest();
    GetArtifacts getArtifactsRequest =
        GetArtifacts.newBuilder().setId(experimentRun.getId()).build();

    GetArtifacts.Response response = experimentRunServiceStub.getArtifacts(getArtifactsRequest);

    LOGGER.info("GetArtifacts Response : " + response.getArtifactsCount());
    assertEquals(
        experimentRun.getArtifactsList(),
        response.getArtifactsList(),
        "ExperimentRun artifacts not matched with expected artifacts");

    LOGGER.info("Get Artifacts from ExperimentRun tags test stop................................");
  }

  @Test
  public void n_getArtifactsNegativeTest() {
    LOGGER.info(
        "Get Artifacts from ExperimentRun Negative test start................................");

    GetArtifacts getArtifactsRequest = GetArtifacts.newBuilder().build();

    try {
      experimentRunServiceStub.getArtifacts(getArtifactsRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getArtifactsRequest = GetArtifacts.newBuilder().setId("dssaa").build();

    try {
      experimentRunServiceStub.getArtifacts(getArtifactsRequest);
      Assertions.fail();
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

    experimentRunServiceStub.logHyperparameter(logHyperparameterRequest);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info("LogHyperparameter Response : \n" + response.getExperimentRun());
    assertTrue(
        response.getExperimentRun().getHyperparametersList().contains(hyperparameter),
        "ExperimentRun hyperparameter not match with expected hyperparameter");

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");
    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info(
        "Log Hyperparameter in ExperimentRun tags test stop................................");
  }

  @Test
  public void o_logHyperparameterNegativeTest() {
    LOGGER.info(
        " Log Hyperparameter in ExperimentRun Negative test start................................");

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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logHyperparameterRequest =
        LogHyperparameter.newBuilder().setId("dsdsfs").setHyperparameter(hyperparameter).build();

    try {
      experimentRunServiceStub.logHyperparameter(logHyperparameterRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    logHyperparameterRequest =
        LogHyperparameter.newBuilder()
            .setId(experimentRun.getId())
            .setHyperparameter(experimentRun.getHyperparametersList().get(0))
            .build();

    try {
      experimentRunServiceStub.logHyperparameter(logHyperparameterRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    LOGGER.info(
        "Log Hyperparameter in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void o_logHyperparametersTest() {
    LOGGER.info(" Log Hyperparameters in ExperimentRun test start................................");

    List<KeyValue> hyperparameters = new ArrayList<>();
    Value blobValue = Value.newBuilder().setStringValue("this is a blob data example").build();
    KeyValue hyperparameter1 =
        KeyValue.newBuilder()
            .setKey("Log new hyperparameter 1 " + Calendar.getInstance().getTimeInMillis())
            .setValue(blobValue)
            .setValueType(ValueType.BLOB)
            .build();
    hyperparameters.add(hyperparameter1);

    Value numValue = Value.newBuilder().setNumberValue(12.02125212).build();
    KeyValue hyperparameter2 =
        KeyValue.newBuilder()
            .setKey("Log new hyperparameter 2 " + Calendar.getInstance().getTimeInMillis())
            .setValue(numValue)
            .setValueType(ValueType.NUMBER)
            .build();
    hyperparameters.add(hyperparameter2);

    LogHyperparameters logHyperparameterRequest =
        LogHyperparameters.newBuilder()
            .setId(experimentRun.getId())
            .addAllHyperparameters(hyperparameters)
            .build();

    experimentRunServiceStub.logHyperparameters(logHyperparameterRequest);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info("LogHyperparameters Response : \n" + response.getExperimentRun());
    assertTrue(
        response.getExperimentRun().getHyperparametersList().containsAll(hyperparameters),
        "ExperimentRun hyperparameters not match with expected hyperparameters");

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");

    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info(
        "Log Hyperparameters in ExperimentRun tags test stop................................");
  }

  @Test
  public void o_logHyperparametersNegativeTest() {
    LOGGER.info(
        " Log Hyperparameters in ExperimentRun Negative test start................................");

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
      Assertions.fail();
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
      Assertions.fail();
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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    LOGGER.info("Log Hyperparameters in ExperimentRun Negative tags test stop.....");
  }

  @Test
  public void p_getHyperparametersTest() {
    LOGGER.info("Get Hyperparameters from ExperimentRun test start........");

    o_logHyperparametersTest();
    GetHyperparameters getHyperparametersRequest =
        GetHyperparameters.newBuilder().setId(experimentRun.getId()).build();

    GetHyperparameters.Response response =
        experimentRunServiceStub.getHyperparameters(getHyperparametersRequest);

    LOGGER.info("GetHyperparameters Response : " + response.getHyperparametersCount());
    assertEquals(
        experimentRun.getHyperparametersList(),
        response.getHyperparametersList(),
        "ExperimentRun Hyperparameters not match with expected hyperparameters");

    LOGGER.info("Get Hyperparameters from ExperimentRun tags test stop......");
  }

  @Test
  public void p_getHyperparametersNegativeTest() {
    LOGGER.info("Get Hyperparameters from ExperimentRun Negative test start..........");

    GetHyperparameters getHyperparametersRequest = GetHyperparameters.newBuilder().build();

    try {
      experimentRunServiceStub.getHyperparameters(getHyperparametersRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getHyperparametersRequest = GetHyperparameters.newBuilder().setId("sdsssd").build();

    try {
      experimentRunServiceStub.getHyperparameters(getHyperparametersRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info(
        "Get Hyperparameters from ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void q_logAttributeTest() {
    LOGGER.info(" Log Attribute in ExperimentRun test start................................");

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

    experimentRunServiceStub.logAttribute(logAttributeRequest);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info("LogAttribute Response : \n" + response.getExperimentRun());
    assertTrue(
        response.getExperimentRun().getAttributesList().contains(attribute),
        "ExperimentRun attribute not match with expected attribute");

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");

    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info("Log Attribute in ExperimentRun tags test stop................................");
  }

  @Test
  public void q_logAttributeNegativeTest() {
    LOGGER.info(
        " Log Attribute in ExperimentRun Negative test start................................");

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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logAttributeRequest = LogAttribute.newBuilder().setId("sdsds").setAttribute(attribute).build();

    try {
      experimentRunServiceStub.logAttribute(logAttributeRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    logAttributeRequest =
        LogAttribute.newBuilder()
            .setId(experimentRun.getId())
            .setAttribute(experimentRun.getAttributesList().get(0))
            .build();

    try {
      experimentRunServiceStub.logAttribute(logAttributeRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    LOGGER.info(
        "Log Attribute in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void q_logAttributesTest() {
    LOGGER.info(" Log Attributes in ExperimentRun test start................................");

    List<KeyValue> attributes = new ArrayList<>();
    Value blobValue =
        Value.newBuilder().setStringValue("this is a blob data example of attribute").build();
    KeyValue attribute1 =
        KeyValue.newBuilder()
            .setKey("Log new attribute 1 " + Calendar.getInstance().getTimeInMillis())
            .setValue(blobValue)
            .setValueType(ValueType.BLOB)
            .build();
    attributes.add(attribute1);
    Value stringValue =
        Value.newBuilder().setStringValue("this is a blob data example of attribute").build();
    KeyValue attribute2 =
        KeyValue.newBuilder()
            .setKey("Log new attribute 2 " + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build();
    attributes.add(attribute2);

    LogAttributes logAttributeRequest =
        LogAttributes.newBuilder()
            .setId(experimentRun.getId())
            .addAllAttributes(attributes)
            .build();

    experimentRunServiceStub.logAttributes(logAttributeRequest);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info("LogAttributes Response : \n" + response.getExperimentRun());
    assertTrue(
        response.getExperimentRun().getAttributesList().containsAll(attributes),
        "ExperimentRun attributes not match with expected attributes");

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");

    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info("Log Attributes in ExperimentRun tags test stop................................");
  }

  @Test
  public void q_logAttributesNegativeTest() {
    LOGGER.info(
        " Log Attributes in ExperimentRun Negative test start................................");

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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    logAttributeRequest =
        LogAttributes.newBuilder().setId("sdsds").addAllAttributes(attributes).build();

    try {
      experimentRunServiceStub.logAttributes(logAttributeRequest);
      Assertions.fail();
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
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    LOGGER.info(
        "Log Attributes in ExperimentRun Negative tags test stop................................");
  }

  @Test
  public void qq_addExperimentRunAttributes() {
    LOGGER.info("Add ExperimentRun Attributes test start................................");

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

    experimentRunServiceStub.addExperimentRunAttributes(request);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info("AddExperimentRunAttributes Response : \n" + response.getExperimentRun());
    assertTrue(
        response.getExperimentRun().getAttributesList().containsAll(attributeList),
        "ExperimentRun attributes not match with expected attributes");

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");

    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info("Add ExperimentRun Attributes test stop................................");
  }

  @Test
  public void qq_addExperimentRunAttributesNegativeTest() {
    LOGGER.info("Add ExperimentRun attributes Negative test start................................");

    AddExperimentRunAttributes request =
        AddExperimentRunAttributes.newBuilder().setId("xyz").build();

    try {
      experimentRunServiceStub.addExperimentRunAttributes(request);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    AddExperimentRunAttributes addAttributesRequest =
        AddExperimentRunAttributes.newBuilder().build();
    try {
      experimentRunServiceStub.addExperimentRunAttributes(addAttributesRequest);
      Assertions.fail();
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

    q_logAttributesTest();
    List<KeyValue> attributes = experimentRun.getAttributesList();
    LOGGER.info("Attributes size : " + attributes.size());

    if (attributes.isEmpty()) {
      LOGGER.warn("Experiment Attributes not found in database ");
      Assertions.fail();
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
          keys.contains(attributeKeyValue.getKey()),
          "Experiment attribute not match with expected attribute");
    }

    getAttributesRequest =
        GetAttributes.newBuilder().setId(experimentRun.getId()).setGetAll(true).build();

    response = experimentRunServiceStub.getExperimentRunAttributes(getAttributesRequest);

    LOGGER.info("GetAttributes Response : " + response.getAttributesCount());
    assertEquals(attributes.size(), response.getAttributesList().size());

    LOGGER.info("Get Attributes from ExperimentRun tags test stop................................");
  }

  @Test
  public void r_getExperimentRunAttributesNegativeTest() {
    LOGGER.info(
        "Get Attributes from ExperimentRun Negative test start................................");

    GetAttributes getAttributesRequest = GetAttributes.newBuilder().build();

    try {
      experimentRunServiceStub.getExperimentRunAttributes(getAttributesRequest);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getAttributesRequest = GetAttributes.newBuilder().setId("dssdds").setGetAll(true).build();

    try {
      experimentRunServiceStub.getExperimentRunAttributes(getAttributesRequest);
      Assertions.fail();
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

    q_logAttributesTest();
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

    experimentRunServiceStub.deleteExperimentRunAttributes(request);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info(
        "DeleteExperimentRunAttributes Response : \n"
            + response.getExperimentRun().getAttributesList());
    assertTrue(response.getExperimentRun().getAttributesList().size() <= 1);

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");
    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    if (!response.getExperimentRun().getAttributesList().isEmpty()) {
      request =
          DeleteExperimentRunAttributes.newBuilder()
              .setId(experimentRun.getId())
              .setDeleteAll(true)
              .build();

      experimentRunServiceStub.deleteExperimentRunAttributes(request);

      response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
      LOGGER.info(
          "DeleteExperimentRunAttributes Response : \n"
              + response.getExperimentRun().getAttributesList());
      assertEquals(0, response.getExperimentRun().getAttributesList().size());

      assertNotEquals(
          experimentRun.getDateUpdated(),
          response.getExperimentRun().getDateUpdated(),
          "ExperimentRun date_updated field not update on database");
      experimentRun = response.getExperimentRun();
      experimentRunMap.put(experimentRun.getId(), experimentRun);
    }

    LOGGER.info("Delete ExperimentRun Attributes test stop................................");
  }

  @Test
  public void rrr_deleteExperimentRunAttributesNegativeTest() {
    LOGGER.info(
        "Delete ExperimentRun Attributes Negative test start................................");

    DeleteExperimentRunAttributes request = DeleteExperimentRunAttributes.newBuilder().build();

    try {
      experimentRunServiceStub.deleteExperimentRunAttributes(request);
      Assertions.fail();
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

    experimentRunServiceStub.deleteExperimentRunAttributes(request);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info(
        "DeleteExperimentRunAttributes Response : \n"
            + response.getExperimentRun().getAttributesList());
    assertEquals(0, response.getExperimentRun().getAttributesList().size());

    LOGGER.info(
        "Delete ExperimentRun Attributes Negative test stop................................");
  }

  @Test
  public void z_deleteExperimentRunTest() {
    LOGGER.info("Delete ExperimentRun test start................................");

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_n_sprt");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        createExperimentRunRequest.getName(),
        experimentRun.getName(),
        "ExperimentRun name not match with expected ExperimentRun name");

    DeleteExperimentRun deleteExperimentRun =
        DeleteExperimentRun.newBuilder().setId(experimentRun.getId()).build();
    DeleteExperimentRun.Response deleteExperimentRunResponse =
        experimentRunServiceStub.deleteExperimentRun(deleteExperimentRun);
    assertTrue(deleteExperimentRunResponse.getStatus());

    LOGGER.info("Delete ExperimentRun test stop................................");
  }

  @Test
  public void z_deleteExperimentRunNegativeTest() {
    LOGGER.info("Delete ExperimentRun Negative test start................................");

    DeleteExperimentRun request = DeleteExperimentRun.newBuilder().build();

    try {
      experimentRunServiceStub.deleteExperimentRun(request);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    request = DeleteExperimentRun.newBuilder().setId("ddsdfds").build();

    try {
      experimentRunServiceStub.deleteExperimentRun(request);
      Assertions.fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    LOGGER.info("Delete ExperimentRun Negative test stop................................");
  }

  @Test
  public void createParentChildExperimentRunTest() {
    LOGGER.info("Create Parent Children ExperimentRun test start................................");

    List<String> experimentRunIds = new ArrayList<>();
    try {
      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project.getId(),
              experiment.getId(),
              "ExperimentRun-parent-1-" + new Date().getTime());
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(experimentRun.getId());
      LOGGER.info("ExperimentRun created successfully");
      assertEquals(
          createExperimentRunRequest.getName(),
          experimentRun.getName(),
          "ExperimentRun name not match with expected ExperimentRun name");

      // Children experimentRun 1
      createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project.getId(),
              experiment.getId(),
              "ExperimentRun-children-1-" + new Date().getTime());
      // Add Parent Id to children
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder().setParentId(experimentRun.getId()).build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun childrenExperimentRun1 = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(childrenExperimentRun1.getId());
      LOGGER.info("ExperimentRun1 created successfully");
      assertEquals(
          createExperimentRunRequest.getName(),
          childrenExperimentRun1.getName(),
          "ExperimentRun name not match with expected ExperimentRun name");

      // Children experimentRun 2
      createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project.getId(),
              experiment.getId(),
              "ExperimentRun-children-2-" + new Date().getTime());
      // Add Parent Id to children
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder().setParentId(experimentRun.getId()).build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun childrenExperimentRun2 = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(childrenExperimentRun2.getId());
      LOGGER.info("ExperimentRun1 created successfully");
      assertEquals(
          createExperimentRunRequest.getName(),
          childrenExperimentRun2.getName(),
          "ExperimentRun2 name not match with expected ExperimentRun name");

    } finally {
      for (String runId : experimentRunIds) {
        DeleteExperimentRun deleteExperimentRun =
            DeleteExperimentRun.newBuilder().setId(runId).build();
        DeleteExperimentRun.Response deleteExperimentRunResponse =
            experimentRunServiceStub.deleteExperimentRun(deleteExperimentRun);
        assertTrue(deleteExperimentRunResponse.getStatus());
      }
    }

    LOGGER.info("Create Parent Children ExperimentRun test stop................................");
  }

  @Test
  public void logExperimentRunCodeVersionTest() {
    LOGGER.info("Log ExperimentRun code version test start................................");

    LogExperimentRunCodeVersion logExperimentRunCodeVersionRequest =
        LogExperimentRunCodeVersion.newBuilder()
            .setId(experimentRun.getId())
            .setCodeVersion(
                CodeVersion.newBuilder()
                    .setCodeArchive(
                        Artifact.newBuilder()
                            .setKey("code_version_image")
                            .setPath("https://xyz_path_string.com/image.png")
                            .setArtifactType(ArtifactType.CODE)
                            .setFilenameExtension("png")
                            .setUploadCompleted(
                                !testConfig
                                    .getArtifactStoreConfig()
                                    .getArtifactStoreType()
                                    .equals(ArtifactStoreConfig.S3_TYPE_STORE))
                            .build())
                    .build())
            .build();
    experimentRunServiceStub.logExperimentRunCodeVersion(logExperimentRunCodeVersionRequest);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    CodeVersion codeVersion = response.getExperimentRun().getCodeVersionSnapshot();
    assertNotEquals(
        logExperimentRunCodeVersionRequest.getCodeVersion(),
        codeVersion,
        "ExperimentRun codeVersion not match with expected ExperimentRun codeVersion");

    var validPrefix =
        testConfig.getArtifactStoreConfig().getPathPrefixWithSeparator() + "CodeVersionEntity";
    assertTrue(
        codeVersion.getCodeArchive().getPath().startsWith(validPrefix),
        "ExperimentRun artifact path not match with expected artifact path");

    assertTrue(
        codeVersion.getCodeArchive().getPath().contains("fake-"),
        "ExperimentRun artifact path not match with expected artifact path");

    assertTrue(
        codeVersion
            .getCodeArchive()
            .getPath()
            .endsWith("." + codeVersion.getCodeArchive().getFilenameExtension()),
        "ExperimentRun artifact path not match with expected artifact path");

    try {
      experimentRunServiceStub.logExperimentRunCodeVersion(logExperimentRunCodeVersionRequest);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    logExperimentRunCodeVersionRequest =
        logExperimentRunCodeVersionRequest.toBuilder()
            .setCodeVersion(
                CodeVersion.newBuilder()
                    .setCodeArchive(
                        Artifact.newBuilder()
                            .setKey("code_version_image_1")
                            .setPath("https://xyz_path_string.com/image.png")
                            .setArtifactType(ArtifactType.IMAGE)
                            .setUploadCompleted(
                                !testConfig
                                    .getArtifactStoreConfig()
                                    .getArtifactStoreType()
                                    .equals(ArtifactStoreConfig.S3_TYPE_STORE))
                            .build())
                    .build())
            .setOverwrite(true)
            .build();
    experimentRunServiceStub.logExperimentRunCodeVersion(logExperimentRunCodeVersionRequest);

    response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    codeVersion = response.getExperimentRun().getCodeVersionSnapshot();
    assertNotEquals(
        logExperimentRunCodeVersionRequest.getCodeVersion(),
        codeVersion,
        "ExperimentRun codeVersion not match with expected ExperimentRun codeVersion");

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
    experimentRunServiceStub.logExperimentRunCodeVersion(logExperimentRunCodeVersionRequest);

    response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    codeVersion = response.getExperimentRun().getCodeVersionSnapshot();
    assertEquals(
        logExperimentRunCodeVersionRequest.getCodeVersion(),
        codeVersion,
        "ExperimentRun codeVersion not match with expected ExperimentRun codeVersion");

    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info("Log ExperimentRun code version test stop................................");
  }

  @Test
  public void getExperimentRunCodeVersionTest() {
    LOGGER.info("Get ExperimentRun code version test start................................");

    LogExperimentRunCodeVersion logExperimentRunCodeVersionRequest =
        LogExperimentRunCodeVersion.newBuilder()
            .setId(experimentRun.getId())
            .setCodeVersion(
                CodeVersion.newBuilder()
                    .setCodeArchive(
                        Artifact.newBuilder()
                            .setKey("code_version_image")
                            .setPath("https://xyz_path_string.com/image.png")
                            .setArtifactType(ArtifactType.CODE)
                            .setUploadCompleted(
                                !testConfig
                                    .getArtifactStoreConfig()
                                    .getArtifactStoreType()
                                    .equals(ArtifactStoreConfig.S3_TYPE_STORE))
                            .build())
                    .build())
            .build();
    experimentRunServiceStub.logExperimentRunCodeVersion(logExperimentRunCodeVersionRequest);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    CodeVersion codeVersion = response.getExperimentRun().getCodeVersionSnapshot();
    assertNotEquals(
        logExperimentRunCodeVersionRequest.getCodeVersion(),
        codeVersion,
        "ExperimentRun codeVersion not match with expected ExperimentRun codeVersion");

    GetExperimentRunCodeVersion getExperimentRunCodeVersionRequest =
        GetExperimentRunCodeVersion.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunCodeVersion.Response getExperimentRunCodeVersionResponse =
        experimentRunServiceStub.getExperimentRunCodeVersion(getExperimentRunCodeVersionRequest);
    assertEquals(
        codeVersion,
        getExperimentRunCodeVersionResponse.getCodeVersion(),
        "ExperimentRun codeVersion not match with expected experimentRun codeVersion");

    LOGGER.info("Get ExperimentRun code version test stop................................");
  }

  @Test
  public void deleteExperimentRunArtifacts() {
    LOGGER.info("Delete ExperimentRun Artifacts test start................................");

    m_logArtifactsTest();
    List<Artifact> artifacts = experimentRun.getArtifactsList();
    LOGGER.info("Artifacts size : " + artifacts.size());
    if (artifacts.isEmpty()) {
      Assertions.fail("Artifacts not found");
    }

    DeleteArtifact request =
        DeleteArtifact.newBuilder()
            .setId(experimentRun.getId())
            .setKey(artifacts.get(0).getKey())
            .build();

    experimentRunServiceStub.deleteArtifact(request);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info(
        "DeleteExperimentRunArtifacts Response : \n"
            + response.getExperimentRun().getArtifactsList());
    Assertions.assertFalse(
        response.getExperimentRun().getArtifactsList().contains(artifacts.get(0)));

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");

    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    LOGGER.info("Delete ExperimentRun Artifacts test stop................................");
  }

  @Test
  public void batchDeleteExperimentRunTest() {
    LOGGER.info("Batch Delete ExperimentRun test start................................");

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        ExperimentTest.getCreateExperimentRequestForOtherTests(
            project.getId(), "Experiment-" + new Date().getTime());
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        createExperimentRequest.getName(),
        experiment.getName(),
        "Experiment name not match with expected Experiment name");

    List<String> experimentRunIds = new ArrayList<>();
    for (int count = 0; count < 5; count++) {
      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project.getId(),
              experiment.getId(),
              "ExperimentRun-" + count + "-" + new Date().getTime());
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(experimentRun.getId());
      LOGGER.info("ExperimentRun created successfully");
      assertEquals(
          createExperimentRunRequest.getName(),
          experimentRun.getName(),
          "ExperimentRun name not match with expected ExperimentRun name");
    }

    DeleteExperimentRuns deleteExperimentRuns =
        DeleteExperimentRuns.newBuilder().addAllIds(experimentRunIds).build();
    DeleteExperimentRuns.Response deleteExperimentRunsResponse =
        experimentRunServiceStub.deleteExperimentRuns(deleteExperimentRuns);
    assertTrue(deleteExperimentRunsResponse.getStatus());

    FindExperimentRuns getExperimentRunsInExperiment =
        FindExperimentRuns.newBuilder().setExperimentId(experiment.getId()).build();

    FindExperimentRuns.Response experimentRunResponse =
        experimentRunServiceStub.findExperimentRuns(getExperimentRunsInExperiment);
    assertEquals(
        0,
        experimentRunResponse.getExperimentRunsCount(),
        "ExperimentRuns count not match with expected experimentRun count");

    LOGGER.info("Batch Delete ExperimentRun test stop................................");
  }

  @Test
  public void deleteExperimentRunByParentEntitiesOwnerTest() {
    LOGGER.info(
        "Delete ExperimentRun by parent entities owner test start.........................");

    if (testConfig.hasAuth()) {
      if (!isRunningIsolated() && !testConfig.isPermissionV2Enabled()) {
        AddCollaboratorRequest addCollaboratorRequest =
            addCollaboratorRequestProjectInterceptor(
                project, CollaboratorType.READ_ONLY, authClientInterceptor);

        AddCollaboratorRequest.Response addCollaboratorResponse =
            collaboratorServiceStubClient1.addOrUpdateProjectCollaborator(addCollaboratorRequest);
        LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
        assertTrue(addCollaboratorResponse.getStatus());
      }
    }

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        ExperimentTest.getCreateExperimentRequestForOtherTests(
            project.getId(), "Experiment-" + new Date().getTime());
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");

    List<String> experimentRunIds = new ArrayList<>();
    for (int count = 0; count < 5; count++) {
      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project.getId(),
              experiment.getId(),
              "ExperimentRun-" + count + "-" + new Date().getTime());
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

    if (testConfig.hasAuth()) {
      if (isRunningIsolated()) {
        when(uac.getUACService().getCurrentUser(any()))
            .thenReturn(Futures.immediateFuture(testUser2));
        // mockGetResourcesForAllEntity(Map.of(project.getId(), project), testUser2);
        when(uac.getAuthzService().isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(false).build()));
      }
      try {
        experimentRunServiceStubClient2.deleteExperimentRuns(deleteExperimentRuns);
        Assertions.fail();
      } catch (StatusRuntimeException e) {
        checkEqualsAssert(e);
      }

      if (isRunningIsolated()) {
        when(uac.getAuthzService().isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(true).build()));
      } else if (testConfig.isPermissionV2Enabled()) {
        var groupStub = GroupServiceGrpc.newBlockingStub(authServiceChannelServiceUser);
        groupStub.addUsers(
            AddGroupUsers.newBuilder()
                .addUserId(testUser2.getVertaInfo().getUserId())
                .setGroupId(groupIdUser1)
                .setOrgId(organizationId)
                .build());
      } else {
        AddCollaboratorRequest addCollaboratorRequest =
            addCollaboratorRequestProjectInterceptor(
                project, CollaboratorType.READ_WRITE, authClientInterceptor);

        AddCollaboratorRequest.Response addCollaboratorResponse =
            collaboratorServiceStubClient1.addOrUpdateProjectCollaborator(addCollaboratorRequest);
        LOGGER.info("Collaborator updated in server : " + addCollaboratorResponse.getStatus());
        assertTrue(addCollaboratorResponse.getStatus());
      }

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

    if (isRunningIsolated()) {
      when(uac.getUACService().getCurrentUser(any()))
          .thenReturn(Futures.immediateFuture(testUser1));
      when(uac.getAuthzService().isSelfAllowed(any()))
          .thenReturn(
              Futures.immediateFuture(
                  IsSelfAllowed.Response.newBuilder().setAllowed(true).build()));
    }

    FindExperimentRuns getExperimentRunsInExperiment =
        FindExperimentRuns.newBuilder().setExperimentId(experiment.getId()).build();

    FindExperimentRuns.Response experimentRunResponse =
        experimentRunServiceStub.findExperimentRuns(getExperimentRunsInExperiment);
    assertEquals(
        0,
        experimentRunResponse.getExperimentRunsCount(),
        "ExperimentRuns count not match with expected experimentRun count");

    LOGGER.info(
        "Delete ExperimentRun by parent entities owner test stop................................");
  }

  @Test
  public void versioningAtExperimentRunCreateTest()
      throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("Versioning ExperimentRun test start................................");

    long repoId = createRepository(versioningServiceBlockingStub, "Repo-" + new Date().getTime());
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);
    Commit commit =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(111)
            .addParentShas(getBranchResponse.getCommit().getCommitSha())
            .build();
    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setCommit(commit)
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                    .addLocation("dataset")
                    .addLocation("train")
                    .build())
            .build();
    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
    Map<String, Location> locationMap = new HashMap<>();
    locationMap.put(
        "location-2", Location.newBuilder().addLocation("dataset").addLocation("train").build());
    createExperimentRunRequest =
        createExperimentRunRequest.toBuilder()
            .setVersionedInputs(
                VersioningEntry.newBuilder()
                    .setRepositoryId(repoId)
                    .setCommit(commitResponse.getCommit().getCommitSha())
                    .putAllKeyLocationMap(locationMap)
                    .build())
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        createExperimentRunRequest.getName(),
        experimentRun.getName(),
        "ExperimentRun name not match with expected ExperimentRun name");
    assertEquals(
        createExperimentRunRequest.getVersionedInputs(),
        experimentRun.getVersionedInputs(),
        "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response getExperimentRunByIdRes =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    assertEquals(
        createExperimentRunRequest.getVersionedInputs(),
        getExperimentRunByIdRes.getExperimentRun().getVersionedInputs(),
        "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    assertTrue(deleteResult.getStatus());

    LOGGER.info("Versioning ExperimentRun test stop................................");
  }

  @Test
  public void versioningAtExperimentRunCreateNegativeTest() throws ModelDBException {
    LOGGER.info("Versioning ExperimentRun negative test start................................");

    long repoId = createRepository(versioningServiceBlockingStub, "Repo-" + new Date().getTime());
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
    Map<String, Location> locationMap = new HashMap<>();
    locationMap.put(
        "location-2", Location.newBuilder().addLocation("dataset").addLocation("train").build());
    createExperimentRunRequest =
        createExperimentRunRequest.toBuilder()
            .setVersionedInputs(
                VersioningEntry.newBuilder()
                    .setRepositoryId(123456)
                    .setCommit("xyz")
                    .putAllKeyLocationMap(locationMap)
                    .build())
            .build();
    try {
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    } catch (StatusRuntimeException e) {
      assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
    }

    try {
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setName("test-" + Calendar.getInstance().getTimeInMillis())
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit("xyz")
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .build();
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      assertEquals(Status.Code.NOT_FOUND, e.getStatus().getCode());
    }

    try {
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setName("test-" + Calendar.getInstance().getTimeInMillis())
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(getBranchResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .build();
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode());
    }

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    assertTrue(deleteResult.getStatus());

    LOGGER.info("Versioning ExperimentRun negative test stop................................");
  }

  @Test
  public void logGetVersioningExperimentRunCreateTest()
      throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("Log and Get Versioning ExperimentRun test start................................");

    long repoId;
    if (testConfig.isPermissionV2Enabled() && testConfig.isPopulateConnectionsBasedOnPrivileges()) {
      repoId =
          RepositoryTest.createRepositoryWithWorkspace(
              versioningServiceBlockingStub,
              "Repo-" + new Date().getTime(),
              testUser1Workspace.getOrgId() + "/" + testUser1Workspace.getName());
    } else {
      repoId =
          RepositoryTest.createRepository(
              versioningServiceBlockingStub, "Repo-" + new Date().getTime());
    }
    try {
      GetBranchRequest getBranchRequest =
          GetBranchRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
              .setBranch(ModelDBConstants.MASTER_BRANCH)
              .build();
      GetBranchRequest.Response getBranchResponse =
          versioningServiceBlockingStub.getBranch(getBranchRequest);
      Commit commit =
          Commit.newBuilder()
              .setMessage("this is the test commit message")
              .setDateCreated(111)
              .addParentShas(getBranchResponse.getCommit().getCommitSha())
              .build();
      CreateCommitRequest createCommitRequest =
          CreateCommitRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
              .setCommit(commit)
              .addBlobs(
                  BlobExpanded.newBuilder()
                      .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                      .addLocation("dataset")
                      .addLocation("train")
                      .build())
              .addBlobs(
                  BlobExpanded.newBuilder()
                      .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                      .addLocation("dataset_1")
                      .addLocation("train")
                      .build())
              .build();
      CreateCommitRequest.Response commitResponse =
          versioningServiceBlockingStub.createCommit(createCommitRequest);

      Map<String, Location> locationMap = new HashMap<>();
      locationMap.put(
          "location-2", Location.newBuilder().addLocation("dataset").addLocation("train").build());

      LogVersionedInput logVersionedInput =
          LogVersionedInput.newBuilder()
              .setId(experimentRun.getId())
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .build();
      experimentRunServiceStub.logVersionedInput(logVersionedInput);

      LogVersionedInput logVersionedInputFail =
          LogVersionedInput.newBuilder()
              .setId(experimentRun.getId())
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getParentShasList().get(0))
                      .build())
              .build();
      try {
        experimentRunServiceStub.logVersionedInput(logVersionedInputFail);
        Assertions.fail();
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.warn(
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
      }

      GetExperimentRunById getExperimentRunById =
          GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
      Response response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
      assertEquals(
          logVersionedInput.getVersionedInputs(),
          response.getExperimentRun().getVersionedInputs(),
          "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");

      locationMap.put(
          "location-1",
          Location.newBuilder().addLocation("dataset_1").addLocation("train").build());
      logVersionedInput =
          LogVersionedInput.newBuilder()
              .setId(experimentRun.getId())
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .build();
      experimentRunServiceStub.logVersionedInput(logVersionedInput);

      response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
      assertEquals(
          logVersionedInput.getVersionedInputs(),
          response.getExperimentRun().getVersionedInputs(),
          "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");

      GetVersionedInput getVersionedInput =
          GetVersionedInput.newBuilder().setId(experimentRun.getId()).build();
      GetVersionedInput.Response getVersionedInputResponse =
          experimentRunServiceStub.getVersionedInputs(getVersionedInput);
      assertEquals(
          logVersionedInput.getVersionedInputs(),
          getVersionedInputResponse.getVersionedInputs(),
          "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");

      if (testConfig.hasAuth()) {
        if (isRunningIsolated()) {
          when(uac.getUACService().getCurrentUser(any()))
              .thenReturn(Futures.immediateFuture(testUser2));
          when(uac.getAuthzService().isSelfAllowed(any()))
              .thenReturn(
                  Futures.immediateFuture(
                      IsSelfAllowed.Response.newBuilder().setAllowed(false).build()));
        }
        getVersionedInput = GetVersionedInput.newBuilder().setId(experimentRun.getId()).build();
        getVersionedInputResponse =
            experimentRunServiceStubClient2.getVersionedInputs(getVersionedInput);
        if (testConfig.isPopulateConnectionsBasedOnPrivileges()) {
          assertTrue(
              getVersionedInputResponse.getVersionedInputs().getKeyLocationMapMap().isEmpty(),
              "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");
        } else {
          Assertions.assertFalse(
              getVersionedInputResponse.getVersionedInputs().getKeyLocationMapMap().isEmpty(),
              "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");
        }
      }
    } finally {
      if (isRunningIsolated()) {
        when(uac.getUACService().getCurrentUser(any()))
            .thenReturn(Futures.immediateFuture(testUser1));
        when(uac.getAuthzService().isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(true).build()));
      }
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      assertTrue(deleteResult.getStatus());
    }

    LOGGER.info("Log and Get Versioning ExperimentRun test stop................................");
  }

  @Test
  public void versioningWithoutLocationAtExperimentRunCreateTest()
      throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info(
        "Versioning without Locations ExperimentRun test start................................");

    long repoId = createRepository(versioningServiceBlockingStub, "Repo-" + new Date().getTime());
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);
    Commit commit =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(111)
            .addParentShas(getBranchResponse.getCommit().getCommitSha())
            .build();
    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setCommit(commit)
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                    .addLocation("dataset")
                    .addLocation("train")
                    .build())
            .build();
    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
    createExperimentRunRequest =
        createExperimentRunRequest.toBuilder()
            .setVersionedInputs(
                VersioningEntry.newBuilder()
                    .setRepositoryId(repoId)
                    .setCommit(commitResponse.getCommit().getCommitSha())
                    .build())
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        createExperimentRunRequest.getName(),
        experimentRun.getName(),
        "ExperimentRun name not match with expected ExperimentRun name");
    assertEquals(
        createExperimentRunRequest.getVersionedInputs(),
        experimentRun.getVersionedInputs(),
        "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response getExperimentRunByIdRes =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    assertEquals(
        createExperimentRunRequest.getVersionedInputs(),
        getExperimentRunByIdRes.getExperimentRun().getVersionedInputs(),
        "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");
    assertEquals(
        0,
        getExperimentRunByIdRes.getExperimentRun().getVersionedInputs().getKeyLocationMapCount(),
        "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    assertTrue(deleteResult.getStatus());

    LOGGER.info(
        "Versioning without Locations ExperimentRun test stop................................");
  }

  @Test
  public void logGetVersioningWithoutLocationsExperimentRunCreateTest()
      throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info(
        "Log and Get Versioning without locations ExperimentRun test start................................");

    List<String> experimentRunIds = new ArrayList<>();
    long repoId = createRepository(versioningServiceBlockingStub, "Repo-" + new Date().getTime());
    try {
      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(experimentRun.getId());
      LOGGER.info("ExperimentRun created successfully");
      assertEquals(
          createExperimentRunRequest.getName(),
          experimentRun.getName(),
          "ExperimentRun name not match with expected ExperimentRun name");
      Assertions.assertFalse(
          createExperimentRunRequest.hasVersionedInputs(),
          "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");

      GetBranchRequest getBranchRequest =
          GetBranchRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
              .setBranch(ModelDBConstants.MASTER_BRANCH)
              .build();
      GetBranchRequest.Response getBranchResponse =
          versioningServiceBlockingStub.getBranch(getBranchRequest);
      Commit commit =
          Commit.newBuilder()
              .setMessage("this is the test commit message")
              .setDateCreated(111)
              .addParentShas(getBranchResponse.getCommit().getCommitSha())
              .build();
      CreateCommitRequest createCommitRequest =
          CreateCommitRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
              .setCommit(commit)
              .addBlobs(
                  BlobExpanded.newBuilder()
                      .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                      .addLocation("dataset")
                      .addLocation("train")
                      .build())
              .build();
      CreateCommitRequest.Response commitResponse =
          versioningServiceBlockingStub.createCommit(createCommitRequest);

      LogVersionedInput logVersionedInput =
          LogVersionedInput.newBuilder()
              .setId(experimentRun.getId())
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .build())
              .build();
      experimentRunServiceStub.logVersionedInput(logVersionedInput);

      GetExperimentRunById getExperimentRunById =
          GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
      GetExperimentRunById.Response response =
          experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
      assertEquals(
          logVersionedInput.getVersionedInputs(),
          response.getExperimentRun().getVersionedInputs(),
          "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");

      GetVersionedInput getVersionedInput =
          GetVersionedInput.newBuilder().setId(experimentRun.getId()).build();
      GetVersionedInput.Response getVersionedInputResponse =
          experimentRunServiceStub.getVersionedInputs(getVersionedInput);
      assertEquals(
          logVersionedInput.getVersionedInputs(),
          getVersionedInputResponse.getVersionedInputs(),
          "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");
      assertEquals(
          0,
          getVersionedInputResponse.getVersionedInputs().getKeyLocationMapCount(),
          "ExperimentRun versioningInput not match with expected ExperimentRun versioningInput");
    } finally {
      for (String runId : experimentRunIds) {
        DeleteExperimentRun deleteExperimentRun =
            DeleteExperimentRun.newBuilder().setId(runId).build();
        DeleteExperimentRun.Response deleteExperimentRunResponse =
            experimentRunServiceStub.deleteExperimentRun(deleteExperimentRun);
        assertTrue(deleteExperimentRunResponse.getStatus());
      }

      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      assertTrue(deleteResult.getStatus());
    }

    LOGGER.info(
        "Log and Get Versioning without locations ExperimentRun test stop................................");
  }

  @Test
  public void findExperimentRunsHyperparameterWithRepository()
      throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("FindExperimentRuns test start................................");

    long repoId;
    if (testConfig.isPermissionV2Enabled()) {
      repoId =
          RepositoryTest.createRepositoryWithWorkspace(
              versioningServiceBlockingStub,
              "Repo-" + new Date().getTime(),
              getWorkspaceNameUser1());
    } else {
      repoId =
          RepositoryTest.createRepository(
              versioningServiceBlockingStub, "Repo-" + new Date().getTime());
    }
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);
    Commit commit =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(111)
            .addParentShas(getBranchResponse.getCommit().getCommitSha())
            .build();
    Location location1 = Location.newBuilder().addLocation("dataset").addLocation("train").build();
    Location location2 =
        Location.newBuilder().addLocation("test-1").addLocation("test1.json").build();
    Location location3 =
        Location.newBuilder().addLocation("test-2").addLocation("test2.json").build();
    Location location4 =
        Location.newBuilder().addLocation("test-location-4").addLocation("test4.json").build();

    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setCommit(commit)
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                    .addAllLocation(location1.getLocationList())
                    .build())
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getBlob(Blob.ContentCase.CONFIG))
                    .addAllLocation(location2.getLocationList())
                    .build())
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                    .addAllLocation(location3.getLocationList())
                    .build())
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getHyperparameterConfigBlob(0.14F, 0.10F))
                    .addAllLocation(location4.getLocationList())
                    .build())
            .build();
    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitResponse.getCommit();

    List<String> experimentRunIds = new ArrayList<>();
    try {
      Map<String, Location> locationMap = new HashMap<>();
      locationMap.put("location-1", location1);

      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment.getId(), "ExperimentRun-1-" + new Date().getTime());
      KeyValue hyperparameter1 = generateNumericKeyValue("C", 0.0001);
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .addHyperparameters(hyperparameter1)
              .build();
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      experimentRunIds.add(createExperimentRunResponse.getExperimentRun().getId());
      LOGGER.info("ExperimentRun created successfully");

      locationMap.put("location-2", location2);

      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment.getId(), "ExperimentRun-2-" + new Date().getTime());
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      LOGGER.info("ExperimentRun created successfully");
      ExperimentRun experimentRunConfig1 = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(experimentRunConfig1.getId());

      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment2.getId(), "ExperimentRun-" + new Date().getTime());
      hyperparameter1 = generateNumericKeyValue("C", 0.0001);
      Map<String, Location> locationMap2 = new HashMap<>();
      locationMap2.put("location-4", location4);
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap2)
                      .build())
              .addHyperparameters(hyperparameter1)
              .build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      LOGGER.info("ExperimentRun created successfully");
      ExperimentRun experimentRunConfig2 = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(experimentRunConfig2.getId());

      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment2.getId(), "ExperimentRun-" + new Date().getTime());
      hyperparameter1 = generateNumericKeyValue("C", 1e-6);
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder().addHyperparameters(hyperparameter1).build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      experimentRunIds.add(createExperimentRunResponse.getExperimentRun().getId());
      LOGGER.info("ExperimentRun created successfully");

      Value hyperparameterFilter = Value.newBuilder().setNumberValue(0.0001).build();
      KeyValueQuery keyValueQuery =
          KeyValueQuery.newBuilder()
              .setKey("hyperparameters.train")
              .setValue(hyperparameterFilter)
              .setOperator(Operator.GTE)
              .setValueType(ValueType.NUMBER)
              .build();

      FindExperimentRuns findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setProjectId(project.getId())
              .addPredicates(keyValueQuery)
              .setAscending(false)
              .setIdsOnly(false)
              .build();

      FindExperimentRuns.Response response =
          experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

      assertEquals(
          2,
          response.getTotalRecords(),
          "Total records count not matched with expected records count");
      assertEquals(
          2,
          response.getExperimentRunsCount(),
          "ExperimentRun count not match with expected experimentRun count");
      assertEquals(
          experimentRunConfig2.getId(),
          response.getExperimentRuns(0).getId(),
          "ExperimentRun count not match with expected experimentRun count");
      for (ExperimentRun exprRun : response.getExperimentRunsList()) {
        for (KeyValue kv : exprRun.getHyperparametersList()) {
          if (kv.getKey().equals("train")) {
            assertTrue(kv.getValue().getNumberValue() > 0.0001, "Value should be GTE 0.0001 " + kv);
          }
        }
      }

      AddCollaboratorRequest addCollaboratorRequest;
      AddCollaboratorRequest.Response addCollaboratorResponse;
      if (testConfig.hasAuth()) {
        if (isRunningIsolated()) {
          when(uac.getUACService().getCurrentUser(any()))
              .thenReturn(Futures.immediateFuture(testUser2));
          mockGetResourcesForAllProjects(Map.of(project.getId(), project), testUser2);
          when(uac.getCollaboratorService().getResourcesSpecialPersonalWorkspace(any()))
              .thenReturn(
                  Futures.immediateFuture(
                      GetResources.Response.newBuilder()
                          .addItem(
                              GetResourcesResponseItem.newBuilder()
                                  .setVisibility(ResourceVisibility.PRIVATE)
                                  .setResourceType(
                                      ResourceType.newBuilder()
                                          .setModeldbServiceResourceType(
                                              ModelDBServiceResourceTypes.PROJECT)
                                          .build())
                                  .setOwnerId(testUser2.getVertaInfo().getDefaultWorkspaceId())
                                  .setWorkspaceId(testUser2.getVertaInfo().getDefaultWorkspaceId())
                                  .build())
                          .build()));
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
        } else if (testConfig.isPermissionV2Enabled()) {
          if (testConfig.isPopulateConnectionsBasedOnPrivileges()) {
            createAndGetRole(
                authServiceChannelServiceUser,
                organizationId,
                Optional.of(roleIdUser1),
                Set.of(ResourceTypeV2.PROJECT));
            var groupStub = GroupServiceGrpc.newBlockingStub(authServiceChannelServiceUser);
            groupStub.addUsers(
                AddGroupUsers.newBuilder()
                    .addUserId(testUser2.getVertaInfo().getUserId())
                    .setGroupId(groupIdUser1)
                    .setOrgId(organizationId)
                    .build());
          } else {
            createAndGetRole(
                authServiceChannelServiceUser,
                organizationId,
                Optional.of(roleIdUser1),
                Set.of(ResourceTypeV2.PROJECT, ResourceTypeV2.DATASET));
            var groupStub = GroupServiceGrpc.newBlockingStub(authServiceChannelServiceUser);
            groupStub.addUsers(
                AddGroupUsers.newBuilder()
                    .addUserId(testUser2.getVertaInfo().getUserId())
                    .setGroupId(groupIdUser1)
                    .setOrgId(organizationId)
                    .build());
          }
        } else {
          addCollaboratorRequest =
              AddCollaboratorRequest.newBuilder()
                  .setShareWith(authClientInterceptor.getClient2Email())
                  .setPermission(
                      CollaboratorPermissions.newBuilder()
                          .setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_ONLY)
                          .build())
                  .setAuthzEntityType(EntitiesEnum.EntitiesTypes.USER)
                  .addEntityIds(project.getId())
                  .build();
          addCollaboratorResponse =
              collaboratorServiceStubClient1.addOrUpdateProjectCollaborator(addCollaboratorRequest);
          LOGGER.info(
              "Project Collaborator added in server : " + addCollaboratorResponse.getStatus());
          assertTrue(addCollaboratorResponse.getStatus());
        }

        findExperimentRuns =
            FindExperimentRuns.newBuilder()
                .setProjectId(project.getId())
                .addPredicates(keyValueQuery)
                .setAscending(false)
                .setIdsOnly(false)
                .build();

        response = experimentRunServiceStubClient2.findExperimentRuns(findExperimentRuns);
        assertEquals(
            2,
            response.getTotalRecords(),
            "Total records count not matched with expected records count");
        assertEquals(
            2,
            response.getExperimentRunsCount(),
            "ExperimentRun count not match with expected experimentRun count");
        if (testConfig.isPopulateConnectionsBasedOnPrivileges()) {
          assertEquals(
              1,
              response.getExperimentRuns(0).getHyperparametersCount(),
              "ExperimentRun hyperparameters count not match with expected experimentRun hyperparameters count");
        } else {
          assertEquals(
              1,
              response.getExperimentRuns(0).getHyperparametersCount(),
              "ExperimentRun hyperparameters count not match with expected experimentRun hyperparameters count");
        }

        if (isRunningIsolated()) {
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
                  Futures.immediateFuture(
                      GetSelfAllowedResources.Response.newBuilder()
                          .addResources(
                              Resources.newBuilder()
                                  .addResourceIds(String.valueOf(repoId))
                                  .setResourceType(
                                      ResourceType.newBuilder()
                                          .setModeldbServiceResourceType(
                                              ModelDBServiceResourceTypes.REPOSITORY)
                                          .build())
                                  .setService(Service.MODELDB_SERVICE)
                                  .build())
                          .build()));
        } else if (testConfig.isPermissionV2Enabled()) {
          createAndGetRole(
              authServiceChannelServiceUser,
              organizationId,
              Optional.of(roleIdUser1),
              Set.of(ResourceTypeV2.PROJECT, ResourceTypeV2.DATASET));
          var groupStub = GroupServiceGrpc.newBlockingStub(authServiceChannelServiceUser);
          groupStub.addUsers(
              AddGroupUsers.newBuilder()
                  .addUserId(testUser2.getVertaInfo().getUserId())
                  .setGroupId(groupIdUser1)
                  .setOrgId(organizationId)
                  .build());
        } else {
          addCollaboratorRequest =
              AddCollaboratorRequest.newBuilder()
                  .setShareWith(authClientInterceptor.getClient2Email())
                  .setPermission(
                      CollaboratorPermissions.newBuilder()
                          .setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_ONLY)
                          .build())
                  .setAuthzEntityType(EntitiesEnum.EntitiesTypes.USER)
                  .addEntityIds(String.valueOf(repoId))
                  .build();
          addCollaboratorResponse =
              collaboratorServiceStubClient1.addOrUpdateRepositoryCollaborator(
                  addCollaboratorRequest);
          LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
          assertTrue(addCollaboratorResponse.getStatus());
        }

        findExperimentRuns =
            FindExperimentRuns.newBuilder()
                .setProjectId(project.getId())
                .addPredicates(keyValueQuery)
                .setAscending(false)
                .setIdsOnly(false)
                .build();

        response = experimentRunServiceStubClient2.findExperimentRuns(findExperimentRuns);
        assertEquals(
            2,
            response.getTotalRecords(),
            "Total records count not matched with expected records count");
        assertEquals(
            2,
            response.getExperimentRunsCount(),
            "ExperimentRun count not match with expected experimentRun count");
        if (testConfig.isPopulateConnectionsBasedOnPrivileges()) {
          assertEquals(
              3,
              response.getExperimentRuns(0).getHyperparametersCount(),
              "ExperimentRun hyperparameters count not match with expected experimentRun hyperparameters count");
        } else {
          assertEquals(
              1,
              response.getExperimentRuns(0).getHyperparametersCount(),
              "ExperimentRun hyperparameters count not match with expected experimentRun hyperparameters count");
        }
      }
    } finally {
      if (testConfig.isPermissionV2Enabled() && !isRunningIsolated()) {
        createAndGetRole(
            authServiceChannelServiceUser,
            organizationId,
            Optional.of(roleIdUser1),
            Set.of(ResourceTypeV2.PROJECT, ResourceTypeV2.DATASET));
      }

      if (testConfig.isPermissionV2Enabled() && isRunningIsolated()) {
        when(uac.getWorkspaceService().getWorkspaceById(any()))
            .thenReturn(
                Futures.immediateFuture(
                    Workspace.newBuilder()
                        .setId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                        .setUsername(testUser1.getVertaInfo().getUsername())
                        .build()));
      }
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      assertTrue(deleteResult.getStatus());

      for (String runId : experimentRunIds) {
        DeleteExperimentRun deleteExperimentRun =
            DeleteExperimentRun.newBuilder().setId(runId).build();
        DeleteExperimentRun.Response deleteExperimentRunResponse =
            experimentRunServiceStub.deleteExperimentRun(deleteExperimentRun);
        assertTrue(deleteExperimentRunResponse.getStatus());
      }
    }

    LOGGER.info("FindExperimentRuns test stop................................");
  }

  @Test
  public void findExperimentRunsHyperparameterWithSortKeyRepository()
      throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("FindExperimentRuns test start................................");

    long repoId =
        RepositoryTest.createRepository(
            versioningServiceBlockingStub, "Repo-" + new Date().getTime());
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);
    Commit commit =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(111)
            .addParentShas(getBranchResponse.getCommit().getCommitSha())
            .build();
    Location location1 = Location.newBuilder().addLocation("dataset").addLocation("train").build();
    Location location2 =
        Location.newBuilder().addLocation("test-1").addLocation("test1.json").build();
    Location location3 =
        Location.newBuilder().addLocation("test-2").addLocation("test2.json").build();
    Location location4 =
        Location.newBuilder().addLocation("test-location-4").addLocation("test4.json").build();

    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setCommit(commit)
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                    .addAllLocation(location1.getLocationList())
                    .build())
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getHyperparameterConfigBlob(3F, 3F))
                    .addAllLocation(location2.getLocationList())
                    .build())
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                    .addAllLocation(location3.getLocationList())
                    .build())
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getHyperparameterConfigBlob(5F, 2F))
                    .addAllLocation(location4.getLocationList())
                    .build())
            .build();
    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitResponse.getCommit();

    Map<String, Location> locationMap = new HashMap<>();
    locationMap.put("location-1", location1);

    List<String> experimentRunIds = new ArrayList<>();

    try {
      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
      KeyValue hyperparameter1 = generateNumericKeyValue("C", 0.0001);
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .addHyperparameters(hyperparameter1)
              .build();
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      experimentRunIds.add(createExperimentRunResponse.getExperimentRun().getId());
      LOGGER.info("ExperimentRun created successfully");

      locationMap.put("location-2", location2);

      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      LOGGER.info("ExperimentRun created successfully");
      ExperimentRun experimentRunConfig1 = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(experimentRunConfig1.getId());

      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment2.getId(), "ExperimentRun-" + new Date().getTime());
      hyperparameter1 = generateNumericKeyValue("C", 0.0002);
      Map<String, Location> locationMap2 = new HashMap<>();
      locationMap2.put("location-4", location4);
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap2)
                      .build())
              .addHyperparameters(hyperparameter1)
              .build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      LOGGER.info("ExperimentRun created successfully");
      ExperimentRun experimentRunConfig2 = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(experimentRunConfig2.getId());

      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment2.getId(), "ExperimentRun-" + new Date().getTime());
      hyperparameter1 = generateNumericKeyValue("C", 0.0003);
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder().addHyperparameters(hyperparameter1).build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      experimentRunIds.add(createExperimentRunResponse.getExperimentRun().getId());
      LOGGER.info("ExperimentRun created successfully");

      try {
        Value hyperparameterFilter = Value.newBuilder().setNumberValue(1).build();
        KeyValueQuery keyValueQuery =
            KeyValueQuery.newBuilder()
                .setKey("hyperparameters.train")
                .setValue(hyperparameterFilter)
                .setOperator(Operator.GTE)
                .setValueType(ValueType.NUMBER)
                .build();

        FindExperimentRuns findExperimentRuns =
            FindExperimentRuns.newBuilder()
                .setProjectId(project.getId())
                .addPredicates(keyValueQuery)
                .setAscending(false)
                .setIdsOnly(false)
                .setSortKey("hyperparameters.train")
                .build();

        FindExperimentRuns.Response response =
            experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

        assertEquals(
            2,
            response.getTotalRecords(),
            "Total records count not matched with expected records count");
        assertEquals(
            2,
            response.getExperimentRunsCount(),
            "ExperimentRun count not match with expected experimentRun count");
        assertEquals(
            experimentRunConfig2.getId(),
            response.getExperimentRuns(0).getId(),
            "ExperimentRun count not match with expected experimentRun count");

        for (int index = 0; index < response.getExperimentRunsCount(); index++) {
          ExperimentRun exprRun = response.getExperimentRuns(index);
          for (KeyValue kv : exprRun.getHyperparametersList()) {
            if (kv.getKey().equals("C")) {
              assertTrue(
                  kv.getValue().getNumberValue() >= 0.0001, "Value should be GTE 0.0001 " + kv);
            } else if (kv.getKey().equals("train")) {
              if (index == 0) {
                assertEquals(
                    5.0F, kv.getValue().getNumberValue(), 0.0, "Value should be GTE 1 " + kv);
              } else {
                assertEquals(
                    3.0F, kv.getValue().getNumberValue(), 0.0, "Value should be GTE 1 " + kv);
              }
            }
          }
        }

        findExperimentRuns =
            FindExperimentRuns.newBuilder()
                .setProjectId(project.getId())
                .addPredicates(keyValueQuery)
                .setAscending(true)
                .setIdsOnly(false)
                .setSortKey("hyperparameters.train")
                .build();

        response = experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

        assertEquals(
            2,
            response.getTotalRecords(),
            "Total records count not matched with expected records count");
        assertEquals(
            2,
            response.getExperimentRunsCount(),
            "ExperimentRun count not match with expected experimentRun count");
        assertEquals(
            experimentRunConfig1.getId(),
            response.getExperimentRuns(0).getId(),
            "ExperimentRun count not match with expected experimentRun count");

        for (int index = 0; index < response.getExperimentRunsCount(); index++) {
          ExperimentRun exprRun = response.getExperimentRuns(index);
          for (KeyValue kv : exprRun.getHyperparametersList()) {
            if (kv.getKey().equals("C")) {
              assertTrue(
                  kv.getValue().getNumberValue() >= 0.0001, "Value should be GTE 0.0001 " + kv);
            } else if (kv.getKey().equals("train")) {
              if (index == 0) {
                assertEquals(
                    3.0F, kv.getValue().getNumberValue(), 0.0, "Value should be GTE 1 " + kv);
              } else {
                assertEquals(
                    5.0F, kv.getValue().getNumberValue(), 0.0, "Value should be GTE 1 " + kv);
              }
            }
          }
        }

        findExperimentRuns =
            FindExperimentRuns.newBuilder()
                .setProjectId(project.getId())
                .addPredicates(keyValueQuery)
                .setAscending(false)
                .setIdsOnly(false)
                .setPageLimit(1)
                .setPageNumber(1)
                .setSortKey("hyperparameters.train")
                .build();

        response = experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

        assertEquals(
            2,
            response.getTotalRecords(),
            "Total records count not matched with expected records count");
        assertEquals(
            1,
            response.getExperimentRunsCount(),
            "ExperimentRun count not match with expected experimentRun count");
        assertEquals(
            experimentRunConfig2.getId(),
            response.getExperimentRuns(0).getId(),
            "ExperimentRun count not match with expected experimentRun count");
        for (ExperimentRun exprRun : response.getExperimentRunsList()) {
          for (KeyValue kv : exprRun.getHyperparametersList()) {
            if (kv.getKey().equals("train")) {
              assertTrue(kv.getValue().getNumberValue() > 1, "Value should be GTE 1 " + kv);
            }
          }
        }

        Value oldHyperparameterFilter = Value.newBuilder().setNumberValue(0.0002).build();
        KeyValueQuery oldKeyValueQuery =
            KeyValueQuery.newBuilder()
                .setKey("hyperparameters.C")
                .setValue(oldHyperparameterFilter)
                .setOperator(Operator.GTE)
                .setValueType(ValueType.NUMBER)
                .build();

        findExperimentRuns =
            FindExperimentRuns.newBuilder()
                .setProjectId(project.getId())
                .addPredicates(oldKeyValueQuery)
                .setAscending(true)
                .setIdsOnly(false)
                .setSortKey("hyperparameters.C")
                .build();
        response = experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

        assertEquals(
            2,
            response.getTotalRecords(),
            "Total records count not matched with expected records count");

        for (int index = 0; index < response.getExperimentRunsCount(); index++) {
          ExperimentRun exprRun = response.getExperimentRuns(index);
          for (KeyValue kv : exprRun.getHyperparametersList()) {
            if (kv.getKey().equals("C")) {
              assertTrue(
                  kv.getValue().getNumberValue() > 0.0001, "Value should be GTE 0.0001 " + kv);
            }
          }
        }

        oldHyperparameterFilter = Value.newBuilder().setNumberValue(1).build();
        oldKeyValueQuery =
            KeyValueQuery.newBuilder()
                .setKey("hyperparameters.train")
                .setValue(oldHyperparameterFilter)
                .setOperator(Operator.GTE)
                .setValueType(ValueType.NUMBER)
                .build();

        findExperimentRuns =
            FindExperimentRuns.newBuilder()
                .setProjectId(project.getId())
                .addPredicates(oldKeyValueQuery)
                .setAscending(false)
                .setIdsOnly(false)
                .setSortKey("hyperparameters.C")
                .build();

        response = experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

        assertEquals(
            2,
            response.getTotalRecords(),
            "Total records count not matched with expected records count");

        for (int index = 0; index < response.getExperimentRunsCount(); index++) {
          ExperimentRun exprRun = response.getExperimentRuns(index);
          for (KeyValue kv : exprRun.getHyperparametersList()) {
            if (kv.getKey().equals("C")) {
              assertTrue(
                  kv.getValue().getNumberValue() > 0.0001, "Value should be GTE 0.0001 " + kv);
            }
          }
        }

        hyperparameterFilter = Value.newBuilder().setNumberValue(5).build();
        keyValueQuery =
            KeyValueQuery.newBuilder()
                .setKey("hyperparameters.train")
                .setValue(hyperparameterFilter)
                .setOperator(Operator.NE)
                .setValueType(ValueType.NUMBER)
                .build();

        findExperimentRuns =
            FindExperimentRuns.newBuilder()
                .setProjectId(project.getId())
                .addPredicates(keyValueQuery)
                .setAscending(false)
                .setIdsOnly(false)
                .setSortKey("hyperparameters.train")
                .build();

        response = experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

        assertEquals(
            3 + experimentRunMap.size(),
            response.getTotalRecords(),
            "Total records count not matched with expected records count");
        assertEquals(
            3 + experimentRunMap.size(),
            response.getExperimentRunsCount(),
            "ExperimentRun count not match with expected experimentRun count");

        // FIX ME: Fix findExperimentRun with string value predicate
        /*hyperparameterFilter = Value.newBuilder().setStringValue("abc").build();
        keyValueQuery =
            KeyValueQuery.newBuilder()
                .setKey("hyperparameters.C")
                .setValue(hyperparameterFilter)
                .setOperator(Operator.CONTAIN)
                .setValueType(ValueType.STRING)
                .build();

        findExperimentRuns =
            FindExperimentRuns.newBuilder()
                .setProjectId(project.getId())
                .addPredicates(keyValueQuery)
                .setAscending(false)
                .setIdsOnly(false)
                .setSortKey("hyperparameters.train")
                .build();

        response = experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

        assertEquals(
            "Total records count not matched with expected records count",
            1,
            response.getTotalRecords());
        assertEquals(
            "ExperimentRun count not match with expected experimentRun count",
            1,
            response.getExperimentRunsCount());*/

      } finally {
        DeleteRepositoryRequest deleteRepository =
            DeleteRepositoryRequest.newBuilder()
                .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId))
                .build();
        DeleteRepositoryRequest.Response deleteResult =
            versioningServiceBlockingStub.deleteRepository(deleteRepository);
        assertTrue(deleteResult.getStatus());
      }
    } finally {
      for (String runId : experimentRunIds) {
        DeleteExperimentRun deleteExperimentRun =
            DeleteExperimentRun.newBuilder().setId(runId).build();
        DeleteExperimentRun.Response deleteExperimentRunResponse =
            experimentRunServiceStub.deleteExperimentRun(deleteExperimentRun);
        assertTrue(deleteExperimentRunResponse.getStatus());
      }
    }

    LOGGER.info("FindExperimentRuns test stop................................");
  }

  @Test
  public void findExperimentRunsCodeConfigWithRepository()
      throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("FindExperimentRuns test start................................");

    long repoId =
        RepositoryTest.createRepository(
            versioningServiceBlockingStub, "Repo-" + new Date().getTime());
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);
    Commit commit =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(111)
            .addParentShas(getBranchResponse.getCommit().getCommitSha())
            .build();
    Location datasetLocation =
        Location.newBuilder().addLocation("dataset").addLocation("train").build();
    Location test1Location =
        Location.newBuilder().addLocation("test-1").addLocation("test1.json").build();
    Location test2Location =
        Location.newBuilder().addLocation("test-2").addLocation("test2.json").build();

    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setCommit(commit)
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                    .addAllLocation(datasetLocation.getLocationList())
                    .build())
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getBlob(Blob.ContentCase.CODE))
                    .addAllLocation(test1Location.getLocationList())
                    .build())
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(
                        Blob.newBuilder()
                            .setCode(
                                CodeBlob.newBuilder()
                                    .setGit(
                                        GitCodeBlob.newBuilder()
                                            .setBranch("abcd")
                                            .setRepo("Repo-" + new Date().getTime())
                                            .setHash(FileHasher.getSha(""))
                                            .setIsDirty(false)
                                            .setTag(
                                                "Tag-" + Calendar.getInstance().getTimeInMillis())
                                            .build())))
                    .addAllLocation(test2Location.getLocationList())
                    .build())
            .build();
    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitResponse.getCommit();

    List<String> experimentRunIds = new ArrayList<>();
    try {
      Map<String, Location> locationMap = new HashMap<>();
      locationMap.put("location-1", datasetLocation);

      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
      KeyValue hyperparameter1 = generateNumericKeyValue("C", 0.0001);
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .addHyperparameters(hyperparameter1)
              .build();
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      experimentRunIds.add(createExperimentRunResponse.getExperimentRun().getId());
      LOGGER.info("ExperimentRun created successfully");

      locationMap.put("location-2", test1Location);

      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun2 = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(experimentRun2.getId());
      LOGGER.info("ExperimentRun created successfully");

      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment2.getId(), "ExperimentRun-" + new Date().getTime());
      hyperparameter1 = generateNumericKeyValue("C", 0.0001);
      Map<String, Location> locationMap2 = new HashMap<>();
      locationMap2.put("location-111", test2Location);
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap2)
                      .build())
              .addHyperparameters(hyperparameter1)
              .build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun3 = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(experimentRun3.getId());
      LOGGER.info("ExperimentRun created successfully");

      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment2.getId(), "ExperimentRun-" + new Date().getTime());
      hyperparameter1 = generateNumericKeyValue("C", 1e-6);
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder().addHyperparameters(hyperparameter1).build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      experimentRunIds.add(createExperimentRunResponse.getExperimentRun().getId());
      LOGGER.info("ExperimentRun created successfully");

      if (isRunningIsolated()) {
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
                Futures.immediateFuture(
                    GetSelfAllowedResources.Response.newBuilder()
                        .addResources(
                            Resources.newBuilder()
                                .addResourceIds(String.valueOf(repoId))
                                .setResourceType(
                                    ResourceType.newBuilder()
                                        .setModeldbServiceResourceType(
                                            ModelDBServiceResourceTypes.REPOSITORY)
                                        .build())
                                .setService(Service.MODELDB_SERVICE)
                                .build())
                        .build()));
      }

      FindExperimentRuns findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setProjectId(project.getId())
              .setAscending(false)
              .setIdsOnly(false)
              .build();

      FindExperimentRuns.Response response =
          experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

      assertEquals(
          4 + experimentRunMap.size(),
          response.getTotalRecords(),
          "Total records count not matched with expected records count");
      assertEquals(
          4 + experimentRunMap.size(),
          response.getExperimentRunsCount(),
          "ExperimentRun count not match with expected experimentRun count");
      for (ExperimentRun exprRun : response.getExperimentRunsList()) {
        if (exprRun.getId().equals(experimentRun2.getId())) {
          String locationKey =
              ModelDBUtils.getLocationWithSlashOperator(test1Location.getLocationList());
          if (!testConfig.isPopulateConnectionsBasedOnPrivileges()) {
            Assertions.assertFalse(
                exprRun.containsCodeVersionFromBlob(locationKey), "Code blob should not empty");
          } else {
            assertTrue(
                exprRun.containsCodeVersionFromBlob(locationKey), "Code blob should not empty");
            Assertions.assertFalse(
                exprRun
                    .getCodeVersionFromBlobOrThrow(locationKey)
                    .getGitSnapshot()
                    .getFilepathsList()
                    .isEmpty(),
                "Expected code config not found in map");
          }
        } else if (exprRun.getId().equals(experimentRun3.getId())) {
          String locationKey =
              ModelDBUtils.getLocationWithSlashOperator(test2Location.getLocationList());
          if (!testConfig.isPopulateConnectionsBasedOnPrivileges()) {
            Assertions.assertFalse(
                exprRun.containsCodeVersionFromBlob(locationKey), "Code blob should not empty");
          } else {
            assertTrue(
                exprRun.containsCodeVersionFromBlob(locationKey), "Code blob should not empty");
            assertTrue(
                exprRun
                    .getCodeVersionFromBlobOrThrow(locationKey)
                    .getGitSnapshot()
                    .getFilepathsList()
                    .isEmpty(),
                "Expected code config not found in map");
          }
        }
      }

      GetExperimentRunById.Response getHydratedExperimentRunsResponse =
          experimentRunServiceStub.getExperimentRunById(
              GetExperimentRunById.newBuilder().setId(experimentRun2.getId()).build());
      ExperimentRun exprRun = getHydratedExperimentRunsResponse.getExperimentRun();
      String locationKey =
          ModelDBUtils.getLocationWithSlashOperator(test1Location.getLocationList());
      if (!testConfig.isPopulateConnectionsBasedOnPrivileges()) {
        Assertions.assertFalse(
            exprRun.containsCodeVersionFromBlob(locationKey), "Code blob should not empty");
      } else {
        assertTrue(exprRun.containsCodeVersionFromBlob(locationKey), "Code blob should not empty");
        Assertions.assertFalse(
            exprRun
                .getCodeVersionFromBlobOrThrow(locationKey)
                .getGitSnapshot()
                .getFilepathsList()
                .isEmpty(),
            "Expected code config not found in map");
      }
    } finally {
      if (isRunningIsolated()) {
        when(uac.getUACService().getCurrentUser(any()))
            .thenReturn(Futures.immediateFuture(testUser1));
        when(uac.getAuthzService().isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(true).build()));
      }

      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      assertTrue(deleteResult.getStatus());

      for (String runId : experimentRunIds) {
        DeleteExperimentRun deleteExperimentRun =
            DeleteExperimentRun.newBuilder().setId(runId).build();
        DeleteExperimentRun.Response deleteExperimentRunResponse =
            experimentRunServiceStub.deleteExperimentRun(deleteExperimentRun);
        assertTrue(deleteExperimentRunResponse.getStatus());
      }
    }

    LOGGER.info("FindExperimentRuns test stop................................");
  }

  @Test
  public void findExperimentRunsHyperparameterWithStringNumberFloat()
      throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("FindExperimentRuns test start................................");

    long repoId =
        RepositoryTest.createRepository(
            versioningServiceBlockingStub, "Repo-" + new Date().getTime());
    List<String> experimentRunIds = new ArrayList<>();
    try {
      GetBranchRequest getBranchRequest =
          GetBranchRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
              .setBranch(ModelDBConstants.MASTER_BRANCH)
              .build();
      GetBranchRequest.Response getBranchResponse =
          versioningServiceBlockingStub.getBranch(getBranchRequest);
      Commit commit =
          Commit.newBuilder()
              .setMessage("this is the test commit message")
              .setDateCreated(111)
              .addParentShas(getBranchResponse.getCommit().getCommitSha())
              .build();
      Location location1 =
          Location.newBuilder().addLocation("dataset").addLocation("train").build();
      Location location2 =
          Location.newBuilder().addLocation("test-1").addLocation("test1.json").build();
      Location location3 =
          Location.newBuilder().addLocation("test-2").addLocation("test2.json").build();
      Location location4 =
          Location.newBuilder().addLocation("test-location-4").addLocation("test4.json").build();

      CreateCommitRequest createCommitRequest =
          CreateCommitRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
              .setCommit(commit)
              .addBlobs(
                  BlobExpanded.newBuilder()
                      .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                      .addAllLocation(location1.getLocationList())
                      .build())
              .addBlobs(
                  BlobExpanded.newBuilder()
                      .setBlob(CommitTest.getHyperparameterConfigBlob(3F, 3F))
                      .addAllLocation(location2.getLocationList())
                      .build())
              .addBlobs(
                  BlobExpanded.newBuilder()
                      .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                      .addAllLocation(location3.getLocationList())
                      .build())
              .addBlobs(
                  BlobExpanded.newBuilder()
                      .setBlob(CommitTest.getHyperparameterConfigBlob(5.1F, 2.5F))
                      .addAllLocation(location4.getLocationList())
                      .build())
              .build();
      CreateCommitRequest.Response commitResponse =
          versioningServiceBlockingStub.createCommit(createCommitRequest);
      commitResponse.getCommit();

      Map<String, Location> locationMap = new HashMap<>();
      locationMap.put("location-1", location1);

      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
      KeyValue hyperparameter1 = generateNumericKeyValue("C1", 0.0001);
      KeyValue hyperparameter2 =
          KeyValue.newBuilder()
              .setKey("C")
              .setValue(Value.newBuilder().setStringValue("2.5").build())
              .build();
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .addHyperparameters(hyperparameter1)
              .addHyperparameters(hyperparameter2)
              .build();
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      experimentRunIds.add(createExperimentRunResponse.getExperimentRun().getId());
      LOGGER.info("ExperimentRun created successfully");

      locationMap.put("location-2", location2);

      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .addHyperparameters(
                  KeyValue.newBuilder()
                      .setKey("C")
                      .setValue(Value.newBuilder().setStringValue("3").build())
                      .build())
              .build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      experimentRunIds.add(createExperimentRunResponse.getExperimentRun().getId());
      LOGGER.info("ExperimentRun created successfully");

      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment2.getId(), "ExperimentRun-" + new Date().getTime());
      hyperparameter1 = generateNumericKeyValue("C", 0.0002);
      Map<String, Location> locationMap2 = new HashMap<>();
      locationMap2.put("location-4", location4);
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap2)
                      .build())
              .addHyperparameters(hyperparameter1)
              .addHyperparameters(
                  KeyValue.newBuilder()
                      .setKey("D")
                      .setValue(Value.newBuilder().setStringValue("test_hyper").build())
                      .build())
              .build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      LOGGER.info("ExperimentRun created successfully");
      ExperimentRun experimentRunConfig2 = createExperimentRunResponse.getExperimentRun();
      experimentRunIds.add(experimentRunConfig2.getId());

      createExperimentRunRequest =
          getCreateExperimentRunRequestSimple(
              project.getId(), experiment2.getId(), "ExperimentRun-" + new Date().getTime());
      hyperparameter1 = generateNumericKeyValue("C", 0.0003);
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder().addHyperparameters(hyperparameter1).build();
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      experimentRunIds.add(createExperimentRunResponse.getExperimentRun().getId());
      LOGGER.info("ExperimentRun created successfully");

      Value hyperparameterFilter = Value.newBuilder().setNumberValue(1).build();
      KeyValueQuery keyValueQuery =
          KeyValueQuery.newBuilder()
              .setKey("hyperparameters.c")
              .setValue(hyperparameterFilter)
              .setOperator(Operator.GTE)
              .setValueType(ValueType.NUMBER)
              .build();

      FindExperimentRuns findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setProjectId(project.getId())
              .addPredicates(keyValueQuery)
              .setAscending(false)
              .setIdsOnly(false)
              .setSortKey("hyperparameters.train")
              .build();

      experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

      keyValueQuery =
          KeyValueQuery.newBuilder()
              .setKey("hyperparameters.d")
              .setValue(Value.newBuilder().setStringValue("test_hyper").build())
              .setOperator(Operator.EQ)
              .setValueType(ValueType.STRING)
              .build();
      findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setProjectId(project.getId())
              .addPredicates(keyValueQuery)
              .setAscending(true)
              .setIdsOnly(false)
              .setSortKey("hyperparameters.train")
              .build();

      experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

      keyValueQuery =
          KeyValueQuery.newBuilder()
              .setKey("hyperparameters.c")
              .setValue(Value.newBuilder().setNumberValue(2.5).build())
              .setOperator(Operator.EQ)
              .setValueType(ValueType.STRING)
              .build();
      findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setProjectId(project.getId())
              .addPredicates(keyValueQuery)
              .setAscending(false)
              .setIdsOnly(false)
              .setSortKey("hyperparameters.train")
              .build();

      experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

      KeyValueQuery oldKeyValueQuery =
          KeyValueQuery.newBuilder()
              .setKey("hyperparameters.C")
              .setValue(Value.newBuilder().setNumberValue(0.0002).build())
              .setOperator(Operator.GTE)
              .setValueType(ValueType.NUMBER)
              .build();

      findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setProjectId(project.getId())
              .addPredicates(oldKeyValueQuery)
              .setAscending(true)
              .setIdsOnly(false)
              .setSortKey("hyperparameters.C")
              .build();

      experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

      Value oldHyperparameterFilter = Value.newBuilder().setNumberValue(1).build();
      oldKeyValueQuery =
          KeyValueQuery.newBuilder()
              .setKey("hyperparameters.train")
              .setValue(oldHyperparameterFilter)
              .setOperator(Operator.GTE)
              .setValueType(ValueType.NUMBER)
              .build();

      findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setProjectId(project.getId())
              .addPredicates(oldKeyValueQuery)
              .setAscending(false)
              .setIdsOnly(false)
              .setSortKey("hyperparameters.C")
              .build();

      FindExperimentRuns.Response response =
          experimentRunServiceStub.findExperimentRuns(findExperimentRuns);

      assertEquals(
          2,
          response.getTotalRecords(),
          "Total records count not matched with expected records count");

      for (int index = 0; index < response.getExperimentRunsCount(); index++) {
        ExperimentRun exprRun = response.getExperimentRuns(index);
        for (KeyValue kv : exprRun.getHyperparametersList()) {
          if (kv.getKey().equals("C")) {
            assertTrue(
                (kv.getValue().getKindCase() == KindCase.STRING_VALUE
                        ? Double.parseDouble(kv.getValue().getStringValue())
                        : kv.getValue().getNumberValue())
                    > 0.0001,
                "Value should be GTE 0.0001 " + kv);
          }
        }
      }
    } finally {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      assertTrue(deleteResult.getStatus());

      for (String runId : experimentRunIds) {
        DeleteExperimentRun deleteExperimentRun =
            DeleteExperimentRun.newBuilder().setId(runId).build();
        DeleteExperimentRun.Response deleteExperimentRunResponse =
            experimentRunServiceStub.deleteExperimentRun(deleteExperimentRun);
        assertTrue(deleteExperimentRunResponse.getStatus());
      }
    }

    LOGGER.info("FindExperimentRuns test stop................................");
  }

  @Test
  public void findExperimentRunsByDatasetVersionId() {
    LOGGER.info("FindExperimentRuns test start................................");

    Map<String, ExperimentRun> experimentRunMap = new HashMap<>();

    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequestSimple(
            project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
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
            project.getId(), experiment.getId(), "ExperimentRun-" + new Date().getTime());
    createExperimentRunRequest = createExperimentRunRequest.toBuilder().build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun12 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun12.getId(), experimentRun12);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        createExperimentRunRequest.getName(),
        experimentRun12.getName(),
        "ExperimentRun name not match with expected ExperimentRun name");

    createExperimentRunRequest =
        getCreateExperimentRunRequestSimple(
            project.getId(), experiment2.getId(), "ExperimentRun-" + new Date().getTime());
    hyperparameter1 = generateNumericKeyValue("C", 0.0001);
    createExperimentRunRequest =
        createExperimentRunRequest.toBuilder().addHyperparameters(hyperparameter1).build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun21 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun21.getId(), experimentRun21);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        createExperimentRunRequest.getName(),
        experimentRun21.getName(),
        "ExperimentRun name not match with expected ExperimentRun name");

    createExperimentRunRequest =
        getCreateExperimentRunRequestSimple(
            project.getId(), experiment2.getId(), "ExperimentRun-" + new Date().getTime());
    hyperparameter1 = generateNumericKeyValue("C", 1e-6);
    createExperimentRunRequest =
        createExperimentRunRequest.toBuilder().addHyperparameters(hyperparameter1).build();
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun22 = createExperimentRunResponse.getExperimentRun();
    experimentRunMap.put(experimentRun22.getId(), experimentRun22);
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        createExperimentRunRequest.getName(),
        experimentRun22.getName(),
        "ExperimentRun name not match with expected ExperimentRun name");

    List<Dataset> datasetList = new ArrayList<>();
    CreateDataset createDatasetRequest =
        DatasetTest.getDatasetRequestForOtherTests("Dataset-" + new Date().getTime());
    if (testConfig.isPermissionV2Enabled()) {
      createDatasetRequest =
          createDatasetRequest.toBuilder().setWorkspaceName(getWorkspaceNameUser1()).build();
    }
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset1 = createDatasetResponse.getDataset();
    datasetList.add(dataset1);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    assertEquals(
        createDatasetRequest.getName(),
        dataset1.getName(),
        "Dataset name not match with expected dataset name");

    createDatasetRequest =
        DatasetTest.getDatasetRequestForOtherTests("rental_TEXT_train_data_1.csv");
    if (testConfig.isPermissionV2Enabled()) {
      createDatasetRequest =
          createDatasetRequest.toBuilder().setWorkspaceName(getWorkspaceNameUser1()).build();
    }
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset2 = createDatasetResponse.getDataset();
    datasetList.add(dataset2);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    assertEquals(
        createDatasetRequest.getName(),
        dataset2.getName(),
        "Dataset name not match with expected dataset name");

    if (isRunningIsolated()) {
      mockGetResourcesForAllDatasets(
          Map.of(dataset1.getId(), dataset1, dataset2.getId(), dataset2), testUser1);
    }

    List<String> datasetVersionIds = new ArrayList<>();
    // Create two datasetVersion of above datasetVersion
    CreateDatasetVersion createDatasetVersionRequest =
        DatasetVersionTest.getDatasetVersionRequest(dataset1.getId());
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
        createDatasetVersionRequest.toBuilder()
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

    // datasetVersion2 of above datasetVersion
    createDatasetVersionRequest = DatasetVersionTest.getDatasetVersionRequest(dataset2.getId());
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
        createDatasetVersionRequest.toBuilder()
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

    try {

      Map<String, Artifact> artifactMap = new HashMap<>();
      for (Artifact existingDataset : experimentRun11.getDatasetsList()) {
        artifactMap.put(existingDataset.getKey(), existingDataset);
      }
      List<Artifact> artifacts = new ArrayList<>();
      Artifact artifact1 =
          Artifact.newBuilder()
              .setKey("Google Pay datasets_1")
              .setPath("This is new added data artifact type in Google Pay datasets")
              .setArtifactType(ArtifactType.DATA)
              .setLinkedArtifactId(datasetVersion1.getId())
              .setUploadCompleted(
                  !testConfig
                      .getArtifactStoreConfig()
                      .getArtifactStoreType()
                      .equals(ArtifactStoreConfig.S3_TYPE_STORE))
              .build();
      artifacts.add(artifact1);
      artifactMap.put(artifact1.getKey(), artifact1);
      Artifact artifact2 =
          Artifact.newBuilder()
              .setKey("Google Pay datasets_2")
              .setPath("This is new added data artifact type in Google Pay datasets")
              .setArtifactType(ArtifactType.DATA)
              .setLinkedArtifactId(datasetVersion2.getId())
              .setUploadCompleted(
                  !testConfig
                      .getArtifactStoreConfig()
                      .getArtifactStoreType()
                      .equals(ArtifactStoreConfig.S3_TYPE_STORE))
              .build();
      artifacts.add(artifact2);
      artifactMap.put(artifact2.getKey(), artifact2);

      LogDatasets logDatasetRequest =
          LogDatasets.newBuilder().setId(experimentRun11.getId()).addAllDatasets(artifacts).build();

      experimentRunServiceStub.logDatasets(logDatasetRequest);

      GetExperimentRunById getExperimentRunById =
          GetExperimentRunById.newBuilder().setId(experimentRun11.getId()).build();
      GetExperimentRunById.Response getExperimentRunByIdResponse =
          experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
      LOGGER.info("LogDataset Response : \n" + getExperimentRunByIdResponse.getExperimentRun());

      for (Artifact datasetArtifact :
          getExperimentRunByIdResponse.getExperimentRun().getDatasetsList()) {
        assertEquals(
            artifactMap.get(datasetArtifact.getKey()).getKey(),
            datasetArtifact.getKey(),
            "Experiment datasets not match with expected datasets");
      }
      experimentRun11 = getExperimentRunByIdResponse.getExperimentRun();

      GetExperimentRunsByDatasetVersionId getExperimentRunsByDatasetVersionId =
          GetExperimentRunsByDatasetVersionId.newBuilder()
              .setDatasetVersionId(datasetVersion1.getId())
              .build();

      GetExperimentRunsByDatasetVersionId.Response response =
          experimentRunServiceStub.getExperimentRunsByDatasetVersionId(
              getExperimentRunsByDatasetVersionId);

      assertEquals(
          1,
          response.getTotalRecords(),
          "Total records count not matched with expected records count");
      assertEquals(
          1,
          response.getExperimentRunsCount(),
          "ExperimentRun count not match with expected experimentRun count");
      assertEquals(
          experimentRun11,
          response.getExperimentRuns(0),
          "ExperimentRun not match with expected experimentRun");

      if (testConfig.hasAuth()) {
        if (isRunningIsolated()) {
          when(uac.getUACService().getCurrentUser(any()))
              .thenReturn(Futures.immediateFuture(testUser2));
          when(uac.getAuthzService().isSelfAllowed(any()))
              .thenReturn(
                  Futures.immediateFuture(
                      IsSelfAllowed.Response.newBuilder().setAllowed(false).build()));
        } else {
          if (testConfig.isPopulateConnectionsBasedOnPrivileges()) {
            createAndGetRole(
                authServiceChannelServiceUser,
                organizationId,
                Optional.of(roleIdUser1),
                Set.of(ResourceTypeV2.PROJECT));
          }

          var groupStub = GroupServiceGrpc.newBlockingStub(authServiceChannelServiceUser);
          groupStub.addUsers(
              AddGroupUsers.newBuilder()
                  .addUserId(testUser2.getVertaInfo().getUserId())
                  .setGroupId(groupIdUser1)
                  .setOrgId(organizationId)
                  .build());
        }
        response =
            experimentRunServiceStubClient2.getExperimentRunsByDatasetVersionId(
                getExperimentRunsByDatasetVersionId);

        assertEquals(1, response.getTotalRecords());
        assertEquals(1, response.getExperimentRunsCount());
        if (testConfig.isPopulateConnectionsBasedOnPrivileges()) {
          assertEquals(0, response.getExperimentRuns(0).getDatasetsCount());
        } else {
          assertEquals(2, response.getExperimentRuns(0).getDatasetsCount());
        }
      }
    } finally {
      if (!isRunningIsolated()) {
        createAndGetRole(
            authServiceChannelServiceUser,
            organizationId,
            Optional.of(roleIdUser1),
            Set.of(ResourceTypeV2.PROJECT, ResourceTypeV2.DATASET));
      }
      for (String datasetVersionId : datasetVersionIds) {
        DeleteDatasetVersion deleteDatasetVersion =
            DeleteDatasetVersion.newBuilder().setId(datasetVersionId).build();
        DeleteDatasetVersion.Response deleteDatasetVersionResponse =
            datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
        LOGGER.info("DatasetVersion deleted successfully");
        LOGGER.info(deleteDatasetVersionResponse.toString());
      }

      for (Dataset dataset : datasetList) {
        if (isRunningIsolated()) {
          var resourcesResponse =
              GetResources.Response.newBuilder()
                  .addItem(
                      GetResourcesResponseItem.newBuilder()
                          .setResourceId(dataset.getId())
                          .setWorkspaceId(authClientInterceptor.getClient1WorkspaceId())
                          .build())
                  .build();
          when(collaboratorBlockingMock.getResources(any())).thenReturn(resourcesResponse);
        }
        DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
        DeleteDataset.Response deleteDatasetResponse =
            datasetServiceStub.deleteDataset(deleteDataset);
        LOGGER.info("Dataset deleted successfully");
        LOGGER.info(deleteDatasetResponse.toString());
        assertTrue(deleteDatasetResponse.getStatus());
      }

      if (isRunningIsolated()) {
        when(uac.getAuthzService().isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(true).build()));
      }
      for (String runId : experimentRunMap.keySet()) {
        DeleteExperimentRun deleteExperimentRun =
            DeleteExperimentRun.newBuilder().setId(runId).build();
        DeleteExperimentRun.Response deleteExperimentRunResponse =
            experimentRunServiceStub.deleteExperimentRun(deleteExperimentRun);
        assertTrue(deleteExperimentRunResponse.getStatus());
      }

      if (!isRunningIsolated()) {
        var groupStub = GroupServiceGrpc.newBlockingStub(authServiceChannelServiceUser);
        groupStub.removeUsers(
            RemoveGroupUsers.newBuilder()
                .addUserId(testUser2.getVertaInfo().getUserId())
                .setGroupId(groupIdUser1)
                .setOrgId(organizationId)
                .build());
      }
    }

    LOGGER.info("FindExperimentRuns test stop................................");
  }

  @Test
  public void deleteExperimentRunHyperparameters() {
    LOGGER.info("Delete ExperimentRun Hyperparameters test start................................");

    o_logHyperparametersTest();
    List<KeyValue> hyperparameters = experimentRun.getHyperparametersList();
    LOGGER.info("Hyperparameters size : " + hyperparameters.size());
    List<String> keys = new ArrayList<>();
    for (int index = 0; index < hyperparameters.size() - 1; index++) {
      KeyValue keyValue = hyperparameters.get(index);
      keys.add(keyValue.getKey());
    }
    LOGGER.info("Hyperparameters key size : " + keys.size());

    DeleteHyperparameters request =
        DeleteHyperparameters.newBuilder()
            .setId(experimentRun.getId())
            .addAllHyperparameterKeys(keys)
            .build();

    experimentRunServiceStub.deleteHyperparameters(request);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info(
        "DeleteExperimentRunHyperparameters Response : \n"
            + response.getExperimentRun().getHyperparametersList());
    assertTrue(response.getExperimentRun().getHyperparametersList().size() <= 1);

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");
    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    if (!response.getExperimentRun().getHyperparametersList().isEmpty()) {
      request =
          DeleteHyperparameters.newBuilder()
              .setId(experimentRun.getId())
              .setDeleteAll(true)
              .build();

      experimentRunServiceStub.deleteHyperparameters(request);

      response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
      LOGGER.info(
          "DeleteExperimentRunHyperparameters Response : \n"
              + response.getExperimentRun().getHyperparametersList());
      assertEquals(0, response.getExperimentRun().getHyperparametersList().size());

      assertNotEquals(
          experimentRun.getDateUpdated(),
          response.getExperimentRun().getDateUpdated(),
          "ExperimentRun date_updated field not update on database");
      experimentRun = response.getExperimentRun();
      experimentRunMap.put(experimentRun.getId(), experimentRun);
    }

    LOGGER.info("Delete ExperimentRun Hyperparameters test stop................................");
  }

  @Test
  public void deleteExperimentRunMetrics() {
    LOGGER.info("Delete ExperimentRun Metrics test start................................");

    i_logMetricsTest();
    List<KeyValue> metrics = experimentRun.getMetricsList();
    LOGGER.info("Metrics size : " + metrics.size());
    List<String> keys = new ArrayList<>();
    for (int index = 0; index < metrics.size() - 1; index++) {
      KeyValue keyValue = metrics.get(index);
      keys.add(keyValue.getKey());
    }
    LOGGER.info("Metrics key size : " + keys.size());

    DeleteMetrics request =
        DeleteMetrics.newBuilder().setId(experimentRun.getId()).addAllMetricKeys(keys).build();

    experimentRunServiceStub.deleteMetrics(request);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info(
        "DeleteExperimentRunMetrics Response : \n" + response.getExperimentRun().getMetricsList());
    assertTrue(response.getExperimentRun().getMetricsList().size() <= 1);

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");
    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    if (!response.getExperimentRun().getMetricsList().isEmpty()) {
      request = DeleteMetrics.newBuilder().setId(experimentRun.getId()).setDeleteAll(true).build();

      experimentRunServiceStub.deleteMetrics(request);

      response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
      LOGGER.info(
          "DeleteExperimentRunMetrics Response : \n"
              + response.getExperimentRun().getMetricsList());
      assertEquals(0, response.getExperimentRun().getMetricsList().size());

      assertNotEquals(
          experimentRun.getDateUpdated(),
          response.getExperimentRun().getDateUpdated(),
          "ExperimentRun date_updated field not update on database");
      experimentRun = response.getExperimentRun();
      experimentRunMap.put(experimentRun.getId(), experimentRun);
    }

    LOGGER.info("Delete ExperimentRun Metrics test stop................................");
  }

  @Test
  public void deleteExperimentRunObservations() {
    LOGGER.info("Delete ExperimentRun Observations test start................................");

    g_logObservationsTest();
    List<Observation> observations = experimentRun.getObservationsList();
    LOGGER.info("Observations size : " + observations.size());
    List<String> keys = new ArrayList<>();
    for (int index = 0; index < observations.size() - 1; index++) {
      Observation observation = observations.get(index);
      if (observation.getOneOfCase().equals(Observation.OneOfCase.ATTRIBUTE)) {
        keys.add(observation.getAttribute().getKey());
      } else {
        keys.add(observation.getArtifact().getKey());
      }
    }
    LOGGER.info("Observations key size : " + keys.size());

    DeleteObservations request =
        DeleteObservations.newBuilder()
            .setId(experimentRun.getId())
            .addAllObservationKeys(keys)
            .build();

    experimentRunServiceStub.deleteObservations(request);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
    GetExperimentRunById.Response response =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    LOGGER.info(
        "DeleteExperimentRunObservations Response : \n"
            + response.getExperimentRun().getObservationsList());
    assertTrue(response.getExperimentRun().getObservationsList().size() <= 1);

    assertNotEquals(
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated(),
        "ExperimentRun date_updated field not update on database");
    experimentRun = response.getExperimentRun();
    experimentRunMap.put(experimentRun.getId(), experimentRun);

    if (!response.getExperimentRun().getObservationsList().isEmpty()) {
      request =
          DeleteObservations.newBuilder().setId(experimentRun.getId()).setDeleteAll(true).build();

      experimentRunServiceStub.deleteObservations(request);

      response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
      LOGGER.info(
          "DeleteExperimentRunObservations Response : \n"
              + response.getExperimentRun().getObservationsList());
      assertEquals(0, response.getExperimentRun().getObservationsList().size());

      assertNotEquals(
          experimentRun.getDateUpdated(),
          response.getExperimentRun().getDateUpdated(),
          "ExperimentRun date_updated field not update on database");
      experimentRun = response.getExperimentRun();
      experimentRunMap.put(experimentRun.getId(), experimentRun);
    }

    LOGGER.info("Delete ExperimentRun Observations test stop................................");
  }

  @Test
  public void cloneExperimentRun() throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("Clone experimentRun test start................................");

    long repoId =
        RepositoryTest.createRepository(
            versioningServiceBlockingStub, "Repo-" + new Date().getTime());
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);
    Commit commit =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(111)
            .addParentShas(getBranchResponse.getCommit().getCommitSha())
            .build();
    Location location1 = Location.newBuilder().addLocation("dataset").addLocation("train").build();
    Location location2 =
        Location.newBuilder().addLocation("test-1").addLocation("test1.json").build();
    Location location3 =
        Location.newBuilder().addLocation("test-2").addLocation("test2.json").build();
    Location location4 =
        Location.newBuilder().addLocation("test-location-4").addLocation("test4.json").build();

    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setCommit(commit)
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                    .addAllLocation(location1.getLocationList())
                    .build())
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getBlob(Blob.ContentCase.CONFIG))
                    .addAllLocation(location2.getLocationList())
                    .build())
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getBlob(Blob.ContentCase.DATASET))
                    .addAllLocation(location3.getLocationList())
                    .build())
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(CommitTest.getHyperparameterConfigBlob(0.14F, 0.10F))
                    .addAllLocation(location4.getLocationList())
                    .build())
            .build();
    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitResponse.getCommit();

    // Create project
    CreateProject createProjectRequest =
        ProjectTest.getCreateProjectRequest("experimentRun-project-" + new Date().getTime());
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project1 = createProjectResponse.getProject();
    LOGGER.info("Project1 created successfully");

    createProjectRequest =
        ProjectTest.getCreateProjectRequest("experimentRun-project-" + new Date().getTime());
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    LOGGER.info("Project2 created successfully");

    if (isRunningIsolated()) {
      mockGetResourcesForAllProjects(
          Map.of(project1.getId(), project1, project2.getId(), project2), testUser1);
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

    try {
      // Create two experiment of above project
      CreateExperiment createExperimentRequest =
          ExperimentTest.getCreateExperimentRequestForOtherTests(
              project1.getId(), "Experiment-1-" + new Date().getTime());
      CreateExperiment.Response createExperimentResponse =
          experimentServiceStub.createExperiment(createExperimentRequest);
      Experiment experiment1 = createExperimentResponse.getExperiment();
      LOGGER.info("Experiment1 created successfully");

      createExperimentRequest =
          ExperimentTest.getCreateExperimentRequestForOtherTests(
              project2.getId(), "Experiment-2-" + new Date().getTime());
      createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
      Experiment experiment2 = createExperimentResponse.getExperiment();
      LOGGER.info("Experiment2 created successfully");

      Map<String, Location> locationMap = new HashMap<>();
      locationMap.put("location-1", location1);

      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project1.getId(), experiment1.getId(), "ExperimentRun-" + new Date().getTime());
      KeyValue hyperparameter1 = generateNumericKeyValue("C", 0.0001);
      createExperimentRunRequest =
          createExperimentRunRequest.toBuilder()
              .setVersionedInputs(
                  VersioningEntry.newBuilder()
                      .setRepositoryId(repoId)
                      .setCommit(commitResponse.getCommit().getCommitSha())
                      .putAllKeyLocationMap(locationMap)
                      .build())
              .addHyperparameters(hyperparameter1)
              .build();
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      LOGGER.info("ExperimentRun created successfully");
      ExperimentRun srcExperimentRun = createExperimentRunResponse.getExperimentRun();

      CloneExperimentRun cloneExperimentRun =
          CloneExperimentRun.newBuilder().setSrcExperimentRunId(srcExperimentRun.getId()).build();
      CloneExperimentRun.Response cloneResponse =
          experimentRunServiceStub.cloneExperimentRun(cloneExperimentRun);
      assertNotEquals(
          srcExperimentRun.getId(),
          cloneResponse.getRun().getId(),
          "Clone run id should not be match with source run id");
      srcExperimentRun =
          srcExperimentRun.toBuilder()
              .setId(cloneResponse.getRun().getId())
              .setName(cloneResponse.getRun().getName())
              .setDateCreated(cloneResponse.getRun().getDateCreated())
              .setDateUpdated(cloneResponse.getRun().getDateUpdated())
              .setStartTime(cloneResponse.getRun().getStartTime())
              .setEndTime(cloneResponse.getRun().getEndTime())
              .build();
      assertEquals(
          srcExperimentRun.getId(),
          cloneResponse.getRun().getId(),
          "Clone experimentRun can not match with expected experimentRun");

      cloneExperimentRun =
          CloneExperimentRun.newBuilder()
              .setSrcExperimentRunId(srcExperimentRun.getId())
              .setDestExperimentRunName("Test - " + Calendar.getInstance().getTimeInMillis())
              .setDestExperimentId(experiment2.getId())
              .build();
      cloneResponse = experimentRunServiceStub.cloneExperimentRun(cloneExperimentRun);
      assertNotEquals(
          srcExperimentRun.getId(),
          cloneResponse.getRun().getId(),
          "Clone run id should not be match with source run id");
      srcExperimentRun =
          srcExperimentRun.toBuilder()
              .setId(cloneResponse.getRun().getId())
              .setName(cloneExperimentRun.getDestExperimentRunName())
              .setProjectId(cloneResponse.getRun().getProjectId())
              .setExperimentId(cloneExperimentRun.getDestExperimentId())
              .setDateCreated(cloneResponse.getRun().getDateCreated())
              .setDateUpdated(cloneResponse.getRun().getDateUpdated())
              .setStartTime(cloneResponse.getRun().getStartTime())
              .setEndTime(cloneResponse.getRun().getEndTime())
              .build();
      assertEquals(
          srcExperimentRun.getId(),
          cloneResponse.getRun().getId(),
          "Clone experimentRun can not match with expected experimentRun");

      try {
        cloneExperimentRun =
            CloneExperimentRun.newBuilder()
                .setSrcExperimentRunId(srcExperimentRun.getId())
                .setDestExperimentId("XYZ")
                .build();
        experimentRunServiceStub.cloneExperimentRun(cloneExperimentRun);
        Assertions.fail();
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.warn(
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
      }
    } finally {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      assertTrue(deleteResult.getStatus());

      if (isRunningIsolated()) {
        when(uacBlockingMock.getCurrentUser(any())).thenReturn(testUser1);
        mockGetSelfAllowedResources(
            Set.of(project1.getId(), project2.getId()),
            ModelDBServiceResourceTypes.PROJECT,
            ModelDBServiceActions.DELETE);
      }
      for (Project project : new Project[] {project1, project2}) {
        DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
        DeleteProject.Response deleteProjectResponse =
            projectServiceStub.deleteProject(deleteProject);
        LOGGER.info("Project deleted successfully");
        LOGGER.info(deleteProjectResponse.toString());
        assertTrue(deleteProjectResponse.getStatus());
      }
    }

    LOGGER.info("Clone experimentRun test stop................................");
  }

  @Test
  public void randomNameGeneration() {
    LOGGER.info("Random name generation test start................................");
    GenerateRandomNameRequest.Response response =
        metadataServiceBlockingStub.generateRandomName(
            GenerateRandomNameRequest.newBuilder().build());
    LOGGER.info("Random name: {}", response.getName());
    Assertions.assertFalse(response.getName().isEmpty(), "Random name should not be empty");
    LOGGER.info("Random name generation test stop................................");
  }

  @Test
  public void logEnvironmentTest() {
    LOGGER.info("logEnvironment test start................................");
    EnvironmentBlob environmentBlob =
        EnvironmentBlob.newBuilder()
            .setPython(
                PythonEnvironmentBlob.newBuilder()
                    .setVersion(
                        VersionEnvironmentBlob.newBuilder().setMajor(3).setMinor(7).setPatch(5))
                    .addRequirements(
                        PythonRequirementEnvironmentBlob.newBuilder()
                            .setLibrary("pytest")
                            .setConstraint("==")
                            .setVersion(VersionEnvironmentBlob.newBuilder().setMajor(1))
                            .build())
                    .addRequirements(
                        PythonRequirementEnvironmentBlob.newBuilder()
                            .setLibrary("verta")
                            .setConstraint("==")
                            .setVersion(
                                VersionEnvironmentBlob.newBuilder().setMajor(14).setMinor(9))))
            .build();
    LogEnvironment request =
        LogEnvironment.newBuilder()
            .setId(experimentRun.getId())
            .setEnvironment(environmentBlob)
            .build();

    experimentRunServiceStub.logEnvironment(request);

    GetExperimentRunById getExperimentRunById =
        GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();

    GetExperimentRunById.Response getExperimentRunByIdResponse =
        experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
    assertEquals(
        request.getEnvironment(),
        getExperimentRunByIdResponse.getExperimentRun().getEnvironment(),
        "environment not match with expected environment");

    try {
      experimentRunServiceStub.logEnvironment(request.toBuilder().clearId().build());
      Assertions.fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("logEnvironment test stop................................");
  }

  @Test
  void shouldUpdateExperimentRunNameSuccessfully() {
    var newName = "Run 007";

    var previousExperimentRun =
        experimentRunServiceStub
            .getExperimentRunById(
                GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build())
            .getExperimentRun();

    assertDoesNotThrow(
        () ->
            experimentRunServiceStub.updateExperimentRunName(
                UpdateExperimentRunName.newBuilder()
                    .setId(experimentRun.getId())
                    .setName(newName)
                    .build()));

    var foundExperimentRun =
        experimentRunServiceStub
            .getExperimentRunById(
                GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build())
            .getExperimentRun();

    assertThat(foundExperimentRun).isNotNull();
    assertThat(foundExperimentRun.getName()).isEqualTo(newName);
    assertThat(foundExperimentRun.getVersionNumber())
        .isEqualTo(previousExperimentRun.getVersionNumber() + 1);
  }

  @Test
  void shouldThrowExceptionWhenUpdateExperimentRunWithoutNameArgument() {
    var exception =
        assertThrows(
            StatusRuntimeException.class,
            () ->
                experimentRunServiceStub.updateExperimentRunName(
                    UpdateExperimentRunName.newBuilder().setId(experimentRun.getId()).build()));

    assertThat(exception.getStatus().getDescription()).isEqualTo("Name is not present");
  }
}
