package ai.verta.modeldb.metadata;

import static org.junit.Assert.*;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.TestsInit;
import ai.verta.modeldb.authservice.*;
import ai.verta.modeldb.versioning.VersioningUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@Ignore(
    "We have decided to remove Metadata service (https://vertaai.atlassian.net/browse/VR-11520) so this test class will remove")
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MetadataTest extends TestsInit {
  private static final Logger LOGGER = LogManager.getLogger(MetadataTest.class.getName());

  @Test
  public void addDeleteLabelsTest() {
    LOGGER.info("Add & Delete labels test start................................");
    IdentificationType id1 =
        IdentificationType.newBuilder()
            .setIdType(IDTypeEnum.IDType.VERSIONING_REPOSITORY)
            .setIntId(1L)
            .build();
    AddLabelsRequest addLabelsRequest1 =
        AddLabelsRequest.newBuilder().setId(id1).addLabels("Backend").addLabels("Frontend").build();
    AddLabelsRequest.Response addLabelsResponse1 =
        metadataServiceBlockingStub.addLabels(addLabelsRequest1);
    assertTrue("Labels not persist successfully", addLabelsResponse1.getStatus());

    IdentificationType id2 =
        IdentificationType.newBuilder()
            .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT)
            .setStringId("12abc345")
            .build();
    AddLabelsRequest addLabelsRequest2 =
        AddLabelsRequest.newBuilder().setId(id2).addLabels("Backend").addLabels("Frontend").build();
    AddLabelsRequest.Response addLabelsResponse2 =
        metadataServiceBlockingStub.addLabels(addLabelsRequest2);
    assertTrue("Labels not persist successfully", addLabelsResponse2.getStatus());

    metadataServiceBlockingStub.addLabels(addLabelsRequest1);
    GetLabelsRequest getLabelsRequest = GetLabelsRequest.newBuilder().setId(id1).build();
    GetLabelsRequest.Response getLabelsResponse =
        metadataServiceBlockingStub.getLabels(getLabelsRequest);
    assertEquals(
        "Expected labels size not in response list", 2, getLabelsResponse.getLabelsCount());

    DeleteLabelsRequest deleteLabelsRequest =
        DeleteLabelsRequest.newBuilder()
            .setId(id1)
            .addLabels("Backend")
            .addLabels("Frontend")
            .build();
    DeleteLabelsRequest.Response deleteLabelsResponse =
        metadataServiceBlockingStub.deleteLabels(deleteLabelsRequest);
    assertTrue(deleteLabelsResponse.getStatus());

    deleteLabelsRequest =
        DeleteLabelsRequest.newBuilder()
            .setId(id2)
            .addLabels("Backend")
            .addLabels("Frontend")
            .build();
    deleteLabelsResponse = metadataServiceBlockingStub.deleteLabels(deleteLabelsRequest);
    assertTrue(deleteLabelsResponse.getStatus());

    LOGGER.info("Add & Delete labels test stop................................");
  }

  @Test
  public void getLabelsTest() {
    LOGGER.info("Get labels test start................................");
    IdentificationType id =
        IdentificationType.newBuilder()
            .setIdType(IDTypeEnum.IDType.VERSIONING_REPOSITORY)
            .setIntId(1L)
            .build();
    AddLabelsRequest addLabelsRequest =
        AddLabelsRequest.newBuilder().setId(id).addLabels("Backend").addLabels("Frontend").build();
    AddLabelsRequest.Response addLabelsResponse =
        metadataServiceBlockingStub.addLabels(addLabelsRequest);

    assertTrue("Labels not persist successfully", addLabelsResponse.getStatus());

    GetLabelsRequest getLabelsRequest = GetLabelsRequest.newBuilder().setId(id).build();
    GetLabelsRequest.Response getLabelsResponse =
        metadataServiceBlockingStub.getLabels(getLabelsRequest);
    assertEquals(
        "Expected labels size not in response list", 2, getLabelsResponse.getLabelsCount());
    assertTrue(
        "Expected label not found in response list",
        getLabelsResponse.getLabelsList().contains("Backend"));

    DeleteLabelsRequest deleteLabelsRequest =
        DeleteLabelsRequest.newBuilder()
            .setId(id)
            .addLabels("Backend")
            .addLabels("Frontend")
            .build();
    DeleteLabelsRequest.Response deleteLabelsResponse =
        metadataServiceBlockingStub.deleteLabels(deleteLabelsRequest);
    assertTrue(deleteLabelsResponse.getStatus());

    LOGGER.info("Get labels test stop................................");
  }

  @Test
  public void addDeleteLabelsWithComboRepoCommitBlobTest() {
    LOGGER.info(
        "Add & Delete labels for combo of repo, commit, blob test start................................");
    List<String> locations = new ArrayList<>();
    locations.add("modeldb");
    locations.add("test.txt");
    String compositeId =
        VersioningUtils.getVersioningCompositeId(1L, UUID.randomUUID().toString(), locations);
    IdentificationType id1 =
        IdentificationType.newBuilder()
            .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB)
            .setStringId(compositeId)
            .build();
    AddLabelsRequest addLabelsRequest2 =
        AddLabelsRequest.newBuilder().setId(id1).addLabels("Backend").addLabels("Frontend").build();
    AddLabelsRequest.Response addLabelsResponse2 =
        metadataServiceBlockingStub.addLabels(addLabelsRequest2);
    assertTrue("Labels not persist successfully", addLabelsResponse2.getStatus());

    DeleteLabelsRequest deleteLabelsRequest =
        DeleteLabelsRequest.newBuilder()
            .setId(id1)
            .addLabels("Backend")
            .addLabels("Frontend")
            .build();
    DeleteLabelsRequest.Response deleteLabelsResponse =
        metadataServiceBlockingStub.deleteLabels(deleteLabelsRequest);
    assertTrue(deleteLabelsResponse.getStatus());

    LOGGER.info(
        "Add & Delete labels for combo of repo, commit, blob  test stop................................");
  }

  @Test
  public void addDeleteLabelsWithComboRepoCommitTest() {
    LOGGER.info("Add & Delete labels for combo of repo, commit test start..........");
    String compositeId = 1L + "::" + UUID.randomUUID().toString();
    IdentificationType id1 =
        IdentificationType.newBuilder()
            .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT)
            .setStringId(compositeId)
            .build();
    AddLabelsRequest addLabelsRequest2 =
        AddLabelsRequest.newBuilder().setId(id1).addLabels("Backend").addLabels("Frontend").build();
    AddLabelsRequest.Response addLabelsResponse2 =
        metadataServiceBlockingStub.addLabels(addLabelsRequest2);
    assertTrue("Labels not persist successfully", addLabelsResponse2.getStatus());

    DeleteLabelsRequest deleteLabelsRequest =
        DeleteLabelsRequest.newBuilder()
            .setId(id1)
            .addLabels("Backend")
            .addLabels("Frontend")
            .build();
    DeleteLabelsRequest.Response deleteLabelsResponse =
        metadataServiceBlockingStub.deleteLabels(deleteLabelsRequest);
    assertTrue(deleteLabelsResponse.getStatus());

    LOGGER.info("Add & Delete labels for combo of repo, commit  test stop.........");
  }

  @Test
  public void addDeleteKeyValuePropertiesTest() {
    LOGGER.info("Add & Delete keyValue properties test start................................");
    String attrKey = "attr_key_1";
    String value = "att_value";
    String propertyName = ModelDBConstants.ATTRIBUTES;
    String id = "REGISTERED_MODEL_" + propertyName + "_" + attrKey;
    IdentificationType id1 = IdentificationType.newBuilder().setStringId(id).build();
    AddKeyValuePropertiesRequest addKeyValuePropertiessRequest1 =
        AddKeyValuePropertiesRequest.newBuilder()
            .setId(id1)
            .addKeyValueProperty(
                KeyValueStringProperty.newBuilder().setKey(attrKey).setValue(value).build())
            .setPropertyName(propertyName)
            .build();
    metadataServiceBlockingStub.addKeyValueProperties(addKeyValuePropertiessRequest1);
    assertTrue(true);

    GetKeyValuePropertiesRequest getKeyValuePropertiessRequest =
        GetKeyValuePropertiesRequest.newBuilder()
            .setId(id1)
            .addKeys(attrKey)
            .setPropertyName(propertyName)
            .build();
    GetKeyValuePropertiesRequest.Response getKeyValuePropertiessResponse =
        metadataServiceBlockingStub.getKeyValueProperties(getKeyValuePropertiessRequest);
    assertEquals(
        "Response value count not match with expected value count",
        1,
        getKeyValuePropertiessResponse.getKeyValuePropertyCount());
    assertEquals(
        "Response value not match with expected value ",
        value,
        getKeyValuePropertiessResponse.getKeyValueProperty(0).getValue());

    DeleteKeyValuePropertiesRequest deleteKeyValuePropertiessRequest =
        DeleteKeyValuePropertiesRequest.newBuilder()
            .setId(id1)
            .addKeys(attrKey)
            .setPropertyName(propertyName)
            .build();
    metadataServiceBlockingStub.deleteKeyValueProperties(deleteKeyValuePropertiessRequest);
    assertTrue(true);

    GetKeyValuePropertiesRequest.Response response =
        metadataServiceBlockingStub.getKeyValueProperties(getKeyValuePropertiessRequest);
    assertEquals(
        "response keyValue count not match with expected keyValue count",
        0,
        response.getKeyValuePropertyCount());

    LOGGER.info("Add & Delete keyValue properties test stop................................");
  }
}
