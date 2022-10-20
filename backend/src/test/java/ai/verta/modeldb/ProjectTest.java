package ai.verta.modeldb;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import ai.verta.common.Artifact;
import ai.verta.common.ArtifactTypeEnum.ArtifactType;
import ai.verta.common.CodeVersion;
import ai.verta.common.CollaboratorTypeEnum.CollaboratorType;
import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.ValueTypeEnum.ValueType;
import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.AuthServiceChannel;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.*;
import ai.verta.uac.CollaboratorServiceGrpc.CollaboratorServiceBlockingStub;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.google.common.util.concurrent.Futures;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class, webEnvironment = DEFINED_PORT)
@ContextConfiguration(classes = {ModeldbTestConfigurationBeans.class})
public class ProjectTest extends ModeldbTestSetup {

  private static final Logger LOGGER = LogManager.getLogger(ProjectTest.class);

  // Project Entities
  private Project project;
  private Project project2;
  private Project project3;
  private final Map<String, Project> projectMap = new HashMap<>();

  // Experiment Entities
  private Experiment experiment;

  // ExperimentRun Entities
  private ExperimentRun experimentRun;

  private Dataset dataset;

  @Before
  public void createEntities() {
    initializedChannelBuilderAndExternalServiceStubs();

    if (isRunningIsolated()) {
      setupMockUacEndpoints(uac);
    }

    // Create all entities
    createProjectEntities();
    createExperimentEntities();
    createExperimentRunEntities();
  }

  @After
  public void removeEntities() {
    if (!projectMap.isEmpty()) {
      if (isRunningIsolated()) {
        mockGetResourcesForAllEntity(projectMap, testUser1);
      }

      DeleteProjects deleteProjects =
          DeleteProjects.newBuilder().addAllIds(projectMap.keySet()).build();
      DeleteProjects.Response deleteProjectsResponse =
          projectServiceStub.deleteProjects(deleteProjects);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectsResponse.toString());
      assertTrue(deleteProjectsResponse.getStatus());
    }

    if (isRunningIsolated()) {
      var authChannelMock = mock(AuthServiceChannel.class);
      when(uac.getBlockingAuthServiceChannel()).thenReturn(authChannelMock);
      var collaboratorBlockingMock = mock(CollaboratorServiceBlockingStub.class);
      when(authChannelMock.getCollaboratorServiceBlockingStub())
          .thenReturn(collaboratorBlockingMock);
      var resourcesResponse =
          GetResources.Response.newBuilder()
              .addItem(
                  GetResourcesResponseItem.newBuilder()
                      .setResourceId(dataset.getId())
                      .setWorkspaceId(authClientInterceptor.getClient1WorkspaceId())
                      .build())
              .build();
      when(collaboratorBlockingMock.getResources(any())).thenReturn(resourcesResponse);
      var authzServiceBlockingStub = mock(AuthzServiceGrpc.AuthzServiceBlockingStub.class);
      when(authChannelMock.getAuthzServiceBlockingStub()).thenReturn(authzServiceBlockingStub);
      when(authzServiceBlockingStub.isSelfAllowed(any()))
          .thenReturn(IsSelfAllowed.Response.newBuilder().setAllowed(true).build());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    projectMap.clear();
  }

  private void createProjectEntities() {

    // Create two project of above project
    CreateProject createProjectRequest = getCreateProjectRequest();
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
    createProjectRequest = getCreateProjectRequest();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    project2 = createProjectResponse.getProject();
    projectMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project2.getName());

    // Create project3
    createProjectRequest = getCreateProjectRequest();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    project3 = createProjectResponse.getProject();
    projectMap.put(project3.getId(), project3);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project3.getName());

    if (isRunningIsolated()) {
      mockGetResourcesForAllEntity(projectMap, testUser1);
    }
  }

  private void createExperimentEntities() {

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        getCreateExperimentRequest(project.getId(), "Experiment-1-" + random);
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

  private void createExperimentRunEntities() {
    CreateDataset createDatasetRequest = getDatasetRequest("Dataset-" + new Date().getTime());
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    dataset = createDatasetResponse.getDataset();
    CreateDatasetVersion createDatasetVersionRequest =
        DatasetVersionTest.getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();

    List<Artifact> datasets = new ArrayList<>();
    datasets.add(
        Artifact.newBuilder()
            .setKey("Google developer datasets")
            .setPath("This is data artifact type in Google developer datasets")
            .setArtifactType(ArtifactType.MODEL)
            .setLinkedArtifactId(datasetVersion1.getId())
            .setUploadCompleted(
                !testConfig
                    .getArtifactStoreConfig()
                    .getArtifactStoreType()
                    .equals(CommonConstants.S3))
            .build());
    datasets.add(
        Artifact.newBuilder()
            .setKey("Google Pay datasets")
            .setPath("This is data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactType.DATA)
            .setLinkedArtifactId(datasetVersion1.getId())
            .setUploadCompleted(
                !testConfig
                    .getArtifactStoreConfig()
                    .getArtifactStoreType()
                    .equals(CommonConstants.S3))
            .build());
    CreateExperimentRun createExperimentRunRequest =
        getCreateExperimentRunRequest(project.getId(), experiment.getId(), "ExperimentRun_sprt_1");
    createExperimentRunRequest =
        createExperimentRunRequest.toBuilder().clearDatasets().addAllDatasets(datasets).build();
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
    if (testConfig.hasAuth()) {
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

  public static CreateProject getCreateProjectRequest(String projectName) {
    return CreateProject.newBuilder()
        .setName(projectName)
        .setDescription("This is a project description.")
        .addTags("tag_x")
        .addTags("tag_y")
        .build();
  }

  private CreateProject getCreateProjectRequest() {
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
            .setUploadCompleted(
                !testConfig
                    .getArtifactStoreConfig()
                    .getArtifactStoreType()
                    .equals(CommonConstants.S3))
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
                    .equals(CommonConstants.S3))
            .build());

    return CreateProject.newBuilder()
        .setName("project-" + new Date().getTime())
        .setDescription("This is a project description.")
        .addTags("tag_x")
        .addTags("tag_y")
        .addAllAttributes(metadataList)
        .addAllArtifacts(artifactList)
        .build();
  }

  private CreateExperiment getCreateExperimentRequest(String projectId, String experimentName) {
    return CreateExperiment.newBuilder()
        .setProjectId(projectId)
        .setName(experimentName)
        .setDescription("This is a experiment description.")
        .setDateCreated(Calendar.getInstance().getTimeInMillis())
        .setDateUpdated(Calendar.getInstance().getTimeInMillis())
        .addTags("tag_x")
        .addTags("tag_y")
        .build();
  }

  private CreateDataset getDatasetRequest(String datasetName) {
    return CreateDataset.newBuilder()
        .setName(datasetName)
        .setVisibility(ResourceVisibility.PRIVATE)
        .addTags("A")
        .addTags("A0")
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
                    .equals(CommonConstants.S3))
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
                    .equals(CommonConstants.S3))
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
                    .equals(CommonConstants.S3))
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
                    .equals(CommonConstants.S3))
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
  public void a_projectCreateTest() {
    LOGGER.info("Create Project test start................................");

    CreateProject createProjectRequest = getCreateProjectRequest(project.getName());

    try {
      if (isRunningIsolated()) {
        when(collaboratorMock.setResource(any()))
            .thenThrow(new AlreadyExistsException("Already exists"));
      }
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

      projectName = "Code Versioning_ver-1.1 (none; git children)" + new Date().getTime();
      createProjectRequest = getCreateProjectRequest(projectName);
      createProjectRequest = createProjectRequest.toBuilder().build();
      response = projectServiceStub.createProject(createProjectRequest);
      projectShortName = projectName;
      projectShortName = ModelDBUtils.convertToProjectShortName(projectShortName);
      assertEquals(projectName, response.getProject().getName());
      assertEquals(projectShortName, response.getProject().getShortName());
      LOGGER.info("Project Created Successfully");
      projects.add(response.getProject());

      projectName = "";
      createProjectRequest = getCreateProjectRequest(projectName);
      createProjectRequest = createProjectRequest.toBuilder().build();
      response = projectServiceStub.createProject(createProjectRequest);
      projectShortName = projectName;
      projectShortName = ModelDBUtils.convertToProjectShortName(projectShortName);
      assertFalse(response.getProject().getName().isEmpty());
      assertFalse(response.getProject().getShortName().isEmpty());
      LOGGER.info("Project Created Successfully");
      projects.add(response.getProject());

    } finally {
      DeleteProjects deleteProjects =
          DeleteProjects.newBuilder()
              .addAllIds(projects.stream().map(Project::getId).collect(Collectors.toList()))
              .build();
      DeleteProjects.Response deleteProjectsResponse =
          projectServiceStub.deleteProjects(deleteProjects);
      LOGGER.info("Projects deleted successfully");
      LOGGER.info(deleteProjectsResponse.toString());
      assertTrue(deleteProjectsResponse.getStatus());
    }

    LOGGER.info("Create Project with MD5 logic test stop................................");
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
      if (isRunningIsolated()) {
        when(authzMock.isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(false).build()));
      }
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
      if (isRunningIsolated()) {
        when(authzMock.isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(false).build()));
      }
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
      if (isRunningIsolated()) {
        when(authzMock.isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(false).build()));
      }
      AddProjectTag addProjectTagRequest = AddProjectTag.newBuilder().setId("xzy").build();
      projectServiceStub.addProjectTag(addProjectTagRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
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
      if (isRunningIsolated()) {
        when(authzMock.isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(false).build()));
      }
      DeleteProjectTag deleteProjectTagRequest = DeleteProjectTag.newBuilder().setId("xyz").build();
      projectServiceStub.deleteProjectTag(deleteProjectTagRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
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
      if (isRunningIsolated()) {
        when(authzMock.isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(false).build()));
      }
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

    if (isRunningIsolated()) {
      mockGetResourcesForAllEntity(Map.of(project.getId(), project), testUser1);
    }
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
      when(collaboratorMock.getResourcesSpecialPersonalWorkspace(any()))
          .thenReturn(Futures.immediateFuture(GetResources.Response.newBuilder().build()));
      GetProjectById getProject = GetProjectById.newBuilder().setId("xyz").build();
      projectServiceStub.getProjectById(getProject);
      fail();
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    LOGGER.info("Get project by ID negative test stop................................");
  }

  @Test
  public void k_getProjectByName() throws ExecutionException, InterruptedException {
    LOGGER.info("Get Project by name test start................................");

    if (isRunningIsolated()) {
      when(uacMock.getCurrentUser(any())).thenReturn(Futures.immediateFuture(testUser2));
      when(workspaceMock.getWorkspaceByName(
              GetWorkspaceByName.newBuilder()
                  .setName(testUser2.getVertaInfo().getUsername())
                  .build()))
          .thenReturn(
              Futures.immediateFuture(
                  Workspace.newBuilder()
                      .setId(testUser2.getVertaInfo().getDefaultWorkspaceId())
                      .build()));
      when(workspaceMock.getWorkspaceById(
              GetWorkspaceById.newBuilder()
                  .setId(testUser2.getVertaInfo().getDefaultWorkspaceId())
                  .build()))
          .thenReturn(
              Futures.immediateFuture(
                  Workspace.newBuilder()
                      .setId(testUser2.getVertaInfo().getDefaultWorkspaceId())
                      .build()));
      when(collaboratorMock.getResourcesSpecialPersonalWorkspace(any()))
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
    }

    Project project = null;
    try {
      // Create project
      CreateProject createProjectRequest = getCreateProjectRequest();
      CreateProject.Response createProjectResponse =
          client2ProjectServiceStub.createProject(createProjectRequest);
      project = createProjectResponse.getProject();
      LOGGER.info("Project created successfully");
      assertEquals(
          "Project name not match with expected project name",
          createProjectRequest.getName(),
          project.getName());

      if (isRunningIsolated()) {
        mockGetResourcesForAllEntity(Map.of(project.getId(), project), testUser2);
      }

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

      if (testConfig.hasAuth()) {
        if (isRunningIsolated()) {
          when(uacMock.getCurrentUser(any())).thenReturn(Futures.immediateFuture(testUser1));
          mockGetResourcesForAllEntity(Map.of(project.getId(), project), testUser1);
          when(collaboratorMock.getResourcesSpecialPersonalWorkspace(any()))
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
                                  .setOwnerId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                                  .setWorkspaceId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                                  .build())
                          .build()));
        } else {
          AddCollaboratorRequest addCollaboratorRequest =
              CollaboratorUtils.addCollaboratorRequestProject(
                  project, authClientInterceptor.getClient1Email(), CollaboratorType.READ_WRITE);

          AddCollaboratorRequest.Response addOrUpdateProjectCollaboratorResponse =
              collaboratorBlockingMock.addOrUpdateProjectCollaborator(addCollaboratorRequest);
          LOGGER.info(
              "Collaborator added in server : "
                  + addOrUpdateProjectCollaboratorResponse.getStatus());
          assertTrue(addOrUpdateProjectCollaboratorResponse.getStatus());
        }

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
          createProjectRequest = getCreateProjectRequest();
          createProjectResponse = projectServiceStub.createProject(createProjectRequest);
          selfProject = createProjectResponse.getProject();
          LOGGER.info("Project created successfully");
          assertEquals(
              "Project name not match with expected project name",
              createProjectRequest.getName(),
              selfProject.getName());

          if (isRunningIsolated()) {
            mockGetResourcesForAllEntity(Map.of(selfProject.getId(), selfProject), testUser1);
          }

          getProject = GetProjectByName.newBuilder().setName(selfProject.getName()).build();
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
      if (project != null) {
        DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
        DeleteProject.Response deleteProjectResponse =
            client2ProjectServiceStub.deleteProject(deleteProject);
        LOGGER.info("Project deleted successfully");
        LOGGER.info(deleteProjectResponse.toString());
        assertTrue(deleteProjectResponse.getStatus());
      }
    }

    LOGGER.info("Get project by name test stop................................");
  }

  @Test
  public void k_getProjectByNameWithWorkspace() {
    LOGGER.info("Get Project by name with workspace test start................................");
    if (!testConfig.hasAuth()) {
      assertTrue(true);
      return;
    }

    Project selfProject = null;
    Project project = null;
    try {

      if (isRunningIsolated()) {
        when(workspaceMock.getWorkspaceByName(
                GetWorkspaceByName.newBuilder()
                    .setName(testUser2.getVertaInfo().getUsername())
                    .build()))
            .thenReturn(
                Futures.immediateFuture(
                    Workspace.newBuilder()
                        .setId(testUser2.getVertaInfo().getDefaultWorkspaceId())
                        .build()));
      }
      // Create project
      CreateProject createProjectRequest = getCreateProjectRequest();
      createProjectRequest =
          createProjectRequest
              .toBuilder()
              .setWorkspaceName(testUser2.getVertaInfo().getUsername())
              .build();
      CreateProject.Response createProjectResponse =
          client2ProjectServiceStub.createProject(createProjectRequest);
      project = createProjectResponse.getProject();
      LOGGER.info("Project created successfully");
      assertEquals(
          "Project name not match with expected project name",
          createProjectRequest.getName(),
          project.getName());

      var projectMap = new HashMap<String, Project>();
      if (isRunningIsolated()) {
        projectMap.put(project.getId(), project);
        mockGetResourcesForAllEntity(projectMap, testUser1);
      } else {
        AddCollaboratorRequest addCollaboratorRequest =
            CollaboratorUtils.addCollaboratorRequestProject(
                project, authClientInterceptor.getClient1Email(), CollaboratorType.READ_WRITE);

        AddCollaboratorRequest.Response addOrUpdateProjectCollaboratorResponse =
            collaboratorServiceStubClient2.addOrUpdateProjectCollaborator(addCollaboratorRequest);
        LOGGER.info(
            "Collaborator added in server : " + addOrUpdateProjectCollaboratorResponse.getStatus());
        assertTrue(addOrUpdateProjectCollaboratorResponse.getStatus());
      }

      // Create project
      createProjectRequest = getCreateProjectRequest(project.getName());
      createProjectResponse = projectServiceStub.createProject(createProjectRequest);
      selfProject = createProjectResponse.getProject();
      LOGGER.info("Project created successfully");
      assertEquals(
          "Project name not match with expected project name",
          createProjectRequest.getName(),
          selfProject.getName());

      if (isRunningIsolated()) {
        projectMap.put(selfProject.getId(), selfProject);
        mockGetResourcesForAllEntity(projectMap, testUser1);
      }

      GetProjectByName getProject =
          GetProjectByName.newBuilder()
              .setName(selfProject.getName())
              .setWorkspaceName(testUser2.getVertaInfo().getUsername())
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
      if (isRunningIsolated()) {
        when(collaboratorMock.getResources(any()))
            .thenReturn(Futures.immediateFuture(GetResources.Response.newBuilder().build()));
      }
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
    CreateProject createProjectRequest = getCreateProjectRequest();
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
    createProjectRequest = getCreateProjectRequest();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    projectsMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project2.getName());

    if (isRunningIsolated()) {
      projectsMap.putAll(projectMap);
      mockGetResourcesForAllEntity(projectsMap, testUser1);
    }

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
    CreateProject createProjectRequest = getCreateProjectRequest();
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
    createProjectRequest = getCreateProjectRequest();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    projectsMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project2.getName());

    if (isRunningIsolated()) {
      mockGetResourcesForAllEntity(projectsMap, testUser1);
    }

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

    if (isRunningIsolated()) {
      mockGetResourcesForAllEntity(Map.of(project.getId(), project), testUser1);
      when(authzMock.getSelfAllowedResources(
              GetSelfAllowedResources.newBuilder()
                  .addActions(
                      Action.newBuilder()
                          .setModeldbServiceAction(ModelDBServiceActions.READ)
                          .setService(ServiceEnum.Service.MODELDB_SERVICE))
                  .setService(ServiceEnum.Service.MODELDB_SERVICE)
                  .setResourceType(
                      ResourceType.newBuilder()
                          .setModeldbServiceResourceType(ModelDBServiceResourceTypes.REPOSITORY))
                  .build()))
          .thenReturn(
              Futures.immediateFuture(GetSelfAllowedResources.Response.newBuilder().build()));
    }

    GetSummary getSummaryRequest = GetSummary.newBuilder().setEntityId(project.getId()).build();
    GetSummary.Response response = projectServiceStub.getSummary(getSummaryRequest);
    LOGGER.info("Response Project Summary : " + CommonUtils.getStringFromProtoObject(response));

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
          lastModifiedExperimentRun.getId(),
          experimentRun.getId());

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
        assertEquals(Status.INTERNAL.getCode(), status.getCode());
      }

      String shortName = ModelDBUtils.convertToProjectShortName(UUID.randomUUID().toString());
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
      LOGGER.error("Error Code : " + status2.getCode() + " Error : " + status2.getDescription());
      fail(e.getMessage());
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
  public void y_projectCascadeDeleteTest() throws InterruptedException {
    LOGGER.info("Project delete with cascading test start................................");
    if (isRunningIsolated()) {
      when(roleServiceBlockingMock.deleteRoleBindings(any()))
          .thenReturn(DeleteRoleBindings.Response.newBuilder().setStatus(true).build());
    }
    cleanUpResources();

    Project project = null;
    ExperimentRun experimentRun1;
    ExperimentRun experimentRun3;
    try {
      // Create project
      CreateProject createProjectRequest = getCreateProjectRequest();
      CreateProject.Response createProjectResponse =
          projectServiceStub.createProject(createProjectRequest);
      project = createProjectResponse.getProject();
      LOGGER.info("\n Project created successfully \n");

      // Create two experiment of above project
      CreateExperiment request =
          getCreateExperimentRequest(project.getId(), "Experiment-1-" + new Date().getTime());
      CreateExperiment.Response response = experimentServiceStub.createExperiment(request);
      Experiment experiment1 = response.getExperiment();
      LOGGER.info("\n Experiment1 created successfully \n");
      request = getCreateExperimentRequest(project.getId(), "Experiment-2-" + new Date().getTime());
      response = experimentServiceStub.createExperiment(request);
      Experiment experiment2 = response.getExperiment();
      LOGGER.info("\n Experiment2 created successfully \n");

      // Create four ExperimentRun of above two experiment, each experiment has two experimentRun
      // For ExperiemntRun of Experiment1
      CreateExperimentRun createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project.getId(), experiment1.getId(), "ExperiemntRun-1-" + new Date().getTime());
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      experimentRun1 = createExperimentRunResponse.getExperimentRun();
      LOGGER.info("\n ExperimentRun1 created successfully \n");
      createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project.getId(), experiment1.getId(), "ExperiemntRun-2-" + new Date().getTime());
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      LOGGER.info("\n ExperimentRun2 created successfully \n");

      // For ExperiemntRun of Experiment2
      createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project.getId(), experiment2.getId(), "ExperiemntRun-3-" + new Date().getTime());
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      experimentRun3 = createExperimentRunResponse.getExperimentRun();
      LOGGER.info("\n ExperimentRun3 created successfully \n");
      createExperimentRunRequest =
          getCreateExperimentRunRequest(
              project.getId(), experiment2.getId(), "ExperimentRun-4-" + new Date().getTime());
      experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      LOGGER.info("\n ExperimentRun4 created successfully \n");

      // Create comment for above experimentRun1 & experimentRun3
      // comment for experiment1
      AddComment addCommentRequest =
          AddComment.newBuilder()
              .setEntityId(experimentRun1.getId())
              .setMessage(
                  "Hello, this project is intreasting." + Calendar.getInstance().getTimeInMillis())
              .build();
      commentServiceBlockingStub.addExperimentRunComment(addCommentRequest);
      LOGGER.info("\n Comment added successfully for ExperimentRun1 \n");
      // comment for experimentRun3
      addCommentRequest =
          AddComment.newBuilder()
              .setEntityId(experimentRun3.getId())
              .setMessage(
                  "Hello, this project is intreasting." + Calendar.getInstance().getTimeInMillis())
              .build();
      commentServiceBlockingStub.addExperimentRunComment(addCommentRequest);
      LOGGER.info("\n Comment added successfully for ExperimentRun3 \n");

      // Create two collaborator for above project
      // For Collaborator1
      if (testConfig.hasAuth() && !isRunningIsolated()) {
        AddCollaboratorRequest addCollaboratorRequest =
            CollaboratorUtils.addCollaboratorRequestProjectInterceptor(
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
    cleanUpResources();

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
      if (testConfig.hasAuth()) {
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
      if (testConfig.hasAuth()) {
        assertEquals(0, runResponse.getExperimentRunsCount());
        assertEquals(0, runResponse.getTotalRecords());
      }
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    if (!isRunningIsolated()) {
      // Start cross-checking for comment of experimentRun
      // For experimentRun1
      GetComments getCommentsRequest =
          GetComments.newBuilder().setEntityId(experimentRun1.getId()).build();
      GetComments.Response getCommentsResponse;
      try {
        commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
        if (testConfig.hasAuth()) {
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
      if (testConfig.hasAuth()) {
        GetCollaborator getCollaboratorRequest =
            GetCollaborator.newBuilder().setEntityId(project.getId()).build();
        try {
          collaboratorServiceStubClient1.getProjectCollaborators(getCollaboratorRequest);
          fail();
        } catch (StatusRuntimeException e) {
          checkEqualsAssert(e);
        }
      }
    }

    List<String> projectIds = new ArrayList<>();
    try {
      for (int count = 0; count < 5; count++) {
        // Create project
        CreateProject createProjectRequest = getCreateProjectRequest();
        CreateProject.Response createProjectResponse =
            projectServiceStub.createProject(createProjectRequest);
        projectIds.add(createProjectResponse.getProject().getId());
        project = createProjectResponse.getProject();
        LOGGER.info("\n Project created successfully \n");

        // Create two experiment of above project
        CreateExperiment request =
            getCreateExperimentRequest(project.getId(), "Experiment1_" + count);
        CreateExperiment.Response response = experimentServiceStub.createExperiment(request);
        Experiment experiment1 = response.getExperiment();
        LOGGER.info("\n Experiment1 created successfully \n");
        request = getCreateExperimentRequest(project.getId(), "Experiment2_" + count);
        response = experimentServiceStub.createExperiment(request);
        Experiment experiment2 = response.getExperiment();
        LOGGER.info("\n Experiment2 created successfully \n");

        // Create four ExperimentRun of above two experiment, each experiment has two
        // experimentRun
        // For ExperiemntRun of Experiment1
        CreateExperimentRun createExperimentRunRequest =
            getCreateExperimentRunRequest(
                project.getId(), experiment1.getId(), "ExperiemntRun1_" + count);
        CreateExperimentRun.Response createExperimentRunResponse =
            experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
        experimentRun1 = createExperimentRunResponse.getExperimentRun();
        LOGGER.info("\n ExperimentRun1 created successfully \n");
        createExperimentRunRequest =
            getCreateExperimentRunRequest(
                project.getId(), experiment1.getId(), "ExperiemntRun2_" + count);
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
        LOGGER.info("\n ExperimentRun2 created successfully \n");

        // For ExperiemntRun of Experiment2
        createExperimentRunRequest =
            getCreateExperimentRunRequest(
                project.getId(), experiment2.getId(), "ExperiemntRun3_" + count);
        createExperimentRunResponse =
            experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
        experimentRun3 = createExperimentRunResponse.getExperimentRun();
        LOGGER.info("\n ExperimentRun3 created successfully \n");
        createExperimentRunRequest =
            getCreateExperimentRunRequest(
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
        if (testConfig.hasAuth() && !isRunningIsolated()) {
          AddCollaboratorRequest addCollaboratorRequest =
              CollaboratorUtils.addCollaboratorRequestProjectInterceptor(
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
    cleanUpResources();

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
        if (testConfig.hasAuth()) {
          fail();
        }
      } catch (StatusRuntimeException ex) {
        checkEqualsAssert(ex);
      }

      // Start cross-checking for experimentRun
      try {
        GetExperimentRunsInProject getExperimentRuns =
            GetExperimentRunsInProject.newBuilder().setProjectId(project.getId()).build();
        GetExperimentRunsInProject.Response getResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRuns);
        if (testConfig.hasAuth() && getResponse.getExperimentRunsCount() > 0) {
          fail();
        }
      } catch (StatusRuntimeException e) {
        checkEqualsAssert(e);
      }

      if (!isRunningIsolated()) {
        // Start cross-checking for comment of experimentRun
        // For experimentRun1
        var getCommentsRequest =
            GetComments.newBuilder().setEntityId(experimentRun1.getId()).build();
        try {
          commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
          if (testConfig.hasAuth()) {
            fail();
          }
        } catch (StatusRuntimeException e) {
          checkEqualsAssert(e);
        }

        // For experimentRun3
        getCommentsRequest = GetComments.newBuilder().setEntityId(experimentRun3.getId()).build();
        try {
          commentServiceBlockingStub.getExperimentRunComments(getCommentsRequest);
          if (testConfig.hasAuth()) {
            fail();
          }
        } catch (StatusRuntimeException e) {
          checkEqualsAssert(e);
        }

        // Start cross-checking for project collaborator
        if (testConfig.hasAuth()) {
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
    CreateProject createProjectRequest = getCreateProjectRequest();
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
    createProjectRequest = getCreateProjectRequest();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project2 = createProjectResponse.getProject();
    projectsMap.put(project2.getId(), project2);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project2.getName());

    // Create project3
    createProjectRequest = getCreateProjectRequest();
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    Project project3 = createProjectResponse.getProject();
    projectsMap.put(project3.getId(), project3);
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project3.getName());

    if (isRunningIsolated()) {
      projectsMap.putAll(projectMap);
      mockGetResourcesForAllEntity(projectsMap, testUser1);
    }

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
    if (isRunningIsolated()) {
      projectMap.forEach((s, project4) -> projectsMap.remove(s));
    }
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
              projectsMap.get(project3.getId()),
              project3);
        } else if (pageNumber == 3) {
          assertEquals(
              "Project not match with expected Project",
              projectsMap.get(project1.getId()),
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
      CreateProject createProjectRequest = getCreateProjectRequest();
      CreateProject.Response createProjectResponse =
          projectServiceStub.createProject(createProjectRequest);
      project = createProjectResponse.getProject();
      LOGGER.info("Project created successfully");
      assertEquals(
          "Project name not match with expected project name",
          createProjectRequest.getName(),
          project.getName());

      if (isRunningIsolated()) {
        mockGetResourcesForAllEntity(Map.of(project.getId(), project), testUser1);
      }

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
                              .setUploadCompleted(
                                  !testConfig
                                      .getArtifactStoreConfig()
                                      .getArtifactStoreType()
                                      .equals(CommonConstants.S3))
                              .build())
                      .build())
              .build();
      LogProjectCodeVersion.Response logProjectCodeVersionResponse =
          projectServiceStub.logProjectCodeVersion(logProjectCodeVersionRequest);
      CodeVersion codeVersion = logProjectCodeVersionResponse.getProject().getCodeVersionSnapshot();
      assertNotEquals(
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
                            .setUploadCompleted(
                                !testConfig
                                    .getArtifactStoreConfig()
                                    .getArtifactStoreType()
                                    .equals(CommonConstants.S3))
                            .build())
                    .build())
            .build();
    LogProjectCodeVersion.Response logProjectCodeVersionResponse =
        projectServiceStub.logProjectCodeVersion(logProjectCodeVersionRequest);
    project = logProjectCodeVersionResponse.getProject();
    assertNotEquals(
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

    if (isRunningIsolated()) {
      when(authzMock.isSelfAllowed(any()))
          .thenReturn(
              Futures.immediateFuture(
                  IsSelfAllowed.Response.newBuilder().setAllowed(false).build()));
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
      if (isRunningIsolated()) {
        when(authzMock.isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(true).build()));
      }
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
      if (isRunningIsolated()) {
        when(authzMock.isSelfAllowed(any()))
            .thenReturn(
                Futures.immediateFuture(
                    IsSelfAllowed.Response.newBuilder().setAllowed(false).build()));
      }
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
  public void createProjectWithDeletedProjectName() {
    CreateProject createProjectRequest = getCreateProjectRequest();
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project testProj = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        testProj.getName());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(testProj.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project delete successfully. Status : {}", deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    createProjectRequest = getCreateProjectRequest(testProj.getName());
    createProjectResponse = projectServiceStub.createProject(createProjectRequest);
    testProj = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        testProj.getName());

    deleteProject = DeleteProject.newBuilder().setId(testProj.getId()).build();
    deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project delete successfully. Status : {}", deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());
  }

  @Test
  public void getProjectDatasetCount() {
    GetProjectDatasetCount.Response response =
        projectServiceStub.getProjectDatasetCount(
            GetProjectDatasetCount.newBuilder().setProjectId(project.getId()).build());
    assertEquals(
        "Project dataset count not match with expected Project dataset count",
        1,
        response.getDatasetCount());
  }

  @Test
  public void createAndDeleteProjectUsingServiceAccount() {
    // Create two project of above project
    CreateProject createProjectRequest = getCreateProjectRequest();
    CreateProject.Response createProjectResponse =
        serviceUserProjectServiceStub.createProject(createProjectRequest);
    DeleteProjects deleteProjects =
        DeleteProjects.newBuilder().addIds(createProjectResponse.getProject().getId()).build();
    DeleteProjects.Response deleteProjectsResponse =
        serviceUserProjectServiceStub.deleteProjects(deleteProjects);
    assertTrue(deleteProjectsResponse.getStatus());
  }
}
