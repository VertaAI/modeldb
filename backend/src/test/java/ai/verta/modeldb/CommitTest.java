package ai.verta.modeldb;

import static ai.verta.modeldb.RepositoryTest.createRepository;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.Commit;
import ai.verta.modeldb.versioning.ConfigBlob;
import ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob;
import ai.verta.modeldb.versioning.CreateCommitRequest;
import ai.verta.modeldb.versioning.DatasetBlob;
import ai.verta.modeldb.versioning.DeleteCommitRequest;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.GetBranchRequest;
import ai.verta.modeldb.versioning.GetCommitComponentRequest;
import ai.verta.modeldb.versioning.GetCommitRequest;
import ai.verta.modeldb.versioning.HyperparameterConfigBlob;
import ai.verta.modeldb.versioning.HyperparameterSetConfigBlob;
import ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob;
import ai.verta.modeldb.versioning.ListCommitBlobsRequest;
import ai.verta.modeldb.versioning.ListCommitsRequest;
import ai.verta.modeldb.versioning.PathDatasetBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
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

  static Blob getBlob(Blob.ContentCase contentCase) throws ModelDBException {
    switch (contentCase) {
      case DATASET:
        DatasetBlob datasetBlob = DatasetBlob.newBuilder().setPath(getPathDatasetBlob()).build();
        return Blob.newBuilder().setDataset(datasetBlob).build();
      case CODE:
        break;
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
                        HyperparameterValuesConfigBlob.newBuilder().setIntValue(0).build())
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
      throws ModelDBException {

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
  public void deleteCommitTest() throws ModelDBException {
    LOGGER.info("Delete of commit test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub);
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
    LOGGER.info("Delete of commit test end................................");
  }

  @Test
  public void listCommitsTest() throws ModelDBException {
    LOGGER.info("List of commits test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub);
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
        getCreateCommitRequest(id, 123, commitResponse.getCommit(), Blob.ContentCase.DATASET);
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
  public void configHyperparameterTest() throws ModelDBException {
    LOGGER.info("Hyperparameter config test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    List<Commit> commitList = new ArrayList<>();
    commitList.add(getBranchResponse.getCommit());
    CreateCommitRequest createCommitRequest =
        getCreateCommitRequest(id, 111, getBranchResponse.getCommit(), Blob.ContentCase.CONFIG);
    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitList.add(commitResponse.getCommit());

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

    commitList.forEach(
        commit -> {
          DeleteCommitRequest deleteCommitRequest =
              DeleteCommitRequest.newBuilder()
                  .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
                  .setCommitSha(commit.getCommitSha())
                  .build();
          versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
        });

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
  public void getCommitBlobTest() {
    LOGGER.info("Get commit blob test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub);

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
    Blob blob =
        Blob.newBuilder()
            .setDataset(
                DatasetBlob.newBuilder()
                    .setPath(
                        PathDatasetBlob.newBuilder()
                            .addComponents(
                                PathDatasetComponentBlob.newBuilder()
                                    .setPath(path)
                                    .setSize(2)
                                    .setLastModifiedAtSource(
                                        Calendar.getInstance().getTimeInMillis())
                                    .build())
                            .build())
                    .build())
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

    // TODO: Add Delete Commit code here

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("Get commit blob test end................................");
  }

  static Blob getBlobFromPath(String path) {
    return Blob.newBuilder()
        .setDataset(
            DatasetBlob.newBuilder()
                .setPath(
                    PathDatasetBlob.newBuilder()
                        .addComponents(
                            PathDatasetComponentBlob.newBuilder()
                                .setPath(path)
                                .setSize(2)
                                .setLastModifiedAtSource(Calendar.getInstance().getTimeInMillis())
                                .build())
                        .build())
                .build())
        .build();
  }

  @Test
  public void getCommitBlobListTest() {
    LOGGER.info("List commit blob test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub);

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
        BlobExpanded.newBuilder().setBlob(getBlobFromPath(path1)).addAllLocation(location1).build();

    String path2 = "/protos/proto/public/test.txt";
    List<String> location2 = new ArrayList<>();
    location2.add("modeldb");
    location2.add("environment.json");
    BlobExpanded blobExpanded2 =
        BlobExpanded.newBuilder().setBlob(getBlobFromPath(path2)).addAllLocation(location2).build();

    String path3 = "xyz.txt";
    List<String> location3 = new ArrayList<>();
    location3.add("modeldb.json");
    BlobExpanded blobExpanded3 =
        BlobExpanded.newBuilder().setBlob(getBlobFromPath(path3)).addAllLocation(location3).build();

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

    long id = createRepository(versioningServiceBlockingStub);
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
        BlobExpanded.newBuilder().setBlob(getBlobFromPath(path1)).addAllLocation(location1).build();

    String path2 = "/protos/proto/public/test.txt";
    List<String> location2 = new ArrayList<>();
    location2.add("modeldb");
    location2.add("environment");
    location2.add("environment.json");
    BlobExpanded blobExpanded2 =
        BlobExpanded.newBuilder().setBlob(getBlobFromPath(path2)).addAllLocation(location2).build();

    String path3 = "/protos/proto/public/test2.txt";
    List<String> location3 = new ArrayList<>();
    location3.add("modeldb");
    location3.add("dataset");
    location3.add("march");
    location3.add("dataset.json");
    BlobExpanded blobExpanded3 =
        BlobExpanded.newBuilder().setBlob(getBlobFromPath(path3)).addAllLocation(location3).build();

    String path4 = "xyz.txt";
    List<String> location4 = new ArrayList<>();
    location4.add("modeldb.json");
    BlobExpanded blobExpanded4 =
        BlobExpanded.newBuilder().setBlob(getBlobFromPath(path4)).addAllLocation(location4).build();

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

    LOGGER.info("List commit blob test end................................");
  }
}
