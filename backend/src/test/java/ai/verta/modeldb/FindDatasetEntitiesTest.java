package ai.verta.modeldb;

import static org.junit.Assert.*;

import ai.verta.modeldb.DatasetServiceGrpc.DatasetServiceBlockingStub;
import ai.verta.modeldb.DatasetVersionServiceGrpc.DatasetVersionServiceBlockingStub;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FindDatasetEntitiesTest {

  private static final Logger LOGGER = LogManager.getLogger(FindDatasetEntitiesTest.class);

  private static String serverName = InProcessServerBuilder.generateName();
  private static InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder channelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static App app;

  // all service stubs
  private static DatasetServiceBlockingStub datasetServiceStub;
  private static DatasetVersionServiceBlockingStub datasetVersionServiceStub;

  // Dataset Entities
  private static Dataset dataset1;
  private static Dataset dataset2;
  private static Dataset dataset3;
  private static Dataset dataset4;
  private static Map<String, Dataset> datasetMap = new HashMap<>();

  // DatasetVersion Entities
  private static DatasetVersion datasetVersion1;
  private static DatasetVersion datasetVersion2;
  private static DatasetVersion datasetVersion3;
  private static DatasetVersion datasetVersion4;
  private static Map<String, DatasetVersion> datasetVersionMap = new HashMap<>();

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
      AuthClientInterceptor authClientInterceptor = new AuthClientInterceptor(testPropMap);
      channelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
    }

    serverBuilder.build().start();
    ManagedChannel channel = channelBuilder.maxInboundMessageSize(1024).build();

    // Create all service blocking stub
    datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);
    datasetVersionServiceStub = DatasetVersionServiceGrpc.newBlockingStub(channel);

    // Create all entities
    createDatasetEntities();
    createDatasetVersionEntities();
  }

  @AfterClass
  public static void removeServerAndService() {
    App.initiateShutdown(0);

    // Remove all entities
    removeEntities();

    // shutdown test server
    serverBuilder.build().shutdownNow();
  }

  private static void createDatasetEntities() {
    DatasetTest datasetTest = new DatasetTest();

    // Create two dataset of above dataset
    CreateDataset createDatasetRequest = datasetTest.getDatasetRequest("Dataset_1");
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
            .addTags("Tag_1")
            .addTags("Tag_2")
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
    createDatasetRequest = datasetTest.getDatasetRequest("Dataset_2");
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
            .addTags("Tag_1")
            .addTags("Tag_3")
            .addTags("Tag_4")
            .build();
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    dataset2 = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected Dataset name",
        createDatasetRequest.getName(),
        dataset2.getName());

    // dataset3 of above dataset
    createDatasetRequest = datasetTest.getDatasetRequest("Dataset_3");
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
            .addTags("Tag_1")
            .addTags("Tag_5")
            .addTags("Tag_6")
            .build();
    createDatasetResponse = datasetServiceStub.createDataset(createDatasetRequest);
    dataset3 = createDatasetResponse.getDataset();
    LOGGER.info("Dataset created successfully");
    assertEquals(
        "Dataset name not match with expected Dataset name",
        createDatasetRequest.getName(),
        dataset3.getName());

    // dataset4 of above dataset
    createDatasetRequest = datasetTest.getDatasetRequest("Dataset_4");
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
            .addTags("Tag_5")
            .addTags("Tag_7")
            .addTags("Tag_8")
            .setDatasetVisibility(DatasetVisibility.PUBLIC)
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
  }

  private static void createDatasetVersionEntities() {
    DatasetVersionTest datasetVersionTest = new DatasetVersionTest();

    long version = 1L;

    // Create two datasetVersion of above dataset
    CreateDatasetVersion createDatasetVersionRequest =
        datasetVersionTest.getDatasetVersionRequest(dataset1.getId());
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
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_2")
            .build();
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        version,
        datasetVersion1.getVersion());

    // datasetVersion2 of above dataset
    createDatasetVersionRequest = datasetVersionTest.getDatasetVersionRequest(dataset1.getId());
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
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_3")
            .addTags("Tag_4")
            .setRawDatasetVersionInfo(
                RawDatasetVersionInfo.newBuilder().setSize(1).setNumRecords(1).build())
            .setDatasetType(DatasetTypeEnum.DatasetType.RAW)
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    datasetVersion2 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        ++version,
        datasetVersion2.getVersion());

    // datasetVersion3 of above dataset
    createDatasetVersionRequest = datasetVersionTest.getDatasetVersionRequest(dataset1.getId());
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
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_1")
            .addTags("Tag_5")
            .addTags("Tag_6")
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    datasetVersion3 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        ++version,
        datasetVersion3.getVersion());

    // datasetVersion4 of above dataset
    createDatasetVersionRequest = datasetVersionTest.getDatasetVersionRequest(dataset1.getId());
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
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("Tag_5")
            .addTags("Tag_7")
            .addTags("Tag_8")
            .setRawDatasetVersionInfo(
                RawDatasetVersionInfo.newBuilder().setSize(1).setNumRecords(1).build())
            .setDatasetType(DatasetTypeEnum.DatasetType.RAW)
            .setDatasetVersionVisibility(DatasetVisibilityEnum.DatasetVisibility.PUBLIC)
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    datasetVersion4 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");
    assertEquals(
        "DatasetVersion version not match with expected DatasetVersion version",
        ++version,
        datasetVersion4.getVersion());

    datasetVersionMap.put(datasetVersion1.getId(), datasetVersion1);
    datasetVersionMap.put(datasetVersion2.getId(), datasetVersion2);
    datasetVersionMap.put(datasetVersion3.getId(), datasetVersion3);
    datasetVersionMap.put(datasetVersion4.getId(), datasetVersion4);
  }

  private static void removeEntities() {
    for (String datasetId : datasetMap.keySet()) {
      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(datasetId).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());
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

  /** Validation check for the predicate value with empty string which is not valid */
  @Test
  public void findDatasetPredicateValueEmptyNegativeTest() {
    LOGGER.info("FindDatasets predicate value is empty negative test start.........");

    DatasetTest datasetTest = new DatasetTest();

    // Validate check for predicate value not empty
    List<KeyValueQuery> predicates = new ArrayList<>();
    Value stringValueType = Value.newBuilder().setStringValue("").build();

    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(stringValueType)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    FindDatasets findDatasets =
        FindDatasets.newBuilder()
            .addDatasetIds(dataset1.getId())
            .addAllPredicates(predicates)
            // .setIdsOnly(true)
            .build();
    try {
      datasetServiceStub.findDatasets(findDatasets);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // If key is not set in predicate
    findDatasets =
        FindDatasets.newBuilder()
            .addDatasetIds(dataset1.getId())
            .addPredicates(
                KeyValueQuery.newBuilder()
                    .setValue(Value.newBuilder().setNumberValue(11).build())
                    .build())
            .build();

    try {
      datasetServiceStub.findDatasets(findDatasets);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("FindDatasets predicate value is empty negative test start.........");
  }

  /** Validate check for protobuf struct type in KeyValueQuery not implemented */
  @Test
  public void findDatasetStructTypeNotImplemented() {
    LOGGER.info(
        "check for protobuf struct type in KeyValueQuery not implemented test start........");

    DatasetTest datasetTest = new DatasetTest();

    // Validate check for struct Type not implemented
    Value numValue = Value.newBuilder().setNumberValue(17.1716586149719).build();

    Struct.Builder struct = Struct.newBuilder();
    struct.putFields("number_value", numValue);
    struct.build();
    Value structValue = Value.newBuilder().setStructValue(struct).build();

    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(structValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    FindDatasets findDatasets =
        FindDatasets.newBuilder()
            .addDatasetIds(dataset1.getId())
            .addPredicates(keyValueQuery)
            .build();

    try {
      datasetServiceStub.findDatasets(findDatasets);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    LOGGER.info(
        "check for protobuf struct type in KeyValueQuery not implemented test start........");
  }

  /** Find dataset with value of attributes.attribute_1 <= 0.6543210 */
  @Test
  public void findDatasetsByAttributesTest() {
    LOGGER.info("FindDatasets by attribute test start................................");

    DatasetTest datasetTest = new DatasetTest();

    // get dataset with value of attributes.attribute_1 <= 0.6543210
    Value numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    FindDatasets findDatasets = FindDatasets.newBuilder().addPredicates(keyValueQuery).build();

    FindDatasets.Response response = datasetServiceStub.findDatasets(findDatasets);
    LOGGER.info("FindDatasets Response : " + response.getDatasetsList());
    assertEquals(
        "Dataset count not match with expected dataset count",
        3,
        response.getDatasetsList().size());

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());

    for (Dataset fetchedDataset : response.getDatasetsList()) {
      boolean doesAttributeExist = false;
      for (KeyValue fetchedAttribute : fetchedDataset.getAttributesList()) {
        if (fetchedAttribute.getKey().equals("attribute_1")) {
          doesAttributeExist = true;
          assertTrue(
              "Dataset attributes.attribute_1 not match with expected dataset attributes.attribute_1",
              fetchedAttribute.getValue().getNumberValue() <= 0.6543210);
        }
      }
      if (!doesAttributeExist) {
        fail("Expected attribute not found in fetched attributes");
      }
    }

    LOGGER.info("FindDatasets by attribute test start................................");
  }

  /**
   * Find dataset with value of attributes.attribute_1 <= 0.6543210 & attributes.attribute_2 == 0.31
   */
  @Test
  public void findDatasetsByMultipleAttributeTest() {
    LOGGER.info("FindDatasets by multiple attribute condition test start..................");

    DatasetTest datasetTest = new DatasetTest();

    List<KeyValueQuery> predicates = new ArrayList<>();
    Value numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setNumberValue(0.31).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery2);

    FindDatasets findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetMap.keySet())
            .addAllPredicates(predicates)
            .setIdsOnly(true)
            .build();

    FindDatasets.Response response = datasetServiceStub.findDatasets(findDatasets);
    LOGGER.info("FindDatasets Response : " + response.getDatasetsCount());
    assertEquals(
        "Dataset count not match with expected dataset count", 1, response.getDatasetsCount());
    assertEquals(
        "Dataset not match with expected dataset",
        dataset2.getId(),
        response.getDatasetsList().get(0).getId());
    assertNotEquals(
        "Dataset not match with expected dataset", dataset2, response.getDatasetsList().get(0));
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    LOGGER.info("FindDatasets by multiple attribute condition test stop..................");
  }

  /** Find dataset with value of metrics.accuracy >= 0.6543210 & tags == Tag_7 */
  @Test
  public void findDatasetsByMetricsAndTagsTest() {
    LOGGER.info("FindDatasets by metrics and tags test start................................");

    DatasetTest datasetTest = new DatasetTest();

    List<KeyValueQuery> predicates = new ArrayList<>();
    Value stringValue = Value.newBuilder().setStringValue("Tag_7").build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery);

    Value numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.GTE)
            .build();
    predicates.add(keyValueQuery2);

    FindDatasets findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetMap.keySet())
            .addAllPredicates(predicates)
            .build();

    FindDatasets.Response response = datasetServiceStub.findDatasets(findDatasets);
    LOGGER.info("FindDatasets Response : " + response.getDatasetsCount());
    assertEquals(
        "Dataset count not match with expected dataset count", 1, response.getDatasetsCount());
    assertEquals(
        "Dataset not match with expected dataset",
        dataset4.getId(),
        response.getDatasetsList().get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    LOGGER.info("FindDatasets by metrics and tags test stop................................");
  }

  /** Find dataset with value of endTime */
  @Test
  public void findDatasetsByDatasetEndTimeTest() {
    LOGGER.info("FindDatasets By Dataset EndTime test start................................");

    DatasetTest datasetTest = new DatasetTest();

    Value stringValue =
        Value.newBuilder().setStringValue(String.valueOf(dataset4.getTimeUpdated())).build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.TIME_UPDATED)
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    FindDatasets findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetMap.keySet())
            .addPredicates(keyValueQuery)
            .build();

    FindDatasets.Response response = datasetServiceStub.findDatasets(findDatasets);
    LOGGER.info("FindDatasets Response : " + response.getDatasetsCount());
    assertEquals(
        "Dataset count not match with expected dataset count", 1, response.getDatasetsCount());
    assertEquals(
        "DatasetRun not match with expected datasetRun",
        dataset4.getId(),
        response.getDatasetsList().get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    LOGGER.info("FindDatasets By Dataset EndTime test stop................................");
  }

  /** FInd Datasets by attribute with pagination */
  @Test
  public void findDatasetsByAttributeWithPaginationTest() {
    LOGGER.info(
        "FindDatasets by attribute with pagination test start................................");

    DatasetTest datasetTest = new DatasetTest();

    Value numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    int pageLimit = 2;
    int count = 0;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      FindDatasets findDatasets =
          FindDatasets.newBuilder()
              .addAllDatasetIds(datasetMap.keySet())
              .addPredicates(keyValueQuery2)
              .setPageLimit(pageLimit)
              .setPageNumber(pageNumber)
              .setAscending(true)
              .setSortKey("name")
              .build();

      FindDatasets.Response response = datasetServiceStub.findDatasets(findDatasets);

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          response.getTotalRecords());

      if (response.getDatasetsList() != null && response.getDatasetsList().size() > 0) {
        isExpectedResultFound = true;
        for (Dataset dataset : response.getDatasetsList()) {
          assertTrue(
              "Dataset not match with expected dataset", datasetMap.containsKey(dataset.getId()));

          if (count == 0) {
            assertEquals(
                "Dataset name not match with expected dataset name",
                dataset1.getName(),
                dataset.getName());
          } else if (count == 1) {
            assertEquals(
                "Dataset name not match with expected dataset name",
                dataset2.getName(),
                dataset.getName());
          } else if (count == 2) {
            assertEquals(
                "Dataset name not match with expected dataset name",
                dataset3.getName(),
                dataset.getName());
          }
          count++;
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More Dataset not found in database");
          assertTrue(true);
        } else {
          fail("Expected dataset not found in response");
        }
        break;
      }
    }

    LOGGER.info(
        "FindDatasets by attribute with pagination test start................................");
  }

  /** Check observations.attributes not support */
  @Test
  public void findDatasetsNotSupportObservationsAttributesTest() {
    LOGGER.info("FindDatasets not support the observation.attributes test start............");

    DatasetTest datasetTest = new DatasetTest();

    Value numValue = Value.newBuilder().setNumberValue(0.31).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    FindDatasets findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetMap.keySet())
            .addPredicates(keyValueQuery2)
            .setAscending(false)
            .setSortKey("observations.attribute.attr_1")
            .build();

    try {
      datasetServiceStub.findDatasets(findDatasets);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    LOGGER.info("FindDatasets not support the observation.attributes test stop............");
  }

  /** Find datasets with value of tags */
  @Test
  public void findDatasetsByTagsTest() {
    LOGGER.info("FindDatasets by tags test start................................");

    DatasetTest datasetTest = new DatasetTest();

    // get dataset with value of tags == test_tag_123
    Value stringValue1 = Value.newBuilder().setStringValue("Tag_1").build();
    KeyValueQuery keyValueQueryTag1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue1)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    // get datasetRun with value of tags == test_tag_456
    Value stringValue2 = Value.newBuilder().setStringValue("Tag_5").build();
    KeyValueQuery keyValueQueryTag2 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue2)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    FindDatasets findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetMap.keySet())
            .addPredicates(keyValueQueryTag1)
            .addPredicates(keyValueQueryTag2)
            .build();

    FindDatasets.Response response = datasetServiceStub.findDatasets(findDatasets);
    LOGGER.info("FindDatasets Response : " + response.getDatasetsCount());
    assertEquals(
        "Dataset count not match with expected dataset count", 1, response.getDatasetsCount());
    assertEquals(
        "Dataset not match with expected dataset",
        dataset3.getId(),
        response.getDatasetsList().get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    LOGGER.info("FindDatasets by tags test start................................");
  }

  /** Find datasets with attribute predicates and sort by attribute key */
  @Test
  public void findAndSortDatasetsByAttributeTest() {
    LOGGER.info("Find and sort Datasets by attributes test start................................");

    Value numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValueLoss)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    FindDatasets findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetMap.keySet())
            .addPredicates(keyValueQueryAttribute_1)
            .setAscending(false)
            .setSortKey("attributes.attribute_1")
            .build();

    FindDatasets.Response response = datasetServiceStub.findDatasets(findDatasets);

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "Dataset count not match with expected dataset count", 3, response.getDatasetsCount());

    KeyValueQuery keyValueQueryAccuracy =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.654321).build())
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetMap.keySet())
            .addPredicates(keyValueQueryAttribute_1)
            .addPredicates(keyValueQueryAccuracy)
            .setAscending(false)
            .setSortKey("attributes.attribute_1")
            .build();
    response = datasetServiceStub.findDatasets(findDatasets);

    assertEquals(
        "Total records count not matched with expected records count",
        2,
        response.getTotalRecords());
    assertEquals(
        "Dataset count not match with expected dataset count", 2, response.getDatasetsCount());

    numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValueLoss)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findDatasets =
        FindDatasets.newBuilder()
            .addAllDatasetIds(datasetMap.keySet())
            .addPredicates(keyValueQueryAttribute_1)
            .setAscending(false)
            .setIdsOnly(true)
            .setSortKey("attributes.attribute_1")
            .build();

    response = datasetServiceStub.findDatasets(findDatasets);

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "Dataset count not match with expected dataset count", 3, response.getDatasetsCount());

    for (int index = 0; index < response.getDatasetsCount(); index++) {
      Dataset dataset = response.getDatasetsList().get(index);
      if (index == 0) {
        assertNotEquals("Dataset not match with expected dataset", dataset3, dataset);
        assertEquals(
            "Dataset Id not match with expected dataset Id", dataset3.getId(), dataset.getId());
      } else if (index == 1) {
        assertNotEquals("Dataset not match with expected dataset", dataset2, dataset);
        assertEquals(
            "Dataset Id not match with expected dataset Id", dataset2.getId(), dataset.getId());
      } else if (index == 2) {
        assertNotEquals("Dataset not match with expected dataset", dataset1, dataset);
        assertEquals(
            "Dataset Id not match with expected dataset Id", dataset1.getId(), dataset.getId());
      }
    }

    LOGGER.info("Find and sort Datasets by attributes test start................................");
  }

  /** Find public datasets sort by name */
  @Test
  public void findPublicDatasetsSortingByNameTest() {
    LOGGER.info(
        "Find public Datasets with sorting by name test start................................");

    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.DATASET_VISIBILITY)
            .setValue(Value.newBuilder().setStringValue("PUBLIC").build())
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    FindDatasets findDatasets =
        FindDatasets.newBuilder()
            .addPredicates(keyValueQuery)
            .setAscending(false)
            .setIdsOnly(false)
            .setSortKey("name")
            .build();

    FindDatasets.Response response = datasetServiceStub.findDatasets(findDatasets);
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());
    assertEquals(
        "Dataset count not match with expected dataset count", 1, response.getDatasetsCount());
    assertEquals(
        "Dataset Id not match with expected dataset Id",
        dataset4.getId(),
        response.getDatasets(0).getId());

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.DATASET_VISIBILITY)
            .setValue(Value.newBuilder().setStringValue("PUBLIC").build())
            .setOperator(OperatorEnum.Operator.NE)
            .build();
    findDatasets =
        FindDatasets.newBuilder().addPredicates(keyValueQuery).setSortKey("name").build();

    response = datasetServiceStub.findDatasets(findDatasets);
    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "Dataset count not match with expected dataset count", 3, response.getDatasetsCount());
    assertEquals(
        "Dataset Id not match with expected dataset Id",
        dataset3.getId(),
        response.getDatasets(0).getId());

    findDatasets =
        FindDatasets.newBuilder()
            .addPredicates(keyValueQuery)
            .setAscending(true)
            .setSortKey("name")
            .build();

    response = datasetServiceStub.findDatasets(findDatasets);
    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "Dataset count not match with expected dataset count", 3, response.getDatasetsCount());
    assertEquals(
        "Dataset Id not match with expected dataset Id",
        dataset1.getId(),
        response.getDatasets(0).getId());

    LOGGER.info(
        "Find public Datasets with sorting by name test start................................");
  }

  /** Validation check for the predicate value with empty string which is not valid */
  @Test
  public void findDatasetVersionsPredicateValueEmptyNegativeTest() {
    LOGGER.info("DatasetVersions predicate value is empty negative test start.........");

    // Validate check for predicate value not empty
    List<KeyValueQuery> predicates = new ArrayList<>();
    Value stringValueType = Value.newBuilder().setStringValue("").build();

    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(stringValueType)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addDatasetVersionIds(datasetVersion1.getId())
            .addAllPredicates(predicates)
            // .setIdsOnly(true)
            .build();
    try {
      datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    // If key is not set in predicate
    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addDatasetVersionIds(datasetVersion1.getId())
            .addPredicates(
                KeyValueQuery.newBuilder()
                    .setValue(Value.newBuilder().setNumberValue(11).build())
                    .build())
            .build();

    try {
      datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("DatasetVersions predicate value is empty negative test stop..............");
  }

  /** Validate check for protobuf struct type in KeyValueQuery not implemented */
  @Test
  public void findDatasetVersionStructTypeNotImplemented() {
    LOGGER.info(
        "Check for protobuf struct type in KeyValueQuery not implemented test start........");

    // Validate check for struct Type not implemented
    Value numValue = Value.newBuilder().setNumberValue(17.1716586149719).build();

    Struct.Builder struct = Struct.newBuilder();
    struct.putFields("number_value", numValue);
    struct.build();
    Value structValue = Value.newBuilder().setStructValue(struct).build();

    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(structValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addDatasetVersionIds(datasetVersion1.getId())
            .addPredicates(keyValueQuery)
            .build();

    try {
      datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
      fail();
    } catch (StatusRuntimeException exc) {
      Status status = Status.fromThrowable(exc);
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    LOGGER.info(
        "Check for protobuf struct type in KeyValueQuery not implemented test stop........");
  }

  /** Find datasetVersion with value of attributes.attribute_1 <= 0.6543210 */
  @Test
  public void findDatasetVersionsByAttributeTest() {
    LOGGER.info("FindDatasetVersions by attribute test start................................");

    // get datasetVersion with value of attributes.attribute_1 <= 0.6543210
    Value numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder().addPredicates(keyValueQuery).build();

    FindDatasetVersions.Response response =
        datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsList());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        3,
        response.getDatasetVersionsList().size());

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());

    for (DatasetVersion fetchedDatasetVersion : response.getDatasetVersionsList()) {
      boolean doesAttributeExist = false;
      for (KeyValue fetchedAttribute : fetchedDatasetVersion.getAttributesList()) {
        if (fetchedAttribute.getKey().equals("attribute_1")) {
          doesAttributeExist = true;
          assertTrue(
              "DatasetVersion attributes.attribute_1 not match with expected datasetVersion attributes.attribute_1",
              fetchedAttribute.getValue().getNumberValue() <= 0.6543210);
        }
      }
      if (!doesAttributeExist) {
        fail("Expected attribute not found in fetched attributes");
      }
    }

    LOGGER.info("FindDatasetVersions by attribute test stop................................");
  }

  /**
   * Find datasetVersion with value of attributes.attribute_1 <= 0.6543210 & attributes.attribute_2
   * == 0.31
   */
  @Test
  public void findDatasetVersionsByMultipleAttributeTest() {
    LOGGER.info("FindDatasetVersions by multiple attribute condition test start..............");

    List<KeyValueQuery> predicates = new ArrayList<>();
    Value numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setNumberValue(0.31).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery2);

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .addAllPredicates(predicates)
            .setIdsOnly(true)
            .build();

    FindDatasetVersions.Response response =
        datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        1,
        response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion not match with expected datasetVersion",
        datasetVersion2.getId(),
        response.getDatasetVersionsList().get(0).getId());
    assertNotEquals(
        "DatasetVersion not match with expected datasetVersion",
        datasetVersion2,
        response.getDatasetVersionsList().get(0));
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    LOGGER.info("FindDatasetVersions by multiple attribute condition test stop..............");
  }

  /** Find datasetVersion with value of metrics.accuracy >= 0.6543210 & tags == Tag_7 */
  @Test
  public void findDatasetVersionsByMetricsAndTagsTest() {
    LOGGER.info("FindDatasetVersions by metrics and tags test start.........");

    List<KeyValueQuery> predicates = new ArrayList<>();
    Value stringValue = Value.newBuilder().setStringValue("Tag_7").build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery);

    Value numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.GTE)
            .build();
    predicates.add(keyValueQuery2);

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .addAllPredicates(predicates)
            .build();

    FindDatasetVersions.Response response =
        datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        1,
        response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion not match with expected datasetVersion",
        datasetVersion4.getId(),
        response.getDatasetVersionsList().get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    LOGGER.info("FindDatasetVersions by metrics and tags test stop.........");
  }

  /** Find datasetVersion with value of endTime */
  @Test
  public void findDatasetVersionsByEndTimeTest() {
    LOGGER.info("FindDatasetVersions by datasetVersion EndTime test start..........");

    Value stringValue =
        Value.newBuilder().setStringValue(String.valueOf(datasetVersion4.getTimeUpdated())).build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.TIME_UPDATED)
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .addPredicates(keyValueQuery)
            .build();

    FindDatasetVersions.Response response =
        datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        1,
        response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersionRun not match with expected datasetVersionRun",
        datasetVersion4.getId(),
        response.getDatasetVersionsList().get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    LOGGER.info("FindDatasetVersions by datasetVersion EndTime test stop..........");
  }

  /** Find DatasetVersion by attribute with pagination */
  @Test
  public void findDatasetVersionsByAtrributeWithPaginationTest() {
    LOGGER.info("FindDatasetVersions by attribute with pagination test start...........");

    Value numValue = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    int pageLimit = 2;
    int count = 0;
    boolean isExpectedResultFound = false;
    for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
      FindDatasetVersions findDatasetVersions =
          FindDatasetVersions.newBuilder()
              .addAllDatasetVersionIds(datasetVersionMap.keySet())
              .addPredicates(keyValueQuery2)
              .setPageLimit(pageLimit)
              .setPageNumber(pageNumber)
              .setAscending(true)
              .setSortKey("version")
              .build();

      FindDatasetVersions.Response response =
          datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          response.getTotalRecords());

      if (response.getDatasetVersionsList() != null
          && response.getDatasetVersionsList().size() > 0) {
        isExpectedResultFound = true;
        for (DatasetVersion datasetVersion : response.getDatasetVersionsList()) {
          assertEquals(
              "DatasetVersion not match with expected datasetVersion",
              datasetVersionMap.get(datasetVersion.getId()),
              datasetVersion);

          if (count == 0) {
            assertEquals(
                "DatasetVersion version not match with expected datasetVersion version",
                datasetVersion1.getVersion(),
                datasetVersion.getVersion());
          } else if (count == 1) {
            assertEquals(
                "DatasetVersion version not match with expected datasetVersion version",
                datasetVersion2.getVersion(),
                datasetVersion.getVersion());
          } else if (count == 2) {
            assertEquals(
                "DatasetVersion version not match with expected datasetVersion version",
                datasetVersion3.getVersion(),
                datasetVersion.getVersion());
          }
          count++;
        }
      } else {
        if (isExpectedResultFound) {
          LOGGER.warn("More DatasetVersion not found in database");
          assertTrue(true);
        } else {
          fail("Expected datasetVersion not found in response");
        }
        break;
      }
    }

    LOGGER.info("FindDatasetVersions by attribute with pagination test stop...........");
  }

  /** Check observations.attributes not support */
  @Test
  public void findDatasetVersionsNotSupportObservationsAttributesTest() {
    LOGGER.info("FindDatasetVersions not support the observation.attributes test start........");

    Value numValue = Value.newBuilder().setNumberValue(0.31).build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .addPredicates(keyValueQuery2)
            .setAscending(false)
            .setSortKey("observations.attribute.attr_1")
            .build();

    try {
      datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
      fail();
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
    }

    LOGGER.info("FindDatasetVersions not support the observation.attributes test stop........");
  }

  /** Find datasetVersion with value of tags */
  @Test
  public void findDatasetVersionsByTagsTest() {
    LOGGER.info("FindDatasetVersions by tags test start................................");

    Value stringValue1 = Value.newBuilder().setStringValue("Tag_1").build();
    KeyValueQuery keyValueQueryTag1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue1)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    // get datasetVersionRun with value of tags == test_tag_456
    Value stringValue2 = Value.newBuilder().setStringValue("Tag_5").build();
    KeyValueQuery keyValueQueryTag2 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue2)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .addPredicates(keyValueQueryTag1)
            .addPredicates(keyValueQueryTag2)
            .build();

    FindDatasetVersions.Response response =
        datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        1,
        response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion not match with expected datasetVersion",
        datasetVersion3.getId(),
        response.getDatasetVersionsList().get(0).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    LOGGER.info("FindDatasetVersions by tags test stop................................");
  }

  /** Find datasetVersions with attribute predicates and sort by attribute key */
  @Test
  public void findAndSortDatasetVersionsByAttributeTest() {
    LOGGER.info("Find and Sort DatasetVersions By attribute test start................");

    Value numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    KeyValueQuery keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValueLoss)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .addPredicates(keyValueQueryAttribute_1)
            .setAscending(false)
            .setSortKey("attributes.attribute_1")
            .build();

    FindDatasetVersions.Response response =
        datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        3,
        response.getDatasetVersionsCount());

    KeyValueQuery keyValueQueryAccuracy =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_2")
            .setValue(Value.newBuilder().setNumberValue(0.654321).build())
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .addPredicates(keyValueQueryAttribute_1)
            .addPredicates(keyValueQueryAccuracy)
            .setAscending(false)
            .setSortKey("attributes.attribute_1")
            .build();
    response = datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);

    assertEquals(
        "Total records count not matched with expected records count",
        2,
        response.getTotalRecords());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        2,
        response.getDatasetVersionsCount());

    numValueLoss = Value.newBuilder().setNumberValue(0.6543210).build();
    keyValueQueryAttribute_1 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValueLoss)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .addPredicates(keyValueQueryAttribute_1)
            .setAscending(false)
            .setIdsOnly(true)
            .setSortKey("attributes.attribute_1")
            .build();

    response = datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);

    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        3,
        response.getDatasetVersionsCount());

    for (int index = 0; index < response.getDatasetVersionsCount(); index++) {
      DatasetVersion datasetVersion = response.getDatasetVersionsList().get(index);
      if (index == 0) {
        assertNotEquals(
            "DatasetVersion not match with expected datasetVersion",
            datasetVersion3,
            datasetVersion);
        assertEquals(
            "DatasetVersion Id not match with expected datasetVersion Id",
            datasetVersion3.getId(),
            datasetVersion.getId());
      } else if (index == 1) {
        assertNotEquals(
            "DatasetVersion not match with expected datasetVersion",
            datasetVersion2,
            datasetVersion);
        assertEquals(
            "DatasetVersion Id not match with expected datasetVersion Id",
            datasetVersion2.getId(),
            datasetVersion.getId());
      } else if (index == 2) {
        assertNotEquals(
            "DatasetVersion not match with expected datasetVersion",
            datasetVersion1,
            datasetVersion);
        assertEquals(
            "DatasetVersion Id not match with expected datasetVersion Id",
            datasetVersion1.getId(),
            datasetVersion.getId());
      }
    }

    LOGGER.info("Find and Sort DatasetVersions By attribute test stop................");
  }

  /** Find public visibility datasetVersions */
  @Test
  public void findPublicDatasetVersionsTest() {
    LOGGER.info("Find Public DatasetVersions test start................................");

    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.DATASET_VERSION_VISIBILITY)
            .setValue(Value.newBuilder().setStringValue("PUBLIC").build())
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addPredicates(keyValueQuery)
            .setAscending(false)
            .setIdsOnly(false)
            .setSortKey("version")
            .build();

    FindDatasetVersions.Response response =
        datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        1,
        response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion Id not match with expected datasetVersion Id",
        datasetVersion4.getId(),
        response.getDatasetVersions(0).getId());

    LOGGER.info("Find Public DatasetVersions test stop................................");
  }
}
