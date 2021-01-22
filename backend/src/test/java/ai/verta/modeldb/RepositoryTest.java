package ai.verta.modeldb;

import static org.junit.Assert.*;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.EntitiesEnum;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.OperatorEnum;
import ai.verta.common.Pagination;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.metadata.AddLabelsRequest;
import ai.verta.modeldb.metadata.DeleteLabelsRequest;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.metadata.IdentificationType;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.CreateCommitRequest;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.FindRepositories;
import ai.verta.modeldb.versioning.GetBranchRequest;
import ai.verta.modeldb.versioning.GetRepositoryRequest;
import ai.verta.modeldb.versioning.ListRepositoriesRequest;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.RepositoryNamedIdentification;
import ai.verta.modeldb.versioning.SetRepository;
import ai.verta.modeldb.versioning.SetRepository.Response;
import ai.verta.modeldb.versioning.SetTagRequest;
import ai.verta.modeldb.versioning.VersioningServiceGrpc.VersioningServiceBlockingStub;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.GetUser;
import ai.verta.uac.UserInfo;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RepositoryTest extends TestsInit {

  private static final Logger LOGGER = LogManager.getLogger(RepositoryTest.class);

  private static Repository repository;
  private static Repository repository2;
  private static Repository repository3;
  private static Map<Long, Repository> repositoryMap;

  @Before
  public void createEntities() {
    // Create all entities
    createRepositoryEntities();
  }

  @After
  public void removeEntities() {
    for (Repository repo : new Repository[] {repository, repository2, repository3}) {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repo.getId()))
              .build();
      DeleteRepositoryRequest.Response response =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      assertTrue("Repository not delete", response.getStatus());
    }

    repository = null;
    repository2 = null;
    repository3 = null;
    repositoryMap = new HashMap<>();
  }

  private static void createRepositoryEntities() {
    String repoName = "Repo-" + new Date().getTime();
    repository = createRepository(repoName);
    LOGGER.info("Repository created successfully");
    assertEquals(
        "Repository name not match with expected Repository name", repoName, repository.getName());

    String repoName2 = "Repo-" + new Date().getTime();
    repository2 = createRepository(repoName2);
    LOGGER.info("Repository2 created successfully");
    assertEquals(
        "Repository name not match with expected Repository name",
        repoName2,
        repository2.getName());

    String repoName3 = "Repo-" + new Date().getTime();
    repository3 = createRepository(repoName3);
    LOGGER.info("Repository3 created successfully");
    assertEquals(
        "Repository name not match with expected Repository name",
        repoName3,
        repository3.getName());

    repositoryMap = new HashMap<>();
    repositoryMap.put(repository.getId(), repository);
    repositoryMap.put(repository2.getId(), repository2);
    repositoryMap.put(repository3.getId(), repository3);
  }

  public static Long createRepository(
      VersioningServiceBlockingStub versioningServiceBlockingStub, String repoName) {
    SetRepository setRepository = getSetRepositoryRequest(repoName);
    Response result = versioningServiceBlockingStub.createRepository(setRepository);
    return result.getRepository().getId();
  }

  private static Repository createRepository(String repoName) {
    SetRepository setRepository = getSetRepositoryRequest(repoName);
    SetRepository.Response result = versioningServiceBlockingStub.createRepository(setRepository);
    return result.getRepository();
  }

  public static SetRepository getSetRepositoryRequest(String repoName) {
    return SetRepository.newBuilder()
        .setId(
            RepositoryIdentification.newBuilder()
                .setNamedId(RepositoryNamedIdentification.newBuilder().setName(repoName).build())
                .build())
        .setRepository(
            Repository.newBuilder()
                .setName(repoName)
                .setDescription("This is test repository description"))
        .build();
  }

  private void checkEqualsAssert(StatusRuntimeException e) {
    Status status = Status.fromThrowable(e);
    LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
    if (config.hasAuth()) {
      assertTrue(
          Status.PERMISSION_DENIED.getCode() == status.getCode()
              || Status.NOT_FOUND.getCode()
                  == status.getCode()); // because of shadow delete the response could be 403 or 404
    } else {
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }
  }

  public static List<KeyValue> getAttributeList() {
    List<KeyValue> attributeList = new ArrayList<>();
    Value intValue = Value.newBuilder().setNumberValue(1.1).build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_1" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueTypeEnum.ValueType.NUMBER)
            .build());
    Value stringValue =
        Value.newBuilder()
            .setStringValue("attributes_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_2_blob_" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueTypeEnum.ValueType.BLOB)
            .build());
    return attributeList;
  }

  @Test
  public void createDeleteRepositoryNegativeTest() {
    LOGGER.info("Create and delete repository negative test start................................");

    String repo1 = "Repo-1-" + new Date().getTime();
    String repo2 = "Repo-2-" + new Date().getTime();
    long id = createRepository(versioningServiceBlockingStub, repo1);
    try {
      try {
        SetRepository setRepository =
            SetRepository.newBuilder()
                .setId(
                    RepositoryIdentification.newBuilder()
                        .setNamedId(
                            RepositoryNamedIdentification.newBuilder()
                                .setWorkspaceName("test1verta_gmail_com")
                                .setName(repo2)
                                .build())
                        .build())
                .setRepository(Repository.newBuilder().setName(repo1))
                .build();
        versioningServiceBlockingStub.createRepository(setRepository);
        Assert.fail();
      } catch (StatusRuntimeException e) {
        if (config.hasAuth()) {
          assertEquals(Status.PERMISSION_DENIED.getCode(), e.getStatus().getCode());
        } else {
          assertEquals(Status.ALREADY_EXISTS.getCode(), e.getStatus().getCode());
        }
      }
      try {
        versioningServiceBlockingStubClient2.updateRepository(
            SetRepository.newBuilder()
                .setId(RepositoryIdentification.newBuilder().setRepoId(id))
                .setRepository(
                    Repository.newBuilder().setName("Repo-updated-name-" + new Date().getTime()))
                .build());
        if (config.hasAuth()) {
          Assert.fail();
        }
      } catch (StatusRuntimeException e) {
        assertEquals(Code.INVALID_ARGUMENT, e.getStatus().getCode());
      }
      try {
        versioningServiceBlockingStubClient2.getRepository(
            GetRepositoryRequest.newBuilder()
                .setId(RepositoryIdentification.newBuilder().setRepoId(id))
                .build());
        if (config.hasAuth()) {
          Assert.fail();
        }
      } catch (StatusRuntimeException e) {
        assertEquals(Code.PERMISSION_DENIED, e.getStatus().getCode());
      }
    } finally {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
              .build();
      try {
        versioningServiceBlockingStubClient2.deleteRepository(deleteRepository);
        if (config.hasAuth()) {
          Assert.fail();
        }
      } catch (StatusRuntimeException e) {
        checkEqualsAssert(e);
      }

      if (config.hasAuth()) {
        DeleteRepositoryRequest.Response deleteResult =
            versioningServiceBlockingStub.deleteRepository(deleteRepository);
        Assert.assertTrue(deleteResult.getStatus());
      }

      try {
        deleteRepository =
            DeleteRepositoryRequest.newBuilder()
                .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
                .build();
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
        Assert.fail();
      } catch (StatusRuntimeException e) {
        Assert.assertEquals(Code.NOT_FOUND, e.getStatus().getCode());
      }
    }

    LOGGER.info("Create and delete repository negative test end................................");
  }

  @Test
  public void updateRepositoryByNameTest() {
    LOGGER.info("Update repository by name test start................................");

    GetRepositoryRequest getRepositoryRequest =
        GetRepositoryRequest.newBuilder()
            .setId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
            .build();
    GetRepositoryRequest.Response getByNameResult =
        versioningServiceBlockingStub.getRepository(getRepositoryRequest);

    SetRepository setRepository =
        SetRepository.newBuilder()
            .setId(
                RepositoryIdentification.newBuilder()
                    .setNamedId(
                        RepositoryNamedIdentification.newBuilder()
                            .setName(repository.getName())
                            .build())
                    .build())
            .setRepository(
                getByNameResult
                    .getRepository()
                    .toBuilder()
                    .setName("Repo-" + new Date().getTime())
                    .build())
            .build();
    SetRepository.Response result = versioningServiceBlockingStub.updateRepository(setRepository);
    Assert.assertTrue(result.hasRepository());
    Assert.assertEquals(setRepository.getRepository().getName(), result.getRepository().getName());
    repository = result.getRepository();

    getRepositoryRequest =
        GetRepositoryRequest.newBuilder()
            .setId(
                RepositoryIdentification.newBuilder()
                    .setNamedId(
                        RepositoryNamedIdentification.newBuilder().setName(repository.getName())))
            .build();
    getByNameResult = versioningServiceBlockingStub.getRepository(getRepositoryRequest);
    Assert.assertEquals(
        "Repository Id not match with expected repository Id",
        repository.getId(),
        getByNameResult.getRepository().getId());
    Assert.assertEquals(
        "Repository name not match with expected repository name",
        repository.getName(),
        getByNameResult.getRepository().getName());
    if (config.hasAuth()) {
      UserInfo userInfo =
          uacServiceStub.getUser(
              GetUser.newBuilder().setEmail(authClientInterceptor.getClient1Email()).build());
      Assert.assertEquals(
          userInfo.getVertaInfo().getUserId(), getByNameResult.getRepository().getOwner());
    }

    LOGGER.info("Update repository by name test end................................");
  }

  @Test
  public void updateRepositoryDescriptionTest() {
    LOGGER.info("Update repository description test start................................");

    GetRepositoryRequest getRepositoryRequest =
        GetRepositoryRequest.newBuilder()
            .setId(
                RepositoryIdentification.newBuilder()
                    .setNamedId(
                        RepositoryNamedIdentification.newBuilder().setName(repository.getName())))
            .build();
    GetRepositoryRequest.Response getByNameResult =
        versioningServiceBlockingStub.getRepository(getRepositoryRequest);

    String description = "this is test repository description from update repository call";
    SetRepository setRepository =
        SetRepository.newBuilder()
            .setId(
                RepositoryIdentification.newBuilder()
                    .setNamedId(
                        RepositoryNamedIdentification.newBuilder()
                            .setName(repository.getName())
                            .build())
                    .build())
            .setRepository(
                getByNameResult
                    .getRepository()
                    .toBuilder()
                    .setName(repository.getName())
                    .setDescription(description)
                    .build())
            .build();
    SetRepository.Response result = versioningServiceBlockingStub.updateRepository(setRepository);
    Assert.assertTrue(result.hasRepository());
    Assert.assertEquals(description, result.getRepository().getDescription());
    repository = result.getRepository();

    getRepositoryRequest =
        GetRepositoryRequest.newBuilder()
            .setId(
                RepositoryIdentification.newBuilder()
                    .setNamedId(
                        RepositoryNamedIdentification.newBuilder().setName(repository.getName())))
            .build();
    getByNameResult = versioningServiceBlockingStub.getRepository(getRepositoryRequest);
    Assert.assertEquals(
        "Repository Id not match with expected repository Id",
        repository.getId(),
        getByNameResult.getRepository().getId());
    Assert.assertEquals(
        "Repository name not match with expected repository name",
        description,
        getByNameResult.getRepository().getDescription());

    LOGGER.info("Update repository description test end................................");
  }

  @Test
  public void getRepositoryByIdTest() {
    LOGGER.info("Get repository by Id test start................................");

    // check id
    GetRepositoryRequest getRepositoryRequest =
        GetRepositoryRequest.newBuilder()
            .setId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .build();
    GetRepositoryRequest.Response getByIdResult =
        versioningServiceBlockingStub.getRepository(getRepositoryRequest);
    Assert.assertEquals(
        "Repository Id not match with expected repository Id",
        repository.getId(),
        getByIdResult.getRepository().getId());
    Assert.assertEquals(
        "Repository name not match with expected repository name",
        repository.getName(),
        getByIdResult.getRepository().getName());

    LOGGER.info("Get repository by Id test end................................");
  }

  @Test
  public void getRepositoryByNameTest() {
    LOGGER.info("Get repository by name test start................................");
    GetRepositoryRequest getRepositoryRequest =
        GetRepositoryRequest.newBuilder()
            .setId(
                RepositoryIdentification.newBuilder()
                    .setNamedId(
                        RepositoryNamedIdentification.newBuilder().setName(repository.getName())))
            .build();
    GetRepositoryRequest.Response getByNameResult =
        versioningServiceBlockingStub.getRepository(getRepositoryRequest);
    Assert.assertEquals(
        "Repository name not match with expected repository name",
        repository.getName(),
        getByNameResult.getRepository().getName());
    if (config.hasAuth()) {
      UserInfo userInfo =
          uacServiceStub.getUser(
              GetUser.newBuilder().setEmail(authClientInterceptor.getClient1Email()).build());
      Assert.assertEquals(
          userInfo.getVertaInfo().getUserId(), getByNameResult.getRepository().getOwner());
    }

    LOGGER.info("Get repository by name test end................................");
  }

  @Test
  public void listRepositoryTest() {
    LOGGER.info("List repository test start................................");

    ListRepositoriesRequest listRepositoriesRequest = ListRepositoriesRequest.newBuilder().build();
    ListRepositoriesRequest.Response listRepositoriesResponse =
        versioningServiceBlockingStub.listRepositories(listRepositoriesRequest);

    List<Repository> expectedRepositories = new ArrayList<>();
    List<Repository> staleRepositories = new ArrayList<>();
    for (Repository repository : listRepositoriesResponse.getRepositoriesList()) {
      if (repositoryMap.containsKey(repository.getId())) {
        expectedRepositories.add(repository);
      } else {
        staleRepositories.add(repository);
      }
    }

    Assert.assertEquals(
        "Repository count not match with expected repository count",
        repositoryMap.size(),
        listRepositoriesResponse.getTotalRecords() - staleRepositories.size());
    Assert.assertEquals(
        "Repository name not match with expected repository name",
        repository3.getName(),
        expectedRepositories.get(0).getName());
    Assert.assertEquals(
        "Repository name not match with expected repository name",
        repository2.getName(),
        expectedRepositories.get(1).getName());

    listRepositoriesRequest =
        ListRepositoriesRequest.newBuilder()
            .setPagination(Pagination.newBuilder().setPageLimit(1).setPageNumber(1).build())
            .build();
    listRepositoriesResponse =
        versioningServiceBlockingStub.listRepositories(listRepositoriesRequest);
    expectedRepositories = new ArrayList<>();
    for (Repository repository : listRepositoriesResponse.getRepositoriesList()) {
      if (repositoryMap.containsKey(repository.getId())) {
        expectedRepositories.add(repository);
      }
    }
    Assert.assertEquals(
        "Repository count not match with expected repository count",
        repositoryMap.size(),
        listRepositoriesResponse.getTotalRecords() - staleRepositories.size());
    Assert.assertEquals(
        "Repository count not match with expected repository count",
        1,
        expectedRepositories.size());
    Assert.assertEquals(
        "Repository name not match with expected repository name",
        repository3.getName(),
        expectedRepositories.get(0).getName());

    LOGGER.info("List repository test end................................");
  }

  private IdentificationType createLabels(Long repoId, List<String> labels) {
    IdentificationType identificationType =
        IdentificationType.newBuilder()
            .setIdType(IDTypeEnum.IDType.VERSIONING_REPOSITORY)
            .setIntId(repoId)
            .build();

    AddLabelsRequest addLabelsRequest1 =
        AddLabelsRequest.newBuilder().setId(identificationType).addAllLabels(labels).build();
    AddLabelsRequest.Response addLabelsResponse1 =
        metadataServiceBlockingStub.addLabels(addLabelsRequest1);
    assertTrue("Labels not persist successfully", addLabelsResponse1.getStatus());
    return identificationType;
  }

  private void deleteLabels(IdentificationType id, List<String> labels) {
    DeleteLabelsRequest deleteLabelsRequest =
        DeleteLabelsRequest.newBuilder().setId(id).addAllLabels(labels).build();
    DeleteLabelsRequest.Response deleteLabelsResponse =
        metadataServiceBlockingStub.deleteLabels(deleteLabelsRequest);
    assertTrue(deleteLabelsResponse.getStatus());
  }

  @Test
  public void findRepositoryTest() {
    LOGGER.info("List repository test start................................");

    CreateDataset createDatasetRequest =
        DatasetTest.getDatasetRequest("Dataset-" + new Date().getTime());
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    Dataset dataset = createDatasetResponse.getDataset();

    List<String> labels = new ArrayList<>();
    labels.add("Backend");
    IdentificationType id1 = createLabels(repository.getId(), labels);
    labels.add("Frontend");
    IdentificationType id2 =
        createLabels(repository2.getId(), Collections.singletonList(labels.get(1)));
    IdentificationType id3 = createLabels(repository3.getId(), labels);
    try {

      FindRepositories findRepositoriesRequest = FindRepositories.newBuilder().build();
      FindRepositories.Response findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);

      List<Repository> expectedRepositories = new ArrayList<>();
      List<Repository> staleRepositories = new ArrayList<>();
      for (Repository repository : findRepositoriesResponse.getRepositoriesList()) {
        if (repositoryMap.containsKey(repository.getId())) {
          expectedRepositories.add(repository);
        } else {
          staleRepositories.add(repository);
        }
      }

      Assert.assertEquals(
          "Repository count not match with expected repository count",
          repositoryMap.size(),
          findRepositoriesResponse.getTotalRecords() - staleRepositories.size());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          repository3.getName(),
          expectedRepositories.get(0).getName());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          repository2.getName(),
          expectedRepositories.get(1).getName());
      Repository repo2 = expectedRepositories.get(0);

      findRepositoriesRequest =
          FindRepositories.newBuilder().setPageLimit(1).setPageNumber(1).build();
      findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          repositoryMap.size(),
          findRepositoriesResponse.getTotalRecords() - staleRepositories.size());
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          1,
          findRepositoriesResponse.getRepositoriesCount());

      if (staleRepositories.size() == 0) {
        Assert.assertEquals(
            "Repository name not match with expected repository name",
            repository3.getName(),
            findRepositoriesResponse.getRepositories(0).getName());
      }

      findRepositoriesRequest =
          FindRepositories.newBuilder().addRepoIds(repository.getId()).build();
      findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          1,
          findRepositoriesResponse.getTotalRecords());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          repository.getName(),
          findRepositoriesResponse.getRepositories(0).getName());

      findRepositoriesRequest =
          FindRepositories.newBuilder()
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey("name")
                      .setValue(Value.newBuilder().setStringValue(repository2.getName()).build())
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .setOperator(OperatorEnum.Operator.NOT_CONTAIN)
                      .build())
              .build();
      findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      expectedRepositories = new ArrayList<>();
      staleRepositories = new ArrayList<>();
      for (Repository repository : findRepositoriesResponse.getRepositoriesList()) {
        if (repositoryMap.containsKey(repository.getId())) {
          expectedRepositories.add(repository);
        } else {
          staleRepositories.add(repository);
        }
      }

      Assert.assertEquals(
          "Repository count not match with expected repository count",
          repositoryMap.size() - 1,
          findRepositoriesResponse.getTotalRecords() - staleRepositories.size());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          repository3.getName(),
          expectedRepositories.get(0).getName());

      findRepositoriesRequest =
          FindRepositories.newBuilder()
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey(ModelDBConstants.LABEL)
                      .setValue(Value.newBuilder().setStringValue("Backend").build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build())
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey(ModelDBConstants.LABEL)
                      .setValue(Value.newBuilder().setStringValue("Frontend").build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build())
              .build();
      findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      expectedRepositories = new ArrayList<>();
      staleRepositories = new ArrayList<>();
      for (Repository repository : findRepositoriesResponse.getRepositoriesList()) {
        if (repositoryMap.containsKey(repository.getId())) {
          expectedRepositories.add(repository);
        } else {
          staleRepositories.add(repository);
        }
      }

      Assert.assertEquals(
          "Repository count not match with expected repository count",
          1,
          findRepositoriesResponse.getTotalRecords());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          repository3.getName(),
          expectedRepositories.get(0).getName());

      findRepositoriesRequest =
          FindRepositories.newBuilder()
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey(ModelDBConstants.LABEL)
                      .setValue(Value.newBuilder().setStringValue("Backend").build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build())
              .build();
      findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      expectedRepositories = new ArrayList<>();
      staleRepositories = new ArrayList<>();
      for (Repository repository : findRepositoriesResponse.getRepositoriesList()) {
        if (repositoryMap.containsKey(repository.getId())) {
          expectedRepositories.add(repository);
        } else {
          staleRepositories.add(repository);
        }
      }
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          2,
          findRepositoriesResponse.getTotalRecords() - staleRepositories.size());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          repository3.getName(),
          expectedRepositories.get(0).getName());

      findRepositoriesRequest =
          FindRepositories.newBuilder()
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey("tags")
                      .setValue(Value.newBuilder().setStringValue("Backend").build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build())
              .build();
      try {
        versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
        fail();
      } catch (StatusRuntimeException exc) {
        Status status = Status.fromThrowable(exc);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
        assertTrue(status.getDescription().contains(": tags"));
      }

      findRepositoriesRequest =
          FindRepositories.newBuilder()
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey("tags")
                      .setValue(Value.newBuilder().setStringValue("Backend").build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build())
              .build();
      try {
        versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
        fail();
      } catch (StatusRuntimeException exc) {
        Status status = Status.fromThrowable(exc);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
        assertTrue(status.getDescription().contains(": tags"));
      }

      if (config.hasAuth()) {
        findRepositoriesRequest =
            FindRepositories.newBuilder()
                .addPredicates(
                    KeyValueQuery.newBuilder()
                        .setKey(ModelDBConstants.OWNER)
                        .setValue(Value.newBuilder().setStringValue(repo2.getOwner()).build())
                        .setOperator(OperatorEnum.Operator.EQ)
                        .setValueType(ValueTypeEnum.ValueType.STRING)
                        .build())
                .build();
        findRepositoriesResponse =
            versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
        expectedRepositories = new ArrayList<>();
        staleRepositories = new ArrayList<>();
        for (Repository repository : findRepositoriesResponse.getRepositoriesList()) {
          if (repositoryMap.containsKey(repository.getId())) {
            expectedRepositories.add(repository);
          } else {
            staleRepositories.add(repository);
          }
        }
        Assert.assertEquals(
            "Repository count not match with expected repository count",
            3,
            findRepositoriesResponse.getTotalRecords() - staleRepositories.size());
        Assert.assertEquals(
            "Repository count not match with expected repository count",
            3,
            findRepositoriesResponse.getRepositoriesCount() - staleRepositories.size());
        Assert.assertEquals(
            "Repository name not match with expected repository name",
            repository3.getName(),
            expectedRepositories.get(0).getName());
        Assert.assertEquals(
            "Repository name not match with expected repository name",
            "This is test repository description",
            expectedRepositories.get(0).getDescription());

        findRepositoriesRequest =
            FindRepositories.newBuilder()
                .addPredicates(
                    KeyValueQuery.newBuilder()
                        .setKey(ModelDBConstants.OWNER)
                        .setValue(Value.newBuilder().setStringValue(repo2.getOwner()).build())
                        .setOperator(OperatorEnum.Operator.NE)
                        .setValueType(ValueTypeEnum.ValueType.STRING)
                        .build())
                .build();
        findRepositoriesResponse =
            versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
        expectedRepositories = new ArrayList<>();
        staleRepositories = new ArrayList<>();
        for (Repository repository : findRepositoriesResponse.getRepositoriesList()) {
          if (!repositoryMap.containsKey(repository.getId())) {
            staleRepositories.add(repository);
          }
        }
        Assert.assertEquals(
            "Repository count not match with expected repository count",
            0,
            findRepositoriesResponse.getTotalRecords() - staleRepositories.size());
      }
    } finally {
      deleteLabels(id1, Collections.singletonList(labels.get(0)));
      deleteLabels(id2, Collections.singletonList(labels.get(1)));
      deleteLabels(id3, labels);

      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());
    }

    LOGGER.info("List repository test end................................");
  }

  @Test
  public void findRepositoriesByFuzzyOwnerTest() {
    LOGGER.info("FindRepositories by owner fuzzy search test start ...");
    if (!config.hasAuth()) {
      assertTrue(true);
      return;
    }

    GetUser getUserRequest =
        GetUser.newBuilder().setEmail(authClientInterceptor.getClient1Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo testUser1 = uacServiceStub.getUser(getUserRequest);
    String testUser1UserName = testUser1.getVertaInfo().getUsername();

    Value stringValue =
        Value.newBuilder().setStringValue(testUser1UserName.substring(0, 2)).build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("owner")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.CONTAIN)
            .build();
    FindRepositories findRepositoriesRequest =
        FindRepositories.newBuilder().addPredicates(keyValueQuery).build();
    FindRepositories.Response findRepositoriesResponse =
        versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
    LOGGER.info("FindProjects Response : " + findRepositoriesResponse.getRepositoriesList());
    assertEquals(
        "Project count not match with expected project count",
        3,
        findRepositoriesResponse.getRepositoriesCount());

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        findRepositoriesResponse.getTotalRecords());

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("owner")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.NOT_CONTAIN)
            .build();
    findRepositoriesRequest = FindRepositories.newBuilder().addPredicates(keyValueQuery).build();
    findRepositoriesResponse =
        versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
    LOGGER.info("FindProjects Response : " + findRepositoriesResponse.getRepositoriesList());
    assertEquals(
        "Project count not match with expected project count",
        0,
        findRepositoriesResponse.getRepositoriesCount());
    assertEquals(
        "Total records count not matched with expected records count",
        0,
        findRepositoriesResponse.getTotalRecords());

    stringValue = Value.newBuilder().setStringValue("asdasdasd").build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("owner")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.CONTAIN)
            .build();

    findRepositoriesRequest = FindRepositories.newBuilder().addPredicates(keyValueQuery).build();
    findRepositoriesResponse =
        versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
    LOGGER.info("FindProjects Response : " + findRepositoriesResponse.getRepositoriesList());
    assertEquals(
        "Project count not match with expected project count",
        0,
        findRepositoriesResponse.getRepositoriesCount());
    assertEquals(
        "Total records count not matched with expected records count",
        0,
        findRepositoriesResponse.getTotalRecords());

    LOGGER.info("FindRepositories by owner fuzzy search test stop ...");
  }

  @Test
  public void findRepositoriesByOwnerArrWithInOperatorTest() {
    LOGGER.info("FindRepositories by owner fuzzy search test start ...");
    if (!config.hasAuth()) {
      assertTrue(true);
      return;
    }

    GetUser getUserRequest =
        GetUser.newBuilder().setEmail(authClientInterceptor.getClient1Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo testUser1 = uacServiceStub.getUser(getUserRequest);

    getUserRequest = GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo testUser2 = uacServiceStub.getUser(getUserRequest);

    String[] ownerArr = {
      testUser1.getVertaInfo().getUserId(), testUser2.getVertaInfo().getUserId()
    };
    Value stringValue = Value.newBuilder().setStringValue(String.join(",", ownerArr)).build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("owner")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.IN)
            .build();
    FindRepositories findRepositoriesRequest =
        FindRepositories.newBuilder().addPredicates(keyValueQuery).build();
    FindRepositories.Response findRepositoriesResponse =
        versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
    LOGGER.info("FindProjects Response : " + findRepositoriesResponse.getRepositoriesList());
    assertEquals(
        "Project count not match with expected project count",
        3,
        findRepositoriesResponse.getRepositoriesCount());

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        findRepositoriesResponse.getTotalRecords());

    LOGGER.info("FindRepositories by owner fuzzy search test stop ...");
  }

  @Test
  public void deleteRepositoryWithCommitTagsTest()
      throws NoSuchAlgorithmException, ModelDBException {
    LOGGER.info("Delete Repository contains commit with tags test start.....");

    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    CreateCommitRequest createCommitRequest =
        CommitTest.getCreateCommitRequest(
            repository.getId(), 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET);

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    assertTrue("Commit not found in response", commitResponse.hasCommit());

    String tag = "v1.0";
    SetTagRequest setTagRequest =
        SetTagRequest.newBuilder()
            .setTag(tag)
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(
                RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .build();

    versioningServiceBlockingStub.setTag(setTagRequest);
    LOGGER.info("Delete Repository contains commit with tags test end.........");
  }

  @Test
  public void addRepositoryAttributes() {
    LOGGER.info("Add Repository Attributes test start................................");

    List<KeyValue> attributeList = getAttributeList();

    repository = repository.toBuilder().addAllAttributes(attributeList).build();
    SetRepository setRepository =
        SetRepository.newBuilder()
            .setId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build())
            .setRepository(repository)
            .build();
    SetRepository.Response response = versioningServiceBlockingStub.updateRepository(setRepository);
    Assert.assertEquals(
        "Repository attributes not match with expected repository attributes",
        attributeList,
        response.getRepository().getAttributesList());
    repository = response.getRepository();

    GetRepositoryRequest getRepositoryRequest =
        GetRepositoryRequest.newBuilder()
            .setId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
            .build();
    GetRepositoryRequest.Response getByNameResult =
        versioningServiceBlockingStub.getRepository(getRepositoryRequest);
    Assert.assertEquals(
        "Repository attributes not match with expected repository attributes",
        attributeList,
        getByNameResult.getRepository().getAttributesList());

    LOGGER.info("Add Repository Attributes test stop................................");
  }

  @Test
  public void updateRepositoryAttributes() {
    LOGGER.info("Update Repository Attributes test start................................");

    SetRepository setRepository = getSetRepositoryRequest("Repo-" + new Date().getTime());
    Repository repository = setRepository.toBuilder().getRepository();
    repository = repository.toBuilder().addAllAttributes(getAttributeList()).build();
    setRepository = setRepository.toBuilder().setRepository(repository).build();
    SetRepository.Response repositoryResponse =
        versioningServiceBlockingStub.createRepository(setRepository);
    repository = repositoryResponse.getRepository();

    try {
      List<KeyValue> attributes = repository.getAttributesList();
      Value stringValue =
          Value.newBuilder()
              .setStringValue(
                  "attribute_1542193772147_updated_test_value"
                      + Calendar.getInstance().getTimeInMillis())
              .build();
      KeyValue keyValue =
          KeyValue.newBuilder()
              .setKey(attributes.get(1).getKey())
              .setValue(stringValue)
              .setValueType(ValueTypeEnum.ValueType.STRING)
              .build();
      repository = repository.toBuilder().addAttributes(keyValue).build();
      RepositoryIdentification repositoryIdentification =
          RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build();
      SetRepository updateRepositoryAttributesRequest =
          SetRepository.newBuilder()
              .setId(repositoryIdentification)
              .setRepository(repository)
              .build();

      repositoryResponse =
          versioningServiceBlockingStub.updateRepository(updateRepositoryAttributesRequest);
      Assert.assertTrue(
          "Repository attributes not match with expected repository attributes",
          repositoryResponse.getRepository().getAttributesList().contains(keyValue));

      GetRepositoryRequest getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
              .build();
      GetRepositoryRequest.Response getByNameResult =
          versioningServiceBlockingStub.getRepository(getRepositoryRequest);
      Assert.assertTrue(
          "Repository attributes not match with expected repository attributes",
          getByNameResult.getRepository().getAttributesList().contains(keyValue));

      Value intValue =
          Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
      keyValue =
          KeyValue.newBuilder()
              .setKey(attributes.get(1).getKey())
              .setValue(intValue)
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      repository = repository.toBuilder().addAttributes(keyValue).build();
      updateRepositoryAttributesRequest =
          SetRepository.newBuilder()
              .setId(repositoryIdentification)
              .setRepository(repository)
              .build();

      repositoryResponse =
          versioningServiceBlockingStub.updateRepository(updateRepositoryAttributesRequest);
      Assert.assertTrue(
          "Repository attributes not match with expected repository attributes",
          repositoryResponse.getRepository().getAttributesList().contains(keyValue));

      getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
              .build();
      getByNameResult = versioningServiceBlockingStub.getRepository(getRepositoryRequest);
      Assert.assertTrue(
          "Repository attributes not match with expected repository attributes",
          getByNameResult.getRepository().getAttributesList().contains(keyValue));

      Value listValue =
          Value.newBuilder()
              .setListValue(
                  ListValue.newBuilder().addValues(intValue).addValues(stringValue).build())
              .build();
      keyValue =
          KeyValue.newBuilder()
              .setKey(attributes.get(0).getKey())
              .setValue(listValue)
              .setValueType(ValueTypeEnum.ValueType.LIST)
              .build();
      repository = repository.toBuilder().addAttributes(keyValue).build();
      updateRepositoryAttributesRequest =
          SetRepository.newBuilder()
              .setId(repositoryIdentification)
              .setRepository(repository)
              .build();

      repositoryResponse =
          versioningServiceBlockingStub.updateRepository(updateRepositoryAttributesRequest);
      Assert.assertTrue(
          "Repository attributes not match with expected repository attributes",
          repositoryResponse.getRepository().getAttributesList().contains(keyValue));

      getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
              .build();
      getByNameResult = versioningServiceBlockingStub.getRepository(getRepositoryRequest);
      Assert.assertTrue(
          "Repository attributes not match with expected repository attributes",
          getByNameResult.getRepository().getAttributesList().contains(keyValue));
    } finally {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
              .build();
      DeleteRepositoryRequest.Response response =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      assertTrue("Repository not delete", response.getStatus());
    }

    LOGGER.info("Update Repository Attributes test stop................................");
  }

  @Test
  public void findRepositoriesFoSharedUserTest() {
    LOGGER.info("FindRepositories by owner fuzzy search test start ....");
    if (!config.hasAuth()) {
      assertTrue(true);
      return;
    }

    GetUser getUserRequest =
        GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo testUser2 = uacServiceStub.getUser(getUserRequest);

    AddCollaboratorRequest addCollaboratorRequest =
        AddCollaboratorRequest.newBuilder()
            .setShareWith(testUser2.getEmail())
            .setPermission(
                CollaboratorPermissions.newBuilder()
                    .setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_WRITE)
                    .build())
            .setAuthzEntityType(EntitiesEnum.EntitiesTypes.USER)
            .addEntityIds(String.valueOf(repository.getId()))
            .build();
    AddCollaboratorRequest.Response collaboratorResponse =
        collaboratorServiceStubClient1.addOrUpdateRepositoryCollaborator(addCollaboratorRequest);
    assertTrue(collaboratorResponse.getStatus());

    FindRepositories findRepositoriesRequest =
        FindRepositories.newBuilder()
            .setWorkspaceName(testUser2.getVertaInfo().getUsername())
            .build();
    FindRepositories.Response findRepositoriesResponse =
        versioningServiceBlockingStubClient2.findRepositories(findRepositoriesRequest);
    LOGGER.info("FindProjects Response : " + findRepositoriesResponse.getRepositoriesList());
    assertEquals(
        "Project count not match with expected project count",
        1,
        findRepositoriesResponse.getRepositoriesCount());

    assertEquals(
        "Total records count not matched with expected records count",
        1,
        findRepositoriesResponse.getTotalRecords());

    LOGGER.info("FindRepositories by owner fuzzy search test stop ....");
  }

  @Test
  public void checkRepositoryNameWithColonAndSlashesTest() {
    LOGGER.info("check repository name with colon and slashes test start....");
    try {
      createRepository(versioningServiceBlockingStub, "Repo: colons test repository");
      fail();
    } catch (StatusRuntimeException e) {
      assertEquals(Status.INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
    }

    try {
      createRepository(versioningServiceBlockingStub, "Repo/ colons test repository");
      fail();
    } catch (StatusRuntimeException e) {
      assertEquals(Status.INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
    }

    try {
      createRepository(versioningServiceBlockingStub, "Repo\\\\ colons test repository");
      fail();
    } catch (StatusRuntimeException e) {
      assertEquals(Status.INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
    }

    LOGGER.info("check repository name with colon and slashes test end....");
  }

  @Test
  public void createdRepositoryWithDeletedRepositoryName() {
    LOGGER.info("createdRepositoryWithDeletedRepositoryName test start....");

    Repository repository = createRepository("Test-" + Calendar.getInstance().getTimeInMillis());

    CreateDataset createDatasetRequest = DatasetTest.getDatasetRequest(repository.getName());
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
            .build();
    DeleteRepositoryRequest.Response response =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    assertTrue("Repository not delete", response.getStatus());

    repository = createRepository(repository.getName());

    deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
            .build();
    response = versioningServiceBlockingStub.deleteRepository(deleteRepository);
    assertTrue("Repository not delete", response.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("createdRepositoryWithDeletedRepositoryName test end....");
  }
}
