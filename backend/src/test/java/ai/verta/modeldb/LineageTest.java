package ai.verta.modeldb;

import static ai.verta.modeldb.CommitTest.getCreateCommitRequest;
import static ai.verta.modeldb.ExperimentTest.getCreateExperimentRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import ai.verta.modeldb.AddLineage.Response;
import ai.verta.modeldb.DatasetServiceGrpc.DatasetServiceBlockingStub;
import ai.verta.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub;
import ai.verta.modeldb.ExperimentServiceGrpc.ExperimentServiceBlockingStub;
import ai.verta.modeldb.LineageEntryBatchResponseSingle.Builder;
import ai.verta.modeldb.LineageServiceGrpc.LineageServiceBlockingStub;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.CreateCommitRequest;
import ai.verta.modeldb.versioning.DeleteCommitRequest;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.GetBranchRequest;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.VersioningServiceGrpc;
import ai.verta.modeldb.versioning.VersioningServiceGrpc.VersioningServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
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
public class LineageTest {

  private static final Logger LOGGER = LogManager.getLogger(LineageTest.class);
  private static final LineageEntry.Builder NOT_EXISTENT_DATASET =
      LineageEntry.newBuilder()
          .setBlob(
              VersioningLineageEntry.newBuilder()
                  .setCommitSha("asdf")
                  .addLocation("tt")
                  .setRepositoryId(0));
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
  private LineageServiceBlockingStub lineageServiceStub;

  private VersioningServiceBlockingStub versioningServiceBlockingStub;
  private ExperimentRunTest experimentRunTest;
  private DatasetVersionTest datasetVersionTest;
  private ProjectServiceBlockingStub projectServiceStub;
  private ExperimentServiceBlockingStub experimentServiceStub;
  private ExperimentRunServiceBlockingStub experimentRunServiceStub;
  private DatasetServiceBlockingStub datasetServiceStub;

  @SuppressWarnings("unchecked")
  @BeforeClass
  public static void setServerAndService() throws Exception {

    Map<String, Object> propertiesMap =
        ModelDBUtils.readYamlProperties(System.getenv(ModelDBConstants.VERTA_MODELDB_CONFIG));
    Map<String, Object> testPropMap = (Map<String, Object>) propertiesMap.get("test");
    Map<String, Object> databasePropMap = (Map<String, Object>) testPropMap.get("test-database");

    App app = App.getInstance();
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
      AuthClientInterceptor authClientInterceptor = new AuthClientInterceptor(testPropMap);
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
    lineageServiceStub = LineageServiceGrpc.newBlockingStub(channel);

    experimentRunTest = new ExperimentRunTest();
    datasetVersionTest = new DatasetVersionTest();
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);
    datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);
    versioningServiceBlockingStub = VersioningServiceGrpc.newBlockingStub(channel);
  }

  @Test
  public void createAndDeleteLineageNegativeTest()
      throws ModelDBException, NoSuchAlgorithmException {

    long repositoryId =
        RepositoryTest.createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    try {
      GetBranchRequest getBranchRequest =
          GetBranchRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repositoryId).build())
              .setBranch(ModelDBConstants.MASTER_BRANCH)
              .build();
      GetBranchRequest.Response getBranchResponse =
          versioningServiceBlockingStub.getBranch(getBranchRequest);

      CreateCommitRequest createCommitRequest =
          getCreateCommitRequest(
              repositoryId, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET, "name");

      CreateCommitRequest.Response commitResponse =
          versioningServiceBlockingStub.createCommit(createCommitRequest);
      String commit1Sha = commitResponse.getCommit().getCommitSha();

      createCommitRequest =
          getCreateCommitRequest(
              repositoryId, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET, "name2");

      commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
      String commit2Sha = commitResponse.getCommit().getCommitSha();
      try {

        List<String> experimentIds = new ArrayList<>();
        List<ExperimentRun> experimentRunList = new ArrayList<>();
        // Create project
        Project project = getProject(projectServiceStub);

        // Create experiment of above project
        Experiment experiment = getExperiment(experimentIds, experimentServiceStub, project);

        ExperimentRun experimentRun =
            getExperimentRun(
                experimentRunList,
                experimentRunTest,
                experimentRunServiceStub,
                project,
                experiment,
                "name1");
        final LineageEntry.Builder inputOutputExp =
            LineageEntry.newBuilder().setExperimentRun(experimentRun.getId());

        final LineageEntry.Builder inputDataset =
            LineageEntry.newBuilder()
                .setBlob(
                    VersioningLineageEntry.newBuilder()
                        .setRepositoryId(repositoryId)
                        .setCommitSha(commit1Sha)
                        .addLocation("/"));

        final LineageEntry.Builder inputDataset2 =
            LineageEntry.newBuilder()
                .setBlob(
                    VersioningLineageEntry.newBuilder()
                        .setRepositoryId(repositoryId)
                        .setCommitSha(commit2Sha)
                        .addLocation("/"));
        try {
          AddLineage.Builder addLineage =
              AddLineage.newBuilder()
                  .addInput(inputDataset)
                  .addInput(LineageEntry.newBuilder())
                  .addOutput(inputOutputExp);
          try {
            lineageServiceStub.addLineage(addLineage.build());
            fail();
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            Assert.assertEquals(Code.INVALID_ARGUMENT, status.getCode());
            Assert.assertNotNull(status.getDescription());
            Assert.assertThat(
                status.getDescription().toLowerCase(), CoreMatchers.containsString("unknown"));
            Assert.assertThat(
                status.getDescription().toLowerCase(), CoreMatchers.containsString("lineage"));
            Assert.assertThat(
                status.getDescription().toLowerCase(), CoreMatchers.containsString("type"));
          }

          addLineage =
              AddLineage.newBuilder()
                  .addInput(NOT_EXISTENT_DATASET)
                  .addInput(inputDataset2)
                  .addOutput(inputOutputExp);
          try {
            lineageServiceStub.addLineage(addLineage.build());
            fail();
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            Assert.assertEquals(Code.NOT_FOUND, status.getCode());
            Assert.assertNotNull(status.getDescription());
            Assert.assertThat(
                status.getDescription().toLowerCase(), CoreMatchers.containsString("couldn't"));
            Assert.assertThat(
                status.getDescription().toLowerCase(), CoreMatchers.containsString("id"));
            Assert.assertThat(
                status.getDescription().toLowerCase(), CoreMatchers.containsString("find"));
            Assert.assertThat(
                status.getDescription().toLowerCase(), CoreMatchers.containsString("repository"));
          }

          addLineage =
              AddLineage.newBuilder()
                  .addInput(inputDataset2)
                  .addInput(inputDataset2)
                  .addOutput(inputOutputExp);
          try {
            lineageServiceStub.addLineage(addLineage.build());
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
          deleteAll(project);
        }
      } finally {
        DeleteCommitRequest deleteCommitRequest =
            DeleteCommitRequest.newBuilder()
                .setRepositoryId(
                    RepositoryIdentification.newBuilder().setRepoId(repositoryId).build())
                .setCommitSha(commit1Sha)
                .build();
        versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
        deleteCommitRequest =
            DeleteCommitRequest.newBuilder()
                .setRepositoryId(
                    RepositoryIdentification.newBuilder().setRepoId(repositoryId).build())
                .setCommitSha(commit2Sha)
                .build();
        versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
      }
    } finally {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repositoryId))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
    }
  }

  @Test
  public void createAndDeleteLineageTest() throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("Create and delete Lineage test start................................");

    List<String> experimentIds = new ArrayList<>();
    List<ExperimentRun> experimentRunList = new ArrayList<>();
    List<DatasetVersion> datasetVersionList = new ArrayList<>();

    // Create project
    Project project = getProject(projectServiceStub);

    // Create experiment of above project
    Experiment experiment = getExperiment(experimentIds, experimentServiceStub, project);

    ExperimentRun experimentRun =
        getExperimentRun(
            experimentRunList,
            experimentRunTest,
            experimentRunServiceStub,
            project,
            experiment,
            "name1");
    final LineageEntry.Builder inputOutputExp =
        LineageEntry.newBuilder().setExperimentRun(experimentRun.getId());

    experimentRun =
        getExperimentRun(
            experimentRunList,
            experimentRunTest,
            experimentRunServiceStub,
            project,
            experiment,
            "name2");
    final LineageEntry.Builder inputExp =
        LineageEntry.newBuilder().setExperimentRun(experimentRun.getId());

    experimentRun =
        getExperimentRun(
            experimentRunList,
            experimentRunTest,
            experimentRunServiceStub,
            project,
            experiment,
            "name3");
    final LineageEntry.Builder outputExp =
        LineageEntry.newBuilder().setExperimentRun(experimentRun.getId());

    long repositoryId =
        RepositoryTest.createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    try {
      GetBranchRequest getBranchRequest =
          GetBranchRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repositoryId).build())
              .setBranch(ModelDBConstants.MASTER_BRANCH)
              .build();
      GetBranchRequest.Response getBranchResponse =
          versioningServiceBlockingStub.getBranch(getBranchRequest);

      CreateCommitRequest createCommitRequest =
          getCreateCommitRequest(
              repositoryId, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET, "name");

      CreateCommitRequest.Response commitResponse =
          versioningServiceBlockingStub.createCommit(createCommitRequest);
      String commit1Sha = commitResponse.getCommit().getCommitSha();

      createCommitRequest =
          getCreateCommitRequest(
              repositoryId, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET, "name2");

      commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
      String commit2Sha = commitResponse.getCommit().getCommitSha();

      createCommitRequest =
          getCreateCommitRequest(
              repositoryId, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET, "name3");

      commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
      String commit3Sha = commitResponse.getCommit().getCommitSha();
      try {
        final LineageEntry.Builder inputDataset =
            LineageEntry.newBuilder()
                .setBlob(
                    VersioningLineageEntry.newBuilder()
                        .setRepositoryId(repositoryId)
                        .setCommitSha(commit1Sha)
                        .addLocation("/")
                        .addLocation("name"));

        final LineageEntry.Builder inputDataset2 =
            LineageEntry.newBuilder()
                .setBlob(
                    VersioningLineageEntry.newBuilder()
                        .setRepositoryId(repositoryId)
                        .setCommitSha(commit2Sha)
                        .addLocation("/")
                        .addLocation("name2"));

        final LineageEntry.Builder outputDataset =
            LineageEntry.newBuilder()
                .setBlob(
                    VersioningLineageEntry.newBuilder()
                        .setRepositoryId(repositoryId)
                        .setCommitSha(commit3Sha)
                        .addLocation("/")
                        .addLocation("name3"));
        try {
          check(
              Collections.singletonList(inputExp),
              Collections.singletonList(null),
              Collections.singletonList(null),
              0L);

          AddLineage.Builder addLineage =
              AddLineage.newBuilder()
                  .addInput(inputDataset)
                  .addOutput(inputOutputExp);
          Response result = lineageServiceStub.addLineage(addLineage.build());
          long id = result.getId();
          addLineage =
              AddLineage.newBuilder()
                  .setId(id)
                  .addInput(inputDataset2);
          result = lineageServiceStub.addLineage(addLineage.build());
          assertEquals(id, result.getId());

          check(
              Arrays.asList(inputOutputExp, inputDataset, inputDataset2, inputExp),
              Arrays.asList(Arrays.asList(inputDataset, inputDataset2), null, null, null),
              Arrays.asList(
                  null,
                  Collections.singletonList(inputOutputExp),
                  Collections.singletonList(inputOutputExp),
                  null),
              id);

          long oldId = id;
          addLineage =
              AddLineage.newBuilder()
                  .addOutput(outputDataset)
                  .addOutput(outputExp)
                  .addInput(inputExp)
                  .addInput(inputOutputExp);
          result = lineageServiceStub.addLineage(addLineage.build());
          id = result.getId();

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
                  null),
              Arrays.asList(oldId, id, id, id, id, id),
              Arrays.asList(id, oldId, oldId, id, id, id),
              result.getId());

          DeleteLineage.Builder deleteLineage = DeleteLineage.newBuilder().setId(oldId);
          Assert.assertTrue(lineageServiceStub.deleteLineage(deleteLineage.build()).getStatus());

          check(
              Arrays.asList(
                  inputOutputExp, inputDataset, inputDataset2, inputExp, outputDataset, outputExp),
              Arrays.asList(
                  null,
                  null,
                  null,
                  null,
                  Arrays.asList(inputExp, inputOutputExp),
                  Arrays.asList(inputExp, inputOutputExp)),
              Arrays.asList(
                  Arrays.asList(outputDataset, outputExp),
                  null,
                  null,
                  Arrays.asList(outputDataset, outputExp),
                  null,
                  null),
              result.getId());

          deleteLineage =
              DeleteLineage.newBuilder()
                  .setId(result.getId())
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
              Arrays.asList(null, null, null, null, null, null),
              result.getId());
        } catch (StatusRuntimeException e) {
          Status status = Status.fromThrowable(e);
          LOGGER.warn(
              "Error Code : " + status.getCode() + " Description : " + status.getDescription());
          e.printStackTrace();
          fail();
        } finally {
          deleteAll(project);
        }
      } finally {
        DeleteCommitRequest deleteCommitRequest =
            DeleteCommitRequest.newBuilder()
                .setRepositoryId(
                    RepositoryIdentification.newBuilder().setRepoId(repositoryId).build())
                .setCommitSha(commit1Sha)
                .build();
        versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
        deleteCommitRequest =
            DeleteCommitRequest.newBuilder()
                .setRepositoryId(
                    RepositoryIdentification.newBuilder().setRepoId(repositoryId).build())
                .setCommitSha(commit2Sha)
                .build();
        versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
      }
    } finally {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repositoryId))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
    }

    LOGGER.info("Create and delete Lineage test stop................................");
  }

  private void deleteAll(Project project) {
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
        getCreateExperimentRequest(project.getId(), "Experiment_n_sprt_abc_");
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
        projectTest.getCreateProjectRequest("experiment_project_n_sprt_abc");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    return project;
  }

  private ExperimentRun getExperimentRun(
      List<ExperimentRun> experimentRunList,
      ExperimentRunTest experimentRunTest,
      ExperimentRunServiceBlockingStub experimentRunServiceStub,
      Project project,
      Experiment experiment,
      String name) {
    CreateExperimentRun createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "exp_run_test" + name);
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun1 = createExperimentRunResponse.getExperimentRun();
    experimentRunList.add(experimentRun1);
    return experimentRun1;
  }

  private static LineageEntryBatchResponse formatToBatch(List<LineageEntry.Builder> x, Long id) {
    LineageEntryBatchResponse.Builder batch = LineageEntryBatchResponse.newBuilder();
    Builder builder = LineageEntryBatchResponseSingle.newBuilder();
    if (x != null) {
      builder
          .setId(id)
          .addAllItems(
              x.stream().map(LineageEntry.Builder::build).sorted(LineageTest::compare)::iterator);
    }
    batch.addItems(builder);
    return batch.build();
  }

  private static LineageEntryBatchResponse sortFormattedArray(LineageEntryBatchResponse x) {
    List<LineageEntryBatchResponseSingle> itemsList = x.getItemsList();
    Builder builder = LineageEntryBatchResponseSingle.newBuilder();
    if (itemsList.size() != 0) {
      LineageEntryBatchResponseSingle lineageEntryBatchResponseSingle = itemsList.get(0);
      builder
          .setId(lineageEntryBatchResponseSingle.getId())
          .addAllItems(
              lineageEntryBatchResponseSingle.getItemsList().stream()
                  .sorted(LineageTest::compare)
                  .collect(Collectors.toList()));
    }
    return x.newBuilderForType().addItems(builder).build();
  }

  private static int compare(LineageEntry o1, LineageEntry o2) {
    final int i = o1.getDescriptionCase().compareTo(o2.getDescriptionCase());
    if (i == 0) {
      switch (o1.getDescriptionCase()) {
        case EXPERIMENT_RUN:
          return o1.getExperimentRun().compareTo(o2.getExperimentRun());
        case BLOB:
          return Integer.compare(o1.getBlob().hashCode(), o2.getBlob().hashCode());
      }
    }
    return i;
  }

  private void check(
      List<LineageEntry.Builder> data,
      List<List<LineageEntry.Builder>> expectedInputs,
      List<List<LineageEntry.Builder>> expectedOutputs,
      List<Long> idsInput0,
      List<Long> idsOutput0,
      Long id0) {
    final List<Long> idsInput;
    final List<Long> idsOutput;
    if (idsInput0 == null) {
      idsInput = new LinkedList<>();
      idsOutput = new LinkedList<>();
      for (int i = 0; i < expectedInputs.size(); ++i) {
        idsInput.add(id0);
        idsOutput.add(id0);
      }
    } else {
      idsInput = idsInput0;
      idsOutput = idsOutput0;
    }
    final Iterator<Long> idsInputIterator = idsInput.iterator();
    final Iterator<Long> idsOutputIterator = idsOutput.iterator();

    List<LineageEntryBatchRequest> iterator =
        data.stream()
            .map(entry -> LineageEntryBatchRequest.newBuilder().setEntry(entry).build())
            .collect(Collectors.toList());
    FindAllInputs.Response findAllInputsResult =
        lineageServiceStub.findAllInputs(FindAllInputs.newBuilder().addAllItems(iterator).build());
    List<LineageEntryBatchResponse> expectedInputsWithoutNulls =
        expectedInputs.stream()
            .map(x -> formatToBatch(x, idsInputIterator.next()))
            .collect(Collectors.toList());
    final List<LineageEntryBatchResponse> collect =
        findAllInputsResult.getInputsList().stream()
            .map(LineageTest::sortFormattedArray)
            .collect(Collectors.toList());
    assertEquals(expectedInputsWithoutNulls, collect);
    FindAllOutputs.Response findAllOutputsResult =
        lineageServiceStub.findAllOutputs(
            FindAllOutputs.newBuilder().addAllItems(iterator).build());
    List<LineageEntryBatchResponse> expectedOutputsWithoutNulls =
        expectedOutputs.stream()
            .map(x -> formatToBatch(x, idsOutputIterator.next()))
            .collect(Collectors.toList());
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

  private void check(
      List<LineageEntry.Builder> data,
      List<List<LineageEntry.Builder>> expectedInputs,
      List<List<LineageEntry.Builder>> expectedOutputs,
      long id) {
    check(data, expectedInputs, expectedOutputs, null, null, id);
  }
}
