package ai.verta.modeldb;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetUsersFuzzy;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class FindDatasetEntitiesTest extends ModeldbTestSetup {

  private static final Logger LOGGER = LogManager.getLogger(FindDatasetEntitiesTest.class);

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

  @Before
  public void createEntities() {
    initializedChannelBuilderAndExternalServiceStubs();

    if (isRunningIsolated()) {
      setupMockUacEndpoints(uac);
    }

    // Create all entities
    createDatasetEntities();
    createDatasetVersionEntities();
  }

  @After
  public void removeEntities() {
    for (DatasetVersion datasetVersion : datasetVersionMap.values()) {
      DeleteDatasetVersion deleteDatasetVersion =
          DeleteDatasetVersion.newBuilder()
              .setDatasetId(datasetVersion.getDatasetId())
              .setId(datasetVersion.getId())
              .build();
      DeleteDatasetVersion.Response deleteDatasetResponse =
          datasetVersionServiceStub.deleteDatasetVersion(deleteDatasetVersion);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
    }

    DeleteDatasets deleteDatasets =
        DeleteDatasets.newBuilder().addAllIds(datasetMap.keySet()).build();
    DeleteDatasets.Response deleteDatasetsResponse =
        datasetServiceStub.deleteDatasets(deleteDatasets);
    LOGGER.info("Datasets deleted successfully");
    LOGGER.info(deleteDatasetsResponse.toString());
    assertTrue(deleteDatasetsResponse.getStatus());

    dataset1 = null;
    dataset2 = null;
    dataset3 = null;
    dataset4 = null;
    datasetMap = new HashMap<>();

    datasetVersion1 = null;
    datasetVersion2 = null;
    datasetVersion3 = null;
    datasetVersion4 = null;
    datasetVersionMap = new HashMap<>();
  }

  private void createDatasetEntities() {
    if (isRunningIsolated()) {
      var resourcesResponse =
          GetResources.Response.newBuilder()
              .addItem(
                  GetResourcesResponseItem.newBuilder()
                      .setResourceId("1")
                      .setWorkspaceId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                      .setOwnerId(testUser1.getVertaInfo().getDefaultWorkspaceId())
                      .build())
              .build();
      when(collaboratorBlockingMock.getResources(any())).thenReturn(resourcesResponse);
    }

    // Create two dataset of above dataset
    CreateDataset createDatasetRequest =
        DatasetTest.getDatasetRequestForOtherTests("Dataset-1-" + new Date().getTime());
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
            .addTags("A1")
            .addTags("A2")
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
    createDatasetRequest =
        DatasetTest.getDatasetRequestForOtherTests("Dataset-2-" + new Date().getTime());
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
    createDatasetRequest =
        DatasetTest.getDatasetRequestForOtherTests("Dataset-3-" + new Date().getTime());
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
    createDatasetRequest =
        DatasetTest.getDatasetRequestForOtherTests("Dataset-4-" + new Date().getTime());
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

  private void createDatasetVersionEntities() {
    // Create two datasetVersion of above dataset
    CreateDatasetVersion createDatasetVersionRequest =
        DatasetVersionTest.getDatasetVersionRequest(dataset1.getId());
    KeyValue attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setStringValue("0.012").build())
            .build();
    KeyValue attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setStringValue("0.99").build())
            .build();
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("A1")
            .addTags("A2")
            .build();
    CreateDatasetVersion.Response createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    datasetVersion1 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");

    // datasetVersion2 of above dataset
    createDatasetVersionRequest = DatasetVersionTest.getDatasetVersionRequest(dataset1.getId());
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setStringValue("0.31").build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setStringValue("0.31").build())
            .build();
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("A1")
            .addTags("A3")
            .addTags("A4")
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    datasetVersion2 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");

    // datasetVersion3 of above dataset
    createDatasetVersionRequest = DatasetVersionTest.getDatasetVersionRequest(dataset1.getId());
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setStringValue("0.6543210").build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setStringValue("0.6543210").build())
            .build();
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("A1")
            .addTags("A5")
            .addTags("A6")
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    datasetVersion3 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");

    // datasetVersion4 of above dataset
    createDatasetVersionRequest = DatasetVersionTest.getDatasetVersionRequest(dataset1.getId());
    attribute1 =
        KeyValue.newBuilder()
            .setKey("attribute_1")
            .setValue(Value.newBuilder().setStringValue("1.00").build())
            .build();
    attribute2 =
        KeyValue.newBuilder()
            .setKey("attribute_2")
            .setValue(Value.newBuilder().setStringValue("0.001212").build())
            .build();
    createDatasetVersionRequest =
        createDatasetVersionRequest
            .toBuilder()
            .addAttributes(attribute1)
            .addAttributes(attribute2)
            .addTags("A5")
            .addTags("A7")
            .addTags("A8")
            .build();
    createDatasetVersionResponse =
        datasetVersionServiceStub.createDatasetVersion(createDatasetVersionRequest);
    datasetVersion4 = createDatasetVersionResponse.getDatasetVersion();
    LOGGER.info("DatasetVersion created successfully");

    datasetVersionMap.put(datasetVersion1.getId(), datasetVersion1);
    datasetVersionMap.put(datasetVersion2.getId(), datasetVersion2);
    datasetVersionMap.put(datasetVersion3.getId(), datasetVersion3);
    datasetVersionMap.put(datasetVersion4.getId(), datasetVersion4);
  }

  /** Validation check for the predicate value with empty string which is not valid */
  @Test
  public void findDatasetPredicateValueEmptyNegativeTest() {
    LOGGER.info("FindDatasets predicate value is empty negative test start.........");
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
    List<Dataset> expectedDatasets = new ArrayList<>();
    for (Dataset dataset : response.getDatasetsList()) {
      if (datasetMap.containsKey(dataset.getId())) {
        expectedDatasets.add(dataset);
      }
    }
    LOGGER.info("FindDatasets Response : " + expectedDatasets.size());
    assertEquals("Dataset count not match with expected dataset count", 3, expectedDatasets.size());

    for (Dataset fetchedDataset : expectedDatasets) {
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
    dataset2 =
        dataset2
            .toBuilder()
            .setTimeUpdated(response.getDatasetsList().get(0).getTimeUpdated())
            .setVersionNumber(response.getDatasetsList().get(0).getVersionNumber())
            .build();
    assertEquals(
        "Dataset not match with expected dataset", dataset2, response.getDatasetsList().get(0));
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    LOGGER.info("FindDatasets by multiple attribute condition test stop..................");
  }

  /** Find dataset with value of metrics.accuracy >= 0.6543210 & tags == A7 */
  @Test
  public void findDatasetsByMetricsAndTagsTest() {
    LOGGER.info("FindDatasets by metrics and tags test start................................");

    List<KeyValueQuery> predicates = new ArrayList<>();
    Value stringValue = Value.newBuilder().setStringValue("A7").build();
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
  public void findDatasetsByDatasetTimeUpdatedTest() {
    LOGGER.info("FindDatasets By Dataset TimeUpdated test start................................");

    GetDatasetById getDatasetById = GetDatasetById.newBuilder().setId(dataset4.getId()).build();
    GetDatasetById.Response getDatasetByIdResponse =
        datasetServiceStub.getDatasetById(getDatasetById);
    assertEquals(
        "Dataset not match with expected dataset",
        dataset4.getId(),
        getDatasetByIdResponse.getDataset().getId());
    dataset4 = getDatasetByIdResponse.getDataset();
    Value numberValue = Value.newBuilder().setNumberValue(dataset4.getTimeUpdated()).build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.TIME_UPDATED)
            .setValue(numberValue)
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

    LOGGER.info("FindDatasets By Dataset TimeUpdated test stop................................");
  }

  /** FInd Datasets by attribute with pagination */
  @Test
  public void findDatasetsByAttributeWithPaginationTest() {
    LOGGER.info(
        "FindDatasets by attribute with pagination test start................................");
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
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("FindDatasets not support the observation.attributes test stop............");
  }

  /** Find datasets with value of tags */
  @Test
  public void findDatasetsByTagsTest() {
    LOGGER.info("FindDatasets by tags test start................................");
    // get dataset with value of tags == test_tag_123
    Value stringValue1 = Value.newBuilder().setStringValue("A1").build();
    KeyValueQuery keyValueQueryTag1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue1)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    // get datasetRun with value of tags == test_tag_456
    Value stringValue2 = Value.newBuilder().setStringValue("A4").build();
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
        dataset2.getId(),
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
        dataset3 =
            dataset3
                .toBuilder()
                .setTimeUpdated(dataset.getTimeUpdated())
                .setVersionNumber(dataset.getVersionNumber())
                .build();
        assertEquals("Dataset not match with expected dataset", dataset3, dataset);
        assertEquals(
            "Dataset Id not match with expected dataset Id", dataset3.getId(), dataset.getId());
      } else if (index == 1) {
        dataset2 =
            dataset2
                .toBuilder()
                .setTimeUpdated(dataset.getTimeUpdated())
                .setVersionNumber(dataset.getVersionNumber())
                .build();
        assertEquals("Dataset not match with expected dataset", dataset2, dataset);
        assertEquals(
            "Dataset Id not match with expected dataset Id", dataset2.getId(), dataset.getId());
      } else if (index == 2) {
        dataset1 =
            dataset1
                .toBuilder()
                .setTimeUpdated(dataset.getTimeUpdated())
                .setVersionNumber(dataset.getVersionNumber())
                .build();
        assertEquals("Dataset not match with expected dataset", dataset1, dataset);
        assertEquals(
            "Dataset Id not match with expected dataset Id", dataset1.getId(), dataset.getId());
      }
    }

    LOGGER.info("Find and sort Datasets by attributes test start................................");
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
            .setDatasetId(dataset1.getId())
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
            .setDatasetId(dataset1.getId())
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
            .setDatasetId(dataset1.getId())
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
    Value numValue = Value.newBuilder().setStringValue("0.6543210").build();
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

    List<DatasetVersion> expectedDatasetVersions = new ArrayList<>();
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsList());
    for (DatasetVersion datasetVersion : response.getDatasetVersionsList()) {
      if (datasetVersionMap.containsKey(datasetVersion.getId())) {
        expectedDatasetVersions.add(datasetVersion);
      }
    }
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        3,
        expectedDatasetVersions.size());

    numValue = Value.newBuilder().setStringValue("0.6543210").build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.NE)
            .build();

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(dataset1.getId())
            .addPredicates(keyValueQuery)
            .build();

    response = datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsList());
    expectedDatasetVersions = new ArrayList<>();
    for (DatasetVersion datasetVersion : response.getDatasetVersionsList()) {
      if (datasetVersionMap.containsKey(datasetVersion.getId())) {
        expectedDatasetVersions.add(datasetVersion);
      }
    }
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        3,
        expectedDatasetVersions.size());

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
    Value numValue = Value.newBuilder().setStringValue("0.6543210").build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.LTE)
            .build();
    predicates.add(keyValueQuery);

    numValue = Value.newBuilder().setStringValue("0.31").build();
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
    assertEquals(
        "DatasetVersion not match with expected datasetVersion",
        datasetVersion2,
        response.getDatasetVersionsList().get(0));
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    LOGGER.info("FindDatasetVersions by multiple attribute condition test stop..............");
  }

  /** Find datasetVersion with value of metrics.accuracy >= 0.6543210 & tags == A7 */
  @Test
  public void findDatasetVersionsByMetricsAndTagsTest() {
    LOGGER.info("FindDatasetVersions by metrics and tags test start.........");

    List<KeyValueQuery> predicates = new ArrayList<>();
    Value stringValue = Value.newBuilder().setStringValue("A7").build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    predicates.add(keyValueQuery);

    Value numValue = Value.newBuilder().setStringValue("0.6543210").build();
    KeyValueQuery keyValueQuery2 =
        KeyValueQuery.newBuilder()
            .setKey("attributes.attribute_1")
            .setValue(numValue)
            .setOperator(OperatorEnum.Operator.GTE)
            .build();
    predicates.add(keyValueQuery2);

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(dataset1.getId())
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

    Value numberValue = Value.newBuilder().setNumberValue(datasetVersion4.getTimeUpdated()).build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.DATE_UPDATED)
            .setValue(numberValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(dataset1.getId())
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

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(dataset1.getId())
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .setAscending(true)
            .build();

    response = datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        4,
        response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersionRun not match with expected datasetVersionRun",
        datasetVersion1.getId(),
        response.getDatasetVersionsList().get(0).getId());
    assertEquals(
        "DatasetVersionRun not match with expected datasetVersionRun",
        datasetVersion4.getId(),
        response.getDatasetVersionsList().get(3).getId());
    assertEquals(
        "Total records count not matched with expected records count",
        4,
        response.getTotalRecords());

    numberValue = Value.newBuilder().setNumberValue(datasetVersion4.getVersion()).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey(ModelDBConstants.VERSION)
            .setValue(numberValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(dataset1.getId())
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .addPredicates(keyValueQuery)
            .build();

    response = datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
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

  /** Find datasetVersion with value of tags */
  @Test
  public void findDatasetVersionsByTagsTest() {
    LOGGER.info("FindDatasetVersions by tags test start................................");

    Value stringValue1 = Value.newBuilder().setStringValue("A1").build();
    KeyValueQuery keyValueQueryTag1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue1)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    // get datasetVersionRun with value of tags == test_tag_456
    Value stringValue2 = Value.newBuilder().setStringValue("A5").build();
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

    stringValue1 = Value.newBuilder().setStringValue("A11111").build();
    keyValueQueryTag1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue1)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(dataset1.getId())
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .addPredicates(keyValueQueryTag1)
            .build();

    response = datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        0,
        response.getDatasetVersionsCount());
    assertEquals(
        "Total records count not matched with expected records count",
        0,
        response.getTotalRecords());

    stringValue1 = Value.newBuilder().setStringValue("A2").build();
    keyValueQueryTag1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue1)
            .setOperator(OperatorEnum.Operator.NE)
            .build();

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .addPredicates(keyValueQueryTag1)
            .build();

    response = datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        3,
        response.getDatasetVersionsCount());
    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());

    stringValue1 = Value.newBuilder().setStringValue("A2").build();
    keyValueQueryTag1 =
        KeyValueQuery.newBuilder()
            .setKey("tags")
            .setValue(stringValue1)
            .setOperator(OperatorEnum.Operator.NOT_CONTAIN)
            .build();

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(dataset1.getId())
            .addAllDatasetVersionIds(datasetVersionMap.keySet())
            .addPredicates(keyValueQueryTag1)
            .build();

    response = datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        3,
        response.getDatasetVersionsCount());
    assertEquals(
        "Total records count not matched with expected records count",
        3,
        response.getTotalRecords());

    LOGGER.info("FindDatasetVersions by tags test stop................................");
  }

  /** Find datasetVersions by workspace */
  @Test
  public void findDatasetVersionsByWorkspaceTest() {
    LOGGER.info("FindDatasetVersions by workspace test start................................");

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder().setDatasetId(dataset1.getId()).build();

    FindDatasetVersions.Response response =
        datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        datasetVersionMap.size(),
        response.getDatasetVersionsCount());
    assertEquals(
        "Total records count not matched with expected records count",
        datasetVersionMap.size(),
        response.getTotalRecords());

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(dataset1.getId())
            .addDatasetVersionIds(datasetVersion1.getId())
            .build();

    response = datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsCount());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        1,
        response.getDatasetVersionsCount());
    assertEquals(
        "Total records count not matched with expected records count",
        1,
        response.getTotalRecords());

    LOGGER.info("FindDatasetVersions by workspace test stop................................");
  }

  @Test
  public void findDatasetsWithMarkedAsProtectedRepositoryTest() {
    LOGGER.info("FindDatasets test start................................");

    long id =
        RepositoryTest.createRepository(
            versioningServiceBlockingStub, "Repo-" + new Date().getTime());

    FindDatasets findDatasets = FindDatasets.newBuilder().build();

    FindDatasets.Response response = datasetServiceStub.findDatasets(findDatasets);
    List<Dataset> expectedDatasets = new ArrayList<>();
    for (Dataset dataset : response.getDatasetsList()) {
      if (datasetMap.containsKey(dataset.getId())) {
        expectedDatasets.add(dataset);
      }
    }
    LOGGER.info("FindDatasets Response : " + expectedDatasets.size());

    assertEquals(
        "Total records count not matched with expected records count",
        datasetMap.size(),
        expectedDatasets.size());

    expectedDatasets.forEach(
        dataset -> {
          if (dataset.getId().equals(String.valueOf(id))) {
            fail("Regular repository should not visible in protected repository list");
          }
        });

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("FindDatasets test stop................................");
  }

  @Test
  public void findDatasetVersionsByFuzzyOwnerTest() {
    LOGGER.info(
        "FindDatasetVersions by owner fuzzy search test start................................");
    if (!testConfig.hasAuth()) {
      assertTrue(true);
      return;
    }
    if (isRunningIsolated()) {
      when(uacBlockingMock.getUsersFuzzy(any()))
          .thenReturn(
              GetUsersFuzzy.Response.newBuilder()
                  .addAllUserInfos(List.of(testUser1, testUser2))
                  .build());
    }

    String testUser1UserName = testUser1.getVertaInfo().getUsername();

    // get datasetVersion with value of attributes.attribute_1 <= 0.6543210
    Value stringValue =
        Value.newBuilder().setStringValue(testUser1UserName.substring(0, 2)).build();
    KeyValueQuery keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("commit.author")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.CONTAIN)
            .build();

    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(dataset1.getId())
            .addPredicates(keyValueQuery)
            .build();

    FindDatasetVersions.Response response =
        datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsList());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        4,
        response.getDatasetVersionsList().size());

    assertEquals(
        "Total records count not matched with expected records count",
        4,
        response.getTotalRecords());

    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("commit.author")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.NOT_CONTAIN)
            .build();
    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(dataset1.getId())
            .addPredicates(keyValueQuery)
            .build();

    response = datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    assertEquals(
        "Total records count not matched with expected records count",
        0,
        response.getTotalRecords());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        0,
        response.getDatasetVersionsCount());

    stringValue = Value.newBuilder().setStringValue("asdasdasd").build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("commit.author")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.CONTAIN)
            .build();

    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(dataset1.getId())
            .addPredicates(keyValueQuery)
            .build();

    if (isRunningIsolated()) {
      when(uacBlockingMock.getUsersFuzzy(any()))
          .thenReturn(GetUsersFuzzy.Response.newBuilder().build());
    }

    response = datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    LOGGER.info("FindDatasetVersions Response : " + response.getDatasetVersionsList());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        0,
        response.getDatasetVersionsList().size());

    stringValue = Value.newBuilder().setStringValue(dataset1.getOwner()).build();
    keyValueQuery =
        KeyValueQuery.newBuilder()
            .setKey("commit.author")
            .setValue(stringValue)
            .setOperator(OperatorEnum.Operator.EQ)
            .build();
    findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(dataset1.getId())
            .addPredicates(keyValueQuery)
            .build();

    response = datasetVersionServiceStub.findDatasetVersions(findDatasetVersions);
    assertEquals(
        "Total records count not matched with expected records count",
        4,
        response.getTotalRecords());
    assertEquals(
        "DatasetVersion count not match with expected datasetVersion count",
        4,
        response.getDatasetVersionsCount());

    LOGGER.info(
        "FindDatasetVersions by owner fuzzy search test stop ................................");
  }
}
