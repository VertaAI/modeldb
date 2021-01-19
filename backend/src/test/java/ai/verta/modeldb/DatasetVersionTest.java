package ai.verta.modeldb;

import static ai.verta.modeldb.CollaboratorTest.addCollaboratorRequestUser;
import static org.junit.Assert.*;

import ai.verta.common.ArtifactPart;
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
import ai.verta.modeldb.cron_jobs.CronJobUtils;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.DatasetBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import ai.verta.modeldb.versioning.S3DatasetBlob;
import ai.verta.modeldb.versioning.S3DatasetComponentBlob;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.CollaboratorServiceGrpc;
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
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import org.junit.Ignore;
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
    deleteEntitiesCron =
        new DeleteEntitiesCron(authService, roleService, CronJobUtils.deleteEntitiesFrequency);
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
        .setDatasetVersionVisibility(DatasetVisibility.PRIVATE)
        .addTags("A")
        .addTags("A0")
        .addAllAttributes(attributeList)
        .setPathDatasetVersionInfo(
            PathDatasetVersionInfo.newBuilder()
                .setSize(2)
                .setLocationType(PathLocationTypeEnum.PathLocationType.S3_FILE_SYSTEM)
                .setBasePath("base_path/v1")
                .addDatasetPartInfos(
                    DatasetPartInfo.newBuilder()
                        .setSize(2)
                        .setLastModifiedAtSource(Calendar.getInstance().getTimeInMillis())
                        .setPath("test/versioning/test.txt")
                        .build())
                .build())
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

    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion1);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion1.getDatasetId());

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion2 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion2);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion2.getDatasetId());

    DeleteDatasetVersion deleteDatasetVersionRequest =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion1.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersionRequest);
    LOGGER.info("DeleteDatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());

    deleteDatasetVersionRequest =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion2.getId()).build();
    deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersionRequest);
    LOGGER.info("DeleteDatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .setDatasetBlob(CommitTest.getDatasetBlobFromPath("datasetVersion").getDataset())
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion1);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        createDatasetVersionRequest.getDatasetBlob(),
        datasetVersion1.getDatasetBlob());

    deleteDatasetVersionRequest =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion1.getId()).build();
    deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersionRequest);
    LOGGER.info("DeleteDatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());

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

    GetAllDatasetVersionsByDatasetId getAllDatasetVersionsByDatasetIdRequest =
        GetAllDatasetVersionsByDatasetId.newBuilder().setDatasetId(dataset.getId()).build();

    GetAllDatasetVersionsByDatasetId.Response getAllDatasetVersionsByDatasetIdResponse =
        datasetVersionServiceStub.getAllDatasetVersionsByDatasetId(
            getAllDatasetVersionsByDatasetIdRequest);
    assertEquals(
        "Total records count not matched with expected records count",
        0,
        getAllDatasetVersionsByDatasetIdResponse.getTotalRecords());

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

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion2 = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionMap.put(datasetVersion2.getId(), datasetVersion2);
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion2);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion2.getDatasetId());

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

    int pageLimit = 1;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      getAllDatasetVersionsByDatasetIdRequest =
          GetAllDatasetVersionsByDatasetId.newBuilder()
              .setDatasetId(dataset.getId())
              .setPageNumber(pageNumber)
              .setPageLimit(pageLimit)
              .setAscending(false)
              .setSortKey(ModelDBConstants.DATE_CREATED)
              .build();

      getAllDatasetVersionsByDatasetIdResponse =
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

    getAllDatasetVersionsByDatasetIdRequest =
        GetAllDatasetVersionsByDatasetId.newBuilder().setDatasetId(dataset.getId()).build();
    getAllDatasetVersionsByDatasetIdResponse =
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

    for (String datasetVersionId :
        new String[] {datasetVersion3.getId(), datasetVersion2.getId(), datasetVersion1.getId()}) {
      DeleteDatasetVersion deleteDatasetVersionRequest =
          DeleteDatasetVersion.newBuilder().setId(datasetVersionId).build();
      DeleteDatasetVersion.Response deleteDatasetVersionResponse =
          datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersionRequest);
      LOGGER.info("DeleteDatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionResponse.toString());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());
  }

  // TODO: getLatestDatasetVersionByDatasetId not supporting sort key because datasetVersion have
  // TODO: just one sortable column which is TIME_UPDATED
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

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion2 = createDatasetVersionResponse.getDatasetVersion();
    datasetVersionMap.put(datasetVersion2.getId(), datasetVersion2);
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion2);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion2.getDatasetId());

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

    for (DatasetVersion datasetVersion :
        new DatasetVersion[] {datasetVersion3, datasetVersion2, datasetVersion1}) {
      DeleteDatasetVersion deleteDatasetVersionRequest =
          DeleteDatasetVersion.newBuilder()
              .setDatasetId(datasetVersion.getDatasetId())
              .setId(datasetVersion.getId())
              .build();
      DeleteDatasetVersion.Response deleteDatasetVersionResponse =
          datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersionRequest);
      LOGGER.info("DeleteDatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionResponse.toString());
    }

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

    // Create datasetVersion
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");

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

    updateDescriptionRequest =
        UpdateDatasetVersionDescription.newBuilder().setId(datasetVersion.getId()).build();

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

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());

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
    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);

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

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Add DatasetVersion tags test stop................................");
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
                .setDatasetId(dataset.getId())
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
          DeleteDatasetVersion.newBuilder()
              .setDatasetId(dataset.getId())
              .setId(datasetVersion.getId())
              .build();
      DeleteDatasetVersion.Response deleteDatasetVersionResponse =
          datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
      LOGGER.info("DatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionResponse.toString());

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
    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);

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
    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);

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
            .setDatasetId(dataset.getId())
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
            .setDatasetId(dataset.getId())
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

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder()
            .setDatasetId(dataset.getId())
            .setId(datasetVersion.getId())
            .build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());

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
            .setDatasetId("123123")
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

    GetDatasetVersionAttributes getDatasetVersionAttributesRequest =
        GetDatasetVersionAttributes.newBuilder()
            .setId(datasetVersion.getId())
            .addAllAttributeKeys(keys)
            .build();

    GetDatasetVersionAttributes.Response response =
        datasetVersionServiceStub.getDatasetVersionAttributes(getDatasetVersionAttributesRequest);
    LOGGER.info(response.getAttributesList().toString());
    assertEquals(keys.size(), response.getAttributesList().size());

    getDatasetVersionAttributesRequest =
        GetDatasetVersionAttributes.newBuilder()
            .setDatasetId(dataset.getId())
            .setId(datasetVersion.getId())
            .setGetAll(true)
            .build();

    response =
        datasetVersionServiceStub.getDatasetVersionAttributes(getDatasetVersionAttributesRequest);
    LOGGER.info(response.getAttributesList().toString());
    assertEquals(datasetVersion.getAttributesList().size(), response.getAttributesList().size());

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder()
            .setDatasetId(dataset.getId())
            .setId(datasetVersion.getId())
            .build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());

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

    GetDatasetVersionAttributes getDatasetVersionAttributesRequest =
        GetDatasetVersionAttributes.newBuilder().build();

    try {
      datasetVersionServiceStub.getDatasetVersionAttributes(getDatasetVersionAttributesRequest);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    getDatasetVersionAttributesRequest =
        GetDatasetVersionAttributes.newBuilder()
            .setDatasetId("123123")
            .setId("jfhdsjfhdsfjk")
            .setGetAll(true)
            .build();
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
            .setDatasetId(dataset.getId())
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

    // Delete all data related to datasetVersion
    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());

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

    DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub datasetVersionServiceStub =
        DatasetVersionServiceGrpc.newBlockingStub(channel);
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

    LOGGER.info(
        "Delete DatasetVersion Attributes Negative test stop................................");
  }

  @Test
  @Ignore
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

    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion1);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion1.getDatasetId());

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion2 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion2);
    assertEquals(
        "DatasetVersion datsetId not match with expected DatasetVersion datsetId",
        dataset.getId(),
        datasetVersion2.getDatasetId());

    List<String> datasetVersionIds = new ArrayList<>();
    datasetVersionIds.add(datasetVersion1.getId());
    datasetVersionIds.add(datasetVersion2.getId());

    DeleteDatasetVersions deleteDatasetVersionsRequest =
        DeleteDatasetVersions.newBuilder().addAllIds(datasetVersionIds).build();
    DeleteDatasetVersions.Response deleteDatasetVersionsResponse =
        datasetVersionServiceStub.deleteDatasetVersions(deleteDatasetVersionsRequest);
    LOGGER.info("DeleteDatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionsResponse.toString());

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

      CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceStub =
          CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

      AddCollaboratorRequest.Response addCollaboratorResponse =
          collaboratorServiceStub.addOrUpdateRepositoryCollaborator(addCollaboratorRequest);
      LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
      assertTrue(addCollaboratorResponse.getStatus());
    }

    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion1);

    createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
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
      CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceStub =
          CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);
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
          collaboratorServiceStub.addOrUpdateRepositoryCollaborator(addCollaboratorRequest);
      LOGGER.info("Collaborator added in server : " + addCollaboratorResponse.getStatus());
      assertTrue(addCollaboratorResponse.getStatus());

      try {
        datasetVersionServiceStubClient2.deleteDatasetVersions(deleteDatasetVersionsRequest);
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.warn(
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.PERMISSION_DENIED.getCode(), status.getCode());
      }
    } else {
      DeleteDatasetVersions.Response deleteDatasetVersionsResponse =
          datasetVersionServiceStub.deleteDatasetVersions(deleteDatasetVersionsRequest);
      LOGGER.info("DeleteDatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionsResponse.toString());
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());
    LOGGER.info(
        "delete DatasetVersion by parent entities owner test stop................................");
  }

  @Test
  @Ignore
  public void getURLForVersionedDatasetBlob() throws IOException {
    LOGGER.info("Get Url for VersionedDatasetBlob test start................................");
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

    String path1 = "verta/test/test1.txt";
    String path2 = "verta/test/test2.txt";
    String internalPath1 = "test/internalDatasetBlobPaths/blobs/test1.txt";
    String internalPath2 = "test/internalDatasetBlobPaths/blobs/test2.txt";
    List<String> location = new ArrayList<>();
    location.add("versioned");
    location.add("s3_versioned");
    // location.add("test.txt");
    DatasetBlob datasetBlob =
        DatasetBlob.newBuilder()
            .setS3(
                S3DatasetBlob.newBuilder()
                    .addComponents(
                        S3DatasetComponentBlob.newBuilder()
                            .setS3VersionId("1.0")
                            .setPath(
                                PathDatasetComponentBlob.newBuilder()
                                    .setPath(path1)
                                    .setSize(2)
                                    .setLastModifiedAtSource(new Date().getTime())
                                    .setInternalVersionedPath(internalPath1)
                                    .build())
                            .build())
                    .addComponents(
                        S3DatasetComponentBlob.newBuilder()
                            .setS3VersionId("1.0")
                            .setPath(
                                PathDatasetComponentBlob.newBuilder()
                                    .setPath(path2)
                                    .setSize(2)
                                    .setLastModifiedAtSource(new Date().getTime())
                                    .setInternalVersionedPath(internalPath2)
                                    .build())
                            .build())
                    .build())
            .build();
    CreateDatasetVersion createDatasetVersionRequest = getDatasetVersionRequest(dataset.getId());
    createDatasetVersionRequest =
        createDatasetVersionRequest.toBuilder().setDatasetBlob(datasetBlob).build();
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    DatasetVersion datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("CreateDatasetVersion Response : \n" + datasetVersion1);

    try {
      GetUrlForDatasetBlobVersioned getUrlForVersionedDatasetBlob =
          GetUrlForDatasetBlobVersioned.newBuilder()
              .setDatasetId(dataset.getId())
              .setDatasetVersionId(datasetVersion1.getId())
              .setPathDatasetComponentBlobPath(path1)
              .setMethod(ModelDBConstants.PUT)
              .setPartNumber(1)
              .build();
      GetUrlForDatasetBlobVersioned.Response getUrlForVersionedDatasetBlobResponse =
          datasetVersionServiceStub.getUrlForDatasetBlobVersioned(getUrlForVersionedDatasetBlob);
      String presignedUrl1 = getUrlForVersionedDatasetBlobResponse.getUrl();
      assertNotNull("Presigned url not match with expected presigned url", presignedUrl1);
      getUrlForVersionedDatasetBlob =
          getUrlForVersionedDatasetBlob.toBuilder().setPartNumber(2).build();
      getUrlForVersionedDatasetBlobResponse =
          datasetVersionServiceStub.getUrlForDatasetBlobVersioned(getUrlForVersionedDatasetBlob);
      String presignedUrl2 = getUrlForVersionedDatasetBlobResponse.getUrl();
      assertNotNull("Presigned url not match with expected presigned url", presignedUrl2);
      // Create the connection and use it to upload the new object using the pre-signed URL.
      HttpURLConnection connection = (HttpURLConnection) new URL(presignedUrl1).openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("PUT");
      OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
      for (int i = 0; i < 130000; ++i) {
        out.write("This text uploaded as an object via presigned URL.");
      }
      out.close();
      // Check the HTTP response code. To complete the upload and make the object available,
      // you must interact with the connection object in some way.
      connection.getResponseCode();
      String etag1 = connection.getHeaderField("ETag");
      connection = (HttpURLConnection) new URL(presignedUrl2).openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("PUT");
      out = new OutputStreamWriter(connection.getOutputStream());
      for (int i = 0; i < 120000; ++i) {
        out.write("This text uploaded as an object via presigned URL2.");
      }
      out.close();
      // Check the HTTP response code. To complete the upload and make the object available,
      // you must interact with the connection object in some way.
      connection.getResponseCode();
      String etag2 = connection.getHeaderField("ETag");
      CommitVersionedDatasetBlobArtifactPart.Response p1 =
          datasetVersionServiceStub.commitVersionedDatasetBlobArtifactPart(
              CommitVersionedDatasetBlobArtifactPart.newBuilder()
                  .setDatasetId(dataset.getId())
                  .setDatasetVersionId(datasetVersion1.getId())
                  .setPathDatasetComponentBlobPath(path1)
                  .setArtifactPart(
                      ArtifactPart.newBuilder()
                          .setEtag(etag1.replaceAll("\"", ""))
                          .setPartNumber(1))
                  .build());
      CommitVersionedDatasetBlobArtifactPart.Response p2 =
          datasetVersionServiceStub.commitVersionedDatasetBlobArtifactPart(
              CommitVersionedDatasetBlobArtifactPart.newBuilder()
                  .setDatasetId(dataset.getId())
                  .setDatasetVersionId(datasetVersion1.getId())
                  .setPathDatasetComponentBlobPath(path1)
                  .setArtifactPart(
                      ArtifactPart.newBuilder()
                          .setEtag(etag2.replaceAll("\"", ""))
                          .setPartNumber(2))
                  .build());
      GetCommittedVersionedDatasetBlobArtifactParts.Response committedArtifactParts =
          datasetVersionServiceStub.getCommittedVersionedDatasetBlobArtifactParts(
              GetCommittedVersionedDatasetBlobArtifactParts.newBuilder()
                  .setDatasetId(dataset.getId())
                  .setDatasetVersionId(datasetVersion1.getId())
                  .setPathDatasetComponentBlobPath(path1)
                  .build());
      CommitMultipartVersionedDatasetBlobArtifact.Response commitMultipartArtifact =
          datasetVersionServiceStub.commitMultipartVersionedDatasetBlobArtifact(
              CommitMultipartVersionedDatasetBlobArtifact.newBuilder()
                  .setDatasetId(dataset.getId())
                  .setDatasetVersionId(datasetVersion1.getId())
                  .setPathDatasetComponentBlobPath(path1)
                  .build());
      GetCommittedVersionedDatasetBlobArtifactParts.Response
          committedVersionedDatasetBlobArtifactParts =
              datasetVersionServiceStub.getCommittedVersionedDatasetBlobArtifactParts(
                  GetCommittedVersionedDatasetBlobArtifactParts.newBuilder()
                      .setDatasetId(dataset.getId())
                      .setDatasetVersionId(datasetVersion1.getId())
                      .setPathDatasetComponentBlobPath(path1)
                      .build());
    } finally {
      DeleteDatasetVersions deleteDatasetVersionsRequest =
          DeleteDatasetVersions.newBuilder().addIds(datasetVersion1.getId()).build();
      DeleteDatasetVersions.Response deleteDatasetVersionsResponse =
          datasetVersionServiceStub.deleteDatasetVersions(deleteDatasetVersionsRequest);
      LOGGER.info("DeleteDatasetVersion deleted successfully");
      LOGGER.info(deleteDatasetVersionsResponse.toString());

      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());
    }
    LOGGER.info("Get Url for VersionedDatasetBlob test stop................................");
  }

  @Test
  public void getDatasetVersionById() {
    LOGGER.info("Get DatasetVersion by Id test start................................");

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

    GetDatasetVersionById getDatasetVersionById =
        GetDatasetVersionById.newBuilder().setId(datasetVersion.getId()).build();
    GetDatasetVersionById.Response getDatasetVersionByIdResponse =
        datasetVersionServiceStub.getDatasetVersionById(getDatasetVersionById);
    assertEquals(
        "Dataset name not match with expected dataset name",
        datasetVersion.getId(),
        getDatasetVersionByIdResponse.getDatasetVersion().getId());

    DeleteDatasetVersion deleteDatasetVersion =
        DeleteDatasetVersion.newBuilder()
            .setDatasetId(dataset.getId())
            .setId(datasetVersion.getId())
            .build();
    DeleteDatasetVersion.Response deleteDatasetVersionResponse =
        datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
    LOGGER.info("DatasetVersion deleted successfully");
    LOGGER.info(deleteDatasetVersionResponse.toString());

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());

    LOGGER.info("Get DatasetVersion by Id test stop................................");
  }

  @Test
  public void checkVersionWithDatasetVersionsTest() {

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
      assertTrue(true);
    }

    DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
    DeleteDataset.Response deleteDatasetResponse = datasetServiceStub.deleteDataset(deleteDataset);
    LOGGER.info("Dataset deleted successfully");
    LOGGER.info(deleteDatasetResponse.toString());
    assertTrue(deleteDatasetResponse.getStatus());
  }
}
