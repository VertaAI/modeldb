package ai.verta.modeldb;

import static ai.verta.modeldb.CommitTest.getBlobFromPath;
import static ai.verta.modeldb.RepositoryTest.createRepository;

import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.BlobDiff;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.Commit;
import ai.verta.modeldb.versioning.ComputeRepositoryDiffRequest;
import ai.verta.modeldb.versioning.CreateCommitRequest;
import ai.verta.modeldb.versioning.DeleteCommitRequest;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.GetBranchRequest;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.VersioningServiceGrpc;
import ai.verta.modeldb.versioning.VersioningServiceGrpc.VersioningServiceBlockingStub;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DiffTest {

  private static final Logger LOGGER = LogManager.getLogger(DiffTest.class);
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

  private final int blobType;
  private final int commitType;

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{0, 0}});
  }

  /*@Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
            {0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}
        });
  }*/

  public DiffTest(int blobType, int commitType) {
    this.blobType = blobType;
    this.commitType = commitType;
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

  @Test
  public void computeRepositoryDiffTest() throws InvalidProtocolBufferException {
    LOGGER.info("Compute repository diff test start................................");

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

    BlobExpanded[] blobExpandedArray;
    BlobDiff[] blobDiffsArray;
    if (commitType == 0) {
      blobExpandedArray = createBlobs(blobType);
      blobDiffsArray = null;
    } else {
      blobExpandedArray = null;
      blobDiffsArray = createDiffs(blobType);
    }

    CreateCommitRequest.Builder createCommitRequestBuilder =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setAuthor(authClientInterceptor.getClient1Email())
                    .setMessage("this is the test commit message")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(getBranchResponse.getCommit().getCommitSha())
                    .build());
    CreateCommitRequest createCommitRequest;
    if (commitType == 0) {
      createCommitRequest =
          createCommitRequestBuilder
              .addBlobs(blobExpandedArray[0])
              .addBlobs(blobExpandedArray[1])
              .addBlobs(blobExpandedArray[2])
              .addBlobs(blobExpandedArray[3])
              .build();
    } else {
      createCommitRequest =
          createCommitRequestBuilder
              .setCommitBase(getBranchResponse.getCommit().getCommitSha())
              .addDiffs(blobDiffsArray[0])
              .addDiffs(blobDiffsArray[1])
              .addDiffs(blobDiffsArray[2])
              .addDiffs(blobDiffsArray[3])
              .build();
    }

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commitA = commitResponse.getCommit();

    createCommitRequestBuilder =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setAuthor(authClientInterceptor.getClient1Email())
                    .setMessage("this is the test commit message")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(commitA.getCommitSha())
                    .build());
    if (commitType == 0) {
      blobExpandedArray[2] = modifiedBlobExpanded(commitType);
    } else {
      blobDiffsArray[2] = modifiedBlobDiff(commitType);
    }
    if (commitType == 0) {
      createCommitRequest =
          createCommitRequestBuilder
              .addBlobs(blobExpandedArray[1])
              .addBlobs(blobExpandedArray[2])
              .addBlobs(blobExpandedArray[4])
              .build();
    } else {
      createCommitRequest =
          createCommitRequestBuilder
              .setCommitBase(getBranchResponse.getCommit().getCommitSha())
              .addDiffs(blobDiffsArray[1])
              .addDiffs(blobDiffsArray[2])
              .addDiffs(blobDiffsArray[4])
              .build();
    }

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commitB = commitResponse.getCommit();

    ComputeRepositoryDiffRequest repositoryDiffRequest =
        ComputeRepositoryDiffRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitA(commitA.getCommitSha())
            .setCommitB(commitB.getCommitSha())
            .build();
    ComputeRepositoryDiffRequest.Response repositoryDiffResponse =
        versioningServiceBlockingStub.computeRepositoryDiff(repositoryDiffRequest);
    LOGGER.info("Diff Response: {}", ModelDBUtils.getStringFromProtoObject(repositoryDiffResponse));
    LOGGER.info("Diff Response: {}", repositoryDiffResponse);
    List<BlobDiff> blobDiffs = repositoryDiffResponse.getDiffsList();
    Assert.assertEquals("blob count not match with expected blob count", 4, blobDiffs.size());
    Map<ProtocolStringList, BlobDiff> result = blobDiffs.stream()
        .collect(Collectors.toMap(BlobDiff::getLocationList, blobDiff -> blobDiff));

    for (Commit commit : new Commit[] {commitA, commitB}) {
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

    LOGGER.info("Compute repository diff test end................................");
  }

  private BlobExpanded modifiedBlobExpanded(int commitType) {
    String path3 = "/protos/proto/public/test22.txt";
    List<String> location3 = new ArrayList<>();
    location3.add("modeldb");
    location3.add("dataset");
    location3.add("march");
    location3.add("dataset.json");
    BlobExpanded blobExpanded3 =
        BlobExpanded.newBuilder().setBlob(getBlobFromPath(path3)).addAllLocation(location3).build();

    return blobExpanded3;
  }

  private BlobDiff modifiedBlobDiff(int commitType) {
    return null;
  }

  private BlobDiff[] createDiffs(int blobType) {
    return null;
  }

  private BlobExpanded[] createBlobs(int blobType) {
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

    String path5 = "/protos/proto/public/algebra.txt";
    List<String> location5 = new ArrayList<>();
    location5.add("maths/algebra");
    BlobExpanded blobExpanded5 =
        BlobExpanded.newBuilder().setBlob(getBlobFromPath(path5)).addAllLocation(location5).build();

    return new BlobExpanded[] {
      blobExpanded1, blobExpanded2, blobExpanded3, blobExpanded4, blobExpanded5
    };
  }
}
