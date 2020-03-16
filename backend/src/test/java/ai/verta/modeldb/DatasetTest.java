package ai.verta.modeldb;

import static org.junit.Assert.*;

import ai.verta.modeldb.DatasetServiceGrpc.DatasetServiceBlockingStub;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.ValueTypeEnum.ValueType;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.CollaboratorTypeEnum;
import ai.verta.uac.GetUser;
import ai.verta.uac.UACServiceGrpc;
import ai.verta.uac.UserInfo;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
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
public class DatasetTest {

  private static final Logger LOGGER = LogManager.getLogger(DatasetTest.class);
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

  private void checkEqualsAssert(StatusRuntimeException e) {
    Status status = Status.fromThrowable(e);
    LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
    } else {
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }
  }

  public CreateDataset getDatasetRequest(String datasetName) {

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
            .setValueType(ValueTypeEnum.ValueType.NUMBER)
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
            .setValueType(ValueTypeEnum.ValueType.LIST)
            .build();
    attributeList.add(keyValue);

    return CreateDataset.newBuilder()
        .setName(datasetName)
        .setDatasetType(DatasetTypeEnum.DatasetType.RAW)
        .setDatasetVisibility(DatasetVisibility.PRIVATE)
        .addTags("tag_x")
        .addTags("tag_y")
        .addAllAttributes(attributeList)
        .build();
  }

  @Test
  public void createAndDeleteDatasetTest() {
    LOGGER.info("Create and delete Dataset test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest = getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);

    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());

    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        createDatasetResponse.getDataset().getName());

    DeleteDataset deleteDataset =
        DeleteDataset.newBuilder().setId(createDatasetResponse.getDataset().getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Create and delete Dataset test stop................................");
  }

  @Test
  public void getAllDatasetTest() {
    LOGGER.info("LogDataset test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    Map<String, Dataset> datasetsMap = new HashMap<>();
    CreateDataset createDatasetRequest = getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset1 = createDatasetResponse.getDataset();
    datasetsMap.put(dataset1.getId(), dataset1);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());

    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        createDatasetResponse.getDataset().getName());

    createDatasetRequest = getDatasetRequest("train_data_1");
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset2 = createDatasetResponse.getDataset();
    datasetsMap.put(dataset2.getId(), dataset2);
    LOGGER.info("LogDataset Response : \n" + createDatasetResponse.getDataset());
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        createDatasetResponse.getDataset().getName());

    createDatasetRequest = getDatasetRequest("train_data_2");
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset3 = createDatasetResponse.getDataset();
    datasetsMap.put(dataset3.getId(), dataset3);
    LOGGER.info("LogDataset Response : \n" + createDatasetResponse.getDataset());
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        createDatasetResponse.getDataset().getName());

    int pageLimit = 1;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      GetAllDatasets getAllDatasets =
          GetAllDatasets.newBuilder()
              .setPageNumber(pageNumber)
              .setPageLimit(pageLimit)
              .setAscending(false)
              .setSortKey(ModelDBConstants.NAME)
              .build();

      GetAllDatasets.Response datasetResponse = datasetServiceStub.getAllDatasets(getAllDatasets);

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          datasetResponse.getTotalRecords());

      if (datasetResponse.getDatasetsList() != null
          && datasetResponse.getDatasetsList().size() > 0) {
        isExpectedResultFound = true;
        LOGGER.info("GetAllDataset Response : " + datasetResponse.getDatasetsCount());
        for (Dataset dataset : datasetResponse.getDatasetsList()) {
          assertEquals(
              "Dataset not match with expected Dataset", datasetsMap.get(dataset.getId()), dataset);
        }

        if (pageNumber == 1) {
          assertEquals(
              "Dataset not match with expected Dataset",
              datasetsMap.get(datasetResponse.getDatasets(0).getId()),
              dataset3);
        } else if (pageNumber == 3) {
          assertEquals(
              "Dataset not match with expected Dataset",
              datasetsMap.get(datasetResponse.getDatasets(0).getId()),
              dataset1);
        }

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
    assertEquals(
        "Dataset count not match with expected dataset count",
        3,
        getAllDatasetResponse.getTotalRecords());

    for (int index = 0; index < getAllDatasetResponse.getDatasetsList().size(); index++) {
      Dataset dataset = getAllDatasetResponse.getDatasets(index);
      if (index == 0) {
        assertEquals(
            "Dataset name not match with expected dataset name",
            dataset3.getName(),
            dataset.getName());
      } else if (index == 1) {
        assertEquals(
            "Dataset name not match with expected dataset name",
            dataset2.getName(),
            dataset.getName());
      }

      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());
    }

    LOGGER.info("LogDataset test stop................................");
  }

  @Test
  public void getDatasetByIdTest() {
    LOGGER.info("Get Dataset by Id test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest = getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    GetDatasetById getDatasetById = GetDatasetById.newBuilder().setId(dataset.getId()).build();
    GetDatasetById.Response getDatasetByIdResponse =
        datasetServiceStub.getDatasetById(getDatasetById);
    assertEquals(
        "Dataset not match with expected dataset", dataset, getDatasetByIdResponse.getDataset());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Get Dataset by Id test stop................................");
  }

  @Test
  public void getDatasetByName() {
    LOGGER.info("Get Dataset by name test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub client2DatasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(client2Channel);

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_f_apt");
    CreateDataset.Response createDatasetResponse =
        client2DatasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    GetDatasetByName getDataset = GetDatasetByName.newBuilder().setName(dataset.getName()).build();

    GetDatasetByName.Response response = client2DatasetServiceStub.getDatasetByName(getDataset);
    LOGGER.info("Response DatasetByUser of Dataset : " + response.getDatasetByUser());
    LOGGER.info("Response SharedDatasetsList of Datasets : " + response.getSharedDatasetsList());
    assertEquals(
        "Dataset name not match", dataset.getName(), response.getDatasetByUser().getName());
    for (Dataset sharedDataset : response.getSharedDatasetsList()) {
      assertEquals("Shared dataset name not match", dataset.getName(), sharedDataset.getName());
    }

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceStub =
          CollaboratorServiceGrpc.newBlockingStub(client2Channel);
      AddCollaboratorRequest addCollaboratorRequest =
          CollaboratorTest.addCollaboratorRequestDataset(
              dataset,
              authClientInterceptor.getClient1Email(),
              CollaboratorTypeEnum.CollaboratorType.READ_WRITE);

      AddCollaboratorRequest.Response addOrUpdateDatasetCollaboratorResponse =
          collaboratorServiceStub.addOrUpdateDatasetCollaborator(addCollaboratorRequest);
      LOGGER.info(
          "Collaborator added in server : " + addOrUpdateDatasetCollaboratorResponse.getStatus());
      assertTrue(addOrUpdateDatasetCollaboratorResponse.getStatus());

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
      createDatasetRequest = getDatasetRequest("dataset_f_apt");
      createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
      Dataset selfDataset = createDatasetResponse.getDataset();
      LOGGER.info("Dataset created successfully");
      assertEquals(
          "Dataset name not match with expected dataset name",
          createDatasetRequest.getName(),
          selfDataset.getName());

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

      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(selfDataset.getId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse =
        client2DatasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Get dataset by name test stop................................");
  }

  @Test
  public void k_getDatasetByNameWithWorkspace() {
    LOGGER.info("Get Dataset by name with workspace test start................................");
    if (app.getAuthServerHost() == null || app.getAuthServerPort() == null) {
      assertTrue(true);
      return;
    }

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub client2DatasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(client2Channel);

    UACServiceGrpc.UACServiceBlockingStub uaServiceStub =
        UACServiceGrpc.newBlockingStub(authServiceChannel);
    GetUser getUserRequest =
        GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo secondUserInfo = uaServiceStub.getUser(getUserRequest);

    getUserRequest = GetUser.newBuilder().setEmail(authClientInterceptor.getClient1Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo firstUserInfo = uaServiceStub.getUser(getUserRequest);

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_f_apt");
    createDatasetRequest =
        createDatasetRequest
            .toBuilder()
            .setWorkspaceName(secondUserInfo.getVertaInfo().getUsername())
            .build();
    CreateDataset.Response createDatasetResponse =
        client2DatasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(client2Channel);
    AddCollaboratorRequest addCollaboratorRequest =
        CollaboratorTest.addCollaboratorRequestDataset(
            dataset,
            authClientInterceptor.getClient1Email(),
            CollaboratorTypeEnum.CollaboratorType.READ_WRITE);

    AddCollaboratorRequest.Response addOrUpdateDatasetCollaboratorResponse =
        collaboratorServiceStub.addOrUpdateDatasetCollaborator(addCollaboratorRequest);
    LOGGER.info(
        "Collaborator added in server : " + addOrUpdateDatasetCollaboratorResponse.getStatus());
    assertTrue(addOrUpdateDatasetCollaboratorResponse.getStatus());

    // Create dataset
    createDatasetRequest = getDatasetRequest("dataset_f_apt");
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    Dataset selfDataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        selfDataset.getName());

    GetDatasetByName getDataset =
        GetDatasetByName.newBuilder()
            .setName(selfDataset.getName())
            .setWorkspaceName(secondUserInfo.getVertaInfo().getUsername())
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

    getDataset =
        GetDatasetByName.newBuilder()
            .setName(selfDataset.getName())
            .setWorkspaceName(firstUserInfo.getVertaInfo().getUsername())
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

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(selfDataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    deleteDatasetResponse = client2DatasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info(
        "Get dataset by name with Email or Username test stop................................");
  }

  @Test
  public void getDatasetByNameNotFoundTest() {
    LOGGER.info("Get Dataset by name NOT_FOUND test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
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

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    UpdateDatasetName updateDatasetNameRequest =
        UpdateDatasetName.newBuilder()
            .setId(dataset.getId())
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
        dataset.getTimeUpdated(),
        response.getDataset().getTimeUpdated());

    updateDatasetNameRequest =
        UpdateDatasetName.newBuilder()
            .setId(dataset.getId())
            .setName("Dataset Name Update 2")
            .build();

    response = datasetServiceStub.updateDatasetName(updateDatasetNameRequest);
    LOGGER.info("UpdateDatasetName Response : " + response.getDataset());
    assertEquals(
        "Dataset name not match with expected dataset name",
        updateDatasetNameRequest.getName(),
        response.getDataset().getName());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Update Dataset Name test stop................................");
  }

  @Test
  public void updateDatasetNameNegativeTest() {
    LOGGER.info("Update Dataset Name Negative test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

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

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    LOGGER.info("Dataset Id : " + dataset.getId());
    updateDatasetNameRequest =
        UpdateDatasetName.newBuilder().setId(dataset.getId()).setName(dataset.getName()).build();
    try {
      datasetServiceStub.updateDatasetName(updateDatasetNameRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Update Dataset Name test stop................................");
  }

  @Test
  public void updateDatasetDescription() {
    LOGGER.info("Update Dataset Description test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    UpdateDatasetDescription updateDescriptionRequest =
        UpdateDatasetDescription.newBuilder()
            .setId(dataset.getId())
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
        dataset.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset = response.getDataset();

    updateDescriptionRequest =
        UpdateDatasetDescription.newBuilder()
            .setId(dataset.getId())
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
        dataset.getTimeUpdated(),
        response.getDataset().getTimeUpdated());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Update Dataset Description test stop................................");
  }

  @Test
  public void updateDatasetDescriptionNegativeTest() {
    LOGGER.info("Update Dataset Description Negative test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

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

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Update Dataset Description test stop................................");
  }

  @Test
  public void addDatasetTags() {
    LOGGER.info("Add Dataset Tags test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    List<String> tagsList = new ArrayList<>();
    tagsList.add("Add Test Tag1");
    tagsList.add("Add Test Tag2");
    AddDatasetTags addDatasetTagsRequest =
        AddDatasetTags.newBuilder().setId(dataset.getId()).addAllTags(tagsList).build();

    AddDatasetTags.Response response = datasetServiceStub.addDatasetTags(addDatasetTagsRequest);

    Dataset checkDataset = response.getDataset();
    assertEquals(4, checkDataset.getTagsCount());
    assertEquals(4, checkDataset.getTagsList().size());
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset.getTimeUpdated(),
        checkDataset.getTimeUpdated());

    tagsList = new ArrayList<>();
    tagsList.add("Add Test Tag3");
    tagsList.add("Add Test Tag2");
    addDatasetTagsRequest =
        AddDatasetTags.newBuilder().setId(dataset.getId()).addAllTags(tagsList).build();

    response = datasetServiceStub.addDatasetTags(addDatasetTagsRequest);

    assertNotEquals(
        "Dataset date_updated field not update on database",
        checkDataset.getTimeUpdated(),
        response.getDataset().getTimeUpdated());

    checkDataset = response.getDataset();
    assertEquals(5, checkDataset.getTagsCount());
    assertEquals(5, checkDataset.getTagsList().size());

    try {
      String tag52 = "Human Activity Recognition using Smartphone Dataset";
      addDatasetTagsRequest =
          AddDatasetTags.newBuilder().setId(dataset.getId()).addTags(tag52).build();
      datasetServiceStub.addDatasetTags(addDatasetTagsRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Add Dataset tags test stop................................");
  }

  @Test
  public void addDatasetNegativeTags() {
    LOGGER.info("Add Dataset Tags Negative test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

    List<String> tagsList = new ArrayList<>();
    tagsList.add("Add Test Tag " + Calendar.getInstance().getTimeInMillis());
    tagsList.add("Add Test Tag 2 " + Calendar.getInstance().getTimeInMillis());
    AddDatasetTags addDatasetTagsRequest = AddDatasetTags.newBuilder().addAllTags(tagsList).build();

    try {
      datasetServiceStub.addDatasetTags(addDatasetTagsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    addDatasetTagsRequest =
        AddDatasetTags.newBuilder().setId("sdasd").addAllTags(dataset.getTagsList()).build();

    try {
      datasetServiceStub.addDatasetTags(addDatasetTagsRequest);
      fail();
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Add Dataset tags Negative test stop................................");
  }

  @Test
  public void getDatasetTags() {
    LOGGER.info("Get Dataset Tags test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    GetTags deleteDatasetTagsRequest = GetTags.newBuilder().setId(dataset.getId()).build();
    GetTags.Response response = datasetServiceStub.getDatasetTags(deleteDatasetTagsRequest);
    LOGGER.info("Tags deleted in server : " + response.getTagsList());
    assertTrue(dataset.getTagsList().containsAll(response.getTagsList()));

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Get Dataset tags test stop................................");
  }

  @Test
  public void getDatasetTagsNegativeTest() {
    LOGGER.info("Get Dataset Tags Negative test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

    GetTags deleteDatasetTagsRequest = GetTags.newBuilder().build();

    try {
      datasetServiceStub.getDatasetTags(deleteDatasetTagsRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Get Dataset tags Negative test stop................................");
  }

  @Test
  public void deleteDatasetTags() {
    LOGGER.info("Delete Dataset Tags test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    try {
      List<String> removableTags = dataset.getTagsList();
      if (removableTags.size() == 0) {
        LOGGER.info("Dataset Tags not found in database ");
        fail();
        return;
      }
      if (dataset.getTagsList().size() > 1) {
        removableTags = dataset.getTagsList().subList(0, dataset.getTagsList().size() - 1);
      }
      DeleteDatasetTags deleteDatasetTagsRequest =
          DeleteDatasetTags.newBuilder().setId(dataset.getId()).addAllTags(removableTags).build();

      DeleteDatasetTags.Response response =
          datasetServiceStub.deleteDatasetTags(deleteDatasetTagsRequest);
      LOGGER.info("Tags deleted in server : " + response.getDataset().getTagsList());
      assertTrue(response.getDataset().getTagsList().size() <= 1);
      assertNotEquals(
          "Dataset date_updated field not update on database",
          dataset.getTimeUpdated(),
          response.getDataset().getTimeUpdated());
      dataset = response.getDataset();

      if (response.getDataset().getTagsList().size() > 0) {
        deleteDatasetTagsRequest =
            DeleteDatasetTags.newBuilder().setId(dataset.getId()).setDeleteAll(true).build();

        response = datasetServiceStub.deleteDatasetTags(deleteDatasetTagsRequest);
        LOGGER.info("Tags deleted in server : " + response.getDataset().getTagsList());
        assertEquals(0, response.getDataset().getTagsList().size());
        assertNotEquals(
            "Dataset date_updated field not update on database",
            dataset.getTimeUpdated(),
            response.getDataset().getTimeUpdated());
      }

      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());

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

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

    DeleteDatasetTags deleteDatasetTagsRequest = DeleteDatasetTags.newBuilder().build();

    try {
      datasetServiceStub.deleteDatasetTags(deleteDatasetTagsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    deleteDatasetTagsRequest =
        DeleteDatasetTags.newBuilder().setId(dataset.getId()).setDeleteAll(true).build();

    DeleteDatasetTags.Response response =
        datasetServiceStub.deleteDatasetTags(deleteDatasetTagsRequest);
    LOGGER.info("Tags deleted in server : " + response.getDataset().getTagsList());
    assertEquals(0, response.getDataset().getTagsList().size());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Delete Dataset tags Negative test stop................................");
  }

  @Test
  public void addDatasetAttributes() {
    LOGGER.info("Add Dataset Attributes test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

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
            .setId(dataset.getId())
            .addAllAttributes(attributeList)
            .build();

    AddDatasetAttributes.Response response =
        datasetServiceStub.addDatasetAttributes(addDatasetAttributesRequest);
    LOGGER.info("Added Dataset Attributes: \n" + response.getDataset());
    assertTrue(response.getDataset().getAttributesList().containsAll(attributeList));
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset.getTimeUpdated(),
        response.getDataset().getTimeUpdated());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Add Dataset Attributes test stop................................");
  }

  @Test
  public void addDatasetAttributesNegativeTest() {
    LOGGER.info("Add Dataset Attributes Negative test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

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

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Add Dataset Attributes Negative test stop................................");
  }

  @Test
  public void updateDatasetAttributes() {
    LOGGER.info("Update Dataset Attributes test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    List<KeyValue> attributes = dataset.getAttributesList();
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
        UpdateDatasetAttributes.newBuilder().setId(dataset.getId()).setAttribute(keyValue).build();

    UpdateDatasetAttributes.Response response =
        datasetServiceStub.updateDatasetAttributes(updateDatasetAttributesRequest);
    LOGGER.info("Updated Dataset : \n" + response.getDataset());
    assertTrue(response.getDataset().getAttributesList().contains(keyValue));
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset = response.getDataset();

    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    keyValue =
        KeyValue.newBuilder()
            .setKey(attributes.get(1).getKey())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build();
    updateDatasetAttributesRequest =
        UpdateDatasetAttributes.newBuilder().setId(dataset.getId()).setAttribute(keyValue).build();

    response = datasetServiceStub.updateDatasetAttributes(updateDatasetAttributesRequest);
    LOGGER.info("Updated Dataset : \n" + response.getDataset());
    assertTrue(response.getDataset().getAttributesList().contains(keyValue));
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset = response.getDataset();

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
        UpdateDatasetAttributes.newBuilder().setId(dataset.getId()).setAttribute(keyValue).build();

    response = datasetServiceStub.updateDatasetAttributes(updateDatasetAttributesRequest);
    LOGGER.info("Updated Dataset : \n" + response.getDataset());
    assertTrue(response.getDataset().getAttributesList().contains(keyValue));
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset.getTimeUpdated(),
        response.getDataset().getTimeUpdated());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Update Dataset Attributes test stop................................");
  }

  @Test
  public void updateDatasetAttributesNegativeTest() {
    LOGGER.info("Update Dataset Attributes Negative test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_n_sprt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    List<KeyValue> attributes = dataset.getAttributesList();
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
            .setId("sfds")
            .setAttribute(dataset.getAttributesList().get(0))
            .build();
    try {
      datasetServiceStub.updateDatasetAttributes(updateDatasetAttributesRequest);
      fail();
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    updateDatasetAttributesRequest =
        UpdateDatasetAttributes.newBuilder().setId(dataset.getId()).clearAttribute().build();

    try {
      datasetServiceStub.updateDatasetAttributes(updateDatasetAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Update Dataset Attributes Negative test stop................................");
  }

  @Test
  public void getDatasetAttributes() {
    LOGGER.info("Get Dataset Attributes test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_f_apt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    List<KeyValue> attributes = dataset.getAttributesList();
    LOGGER.info("Attributes size : " + attributes.size());

    if (attributes.size() == 0) {
      LOGGER.warn("Dataset Attributes not found in database ");
      fail();
      return;
    }

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

    GetAttributes getDatasetAttributesRequest =
        GetAttributes.newBuilder().setId(dataset.getId()).addAllAttributeKeys(keys).build();

    GetAttributes.Response response =
        datasetServiceStub.getDatasetAttributes(getDatasetAttributesRequest);
    LOGGER.info(response.getAttributesList().toString());
    assertEquals(keys.size(), response.getAttributesList().size());

    getDatasetAttributesRequest =
        GetAttributes.newBuilder().setId(dataset.getId()).setGetAll(true).build();

    response = datasetServiceStub.getDatasetAttributes(getDatasetAttributesRequest);
    LOGGER.info(response.getAttributesList().toString());
    assertEquals(dataset.getAttributesList().size(), response.getAttributesList().size());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Get Dataset Attributes test stop................................");
  }

  @Test
  public void getDatasetAttributesNegativeTest() {
    LOGGER.info("Get Dataset Attributes Negative test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

    GetAttributes getDatasetAttributesRequest = GetAttributes.newBuilder().build();

    try {
      datasetServiceStub.getDatasetAttributes(getDatasetAttributesRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getDatasetAttributesRequest =
        GetAttributes.newBuilder().setId("jfhdsjfhdsfjk").setGetAll(true).build();
    try {
      datasetServiceStub.getDatasetAttributes(getDatasetAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      checkEqualsAssert(ex);
    }

    LOGGER.info("Get Dataset Attributes Negative test stop................................");
  }

  @Test
  public void deleteDatasetAttributesTest() {
    LOGGER.info("Delete Dataset Attributes test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_iiidpat");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());
    LOGGER.info("Dataset created successfully");

    List<KeyValue> attributes = dataset.getAttributesList();
    LOGGER.info("Attributes size : " + attributes.size());
    assertEquals(
        "Attribute list size not match with expected attribute list size", 3, attributes.size());
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
            .setId(dataset.getId())
            .addAllAttributeKeys(keys)
            .build();

    DeleteDatasetAttributes.Response response =
        datasetServiceStub.deleteDatasetAttributes(deleteDatasetAttributesRequest);
    LOGGER.info("Attributes deleted in server : " + response.getDataset());
    assertEquals(1, response.getDataset().getAttributesList().size());
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset.getTimeUpdated(),
        response.getDataset().getTimeUpdated());
    dataset = response.getDataset();

    if (response.getDataset().getAttributesList().size() != 0) {
      deleteDatasetAttributesRequest =
          DeleteDatasetAttributes.newBuilder().setId(dataset.getId()).setDeleteAll(true).build();
      response = datasetServiceStub.deleteDatasetAttributes(deleteDatasetAttributesRequest);
      LOGGER.info(
          "All the Attributes deleted from server. Attributes count : "
              + response.getDataset().getAttributesCount());
      assertEquals(0, response.getDataset().getAttributesList().size());
      assertNotEquals(
          "Dataset date_updated field not update on database",
          dataset.getTimeUpdated(),
          response.getDataset().getTimeUpdated());
    }

    // Delete all data related to dataset
    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Delete Dataset Attributes test stop................................");
  }

  @Test
  public void deleteDatasetAttributesNegativeTest() {
    LOGGER.info("Delete Dataset Attributes Negative test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);

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

    // Create dataset
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_f_apt");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    deleteDatasetAttributesRequest =
        DeleteDatasetAttributes.newBuilder().setId(dataset.getId()).setDeleteAll(true).build();
    DeleteDatasetAttributes.Response response =
        datasetServiceStub.deleteDatasetAttributes(deleteDatasetAttributesRequest);
    LOGGER.info(
        "All the Attributes deleted from server. Attributes count : "
            + response.getDataset().getAttributesCount());
    assertEquals(0, response.getDataset().getAttributesList().size());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Delete Dataset Attributes Negative test stop................................");
  }

  @Test
  public void setDatasetVisibility() {
    LOGGER.info("Set Dataset visibility test start................................");

    DatasetServiceBlockingStub datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);
    // Create public dataset
    // Public dataset 1
    CreateDataset createDatasetRequest = getDatasetRequest("dataset_appctct");
    createDatasetRequest =
        createDatasetRequest.toBuilder().setDatasetVisibility(DatasetVisibility.PUBLIC).build();
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());
    assertEquals(
        "Dataset visibility not match with expected dataset visibility",
        DatasetVisibility.PUBLIC,
        dataset.getDatasetVisibility());
    LOGGER.info("Dataset created successfully");

    SetDatasetVisibilty setDatasetVisibilty =
        SetDatasetVisibilty.newBuilder()
            .setId(dataset.getId())
            .setDatasetVisibility(DatasetVisibility.PRIVATE)
            .build();
    SetDatasetVisibilty.Response response =
        datasetServiceStub.setDatasetVisibility(setDatasetVisibilty);
    Dataset visibilityDataset = response.getDataset();
    assertEquals(
        "Dataset name not match with expected dataset name",
        dataset.getName(),
        visibilityDataset.getName());
    assertEquals(
        "Dataset visibility not match with updated dataset visibility",
        DatasetVisibility.PRIVATE,
        visibilityDataset.getDatasetVisibility());
    LOGGER.info("Set dataset visibility successfully");
    assertNotEquals(
        "Dataset date_updated field not update on database",
        dataset.getTimeUpdated(),
        response.getDataset().getTimeUpdated());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Set Dataset visibility test stop................................");
  }

  @Test
  public void batchDeleteDatasetsTest() {
    LOGGER.info("batch delete Dataset test start................................");

    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    List<String> datasetIds = new ArrayList<>();
    CreateDataset createDatasetRequest = getDatasetRequest("rental_TEXT_train_data_1.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    datasetIds.add(createDatasetResponse.getDataset().getId());
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());

    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        createDatasetResponse.getDataset().getName());

    createDatasetRequest = getDatasetRequest("rental_TEXT_train_data_2.csv");
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    datasetIds.add(createDatasetResponse.getDataset().getId());
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());

    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        createDatasetResponse.getDataset().getName());

    DeleteDatasets deleteDatasets = DeleteDatasets.newBuilder().addAllIds(datasetIds).build();
    DeleteDatasets.Response deleteDatasetsResponse =
        datasetServiceStub.deleteDatasets(deleteDatasets);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetsResponse.toString());
    assertTrue(deleteDatasetsResponse.getStatus());

    LOGGER.info("batch delete Dataset test stop................................");
  }

  @Test
  public void getLastExperimentByDataset() {
    LOGGER.info("Get last experiment by dataset test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();
    ExperimentRunTest experimentRunTest = new ExperimentRunTest();
    DatasetVersionTest datasetVersionTest = new DatasetVersionTest();

    ProjectServiceGrpc.ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);

    // Create project
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_1");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project.getName());

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_1");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment1 = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment1.getName());

    createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_2");
    createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment2 = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment2.getName());

    CreateExperimentRun createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment1.getId(), "ExperimentRun_1");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment2.getId(), "ExperimentRun_2");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun2 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun2.getName());

    DatasetTest datasetTest = new DatasetTest();
    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
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
    DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion1);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion1.getDatasetId());
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        1,
        datasetVersion1.getVersion());

    createDatasetVersionRequest = datasetVersionTest.getDatasetVersionRequest(dataset.getId());
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .setRawDatasetVersionInfo(
                RawDatasetVersionInfo.newBuilder().setSize(1).setNumRecords(1).build())
            .setDatasetType(DatasetTypeEnum.DatasetType.RAW)
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion2 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion2);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion2.getDatasetId());
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        2,
        datasetVersion2.getVersion());

    Artifact artifact =
        Artifact.newBuilder()
            .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactTypeEnum.ArtifactType.DATA)
            .setLinkedArtifactId(datasetVersion2.getId())
            .build();

    LogDataset logDatasetRequest =
        LogDataset.newBuilder().setId(experimentRun2.getId()).setDataset(artifact).build();

    LogDataset.Response response = experimentRunServiceStub.logDataset(logDatasetRequest);

    LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());
    assertTrue(
        "Experiment dataset not match with expected dataset",
        response.getExperimentRun().getDatasetsList().contains(artifact));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun2.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());

    artifact =
        Artifact.newBuilder()
            .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactTypeEnum.ArtifactType.DATA)
            .setLinkedArtifactId(datasetVersion1.getId())
            .build();

    logDatasetRequest =
        LogDataset.newBuilder().setId(experimentRun.getId()).setDataset(artifact).build();

    response = experimentRunServiceStub.logDataset(logDatasetRequest);

    LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());
    assertTrue(
        "Experiment dataset not match with expected dataset",
        response.getExperimentRun().getDatasetsList().contains(artifact));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());

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
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get last experiment by dataset test stop................................");
  }

  @Test
  public void getExperimentRunByDataset() {
    LOGGER.info("Get experimentRun by dataset test start................................");

    ProjectTest projectTest = new ProjectTest();
    ExperimentTest experimentTest = new ExperimentTest();
    ExperimentRunTest experimentRunTest = new ExperimentRunTest();
    DatasetVersionTest datasetVersionTest = new DatasetVersionTest();

    ProjectServiceGrpc.ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);
    ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);
    ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);

    // Create project
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("project_1");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    Project project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected project name",
        createProjectRequest.getName(),
        project.getName());

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_1");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment1 = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment1.getName());

    createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_2");
    createExperimentResponse = experimentServiceStub.createExperiment(createExperimentRequest);
    Experiment experiment2 = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment2.getName());

    CreateExperimentRun createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment1.getId(), "ExperimentRun_1");
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());

    createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment2.getId(), "ExperimentRun_2");
    createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    ExperimentRun experimentRun2 = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun2.getName());

    DatasetTest datasetTest = new DatasetTest();
    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
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
    DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion1);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion1.getDatasetId());
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        1,
        datasetVersion1.getVersion());

    createDatasetVersionRequest = datasetVersionTest.getDatasetVersionRequest(dataset.getId());
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .setRawDatasetVersionInfo(
                RawDatasetVersionInfo.newBuilder().setSize(1).setNumRecords(1).build())
            .setDatasetType(DatasetTypeEnum.DatasetType.RAW)
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion2 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion2);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion2.getDatasetId());
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        2,
        datasetVersion2.getVersion());

    Artifact artifact =
        Artifact.newBuilder()
            .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactTypeEnum.ArtifactType.DATA)
            .setLinkedArtifactId(datasetVersion2.getId())
            .build();

    LogDataset logDatasetRequest =
        LogDataset.newBuilder().setId(experimentRun2.getId()).setDataset(artifact).build();

    LogDataset.Response response = experimentRunServiceStub.logDataset(logDatasetRequest);

    LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());
    assertTrue(
        "Experiment dataset not match with expected dataset",
        response.getExperimentRun().getDatasetsList().contains(artifact));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun2.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());

    artifact =
        Artifact.newBuilder()
            .setKey("Google Pay datasets " + Calendar.getInstance().getTimeInMillis())
            .setPath("This is new added data artifact type in Google Pay datasets")
            .setArtifactType(ArtifactTypeEnum.ArtifactType.DATA)
            .setLinkedArtifactId(datasetVersion1.getId())
            .build();

    logDatasetRequest =
        LogDataset.newBuilder().setId(experimentRun.getId()).setDataset(artifact).build();

    response = experimentRunServiceStub.logDataset(logDatasetRequest);

    LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());
    assertTrue(
        "Experiment dataset not match with expected dataset",
        response.getExperimentRun().getDatasetsList().contains(artifact));

    assertNotEquals(
        "ExperimentRun date_updated field not update on database",
        experimentRun.getDateUpdated(),
        response.getExperimentRun().getDateUpdated());

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
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    LOGGER.info("Get experimentRun by dataset test stop................................");
  }
}
