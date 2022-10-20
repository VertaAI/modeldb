package ai.verta.modeldb;

import static ai.verta.modeldb.CommitTest.getDatasetBlobFromPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.authservice.*;
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
import ai.verta.modeldb.versioning.ListBranchesRequest;
import ai.verta.modeldb.versioning.ListCommitsLogRequest;
import ai.verta.modeldb.versioning.ListTagsRequest;
import ai.verta.modeldb.versioning.PathDatasetBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.SetBranchRequest;
import ai.verta.modeldb.versioning.SetRepository;
import ai.verta.modeldb.versioning.SetTagRequest;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.UserInfo;
import com.google.common.util.concurrent.Futures;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class, webEnvironment = DEFINED_PORT)
@ContextConfiguration(classes = {ModeldbTestConfigurationBeans.class})
public class BranchTest extends ModeldbTestSetup {

  private static final Logger LOGGER = LogManager.getLogger(BranchTest.class);
  private static Repository repository;
  private static Commit parentCommit;

  @Before
  public void createEntities() {
    initializedChannelBuilderAndExternalServiceStubs();

    if (isRunningIsolated()) {
      setupMockUacEndpoints(uac);
    }

    // Create all entities
    createRepositoryEntities();
  }

  @After
  public void removeEntities() {
    if (repository != null) {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
              .build();
      DeleteRepositoryRequest.Response response =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      assertTrue("Repository not delete", response.getStatus());
      repository = null;
      parentCommit = null;
    }
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
    repository = createRepository(repoName);
    LOGGER.info("Repository created successfully");
    assertEquals(
        "Repository name not match with expected Repository name", repoName, repository.getName());

    if (isRunningIsolated()) {
      mockGetResourcesForAllRepositories(Map.of(repository.getId(), repository), testUser1);
    }

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

  protected void mockGetResourcesForAllRepositories(
      Map<Long, Repository> repositoryMap, UserInfo userInfo) {
    var repoIdNameMap =
        repositoryMap.entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> String.valueOf(entry.getKey()), entry -> entry.getValue().getName()));
    mockGetResources(repoIdNameMap, userInfo);
    when(collaboratorMock.getResourcesSpecialPersonalWorkspace(any()))
        .thenReturn(
            Futures.immediateFuture(
                GetResources.Response.newBuilder()
                    .addAllItem(
                        repositoryMap.values().stream()
                            .map(
                                repository ->
                                    GetResourcesResponseItem.newBuilder()
                                        .setVisibility(ResourceVisibility.PRIVATE)
                                        .setResourceId(String.valueOf(repository.getId()))
                                        .setResourceName(repository.getName())
                                        .setResourceType(
                                            ResourceType.newBuilder()
                                                .setModeldbServiceResourceType(
                                                    ModelDBServiceResourceTypes.REPOSITORY)
                                                .build())
                                        .setOwnerId(repository.getWorkspaceServiceId())
                                        .setWorkspaceId(repository.getWorkspaceServiceId())
                                        .build())
                            .collect(Collectors.toList()))
                    .build()));
    mockGetSelfAllowedResources(
        repoIdNameMap.keySet(), ModelDBServiceResourceTypes.REPOSITORY, ModelDBServiceActions.READ);
  }

  public static Repository createRepository(String repoName) {
    SetRepository setRepository = RepositoryTest.getSetRepositoryRequest(repoName);
    SetRepository.Response result = versioningServiceBlockingStub.createRepository(setRepository);
    return result.getRepository();
  }

  @Test
  public void addTagTest() {
    LOGGER.info("Add tags test start................................");
    CreateCommitRequest createCommitRequest =
        CreateCommitRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .setCommit(
                Commit.newBuilder()
                    .setMessage("this is the test commit message")
                    .setDateCreated(Calendar.getInstance().getTimeInMillis())
                    .addParentShas(parentCommit.getCommitSha())
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

    String tagName = "backend-commit-tag-1-" + new Date().getTime();
    SetTagRequest setTagRequest =
        SetTagRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setTag(tagName)
            .build();

    versioningServiceBlockingStub.setTag(setTagRequest);

    GetTagRequest getTagRequest =
        GetTagRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .setTag(tagName)
            .build();
    GetTagRequest.Response getTagResponse = versioningServiceBlockingStub.getTag(getTagRequest);

    assertEquals(
        "Expected tag not found in response",
        commitResponse.getCommit(),
        getTagResponse.getCommit());

    ListTagsRequest listTagsRequest =
        ListTagsRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .build();
    ListTagsRequest.Response listTagsResponse =
        versioningServiceBlockingStub.listTags(listTagsRequest);
    assertEquals(
        "Tag count not match with expected tag count", 1, listTagsResponse.getTotalRecords());
    assertTrue(
        "Expected tag not found in the response", listTagsResponse.getTagsList().contains(tagName));

    setTagRequest =
        SetTagRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
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
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .setTag(tagName)
            .build();
    versioningServiceBlockingStub.deleteTag(deleteTagRequest);

    deleteTagRequest =
        DeleteTagRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
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
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .build();
    listTagsResponse = versioningServiceBlockingStub.listTags(listTagsRequest);
    assertEquals(
        "Tag count not match with expected tag count", 0, listTagsResponse.getTotalRecords());

    LOGGER.info("Add tag test end................................");
  }

  @Test
  public void createGetListDeleteBranchTest() {
    LOGGER.info("branch test start................................");
    List<Commit> commitShaList = new LinkedList<>();
    String branchName2 = "branch-commits-label-2-" + new Date().getTime();
    Commit commit3 = null;
    try {
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

      CreateCommitRequest createCommitRequest =
          CreateCommitRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setCommit(
                  Commit.newBuilder()
                      .setMessage("this is the test commit message")
                      .setDateCreated(Calendar.getInstance().getTimeInMillis())
                      .addParentShas(parentCommit.getCommitSha())
                      .build())
              .addBlobs(blobExpanded1)
              .build();

      CreateCommitRequest.Response commitResponse =
          versioningServiceBlockingStub.createCommit(createCommitRequest);
      Commit commit1 = commitResponse.getCommit();

      createCommitRequest =
          CreateCommitRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setCommit(
                  Commit.newBuilder()
                      .setMessage("this is the test commit message")
                      .setDateCreated(Calendar.getInstance().getTimeInMillis())
                      .addParentShas(commitResponse.getCommit().getCommitSha())
                      .build())
              .addBlobs(blobExpanded2)
              .build();

      commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
      Commit commit2 = commitResponse.getCommit();

      createCommitRequest =
          CreateCommitRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setCommit(
                  Commit.newBuilder()
                      .setMessage("this is the test commit message")
                      .setDateCreated(Calendar.getInstance().getTimeInMillis())
                      .addParentShas(commitResponse.getCommit().getCommitSha())
                      .build())
              .addBlobs(blobExpanded3)
              .build();

      commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
      commit3 = commitResponse.getCommit();

      createCommitRequest =
          CreateCommitRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setCommit(
                  Commit.newBuilder()
                      .setMessage("this is the test commit message")
                      .setDateCreated(Calendar.getInstance().getTimeInMillis())
                      .addParentShas(commitResponse.getCommit().getCommitSha())
                      .build())
              .addBlobs(blobExpanded4)
              .build();

      commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
      Commit commit4 = commitResponse.getCommit();

      commitShaList.add(commit4);
      commitShaList.add(commit3);
      commitShaList.add(commit2);
      commitShaList.add(commit1);

      String branchName1 = "branch-commits-label-1-" + new Date().getTime();
      SetBranchRequest setBranchRequest =
          SetBranchRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setBranch(branchName1)
              .setCommitSha(commit4.getCommitSha())
              .build();
      versioningServiceBlockingStub.setBranch(setBranchRequest);

      setBranchRequest =
          SetBranchRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setBranch(branchName2)
              .setCommitSha(commit3.getCommitSha())
              .build();
      versioningServiceBlockingStub.setBranch(setBranchRequest);

      GetBranchRequest getBranchRequest =
          GetBranchRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setBranch(branchName1)
              .build();
      GetBranchRequest.Response getBranchResponse =
          versioningServiceBlockingStub.getBranch(getBranchRequest);
      Commit branchRootCommit = getBranchResponse.getCommit();
      Assert.assertEquals(
          "Expected commit not found in the response",
          commit4.getCommitSha(),
          branchRootCommit.getCommitSha());

      ListBranchesRequest listBranchesRequest =
          ListBranchesRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
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
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setBranch(branchName1)
              .build();
      versioningServiceBlockingStub.deleteBranch(deleteBranchRequest);

      getBranchRequest =
          GetBranchRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setBranch(branchName1)
              .build();
      try {
        versioningServiceBlockingStub.getBranch(getBranchRequest);
        Assert.fail();
      } catch (StatusRuntimeException e) {
        Assert.assertEquals(Code.NOT_FOUND, e.getStatus().getCode());
        e.printStackTrace();
      }

    } finally {
      Commit commit3Final = commit3;
      commitShaList.forEach(
          commit -> {
            DeleteCommitRequest deleteCommitRequest =
                DeleteCommitRequest.newBuilder()
                    .setRepositoryId(
                        RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
                    .setCommitSha(commit.getCommitSha())
                    .build();
            if (commit3Final != null && commit.getCommitSha().equals(commit3Final.getCommitSha())) {
              try {
                versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
              } catch (StatusRuntimeException e) {
                Assert.assertEquals(Code.FAILED_PRECONDITION, e.getStatus().getCode());
                e.printStackTrace();

                DeleteBranchRequest deleteBranchRequest1 =
                    DeleteBranchRequest.newBuilder()
                        .setRepositoryId(
                            RepositoryIdentification.newBuilder()
                                .setRepoId(repository.getId())
                                .build())
                        .setBranch(branchName2)
                        .build();
                versioningServiceBlockingStub.deleteBranch(deleteBranchRequest1);
                versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
              }
            } else {
              versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
            }
          });
    }
    LOGGER.info("Branch test end................................");
  }

  /**
   * Prevent deletion of commit if referred to by branch or tag. if commit associated with branch or
   * tag ModelDb throws the error with status code FAILED_PRECONDITION
   */
  @Test
  public void branchTest() {
    LOGGER.info("branch test start................................");
    List<String> commitShaList = new ArrayList<>();
    try {
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

      CreateCommitRequest createCommitRequest =
          CreateCommitRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setCommit(
                  Commit.newBuilder()
                      .setMessage("this is the test commit message")
                      .setDateCreated(Calendar.getInstance().getTimeInMillis())
                      .addParentShas(parentCommit.getCommitSha())
                      .build())
              .addBlobs(blobExpanded1)
              .build();

      CreateCommitRequest.Response commitResponse =
          versioningServiceBlockingStub.createCommit(createCommitRequest);
      Commit commit1 = commitResponse.getCommit();

      createCommitRequest =
          CreateCommitRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setCommit(
                  Commit.newBuilder()
                      .setMessage("this is the test commit message")
                      .setDateCreated(Calendar.getInstance().getTimeInMillis())
                      .addParentShas(commit1.getCommitSha())
                      .build())
              .addBlobs(blobExpanded2)
              .build();

      commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
      Commit commit2 = commitResponse.getCommit();

      createCommitRequest =
          CreateCommitRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setCommit(
                  Commit.newBuilder()
                      .setMessage("this is the test commit message")
                      .setDateCreated(Calendar.getInstance().getTimeInMillis())
                      .addParentShas(commit2.getCommitSha())
                      .build())
              .addBlobs(blobExpanded3)
              .build();

      commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
      Commit commit3 = commitResponse.getCommit();

      createCommitRequest =
          CreateCommitRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setCommit(
                  Commit.newBuilder()
                      .setMessage("this is the test commit message")
                      .setDateCreated(Calendar.getInstance().getTimeInMillis())
                      .addParentShas(commit3.getCommitSha())
                      .build())
              .addBlobs(blobExpanded4)
              .build();

      commitResponse = versioningServiceBlockingStub.createCommit(createCommitRequest);
      Commit commit4 = commitResponse.getCommit();

      commitShaList.add(commit4.getCommitSha());
      commitShaList.add(commit3.getCommitSha());
      commitShaList.add(commit2.getCommitSha());
      commitShaList.add(commit1.getCommitSha());

      String branchName = "get-list-branch-commits-" + new Date().getTime();
      SetBranchRequest setBranchRequest =
          SetBranchRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setBranch(branchName)
              .setCommitSha(commitShaList.get(3))
              .build();
      versioningServiceBlockingStub.setBranch(setBranchRequest);

      GetBranchRequest getBranchRequest =
          GetBranchRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setBranch(branchName)
              .build();
      GetBranchRequest.Response getBranchResponse =
          versioningServiceBlockingStub.getBranch(getBranchRequest);
      Commit branchRootCommit = getBranchResponse.getCommit();
      Assert.assertEquals(
          "Expected commit not found in the response",
          commitShaList.get(3),
          branchRootCommit.getCommitSha());

      ListCommitsLogRequest listCommitsLogRequest =
          ListCommitsLogRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
              .setBranch(branchName)
              .build();
      ListCommitsLogRequest.Response listBranchCommitsResponse =
          versioningServiceBlockingStub.listCommitsLog(listCommitsLogRequest);
      Assert.assertEquals(
          "Commit count not match with expected commit count",
          2,
          listBranchCommitsResponse.getCommitsCount());
    } finally {
      commitShaList.forEach(
          commitSha -> {
            DeleteCommitRequest deleteCommitRequest =
                DeleteCommitRequest.newBuilder()
                    .setRepositoryId(
                        RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
                    .setCommitSha(commitSha)
                    .build();
            if (commitShaList.get(3).equals(commitSha)) {
              try {
                versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
              } catch (StatusRuntimeException e) {
                Assert.assertEquals(Code.FAILED_PRECONDITION, e.getStatus().getCode());
                e.printStackTrace();
              }
            } else {
              versioningServiceBlockingStub.deleteCommit(deleteCommitRequest);
            }
          });
    }
    LOGGER.info("Branch test end................................");
  }
}
