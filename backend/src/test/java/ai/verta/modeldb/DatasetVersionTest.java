package ai.verta.modeldb;

import static ai.verta.modeldb.CollaboratorTest.addCollaboratorRequestUser;
import static org.junit.Assert.*;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.KeyValue;
import ai.verta.common.ValueTypeEnum.ValueType;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.CollaboratorServiceGrpc;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import io.grpc.ManagedChannel;
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
public class DatasetVersionTest {

  private static final Logger LOGGER = LogManager.getLogger(CommentTest.class);
  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;
  private ManagedChannel client2Channel = null;
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
  }

  @Before
  public void initializeChannel() throws IOException {
    grpcCleanup.register(serverBuilder.build().start());
    channel = grpcCleanup.register(channelBuilder.maxInboundMessageSize(1024).build());
    client2Channel =
        grpcCleanup.register(client2ChannelBuilder.maxInboundMessageSize(1024).build());
  }

  public CreateDatasetVersion getDatasetVersionRequest(String datasetId) {

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

    return CreateDatasetVersion.newBuilder()
        .setDatasetId(datasetId)
        .setDescription("this is the description of datsetVersion")
        .setDatasetType(DatasetTypeEnum.DatasetType.RAW)
        .setDatasetVersionVisibility(DatasetVisibilityEnum.DatasetVisibility.PRIVATE)
        .addTags("DatasetVersion_tag_x")
        .addTags("DatasetVersion_tag_y")
        .addAllAttributes(attributeList)
        .build();
  }

  @Test
  public void createDatasetVersionTest() {

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    long version = 1L;
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
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
        version,
        datasetVersion1.getVersion());

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
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
        ++version,
        datasetVersion2.getVersion());

    GetDatasetById getDatasetById = GetDatasetById.newBuilder().setId(dataset.getId()).build();
    GetDatasetById.Response getDatasetByIdResponse =
        datasetServiceStub.getDatasetById(getDatasetById);
    assertEquals(
        "Dataset Id not match with expected dataset ID",
        dataset.getId(),
        getDatasetByIdResponse.getDataset().getId());
    assertNotEquals(
        "Dataset updated time not match with expected dataset updated time",
        dataset.getTimeUpdated(),
        getDatasetByIdResponse.getDataset().getTimeUpdated());

    DeleteDatasetVersion deleteDatasetVersionRequest =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion1.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersionRequest);
    LOGGER.info("DeleteDatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    deleteDatasetVersionRequest =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion2.getId()).build();
    deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersionRequest);
    LOGGER.info("DeleteDatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());
  }

  @Test
  public void getAllDatasetVersionsByDatasetIdTest() {

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    long version = 1L;
    Map<String, DatasetVersion> datasetVersionMap = new HashMap<>();
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionMap.put(datasetVersion1.getId(), datasetVersion1);
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion1);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion1.getDatasetId());
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        version,
        datasetVersion1.getVersion());

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
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
    datasetVersionMap.put(datasetVersion2.getId(), datasetVersion2);
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion2);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion2.getDatasetId());
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        ++version,
        datasetVersion2.getVersion());

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion3 = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionMap.put(datasetVersion3.getId(), datasetVersion3);
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion3);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion3.getDatasetId());
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        ++version,
        datasetVersion3.getVersion());

    int pageLimit = 1;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      GetAllDatasetVersionsByDatasetId getAllDatasetVersionsByDatasetIdRequest =
          GetAllDatasetVersionsByDatasetId.newBuilder()
              .setDatasetId(dataset.getId())
              .setPageNumber(pageNumber)
              .setPageLimit(pageLimit)
              .setAscending(false)
              .setSortKey(ModelDBConstants.VERSION)
              .build();

      GetAllDatasetVersionsByDatasetId.Response getAllDatasetVersionsByDatasetIdResponse =
          datasetVersionServiceStub.getAllDatasetVersionsByDatasetId(
              getAllDatasetVersionsByDatasetIdRequest);

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          getAllDatasetVersionsByDatasetIdResponse.getTotalRecords());

      if (getAllDatasetVersionsByDatasetIdResponse.getDatasetVersionsList() != null
          && getAllDatasetVersionsByDatasetIdResponse.getDatasetVersionsList().size() > 0) {
        isExpectedResultFound = true;
        LOGGER.info(
            "GetAllDataset Response : "
                + getAllDatasetVersionsByDatasetIdResponse.getDatasetVersionsCount());
        for (DatasetVersion datasetVersion :
            getAllDatasetVersionsByDatasetIdResponse.getDatasetVersionsList()) {
          assertEquals(
              "DatasetVersion not match with expected DatasetVersion",
              datasetVersionMap.get(datasetVersion.getId()),
              datasetVersion);
        }

        if (pageNumber == 1) {
          assertEquals(
              "DatasetVersion not match with expected DatasetVersion",
              datasetVersion3,
              datasetVersionMap.get(
                  getAllDatasetVersionsByDatasetIdResponse.getDatasetVersions(0).getId()));
        } else if (pageNumber == 3) {
          assertEquals(
              "DatasetVersion not match with expected DatasetVersion",
              datasetVersion1,
              datasetVersionMap.get(
                  getAllDatasetVersionsByDatasetIdResponse.getDatasetVersions(0).getId()));
        }

      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More DatasetVersion not found in database");
          assertTrue(true);
        } else {
          fail("Expected DatasetVersion not found in response");
        }
        break;
      }
    }

    GetAllDatasetVersionsByDatasetId getAllDatasetVersionsByDatasetIdRequest =
        GetAllDatasetVersionsByDatasetId.newBuilder().setDatasetId(dataset.getId()).build();
    GetAllDatasetVersionsByDatasetId.Response getAllDatasetVersionsByDatasetIdResponse =
        datasetVersionServiceStub.getAllDatasetVersionsByDatasetId(
            getAllDatasetVersionsByDatasetIdRequest);
    assertEquals(
        "DatasetVersions count not match with expected DatasetVersion count",
        datasetVersionMap.size(),
        getAllDatasetVersionsByDatasetIdResponse.getTotalRecords());

    for (DatasetVersion datasetVersion :
        getAllDatasetVersionsByDatasetIdResponse.getDatasetVersionsList()) {
      assertEquals(
          "DatasetVersion not match with expected DatasetVersion",
          datasetVersionMap.get(datasetVersion.getId()),
          datasetVersion);
    }

    for (String datasetVersionId : datasetVersionMap.keySet()) {
      DeleteDatasetVersion deleteDatasetVersionRequest =
          DeleteDatasetVersion.newBuilder().setId(datasetVersionId).build();
      DeleteDatasetVersion.Response deleteDatasetVersionResponse =
          datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersionRequest);
      LOGGER.info("DeleteDatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionResponse.toString());
      assertTrue(deleteDatasetVersionResponse.getStatus());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());
  }

  @Test
  public void getLatestDatasetVersionByDatasetId() {

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    long version = 1L;
    Map<String, DatasetVersion> datasetVersionMap = new HashMap<>();
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionMap.put(datasetVersion1.getId(), datasetVersion1);
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion1);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion1.getDatasetId());
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        version,
        datasetVersion1.getVersion());

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
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
    datasetVersionMap.put(datasetVersion2.getId(), datasetVersion2);
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion2);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion2.getDatasetId());
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        ++version,
        datasetVersion2.getVersion());

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion3 = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionMap.put(datasetVersion3.getId(), datasetVersion3);
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion3);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion3.getDatasetId());
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        ++version,
        datasetVersion3.getVersion());

    GetLatestDatasetVersionByDatasetId getLatestDatasetVersionByDatasetIdRequest =
        GetLatestDatasetVersionByDatasetId.newBuilder().setDatasetId(dataset.getId()).build();
    GetLatestDatasetVersionByDatasetId.Response getLatestDatasetVersionByDatasetIdResponse =
        datasetVersionServiceStub.getLatestDatasetVersionByDatasetId(
            getLatestDatasetVersionByDatasetIdRequest);
    assertEquals(
        "DatasetVersions not match with expected DatasetVersion",
        datasetVersion3,
        getLatestDatasetVersionByDatasetIdResponse.getDatasetVersion());

    getLatestDatasetVersionByDatasetIdRequest =
        GetLatestDatasetVersionByDatasetId.newBuilder()
            .setDatasetId(dataset.getId())
            .setAscending(true)
            .build();
    getLatestDatasetVersionByDatasetIdResponse =
        datasetVersionServiceStub.getLatestDatasetVersionByDatasetId(
            getLatestDatasetVersionByDatasetIdRequest);
    assertEquals(
        "DatasetVersions not match with expected DatasetVersion",
        datasetVersion1,
        getLatestDatasetVersionByDatasetIdResponse.getDatasetVersion());

    getLatestDatasetVersionByDatasetIdRequest =
        GetLatestDatasetVersionByDatasetId.newBuilder()
            .setDatasetId(dataset.getId())
            .setSortKey(ModelDBConstants.TIME_UPDATED)
            .build();
    getLatestDatasetVersionByDatasetIdResponse =
        datasetVersionServiceStub.getLatestDatasetVersionByDatasetId(
            getLatestDatasetVersionByDatasetIdRequest);
    assertEquals(
        "DatasetVersions not match with expected DatasetVersion",
        datasetVersion3,
        getLatestDatasetVersionByDatasetIdResponse.getDatasetVersion());

    for (DatasetVersion datasetVersion : datasetVersionMap.values()) {

      DeleteDatasetVersion deleteDatasetVersionRequest =
          DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
      DeleteDatasetVersion.Response deleteDatasetVersionResponse =
          datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersionRequest);
      LOGGER.info("DeleteDatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionResponse.toString());
      assertTrue(deleteDatasetVersionResponse.getStatus());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());
  }

  @Test
  public void getDatasetVersionByIdTest() {

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    long version = 1L;
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion.getDatasetId());
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        version,
        datasetVersion.getVersion());

    GetDatasetVersionById getDatasetVersionByIdRequest =
        GetDatasetVersionById.newBuilder().setId(datasetVersion.getId()).build();
    GetDatasetVersionById.Response getDatasetVersionByIdResponse =
        datasetVersionServiceStub.getDatasetVersionById(getDatasetVersionByIdRequest);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        getDatasetVersionByIdResponse.getDatasetVersion().getDatasetId());
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        datasetVersion.getVersion(),
        getDatasetVersionByIdResponse.getDatasetVersion().getVersion());

    DeleteDatasetVersion deleteDatasetVersionRequest =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersionRequest);
    LOGGER.info("DeleteDatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());
  }

  @Test
  public void updateDatasetVersionDescription() {
    LOGGER.info("Update DatasetVersion Description test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    long version = 1L;
    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        version,
        datasetVersion.getVersion());

    UpdateDatasetVersionDescription updateDescriptionRequest =
        UpdateDatasetVersionDescription.newBuilder()
            .setId(datasetVersion.getId())
            .setDescription("DatasetVersion Description Update 1")
            .build();

    UpdateDatasetVersionDescription.Response response =
        datasetVersionServiceStub.updateDatasetVersionDescription(updateDescriptionRequest);
    LOGGER.info("UpdateDatasetVersionDescription Response : " + response.getDatasetVersion());
    assertEquals(
        "DatasetVersion description not match with expected datasetVersion description",
        updateDescriptionRequest.getDescription(),
        response.getDatasetVersion().getDescription());
    assertNotEquals(
        "DatasetVersion date_updated field not update on database",
        datasetVersion.getTimeUpdated(),
        response.getDatasetVersion().getTimeUpdated());
    datasetVersion = response.getDatasetVersion();

    updateDescriptionRequest =
        UpdateDatasetVersionDescription.newBuilder()
            .setId(datasetVersion.getId())
            .setDescription("DatasetVersion Description Update 2")
            .build();

    response = datasetVersionServiceStub.updateDatasetVersionDescription(updateDescriptionRequest);
    LOGGER.info("UpdateDatasetVersionDescription Response : " + response.getDatasetVersion());
    assertEquals(
        "DatasetVersion description not match with expected datasetVersion description",
        updateDescriptionRequest.getDescription(),
        response.getDatasetVersion().getDescription());
    assertNotEquals(
        "DatasetVersion date_updated field not update on database",
        datasetVersion.getTimeUpdated(),
        response.getDatasetVersion().getTimeUpdated());

    GetDatasetById getDatasetById = GetDatasetById.newBuilder().setId(dataset.getId()).build();
    GetDatasetById.Response getDatasetByIdResponse =
        datasetServiceStub.getDatasetById(getDatasetById);
    assertEquals(
        "Dataset Id not match with expected dataset ID",
        dataset.getId(),
        getDatasetByIdResponse.getDataset().getId());
    assertNotEquals(
        "Dataset updated time not match with expected dataset updated time",
        dataset.getTimeUpdated(),
        getDatasetByIdResponse.getDataset().getTimeUpdated());

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Update DatasetVersion Description test stop................................");
  }

  @Test
  public void updateDatasetVersionDescriptionNegativeTest() {
    LOGGER.info(
        "Update DatasetVersion Description Negative test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    UpdateDatasetVersionDescription updateDescriptionRequest =
        UpdateDatasetVersionDescription.newBuilder()
            .setDescription(
                "This is update from UpdateDatasetVersionDescription."
                    + Calendar.getInstance().getTimeInMillis())
            .build();

    try {
      datasetVersionServiceStub.updateDatasetVersionDescription(updateDescriptionRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Update DatasetVersion Description test stop................................");
  }

  @Test
  public void addDatasetVersionTags() {
    LOGGER.info("Add DatasetVersion Tags test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());

    List<String> tagsList = new ArrayList<>();
    tagsList.add("Add Test Tag1");
    tagsList.add("Add Test Tag2");
    AddDatasetVersionTags addDatasetVersionTagsRequest =
        AddDatasetVersionTags.newBuilder()
            .setId(datasetVersion.getId())
            .addAllTags(tagsList)
            .build();

    AddDatasetVersionTags.Response response =
        datasetVersionServiceStub.addDatasetVersionTags(addDatasetVersionTagsRequest);

    DatasetVersion checkDatasetVersion = response.getDatasetVersion();
    assertEquals(4, checkDatasetVersion.getTagsCount());
    assertEquals(4, checkDatasetVersion.getTagsList().size());
    assertNotEquals(
        "DatasetVersion date_updated field not update on database",
        datasetVersion.getTimeUpdated(),
        checkDatasetVersion.getTimeUpdated());

    tagsList = new ArrayList<>();
    tagsList.add("Add Test Tag3");
    tagsList.add("Add Test Tag2");
    addDatasetVersionTagsRequest =
        AddDatasetVersionTags.newBuilder()
            .setId(datasetVersion.getId())
            .addAllTags(tagsList)
            .build();

    response = datasetVersionServiceStub.addDatasetVersionTags(addDatasetVersionTagsRequest);

    assertNotEquals(
        "DatasetVersion date_updated field not update on database",
        checkDatasetVersion.getTimeUpdated(),
        response.getDatasetVersion().getTimeUpdated());

    checkDatasetVersion = response.getDatasetVersion();
    assertEquals(5, checkDatasetVersion.getTagsCount());
    assertEquals(5, checkDatasetVersion.getTagsList().size());

    try {
      String tag52 = "Human Activity Recognition using Smartphone DatasetVersion";
      addDatasetVersionTagsRequest =
          AddDatasetVersionTags.newBuilder().setId(datasetVersion.getId()).addTags(tag52).build();
      datasetVersionServiceStub.addDatasetVersionTags(addDatasetVersionTagsRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    GetDatasetById getDatasetById = GetDatasetById.newBuilder().setId(dataset.getId()).build();
    GetDatasetById.Response getDatasetByIdResponse =
        datasetServiceStub.getDatasetById(getDatasetById);
    assertEquals(
        "Dataset Id not match with expected dataset ID",
        dataset.getId(),
        getDatasetByIdResponse.getDataset().getId());
    assertNotEquals(
        "Dataset updated time not match with expected dataset updated time",
        dataset.getTimeUpdated(),
        getDatasetByIdResponse.getDataset().getTimeUpdated());

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Add DatasetVersion tags test stop................................");
  }

  @Test
  public void addDatasetVersionNegativeTags() {
    LOGGER.info("Add DatasetVersion Tags Negative test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    List<String> tagsList = new ArrayList<>();
    tagsList.add("Add Test Tag " + Calendar.getInstance().getTimeInMillis());
    tagsList.add("Add Test Tag 2 " + Calendar.getInstance().getTimeInMillis());
    AddDatasetVersionTags addDatasetVersionTagsRequest =
        AddDatasetVersionTags.newBuilder().addAllTags(tagsList).build();

    try {
      datasetVersionServiceStub.addDatasetVersionTags(addDatasetVersionTagsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }
    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());

    addDatasetVersionTagsRequest =
        AddDatasetVersionTags.newBuilder()
            .setId("sdasd")
            .addAllTags(datasetVersion.getTagsList())
            .build();

    try {
      datasetVersionServiceStub.addDatasetVersionTags(addDatasetVersionTagsRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Add DatasetVersion tags Negative test stop................................");
  }

  @Test
  public void getDatasetVersionTags() {
    LOGGER.info("Get DatasetVersion Tags test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());

    GetTags deleteDatasetVersionTagsRequest =
        GetTags.newBuilder().setId(datasetVersion.getId()).build();
    GetTags.Response response =
        datasetVersionServiceStub.getDatasetVersionTags(deleteDatasetVersionTagsRequest);
    LOGGER.info("Tags deleted in server : " + response.getTagsList());
    assertTrue(datasetVersion.getTagsList().containsAll(response.getTagsList()));

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Get DatasetVersion tags test stop................................");
  }

  @Test
  public void getDatasetVersionTagsNegativeTest() {
    LOGGER.info("Get DatasetVersion Tags Negative test start................................");

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);

    GetTags deleteDatasetVersionTagsRequest = GetTags.newBuilder().build();

    try {
      datasetVersionServiceStub.getDatasetVersionTags(deleteDatasetVersionTagsRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("Get DatasetVersion tags Negative test stop................................");
  }

  @Test
  public void deleteDatasetVersionTags() {
    LOGGER.info("Delete DatasetVersion Tags test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());

    try {
      List<String> removableTags = datasetVersion.getTagsList();
      if (removableTags.size() == 0) {
        LOGGER.info("DatasetVersion Tags not found in database ");
        fail();
        return;
      }
      if (datasetVersion.getTagsList().size() > 1) {
        removableTags =
            datasetVersion.getTagsList().subList(0, datasetVersion.getTagsList().size() - 1);
      }
      DeleteDatasetVersionTags deleteDatasetVersionTagsRequest =
          DeleteDatasetVersionTags.newBuilder()
              .setId(datasetVersion.getId())
              .addAllTags(removableTags)
              .build();

      DeleteDatasetVersionTags.Response response =
          datasetVersionServiceStub.deleteDatasetVersionTags(deleteDatasetVersionTagsRequest);
      LOGGER.info("Tags deleted in server : " + response.getDatasetVersion().getTagsList());
      assertTrue(response.getDatasetVersion().getTagsList().size() <= 1);
      assertNotEquals(
          "DatasetVersion date_updated field not update on database",
          datasetVersion.getTimeUpdated(),
          response.getDatasetVersion().getTimeUpdated());
      datasetVersion = response.getDatasetVersion();

      if (response.getDatasetVersion().getTagsList().size() > 0) {
        deleteDatasetVersionTagsRequest =
            DeleteDatasetVersionTags.newBuilder()
                .setId(datasetVersion.getId())
                .setDeleteAll(true)
                .build();

        response =
            datasetVersionServiceStub.deleteDatasetVersionTags(deleteDatasetVersionTagsRequest);
        LOGGER.info("Tags deleted in server : " + response.getDatasetVersion().getTagsList());
        assertEquals(0, response.getDatasetVersion().getTagsList().size());
        assertNotEquals(
            "DatasetVersion date_updated field not update on database",
            datasetVersion.getTimeUpdated(),
            response.getDatasetVersion().getTimeUpdated());
      }

      DeleteDatasetVersion deleteDatasetVersion =
          DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
      DeleteDatasetVersion.Response deleteDatasetVersionResponse =
          datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
      LOGGER.info("DatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionResponse.toString());
      assertTrue(deleteDatasetVersionResponse.getStatus());

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

    LOGGER.info("Delete DatasetVersion tags test stop................................");
  }

  @Test
  public void deleteDatasetVersionTagsNegativeTest() {
    LOGGER.info("Delete DatasetVersion Tags Negative test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    DeleteDatasetVersionTags deleteDatasetVersionTagsRequest =
        DeleteDatasetVersionTags.newBuilder().build();

    try {
      datasetVersionServiceStub.deleteDatasetVersionTags(deleteDatasetVersionTagsRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());

    deleteDatasetVersionTagsRequest =
        DeleteDatasetVersionTags.newBuilder()
            .setId(datasetVersion.getId())
            .setDeleteAll(true)
            .build();

    DeleteDatasetVersionTags.Response response =
        datasetVersionServiceStub.deleteDatasetVersionTags(deleteDatasetVersionTagsRequest);
    LOGGER.info("Tags deleted in server : " + response.getDatasetVersion().getTagsList());
    assertEquals(0, response.getDatasetVersion().getTagsList().size());

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Delete DatasetVersion tags Negative test stop................................");
  }

  @Test
  public void addDatasetVersionAttributes() {
    LOGGER.info("Add DatasetVersion Attributes test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());

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

    AddDatasetVersionAttributes addDatasetVersionAttributesRequest =
        AddDatasetVersionAttributes.newBuilder()
            .setId(datasetVersion.getId())
            .addAllAttributes(attributeList)
            .build();

    AddDatasetVersionAttributes.Response response =
        datasetVersionServiceStub.addDatasetVersionAttributes(addDatasetVersionAttributesRequest);
    LOGGER.info("Added DatasetVersion Attributes: \n" + response.getDatasetVersion());
    assertTrue(response.getDatasetVersion().getAttributesList().containsAll(attributeList));
    assertNotEquals(
        "DatasetVersion date_updated field not update on database",
        datasetVersion.getTimeUpdated(),
        response.getDatasetVersion().getTimeUpdated());

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Add DatasetVersion Attributes test stop................................");
  }

  @Test
  public void addDatasetVersionAttributesNegativeTest() {
    LOGGER.info(
        "Add DatasetVersion Attributes Negative test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

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

    AddDatasetVersionAttributes addDatasetVersionAttributesRequest =
        AddDatasetVersionAttributes.newBuilder().addAllAttributes(attributeList).build();

    try {
      datasetVersionServiceStub.addDatasetVersionAttributes(addDatasetVersionAttributesRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Add DatasetVersion Attributes Negative test stop................................");
  }

  @Test
  public void updateDatasetVersionAttributes() {
    LOGGER.info("Update DatasetVersion Attributes test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());

    List<KeyValue> attributes = datasetVersion.getAttributesList();
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
    UpdateDatasetVersionAttributes updateDatasetVersionAttributesRequest =
        UpdateDatasetVersionAttributes.newBuilder()
            .setId(datasetVersion.getId())
            .setAttribute(keyValue)
            .build();

    UpdateDatasetVersionAttributes.Response response =
        datasetVersionServiceStub.updateDatasetVersionAttributes(
            updateDatasetVersionAttributesRequest);
    LOGGER.info("Updated DatasetVersion : \n" + response.getDatasetVersion());
    assertTrue(response.getDatasetVersion().getAttributesList().contains(keyValue));
    assertNotEquals(
        "DatasetVersion date_updated field not update on database",
        datasetVersion.getTimeUpdated(),
        response.getDatasetVersion().getTimeUpdated());
    datasetVersion = response.getDatasetVersion();

    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
    keyValue =
        KeyValue.newBuilder()
            .setKey(attributes.get(1).getKey())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build();
    updateDatasetVersionAttributesRequest =
        UpdateDatasetVersionAttributes.newBuilder()
            .setId(datasetVersion.getId())
            .setAttribute(keyValue)
            .build();

    response =
        datasetVersionServiceStub.updateDatasetVersionAttributes(
            updateDatasetVersionAttributesRequest);
    LOGGER.info("Updated DatasetVersion : \n" + response.getDatasetVersion());
    assertTrue(response.getDatasetVersion().getAttributesList().contains(keyValue));
    assertNotEquals(
        "DatasetVersion date_updated field not update on database",
        datasetVersion.getTimeUpdated(),
        response.getDatasetVersion().getTimeUpdated());
    datasetVersion = response.getDatasetVersion();

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
    updateDatasetVersionAttributesRequest =
        UpdateDatasetVersionAttributes.newBuilder()
            .setId(datasetVersion.getId())
            .setAttribute(keyValue)
            .build();

    response =
        datasetVersionServiceStub.updateDatasetVersionAttributes(
            updateDatasetVersionAttributesRequest);
    LOGGER.info("Updated DatasetVersion : \n" + response.getDatasetVersion());
    assertTrue(response.getDatasetVersion().getAttributesList().contains(keyValue));
    assertNotEquals(
        "DatasetVersion date_updated field not update on database",
        datasetVersion.getTimeUpdated(),
        response.getDatasetVersion().getTimeUpdated());

    GetDatasetById getDatasetById = GetDatasetById.newBuilder().setId(dataset.getId()).build();
    GetDatasetById.Response getDatasetByIdResponse =
        datasetServiceStub.getDatasetById(getDatasetById);
    assertEquals(
        "Dataset Id not match with expected dataset ID",
        dataset.getId(),
        getDatasetByIdResponse.getDataset().getId());
    assertNotEquals(
        "Dataset updated time not match with expected dataset updated time",
        dataset.getTimeUpdated(),
        getDatasetByIdResponse.getDataset().getTimeUpdated());

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Update DatasetVersion Attributes test stop................................");
  }

  @Test
  public void updateDatasetVersionAttributesNegativeTest() {
    LOGGER.info(
        "Update DatasetVersion Attributes Negative test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());

    List<KeyValue> attributes = datasetVersion.getAttributesList();
    Value stringValue = Value.newBuilder().setStringValue("attribute_updated_test_value").build();
    KeyValue keyValue =
        KeyValue.newBuilder()
            .setKey(attributes.get(0).getKey())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build();
    UpdateDatasetVersionAttributes updateDatasetVersionAttributesRequest =
        UpdateDatasetVersionAttributes.newBuilder().setAttribute(keyValue).build();

    try {
      datasetVersionServiceStub.updateDatasetVersionAttributes(
          updateDatasetVersionAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    updateDatasetVersionAttributesRequest =
        UpdateDatasetVersionAttributes.newBuilder()
            .setId("sfds")
            .setAttribute(datasetVersion.getAttributesList().get(0))
            .build();
    try {
      datasetVersionServiceStub.updateDatasetVersionAttributes(
          updateDatasetVersionAttributesRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }

    updateDatasetVersionAttributesRequest =
        UpdateDatasetVersionAttributes.newBuilder()
            .setId(datasetVersion.getId())
            .clearAttribute()
            .build();

    try {
      datasetVersionServiceStub.updateDatasetVersionAttributes(
          updateDatasetVersionAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info(
        "Update DatasetVersion Attributes Negative test stop................................");
  }

  @Test
  public void getDatasetVersionAttributes() {
    LOGGER.info("Get DatasetVersion Attributes test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());

    List<KeyValue> attributes = datasetVersion.getAttributesList();
    LOGGER.info("Attributes size : " + attributes.size());

    if (attributes.size() == 0) {
      LOGGER.warn("DatasetVersion Attributes not found in database ");
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

    GetAttributes getDatasetVersionAttributesRequest =
        GetAttributes.newBuilder().setId(datasetVersion.getId()).addAllAttributeKeys(keys).build();

    GetAttributes.Response response =
        datasetVersionServiceStub.getDatasetVersionAttributes(getDatasetVersionAttributesRequest);
    LOGGER.info(response.getAttributesList().toString());
    assertEquals(keys.size(), response.getAttributesList().size());

    getDatasetVersionAttributesRequest =
        GetAttributes.newBuilder().setId(datasetVersion.getId()).setGetAll(true).build();

    response =
        datasetVersionServiceStub.getDatasetVersionAttributes(getDatasetVersionAttributesRequest);
    LOGGER.info(response.getAttributesList().toString());
    assertEquals(datasetVersion.getAttributesList().size(), response.getAttributesList().size());

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Get DatasetVersion Attributes test stop................................");
  }

  @Test
  public void getDatasetVersionAttributesNegativeTest() {
    LOGGER.info(
        "Get DatasetVersion Attributes Negative test start................................");

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);

    GetAttributes getDatasetVersionAttributesRequest = GetAttributes.newBuilder().build();

    try {
      datasetVersionServiceStub.getDatasetVersionAttributes(getDatasetVersionAttributesRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getDatasetVersionAttributesRequest =
        GetAttributes.newBuilder().setId("jfhdsjfhdsfjk").setGetAll(true).build();
    try {
      datasetVersionServiceStub.getDatasetVersionAttributes(getDatasetVersionAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
    }

    LOGGER.info("Get DatasetVersion Attributes Negative test stop................................");
  }

  @Test
  public void deleteDatasetVersionAttributesTest() {
    LOGGER.info("Delete DatasetVersion Attributes test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());
    LOGGER.info("DatasetVersion created successfully");

    List<KeyValue> attributes = datasetVersion.getAttributesList();
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

    DeleteDatasetVersionAttributes deleteDatasetVersionAttributesRequest =
        DeleteDatasetVersionAttributes.newBuilder()
            .setId(datasetVersion.getId())
            .addAllAttributeKeys(keys)
            .build();

    DeleteDatasetVersionAttributes.Response response =
        datasetVersionServiceStub.deleteDatasetVersionAttributes(
            deleteDatasetVersionAttributesRequest);
    LOGGER.info("Attributes deleted in server : " + response.getDatasetVersion());
    assertEquals(1, response.getDatasetVersion().getAttributesList().size());
    assertNotEquals(
        "DatasetVersion date_updated field not update on database",
        datasetVersion.getTimeUpdated(),
        response.getDatasetVersion().getTimeUpdated());
    datasetVersion = response.getDatasetVersion();

    if (response.getDatasetVersion().getAttributesList().size() != 0) {
      deleteDatasetVersionAttributesRequest =
          DeleteDatasetVersionAttributes.newBuilder()
              .setId(datasetVersion.getId())
              .setDeleteAll(true)
              .build();
      response =
          datasetVersionServiceStub.deleteDatasetVersionAttributes(
              deleteDatasetVersionAttributesRequest);
      LOGGER.info(
          "All the Attributes deleted from server. Attributes count : "
              + response.getDatasetVersion().getAttributesCount());
      assertEquals(0, response.getDatasetVersion().getAttributesList().size());
      assertNotEquals(
          "DatasetVersion date_updated field not update on database",
          datasetVersion.getTimeUpdated(),
          response.getDatasetVersion().getTimeUpdated());
    }

    GetDatasetById getDatasetById = GetDatasetById.newBuilder().setId(dataset.getId()).build();
    GetDatasetById.Response getDatasetByIdResponse =
        datasetServiceStub.getDatasetById(getDatasetById);
    assertEquals(
        "Dataset Id not match with expected dataset ID",
        dataset.getId(),
        getDatasetByIdResponse.getDataset().getId());
    assertNotEquals(
        "Dataset updated time not match with expected dataset updated time",
        dataset.getTimeUpdated(),
        getDatasetByIdResponse.getDataset().getTimeUpdated());

    // Delete all data related to datasetVersion
    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Delete DatasetVersion Attributes test stop................................");
  }

  @Test
  public void deleteDatasetVersionAttributesNegativeTest() {
    LOGGER.info(
        "Delete DatasetVersion Attributes Negative test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    DeleteDatasetVersionAttributes deleteDatasetVersionAttributesRequest =
        DeleteDatasetVersionAttributes.newBuilder().build();

    try {
      datasetVersionServiceStub.deleteDatasetVersionAttributes(
          deleteDatasetVersionAttributesRequest);
      fail();
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());

    deleteDatasetVersionAttributesRequest =
        DeleteDatasetVersionAttributes.newBuilder()
            .setId(datasetVersion.getId())
            .setDeleteAll(true)
            .build();
    DeleteDatasetVersionAttributes.Response response =
        datasetVersionServiceStub.deleteDatasetVersionAttributes(
            deleteDatasetVersionAttributesRequest);
    LOGGER.info(
        "All the Attributes deleted from server. Attributes count : "
            + response.getDatasetVersion().getAttributesCount());
    assertEquals(0, response.getDatasetVersion().getAttributesList().size());

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info(
        "Delete DatasetVersion Attributes Negative test stop................................");
  }

  @Test
  public void setDatasetVersionVisibility() {
    LOGGER.info("Set DatasetVersion visibility test start................................");

    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    // Create public datasetVersion
    // Public datasetVersion 1
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .setDatasetVersionVisibility(DatasetVisibility.PUBLIC)
            .build();
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        1L,
        datasetVersion.getVersion());
    assertEquals(
        "DatasetVersion visibility not match with expected datasetVersion visibility",
        DatasetVisibility.PUBLIC,
        datasetVersion.getDatasetVersionVisibility());
    LOGGER.info("DatasetVersion created successfully");

    SetDatasetVersionVisibilty setDatasetVersionVisibilty =
        SetDatasetVersionVisibilty.newBuilder()
            .setId(datasetVersion.getId())
            .setDatasetVersionVisibility(DatasetVisibility.PRIVATE)
            .build();
    SetDatasetVersionVisibilty.Response response =
        datasetVersionServiceStub.setDatasetVersionVisibility(setDatasetVersionVisibilty);
    DatasetVersion visibilityDatasetVersion = response.getDatasetVersion();
    assertEquals(
        "DatasetVersion version not match with expected datasetVersion version",
        datasetVersion.getVersion(),
        visibilityDatasetVersion.getVersion());
    assertEquals(
        "DatasetVersion visibility not match with updated datasetVersion visibility",
        DatasetVisibility.PRIVATE,
        visibilityDatasetVersion.getDatasetVersionVisibility());
    LOGGER.info("Set datasetVersion visibility successfully");
    assertNotEquals(
        "DatasetVersion date_updated field not update on database",
        datasetVersion.getTimeUpdated(),
        response.getDatasetVersion().getTimeUpdated());

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());
    assertTrue(deleteDatasetVersionResponse.getStatus());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Set DatasetVersion visibility test stop................................");
  }

  @Test
  public void batchDeleteDatasetVersionTest() {
    LOGGER.info("batch delete DatasetVersion test start................................");
    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);
    assertEquals(
        "Dataset name not match with expected dataset name",
        createDatasetRequest.getName(),
        dataset.getName());

    long version = 1L;
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
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
        version,
        datasetVersion1.getVersion());

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
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
        ++version,
        datasetVersion2.getVersion());

    GetDatasetById getDatasetById = GetDatasetById.newBuilder().setId(dataset.getId()).build();
    GetDatasetById.Response getDatasetByIdResponse =
        datasetServiceStub.getDatasetById(getDatasetById);
    assertEquals(
        "Dataset Id not match with expected dataset ID",
        dataset.getId(),
        getDatasetByIdResponse.getDataset().getId());
    assertNotEquals(
        "Dataset updated time not match with expected dataset updated time",
        dataset.getTimeUpdated(),
        getDatasetByIdResponse.getDataset().getTimeUpdated());
    dataset = getDatasetByIdResponse.getDataset();

    List<String> datasetVersionIds = new ArrayList<>();
    datasetVersionIds.add(datasetVersion1.getId());
    datasetVersionIds.add(datasetVersion2.getId());

    DeleteDatasetVersions deleteDatasetVersionsRequest =
        DeleteDatasetVersions.newBuilder().addAllIds(datasetVersionIds).build();
    DeleteDatasetVersions.Response deleteDatasetVersionsResponse =
        datasetVersionServiceStub.deleteDatasetVersions(deleteDatasetVersionsRequest);
    LOGGER.info("DeleteDatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionsResponse.toString());
    assertTrue(deleteDatasetVersionsResponse.getStatus());

    getDatasetById = GetDatasetById.newBuilder().setId(dataset.getId()).build();
    getDatasetByIdResponse = datasetServiceStub.getDatasetById(getDatasetById);
    assertEquals(
        "Dataset Id not match with expected dataset ID",
        dataset.getId(),
        getDatasetByIdResponse.getDataset().getId());
    assertNotEquals(
        "Dataset updated time not match with expected dataset updated time",
        dataset.getTimeUpdated(),
        getDatasetByIdResponse.getDataset().getTimeUpdated());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());
    LOGGER.info("batch delete DatasetVersion test stop................................");
  }

  @Test
  public void deleteDatasetVersionTest() {
    LOGGER.info(
        "delete DatasetVersion by parent entities owner test start................................");
    DatasetTest datasetTest = new DatasetTest();

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);
    CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceStub =
        CollaboratorServiceGrpc.newBlockingStub(channel);
    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStubClient2 =
        DatasetVersionServiceGrpc.newBlockingStub(client2Channel);

    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    Dataset dataset = createDatasetResponse.getDataset();
    LOGGER.info("CreateDataset Response : \n" + dataset);

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      AddCollaboratorRequest addCollaboratorRequest =
          addCollaboratorRequestUser(
              dataset.getId(),
              authClientInterceptor.getClient2Email(),
              CollaboratorTypeEnum.CollaboratorType.READ_ONLY,
              "Please refer shared dataset for your invention");

      AddCollaboratorRequest.Response addCollaboratorResponse =
          collaboratorServiceStub.addOrUpdateDatasetCollaborator(addCollaboratorRequest);
      LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
      assertTrue(addCollaboratorResponse.getStatus());
    }

    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion1);

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
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

    List<String> datasetVersionIds = new ArrayList<>();
    datasetVersionIds.add(datasetVersion1.getId());
    datasetVersionIds.add(datasetVersion2.getId());

    DeleteDatasetVersions deleteDatasetVersionsRequest =
        DeleteDatasetVersions.newBuilder().addAllIds(datasetVersionIds).build();

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      try {
        datasetVersionServiceStubClient2.deleteDatasetVersions(deleteDatasetVersionsRequest);
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.warn(
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
      }

      AddCollaboratorRequest addCollaboratorRequest =
          addCollaboratorRequestUser(
              dataset.getId(),
              authClientInterceptor.getClient2Email(),
              CollaboratorTypeEnum.CollaboratorType.READ_WRITE,
              "Please refer shared dataset for your invention");

      AddCollaboratorRequest.Response addCollaboratorResponse =
          collaboratorServiceStub.addOrUpdateDatasetCollaborator(addCollaboratorRequest);
      LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
      assertTrue(addCollaboratorResponse.getStatus());

      DeleteDatasetVersions.Response deleteDatasetVersionsResponse =
          datasetVersionServiceStubClient2.deleteDatasetVersions(deleteDatasetVersionsRequest);
      LOGGER.info("DeleteDatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionsResponse.toString());
      assertTrue(deleteDatasetVersionsResponse.getStatus());
    } else {
      DeleteDatasetVersions.Response deleteDatasetVersionsResponse =
          datasetVersionServiceStub.deleteDatasetVersions(deleteDatasetVersionsRequest);
      LOGGER.info("DeleteDatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionsResponse.toString());
      assertTrue(deleteDatasetVersionsResponse.getStatus());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());
    LOGGER.info(
        "delete DatasetVersion by parent entities owner test stop................................");
  }
}
