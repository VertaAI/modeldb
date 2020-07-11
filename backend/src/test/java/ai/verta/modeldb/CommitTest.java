package ai.verta.modeldb;

import static ai.verta.modeldb.RepositoryTest.NAME;
import static ai.verta.modeldb.RepositoryTest.createRepository;
import static org.junit.Assert.*;

import ai.verta.common.ArtifactPart;
import ai.verta.common.Pagination;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.cron_jobs.CronJobUtils;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.BlobDiff;
import ai.verta.modeldb.versioning.BlobDiff.ContentCase;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.BlobType;
import ai.verta.modeldb.versioning.CodeBlob;
import ai.verta.modeldb.versioning.Commit;
import ai.verta.modeldb.versioning.CommitMultipartVersionedBlobArtifact;
import ai.verta.modeldb.versioning.CommitVersionedBlobArtifactPart;
import ai.verta.modeldb.versioning.ConfigBlob;
import ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob;
import ai.verta.modeldb.versioning.CreateCommitRequest;
import ai.verta.modeldb.versioning.DatasetBlob;
import ai.verta.modeldb.versioning.DeleteCommitRequest;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.DeleteTagRequest;
import ai.verta.modeldb.versioning.DiffStatusEnum.DiffStatus;
import ai.verta.modeldb.versioning.DockerEnvironmentBlob;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.FindRepositoriesBlobs;
import ai.verta.modeldb.versioning.GetBranchRequest;
import ai.verta.modeldb.versioning.GetCommitComponentRequest;
import ai.verta.modeldb.versioning.GetCommitRequest;
import ai.verta.modeldb.versioning.GetCommittedVersionedBlobArtifactParts;
import ai.verta.modeldb.versioning.GetUrlForBlobVersioned;
import ai.verta.modeldb.versioning.GitCodeBlob;
import ai.verta.modeldb.versioning.HyperparameterConfigBlob;
import ai.verta.modeldb.versioning.HyperparameterSetConfigBlob;
import ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob;
import ai.verta.modeldb.versioning.ListCommitBlobsRequest;
import ai.verta.modeldb.versioning.ListCommitsRequest;
import ai.verta.modeldb.versioning.MergeRepositoryCommitsRequest;
import ai.verta.modeldb.versioning.NotebookCodeBlob;
import ai.verta.modeldb.versioning.PathDatasetBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentDiff;
import ai.verta.modeldb.versioning.PathDatasetDiff;
import ai.verta.modeldb.versioning.PythonEnvironmentBlob;
import ai.verta.modeldb.versioning.PythonEnvironmentDiff;
import ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob;
import ai.verta.modeldb.versioning.PythonRequirementEnvironmentDiff;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.RevertRepositoryCommitsRequest;
import ai.verta.modeldb.versioning.S3DatasetBlob;
import ai.verta.modeldb.versioning.S3DatasetComponentBlob;
import ai.verta.modeldb.versioning.SetBranchRequest;
import ai.verta.modeldb.versioning.SetTagRequest;
import ai.verta.modeldb.versioning.VersionEnvironmentBlob;
import ai.verta.modeldb.versioning.VersioningServiceGrpc;
import ai.verta.modeldb.versioning.VersioningServiceGrpc.VersioningServiceBlockingStub;
import ai.verta.modeldb.versioning.VersioningUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CommitTest {

  private static final Logger LOGGER = LogManager.getLogger(CommitTest.class);
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
  private static AuthClientInterceptor authClientInterceptor;
  private static App app;
  private static DeleteEntitiesCron deleteEntitiesCron;

  private static long time = Calendar.getInstance().getTimeInMillis();

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
      channelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
      client2ChannelBuilder.intercept(authClientInterceptor.getClient2AuthInterceptor());
    }
    deleteEntitiesCron =
        new DeleteEntitiesCron(authService, roleService, CronJobUtils.deleteEntitiesFrequency);
    deleteEntitiesCron.run();
    deleteEntitiesCron.run();
    deleteEntitiesCron.run();
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
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      authServiceChannel =
          ManagedChannelBuilder.forTarget(app.getAuthServerHost() + ":" + app.getAuthServerPort())
              .usePlaintext()
              .intercept(authClientInterceptor.getClient1AuthInterceptor())
              .build();
    }
  }

  private static PathDatasetComponentBlob getPathDatasetComponentBlob(String blobLocation) {
    return PathDatasetComponentBlob.newBuilder()
        .setPath(blobLocation)
        .setSize(2)
        .setLastModifiedAtSource(Calendar.getInstance().getTimeInMillis())
        .build();
  }

  private static PathDatasetBlob getPathDatasetBlob() {
    return PathDatasetBlob.newBuilder()
        .addComponents(
            getPathDatasetComponentBlob("/protos/proto/public/versioning/versioning.proto"))
        .build();
  }

  private Blob getPythonBlobFromRequirement(PythonRequirementEnvironmentBlob requirement) {
    return Blob.newBuilder()
        .setEnvironment(
            EnvironmentBlob.newBuilder()
                .setPython(
                    PythonEnvironmentBlob.newBuilder()
                        .setVersion(
                            VersionEnvironmentBlob.newBuilder().setMajor(3).setMinor(7).setPatch(5))
                        .addRequirements(requirement)
                        .addRequirements(
                            PythonRequirementEnvironmentBlob.newBuilder()
                                .setLibrary("verta")
                                .setConstraint("==")
                                .setVersion(
                                    VersionEnvironmentBlob.newBuilder().setMajor(14).setMinor(9)))))
        .build();
  }

  static Blob getBlob(Blob.ContentCase contentCase)
      throws ModelDBException, NoSuchAlgorithmException {
    switch (contentCase) {
      case DATASET:
        DatasetBlob datasetBlob = DatasetBlob.newBuilder().setPath(getPathDatasetBlob()).build();
        return Blob.newBuilder().setDataset(datasetBlob).build();
      case CODE:
        return getCodeBlobFromPath("abc");
      case ENVIRONMENT:
        break;
      case CONFIG:
        List<HyperparameterConfigBlob> hyperparameterConfigBlobs = getHyperparameterConfigList();
        List<HyperparameterSetConfigBlob> setConfigBlobs = getContinuesList();
        ConfigBlob configBlob =
            ConfigBlob.newBuilder()
                .addAllHyperparameters(hyperparameterConfigBlobs)
                .addAllHyperparameterSet(setConfigBlobs)
                .build();
        return Blob.newBuilder().setConfig(configBlob).build();
      case CONTENT_NOT_SET:
      default:
        throw new ModelDBException("Invalid blob type found", Status.Code.INVALID_ARGUMENT);
    }
    throw new ModelDBException("Invalid blob type found", Status.Code.INVALID_ARGUMENT);
  }

  public static Blob getHyperparameterConfigBlob(float value1, float value2) {
    List<HyperparameterConfigBlob> hyperparameterConfigBlobs = new ArrayList<>();
    hyperparameterConfigBlobs.add(
        HyperparameterConfigBlob.newBuilder()
            .setName("train")
            .setValue(HyperparameterValuesConfigBlob.newBuilder().setFloatValue(value1).build())
            .build());
    hyperparameterConfigBlobs.add(
        HyperparameterConfigBlob.newBuilder()
            .setName("tuning-blob")
            .setValue(HyperparameterValuesConfigBlob.newBuilder().setFloatValue(value2).build())
            .build());

    ConfigBlob configBlob =
        ConfigBlob.newBuilder().addAllHyperparameters(hyperparameterConfigBlobs).build();
    return Blob.newBuilder().setConfig(configBlob).build();
  }

  static List<HyperparameterConfigBlob> getHyperparameterConfigList() {
    List<HyperparameterConfigBlob> hyperparameterConfigBlobs = new ArrayList<>();
    hyperparameterConfigBlobs.add(
        HyperparameterConfigBlob.newBuilder()
            .setName("train")
            .setValue(HyperparameterValuesConfigBlob.newBuilder().setFloatValue(0.12F).build())
            .build());
    hyperparameterConfigBlobs.add(
        HyperparameterConfigBlob.newBuilder()
            .setName("tuning")
            .setValue(HyperparameterValuesConfigBlob.newBuilder().setFloatValue(0.9F).build())
            .build());
    return hyperparameterConfigBlobs;
  }

  static List<HyperparameterSetConfigBlob> getContinuesList() {
    List<HyperparameterSetConfigBlob> setConfigBlobs = new ArrayList<>();
    setConfigBlobs.add(
        HyperparameterSetConfigBlob.newBuilder()
            .setName("continues-hyperparameter-1")
            .setContinuous(
                ContinuousHyperparameterSetConfigBlob.newBuilder()
                    .setIntervalBegin(
                        HyperparameterValuesConfigBlob.newBuilder().setIntValue(2).build())
                    .setIntervalStep(
                        HyperparameterValuesConfigBlob.newBuilder().setIntValue(2).build())
                    .setIntervalEnd(
                        HyperparameterValuesConfigBlob.newBuilder().setIntValue(10).build())
                    .build())
            .build());
    setConfigBlobs.add(
        HyperparameterSetConfigBlob.newBuilder()
            .setName("continues-hyperparameter-2")
            .setContinuous(
                ContinuousHyperparameterSetConfigBlob.newBuilder()
                    .setIntervalBegin(
                        HyperparameterValuesConfigBlob.newBuilder().setIntValue(1).build())
                    .setIntervalStep(
                        HyperparameterValuesConfigBlob.newBuilder().setIntValue(1).build())
                    .setIntervalEnd(
                        HyperparameterValuesConfigBlob.newBuilder().setIntValue(10).build())
                    .build())
            .build());
    return setConfigBlobs;
  }

  public static CreateCommitRequest getCreateCommitRequest(
      Long repoId, long commitTime, Commit parentCommit, Blob.ContentCase contentCase)
      throws ModelDBException, NoSuchAlgorithmException {

    Commit commit =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(commitTime)
            .addParentShas(parentCommit.getCommitSha())
            .build();
    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId).build())
            .setCommit(commit)
            .addBlobs(
                BlobExpanded.newBuilder().setBlob(getBlob(contentCase)).addLocation("/").build())
            .build();

    return createCommitRequest;
  }

  @Test
  public void initialCommitTest() {
    LOGGER.info("initial commit test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);
    assertEquals(
        "Initial commit parameters not match with expected parameters",
        ModelDBConstants.INITIAL_COMMIT_MESSAGE,
        getBranchResponse.getCommit().getMessage());

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());
    LOGGER.info("Initial commit test end................................");
  }

  @Test
  public void createDeleteCommitTest() throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("Create & Delete of commit test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    CreateCommitRequest createCommitRequest =
        getCreateCommitRequest(id, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET);

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    assertTrue("Commit not found in response", commitResponse.hasCommit());

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    GetCommitRequest getCommitRequest =
        GetCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    try {
      versioningServiceBlockingStub.getCommit(getCommitRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());
    LOGGER.info("Create & Delete of commit test end................................");
  }

  @Test
  public void listCommitsTest() throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("List of commits test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);
    Commit parentCommit = getBranchResponse.getCommit();

    CreateCommitRequest createCommitRequest =
        getCreateCommitRequest(id, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET);
    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commit1 = commitResponse.getCommit();
    createCommitRequest =
        getCreateCommitRequest(id, 123, commitResponse.getCommit(), Blob.ContentCase.CONFIG);
    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commit2 = commitResponse.getCommit();
    createCommitRequest =
        getCreateCommitRequest(id, 450, commitResponse.getCommit(), Blob.ContentCase.DATASET);
    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commit3 = commitResponse.getCommit();
    createCommitRequest =
        getCreateCommitRequest(id, 500, commitResponse.getCommit(), Blob.ContentCase.DATASET);
    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commit4 = commitResponse.getCommit();
    List<Commit> commitList = new LinkedList<>();
    commitList.add(commit4);
    commitList.add(commit3);
    commitList.add(commit2);
    commitList.add(commit1);
    commitList.add(parentCommit);

    // Fetch all commits of repository
    ListCommitsRequest listCommitsRequest =
        ListCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();
    ListCommitsRequest.Response listCommitsResponse =
        versioningServiceBlockingStub.listCommits(listCommitsRequest);
    Assert.assertEquals(
        "Commit count not match with the expected count", 5, listCommitsResponse.getCommitsCount());
    Assert.assertEquals(
        "Commit list not match with expected commit list",
        commitList,
        listCommitsResponse.getCommitsList());
    Assert.assertEquals(
        "Commit list not match with expected commit list",
        commitList.get(1),
        listCommitsResponse.getCommits(1));

    // fetch all commits from base commit
    listCommitsRequest =
        ListCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitBase(commitList.get(2).getCommitSha())
            .build();
    listCommitsResponse = versioningServiceBlockingStub.listCommits(listCommitsRequest);
    Assert.assertEquals(
        "Commit count not match with the expected count", 3, listCommitsResponse.getCommitsCount());
    Assert.assertEquals(
        "Commit list not match with expected commit list",
        commitList.subList(0, 3),
        listCommitsResponse.getCommitsList());
    Assert.assertEquals(
        "Commit list not match with expected commit list",
        commitList.get(1),
        listCommitsResponse.getCommits(1));

    // fetch all commits from provided base commit to head commit
    listCommitsRequest =
        ListCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitBase(commitList.get(2).getCommitSha())
            .setCommitHead(commitList.get(1).getCommitSha())
            .build();
    listCommitsResponse = versioningServiceBlockingStub.listCommits(listCommitsRequest);
    Assert.assertEquals(
        "Commit count not match with the expected count", 2, listCommitsResponse.getCommitsCount());
    Assert.assertEquals(
        "Commit list not match with expected commit list",
        commitList.subList(1, 3),
        listCommitsResponse.getCommitsList());
    Assert.assertEquals(
        "Commit list not match with expected commit list",
        commitList.get(1),
        listCommitsResponse.getCommits(0));

    // Fetch commits by pagination
    listCommitsRequest =
        ListCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setPagination(Pagination.newBuilder().setPageNumber(1).setPageLimit(2).build())
            .build();
    listCommitsResponse = versioningServiceBlockingStub.listCommits(listCommitsRequest);
    Assert.assertEquals(
        "Commit count not match with the expected count", 5, listCommitsResponse.getTotalRecords());
    Assert.assertEquals(
        "Commit list count not match with expected commit list count",
        2,
        listCommitsResponse.getCommitsCount());
    Assert.assertEquals(
        "Commit list not match with expected commit list",
        commitList.get(0),
        listCommitsResponse.getCommits(0));

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("List of commits test end................................");
  }

  @Test
  public void getCommitsTest() throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("Get commits test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);
    Commit parentCommit = getBranchResponse.getCommit();

    CreateCommitRequest createCommitRequest =
        getCreateCommitRequest(id, 111, parentCommit, Blob.ContentCase.DATASET);
    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commit1 = commitResponse.getCommit();

    GetCommitRequest getCommitRequest =
        GetCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commit1.getCommitSha())
            .build();
    GetCommitRequest.Response getCommitResponse =
        versioningServiceBlockingStub.getCommit(getCommitRequest);
    assertEquals(
        "Commit not match with the expected commit", commit1, getCommitResponse.getCommit());

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(getCommitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("Get commits test end................................");
  }

  @Test
  public void configHyperparameterTest() throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("Hyperparameter config test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    CreateCommitRequest createCommitRequest =
        getCreateCommitRequest(id, 111, getBranchResponse.getCommit(), Blob.ContentCase.CONFIG);
    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    ListCommitBlobsRequest listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();

    ListCommitBlobsRequest.Response listCommitBlobsResponse =
        versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        1,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob count not match with expected blob count",
        2,
        listCommitBlobsResponse.getBlobs(0).getBlob().getConfig().getHyperparameterSetCount());
    Assert.assertEquals(
        "blob count not match with expected blob count",
        2,
        listCommitBlobsResponse.getBlobs(0).getBlob().getConfig().getHyperparametersCount());

    try {
      DeleteCommitRequest deleteCommitRequest =
          DeleteCommitRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
              .setCommitSha(getBranchResponse.getCommit().getCommitSha())
              .build();
      versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.FAILED_PRECONDITION.getCode(), status.getCode());
    }
    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("Hyperparameter config test end................................");
  }

  @Test
  public void getCommitComponentTest() {
    LOGGER.info("Get commit component test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);

    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    String path = "/protos/proto/public/versioning/versioning.proto";
    List<String> location = new ArrayList<>();
    location.add("modeldb");
    location.add("environment");
    location.add("train");
    Blob blob = getDatasetBlobFromPath(path);

    Commit.Builder commitBuilder =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .addParentShas(getBranchResponse.getCommit().getCommitSha());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      commitBuilder.setAuthor(authClientInterceptor.getClient1Email());
    }

    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.build())
            .addBlobs(BlobExpanded.newBuilder().setBlob(blob).addAllLocation(location).build())
            .build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    GetCommitComponentRequest getCommitBlobRequest =
        GetCommitComponentRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .addAllLocation(location)
            .build();
    GetCommitComponentRequest.Response getCommitBlobResponse =
        versioningServiceBlockingStub.getCommitComponent(getCommitBlobRequest);
    assertEquals(
        "Blob path not match with expected blob path",
        path,
        getCommitBlobResponse.getBlob().getDataset().getPath().getComponents(0).getPath());

    location.add("xyz");
    getCommitBlobRequest =
        GetCommitComponentRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .addAllLocation(location)
            .build();
    try {
      versioningServiceBlockingStub.getCommitComponent(getCommitBlobRequest);
      Assert.fail();
    } catch (StatusRuntimeException e) {
      Assert.assertEquals(Code.NOT_FOUND, e.getStatus().getCode());
      e.printStackTrace();
    }

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("Get commit blob test end................................");
  }

  static Blob getDatasetBlobFromPath(String path) {
    return Blob.newBuilder()
        .setDataset(
            DatasetBlob.newBuilder()
                .setPath(
                    PathDatasetBlob.newBuilder()
                        .addComponents(
                            PathDatasetComponentBlob.newBuilder()
                                .setPath(path)
                                .setSize(2)
                                .setLastModifiedAtSource(time)
                                .build())
                        .build())
                .build())
        .build();
  }

  static Blob getS3DatasetBlobFromPath(String path) {
    return Blob.newBuilder()
        .setDataset(
            DatasetBlob.newBuilder()
                .setS3(
                    S3DatasetBlob.newBuilder()
                        .addComponents(
                            S3DatasetComponentBlob.newBuilder()
                                .setPath(
                                    PathDatasetComponentBlob.newBuilder()
                                        .setPath(path)
                                        .setSize(2)
                                        .setLastModifiedAtSource(time)))))
        .build();
  }

  static Blob getCodeBlobFromPath(String branch) throws NoSuchAlgorithmException {
    GitCodeBlob gitCodeBlob =
        GitCodeBlob.newBuilder()
            .setBranch(branch)
            .setRepo(RepositoryTest.NAME)
            .setHash(FileHasher.getSha(""))
            .setIsDirty(false)
            .setTag("Tag-" + Calendar.getInstance().getTimeInMillis())
            .build();
    String path = "/protos/proto/public/versioning/versioning.proto";
    NotebookCodeBlob notebookCodeBlob =
        NotebookCodeBlob.newBuilder()
            .setGitRepo(gitCodeBlob)
            .setPath(
                PathDatasetComponentBlob.newBuilder()
                    .setPath(path)
                    .setSize(2)
                    .setLastModifiedAtSource(time)
                    .build())
            .build();
    return Blob.newBuilder()
        .setCode(CodeBlob.newBuilder().setNotebook(notebookCodeBlob).build())
        .build();
  }

  static Blob getEnvironmentBlobFromPath() throws NoSuchAlgorithmException {
    return Blob.newBuilder()
        .setEnvironment(
            EnvironmentBlob.newBuilder()
                .addCommandLine("docker pull vertaaiofficial/modeldb-backend:latest")
                .setDocker(
                    DockerEnvironmentBlob.newBuilder()
                        .setRepository(RepositoryTest.NAME)
                        .setSha(FileHasher.getSha(""))
                        .setTag("Tag-" + Calendar.getInstance().getTimeInMillis())
                        .build())
                .setPython(
                    PythonEnvironmentBlob.newBuilder()
                        .setVersion(
                            VersionEnvironmentBlob.newBuilder()
                                .setMajor(10)
                                .setMinor(1)
                                .setPatch(2)
                                .build())
                        .addConstraints(
                            PythonRequirementEnvironmentBlob.newBuilder()
                                .setVersion(
                                    VersionEnvironmentBlob.newBuilder()
                                        .setMajor(10)
                                        .setMinor(1)
                                        .setPatch(2)
                                        .build())
                                .setLibrary("logs")
                                .setConstraint("constraint-1")
                                .build())
                        .build())
                .build())
        .build();
  }

  @Test
  public void createCommitWith2SameBlobTest() {
    LOGGER.info("List commit blob test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);

    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    String path1 = "/protos/proto/public/versioning/versioning.proto";
    List<String> location1 = new ArrayList<>();
    location1.add("modeldb");
    location1.add("environment");
    location1.add("train.json"); // file
    BlobExpanded blobExpanded1 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path1))
            .addAllLocation(location1)
            .build();

    String path2 = "/protos/proto/public/test.txt";
    List<String> location2 = new ArrayList<>();
    location2.add("modeldb");
    location2.add("environment.json");
    BlobExpanded blobExpanded2 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path2))
            .addAllLocation(location2)
            .build();

    List<String> location3 = new ArrayList<>();
    location3.add("modeldb.json");
    BlobExpanded blobExpanded3 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path2))
            .addAllLocation(location3)
            .build();

    Commit.Builder commitBuilder =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .addParentShas(getBranchResponse.getCommit().getCommitSha());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      commitBuilder.setAuthor(authClientInterceptor.getClient1Email());
    }
    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.build())
            .addBlobs(blobExpanded1)
            .addBlobs(blobExpanded2)
            .addBlobs(blobExpanded3)
            .build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    ListCommitBlobsRequest listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();

    ListCommitBlobsRequest.Response listCommitBlobsResponse =
        versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        3,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob data not match with expected blob data",
        new HashSet<>(Arrays.asList(blobExpanded1, blobExpanded2, blobExpanded3)),
        new HashSet<>(listCommitBlobsResponse.getBlobsList()));

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("List commit blob test end................................");
  }

  @Test
  public void createCommitWithNoBlobTest() {
    LOGGER.info("createCommitWithNoBlobTest test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);

    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    Commit.Builder commitBuilder =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .addParentShas(getBranchResponse.getCommit().getCommitSha());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      commitBuilder.setAuthor(authClientInterceptor.getClient1Email());
    }
    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.build())
            .build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    ListCommitBlobsRequest listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();

    ListCommitBlobsRequest.Response listCommitBlobsResponse =
        versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        0,
        listCommitBlobsResponse.getBlobsCount());

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("List commit blob test end................................");
  }

  @Test
  public void getCommitBlobListTest() {
    LOGGER.info("List commit blob test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);

    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    String path1 = "/protos/proto/public/versioning/versioning.proto";
    List<String> location1 = new ArrayList<>();
    location1.add("modeldb");
    location1.add("environment");
    location1.add("train.json"); // file
    BlobExpanded blobExpanded1 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path1))
            .addAllLocation(location1)
            .build();

    String path2 = "/protos/proto/public/test.txt";
    List<String> location2 = new ArrayList<>();
    location2.add("modeldb");
    location2.add("environment.json");
    BlobExpanded blobExpanded2 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path2))
            .addAllLocation(location2)
            .build();

    String path3 = "xyz.txt";
    List<String> location3 = new ArrayList<>();
    location3.add("modeldb.json");
    BlobExpanded blobExpanded3 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path3))
            .addAllLocation(location3)
            .build();

    Commit.Builder commitBuilder =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .addParentShas(getBranchResponse.getCommit().getCommitSha());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      commitBuilder.setAuthor(authClientInterceptor.getClient1Email());
    }
    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.build())
            .addBlobs(blobExpanded1)
            .addBlobs(blobExpanded2)
            .addBlobs(blobExpanded3)
            .build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    ListCommitBlobsRequest listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .build();

    ListCommitBlobsRequest.Response listCommitBlobsResponse =
        versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        2,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob data not match with expected blob data",
        new HashSet<>(Arrays.asList(blobExpanded1, blobExpanded2)),
        new HashSet<>(listCommitBlobsResponse.getBlobsList()));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb.json")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        1,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob data not match with expected blob data",
        blobExpanded3,
        listCommitBlobsResponse.getBlobs(0));

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("List commit blob test end................................");
  }

  @Test
  public void getCommitBlobListUsecase2Test() {
    LOGGER.info("List commit blob test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    String path1 = "/protos/proto/public/versioning/versioning.proto";
    List<String> location1 = new ArrayList<>();
    location1.add("modeldb");
    location1.add("environment");
    location1.add("march");
    location1.add("train.json"); // file
    BlobExpanded blobExpanded1 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path1))
            .addAllLocation(location1)
            .build();

    String path2 = "/protos/proto/public/test.txt";
    List<String> location2 = new ArrayList<>();
    location2.add("modeldb");
    location2.add("environment");
    location2.add("environment.json");
    BlobExpanded blobExpanded2 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path2))
            .addAllLocation(location2)
            .build();

    String path3 = "/protos/proto/public/test2.txt";
    List<String> location3 = new ArrayList<>();
    location3.add("modeldb");
    location3.add("dataset");
    location3.add("march");
    location3.add("dataset.json");
    BlobExpanded blobExpanded3 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path3))
            .addAllLocation(location3)
            .build();

    String path4 = "xyz.txt";
    List<String> location4 = new ArrayList<>();
    location4.add("modeldb.json");
    BlobExpanded blobExpanded4 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path4))
            .addAllLocation(location4)
            .build();

    String path5 = "versioned_modeldb.txt";
    List<String> location5 = new ArrayList<>();
    location5.add("versioned");
    location5.add("modeldb.json");
    BlobExpanded blobExpanded5 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path5))
            .addAllLocation(location5)
            .build();

    Commit.Builder commitBuilder =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .addParentShas(getBranchResponse.getCommit().getCommitSha());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      commitBuilder.setAuthor(authClientInterceptor.getClient1Email());
    }

    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.build())
            .addBlobs(blobExpanded1)
            .addBlobs(blobExpanded2)
            .addBlobs(blobExpanded3)
            .addBlobs(blobExpanded4)
            .addBlobs(blobExpanded5)
            .build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    ListCommitBlobsRequest listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .addLocationPrefix("environment")
            .build();

    ListCommitBlobsRequest.Response listCommitBlobsResponse =
        versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        2,
        listCommitBlobsResponse.getBlobsCount());
    assertTrue(
        "blob data not match with expected blob data",
        listCommitBlobsResponse.getBlobsList().contains(blobExpanded1));
    assertTrue(
        "blob data not match with expected blob data",
        listCommitBlobsResponse.getBlobsList().contains(blobExpanded2));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .addLocationPrefix("dataset")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        1,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob data not match with expected blob data",
        blobExpanded3,
        listCommitBlobsResponse.getBlobs(0));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .addLocationPrefix("march")
            .addLocationPrefix("dataset")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        0,
        listCommitBlobsResponse.getBlobsCount());

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .addLocationPrefix("dataset")
            .addLocationPrefix("march")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        1,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob data not match with expected blob data",
        blobExpanded3,
        listCommitBlobsResponse.getBlobs(0));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        3,
        listCommitBlobsResponse.getBlobsCount());
    assertTrue(
        "blob data not match with expected blob data",
        listCommitBlobsResponse.getBlobsList().contains(blobExpanded1));
    assertTrue(
        "blob data not match with expected blob data",
        listCommitBlobsResponse.getBlobsList().contains(blobExpanded3));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("dataset.json")
            .build();

    try {
      versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
      Assert.fail();
    } catch (StatusRuntimeException e) {
      Assert.assertEquals(Code.NOT_FOUND, e.getStatus().getCode());
      e.printStackTrace();
    }

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addAllLocationPrefix(location5)
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        1,
        listCommitBlobsResponse.getBlobsCount());

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("List commit blob test end................................");
  }

  @Test
  public void getCommitCodeBlobListTest() throws NoSuchAlgorithmException {
    LOGGER.info("List commit code blob test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    String branch1 = "branch-1";
    List<String> location1 = new ArrayList<>();
    location1.add("modeldb");
    location1.add("environment");
    location1.add("march");
    location1.add("train.json"); // file
    BlobExpanded blobExpanded1 =
        BlobExpanded.newBuilder()
            .setBlob(getCodeBlobFromPath(branch1))
            .addAllLocation(location1)
            .build();

    List<String> location2 = new ArrayList<>();
    location2.add("modeldb");
    location2.add("environment");
    location2.add("environment.json");
    BlobExpanded blobExpanded2 =
        BlobExpanded.newBuilder()
            .setBlob(getCodeBlobFromPath(branch1))
            .addAllLocation(location2)
            .build();

    List<String> location3 = new ArrayList<>();
    location3.add("modeldb");
    location3.add("dataset");
    location3.add("march");
    location3.add("dataset.json");
    BlobExpanded blobExpanded3 =
        BlobExpanded.newBuilder()
            .setBlob(getCodeBlobFromPath(branch1))
            .addAllLocation(location3)
            .build();

    List<String> location4 = new ArrayList<>();
    location4.add("modeldb.json");
    BlobExpanded blobExpanded4 =
        BlobExpanded.newBuilder()
            .setBlob(getCodeBlobFromPath(branch1))
            .addAllLocation(location4)
            .build();

    Commit.Builder commitBuilder =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .addParentShas(getBranchResponse.getCommit().getCommitSha());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      commitBuilder.setAuthor(authClientInterceptor.getClient1Email());
    }

    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.build())
            .addBlobs(blobExpanded1)
            .addBlobs(blobExpanded2)
            .addBlobs(blobExpanded3)
            .addBlobs(blobExpanded4)
            .build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    ListCommitBlobsRequest listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .addLocationPrefix("environment")
            .build();

    ListCommitBlobsRequest.Response listCommitBlobsResponse =
        versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        2,
        listCommitBlobsResponse.getBlobsCount());
    assertTrue(
        "blob data not match with expected blob data",
        listCommitBlobsResponse.getBlobsList().contains(blobExpanded1));
    assertTrue(
        "blob data not match with expected blob data",
        listCommitBlobsResponse.getBlobsList().contains(blobExpanded2));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .addLocationPrefix("dataset")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        1,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob data not match with expected blob data",
        blobExpanded3,
        listCommitBlobsResponse.getBlobs(0));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .addLocationPrefix("march")
            .addLocationPrefix("dataset")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        0,
        listCommitBlobsResponse.getBlobsCount());

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .addLocationPrefix("dataset")
            .addLocationPrefix("march")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        1,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob data not match with expected blob data",
        blobExpanded3,
        listCommitBlobsResponse.getBlobs(0));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        3,
        listCommitBlobsResponse.getBlobsCount());
    assertTrue(
        "blob data not match with expected blob data",
        listCommitBlobsResponse.getBlobsList().contains(blobExpanded1));
    assertTrue(
        "blob data not match with expected blob data",
        listCommitBlobsResponse.getBlobsList().contains(blobExpanded3));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("dataset.json")
            .build();

    try {
      versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
      Assert.fail();
    } catch (StatusRuntimeException e) {
      Assert.assertEquals(Code.NOT_FOUND, e.getStatus().getCode());
      e.printStackTrace();
    }

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("List commit code blob test end................................");
  }

  @Test
  public void getCommitEnvironmentBlobListTest() throws NoSuchAlgorithmException {
    LOGGER.info("List commit environment blob test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    String branch1 = "branch-1";
    List<String> location1 = new ArrayList<>();
    location1.add("modeldb");
    location1.add("environment");
    location1.add("march");
    location1.add("train.json"); // file
    BlobExpanded blobExpanded1 =
        BlobExpanded.newBuilder()
            .setBlob(getEnvironmentBlobFromPath())
            .addAllLocation(location1)
            .build();

    List<String> location2 = new ArrayList<>();
    location2.add("modeldb");
    location2.add("environment");
    location2.add("environment.json");
    BlobExpanded blobExpanded2 =
        BlobExpanded.newBuilder()
            .setBlob(getEnvironmentBlobFromPath())
            .addAllLocation(location2)
            .build();

    List<String> location3 = new ArrayList<>();
    location3.add("modeldb");
    location3.add("dataset");
    location3.add("march");
    location3.add("dataset.json");
    BlobExpanded blobExpanded3 =
        BlobExpanded.newBuilder()
            .setBlob(getEnvironmentBlobFromPath())
            .addAllLocation(location3)
            .build();

    List<String> location4 = new ArrayList<>();
    location4.add("modeldb.json");
    BlobExpanded blobExpanded4 =
        BlobExpanded.newBuilder()
            .setBlob(getEnvironmentBlobFromPath())
            .addAllLocation(location4)
            .build();

    Commit.Builder commitBuilder =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .addParentShas(getBranchResponse.getCommit().getCommitSha());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      commitBuilder.setAuthor(authClientInterceptor.getClient1Email());
    }

    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.build())
            .addBlobs(blobExpanded1)
            .addBlobs(blobExpanded2)
            .addBlobs(blobExpanded3)
            .addBlobs(blobExpanded4)
            .build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    ListCommitBlobsRequest listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .addLocationPrefix("environment")
            .build();

    ListCommitBlobsRequest.Response listCommitBlobsResponse =
        versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        2,
        listCommitBlobsResponse.getBlobsCount());
    assertTrue(
        "blob data not match with expected blob data",
        listCommitBlobsResponse.getBlobsList().contains(blobExpanded1));
    assertTrue(
        "blob data not match with expected blob data",
        listCommitBlobsResponse.getBlobsList().contains(blobExpanded2));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .addLocationPrefix("dataset")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        1,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob data not match with expected blob data",
        blobExpanded3,
        listCommitBlobsResponse.getBlobs(0));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .addLocationPrefix("march")
            .addLocationPrefix("dataset")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        0,
        listCommitBlobsResponse.getBlobsCount());

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .addLocationPrefix("dataset")
            .addLocationPrefix("march")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        1,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob data not match with expected blob data",
        blobExpanded3,
        listCommitBlobsResponse.getBlobs(0));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb")
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        3,
        listCommitBlobsResponse.getBlobsCount());
    assertTrue(
        "blob data not match with expected blob data",
        listCommitBlobsResponse.getBlobsList().contains(blobExpanded1));
    assertTrue(
        "blob data not match with expected blob data",
        listCommitBlobsResponse.getBlobsList().contains(blobExpanded3));

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("dataset.json")
            .build();

    try {
      versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
      Assert.fail();
    } catch (StatusRuntimeException e) {
      Assert.assertEquals(Code.NOT_FOUND, e.getStatus().getCode());
      e.printStackTrace();
    }

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("List commit environment blob test end................................");
  }

  /**
   * Check parent commits exists or not when creating new commit. if parent commit not exists then
   * ModelDB throw the error with INVALID_ARGUMENT
   *
   * @throws ModelDBException modelDBException
   */
  @Test
  public void createDeleteCommitWithParentCommitExistsTest()
      throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("Check parent commits exists of commit test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    CreateCommitRequest createCommitRequest =
        getCreateCommitRequest(id, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET);

    CreateCommitRequest createCommitRequest1 = createCommitRequest.toBuilder().build();
    Commit commit =
        createCommitRequest1.toBuilder().getCommit().toBuilder().addParentShas("abc").build();
    createCommitRequest1 = createCommitRequest1.toBuilder().setCommit(commit).build();

    try {
      CreateCommitRequest.Response commitResponse =
          versioningServiceBlockingStub.createCommit(createCommitRequest1);
      assertTrue("Commit not found in response", commitResponse.hasCommit());
    } catch (StatusRuntimeException e) {
      Assert.assertEquals(Code.INVALID_ARGUMENT, e.getStatus().getCode());
      e.printStackTrace();
    }
    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    assertTrue("Commit not found in response", commitResponse.hasCommit());

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());
    LOGGER.info("Check parent commits exists of commit test end................................");
  }

  /**
   * When create and delete the commit we should have to update Repository 'updated_time' so this
   * test case check the repository 'updated_time' updated or not
   *
   * @throws ModelDBException modelDBException
   */
  @Test
  public void checkRepoUpdatedTimeWithCreateDeleteCommitTest()
      throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info(
        "Check repo updated time with Create & Delete of commit test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    CreateCommitRequest createCommitRequest =
        getCreateCommitRequest(id, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET);

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    assertTrue("Commit not found in response", commitResponse.hasCommit());

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    GetCommitRequest getCommitRequest =
        GetCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    try {
      versioningServiceBlockingStub.getCommit(getCommitRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());
    LOGGER.info(
        "Check repo updated time with Create & Delete of commit test end................................");
  }

  @Test
  public void deleteCommitHasHeadOfTwoBranchesTest()
      throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("branch test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    CreateCommitRequest createCommitRequest =
        getCreateCommitRequest(id, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET);

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commit1 = commitResponse.getCommit();

    createCommitRequest =
        getCreateCommitRequest(id, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET);

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commit2 = commitResponse.getCommit();

    List<Commit> commitShaList = new LinkedList<>();
    commitShaList.add(commit2);
    commitShaList.add(commit1);

    String branchName1 = "branch-commits-label-1";
    SetBranchRequest setBranchRequest =
        SetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(branchName1)
            .setCommitSha(commit1.getCommitSha())
            .build();
    versioningServiceBlockingStub.setBranch(setBranchRequest);

    String branchName2 = "branch-commits-label-2";
    setBranchRequest =
        SetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(branchName2)
            .setCommitSha(commit1.getCommitSha())
            .build();
    versioningServiceBlockingStub.setBranch(setBranchRequest);

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commit1.getCommitSha())
            .build();
    try {
      versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Assert.assertEquals(Code.FAILED_PRECONDITION, e.getStatus().getCode());
      e.printStackTrace();
    }

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("Branch test end................................");
  }

  /**
   * create repo creates the inti commit commit A is child of init commit B is child of A commit C
   * is child of B commit D is child of C
   *
   * <p>we revert commit B and base it on B we revert commit C and base it on A
   *
   * @throws ModelDBException
   * @throws NoSuchAlgorithmException
   */
  @Test
  public void revertCommitTest() throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("Revert commit test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    CreateCommitRequest createCommitRequestCommitA =
        getCreateCommitRequest(id, 111, getBranchResponse.getCommit(), Blob.ContentCase.CONFIG);

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequestCommitA);
    assertTrue("Commit not found in response", commitResponse.hasCommit());
    Commit commitA = commitResponse.getCommit();

    CreateCommitRequest createCommitRequestCommitB =
        getCreateCommitRequest(id, 112, commitA, Blob.ContentCase.DATASET);
    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequestCommitB);
    assertTrue("Commit not found in response", commitResponse.hasCommit());
    Commit commitB = commitResponse.getCommit();

    CreateCommitRequest createCommitRequestCommitC =
        getCreateCommitRequest(id, 113, commitB, Blob.ContentCase.DATASET);
    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequestCommitC);
    assertTrue("Commit not found in response", commitResponse.hasCommit());
    Commit commitC = commitResponse.getCommit();

    CreateCommitRequest createCommitRequestCommitD =
        getCreateCommitRequest(id, 114, commitC, Blob.ContentCase.CONFIG);
    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequestCommitD);
    assertTrue("Commit not found in response", commitResponse.hasCommit());
    Commit commitD = commitResponse.getCommit();

    RevertRepositoryCommitsRequest revertRepositoryCommitsRequest =
        RevertRepositoryCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitToRevertSha(commitB.getCommitSha())
            .setBaseCommitSha(commitB.getCommitSha())
            .build();
    RevertRepositoryCommitsRequest.Response revertCommitResponse =
        versioningServiceBlockingStub.revertRepositoryCommits(revertRepositoryCommitsRequest);
    assertTrue("Commit not found in response", commitResponse.hasCommit());
    Commit revertedCommit1 = revertCommitResponse.getCommit();
    assertEquals(
        "Revert message not match with expected message",
        VersioningUtils.revertCommitMessage(commitB),
        revertedCommit1.getMessage());

    ListCommitBlobsRequest listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(revertedCommit1.getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();

    ListCommitBlobsRequest.Response listCommitBlobsResponse =
        versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        createCommitRequestCommitB.getBlobsCount(),
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob count not match with expected blob count",
        createCommitRequestCommitB.getBlobsList(),
        listCommitBlobsResponse.getBlobsList());

    revertRepositoryCommitsRequest =
        RevertRepositoryCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitToRevertSha(commitC.getCommitSha())
            .setBaseCommitSha(commitA.getCommitSha())
            .build();
    revertCommitResponse =
        versioningServiceBlockingStub.revertRepositoryCommits(revertRepositoryCommitsRequest);
    assertTrue("Commit not found in response", commitResponse.hasCommit());
    Commit revertedCommit2 = revertCommitResponse.getCommit();
    assertEquals(
        "Revert message not match with expected message",
        VersioningUtils.revertCommitMessage(commitC),
        revertedCommit2.getMessage());

    listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(revertedCommit2.getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();

    listCommitBlobsResponse = versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        createCommitRequestCommitA.getBlobsCount(),
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob count not match with expected blob count",
        2,
        listCommitBlobsResponse.getBlobs(0).getBlob().getConfig().getHyperparameterSetCount());
    Assert.assertEquals(
        "blob count not match with expected blob count",
        2,
        listCommitBlobsResponse.getBlobs(0).getBlob().getConfig().getHyperparametersCount());

    for (Commit deleteCommit :
        new Commit[] {revertedCommit2, revertedCommit1, commitD, commitC, commitB, commitA}) {
      DeleteCommitRequest deleteCommitRequest =
          DeleteCommitRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
              .setCommitSha(deleteCommit.getCommitSha())
              .build();
      versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
    }

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());
    LOGGER.info("Revert commit test end................................");
  }

  @Test
  public void revertToMasterCommitNoBlobTest() throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("Revert commit test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    CreateCommitRequest createCommitRequestCommitA =
        getCreateCommitRequest(id, 111, getBranchResponse.getCommit(), Blob.ContentCase.CONFIG);

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequestCommitA);
    assertTrue("Commit not found in response", commitResponse.hasCommit());
    Commit commitA = commitResponse.getCommit();

    RevertRepositoryCommitsRequest revertRepositoryCommitsRequest =
        RevertRepositoryCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitToRevertSha(commitA.getCommitSha())
            .setBaseCommitSha(getBranchResponse.getCommit().getCommitSha())
            .build();
    RevertRepositoryCommitsRequest.Response revertCommitResponse =
        versioningServiceBlockingStub.revertRepositoryCommits(revertRepositoryCommitsRequest);
    assertTrue("Commit not found in response", commitResponse.hasCommit());
    Commit revertedCommit1 = revertCommitResponse.getCommit();
    assertEquals(
        "Revert message not match with expected message",
        VersioningUtils.revertCommitMessage(commitA),
        revertedCommit1.getMessage());

    for (Commit deleteCommit : new Commit[] {revertedCommit1, commitA}) {
      DeleteCommitRequest deleteCommitRequest =
          DeleteCommitRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
              .setCommitSha(deleteCommit.getCommitSha())
              .build();
      versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
    }

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());
    LOGGER.info("Revert commit test end................................");
  }

  @Test
  public void FindRepositoryBlobsTest() throws ModelDBException, NoSuchAlgorithmException {
    LOGGER.info("Find repository blobs test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    CreateCommitRequest createCommitRequest =
        getCreateCommitRequest(id, 111, getBranchResponse.getCommit(), Blob.ContentCase.CONFIG);
    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit configCommit = commitResponse.getCommit();

    FindRepositoriesBlobs findRepositoriesBlobs =
        FindRepositoriesBlobs.newBuilder()
            .addCommits(commitResponse.getCommit().getCommitSha())
            .build();

    FindRepositoriesBlobs.Response listCommitBlobsResponse =
        versioningServiceBlockingStub.findRepositoriesBlobs(findRepositoriesBlobs);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        1,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob count not match with expected blob count",
        2,
        listCommitBlobsResponse.getBlobs(0).getBlob().getConfig().getHyperparameterSetCount());
    Assert.assertEquals(
        "blob count not match with expected blob count",
        2,
        listCommitBlobsResponse.getBlobs(0).getBlob().getConfig().getHyperparametersCount());

    Commit commit =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(112)
            .addParentShas(commitResponse.getCommit().getCommitSha())
            .build();
    Location location = Location.newBuilder().addLocation("dataset").addLocation("test").build();
    createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commit)
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(getBlob(Blob.ContentCase.DATASET))
                    .addAllLocation(location.getLocationList())
                    .build())
            .build();

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);

    findRepositoriesBlobs =
        FindRepositoriesBlobs.newBuilder()
            .addCommits(configCommit.getCommitSha())
            .addBlobType(BlobType.CONFIG_BLOB)
            .addBlobType(BlobType.DATASET_BLOB)
            .build();

    listCommitBlobsResponse =
        versioningServiceBlockingStub.findRepositoriesBlobs(findRepositoriesBlobs);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        1,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob count not match with expected blob count",
        2,
        listCommitBlobsResponse.getBlobs(0).getBlob().getConfig().getHyperparameterSetCount());

    findRepositoriesBlobs =
        FindRepositoriesBlobs.newBuilder()
            .addBlobType(BlobType.CONFIG_BLOB)
            .addBlobType(BlobType.DATASET_BLOB)
            .build();

    listCommitBlobsResponse =
        versioningServiceBlockingStub.findRepositoriesBlobs(findRepositoriesBlobs);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        2,
        listCommitBlobsResponse.getBlobsCount());

    try {
      DeleteCommitRequest deleteCommitRequest =
          DeleteCommitRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
              .setCommitSha(getBranchResponse.getCommit().getCommitSha())
              .build();
      versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.FAILED_PRECONDITION.getCode(), status.getCode());
    }
    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("Find repository blobs test end................................");
  }

  @Test
  public void checkDuplicateLocationPathWhenSingleBlobTest() {
    LOGGER.info(
        "Check duplication when log single blob test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);

    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    String path1 = "xyz.txt";
    List<String> location1 = new ArrayList<>();
    location1.add("modeldb.json");
    BlobExpanded blobExpanded1 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path1))
            .addAllLocation(location1)
            .build();

    Commit.Builder commitBuilder =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .addParentShas(getBranchResponse.getCommit().getCommitSha());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      commitBuilder.setAuthor(authClientInterceptor.getClient1Email());
    }
    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.build())
            .addBlobs(blobExpanded1)
            .build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    ListCommitBlobsRequest listCommitBlobsRequest =
        ListCommitBlobsRequest.newBuilder()
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .addLocationPrefix("modeldb.json")
            .build();

    ListCommitBlobsRequest.Response listCommitBlobsResponse =
        versioningServiceBlockingStub.listCommitBlobs(listCommitBlobsRequest);
    Assert.assertEquals(
        "blob count not match with expected blob count",
        1,
        listCommitBlobsResponse.getBlobsCount());
    Assert.assertEquals(
        "blob data not match with expected blob data",
        blobExpanded1,
        listCommitBlobsResponse.getBlobs(0));
    assertEquals(
        "Multiple location found for single commit blob",
        1,
        listCommitBlobsResponse.getBlobs(0).getLocationCount());

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("Check duplication when log single blob test end................................");
  }

  @Test
  public void deleteCommitWithTagsTest() throws NoSuchAlgorithmException, ModelDBException {
    LOGGER.info("Delete commit with tags test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    CreateCommitRequest createCommitRequest =
        CommitTest.getCreateCommitRequest(
            id, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET);

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    assertTrue("Commit not found in response", commitResponse.hasCommit());

    String tag = "v1.0";
    SetTagRequest setTagRequest =
        SetTagRequest.newBuilder()
            .setTag(tag)
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();
    versioningServiceBlockingStub.setTag(setTagRequest);

    DeleteCommitRequest deleteCommitRequest =
        DeleteCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .build();
    try {
      versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.FAILED_PRECONDITION.getCode(), status.getCode());
    }

    DeleteTagRequest deleteTagRequest =
        DeleteTagRequest.newBuilder()
            .setTag(tag)
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();
    versioningServiceBlockingStub.deleteTag(deleteTagRequest);

    versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());
    LOGGER.info("Delete commit with tags test end................................");
  }

  @Test
  @Ignore
  public void getURLForVersionedBlob() throws IOException {
    LOGGER.info("Get Url for VersionedBlob test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);

    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    String path1 = "verta/test/test1.txt";
    String path2 = "verta/test/test2.txt";
    String internalPath1 = "test/internalBlobPaths/blobs/test1.txt";
    String internalPath2 = "test/internalBlobPaths/blobs/test2.txt";
    List<String> location = new ArrayList<>();
    location.add("versioned");
    location.add("s3_versioned");
    // location.add("test.txt");

    Blob blob =
        Blob.newBuilder()
            .setDataset(
                DatasetBlob.newBuilder()
                    .setS3(
                        S3DatasetBlob.newBuilder()
                            .addComponents(
                                S3DatasetComponentBlob.newBuilder()
                                    .setS3VersionId("1.0")
                                    .setPath(
                                        PathDatasetComponentBlob.newBuilder()
                                            .setPath(path1)
                                            .setSize(2)
                                            .setLastModifiedAtSource(time)
                                            .setInternalVersionedPath(internalPath1)
                                            .build())
                                    .build())
                            .addComponents(
                                S3DatasetComponentBlob.newBuilder()
                                    .setS3VersionId("1.0")
                                    .setPath(
                                        PathDatasetComponentBlob.newBuilder()
                                            .setPath(path2)
                                            .setSize(2)
                                            .setLastModifiedAtSource(time)
                                            .setInternalVersionedPath(internalPath2)
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();
    BlobExpanded blobExpanded =
        BlobExpanded.newBuilder().setBlob(blob).addAllLocation(location).build();

    Commit.Builder commitBuilder =
        Commit.newBuilder()
            .setMessage("this is the test commit message")
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .addParentShas(getBranchResponse.getCommit().getCommitSha());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      commitBuilder.setAuthor(authClientInterceptor.getClient1Email());
    }
    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.build())
            .addBlobs(blobExpanded)
            .build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    try {
      GetUrlForBlobVersioned getUrlForVersionedBlob =
          GetUrlForBlobVersioned.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
              .setCommitSha(commitResponse.getCommit().getCommitSha())
              .addAllLocation(location)
              .setPathDatasetComponentBlobPath(path1)
              .setMethod(ModelDBConstants.PUT)
              .setPartNumber(1)
              .build();
      GetUrlForBlobVersioned.Response getUrlForVersionedBlobResponse =
          versioningServiceBlockingStub.getUrlForBlobVersioned(getUrlForVersionedBlob);
      String presignedUrl1 = getUrlForVersionedBlobResponse.getUrl();
      assertNotNull("Presigned url not match with expected presigned url", presignedUrl1);

      getUrlForVersionedBlob = getUrlForVersionedBlob.toBuilder().setPartNumber(2).build();
      getUrlForVersionedBlobResponse =
          versioningServiceBlockingStub.getUrlForBlobVersioned(getUrlForVersionedBlob);
      String presignedUrl2 = getUrlForVersionedBlobResponse.getUrl();
      assertNotNull("Presigned url not match with expected presigned url", presignedUrl2);

      // Create the connection and use it to upload the new object using the pre-signed URL.
      HttpURLConnection connection = (HttpURLConnection) new URL(presignedUrl1).openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("PUT");
      OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
      for (int i = 0; i < 130000; ++i) {
        out.write("This text uploaded as an object via presigned URL.");
      }
      out.close();

      // Check the HTTP response code. To complete the upload and make the object available,
      // you must interact with the connection object in some way.
      connection.getResponseCode();
      String etag1 = connection.getHeaderField("ETag");
      connection = (HttpURLConnection) new URL(presignedUrl2).openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("PUT");
      out = new OutputStreamWriter(connection.getOutputStream());
      for (int i = 0; i < 120000; ++i) {
        out.write("This text uploaded as an object via presigned URL2.");
      }
      out.close();

      // Check the HTTP response code. To complete the upload and make the object available,
      // you must interact with the connection object in some way.
      connection.getResponseCode();
      String etag2 = connection.getHeaderField("ETag");

      CommitVersionedBlobArtifactPart.Response p1 =
          versioningServiceBlockingStub.commitVersionedBlobArtifactPart(
              CommitVersionedBlobArtifactPart.newBuilder()
                  .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
                  .setCommitSha(commitResponse.getCommit().getCommitSha())
                  .addAllLocation(location)
                  .setPathDatasetComponentBlobPath(path1)
                  .setArtifactPart(
                      ArtifactPart.newBuilder()
                          .setEtag(etag1.replaceAll("\"", ""))
                          .setPartNumber(1))
                  .build());
      CommitVersionedBlobArtifactPart.Response p2 =
          versioningServiceBlockingStub.commitVersionedBlobArtifactPart(
              CommitVersionedBlobArtifactPart.newBuilder()
                  .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
                  .setCommitSha(commitResponse.getCommit().getCommitSha())
                  .addAllLocation(location)
                  .setPathDatasetComponentBlobPath(path1)
                  .setArtifactPart(
                      ArtifactPart.newBuilder()
                          .setEtag(etag2.replaceAll("\"", ""))
                          .setPartNumber(2))
                  .build());
      GetCommittedVersionedBlobArtifactParts.Response committedArtifactParts =
          versioningServiceBlockingStub.getCommittedVersionedBlobArtifactParts(
              GetCommittedVersionedBlobArtifactParts.newBuilder()
                  .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
                  .setCommitSha(commitResponse.getCommit().getCommitSha())
                  .addAllLocation(location)
                  .setPathDatasetComponentBlobPath(path1)
                  .build());
      CommitMultipartVersionedBlobArtifact.Response commitMultipartArtifact =
          versioningServiceBlockingStub.commitMultipartVersionedBlobArtifact(
              CommitMultipartVersionedBlobArtifact.newBuilder()
                  .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
                  .setCommitSha(commitResponse.getCommit().getCommitSha())
                  .addAllLocation(location)
                  .setPathDatasetComponentBlobPath(path1)
                  .build());
      GetCommittedVersionedBlobArtifactParts.Response committedVersionedBlobArtifactParts =
          versioningServiceBlockingStub.getCommittedVersionedBlobArtifactParts(
              GetCommittedVersionedBlobArtifactParts.newBuilder()
                  .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
                  .setCommitSha(commitResponse.getCommit().getCommitSha())
                  .addAllLocation(location)
                  .setPathDatasetComponentBlobPath(path1)
                  .build());
    } finally {
      DeleteCommitRequest deleteCommitRequest =
          DeleteCommitRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
              .setCommitSha(commitResponse.getCommit().getCommitSha())
              .build();
      versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);

      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      Assert.assertTrue(deleteResult.getStatus());
    }

    LOGGER.info("Get Url for VersionedBlob test stop................................");
  }

  @Test
  public void mergeConflictTest() {
    LOGGER.info("merge Conflict test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id =
        createRepository(versioningServiceBlockingStub, RepositoryTest.NAME + "mergeConflict");

    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    String path1 = "s3://verta-scala-demo-super-big";
    List<String> location1 = new ArrayList<>();
    location1.add("blob");
    BlobExpanded blobExpanded1 =
        BlobExpanded.newBuilder()
            .setBlob(getS3DatasetBlobFromPath(path1))
            .addAllLocation(location1)
            .build();

    String path2 = "testdir/testsubdir/testfile2";
    List<String> location2 = new ArrayList<>();
    location2.add("blob");
    BlobExpanded blobExpanded2 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path2))
            .addAllLocation(location2)
            .build();

    Commit.Builder commitBuilder =
        Commit.newBuilder()
            .setMessage("s3blob")
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .addParentShas(getBranchResponse.getCommit().getCommitSha());

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      commitBuilder.setAuthor(authClientInterceptor.getClient1Email());
    }
    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.build())
            .addBlobs(blobExpanded1)
            .build();

    CreateCommitRequest.Response commitResponse1 =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.setMessage("pathblob").build())
            .addBlobs(blobExpanded2)
            .build();
    CreateCommitRequest.Response commitResponse2 =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    MergeRepositoryCommitsRequest repositoryMergeRequest =
        MergeRepositoryCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitShaA(commitResponse1.getCommit().getCommitSha())
            .setCommitShaB(commitResponse2.getCommit().getCommitSha())
            .build();
    MergeRepositoryCommitsRequest.Response mergeReponse1 =
        versioningServiceBlockingStub.mergeRepositoryCommits(repositoryMergeRequest);

    Assert.assertTrue(
        "there shouldn't be a commit", mergeReponse1.getCommit().getCommitSha() == "");
    Assert.assertTrue("conflicts should be non empty", !mergeReponse1.getConflictsList().isEmpty());
    Assert.assertTrue("there should be 2 conflicts", mergeReponse1.getConflictsList().size() == 2);
    BlobDiff diff = mergeReponse1.getConflictsList().get(0);
    Assert.assertTrue(
        "there should be a dataset diff", diff.getContentCase() == ContentCase.DATASET);
    Assert.assertTrue("diff location should be blob", diff.getLocation(0).equalsIgnoreCase("blob"));
    Assert.assertTrue(
        "diff status should be conflicted", diff.getStatus() == DiffStatus.CONFLICTED);
    if (diff.getDataset().getContentCase().getNumber() != 2) {
      diff = mergeReponse1.getConflictsList().get(1);
    }
    PathDatasetDiff pathDiff = diff.getDataset().getPath();
    Assert.assertTrue("path diff should have one component", pathDiff.getComponentsCount() == 1);
    PathDatasetComponentDiff componentDiff = pathDiff.getComponents(0);
    Assert.assertTrue(
        "component diff does not have a A",
        componentDiff.getA().equals(PathDatasetComponentBlob.getDefaultInstance()));
    Assert.assertTrue(
        "component diff does have a B",
        !componentDiff.getB().equals(PathDatasetComponentBlob.getDefaultInstance()));
    Assert.assertTrue(
        "component diff does have a B", componentDiff.getB().getPath().equalsIgnoreCase(path2));
    Assert.assertTrue(
        "component diff does not have a C",
        componentDiff.getC().equals(PathDatasetComponentBlob.getDefaultInstance()));
    for (Commit commit : new Commit[] {commitResponse1.getCommit(), commitResponse2.getCommit()}) {
      DeleteCommitRequest deleteCommitRequest =
          DeleteCommitRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
              .setCommitSha(commit.getCommitSha())
              .build();
      versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
    }

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("merge Conflict test end................................");
  }

  @Test
  public void mergeConflictBugTest() {
    LOGGER.info("merge Conflict test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id =
        createRepository(versioningServiceBlockingStub, RepositoryTest.NAME + "mergeConflictBug");

    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    Commit.Builder commitBuilder =
        Commit.newBuilder()
            .setMessage("pytest1")
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .addParentShas(getBranchResponse.getCommit().getCommitSha());

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      commitBuilder.setAuthor(authClientInterceptor.getClient1Email());
    }

    List<String> location1 = new ArrayList<>();
    location1.add("env");
    PythonRequirementEnvironmentBlob requirement =
        PythonRequirementEnvironmentBlob.newBuilder()
            .setLibrary("pytest")
            .setConstraint("==")
            .setVersion(VersionEnvironmentBlob.newBuilder().setMajor(1))
            .build();
    BlobExpanded blobExpanded1 =
        BlobExpanded.newBuilder()
            .setBlob(getPythonBlobFromRequirement(requirement))
            .addAllLocation(location1)
            .build();

    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.build())
            .addBlobs(blobExpanded1)
            .build();

    CreateCommitRequest.Response commitResponse1 =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    requirement =
        PythonRequirementEnvironmentBlob.newBuilder()
            .setLibrary("pytest")
            .setConstraint("==")
            .setVersion(VersionEnvironmentBlob.newBuilder().setMajor(2))
            .build();
    BlobExpanded blobExpanded2 =
        BlobExpanded.newBuilder()
            .setBlob(getPythonBlobFromRequirement(requirement))
            .addAllLocation(location1)
            .build();

    createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(commitBuilder.setMessage("pytest2").build())
            .addBlobs(blobExpanded2)
            .build();
    CreateCommitRequest.Response commitResponse2 =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    MergeRepositoryCommitsRequest repositoryMergeRequest =
        MergeRepositoryCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitShaA(commitResponse1.getCommit().getCommitSha())
            .setCommitShaB(commitResponse2.getCommit().getCommitSha())
            .build();
    MergeRepositoryCommitsRequest.Response mergeReponse1 =
        versioningServiceBlockingStub.mergeRepositoryCommits(repositoryMergeRequest);

    Assert.assertTrue(
        "there shouldn't be a commit", mergeReponse1.getCommit().getCommitSha() == "");
    Assert.assertTrue("conflicts should be non empty", !mergeReponse1.getConflictsList().isEmpty());
    Assert.assertTrue("there should be 2 conflicts", mergeReponse1.getConflictsList().size() == 1);
    BlobDiff diff = mergeReponse1.getConflictsList().get(0);
    Assert.assertTrue(
        "there should be a environment diff", diff.getContentCase() == ContentCase.ENVIRONMENT);
    Assert.assertTrue("diff location should be env", diff.getLocation(0).equalsIgnoreCase("env"));
    Assert.assertTrue(
        "diff status should be conflicted", diff.getStatus() == DiffStatus.CONFLICTED);

    PythonEnvironmentDiff pythonDiff = diff.getEnvironment().getPython();
    Assert.assertTrue(
        "requirement count should have one element", pythonDiff.getRequirementsCount() == 1);
    PythonRequirementEnvironmentDiff reqDiff = pythonDiff.getRequirements(0);
    Assert.assertTrue(
        "python diff does have a A",
        reqDiff.getA().getLibrary().equals("pytest")
            && reqDiff.getA().getVersion().getMajor() == 1);
    Assert.assertTrue(
        "python diff does have a B",
        reqDiff.getB().getLibrary().equals("pytest")
            && reqDiff.getB().getVersion().getMajor() == 2);
    Assert.assertTrue(
        "python diff does not have a C",
        reqDiff.getC().equals(PythonRequirementEnvironmentBlob.getDefaultInstance()));
    for (Commit commit : new Commit[] {commitResponse1.getCommit(), commitResponse2.getCommit()}) {
      DeleteCommitRequest deleteCommitRequest =
          DeleteCommitRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
              .setCommitSha(commit.getCommitSha())
              .build();
      versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
    }

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("merge Conflict Bug test end................................");
  }
}
