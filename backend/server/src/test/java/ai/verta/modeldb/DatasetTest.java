package ai.verta.modeldb;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import ai.verta.common.Artifact;
import ai.verta.common.ArtifactTypeEnum.ArtifactType;
import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum;
import ai.verta.common.ValueTypeEnum.ValueType;
import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.AddGroupUsers;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GroupServiceGrpc;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ResourceTypeV2;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Workspace;
import com.google.common.util.concurrent.Futures;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class, webEnvironment = DEFINED_PORT)
@ContextConfiguration(classes = {ModeldbTestConfigurationBeans.class})
public class DatasetTest extends ModeldbTestSetup {

  private static final Logger LOGGER = LogManager.getLogger(DatasetTest.class);

  // Dataset Entities
  private static Dataset dataset1;
  private static Dataset dataset2;
  private static Dataset dataset3;
  private static Dataset dataset4;
  private static Map<String, Dataset> datasetMap = new HashMap<>();

  @Before
  public void createEntities() {
    initializeChannelBuilderAndExternalServiceStubs();

    if (isRunningIsolated()) {
      setupMockUacEndpoints(uac);
    }

    // Create all entities
    createDatasetEntities();
  }

  @After
  public void removeEntities() {
    if (!datasetMap.isEmpty()) {
      if (isRunningIsolated()) {
        mockGetResourcesForAllDatasets(datasetMap, testUser1);
        mockGetSelfAllowedResources(
            datasetMap.keySet(), ModelDBServiceResourceTypes.DATASET, ModelDBServiceActions.DELETE);
      }

      DeleteDatasets deleteDatasets =
          DeleteDatasets.newBuilder().addAllIds(datasetMap.keySet()).build();
      DeleteDatasets.Response deleteDatasetsResponse =
          datasetServiceStub.deleteDatasets(deleteDatasets);
      LOGGER.info("Datasets deleted successfully");
      LOGGER.info(deleteDatasetsResponse.toString());
      assertTrue(deleteDatasetsResponse.getStatus());
    }

    dataset1 = null;
    dataset2 = null;
    dataset3 = null;
    dataset4 = null;
    datasetMap = new HashMap<>();

    cleanUpResources();
  }

  private void createDatasetEntities() {
    if (isRunningIsolated()) {
      var resourcesResponse =
          GetResources.Response.newBuilder()
              .addItem(
                  GetResourcesResponseItem.newBuilder()
                      .setWorkspaceId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                      .setOwnerId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                      .setVisibility(ResourceVisibility.PRIVATE)
                      .build())
              .build();
      doReturn(resourcesResponse).when(collaboratorBlockingMock).getResources(any());
    }

    // Create two dataset of above dataset
    CreateDataset createDatasetRequest = getDatasetRequest("Dataset-1-" + new Date().getTime());
    KeyValue attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.012).build())
            .build();
    KeyValue attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.99).build())
            .build();
    createDatasetRequest =
        createDatasetRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("A00")
            .addTags("A01")
            .build();
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    dataset1 = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected Dataset name",
        createDatasetRequest.getName(),
        dataset1.getName());

    // dataset2 of above dataset
    createDatasetRequest = getDatasetRequest("Dataset-2-" + new Date().getTime());
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.31).build())
            .build();
    createDatasetRequest =
        createDatasetRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("A1")
            .addTags("A3")
            .addTags("A4")
            .build();
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    dataset2 = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected Dataset name",
        createDatasetRequest.getName(),
        dataset2.getName());

    // dataset3 of above dataset
    createDatasetRequest = getDatasetRequest("Dataset-3-" + new Date().getTime());
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.6543210).build())
            .build();
    createDatasetRequest =
        createDatasetRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .build();
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    dataset3 = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected Dataset name",
        createDatasetRequest.getName(),
        dataset3.getName());

    // dataset4 of above dataset
    createDatasetRequest = getDatasetRequest("Dataset-4-" + new Date().getTime());
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setNumberValue(1.00).build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.001212).build())
            .build();
    createDatasetRequest =
        createDatasetRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("A5")
            .addTags("A7")
            .addTags("A8")
            .build();
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    dataset4 = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected Dataset name",
        createDatasetRequest.getName(),
        dataset4.getName());

    datasetMap.put(dataset1.getId(), dataset1);
    datasetMap.put(dataset2.getId(), dataset2);
    datasetMap.put(dataset3.getId(), dataset3);
    datasetMap.put(dataset4.getId(), dataset4);

    if (isRunningIsolated()) {
      mockGetResourcesForAllDatasets(datasetMap, testUser1);
    }
  }

  private void checkValidArtifactPath(
      String entityId, String entityName, List<Artifact> artifacts) {
    for (var responseArtifact : artifacts) {
      var validPrefix =
          testConfig.getArtifactStoreConfig().getPathPrefixWithSeparator() + entityName;
      var path = validPrefix + "/" + entityId + "/" + responseArtifact.getKey();

      var filenameExtension = responseArtifact.getFilenameExtension();
      if (!filenameExtension.isEmpty() && !filenameExtension.endsWith("." + filenameExtension)) {
        path += "." + filenameExtension;
      }

      assertEquals(
          "Dataset artifact path not match with expected artifact path",
          path,
          responseArtifact.getPath());
    }
  }

  private void checkEqualsAssert(StatusRuntimeException e) {
    Status status = Status.fromThrowable(e);
    LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
    if (testConfig.hasAuth()) {
      assertTrue(
          Status.PERMISSION_DENIED.getCode() == status.getCode()
              || Status.NOT_FOUND.getCode()
                  == status.getCode()); // because of shadow delete the response could be 403 or 404
    } else {
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }
  }

  private CreateDataset getDatasetRequest(String datasetName) {

    List<KeyValue> attributeList = new ArrayList<>();
    Value stringValue =
        Value.newBuilder()
            .setStringValue("attribute_" + Calendar.getInstance().getTimeInMillis() + "_value")
            .build();
    KeyValue keyValue =
        KeyValue.newBuilder()
            .setKey("attribute_1_" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .build();
    attributeList.add(keyValue);

    Value intValue = Value.newBuilder().setNumberValue(12345).build();
    keyValue =
        KeyValue.newBuilder()
            .setKey("attribute_2_" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build();
    attributeList.add(keyValue);

    Value listValue =
        Value.newBuilder()
            .setListValue(ListValue.newBuilder().addValues(intValue).addValues(stringValue).build())
            .build();
    keyValue =
        KeyValue.newBuilder()
            .setKey("attribute_3_" + Calendar.getInstance().getTimeInMillis())
            .setValue(listValue)
            .setValueType(ValueType.LIST)
            .build();
    attributeList.add(keyValue);

    return CreateDataset.newBuilder()
        .setName(datasetName)
        .setVisibility(ResourceVisibility.PRIVATE)
        .addTags("A")
        .addTags("A0")
        .addAllAttributes(attributeList)
        .build();
  }

  public static CreateDataset getDatasetRequestForOtherTests(String datasetName) {
    return CreateDataset.newBuilder()
        .setName(datasetName)
        .setVisibility(ResourceVisibility.PRIVATE)
        .addTags("A")
        .addTags("A0")
        .build();
  }

  @Test
  public void createAndDeleteDatasetTest() {
    LOGGER.info("Create and delete Dataset test start................................");

    try {
      CreateDataset createDatasetRequest = getDatasetRequest(dataset1.getName());
      datasetServiceStub.createDataset(createDatasetRequest);
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    long id = 0;
    try {
      id =
          RepositoryTest.createRepository(
              versioningServiceBlockingStub, "Repo-" + new Date().getTime());

    } finally {
      if (id != 0) {
        DeleteRepositoryRequest.Response deleteRepoResponse =
            versioningServiceBlockingStub.deleteRepository(
                DeleteRepositoryRequest.newBuilder()
                    .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
                    .build());
        assertTrue(deleteRepoResponse.getStatus());
      }
    }

    LOGGER.info("Create and delete Dataset test stop................................");
  }

  @Test
  public void getAllDatasetTest() {
    LOGGER.info("LogDataset test start................................");

    int pageLimit = 1;
    boolean isExpectedResultFound = false;
    int actualPageNumber = 1;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      GetAllDatasets getAllDatasets =
          GetAllDatasets.newBuilder()
              .setPageNumber(pageNumber)
              .setPageLimit(pageLimit)
              .setAscending(false)
              .setSortKey(ModelDBConstants.NAME)
              .build();

      GetAllDatasets.Response datasetResponse = datasetServiceStub.getAllDatasets(getAllDatasets);
      if (datasetResponse.getDatasetsList() != null
          && datasetResponse.getDatasetsList().size() > 0) {
        isExpectedResultFound = true;
        LOGGER.info("GetAllDataset Response : " + datasetResponse.getDatasetsCount());
        Dataset resDataset = null;
        for (Dataset dataset : datasetResponse.getDatasetsList()) {
          if (datasetMap.containsKey(dataset.getId())) {
            assertEquals(
                "Dataset not match with expected Dataset",
                datasetMap.get(dataset.getId()),
                dataset);
            resDataset = datasetResponse.getDatasets(0);
          }
        }

        if (resDataset == null) {
          continue;
        }
        if (datasetMap.containsKey(resDataset.getId())) {
          if (actualPageNumber == 1) {
            assertEquals(
                "Dataset not match with expected Dataset",
                datasetMap.get(resDataset.getId()),
                dataset4);
          } else if (actualPageNumber == 3) {
            assertEquals(
                "Dataset not match with expected Dataset",
                datasetMap.get(resDataset.getId()),
                dataset2);
          }
        }
        actualPageNumber = actualPageNumber + 1;
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More Dataset not found in database");
          assertTrue(true);
        } else {
          fail("Expected Dataset not found in response");
        }
        break;
      }
    }

    GetAllDatasets getAllDataset = GetAllDatasets.newBuilder().build();
    GetAllDatasets.Response getAllDatasetResponse =
        datasetServiceStub.getAllDatasets(getAllDataset);

    for (int index = 0; index < getAllDatasetResponse.getDatasetsList().size(); index++) {
      Dataset dataset = getAllDatasetResponse.getDatasets(index);
      if (datasetMap.containsKey(dataset.getId())) {
        if (index == 0) {
          assertEquals(
              "Dataset name not match with expected dataset name",
              dataset4.getName(),
              dataset.getName());
        } else if (index == 1) {
          assertEquals(
              "Dataset name not match with expected dataset name",
              dataset3.getName(),
              dataset.getName());
        }
      }
    }

    LOGGER.info("LogDataset test stop................................");
  }

  @Test
  public void getDatasetByIdTest() {
    LOGGER.info("Get Dataset by Id test start................................");

    if (isRunningIsolated()) {
      mockGetResourcesForAllDatasets(Map.of(dataset1.getId(), dataset1), testUser1);
    }

    GetDatasetById getDatasetById = GetDatasetById.newBuilder().setId(dataset1.getId()).build();
    GetDatasetById.Response getDatasetByIdResponse =
        datasetServiceStub.getDatasetById(getDatasetById);
    assertEquals(
        "Dataset not match with expected dataset", dataset1, getDatasetByIdResponse.getDataset());

    LOGGER.info("Get Dataset by Id test stop................................");
  }

  @Test
  public void getDatasetByName() {
    LOGGER.info("Get Dataset by name test start................................");

    if (isRunningIsolated()) {
      doReturn(testUser2).when(uacBlockingMock).getCurrentUser(any());
    }

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("Dataset-" + new Date().getTime());
    CreateDataset.Response createDatasetResponse =
        datasetServiceStubClient2.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    if (isRunningIsolated()) {
      mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), testUser2);
    }

    try {
      GetDatasetByName getDataset =
          GetDatasetByName.newBuilder().setName(dataset.getName()).build();

      GetDatasetByName.Response response = datasetServiceStubClient2.getDatasetByName(getDataset);
      LOGGER.info("Response DatasetByUser of Dataset : " + response.getDatasetByUser());
      LOGGER.info("Response SharedDatasetsList of Datasets : " + response.getSharedDatasetsList());
      assertEquals(
          "Dataset name not match", dataset.getName(), response.getDatasetByUser().getName());
      for (Dataset sharedDataset : response.getSharedDatasetsList()) {
        assertEquals("Shared dataset name not match", dataset.getName(), sharedDataset.getName());
      }

      if (testConfig.hasAuth()) {
        if (isRunningIsolated()) {
          doReturn(testUser1).when(uacBlockingMock).getCurrentUser(any());
          mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), testUser2);
        } else if (!testConfig.isPermissionV2Enabled()) {
          AddCollaboratorRequest addCollaboratorRequest =
              CollaboratorUtils.addCollaboratorRequestDataset(
                  dataset,
                  authClientInterceptor.getClient1Email(),
                  CollaboratorTypeEnum.CollaboratorType.READ_WRITE);

          AddCollaboratorRequest.Response addOrUpdateDatasetCollaboratorResponse =
              collaboratorServiceStubClient2.addOrUpdateDatasetCollaborator(addCollaboratorRequest);
          LOGGER.info(
              "Collaborator added in server : "
                  + addOrUpdateDatasetCollaboratorResponse.getStatus());
          assertTrue(addOrUpdateDatasetCollaboratorResponse.getStatus());
        }

        GetDatasetByName.Response getDatasetByNameResponse =
            datasetServiceStub.getDatasetByName(getDataset);
        LOGGER.info(
            "Response DatasetByUser of Dataset : " + getDatasetByNameResponse.getDatasetByUser());
        LOGGER.info(
            "Response SharedDatasetsList of Datasets : "
                + getDatasetByNameResponse.getSharedDatasetsList());
        assertTrue(
            "Dataset name not match",
            getDatasetByNameResponse.getDatasetByUser() == null
                || getDatasetByNameResponse.getDatasetByUser().getId().isEmpty());
        for (Dataset sharedDataset : getDatasetByNameResponse.getSharedDatasetsList()) {
          assertEquals("Shared dataset name not match", dataset.getName(), sharedDataset.getName());
        }

        // Create dataset
        createDatasetRequest = getDatasetRequest("Dataset-" + new Date().getTime());
        createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
        Dataset selfDataset = createDatasetResponse.getDataset();
        LOGGER.info("Dataset created successfully");
        assertEquals(
            "Dataset name not match with expected dataset name",
            createDatasetRequest.getName(),
            selfDataset.getName());
        if (isRunningIsolated()) {
          mockGetResourcesForAllDatasets(Map.of(selfDataset.getId(), selfDataset), testUser1);
        }

        try {
          getDataset = GetDatasetByName.newBuilder().setName(selfDataset.getName()).build();
          getDatasetByNameResponse = datasetServiceStub.getDatasetByName(getDataset);
          LOGGER.info(
              "Response DatasetByUser of Dataset : " + getDatasetByNameResponse.getDatasetByUser());
          LOGGER.info(
              "Response SharedDatasetsList of Datasets : "
                  + getDatasetByNameResponse.getSharedDatasetsList());
          assertEquals(
              "Dataset name not match",
              selfDataset.getName(),
              getDatasetByNameResponse.getDatasetByUser().getName());
          for (Dataset sharedDataset : getDatasetByNameResponse.getSharedDatasetsList()) {
            assertEquals(
                "Shared dataset name not match", selfDataset.getName(), sharedDataset.getName());
          }
        } finally {
          DeleteDataset deleteDataset =
              DeleteDataset.newBuilder().setId(selfDataset.getId()).build();
          DeleteDataset.Response deleteDatasetResponse =
              datasetServiceStub.deleteDataset(deleteDataset);
          LOGGER.info("Dataset deleted successfully");
          LOGGER.info(deleteDatasetResponse.toString());
          assertTrue(deleteDatasetResponse.getStatus());
        }
      }
    } finally {
      if (isRunningIsolated()) {
        doReturn(testUser2).when(uacBlockingMock).getCurrentUser(any());
        mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), testUser2);
      }
      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStubClient2.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());
    }

    LOGGER.info("Get dataset by name test stop................................");
  }

  @Test
  public void k_getDatasetByNameWithWorkspace() {
    LOGGER.info("Get Dataset by name with workspace test start................................");
    if (!testConfig.hasAuth()) {
      assertTrue(true);
      return;
    }

    if (isRunningIsolated()) {
      doReturn(testUser2).when(uacBlockingMock).getCurrentUser(any());
    }

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("Dataset-" + new Date().getTime());
    var workspaceNameUser2 = testUser2.getVertaInfo().getUsername();
    if (testConfig.isPermissionV2Enabled() && !isRunningIsolated()) {
      var groupIdUser1 =
          createAndGetGroup(authServiceChannelServiceUser, organizationId, testUser1);
      var groupStub = GroupServiceGrpc.newBlockingStub(authServiceChannelServiceUser);
      groupStub.addUsers(
          AddGroupUsers.newBuilder()
              .addUserId(testUser2.getVertaInfo().getUserId())
              .setGroupId(groupIdUser1)
              .setOrgId(organizationId)
              .build());

      var roleIdUser1 =
          createAndGetRole(
                  authServiceChannelServiceUser,
                  organizationId,
                  Optional.empty(),
                  Set.of(ResourceTypeV2.PROJECT, ResourceTypeV2.DATASET))
              .getRole()
              .getId();

      var testUser1Workspace =
          createWorkspaceAndRoleForUser(
              authServiceChannelServiceUser,
              organizationId,
              groupIdUser1,
              roleIdUser1,
              testUser2.getVertaInfo().getUsername(),
              Optional.empty());
      workspaceNameUser2 = organizationId + "/" + testUser1Workspace.getName();
    }
    createDatasetRequest =
        createDatasetRequest.toBuilder().setWorkspaceName(workspaceNameUser2).build();
    CreateDataset.Response createDatasetResponse =
        datasetServiceStubClient2.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());
    try {

      if (isRunningIsolated()) {
        doReturn(testUser1).when(uacBlockingMock).getCurrentUser(any());
        mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), testUser1);
      } else if (!testConfig.isPermissionV2Enabled()) {
        AddCollaboratorRequest addCollaboratorRequest =
            CollaboratorUtils.addCollaboratorRequestDataset(
                dataset,
                authClientInterceptor.getClient1Email(),
                CollaboratorTypeEnum.CollaboratorType.READ_WRITE);

        AddCollaboratorRequest.Response addOrUpdateDatasetCollaboratorResponse =
            collaboratorServiceStubClient2.addOrUpdateDatasetCollaborator(addCollaboratorRequest);
        LOGGER.info(
            "Collaborator added in server : " + addOrUpdateDatasetCollaboratorResponse.getStatus());
        assertTrue(addOrUpdateDatasetCollaboratorResponse.getStatus());
      }

      // Create dataset
      createDatasetRequest = getDatasetRequest(dataset.getName());
      var workspaceNameUser1 = testUser1.getVertaInfo().getUsername();
      if (testConfig.isPermissionV2Enabled()) {
        workspaceNameUser1 = getWorkspaceNameUser1();
        createDatasetRequest =
            createDatasetRequest.toBuilder().setWorkspaceName(workspaceNameUser1).build();
      }
      createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
      Dataset selfDataset = createDatasetResponse.getDataset();
      LOGGER.info("Dataset created successfully");
      assertEquals(
          "Dataset name not match with expected dataset name",
          createDatasetRequest.getName(),
          selfDataset.getName());

      try {
        if (isRunningIsolated()) {
          mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), testUser2);
          doReturn(
                  Workspace.newBuilder()
                      .setId(testUser2.getVertaInfo().getDefaultWorkspaceId())
                      .setUsername(testUser2.getVertaInfo().getUsername())
                      .build())
              .when(workspaceBlockingMock)
              .getWorkspaceByName(any());
        }
        GetDatasetByName getDataset =
            GetDatasetByName.newBuilder()
                .setName(selfDataset.getName())
                .setWorkspaceName(workspaceNameUser2)
                .build();
        GetDatasetByName.Response getDatasetByNameResponse =
            datasetServiceStub.getDatasetByName(getDataset);
        LOGGER.info(
            "Response DatasetByUser of Dataset : " + getDatasetByNameResponse.getDatasetByUser());
        LOGGER.info(
            "Response SharedDatasetsList of Datasets : "
                + getDatasetByNameResponse.getSharedDatasetsList());
        assertTrue(
            "Dataset name not match",
            getDatasetByNameResponse.getDatasetByUser() == null
                || getDatasetByNameResponse.getDatasetByUser().getId().isEmpty());
        for (Dataset sharedDataset : getDatasetByNameResponse.getSharedDatasetsList()) {
          assertEquals("Shared dataset name not match", dataset.getName(), sharedDataset.getName());
        }

        if (isRunningIsolated()) {
          mockGetResourcesForAllDatasets(Map.of(selfDataset.getId(), selfDataset), testUser1);
          doReturn(
                  Workspace.newBuilder()
                      .setId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                      .setUsername(testUser1.getVertaInfo().getUsername())
                      .build())
              .when(workspaceBlockingMock)
              .getWorkspaceByName(any());
        }

        getDataset =
            GetDatasetByName.newBuilder()
                .setName(selfDataset.getName())
                .setWorkspaceName(workspaceNameUser1)
                .build();
        getDatasetByNameResponse = datasetServiceStub.getDatasetByName(getDataset);
        LOGGER.info(
            "Response DatasetByUser of Dataset : " + getDatasetByNameResponse.getDatasetByUser());
        LOGGER.info(
            "Response SharedDatasetsList of Datasets : "
                + getDatasetByNameResponse.getSharedDatasetsList());
        assertTrue(
            "Dataset name not match",
            getDatasetByNameResponse.getDatasetByUser() != null
                && getDatasetByNameResponse.getDatasetByUser().equals(selfDataset));
        for (Dataset sharedDataset : getDatasetByNameResponse.getSharedDatasetsList()) {
          assertEquals("Shared dataset name not match", dataset.getName(), sharedDataset.getName());
        }
      } finally {
        DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(selfDataset.getId()).build();
        DeleteDataset.Response deleteDatasetResponse =
            datasetServiceStub.deleteDataset(deleteDataset);
        LOGGER.info("Dataset deleted successfully");
        LOGGER.info(deleteDatasetResponse.toString());
        assertTrue(deleteDatasetResponse.getStatus());
      }
    } finally {
      if (isRunningIsolated()) {
        mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), testUser1);
        mockGetSelfAllowedResources(
            Set.of(dataset.getId()),
            ModelDBServiceResourceTypes.DATASET,
            ModelDBServiceActions.DELETE);
      }
      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStubClient2.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());
    }
    LOGGER.info(
        "Get dataset by name with Email or Username test stop................................");
  }

  @Test
  public void getDatasetByNameNotFoundTest() {
    LOGGER.info("Get Dataset by name NOT_FOUND test start................................");

    try {
      GetDatasetByName getDataset = GetDatasetByName.newBuilder().setName("test").build();
      datasetServiceStub.getDatasetByName(getDataset);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }
    LOGGER.info("Get dataset by name NOT_FOUND test stop................................");
  }

  @Test
  public void updateDatasetName() {
    LOGGER.info("Update Dataset Name test start................................");

    UpdateDatasetName updateDatasetNameRequest =
        UpdateDatasetName.newBuilder()
            .setId(dataset1.getId())
            .setName("Dataset Name Update 1")
            .build();

    UpdateDatasetName.Response response =
        datasetServiceStub.updateDatasetName(updateDatasetNameRequest);
    LOGGER.info("UpdateDatasetName Response : " + response.getDataset());
    assertEquals(
        "Dataset name not match with expected dataset name",
        updateDatasetNameRequest.getName(),
        response.getDataset().getName());
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset1.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);

    updateDatasetNameRequest =
        UpdateDatasetName.newBuilder()
            .setId(dataset1.getId())
            .setName("Dataset Name Update 2")
            .build();

    response = datasetServiceStub.updateDatasetName(updateDatasetNameRequest);
    LOGGER.info("UpdateDatasetName Response : " + response.getDataset());
    assertEquals(
        "Dataset name not match with expected dataset name",
        updateDatasetNameRequest.getName(),
        response.getDataset().getName());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);

    LOGGER.info("Update Dataset Name test stop................................");
  }

  @Test
  public void updateDatasetNameNegativeTest() {
    LOGGER.info("Update Dataset Name Negative test start................................");

    UpdateDatasetName updateDatasetNameRequest =
        UpdateDatasetName.newBuilder().setName("Update Dataset Name 1 ").build();

    try {
      datasetServiceStub.updateDatasetName(updateDatasetNameRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    updateDatasetNameRequest =
        UpdateDatasetName.newBuilder().setId(dataset1.getId()).setName(dataset1.getName()).build();
    try {
      datasetServiceStub.updateDatasetName(updateDatasetNameRequest);
      assertTrue(true);
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    LOGGER.info("Update Dataset Name test stop................................");
  }

  @Test
  public void updateDatasetDescription() {
    LOGGER.info("Update Dataset Description test start................................");

    UpdateDatasetDescription updateDescriptionRequest =
        UpdateDatasetDescription.newBuilder()
            .setId(dataset1.getId())
            .setDescription("Dataset Description Update 1")
            .build();

    UpdateDatasetDescription.Response response =
        datasetServiceStub.updateDatasetDescription(updateDescriptionRequest);
    LOGGER.info("UpdateDatasetDescription Response : " + response.getDataset());
    assertEquals(
        "Dataset description not match with expected dataset description",
        updateDescriptionRequest.getDescription(),
        response.getDataset().getDescription());
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset1.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);

    updateDescriptionRequest =
        UpdateDatasetDescription.newBuilder()
            .setId(dataset1.getId())
            .setDescription("Dataset Description Update 2")
            .build();

    response = datasetServiceStub.updateDatasetDescription(updateDescriptionRequest);
    LOGGER.info("UpdateDatasetDescription Response : " + response.getDataset());
    assertEquals(
        "Dataset description not match with expected dataset description",
        updateDescriptionRequest.getDescription(),
        response.getDataset().getDescription());
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset1.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);

    updateDescriptionRequest =
        UpdateDatasetDescription.newBuilder().setId(dataset1.getId()).build();

    response = datasetServiceStub.updateDatasetDescription(updateDescriptionRequest);
    LOGGER.info("UpdateDatasetDescription Response : " + response.getDataset());
    assertEquals(
        "Dataset description not match with expected dataset description",
        updateDescriptionRequest.getDescription(),
        response.getDataset().getDescription());
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset1.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);

    LOGGER.info("Update Dataset Description test stop................................");
  }

  @Test
  public void updateDatasetDescriptionNegativeTest() {
    LOGGER.info("Update Dataset Description Negative test start................................");

    UpdateDatasetDescription updateDescriptionRequest =
        UpdateDatasetDescription.newBuilder()
            .setDescription(
                "This is update from UpdateDatasetDescription."
                    + Calendar.getInstance().getTimeInMillis())
            .build();

    try {
      datasetServiceStub.updateDatasetDescription(updateDescriptionRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Update Dataset Description test stop................................");
  }

  @Test
  public void addDatasetTags() {
    LOGGER.info("Add Dataset Tags test start................................");

    List<String> tagsList = new ArrayList<>();
    tagsList.add("A11");
    tagsList.add("A22");
    AddDatasetTags addDatasetTagsRequest =
        AddDatasetTags.newBuilder().setId(dataset1.getId()).addAllTags(tagsList).build();

    AddDatasetTags.Response response = datasetServiceStub.addDatasetTags(addDatasetTagsRequest);

    Dataset checkDataset = response.getDataset();
    assertEquals(dataset1.getTagsCount() + tagsList.size(), checkDataset.getTagsCount());
    assertEquals(dataset1.getTagsCount() + tagsList.size(), checkDataset.getTagsList().size());
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset1.getTimeUpdated(),
        checkDataset.getTimeUpdated());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);

    tagsList = new ArrayList<>();
    tagsList.add("A33");
    tagsList.add("A22");
    addDatasetTagsRequest =
        AddDatasetTags.newBuilder().setId(dataset1.getId()).addAllTags(tagsList).build();

    response = datasetServiceStub.addDatasetTags(addDatasetTagsRequest);

    assertNotEquals(
        "Dataset date_updated field not update on database",
        checkDataset.getTimeUpdated(),
        response.getDataset().getTimeUpdated());

    checkDataset = response.getDataset();
    assertEquals(dataset1.getTagsCount() + 1, checkDataset.getTagsCount());
    assertEquals(dataset1.getTagsCount() + 1, checkDataset.getTagsList().size());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);

    try {
      String tag52 = "Human Activity Recognition using Smartphone Dataset";
      addDatasetTagsRequest =
          AddDatasetTags.newBuilder().setId(dataset1.getId()).addTags(tag52).build();
      datasetServiceStub.addDatasetTags(addDatasetTagsRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Add Dataset tags test stop................................");
  }

  @Test
  public void addDatasetNegativeTags() {
    LOGGER.info("Add Dataset Tags Negative test start................................");

    List<String> tagsList = new ArrayList<>();
    tagsList.add("A " + Calendar.getInstance().getTimeInMillis());
    tagsList.add("A 2 " + Calendar.getInstance().getTimeInMillis());
    AddDatasetTags addDatasetTagsRequest = AddDatasetTags.newBuilder().addAllTags(tagsList).build();

    try {
      datasetServiceStub.addDatasetTags(addDatasetTagsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    addDatasetTagsRequest =
        AddDatasetTags.newBuilder().setId("123123").addAllTags(dataset1.getTagsList()).build();

    try {
      datasetServiceStub.addDatasetTags(addDatasetTagsRequest);
      fail();
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    LOGGER.info("Add Dataset tags Negative test stop................................");
  }

  @Test
  public void deleteDatasetTags() {
    LOGGER.info("Delete Dataset Tags test start................................");

    try {
      List<String> removableTags = dataset1.getTagsList();
      if (removableTags.size() == 0) {
        LOGGER.info("Dataset Tags not found in database ");
        fail();
        return;
      }
      if (dataset1.getTagsList().size() > 1) {
        removableTags = dataset1.getTagsList().subList(0, dataset1.getTagsList().size() - 1);
      }
      DeleteDatasetTags deleteDatasetTagsRequest =
          DeleteDatasetTags.newBuilder().setId(dataset1.getId()).addAllTags(removableTags).build();

      DeleteDatasetTags.Response response =
          datasetServiceStub.deleteDatasetTags(deleteDatasetTagsRequest);
      LOGGER.info("Tags deleted in server : " + response.getDataset().getTagsList());
      assertTrue(response.getDataset().getTagsList().size() <= 1);
      assertNotEquals(
          "Dataset date_updated field not update on database",
          dataset1.getTimeUpdated(),
          response.getDataset().getTimeUpdated());
      dataset1 = response.getDataset();
      datasetMap.put(dataset1.getId(), dataset1);

      if (response.getDataset().getTagsList().size() > 0) {
        deleteDatasetTagsRequest =
            DeleteDatasetTags.newBuilder().setId(dataset1.getId()).setDeleteAll(true).build();

        response = datasetServiceStub.deleteDatasetTags(deleteDatasetTagsRequest);
        LOGGER.info("Tags deleted in server : " + response.getDataset().getTagsList());
        assertEquals(0, response.getDataset().getTagsList().size());
        assertNotEquals(
            "Dataset date_updated field not update on database",
            dataset1.getTimeUpdated(),
            response.getDataset().getTimeUpdated());
        dataset1 = response.getDataset();
        datasetMap.put(dataset1.getId(), dataset1);
      }
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      fail();
    }

    LOGGER.info("Delete Dataset tags test stop................................");
  }

  @Test
  public void deleteDatasetTagsNegativeTest() {
    LOGGER.info("Delete Dataset Tags Negative test start................................");

    DeleteDatasetTags deleteDatasetTagsRequest = DeleteDatasetTags.newBuilder().build();

    try {
      datasetServiceStub.deleteDatasetTags(deleteDatasetTagsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    deleteDatasetTagsRequest =
        DeleteDatasetTags.newBuilder().setId(dataset1.getId()).setDeleteAll(true).build();

    DeleteDatasetTags.Response response =
        datasetServiceStub.deleteDatasetTags(deleteDatasetTagsRequest);
    LOGGER.info("Tags deleted in server : " + response.getDataset().getTagsList());
    assertEquals(0, response.getDataset().getTagsList().size());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);

    LOGGER.info("Delete Dataset tags Negative test stop................................");
  }

  @Test
  public void addDatasetAttributes() {
    LOGGER.info("Add Dataset Attributes test start................................");

    List<KeyValue> attributeList = new ArrayList<>();
    Value intValue = Value.newBuilder().setNumberValue(1.1).build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_1" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build());
    Value stringValue =
        Value.newBuilder()
            .setStringValue("attributes_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_2" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.BLOB)
            .build());

    AddDatasetAttributes addDatasetAttributesRequest =
        AddDatasetAttributes.newBuilder()
            .setId(dataset1.getId())
            .addAllAttributes(attributeList)
            .build();

    AddDatasetAttributes.Response response =
        datasetServiceStub.addDatasetAttributes(addDatasetAttributesRequest);
    LOGGER.info("Added Dataset Attributes: \n" + response.getDataset());
    assertTrue(response.getDataset().getAttributesList().containsAll(attributeList));
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset1.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);

    LOGGER.info("Add Dataset Attributes test stop................................");
  }

  @Test
  public void addDatasetAttributesNegativeTest() {
    LOGGER.info("Add Dataset Attributes Negative test start................................");

    List<KeyValue> attributeList = new ArrayList<>();
    Value intValue = Value.newBuilder().setNumberValue(1.1).build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build());
    Value stringValue =
        Value.newBuilder()
            .setStringValue("attributes_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.BLOB)
            .build());

    AddDatasetAttributes addDatasetAttributesRequest =
        AddDatasetAttributes.newBuilder().addAllAttributes(attributeList).build();

    try {
      datasetServiceStub.addDatasetAttributes(addDatasetAttributesRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Add Dataset Attributes Negative test stop................................");
  }

  @Test
  public void updateDatasetAttributes() {
    LOGGER.info("Update Dataset Attributes test start................................");

    addDatasetAttributes();
    List<KeyValue> attributes = dataset1.getAttributesList();
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
            .setValueType(ValueType.STRING)
            .build();
    UpdateDatasetAttributes updateDatasetAttributesRequest =
        UpdateDatasetAttributes.newBuilder().setId(dataset1.getId()).setAttribute(keyValue).build();

    UpdateDatasetAttributes.Response response =
        datasetServiceStub.updateDatasetAttributes(updateDatasetAttributesRequest);
    LOGGER.info("Updated Dataset : \n" + response.getDataset());
    assertTrue(response.getDataset().getAttributesList().contains(keyValue));
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset1.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);

    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    keyValue =
        KeyValue.newBuilder()
            .setKey(attributes.get(1).getKey())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build();
    updateDatasetAttributesRequest =
        UpdateDatasetAttributes.newBuilder().setId(dataset1.getId()).setAttribute(keyValue).build();

    response = datasetServiceStub.updateDatasetAttributes(updateDatasetAttributesRequest);
    LOGGER.info("Updated Dataset : \n" + response.getDataset());
    assertTrue(response.getDataset().getAttributesList().contains(keyValue));
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset1.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);

    Value listValue =
        Value.newBuilder()
            .setListValue(ListValue.newBuilder().addValues(intValue).addValues(stringValue).build())
            .build();
    keyValue =
        KeyValue.newBuilder()
            .setKey(attributes.get(0).getKey())
            .setValue(listValue)
            .setValueType(ValueType.LIST)
            .build();
    updateDatasetAttributesRequest =
        UpdateDatasetAttributes.newBuilder().setId(dataset1.getId()).setAttribute(keyValue).build();

    response = datasetServiceStub.updateDatasetAttributes(updateDatasetAttributesRequest);
    LOGGER.info("Updated Dataset : \n" + response.getDataset());
    assertTrue(response.getDataset().getAttributesList().contains(keyValue));
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset1.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);
    LOGGER.info("Update Dataset Attributes test stop................................");
  }

  @Test
  public void updateDatasetAttributesNegativeTest() {
    LOGGER.info("Update Dataset Attributes Negative test start................................");

    addDatasetAttributes();
    List<KeyValue> attributes = dataset1.getAttributesList();
    Value stringValue = Value.newBuilder().setStringValue("attribute_updated_test_value").build();
    KeyValue keyValue =
        KeyValue.newBuilder()
            .setKey(attributes.get(0).getKey())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build();
    UpdateDatasetAttributes updateDatasetAttributesRequest =
        UpdateDatasetAttributes.newBuilder().setAttribute(keyValue).build();

    try {
      datasetServiceStub.updateDatasetAttributes(updateDatasetAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    updateDatasetAttributesRequest =
        UpdateDatasetAttributes.newBuilder()
            .setId("123132")
            .setAttribute(dataset1.getAttributesList().get(0))
            .build();
    try {
      datasetServiceStub.updateDatasetAttributes(updateDatasetAttributesRequest);
      fail();
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    updateDatasetAttributesRequest =
        UpdateDatasetAttributes.newBuilder().setId(dataset1.getId()).clearAttribute().build();

    try {
      datasetServiceStub.updateDatasetAttributes(updateDatasetAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Update Dataset Attributes Negative test stop................................");
  }

  @Test
  public void deleteDatasetAttributesTest() {
    LOGGER.info("Delete Dataset Attributes test start................................");

    addDatasetAttributes();
    List<KeyValue> attributes = dataset1.getAttributesList();
    LOGGER.info("Attributes size : " + attributes.size());
    List<String> keys = new ArrayList<>();
    if (attributes.size() > 1) {
      for (int index = 0; index < attributes.size() - 1; index++) {
        KeyValue keyValue = attributes.get(index);
        keys.add(keyValue.getKey());
      }
    } else {
      keys.add(attributes.get(0).getKey());
    }
    LOGGER.info("Attributes key size : " + keys.size());

    DeleteDatasetAttributes deleteDatasetAttributesRequest =
        DeleteDatasetAttributes.newBuilder()
            .setId(dataset1.getId())
            .addAllAttributeKeys(keys)
            .build();

    DeleteDatasetAttributes.Response response =
        datasetServiceStub.deleteDatasetAttributes(deleteDatasetAttributesRequest);
    LOGGER.info("Attributes deleted in server : " + response.getDataset());
    assertEquals(1, response.getDataset().getAttributesList().size());
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset1.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset1 = response.getDataset();
    datasetMap.put(dataset1.getId(), dataset1);

    if (response.getDataset().getAttributesList().size() != 0) {
      deleteDatasetAttributesRequest =
          DeleteDatasetAttributes.newBuilder().setId(dataset1.getId()).setDeleteAll(true).build();
      response = datasetServiceStub.deleteDatasetAttributes(deleteDatasetAttributesRequest);
      LOGGER.info(
          "All the Attributes deleted from server. Attributes count : "
              + response.getDataset().getAttributesCount());
      assertEquals(0, response.getDataset().getAttributesList().size());
      assertNotEquals(
          "Dataset date_updated field not update on database",
          dataset1.getTimeUpdated(),
          response.getDataset().getTimeUpdated());
      dataset1 = response.getDataset();
      datasetMap.put(dataset1.getId(), dataset1);
    }

    LOGGER.info("Delete Dataset Attributes test stop................................");
  }

  @Test
  public void deleteDatasetAttributesNegativeTest() {
    LOGGER.info("Delete Dataset Attributes Negative test start................................");

    DeleteDatasetAttributes deleteDatasetAttributesRequest =
        DeleteDatasetAttributes.newBuilder().build();

    try {
      datasetServiceStub.deleteDatasetAttributes(deleteDatasetAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Delete Dataset Attributes Negative test stop................................");
  }

  @Test
  public void batchDeleteDatasetsTest() {
    LOGGER.info("batch delete Dataset test start................................");

    if (isRunningIsolated()) {
      doReturn(testUser1).when(uacBlockingMock).getCurrentUser(any());
    }

    Map<String, Dataset> datasetMap = new HashMap<>();
    CreateDataset createDatasetRequest = getDatasetRequest("Dataset-" + new Date().getTime());
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    var dataset = createDatasetResponse.getDataset();
    datasetMap.put(dataset.getId(), dataset);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());

    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        createDatasetResponse.getDataset().getName());

    createDatasetRequest = getDatasetRequest("Dataset-" + new Date().getTime());
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    dataset = createDatasetResponse.getDataset();
    datasetMap.put(dataset.getId(), dataset);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());

    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        createDatasetResponse.getDataset().getName());

    if (isRunningIsolated()) {
      mockGetResourcesForAllDatasets(datasetMap, testUser1);
    }

    DeleteDatasets deleteDatasets =
        DeleteDatasets.newBuilder().addAllIds(datasetMap.keySet()).build();
    DeleteDatasets.Response deleteDatasetsResponse =
        datasetServiceStub.deleteDatasets(deleteDatasets);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetsResponse.toString());
    assertTrue(deleteDatasetsResponse.getStatus());

    LOGGER.info("batch delete Dataset test stop................................");
  }

  @Test
  public void getLastExperimentByDataset() throws Exception {
    LOGGER.info("Get last experiment by dataset test start................................");
    // Create project
    CreateProject createProjectRequest =
        ProjectTest.getCreateProjectRequest("project-1" + new Date().getTime());
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project.getName());

    CreateDataset createDatasetRequest = getDatasetRequest("Dataset-" + new Date().getTime());
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    Dataset dataset = createDatasetResponse.getDataset();
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    if (isRunningIsolated()) {
      mockGetResourcesForAllProjects(Map.of(project.getId(), project), testUser1);
      mockGetSelfAllowedResources(
          Set.of(), ModelDBServiceResourceTypes.REPOSITORY, ModelDBServiceActions.READ);
    }

    try {
      // Create two experiment of above project
      CreateExperiment createExperimentRequest =
          ExperimentTest.getCreateExperimentRequestForOtherTests(
              project.getId(), "Experiment-" + new Date().getTime());
      CreateExperiment.Response createExperimentResponse =
          experimentServiceStub.createExperiment(createExperimentRequest);
      Experiment experiment1 = createExperimentResponse.getExperiment();
      LOGGER.info("Experiment created successfully");
      assertEquals(
          "Experiment name not match with expected Experiment name",
          createExperimentRequest.getName(),
          experiment1.getName());

      createExperimentRequest =
          ExperimentTest.getCreateExperimentRequestForOtherTests(
              project.getId(), "Experiment-" + new Date().getTime());
      createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
      Experiment experiment2 = createExperimentResponse.getExperiment();
      LOGGER.info("Experiment created successfully");
      assertEquals(
          "Experiment name not match with expected Experiment name",
          createExperimentRequest.getName(),
          experiment2.getName());

      CreateExperimentRun createExperimentRunRequest =
          ExperimentRunTest.getCreateExperimentRunRequestForOtherTests(
              project.getId(), experiment1.getId(), "ExperimentRun-" + new Date().getTime());
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
      LOGGER.info("ExperimentRun created successfully");
      assertEquals(
          "ExperimentRun name not match with expected ExperimentRun name",
          createExperimentRunRequest.getName(),
          experimentRun.getName());

      createExperimentRunRequest =
          ExperimentRunTest.getCreateExperimentRunRequestForOtherTests(
              project.getId(), experiment2.getId(), "ExperimentRun-" + new Date().getTime());
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun2 = createExperimentRunResponse.getExperimentRun();
      LOGGER.info("ExperimentRun created successfully");
      assertEquals(
          "ExperimentRun name not match with expected ExperimentRun name",
          createExperimentRunRequest.getName(),
          experimentRun2.getName());

      if (isRunningIsolated()) {
        mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), testUser1);
      }

      CreateDatasetVersion createDatasetVersionRequest =
          DatasetVersionTest.getDatasetVersionRequest(dataset.getId());
      CreateDatasetVersion.Response createDatasetVersionResponse =
          datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
      DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
      LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion1);
      assertEquals(
          "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
          dataset.getId(),
          datasetVersion1.getDatasetId());

      createDatasetVersionRequest = DatasetVersionTest.getDatasetVersionRequest(dataset.getId());
      createDatasetVersionResponse =
          datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
      DatasetVersion datasetVersion2 = createDatasetVersionResponse.getDatasetVersion();
      LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion2);
      assertEquals(
          "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
          dataset.getId(),
          datasetVersion2.getDatasetId());

      Artifact artifact =
          Artifact.newBuilder()
              .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
              .setPath("This is new added data artifact type in Google Pay datasets")
              .setArtifactType(ArtifactType.DATA)
              .setLinkedArtifactId(datasetVersion2.getId())
              .setUploadCompleted(
                  !testConfig
                      .getArtifactStoreConfig()
                      .getArtifactStoreType()
                      .equals(CommonConstants.S3))
              .build();

      LogDataset logDatasetRequest =
          LogDataset.newBuilder().setId(experimentRun2.getId()).setDataset(artifact).build();

      if (isRunningIsolated()) {
        mockGetResourcesForAllProjects(Map.of(project.getId(), project), testUser1);
        mockGetSelfAllowedResources(
            Set.of(), ModelDBServiceResourceTypes.REPOSITORY, ModelDBServiceActions.READ);
      }

      experimentRunServiceStub.logDataset(logDatasetRequest);

      GetExperimentRunById getExperimentRunById =
          GetExperimentRunById.newBuilder().setId(experimentRun2.getId()).build();
      GetExperimentRunById.Response response =
          experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
      LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());
      checkValidArtifactPath(
          response.getExperimentRun().getId(),
          "ExperimentRunEntity",
          response.getExperimentRun().getDatasetsList());
      var keys =
          response.getExperimentRun().getDatasetsList().stream()
              .map(Artifact::getKey)
              .collect(Collectors.toList());
      assertTrue(
          "Experiment dataset not match with expected dataset", keys.contains(artifact.getKey()));

      assertNotEquals(
          "ExperimentRun date_updated field not update on database",
          experimentRun2.getDateUpdated(),
          response.getExperimentRun().getDateUpdated());

      artifact =
          Artifact.newBuilder()
              .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
              .setPath("This is new added data artifact type in Google Pay datasets")
              .setArtifactType(ArtifactType.DATA)
              .setLinkedArtifactId(datasetVersion1.getId())
              .setUploadCompleted(
                  !testConfig
                      .getArtifactStoreConfig()
                      .getArtifactStoreType()
                      .equals(CommonConstants.S3))
              .build();

      logDatasetRequest =
          LogDataset.newBuilder().setId(experimentRun.getId()).setDataset(artifact).build();

      experimentRunServiceStub.logDataset(logDatasetRequest);

      getExperimentRunById = GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
      response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
      LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());
      checkValidArtifactPath(
          response.getExperimentRun().getId(),
          "ExperimentRunEntity",
          response.getExperimentRun().getDatasetsList());
      keys =
          response.getExperimentRun().getDatasetsList().stream()
              .map(Artifact::getKey)
              .collect(Collectors.toList());
      assertTrue(
          "Experiment dataset not match with expected dataset", keys.contains(artifact.getKey()));

      assertNotEquals(
          "ExperimentRun date_updated field not update on database",
          experimentRun.getDateUpdated(),
          response.getExperimentRun().getDateUpdated());

      updateTimestampOfResources();

      if (isRunningIsolated()) {
        mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), testUser1);
        mockGetResources(Map.of(project.getId(), project.getName()), testUser1);
        mockGetSelfAllowedResources(
            Set.of(project.getId()),
            ModelDBServiceResourceTypes.PROJECT,
            ModelDBServiceActions.READ);
        mockGetSelfAllowedResources(
            Set.of(), ModelDBServiceResourceTypes.REPOSITORY, ModelDBServiceActions.READ);
      }

      LastExperimentByDatasetId lastExperimentByDatasetId =
          LastExperimentByDatasetId.newBuilder().setDatasetId(dataset.getId()).build();
      LastExperimentByDatasetId.Response lastExperimentResponse =
          datasetServiceStub.getLastExperimentByDatasetId(lastExperimentByDatasetId);
      assertEquals(
          "Last updated Experiment not match with expected last updated Experiment",
          experiment1.getId(),
          lastExperimentResponse.getExperiment().getId());

      KeyValueQuery keyValueQuery =
          KeyValueQuery.newBuilder()
              .setKey(ModelDBConstants.ATTRIBUTES + ".loss")
              .setValue(Value.newBuilder().setNumberValue(0.12).build())
              .setOperator(OperatorEnum.Operator.IN)
              .build();
      FindExperimentRuns findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setExperimentId(experiment1.getId())
              .addPredicates(keyValueQuery)
              .build();
      try {
        experimentRunServiceStub.findExperimentRuns(findExperimentRuns);
      } catch (StatusRuntimeException ex) {
        Status status = Status.fromThrowable(ex);
        LOGGER.warn(
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
      }

    } finally {
      if (isRunningIsolated()) {
        mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), testUser1);
        mockGetSelfAllowedResources(
            Set.of(dataset.getId()),
            ModelDBServiceResourceTypes.DATASET,
            ModelDBServiceActions.DELETE);
      }
      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());

      if (isRunningIsolated()) {
        mockGetResourcesForAllProjects(Map.of(project.getId(), project), testUser1);
        mockGetSelfAllowedResources(
            Set.of(project.getId()),
            ModelDBServiceResourceTypes.PROJECT,
            ModelDBServiceActions.DELETE);
      }
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }

    LOGGER.info("Get last experiment by dataset test stop................................");
  }

  @Test
  public void getExperimentRunByDataset() {
    LOGGER.info("Get experimentRun by dataset test start................................");
    // Create project
    CreateProject createProjectRequest =
        ProjectTest.getCreateProjectRequest("project-" + new Date().getTime());
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project.getName());

    CreateDataset createDatasetRequest = getDatasetRequest("Dataset-" + new Date().getTime());
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    Dataset dataset = createDatasetResponse.getDataset();
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    try {
      // Create two experiment of above project
      CreateExperiment createExperimentRequest =
          ExperimentTest.getCreateExperimentRequestForOtherTests(
              project.getId(), "Experiment-" + new Date().getTime());
      CreateExperiment.Response createExperimentResponse =
          experimentServiceStub.createExperiment(createExperimentRequest);
      Experiment experiment1 = createExperimentResponse.getExperiment();
      LOGGER.info("Experiment created successfully");
      assertEquals(
          "Experiment name not match with expected Experiment name",
          createExperimentRequest.getName(),
          experiment1.getName());

      createExperimentRequest =
          ExperimentTest.getCreateExperimentRequestForOtherTests(
              project.getId(), "Experiment-" + new Date().getTime());
      createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
      Experiment experiment2 = createExperimentResponse.getExperiment();
      LOGGER.info("Experiment created successfully");
      assertEquals(
          "Experiment name not match with expected Experiment name",
          createExperimentRequest.getName(),
          experiment2.getName());

      if (isRunningIsolated()) {
        mockGetSelfAllowedResources(
            Set.of(project.getId()),
            ModelDBServiceResourceTypes.PROJECT,
            ModelDBServiceActions.READ);
        mockGetSelfAllowedResources(
            Set.of(), ModelDBServiceResourceTypes.REPOSITORY, ModelDBServiceActions.READ);
      }

      CreateExperimentRun createExperimentRunRequest =
          ExperimentRunTest.getCreateExperimentRunRequestForOtherTests(
              project.getId(), experiment1.getId(), "ExperimentRun-" + new Date().getTime());
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
      LOGGER.info("ExperimentRun created successfully");
      assertEquals(
          "ExperimentRun name not match with expected ExperimentRun name",
          createExperimentRunRequest.getName(),
          experimentRun.getName());

      createExperimentRunRequest =
          ExperimentRunTest.getCreateExperimentRunRequestForOtherTests(
              project.getId(), experiment2.getId(), "ExperimentRun-" + new Date().getTime());
      createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun2 = createExperimentRunResponse.getExperimentRun();
      LOGGER.info("ExperimentRun created successfully");
      assertEquals(
          "ExperimentRun name not match with expected ExperimentRun name",
          createExperimentRunRequest.getName(),
          experimentRun2.getName());

      CreateDatasetVersion createDatasetVersionRequest =
          DatasetVersionTest.getDatasetVersionRequest(dataset.getId());
      CreateDatasetVersion.Response createDatasetVersionResponse =
          datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
      DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
      LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion1);
      assertEquals(
          "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
          dataset.getId(),
          datasetVersion1.getDatasetId());

      createDatasetVersionRequest = DatasetVersionTest.getDatasetVersionRequest(dataset.getId());
      createDatasetVersionResponse =
          datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
      DatasetVersion datasetVersion2 = createDatasetVersionResponse.getDatasetVersion();
      LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion2);
      assertEquals(
          "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
          dataset.getId(),
          datasetVersion2.getDatasetId());

      Artifact artifact =
          Artifact.newBuilder()
              .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
              .setPath("This is new added data artifact type in Google Pay datasets")
              .setArtifactType(ArtifactType.DATA)
              .setLinkedArtifactId(datasetVersion2.getId())
              .setUploadCompleted(
                  !testConfig
                      .getArtifactStoreConfig()
                      .getArtifactStoreType()
                      .equals(CommonConstants.S3))
              .build();

      LogDataset logDatasetRequest =
          LogDataset.newBuilder().setId(experimentRun2.getId()).setDataset(artifact).build();

      experimentRunServiceStub.logDataset(logDatasetRequest);

      GetExperimentRunById getExperimentRunById =
          GetExperimentRunById.newBuilder().setId(experimentRun2.getId()).build();
      GetExperimentRunById.Response response =
          experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
      LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());
      checkValidArtifactPath(
          response.getExperimentRun().getId(),
          "ExperimentRunEntity",
          response.getExperimentRun().getDatasetsList());
      var keys =
          response.getExperimentRun().getDatasetsList().stream()
              .map(Artifact::getKey)
              .collect(Collectors.toList());
      assertTrue(
          "Experiment dataset not match with expected dataset", keys.contains(artifact.getKey()));

      assertNotEquals(
          "ExperimentRun date_updated field not update on database",
          experimentRun2.getDateUpdated(),
          response.getExperimentRun().getDateUpdated());

      artifact =
          Artifact.newBuilder()
              .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
              .setPath("This is new added data artifact type in Google Pay datasets")
              .setArtifactType(ArtifactType.DATA)
              .setLinkedArtifactId(datasetVersion1.getId())
              .setUploadCompleted(
                  !testConfig
                      .getArtifactStoreConfig()
                      .getArtifactStoreType()
                      .equals(CommonConstants.S3))
              .build();

      logDatasetRequest =
          LogDataset.newBuilder().setId(experimentRun.getId()).setDataset(artifact).build();

      experimentRunServiceStub.logDataset(logDatasetRequest);

      getExperimentRunById = GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
      response = experimentRunServiceStub.getExperimentRunById(getExperimentRunById);
      LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());
      checkValidArtifactPath(
          response.getExperimentRun().getId(),
          "ExperimentRunEntity",
          response.getExperimentRun().getDatasetsList());
      keys =
          response.getExperimentRun().getDatasetsList().stream()
              .map(Artifact::getKey)
              .collect(Collectors.toList());
      assertTrue(
          "Experiment dataset not match with expected dataset", keys.contains(artifact.getKey()));

      assertNotEquals(
          "ExperimentRun date_updated field not update on database",
          experimentRun.getDateUpdated(),
          response.getExperimentRun().getDateUpdated());

      if (isRunningIsolated()) {
        mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), testUser1);
        mockGetResources(Map.of(project.getId(), project.getName()), testUser1);
        mockGetSelfAllowedResources(
            Set.of(project.getId()),
            ModelDBServiceResourceTypes.PROJECT,
            ModelDBServiceActions.READ);
        mockGetSelfAllowedResources(
            Set.of(), ModelDBServiceResourceTypes.REPOSITORY, ModelDBServiceActions.READ);
      }

      GetExperimentRunByDataset getExperimentRunByDatasetRequest =
          GetExperimentRunByDataset.newBuilder().setDatasetId(dataset.getId()).build();
      GetExperimentRunByDataset.Response getExperimentRunByDatasetResponse =
          datasetServiceStub.getExperimentRunByDataset(getExperimentRunByDatasetRequest);
      assertEquals(
          "ExperimentRun count not match with expected ExperimentRun count",
          2,
          getExperimentRunByDatasetResponse.getExperimentRunsCount());

      assertEquals(
          "ExperimentRun not match with expected ExperimentRun",
          experimentRun.getId(),
          getExperimentRunByDatasetResponse.getExperimentRuns(0).getId());

      assertEquals(
          "ExperimentRun not match with expected ExperimentRun",
          experimentRun2.getId(),
          getExperimentRunByDatasetResponse.getExperimentRuns(1).getId());

      KeyValueQuery keyValueQuery =
          KeyValueQuery.newBuilder()
              .setKey(ModelDBConstants.ATTRIBUTES + ".loss")
              .setValue(Value.newBuilder().setNumberValue(0.12).build())
              .setOperator(OperatorEnum.Operator.IN)
              .build();
      FindExperimentRuns findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setExperimentId(experiment1.getId())
              .addPredicates(keyValueQuery)
              .build();
      try {
        experimentRunServiceStub.findExperimentRuns(findExperimentRuns);
      } catch (StatusRuntimeException ex) {
        Status status = Status.fromThrowable(ex);
        LOGGER.warn(
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
      }
    } finally {
      if (isRunningIsolated()) {
        mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), testUser1);
        mockGetSelfAllowedResources(
            Set.of(dataset.getId()),
            ModelDBServiceResourceTypes.DATASET,
            ModelDBServiceActions.DELETE);
      }
      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());

      if (isRunningIsolated()) {
        mockGetResourcesForAllProjects(Map.of(project.getId(), project), testUser1);
        mockGetSelfAllowedResources(
            Set.of(project.getId()),
            ModelDBServiceResourceTypes.PROJECT,
            ModelDBServiceActions.DELETE);
      }
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info("Project deleted successfully");
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    }

    LOGGER.info("Get experimentRun by dataset test stop................................");
  }

  @Test
  public void createDatasetAndRepositoryWithSameNameTest() {
    LOGGER.info("Create and delete Dataset test start................................");

    long id =
        RepositoryTest.createRepository(
            versioningServiceBlockingStub, "Repo-" + new Date().getTime());

    CreateDataset createDatasetRequest = getDatasetRequest("Detaset-" + new Date().getTime());
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);

    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());

    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        createDatasetResponse.getDataset().getName());

    if (isRunningIsolated()) {
      var dataset = createDatasetResponse.getDataset();
      mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), testUser1);
    }

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    versioningServiceBlockingStub.deleteRepository(deleteRepository);

    DeleteDataset deleteDataset =
        DeleteDataset.newBuilder().setId(createDatasetResponse.getDataset().getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Create and delete Dataset test stop................................");
  }

  @Test
  public void checkDatasetNameWithColonAndSlashesTest() {
    LOGGER.info("check dataset name with colon and slashes test start...........");

    CreateDataset createDatasetRequest =
        getDatasetRequest("Dataset: colons test dataset-" + new Date().getTime());
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset1 = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset1);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset1.getName());

    createDatasetRequest =
        getDatasetRequest("Dataset/ colons test dataset-" + new Date().getTime());
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset2 = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset2);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset2.getName());

    createDatasetRequest =
        getDatasetRequest("Dataset\\\\ colons test dataset-" + new Date().getTime());
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset3 = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset3);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset3.getName());

    for (Dataset dataset : new Dataset[] {dataset1, dataset2, dataset3}) {
      if (isRunningIsolated()) {
        var resourcesResponse =
            GetResources.Response.newBuilder()
                .addItem(
                    GetResourcesResponseItem.newBuilder()
                        .setResourceId(dataset.getId())
                        .setWorkspaceId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                        .setOwnerId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                        .setVisibility(ResourceVisibility.PRIVATE)
                        .build())
                .build();
        doReturn(Futures.immediateFuture(resourcesResponse))
            .when(collaboratorMock)
            .getResources(any());
        doReturn(resourcesResponse).when(collaboratorBlockingMock).getResources(any());
      }
      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());
    }

    LOGGER.info("check dataset name with colon and slashes test stop...........");
  }

  @Test
  public void createAndDeleteDatasetUsingServiceAccount() {
    // Create two dataset of above dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset-" + new Date().getTime());
    CreateDataset.Response createDatasetResponse =
        datasetServiceStubServiceAccount.createDataset(createDatasetRequest);

    if (isRunningIsolated()) {
      var dataset = createDatasetResponse.getDataset();
      mockGetResourcesForAllDatasets(Map.of(dataset.getId(), dataset), serviceAccountUser);
    }

    DeleteDatasets deleteDatasets =
        DeleteDatasets.newBuilder().addIds(createDatasetResponse.getDataset().getId()).build();
    DeleteDatasets.Response deleteDatasetsResponse =
        datasetServiceStubServiceAccount.deleteDatasets(deleteDatasets);
    assertTrue(deleteDatasetsResponse.getStatus());
  }
}
