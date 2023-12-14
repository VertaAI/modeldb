package ai.verta.modeldb;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import ai.verta.modeldb.authservice.*;
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
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.SetRepository;
import ai.verta.modeldb.versioning.VersionEnvironmentBlob;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tests diffs after commit creation with diff or blob description and checks resulting diff. Tests
 * 2 modified cases: same type and different type.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = App.class, webEnvironment = DEFINED_PORT)
@ContextConfiguration(classes = {ModeldbTestConfigurationBeans.class})
class MergeTest extends ModeldbTestSetup {

  private static final Logger LOGGER = LogManager.getLogger(MergeTest.class);
  private final String FIRST_NAME = "train.json";
  private final String OTHER_NAME = "environment.json";
  private final boolean USE_SAME_NAMES = false; // TODO: set to true after fixing VR-3688
  private final String SECOND_NAME = USE_SAME_NAMES ? FIRST_NAME : OTHER_NAME;

  private Repository repository;
  private Commit parentCommit;

  private static final long time = Calendar.getInstance().getTimeInMillis();

  // 1. blob type: 0 -- dataset path, 1 -- config, 2 -- python environment, 3 --
  // Git Notebook Code
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{0}, {1}, {2}, {3}});
  }

  @BeforeEach
  @Override
  public void setUp() {
    super.setUp();
    initializeChannelBuilderAndExternalServiceStubs();

    if (isRunningIsolated()) {
      setupMockUacEndpoints(uac);
    }

    // Create all entities
    createRepositoryEntities();
  }

  @AfterEach
  @Override
  public void tearDown() {
    for (Repository repo : new Repository[] {repository}) {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repo.getId()))
              .build();
      DeleteRepositoryRequest.Response response =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      Assert.assertTrue("Repository not delete", response.getStatus());
    }

    repository = null;
    cleanUpResources();
    super.tearDown();
  }

  private void createRepositoryEntities() {
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
    }

    String repoName = "Repo-" + new Date().getTime();
    SetRepository setRepository = RepositoryTest.getSetRepositoryRequest(repoName);
    repository = versioningServiceBlockingStub.createRepository(setRepository).getRepository();
    LOGGER.info("Repository created successfully");
    Assert.assertEquals(
        "Repository name not match with expected Repository name", repoName, repository.getName());

    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);
    parentCommit = getBranchResponse.getCommit();
  }

  @ParameterizedTest
  @MethodSource(value = "data")
  public void mergeRepositoryCommitTest(int blobType) {
    LOGGER.info("Merge Repository Commit test start................................");

    BlobExpanded[] blobExpandedArray;
    blobExpandedArray = createBlobs(blobType);

    CreateCommitRequest.Builder createCommitRequestBuilder =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .setCommit(
                Commit.newBuilder()
                    .setMessage("this is the first non init commit")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(parentCommit.getCommitSha())
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
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .setCommit(
                Commit.newBuilder()
                    .setMessage("this commit can be merged with previous")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(parentCommit.getCommitSha())
                    .build());

    LinkedList<BlobExpanded> blobsB = new LinkedList<>();
    blobsB.add(blobExpandedArray[1]);
    createCommitRequest = createCommitRequestBuilder.addAllBlobs(blobsB).build();

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commitB = commitResponse.getCommit();

    MergeRepositoryCommitsRequest repositoryMergeRequest =
        MergeRepositoryCommitsRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
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
                .setRepositoryId(
                    RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
                .build());

    // FIXME:  blobs randomize order of
    // Assert.assertTrue(commitBlobs.getBlobsList().containsAll(blobsA));
    // Assert.assertTrue(commitBlobs.getBlobsList().containsAll(blobsB));
    Assert.assertEquals(2, commitBlobs.getBlobsList().size());

    //    List<BlobExpanded> blobList = new LinkedList<BlobExpanded>(commitBlobs.getBlobsList());
    //    blobList.removeAll(blobsA);
    //    blobList.removeAll(blobsB);
    //    Assert.assertTrue(blobList.isEmpty());

    createCommitRequestBuilder =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .setCommit(
                Commit.newBuilder()
                    .setMessage("this commit should conflict with previous")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(parentCommit.getCommitSha())
                    .build());

    LinkedList<BlobExpanded> blobsC = new LinkedList<>();
    blobsC.add(blobExpandedArray[2]);
    createCommitRequest = createCommitRequestBuilder.addAllBlobs(blobsC).build();

    commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
    Commit commitC = commitResponse.getCommit();

    repositoryMergeRequest =
        MergeRepositoryCommitsRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .setCommitShaA(commitA.getCommitSha())
            .setCommitShaB(commitC.getCommitSha())
            .build();
    MergeRepositoryCommitsRequest.Response mergeReponse2 =
        versioningServiceBlockingStub.mergeRepositoryCommits(repositoryMergeRequest);
    Assert.assertSame("", mergeReponse2.getCommit().getCommitSha());
    Assert.assertFalse(mergeReponse2.getConflictsList().isEmpty());

    // Now we apply commit D and commit E on top of commit A , these two commits both modify blobs
    // in commit A in different ways and should lead to conflict

    createCommitRequestBuilder =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
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
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
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
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .setCommitShaA(commitD.getCommitSha())
            .setCommitShaB(commitE.getCommitSha())
            .build();
    MergeRepositoryCommitsRequest.Response mergeReponse3 =
        versioningServiceBlockingStub.mergeRepositoryCommits(repositoryMergeRequest);
    Assert.assertSame("", mergeReponse3.getCommit().getCommitSha());
    Assert.assertFalse(mergeReponse3.getConflictsList().isEmpty());

    for (Commit commit :
        new Commit[] {commitE, commitD, commitC, mergeReponse1.getCommit(), commitB, commitA}) {
      DeleteCommitRequest deleteCommitRequest =
          DeleteCommitRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setCommitSha(commit.getCommitSha())
              .build();
      versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
    }

    LOGGER.info("Compute repository diff test end................................");
  }

  private final List<String> LOCATION1 =
      Arrays.asList("modeldb", "march", "environment", FIRST_NAME);
  private final List<String> LOCATION2 = Arrays.asList("modeldb", "environment", SECOND_NAME);
  private final List<String> LOCATION3 = Arrays.asList("modeldb", "blob", "march", "blob.json");
  private final List<String> LOCATION4 = Collections.singletonList("modeldb.json");
  private final List<String> LOCATION5 = Collections.singletonList("maths/algebra");

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
