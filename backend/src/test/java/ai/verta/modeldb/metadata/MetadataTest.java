package ai.verta.modeldb.metadata;

import static org.junit.Assert.*;

import ai.verta.modeldb.App;
import ai.verta.modeldb.AuthClientInterceptor;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.metadata.MetadataServiceGrpc.MetadataServiceBlockingStub;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.VersioningUtils;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
public class MetadataTest {
  private static final Logger LOGGER = LogManager.getLogger(MetadataTest.class.getName());

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;
  private static String serverName = InProcessServerBuilder.generateName();
  private static InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder channelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static AuthClientInterceptor authClientInterceptor;

  @SuppressWarnings("unchecked")
  @BeforeClass
  public static void setServerAndService() throws Exception {

    Map<String, Object> propertiesMap =
        ModelDBUtils.readYamlProperties(System.getenv(ModelDBConstants.VERTA_MODELDB_CONFIG));
    Map<String, Object> testPropMap = (Map<String, Object>) propertiesMap.get("test");
    Map<String, Object> databasePropMap = (Map<String, Object>) testPropMap.get("test-database");

    App app = App.getInstance();
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
  }

  @Before
  public void initializeChannel() throws IOException {
    grpcCleanup.register(serverBuilder.build().start());
    channel = grpcCleanup.register(channelBuilder.maxInboundMessageSize(1024).build());
  }

  @Test
  public void addDeleteLabelsTest() {
    LOGGER.info("Add & Delete labels test start................................");

    MetadataServiceBlockingStub serviceBlockingStub = MetadataServiceGrpc.newBlockingStub(channel);

    IdentificationType id1 =
        IdentificationType.newBuilder()
            .setIdType(IDTypeEnum.IDType.VERSIONING_REPOSITORY)
            .setIntId(1L)
            .build();
    AddLabelsRequest addLabelsRequest1 =
        AddLabelsRequest.newBuilder().setId(id1).addLabels("Backend").addLabels("Frontend").build();
    AddLabelsRequest.Response addLabelsResponse1 = serviceBlockingStub.addLabels(addLabelsRequest1);
    assertTrue("Labels not persist successfully", addLabelsResponse1.getStatus());

    IdentificationType id2 =
        IdentificationType.newBuilder()
            .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT)
            .setStringId("12abc345")
            .build();
    AddLabelsRequest addLabelsRequest2 =
        AddLabelsRequest.newBuilder().setId(id2).addLabels("Backend").addLabels("Frontend").build();
    AddLabelsRequest.Response addLabelsResponse2 = serviceBlockingStub.addLabels(addLabelsRequest2);
    assertTrue("Labels not persist successfully", addLabelsResponse2.getStatus());

    serviceBlockingStub.addLabels(addLabelsRequest1);
    GetLabelsRequest getLabelsRequest = GetLabelsRequest.newBuilder().setId(id1).build();
    GetLabelsRequest.Response getLabelsResponse = serviceBlockingStub.getLabels(getLabelsRequest);
    assertEquals(
        "Expected labels size not in response list", 2, getLabelsResponse.getLabelsCount());

    DeleteLabelsRequest deleteLabelsRequest =
        DeleteLabelsRequest.newBuilder()
            .setId(id1)
            .addLabels("Backend")
            .addLabels("Frontend")
            .build();
    DeleteLabelsRequest.Response deleteLabelsResponse =
        serviceBlockingStub.deleteLabels(deleteLabelsRequest);
    assertTrue(deleteLabelsResponse.getStatus());

    deleteLabelsRequest =
        DeleteLabelsRequest.newBuilder()
            .setId(id2)
            .addLabels("Backend")
            .addLabels("Frontend")
            .build();
    deleteLabelsResponse = serviceBlockingStub.deleteLabels(deleteLabelsRequest);
    assertTrue(deleteLabelsResponse.getStatus());

    LOGGER.info("Add & Delete labels test stop................................");
  }

  @Test
  public void getLabelsTest() {
    LOGGER.info("Get labels test start................................");

    MetadataServiceBlockingStub serviceBlockingStub = MetadataServiceGrpc.newBlockingStub(channel);

    IdentificationType id =
        IdentificationType.newBuilder()
            .setIdType(IDTypeEnum.IDType.VERSIONING_REPOSITORY)
            .setIntId(1L)
            .build();
    AddLabelsRequest addLabelsRequest =
        AddLabelsRequest.newBuilder().setId(id).addLabels("Backend").addLabels("Frontend").build();
    AddLabelsRequest.Response addLabelsResponse = serviceBlockingStub.addLabels(addLabelsRequest);

    assertTrue("Labels not persist successfully", addLabelsResponse.getStatus());

    GetLabelsRequest getLabelsRequest = GetLabelsRequest.newBuilder().setId(id).build();
    GetLabelsRequest.Response getLabelsResponse = serviceBlockingStub.getLabels(getLabelsRequest);
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
        serviceBlockingStub.deleteLabels(deleteLabelsRequest);
    assertTrue(deleteLabelsResponse.getStatus());

    LOGGER.info("Get labels test stop................................");
  }

  @Test
  public void addDeleteLabelsWithComboRepoCommitBlobTest() {
    LOGGER.info(
        "Add & Delete labels for combo of repo, commit, blob test start................................");

    MetadataServiceBlockingStub serviceBlockingStub = MetadataServiceGrpc.newBlockingStub(channel);

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
    AddLabelsRequest.Response addLabelsResponse2 = serviceBlockingStub.addLabels(addLabelsRequest2);
    assertTrue("Labels not persist successfully", addLabelsResponse2.getStatus());

    DeleteLabelsRequest deleteLabelsRequest =
        DeleteLabelsRequest.newBuilder()
            .setId(id1)
            .addLabels("Backend")
            .addLabels("Frontend")
            .build();
    DeleteLabelsRequest.Response deleteLabelsResponse =
        serviceBlockingStub.deleteLabels(deleteLabelsRequest);
    assertTrue(deleteLabelsResponse.getStatus());

    LOGGER.info(
        "Add & Delete labels for combo of repo, commit, blob  test stop................................");
  }

  @Test
  public void addDeleteLabelsWithComboRepoCommitTest() {
    LOGGER.info("Add & Delete labels for combo of repo, commit test start..........");

    MetadataServiceBlockingStub serviceBlockingStub = MetadataServiceGrpc.newBlockingStub(channel);

    String compositeId = 1L + "::" + UUID.randomUUID().toString();
    IdentificationType id1 =
        IdentificationType.newBuilder()
            .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT)
            .setStringId(compositeId)
            .build();
    AddLabelsRequest addLabelsRequest2 =
        AddLabelsRequest.newBuilder().setId(id1).addLabels("Backend").addLabels("Frontend").build();
    AddLabelsRequest.Response addLabelsResponse2 = serviceBlockingStub.addLabels(addLabelsRequest2);
    assertTrue("Labels not persist successfully", addLabelsResponse2.getStatus());

    DeleteLabelsRequest deleteLabelsRequest =
        DeleteLabelsRequest.newBuilder()
            .setId(id1)
            .addLabels("Backend")
            .addLabels("Frontend")
            .build();
    DeleteLabelsRequest.Response deleteLabelsResponse =
        serviceBlockingStub.deleteLabels(deleteLabelsRequest);
    assertTrue(deleteLabelsResponse.getStatus());

    LOGGER.info("Add & Delete labels for combo of repo, commit  test stop.........");
  }

  @Test
  public void addDeleteKeyValuePropertiesTest() {
    LOGGER.info("Add & Delete keyValue properties test start................................");

    MetadataServiceBlockingStub serviceBlockingStub = MetadataServiceGrpc.newBlockingStub(channel);

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
    serviceBlockingStub.addKeyValueProperties(addKeyValuePropertiessRequest1);
    assertTrue(true);

    GetKeyValuePropertiesRequest getKeyValuePropertiessRequest =
        GetKeyValuePropertiesRequest.newBuilder()
            .setId(id1)
            .addKeys(attrKey)
            .setPropertyName(propertyName)
            .build();
    GetKeyValuePropertiesRequest.Response getKeyValuePropertiessResponse =
        serviceBlockingStub.getKeyValueProperties(getKeyValuePropertiessRequest);
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
    serviceBlockingStub.deleteKeyValueProperties(deleteKeyValuePropertiessRequest);
    assertTrue(true);

    GetKeyValuePropertiesRequest.Response response =
        serviceBlockingStub.getKeyValueProperties(getKeyValuePropertiessRequest);
    assertEquals(
        "response keyValue count not match with expected keyValue count",
        0,
        response.getKeyValuePropertyCount());

    LOGGER.info("Add & Delete keyValue properties test stop................................");
  }
}
