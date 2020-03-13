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
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
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
  private static final Logger LOGGER = Logger.getLogger(MetadataTest.class.getName());

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
            .setIdType(IDTypeEnum.IDType.VERSIONING_COMMIT)
            .setStringId("12abc345")
            .build();
    AddLabelsRequest addLabelsRequest2 =
        AddLabelsRequest.newBuilder().setId(id2).addLabels("Backend").addLabels("Frontend").build();
    AddLabelsRequest.Response addLabelsResponse2 = serviceBlockingStub.addLabels(addLabelsRequest2);
    assertTrue("Labels not persist successfully", addLabelsResponse2.getStatus());

    try {
      serviceBlockingStub.addLabels(addLabelsRequest1);
    } catch (StatusRuntimeException ex) {
      assertEquals(
          "Data already exists but the backend not return an expected response",
          Status.ALREADY_EXISTS.getCode(),
          ex.getStatus().getCode());
    }

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
}
