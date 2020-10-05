package ai.verta.modeldb;

import static ai.verta.modeldb.RepositoryTest.createRepository;

import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.CodeBlob;
import ai.verta.modeldb.versioning.Commit;
import ai.verta.modeldb.versioning.ConfigBlob;
import ai.verta.modeldb.versioning.CreateCommitRequest;
import ai.verta.modeldb.versioning.DatasetBlob;
import ai.verta.modeldb.versioning.DeleteCommitRequest;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlob;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.modeldb.versioning.EnvironmentVariablesBlob;
import ai.verta.modeldb.versioning.GetBranchRequest;
import ai.verta.modeldb.versioning.GitCodeBlob;
import ai.verta.modeldb.versioning.HyperparameterSetConfigBlob;
import ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob;
import ai.verta.modeldb.versioning.ListCommitBlobsRequest;
import ai.verta.modeldb.versioning.MergeRepositoryCommitsRequest;
import ai.verta.modeldb.versioning.PathDatasetBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import ai.verta.modeldb.versioning.PythonEnvironmentBlob;
import ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.VersionEnvironmentBlob;
import ai.verta.modeldb.versioning.VersioningServiceGrpc;
import ai.verta.modeldb.versioning.VersioningServiceGrpc.VersioningServiceBlockingStub;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
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
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests diffs after commit creation with diff or blob description and checks resulting diff. Tests
 * 2 modified cases: same type and different type.
 */
@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MergeTest {

  private static final Logger LOGGER = LogManager.getLogger(MergeTest.class);
  private static final String FIRST_NAME = "train.json";
  private static final String OTHER_NAME = "environment.json";
  private static final boolean USE_SAME_NAMES = false; // TODO: set to true after fixing VR-3688
  private static final String SECOND_NAME = USE_SAME_NAMES ? FIRST_NAME : OTHER_NAME;
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
    deleteEntitiesCron = new DeleteEntitiesCron(authService, roleService, 100);
  }

  private final int blobType;

  // 1. blob type: 0 -- dataset path, 1 -- config, 2 -- python environment, 3 --
  // Git Notebook Code
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{0}, {1}, {2}, {3}});
  }

  public MergeTest(int blobType) {
    this.blobType = blobType;
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

  @Test
  public void mergeRepositoryCommitTest() throws InvalidProtocolBufferException {
    LOGGER.info("Merge Repository Commit test start................................");

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

    BlobExpanded[] blobExpandedArray;
    blobExpandedArray = createBlobs(blobType);

    CreateCommitRequest.Builder createCommitRequestBuilder =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setMessage("this is the first non init commit")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(getBranchResponse.getCommit().getCommitSha())
                    .build());
    CreateCommitRequest createCommitRequest;
    LinkedList<BlobExpanded> blobsA = new LinkedList<>();
    blobsA.add(blobExpandedArray[0]);
    createCommitRequest = createCommitRequestBuilder.addAllBlobs(blobsA).build();

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commitA = commitResponse.getCommit();

    createCommitRequestBuilder =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setMessage("this commit can be merged with previous")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(getBranchResponse.getCommit().getCommitSha())
                    .build());

    LinkedList<BlobExpanded> blobsB = new LinkedList<>();
    blobsB.add(blobExpandedArray[1]);
    createCommitRequest = createCommitRequestBuilder.addAllBlobs(blobsB).build();

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commitB = commitResponse.getCommit();

    MergeRepositoryCommitsRequest repositoryMergeRequest =
        MergeRepositoryCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitShaA(commitA.getCommitSha())
            .setCommitShaB(commitB.getCommitSha())
            .build();
    MergeRepositoryCommitsRequest.Response mergeReponse1 =
        versioningServiceBlockingStub.mergeRepositoryCommits(repositoryMergeRequest);

    Assert.assertNotNull(mergeReponse1.getCommit());

    ListCommitBlobsRequest.Response commitBlobs =
        versioningServiceBlockingStub.listCommitBlobs(
            ListCommitBlobsRequest.newBuilder()
                .setCommitSha(mergeReponse1.getCommit().getCommitSha())
                .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
                .build());

    // FIXME:  blobs randomize order of
    // Assert.assertTrue(commitBlobs.getBlobsList().containsAll(blobsA));
    // Assert.assertTrue(commitBlobs.getBlobsList().containsAll(blobsB));
    Assert.assertTrue(commitBlobs.getBlobsList().size() == 2);

    //    List<BlobExpanded> blobList = new LinkedList<BlobExpanded>(commitBlobs.getBlobsList());
    //    blobList.removeAll(blobsA);
    //    blobList.removeAll(blobsB);
    //    Assert.assertTrue(blobList.isEmpty());

    createCommitRequestBuilder =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setMessage("this commit should conflict with previous")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(getBranchResponse.getCommit().getCommitSha())
                    .build());

    LinkedList<BlobExpanded> blobsC = new LinkedList<>();
    blobsC.add(blobExpandedArray[2]);
    createCommitRequest = createCommitRequestBuilder.addAllBlobs(blobsC).build();

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commitC = commitResponse.getCommit();

    repositoryMergeRequest =
        MergeRepositoryCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitShaA(commitA.getCommitSha())
            .setCommitShaB(commitC.getCommitSha())
            .build();
    MergeRepositoryCommitsRequest.Response mergeReponse2 =
        versioningServiceBlockingStub.mergeRepositoryCommits(repositoryMergeRequest);
    Assert.assertTrue(mergeReponse2.getCommit().getCommitSha() == "");
    Assert.assertTrue(!mergeReponse2.getConflictsList().isEmpty());

    // Now we apply commit D and commit E on top of commit A , these two commits both modify blobs
    // in commit A in different ways and should lead to conflict

    createCommitRequestBuilder =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setMessage("this commit modifies blob in first non init commit")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(commitA.getCommitSha())
                    .build());

    LinkedList<BlobExpanded> blobsD = new LinkedList<>();
    blobsD.add(blobExpandedArray[2]);
    createCommitRequest = createCommitRequestBuilder.addAllBlobs(blobsD).build();
    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commitD = commitResponse.getCommit();

    createCommitRequestBuilder =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommit(
                Commit.newBuilder()
                    .setMessage("this commit also modifies blob in first non init commit")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(commitA.getCommitSha())
                    .build());

    LinkedList<BlobExpanded> blobsE = new LinkedList<>();
    blobsE.add(blobExpandedArray[3]);
    createCommitRequest = createCommitRequestBuilder.addAllBlobs(blobsE).build();
    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commitE = commitResponse.getCommit();
    repositoryMergeRequest =
        MergeRepositoryCommitsRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setCommitShaA(commitD.getCommitSha())
            .setCommitShaB(commitE.getCommitSha())
            .build();
    MergeRepositoryCommitsRequest.Response mergeReponse3 =
        versioningServiceBlockingStub.mergeRepositoryCommits(repositoryMergeRequest);
    Assert.assertTrue(mergeReponse3.getCommit().getCommitSha() == "");
    Assert.assertTrue(!mergeReponse3.getConflictsList().isEmpty());

    for (Commit commit :
        new Commit[] {commitE, commitD, commitC, mergeReponse1.getCommit(), commitB, commitA}) {
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

  private static List<String> LOCATION1 =
      Arrays.asList("modeldb", "march", "environment", FIRST_NAME);
  private static List<String> LOCATION2 = Arrays.asList("modeldb", "environment", SECOND_NAME);
  private static List<String> LOCATION3 = Arrays.asList("modeldb", "blob", "march", "blob.json");
  private static List<String> LOCATION4 = Collections.singletonList("modeldb.json");
  private static List<String> LOCATION5 = Collections.singletonList("maths/algebra");

  private BlobExpanded modifiedBlobExpanded(int blobType) {
    String path3 = "/protos/proto/public/test22.txt";
    BlobExpanded blobExpanded3 =
        BlobExpanded.newBuilder()
            .setBlob(getDatasetBlobFromPath(path3, 5))
            .addAllLocation(LOCATION3)
            .build();

    return blobExpanded3;
  }

  static Blob getDatasetBlobFromPath(String path, long size) {
    return Blob.newBuilder()
        .setDataset(
            DatasetBlob.newBuilder()
                .setPath(
                    PathDatasetBlob.newBuilder()
                        .addComponents(
                            PathDatasetComponentBlob.newBuilder()
                                .setPath(path)
                                .setSize(size)
                                .setLastModifiedAtSource(time)
                                .build())
                        .build())
                .build())
        .build();
  }

  static Blob getDatasetBlobFromPathMultiple(String path, long size) {
    return Blob.newBuilder()
        .setDataset(
            DatasetBlob.newBuilder()
                .setPath(
                    PathDatasetBlob.newBuilder()
                        .addComponents(
                            PathDatasetComponentBlob.newBuilder()
                                .setPath(path)
                                .setSize(size)
                                .setLastModifiedAtSource(time)
                                .build())
                        .addComponents(
                            PathDatasetComponentBlob.newBuilder()
                                .setPath(path + "~")
                                .setSize(size)
                                .setLastModifiedAtSource(time)
                                .build())
                        .build())
                .build())
        .build();
  }
  /**
   * blob 1 is original blob 2 is completely unrelated blob used for merge to go through blob 3 is
   * meant to modify blob 1 blob 4 is meant to modify blob 1
   *
   * @return
   */
  private BlobExpanded[] createBlobs(int blobType) {
    final BlobExpanded blobExpanded1, blobExpanded2, blobExpanded3, blobExpanded4, blobExpanded5;
    switch (blobType) {
      case 0:
        String path1 = "/protos/proto/public/versioning/versioning.proto";
        blobExpanded1 =
            BlobExpanded.newBuilder()
                .setBlob(getDatasetBlobFromPath(path1, 2))
                .addAllLocation(LOCATION1)
                .build();

        blobExpanded2 =
            BlobExpanded.newBuilder()
                .setBlob(getDatasetBlobFromPath(path1, 3))
                .addAllLocation(LOCATION2)
                .build();

        blobExpanded3 =
            BlobExpanded.newBuilder()
                .setBlob(getDatasetBlobFromPathMultiple(path1, 3))
                .addAllLocation(LOCATION1)
                .build();

        blobExpanded4 =
            BlobExpanded.newBuilder()
                .setBlob(getDatasetBlobFromPath(path1, 4))
                .addAllLocation(LOCATION1)
                .build();

        String path5 = "/protos/proto/public/algebra.txt";
        blobExpanded5 =
            BlobExpanded.newBuilder()
                .setBlob(getDatasetBlobFromPath(path5, 5))
                .addAllLocation(LOCATION5)
                .build();
        break;
      case 1:
        blobExpanded1 =
            BlobExpanded.newBuilder()
                .setBlob(
                    Blob.newBuilder()
                        .setConfig(
                            ConfigBlob.newBuilder()
                                .addHyperparameterSet(
                                    HyperparameterSetConfigBlob.newBuilder()
                                        .setName("C")
                                        .setDiscrete(
                                            DiscreteHyperparameterSetConfigBlob.newBuilder()
                                                .addValues(
                                                    HyperparameterValuesConfigBlob.newBuilder()
                                                        .setFloatValue(1.3f))))))
                .addAllLocation(LOCATION1)
                .build();
        blobExpanded2 =
            BlobExpanded.newBuilder()
                .setBlob(
                    Blob.newBuilder()
                        .setConfig(
                            ConfigBlob.newBuilder()
                                .addHyperparameterSet(
                                    HyperparameterSetConfigBlob.newBuilder()
                                        .setName("D")
                                        .setDiscrete(
                                            DiscreteHyperparameterSetConfigBlob.newBuilder()
                                                .addValues(
                                                    HyperparameterValuesConfigBlob.newBuilder()
                                                        .setFloatValue(1.3f))))))
                .addAllLocation(LOCATION2)
                .build();
        blobExpanded3 =
            BlobExpanded.newBuilder()
                .setBlob(
                    Blob.newBuilder()
                        .setConfig(
                            ConfigBlob.newBuilder()
                                .addHyperparameterSet(
                                    HyperparameterSetConfigBlob.newBuilder()
                                        .setName("C")
                                        .setDiscrete(
                                            DiscreteHyperparameterSetConfigBlob.newBuilder()
                                                .addValues(
                                                    HyperparameterValuesConfigBlob.newBuilder()
                                                        .setFloatValue(1.35f))))))
                .addAllLocation(LOCATION1)
                .build();
        blobExpanded4 =
            BlobExpanded.newBuilder()
                .setBlob(
                    Blob.newBuilder()
                        .setConfig(
                            ConfigBlob.newBuilder()
                                .addHyperparameterSet(
                                    HyperparameterSetConfigBlob.newBuilder()
                                        .setName("C")
                                        .setDiscrete(
                                            DiscreteHyperparameterSetConfigBlob.newBuilder()
                                                .addValues(
                                                    HyperparameterValuesConfigBlob.newBuilder()
                                                        .setFloatValue(1.36f))))))
                .addAllLocation(LOCATION1)
                .build();
        break;
      case 2:
        PythonEnvironmentBlob.Builder pythonBuilder =
            PythonEnvironmentBlob.newBuilder()
                .addRequirements(
                    PythonRequirementEnvironmentBlob.newBuilder()
                        .setLibrary("flask")
                        .setVersion(
                            VersionEnvironmentBlob.newBuilder()
                                .setMajor(1)
                                .setMinor(1)
                                .setPatch(1)))
                .addRequirements(
                    PythonRequirementEnvironmentBlob.newBuilder()
                        .setLibrary("numpy")
                        .setConstraint(">=")
                        .setVersion(
                            VersionEnvironmentBlob.newBuilder()
                                .setMajor(1)
                                .setMinor(18)
                                .setPatch(1)))
                .setVersion(
                    VersionEnvironmentBlob.newBuilder().setMajor(2).setMinor(7).setPatch(3));

        PythonEnvironmentBlob.Builder pythonBuilder2 =
            PythonEnvironmentBlob.newBuilder()
                .addRequirements(
                    PythonRequirementEnvironmentBlob.newBuilder()
                        .setLibrary("flask")
                        .setVersion(
                            VersionEnvironmentBlob.newBuilder()
                                .setMajor(1)
                                .setMinor(1)
                                .setPatch(1)))
                .addRequirements(
                    PythonRequirementEnvironmentBlob.newBuilder()
                        .setLibrary("numpy")
                        .setConstraint(">=")
                        .setVersion(
                            VersionEnvironmentBlob.newBuilder()
                                .setMajor(1)
                                .setMinor(19)
                                .setPatch(1)))
                .setVersion(
                    VersionEnvironmentBlob.newBuilder().setMajor(2).setMinor(7).setPatch(3));
        PythonEnvironmentBlob.Builder pythonBuilder3 =
            PythonEnvironmentBlob.newBuilder()
                .addRequirements(
                    PythonRequirementEnvironmentBlob.newBuilder()
                        .setLibrary("flask")
                        .setVersion(
                            VersionEnvironmentBlob.newBuilder()
                                .setMajor(1)
                                .setMinor(1)
                                .setPatch(1)))
                .addRequirements(
                    PythonRequirementEnvironmentBlob.newBuilder()
                        .setLibrary("numpy")
                        .setConstraint(">=")
                        .setVersion(
                            VersionEnvironmentBlob.newBuilder()
                                .setMajor(1)
                                .setMinor(17)
                                .setPatch(1)))
                .setVersion(
                    VersionEnvironmentBlob.newBuilder().setMajor(2).setMinor(7).setPatch(3));

        EnvironmentBlob.Builder builder =
            EnvironmentBlob.newBuilder()
                .addAllCommandLine(Arrays.asList("ECHO 123", "ls ..", "make all"))
                .addEnvironmentVariables(
                    EnvironmentVariablesBlob.newBuilder()
                        .setValue("/tmp/diff")
                        .setName("DIFF_LOCATION"));
        blobExpanded1 =
            BlobExpanded.newBuilder()
                .setBlob(Blob.newBuilder().setEnvironment(builder.setPython(pythonBuilder)))
                .addAllLocation(LOCATION1)
                .build();

        pythonBuilder.addConstraints(
            PythonRequirementEnvironmentBlob.newBuilder()
                .setLibrary("boto")
                .setConstraint("<=")
                .setVersion(
                    VersionEnvironmentBlob.newBuilder().setMajor(1).setMinor(1).setPatch(11)));
        Blob.Builder builderForBlob =
            Blob.newBuilder().setEnvironment(builder.setPython(pythonBuilder));
        blobExpanded2 =
            BlobExpanded.newBuilder()
                .setBlob(Blob.newBuilder().setEnvironment(builder.setPython(pythonBuilder)))
                .addAllLocation(LOCATION2)
                .build();

        blobExpanded3 =
            BlobExpanded.newBuilder()
                .setBlob(Blob.newBuilder().setEnvironment(builder.setPython(pythonBuilder2)))
                .addAllLocation(LOCATION1)
                .build();

        blobExpanded4 =
            BlobExpanded.newBuilder()
                .setBlob(Blob.newBuilder().setEnvironment(builder.setPython(pythonBuilder3)))
                .addAllLocation(LOCATION1)
                .build();
        break;
      default:
        blobExpanded1 =
            BlobExpanded.newBuilder()
                .setBlob(
                    Blob.newBuilder()
                        .setCode(
                            CodeBlob.newBuilder()
                                .setGit(
                                    GitCodeBlob.newBuilder()
                                        .setRepo("https://github.com/VertaAI/modeldb")
                                        .setBranch("master"))))
                .addAllLocation(LOCATION1)
                .build();
        blobExpanded2 =
            BlobExpanded.newBuilder()
                .setBlob(
                    Blob.newBuilder()
                        .setCode(
                            CodeBlob.newBuilder()
                                .setGit(
                                    GitCodeBlob.newBuilder()
                                        .setRepo("https://github.com/VertaAI/modeldb")
                                        .setBranch("feature"))))
                .addAllLocation(LOCATION2)
                .build();
        blobExpanded3 =
            BlobExpanded.newBuilder()
                .setBlob(
                    Blob.newBuilder()
                        .setCode(
                            CodeBlob.newBuilder()
                                .setGit(
                                    GitCodeBlob.newBuilder()
                                        .setRepo("https://github.com/VertaAI/modeldb")
                                        .setBranch("feature"))))
                .addAllLocation(LOCATION1)
                .build();
        blobExpanded4 =
            BlobExpanded.newBuilder()
                .setBlob(
                    Blob.newBuilder()
                        .setCode(
                            CodeBlob.newBuilder()
                                .setGit(
                                    GitCodeBlob.newBuilder()
                                        .setRepo("https://github.com/VertaAI/modeldb")
                                        .setBranch("anotherFeature"))))
                .addAllLocation(LOCATION1)
                .build();
        break;
    }
    return new BlobExpanded[] {blobExpanded1, blobExpanded2, blobExpanded3, blobExpanded4};
  }
}
