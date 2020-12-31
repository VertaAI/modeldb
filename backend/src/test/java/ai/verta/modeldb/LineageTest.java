package ai.verta.modeldb;

import static ai.verta.modeldb.ExperimentTest.getCreateExperimentRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import ai.verta.modeldb.DatasetServiceGrpc.DatasetServiceBlockingStub;
import ai.verta.modeldb.DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub;
import ai.verta.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub;
import ai.verta.modeldb.ExperimentServiceGrpc.ExperimentServiceBlockingStub;
import ai.verta.modeldb.LineageEntryEnum.LineageEntryType;
import ai.verta.modeldb.LineageServiceGrpc.LineageServiceBlockingStub;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.authservice.*;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.cron_jobs.CronJobUtils;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LineageTest {

  private static final Logger LOGGER = LogManager.getLogger(LineageTest.class);
  private static final LineageEntry.Builder NOT_EXISTENT_DATASET =
      LineageEntry.newBuilder()
          .setType(LineageEntryType.DATASET_VERSION)
          .setExternalId("id_not_existent_dataset");

  private static String serverName = InProcessServerBuilder.generateName();
  private static InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder channelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static LineageServiceBlockingStub lineageServiceStub;

  private static DatasetVersionServiceBlockingStub datasetVersionServiceStub;
  private static ProjectServiceBlockingStub projectServiceStub;
  private static ExperimentServiceBlockingStub experimentServiceStub;
  private static ExperimentRunServiceBlockingStub experimentRunServiceStub;
  private static DatasetServiceBlockingStub datasetServiceStub;
  private static DeleteEntitiesCron deleteEntitiesCron;

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
    serverBuilder.build().start();

    Map<String, Object> testUerPropMap = (Map<String, Object>) testPropMap.get("testUsers");
    if (testUerPropMap != null && testUerPropMap.size() > 0) {
      AuthClientInterceptor authClientInterceptor = new AuthClientInterceptor(testPropMap);
      channelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
    }
    deleteEntitiesCron =
        new DeleteEntitiesCron(authService, roleService, CronJobUtils.deleteEntitiesFrequency);

    ManagedChannel channel = channelBuilder.maxInboundMessageSize(1024).build();
    lineageServiceStub = LineageServiceGrpc.newBlockingStub(channel);

    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);
    datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);
    datasetVersionServiceStub = DatasetVersionServiceGrpc.newBlockingStub(channel);
  }

  @AfterClass
  public static void removeServerAndService() {
    // Delete entities by cron job
    deleteEntitiesCron.run();
    App.initiateShutdown(0);
  }

  @Test
  public void createAndDeleteLineageNegativeTest() {
    List<String> experimentIds = new ArrayList<>();
    List<ExperimentRun> experimentRunList = new ArrayList<>();
    List<DatasetVersion> datasetVersionList = new ArrayList<>();
    // Create project
    Project project = getProject(projectServiceStub);

    // Create experiment of above project
    Experiment experiment = getExperiment(experimentIds, experimentServiceStub, project);

    ExperimentRun experimentRun =
        getExperimentRun(experimentRunList, experimentRunServiceStub, project, experiment, "name1");
    final LineageEntry.Builder inputOutputExp =
        LineageEntry.newBuilder()
            .setType(LineageEntryType.EXPERIMENT_RUN)
            .setExternalId(experimentRun.getId());
    DatasetTest datasetTest = new DatasetTest();

    DatasetVersion datasetVersion =
        getDatasetVersion(
            datasetVersionList, datasetVersionServiceStub, datasetServiceStub, "name");
    final LineageEntry.Builder inputDataset =
        LineageEntry.newBuilder()
            .setType(LineageEntryType.DATASET_VERSION)
            .setExternalId(datasetVersion.getId());

    datasetVersion =
        getDatasetVersion(
            datasetVersionList, datasetVersionServiceStub, datasetServiceStub, "name1");
    final LineageEntry.Builder inputDataset2 =
        LineageEntry.newBuilder()
            .setType(LineageEntryType.DATASET_VERSION)
            .setExternalId(datasetVersion.getId());
    try {
      AddLineage.Builder addLineage =
          AddLineage.newBuilder()
              .addInput(inputDataset)
              .addInput(LineageEntry.newBuilder().setType(LineageEntryType.DATASET_VERSION))
              .addOutput(inputOutputExp);
      try {
        Assert.assertFalse(lineageServiceStub.addLineage(addLineage.build()).getStatus());
        fail();
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        Assert.assertEquals(Code.INVALID_ARGUMENT, status.getCode());
        Assert.assertNotNull(status.getDescription());
        Assert.assertThat(
            status.getDescription().toLowerCase(), CoreMatchers.containsString("external"));
        Assert.assertThat(status.getDescription().toLowerCase(), CoreMatchers.containsString("id"));
        Assert.assertThat(
            status.getDescription().toLowerCase(), CoreMatchers.containsString("empty"));
      }

      addLineage =
          AddLineage.newBuilder()
              .addInput(inputDataset)
              .addInput(inputDataset2)
              .addOutput(
                  LineageEntry.newBuilder()
                      .setType(LineageEntryType.UNKNOWN)
                      .setExternalId("id_input_output_exp"));
      try {
        Assert.assertFalse(lineageServiceStub.addLineage(addLineage.build()).getStatus());
        fail();
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        Assert.assertEquals(Code.INVALID_ARGUMENT, status.getCode());
        Assert.assertNotNull(status.getDescription());
        Assert.assertThat(
            status.getDescription().toLowerCase(), CoreMatchers.containsString("unknown"));
        Assert.assertThat(
            status.getDescription().toLowerCase(), CoreMatchers.containsString("type"));
      }

      addLineage =
          AddLineage.newBuilder()
              .addInput(NOT_EXISTENT_DATASET)
              .addInput(inputDataset2)
              .addOutput(inputOutputExp);
      try {
        Assert.assertFalse(lineageServiceStub.addLineage(addLineage.build()).getStatus());
        fail();
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        Assert.assertEquals(Code.INVALID_ARGUMENT, status.getCode());
        Assert.assertNotNull(status.getDescription());
        Assert.assertThat(
            status.getDescription().toLowerCase(), CoreMatchers.containsString("external"));
        Assert.assertThat(status.getDescription().toLowerCase(), CoreMatchers.containsString("id"));
        Assert.assertThat(
            status.getDescription().toLowerCase(), CoreMatchers.containsString("not"));
        Assert.assertThat(
            status.getDescription().toLowerCase(), CoreMatchers.containsString("exists"));
      }

      addLineage =
          AddLineage.newBuilder()
              .addInput(inputDataset2)
              .addInput(inputDataset2)
              .addOutput(inputOutputExp);
      try {
        Assert.assertFalse(lineageServiceStub.addLineage(addLineage.build()).getStatus());
        fail();
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        Assert.assertEquals(Code.INVALID_ARGUMENT, status.getCode());
        Assert.assertNotNull(status.getDescription());
        Assert.assertThat(
            status.getDescription().toLowerCase(), CoreMatchers.containsString("non-unique"));
        Assert.assertThat(
            status.getDescription().toLowerCase(), CoreMatchers.containsString("ids"));
      }
    } finally {
      deleteAll(datasetVersionList, project);
    }
  }

  @Test
  public void createAndDeleteLineageTest() {
    LOGGER.info("Create and delete Lineage test start................................");

    List<String> experimentIds = new ArrayList<>();
    List<ExperimentRun> experimentRunList = new ArrayList<>();
    List<DatasetVersion> datasetVersionList = new ArrayList<>();

    // Create project
    Project project = getProject(projectServiceStub);

    try {

      // Create experiment of above project
      Experiment experiment = getExperiment(experimentIds, experimentServiceStub, project);

      ExperimentRun experimentRun =
          getExperimentRun(
              experimentRunList, experimentRunServiceStub, project, experiment, "name1");
      final LineageEntry.Builder inputOutputExp =
          LineageEntry.newBuilder()
              .setType(LineageEntryType.EXPERIMENT_RUN)
              .setExternalId(experimentRun.getId());

      experimentRun =
          getExperimentRun(
              experimentRunList, experimentRunServiceStub, project, experiment, "name2");
      final LineageEntry.Builder inputExp =
          LineageEntry.newBuilder()
              .setType(LineageEntryType.EXPERIMENT_RUN)
              .setExternalId(experimentRun.getId());

      experimentRun =
          getExperimentRun(
              experimentRunList, experimentRunServiceStub, project, experiment, "name3");
      final LineageEntry.Builder outputExp =
          LineageEntry.newBuilder()
              .setType(LineageEntryType.EXPERIMENT_RUN)
              .setExternalId(experimentRun.getId());

      DatasetTest datasetTest = new DatasetTest();

      DatasetVersion datasetVersion =
          getDatasetVersion(
              datasetVersionList, datasetVersionServiceStub, datasetServiceStub, "name");
      final LineageEntry.Builder inputDataset =
          LineageEntry.newBuilder()
              .setType(LineageEntryType.DATASET_VERSION)
              .setExternalId(datasetVersion.getId());

      datasetVersion =
          getDatasetVersion(
              datasetVersionList, datasetVersionServiceStub, datasetServiceStub, "name1");
      final LineageEntry.Builder inputDataset2 =
          LineageEntry.newBuilder()
              .setType(LineageEntryType.DATASET_VERSION)
              .setExternalId(datasetVersion.getId());

      datasetVersion =
          getDatasetVersion(
              datasetVersionList, datasetVersionServiceStub, datasetServiceStub, "name2");
      final LineageEntry.Builder outputDataset =
          LineageEntry.newBuilder()
              .setType(LineageEntryType.DATASET_VERSION)
              .setExternalId(datasetVersion.getId());

      check(
          Arrays.asList(inputExp, NOT_EXISTENT_DATASET),
          Arrays.asList(null, null),
          Arrays.asList(null, null));

      AddLineage.Builder addLineage =
          AddLineage.newBuilder()
              .addInput(inputDataset)
              .addInput(inputDataset2)
              .addOutput(inputOutputExp);
      Assert.assertTrue(lineageServiceStub.addLineage(addLineage.build()).getStatus());

      check(
          Arrays.asList(
              inputOutputExp, inputDataset, inputDataset2, inputExp, NOT_EXISTENT_DATASET),
          Arrays.asList(Arrays.asList(inputDataset, inputDataset2), null, null, null, null),
          Arrays.asList(
              null,
              Collections.singletonList(inputOutputExp),
              Collections.singletonList(inputOutputExp),
              null,
              null));

      addLineage =
          AddLineage.newBuilder()
              .addOutput(outputDataset)
              .addOutput(outputExp)
              .addInput(inputExp)
              .addInput(inputOutputExp);
      Assert.assertTrue(lineageServiceStub.addLineage(addLineage.build()).getStatus());

      check(
          Arrays.asList(
              inputOutputExp, inputDataset, inputDataset2, inputExp, outputDataset, outputExp),
          Arrays.asList(
              Arrays.asList(inputDataset, inputDataset2),
              null,
              null,
              null,
              Arrays.asList(inputExp, inputOutputExp),
              Arrays.asList(inputExp, inputOutputExp)),
          Arrays.asList(
              Arrays.asList(outputDataset, outputExp),
              Collections.singletonList(inputOutputExp),
              Collections.singletonList(inputOutputExp),
              Arrays.asList(outputDataset, outputExp),
              null,
              null));

      DeleteLineage.Builder deleteLineage =
          DeleteLineage.newBuilder()
              .addInput(inputExp)
              .addOutput(outputDataset)
              .addOutput(outputExp);
      Assert.assertTrue(lineageServiceStub.deleteLineage(deleteLineage.build()).getStatus());

      check(
          Arrays.asList(
              inputOutputExp, inputDataset, inputDataset2, inputExp, outputDataset, outputExp),
          Arrays.asList(
              Arrays.asList(inputDataset, inputDataset2),
              null,
              null,
              null,
              Collections.singletonList(inputOutputExp),
              Collections.singletonList(inputOutputExp)),
          Arrays.asList(
              Arrays.asList(outputDataset, outputExp),
              Collections.singletonList(inputOutputExp),
              Collections.singletonList(inputOutputExp),
              null,
              null,
              null));

      deleteLineage =
          DeleteLineage.newBuilder()
              .addInput(inputExp)
              .addInput(inputDataset)
              .addInput(inputDataset2)
              .addInput(inputOutputExp)
              .addOutput(inputOutputExp)
              .addOutput(outputDataset)
              .addOutput(outputExp);
      Assert.assertTrue(lineageServiceStub.deleteLineage(deleteLineage.build()).getStatus());

      check(
          Arrays.asList(
              inputOutputExp, inputDataset, inputDataset2, inputExp, outputDataset, outputExp),
          Arrays.asList(null, null, null, null, null, null),
          Arrays.asList(null, null, null, null, null, null));
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      e.printStackTrace();
      fail();
    } finally {
      deleteAll(datasetVersionList, project);
    }

    LOGGER.info("Create and delete Lineage test stop................................");
  }

  private void deleteAll(List<DatasetVersion> datasetVersionList, Project project) {
    for (DatasetVersion datasetVersion1 : datasetVersionList) {
      DeleteDatasetVersion deleteDatasetVersionRequest =
          DeleteDatasetVersion.newBuilder()
              .setDatasetId(datasetVersion1.getDatasetId())
              .setId(datasetVersion1.getId())
              .build();
      DeleteDatasetVersion.Response deleteDatasetVersionResponse =
          datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersionRequest);
      LOGGER.info("DeleteDatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionResponse.toString());

      DeleteDataset deleteDataset =
          DeleteDataset.newBuilder().setId(datasetVersion1.getDatasetId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
    }

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
  }

  private Experiment getExperiment(
      List<String> experimentIds,
      ExperimentServiceBlockingStub experimentServiceStub,
      Project project) {
    CreateExperiment createExperimentRequest =
        getCreateExperimentRequest(project.getId(), "Experiment-" + new Date().getTime());
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment = createExperimentResponse.getExperiment();
    experimentIds.add(experiment.getId());
    LOGGER.info("Experiment created successfully");
    return experiment;
  }

  private Project getProject(ProjectServiceBlockingStub projectServiceStub) {
    ProjectTest projectTest = new ProjectTest();
    CreateProject createProjectRequest =
        projectTest.getCreateProjectRequest("Project-" + new Date().getTime());
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    return project;
  }

  private DatasetVersion getDatasetVersion(
      List<DatasetVersion> datasetVersionList,
      DatasetVersionServiceBlockingStub datasetVersionServiceStub,
      DatasetServiceBlockingStub datasetServiceStub,
      String name) {
    CreateDataset createDatasetRequest =
        DatasetTest.getDatasetRequest("Dataset-" + new Date().getTime() + "-" + name);
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    Dataset dataset = createDatasetResponse.getDataset();
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    CreateDatasetVersion createDatasetVersionRequest =
        DatasetVersionTest.getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionList.add(datasetVersion);
    return datasetVersion;
  }

  private ExperimentRun getExperimentRun(
      List<ExperimentRun> experimentRunList,
      ExperimentRunServiceBlockingStub experimentRunServiceStub,
      Project project,
      Experiment experiment,
      String name) {
    CreateExperimentRun createExperimentRunRequest =
        ExperimentRunTest.getCreateExperimentRunRequest(
            project.getId(),
            experiment.getId(),
            "ExperimentRun-" + new Date().getTime() + "-" + name);
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun1 = createExperimentRunResponse.getExperimentRun();
    experimentRunList.add(experimentRun1);
    return experimentRun1;
  }

  private static LineageEntryBatch formatToBatch(List<LineageEntry.Builder> x) {
    LineageEntryBatch.Builder batch = LineageEntryBatch.newBuilder();
    if (x != null) {
      batch.addAllItems(
          x.stream().map(LineageEntry.Builder::build).sorted(LineageTest::compare)::iterator);
    }
    return batch.build();
  }

  private static LineageEntryBatch sortFormattedArray(LineageEntryBatch x) {
    return x.newBuilderForType()
        .addAllItems(
            x.getItemsList().stream().sorted(LineageTest::compare).collect(Collectors.toList()))
        .build();
  }

  private static int compare(LineageEntry o1, LineageEntry o2) {
    final int i = o1.getExternalId().compareTo(o2.getExternalId());
    if (i == 0) {
      return o1.getType().compareTo(o2.getType());
    }
    return i;
  }

  private void check(
      List<LineageEntry.Builder> data,
      List<List<LineageEntry.Builder>> expectedInputs,
      List<List<LineageEntry.Builder>> expectedOutputs) {

    List<LineageEntry> iterator =
        data.stream().map(LineageEntry.Builder::build).collect(Collectors.toList());
    FindAllInputs.Response findAllInputsResult =
        lineageServiceStub.findAllInputs(FindAllInputs.newBuilder().addAllItems(iterator).build());
    List<LineageEntryBatch> expectedInputsWithoutNulls =
        expectedInputs.stream().map(LineageTest::formatToBatch).collect(Collectors.toList());
    final List<LineageEntryBatch> collect =
        findAllInputsResult.getInputsList().stream()
            .map(LineageTest::sortFormattedArray)
            .collect(Collectors.toList());
    assertEquals(expectedInputsWithoutNulls, collect);
    FindAllOutputs.Response findAllOutputsResult =
        lineageServiceStub.findAllOutputs(
            FindAllOutputs.newBuilder().addAllItems(iterator).build());
    List<LineageEntryBatch> expectedOutputsWithoutNulls =
        expectedOutputs.stream().map(LineageTest::formatToBatch).collect(Collectors.toList());
    assertEquals(
        expectedOutputsWithoutNulls,
        findAllOutputsResult.getOutputsList().stream()
            .map(LineageTest::sortFormattedArray)
            .collect(Collectors.toList()));
    FindAllInputsOutputs.Response findAllInputsOutputsResult =
        lineageServiceStub.findAllInputsOutputs(
            FindAllInputsOutputs.newBuilder().addAllItems(iterator).build());
    assertEquals(
        expectedInputsWithoutNulls,
        findAllInputsOutputsResult.getInputsList().stream()
            .map(LineageTest::sortFormattedArray)
            .collect(Collectors.toList()));
    assertEquals(
        expectedOutputsWithoutNulls,
        findAllInputsOutputsResult.getOutputsList().stream()
            .map(LineageTest::sortFormattedArray)
            .collect(Collectors.toList()));
  }
}
