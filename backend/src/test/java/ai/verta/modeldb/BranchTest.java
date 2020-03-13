package ai.verta.modeldb;

import static ai.verta.modeldb.CommitTest.getBlobFromPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import ai.verta.modeldb.versioning.CreateCommitRequest;
import ai.verta.modeldb.versioning.DatasetBlob;
import ai.verta.modeldb.versioning.DeleteBranchRequest;
import ai.verta.modeldb.versioning.DeleteCommitRequest;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.DeleteTagRequest;
import ai.verta.modeldb.versioning.GetBranchRequest;
import ai.verta.modeldb.versioning.GetTagRequest;
import ai.verta.modeldb.versioning.ListBranchCommitsRequest;
import ai.verta.modeldb.versioning.ListBranchesRequest;
import ai.verta.modeldb.versioning.ListTagsRequest;
import ai.verta.modeldb.versioning.PathDatasetBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.RepositoryNamedIdentification;
import ai.verta.modeldb.versioning.SetBranchRequest;
import ai.verta.modeldb.versioning.SetRepository;
import ai.verta.modeldb.versioning.SetRepository.Response;
import ai.verta.modeldb.versioning.SetTagRequest;
import ai.verta.modeldb.versioning.VersioningServiceGrpc;
import ai.verta.modeldb.versioning.VersioningServiceGrpc.VersioningServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
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
public class BranchTest {

  private static final Logger LOGGER = LogManager.getLogger(BranchTest.class);
  private static final String NAME = "repository_name";
  private static final String NAME_2 = "repository_name2";
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

  public static Long createRepository(VersioningServiceBlockingStub versioningServiceBlockingStub) {
    SetRepository setRepository =
        SetRepository.newBuilder()
            .setId(
                RepositoryIdentification.newBuilder()
                    .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME).build())
                    .build())
            .setRepository(Repository.newBuilder().setName(NAME))
            .build();
    Response result = versioningServiceBlockingStub.createRepository(setRepository);
    return result.getRepository().getId();
  }

  @Test
  public void addTagTest() {
    LOGGER.info("Add tags test start................................");

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
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setAuthor(authClientInterceptor.getClient1Email())
                    .setMessage("this is the test commit message")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(getBranchResponse.getCommit().getCommitSha())
                    .build())
            .addBlobs(
                BlobExpanded.newBuilder()
                    .setBlob(
                        Blob.newBuilder()
                            .setDataset(
                                DatasetBlob.newBuilder()
                                    .setPath(
                                        PathDatasetBlob.newBuilder()
                                            .addComponents(
                                                PathDatasetComponentBlob.newBuilder()
                                                    .setPath("/public/versioning.proto")
                                                    .setSize(2)
                                                    .setLastModifiedAtSource(
                                                        Calendar.getInstance().getTimeInMillis())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .addLocation("public")
                    .build())
            .build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);

    String tagName = "backend-commit-tag-1";
    SetTagRequest setTagRequest =
        SetTagRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setTag(tagName)
            .build();

    versioningServiceBlockingStub.setTag(setTagRequest);

    GetTagRequest getTagRequest =
        GetTagRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setTag(tagName)
            .build();
    GetTagRequest.Response getTagResponse = versioningServiceBlockingStub.getTag(getTagRequest);

    assertEquals(
        "Expected tag not found in response",
        commitResponse.getCommit(),
        getTagResponse.getCommit());

    ListTagsRequest listTagsRequest =
        ListTagsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();
    ListTagsRequest.Response listTagsResponse =
        versioningServiceBlockingStub.listTags(listTagsRequest);
    assertEquals(
        "Tag count not match with expected tag count", 1, listTagsResponse.getTotalRecords());
    assertTrue(
        "Expected tag not found in the response", listTagsResponse.getTagsList().contains(tagName));

    setTagRequest =
        SetTagRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setTag(tagName)
            .build();

    try {
      versioningServiceBlockingStub.setTag(setTagRequest);
      Assert.fail();
    } catch (StatusRuntimeException e) {
      Assert.assertEquals(Code.NOT_FOUND, e.getStatus().getCode());
    }

    DeleteTagRequest deleteTagRequest =
        DeleteTagRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setTag(tagName)
            .build();
    versioningServiceBlockingStub.deleteTag(deleteTagRequest);

    deleteTagRequest =
        DeleteTagRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setTag(tagName)
            .build();
    try {
      versioningServiceBlockingStub.deleteTag(deleteTagRequest);
      Assert.fail();
    } catch (StatusRuntimeException e) {
      Assert.assertEquals(Code.NOT_FOUND, e.getStatus().getCode());
    }

    listTagsRequest =
        ListTagsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();
    listTagsResponse = versioningServiceBlockingStub.listTags(listTagsRequest);
    assertEquals(
        "Tag count not match with expected tag count", 0, listTagsResponse.getTotalRecords());

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("Add tag test end................................");
  }

  @Test
  public void createGetListDeleteBranchTest() {
    LOGGER.info("branch test start................................");

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

    List<String> commitShaList = new ArrayList<>();
    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setAuthor(authClientInterceptor.getClient1Email())
                    .setMessage("this is the test commit message")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(getBranchResponse.getCommit().getCommitSha())
                    .build())
            .addBlobs(blobExpanded1)
            .build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitShaList.add(commitResponse.getCommit().getCommitSha());

    createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setAuthor(authClientInterceptor.getClient1Email())
                    .setMessage("this is the test commit message")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(commitResponse.getCommit().getCommitSha())
                    .build())
            .addBlobs(blobExpanded2)
            .build();

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitShaList.add(commitResponse.getCommit().getCommitSha());

    createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setAuthor(authClientInterceptor.getClient1Email())
                    .setMessage("this is the test commit message")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(commitResponse.getCommit().getCommitSha())
                    .build())
            .addBlobs(blobExpanded3)
            .build();

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitShaList.add(commitResponse.getCommit().getCommitSha());

    createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setAuthor(authClientInterceptor.getClient1Email())
                    .setMessage("this is the test commit message")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(commitResponse.getCommit().getCommitSha())
                    .build())
            .addBlobs(blobExpanded4)
            .build();

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitShaList.add(commitResponse.getCommit().getCommitSha());

    String branchName1 = "branch-commits-label-1";
    SetBranchRequest setBranchRequest =
        SetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(branchName1)
            .setCommitSha(commitShaList.get(0))
            .build();
    versioningServiceBlockingStub.setBranch(setBranchRequest);

    String branchName2 = "branch-commits-label-2";
    setBranchRequest =
        SetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(branchName2)
            .setCommitSha(commitShaList.get(1))
            .build();
    versioningServiceBlockingStub.setBranch(setBranchRequest);

    getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(branchName1)
            .build();
    getBranchResponse = versioningServiceBlockingStub.getBranch(getBranchRequest);
    Commit branchRootCommit = getBranchResponse.getCommit();
    Assert.assertEquals(
        "Expected commit not found in the response",
        commitShaList.get(0),
        branchRootCommit.getCommitSha());

    ListBranchesRequest listBranchesRequest =
        ListBranchesRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();
    ListBranchesRequest.Response listBranchesResponse =
        versioningServiceBlockingStub.listBranches(listBranchesRequest);
    Assert.assertEquals(
        "Branches count not match with expected branches count",
        3,
        listBranchesResponse.getBranchesCount());
    Assert.assertTrue(
        "Expected branch name not found in the response",
        listBranchesResponse.getBranchesList().contains(branchName1));
    Assert.assertTrue(
        "Expected branch name not found in the response",
        listBranchesResponse.getBranchesList().contains(branchName2));

    DeleteBranchRequest deleteBranchRequest =
        DeleteBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(branchName1)
            .build();
    versioningServiceBlockingStub.deleteBranch(deleteBranchRequest);

    getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(branchName1)
            .build();
    try {
      versioningServiceBlockingStub.getBranch(getBranchRequest);
      Assert.fail();
    } catch (StatusRuntimeException e) {
      Assert.assertEquals(Code.NOT_FOUND, e.getStatus().getCode());
      e.printStackTrace();
    }

    commitShaList.forEach(
        commitSha -> {
          DeleteCommitRequest deleteCommitRequest =
              DeleteCommitRequest.newBuilder()
                  .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
                  .setCommitSha(commitSha)
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

    LOGGER.info("Branch test end................................");
  }

  @Test
  public void branchTest() {
    LOGGER.info("branch test start................................");

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

    List<String> commitShaList = new ArrayList<>();
    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setAuthor(authClientInterceptor.getClient1Email())
                    .setMessage("this is the test commit message")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(getBranchResponse.getCommit().getCommitSha())
                    .build())
            .addBlobs(blobExpanded1)
            .build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitShaList.add(commitResponse.getCommit().getCommitSha());

    createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setAuthor(authClientInterceptor.getClient1Email())
                    .setMessage("this is the test commit message")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(commitResponse.getCommit().getCommitSha())
                    .build())
            .addBlobs(blobExpanded2)
            .build();

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitShaList.add(commitResponse.getCommit().getCommitSha());

    createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setAuthor(authClientInterceptor.getClient1Email())
                    .setMessage("this is the test commit message")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(commitResponse.getCommit().getCommitSha())
                    .build())
            .addBlobs(blobExpanded3)
            .build();

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitShaList.add(commitResponse.getCommit().getCommitSha());

    createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setAuthor(authClientInterceptor.getClient1Email())
                    .setMessage("this is the test commit message")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(commitResponse.getCommit().getCommitSha())
                    .build())
            .addBlobs(blobExpanded4)
            .build();

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    commitShaList.add(commitResponse.getCommit().getCommitSha());

    String branchName = "get-list-branch-commits";
    SetBranchRequest setBranchRequest =
        SetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(branchName)
            .setCommitSha(commitShaList.get(3))
            .build();
    versioningServiceBlockingStub.setBranch(setBranchRequest);

    getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(branchName)
            .build();
    getBranchResponse = versioningServiceBlockingStub.getBranch(getBranchRequest);
    Commit branchRootCommit = getBranchResponse.getCommit();
    Assert.assertEquals(
        "Expected commit not found in the response",
        commitShaList.get(3),
        branchRootCommit.getCommitSha());

    ListBranchCommitsRequest listBranchCommitsRequest =
        ListBranchCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(branchName)
            .build();
    ListBranchCommitsRequest.Response listBranchCommitsResponse =
        versioningServiceBlockingStub.listBranchCommits(listBranchCommitsRequest);
    Assert.assertEquals(
        "Commit count not match with expected commit count",
        5,
        listBranchCommitsResponse.getCommitsCount());

    commitShaList.forEach(
        commitSha -> {
          DeleteCommitRequest deleteCommitRequest =
              DeleteCommitRequest.newBuilder()
                  .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
                  .setCommitSha(commitSha)
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

    LOGGER.info("Branch test end................................");
  }
}
