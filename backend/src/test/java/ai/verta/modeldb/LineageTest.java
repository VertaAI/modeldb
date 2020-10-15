package ai.verta.modeldb;

import static ai.verta.modeldb.CommitTest.getCreateCommitRequest;
import static ai.verta.modeldb.ExperimentTest.getCreateExperimentRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import ai.verta.modeldb.AddLineage.Response;
import ai.verta.modeldb.DatasetServiceGrpc.DatasetServiceBlockingStub;
import ai.verta.modeldb.DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub;
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
import ai.verta.modeldb.cron_jobs.CronJobUtils;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
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
import io.grpc.ManagedChannelBuilder;
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
  private ManagedChannel client2Channel = null;
  private ManagedChannel authServiceChannel = null;
  private static String serverName = InProcessServerBuilder.generateName();
  private static InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder channelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder client2ChannelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private LineageServiceBlockingStub lineageServiceStub;
  private static App app;
  private static AuthClientInterceptor authClientInterceptor;

  private VersioningServiceBlockingStub versioningServiceBlockingStub;
  private VersioningServiceBlockingStub versioningServiceBlockingStubClient2;
  private ExperimentRunTest experimentRunTest;
  private ProjectServiceBlockingStub projectServiceStub;
  private ExperimentServiceBlockingStub experimentServiceStub;
  private ExperimentRunServiceBlockingStub experimentRunServiceStub;
  private static DeleteEntitiesCron deleteEntitiesCron;

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
      channelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
      client2ChannelBuilder.intercept(authClientInterceptor.getClient2AuthInterceptor());
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
    if (!client2Channel.isShutdown()) {
      client2Channel.shutdownNow();
    }
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      if (!authServiceChannel.isShutdown()) {
        authServiceChannel.shutdownNow();
      }
    }
  }

  @Before
  public void initializeChannel() throws IOException {
    grpcCleanup.register(serverBuilder.build().start());
    channel = grpcCleanup.register(channelBuilder.maxInboundMessageSize(1024).build());
    client2Channel =
        grpcCleanup.register(client2ChannelBuilder.maxInboundMessageSize(1024).build());
    lineageServiceStub = LineageServiceGrpc.newBlockingStub(channel);
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      authServiceChannel =
          ManagedChannelBuilder.forTarget(app.getAuthServerHost() + ":" + app.getAuthServerPort())
              .usePlaintext()
              .intercept(authClientInterceptor.getClient1AuthInterceptor())
              .build();
    }

    experimentRunTest = new ExperimentRunTest();
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);
    versioningServiceBlockingStub = VersioningServiceGrpc.newBlockingStub(channel);
    versioningServiceBlockingStubClient2 = VersioningServiceGrpc.newBlockingStub(client2Channel);
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
            LineageEntry.newBuilder().setExternalId(experimentRun.getId());

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
        LineageEntry.newBuilder().setExternalId(experimentRun.getId());

    experimentRun =
        getExperimentRun(
            experimentRunList,
            experimentRunTest,
            experimentRunServiceStub,
            project,
            experiment,
            "name2");
    final LineageEntry.Builder inputExp =
        LineageEntry.newBuilder().setExternalId(experimentRun.getId());

    experimentRun =
        getExperimentRun(
            experimentRunList,
            experimentRunTest,
            experimentRunServiceStub,
            project,
            experiment,
            "name3");
    final LineageEntry.Builder outputExp =
        LineageEntry.newBuilder().setExternalId(experimentRun.getId());

    long repositoryId =
        RepositoryTest.createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    long repositoryId2 =
        RepositoryTest.createRepository(
            versioningServiceBlockingStubClient2, RepositoryTest.NAME + "2");
    try {
      GetBranchRequest getBranchRequest =
          GetBranchRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repositoryId).build())
              .setBranch(ModelDBConstants.MASTER_BRANCH)
              .build();
      GetBranchRequest.Response getBranchResponse =
          versioningServiceBlockingStub.getBranch(getBranchRequest);
      getBranchRequest =
          GetBranchRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repositoryId2).build())
              .setBranch(ModelDBConstants.MASTER_BRANCH)
              .build();
      GetBranchRequest.Response getBranchResponse2 =
          versioningServiceBlockingStubClient2.getBranch(getBranchRequest);

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

      createCommitRequest =
          getCreateCommitRequest(
              repositoryId2,
              111,
              getBranchResponse2.getCommit(),
              Blob.ContentCase.DATASET,
              "name4");

      commitResponse = versioningServiceBlockingStubClient2.createCommit(createCommitRequest);
      String commit4Sha = commitResponse.getCommit().getCommitSha();
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

        final LineageEntry.Builder inputDatasetClient2 =
            LineageEntry.newBuilder()
                .setBlob(
                    VersioningLineageEntry.newBuilder()
                        .setRepositoryId(repositoryId2)
                        .setCommitSha(commit4Sha)
                        .addLocation("/")
                        .addLocation("name4"));
        try {
          check(
              Collections.singletonList(inputExp),
              Collections.singletonList(null),
              Collections.singletonList(null),
              0L);

          AddLineage.Builder addLineage =
              AddLineage.newBuilder().addInput(inputDataset).addOutput(inputOutputExp);

          Response result = lineageServiceStub.addLineage(addLineage.build());
          long id = result.getId();

          FindAllInputsOutputs.Response findAllInputsOutputsResult =
              lineageServiceStub.findAllInputsOutputs(
                  FindAllInputsOutputs.newBuilder()
                      .addItems(LineageEntryBatchRequest.newBuilder().setId(id))
                      .build());
          Assert.assertEquals(
              FindAllInputsOutputs.Response.newBuilder()
                  .addInputs(
                      LineageEntryBatchResponse.newBuilder()
                          .addItems(
                              LineageEntryBatchResponseSingle.newBuilder()
                                  .setId(id)
                                  .addItems(inputDataset)))
                  .addOutputs(
                      LineageEntryBatchResponse.newBuilder()
                          .addItems(
                              LineageEntryBatchResponseSingle.newBuilder()
                                  .setId(id)
                                  .addItems(inputOutputExp)))
                  .build(),
              findAllInputsOutputsResult);

          addLineage = AddLineage.newBuilder().setId(id).addInput(inputDataset2);
          result = lineageServiceStub.addLineage(addLineage.build());
          assertEquals(id, result.getId());

          LineageServiceBlockingStub lineageServiceBlockingStubClient2 =
              LineageServiceGrpc.newBlockingStub(client2Channel);
          if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
            addLineage = AddLineage.newBuilder().setId(id).addInput(inputDatasetClient2);
            result = lineageServiceBlockingStubClient2.addLineage(addLineage.build());
            assertEquals(id, result.getId());
          }
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

          if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
            DeleteLineage.Builder deleteLineage =
                DeleteLineage.newBuilder().setId(oldId).addInput(inputDatasetClient2);
            lineageServiceBlockingStubClient2.deleteLineage(deleteLineage.build());
          }
          DeleteLineage.Builder deleteLineage = DeleteLineage.newBuilder().setId(oldId);
          lineageServiceStub.deleteLineage(deleteLineage.build());

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

          findAllInputsOutputsResult =
              lineageServiceStub.findAllInputsOutputs(
                  FindAllInputsOutputs.newBuilder()
                      .addItems(LineageEntryBatchRequest.newBuilder().setId(id))
                      .build());
          checkInputsOutputs(findAllInputsOutputsResult, 2, 2);
          deleteLineage = DeleteLineage.newBuilder().setId(result.getId()).addOutput(outputExp);
          lineageServiceStub.deleteLineage(deleteLineage.build());
          findAllInputsOutputsResult =
              lineageServiceStub.findAllInputsOutputs(
                  FindAllInputsOutputs.newBuilder()
                      .addItems(LineageEntryBatchRequest.newBuilder().setId(id))
                      .build());
          checkInputsOutputs(findAllInputsOutputsResult, 2, 1);
          deleteLineage = DeleteLineage.newBuilder().setId(result.getId()).addInput(inputExp);
          lineageServiceStub.deleteLineage(deleteLineage.build());
          findAllInputsOutputsResult =
              lineageServiceStub.findAllInputsOutputs(
                  FindAllInputsOutputs.newBuilder()
                      .addItems(LineageEntryBatchRequest.newBuilder().setId(id))
                      .build());
          checkInputsOutputs(findAllInputsOutputsResult, 1, 1);

          deleteLineage =
              DeleteLineage.newBuilder()
                  .setId(result.getId())
                  .addInput(inputOutputExp)
                  .addOutput(outputDataset);
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
        deleteCommitRequest =
            DeleteCommitRequest.newBuilder()
                .setRepositoryId(
                    RepositoryIdentification.newBuilder().setRepoId(repositoryId).build())
                .setCommitSha(commit3Sha)
                .build();
        versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
        deleteCommitRequest =
            DeleteCommitRequest.newBuilder()
                .setRepositoryId(
                    RepositoryIdentification.newBuilder().setRepoId(repositoryId2).build())
                .setCommitSha(commit4Sha)
                .build();
        versioningServiceBlockingStubClient2.deleteCommit(deleteCommitRequest);
      }
    } finally {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repositoryId))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repositoryId2))
              .build();
      deleteResult = versioningServiceBlockingStubClient2.deleteRepository(deleteRepository);
    }

    LOGGER.info("Create and delete Lineage test stop................................");
  }

  public void checkInputsOutputs(
      FindAllInputsOutputs.Response findAllInputsOutputsResult, int inputsCount, int outputsCount) {
    Assert.assertEquals(1, findAllInputsOutputsResult.getInputsCount());
    LineageEntryBatchResponse inputs = findAllInputsOutputsResult.getInputs(0);
    Assert.assertEquals(1, inputs.getItemsCount());
    Assert.assertEquals(inputsCount, inputs.getItems(0).getItemsCount());
    Assert.assertEquals(1, findAllInputsOutputsResult.getOutputsCount());
    LineageEntryBatchResponse outputs = findAllInputsOutputsResult.getOutputs(0);
    Assert.assertEquals(1, outputs.getItemsCount());
    Assert.assertEquals(outputsCount, outputs.getItems(0).getItemsCount());
  }

  // TODO: dataset version
  private void deleteAll(/*List<DatasetVersion> datasetVersionList, */ Project project) {
    /*for (DatasetVersion datasetVersion1 : datasetVersionList) {
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
    }*/
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

  private DatasetVersion getDatasetVersion(
      List<DatasetVersion> datasetVersionList,
      DatasetVersionTest datasetVersionTest,
      DatasetVersionServiceBlockingStub datasetVersionServiceStub,
      DatasetTest datasetTest,
      DatasetServiceBlockingStub datasetServiceStub,
      String name) {
    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv" + name);
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    Dataset dataset = createDatasetResponse.getDataset();
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    CreateDatasetVersion createDatasetVersionRequest =
        datasetVersionTest.getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionList.add(datasetVersion);
    return datasetVersion;
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
      switch (o1.getType()) {
        case EXPERIMENT_RUN:
          return o1.getExternalId().compareTo(o2.getExternalId());
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
