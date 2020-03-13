package ai.verta.modeldb;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import ai.verta.artifactstore.ArtifactStoreGrpc;
import ai.verta.artifactstore.ArtifactStoreGrpc.ArtifactStoreBlockingStub;
import ai.verta.artifactstore.DeleteArtifact;
import ai.verta.artifactstore.GetArtifact;
import ai.verta.artifactstore.StoreArtifact;
import ai.verta.artifactstore.StoreArtifactWithStream;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.api.client.util.IOUtils;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
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
public class ArtifactStoreTest {

  private static final Logger LOGGER = Logger.getLogger(ArtifactStoreTest.class.getName());
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
  public void storeArtifactOnCloudTest() {
    LOGGER.info("store artifact on cloud test start................................");

    try {
      ArtifactStoreBlockingStub artifactStoreBlockingStub =
          ArtifactStoreGrpc.newBlockingStub(channel);

      StoreArtifact storeArtifact =
          StoreArtifact.newBuilder()
              .setKey("verta_logo.png")
              .setPath(
                  "https://www.verta.ai/static/logo-landing-424af27a5fc184c64225f604232db39e.png")
              .build();

      StoreArtifact.Response response = artifactStoreBlockingStub.storeArtifact(storeArtifact);

      String cloudFileKey = response.getArtifactStoreKey();
      String cloudFilePath = response.getArtifactStorePath();
      LOGGER.log(
          Level.INFO,
          "StoreArtifact.Response : \n cloudFileKey - "
              + cloudFileKey
              + " \n cloudFilePath - "
              + cloudFilePath);

      assumeTrue(cloudFileKey != null && !cloudFileKey.isEmpty());
      assumeTrue(cloudFilePath != null && !cloudFilePath.isEmpty());

      DeleteArtifact deleteArtifact = DeleteArtifact.newBuilder().setKey(cloudFileKey).build();
      DeleteArtifact.Response deleteArtifactResponse =
          artifactStoreBlockingStub.deleteArtifact(deleteArtifact);
      assertTrue(deleteArtifactResponse.getStatus());

    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warning(
          "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      fail();
    }

    LOGGER.info("store artifact on cloud test stop................................");
  }

  @Test
  public void storeStreamArtifactOnCloudTest() throws IOException {
    LOGGER.info("store stream artifact on cloud test start................................");

    try {
      ArtifactStoreBlockingStub artifactStoreBlockingStub =
          ArtifactStoreGrpc.newBlockingStub(channel);

      URL url =
          new URL("https://www.verta.ai/static/logo-landing-424af27a5fc184c64225f604232db39e.png");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      InputStream inputStream = connection.getInputStream();

      StoreArtifactWithStream storeArtifactRequest =
          StoreArtifactWithStream.newBuilder()
              .setKey("verta_logo_using_stream.png")
              .setClientFile(ByteString.readFrom(inputStream))
              .build();
      inputStream.close();

      LOGGER.info("StoreStreamArtifact Request called");

      StoreArtifactWithStream.Response response =
          artifactStoreBlockingStub.storeArtifactWithStream(storeArtifactRequest);

      String cloudFileKey = response.getCloudFileKey();
      String cloudFilePath = response.getCloudFilePath();
      LOGGER.log(
          Level.INFO,
          "StoreStreamArtifact.Response : \n cloudFileKey - "
              + cloudFileKey
              + " \n cloudFilePath - "
              + cloudFilePath);

      assumeTrue(cloudFileKey != null && !cloudFileKey.isEmpty());
      assumeTrue(cloudFilePath != null && !cloudFilePath.isEmpty());

      DeleteArtifact deleteArtifact = DeleteArtifact.newBuilder().setKey(cloudFileKey).build();
      DeleteArtifact.Response deleteArtifactResponse =
          artifactStoreBlockingStub.deleteArtifact(deleteArtifact);
      assertTrue(deleteArtifactResponse.getStatus());

    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.warning(
          "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      fail();
    }

    LOGGER.info("store stream artifact on cloud test stop................................");
  }

  @Test
  public void getArtifactFromCloudTest() {
    LOGGER.info("get artifact from cloud test start................................");

    try {
      ArtifactStoreBlockingStub artifactStoreBlockingStub =
          ArtifactStoreGrpc.newBlockingStub(channel);

      StoreArtifact storeArtifact =
          StoreArtifact.newBuilder()
              .setKey("verta_logo.png")
              .setPath(
                  "https://www.verta.ai/static/logo-landing-424af27a5fc184c64225f604232db39e.png")
              .build();

      StoreArtifact.Response response = artifactStoreBlockingStub.storeArtifact(storeArtifact);

      String cloudFileKey = response.getArtifactStoreKey();
      String cloudFilePath = response.getArtifactStorePath();
      LOGGER.log(
          Level.INFO,
          "StoreArtifact.Response : \n cloudFileKey - "
              + cloudFileKey
              + " \n cloudFilePath - "
              + cloudFilePath);

      assumeTrue(cloudFileKey != null && !cloudFileKey.isEmpty());
      assumeTrue(cloudFilePath != null && !cloudFilePath.isEmpty());

      GetArtifact getArtifactRequest = GetArtifact.newBuilder().setKey(cloudFileKey).build();
      GetArtifact.Response getArtifactResponse =
          artifactStoreBlockingStub.getArtifact(getArtifactRequest);
      ByteString responseByteString = getArtifactResponse.getContents();
      InputStream inputStream = responseByteString.newInput();

      String rootPath = System.getProperty("user.dir");
      FileOutputStream fileOutputStream =
          new FileOutputStream(new File(rootPath + File.separator + cloudFileKey));
      IOUtils.copy(inputStream, fileOutputStream);
      fileOutputStream.close();
      inputStream.close();

      File downloadedFile = new File(rootPath + File.separator + cloudFileKey);
      if (!downloadedFile.exists()) {
        fail("File not fount at download destination");
      }
      downloadedFile.delete();

      DeleteArtifact deleteArtifact = DeleteArtifact.newBuilder().setKey(cloudFileKey).build();
      DeleteArtifact.Response deleteArtifactResponse =
          artifactStoreBlockingStub.deleteArtifact(deleteArtifact);
      assertTrue(deleteArtifactResponse.getStatus());

    } catch (Exception e) {
      e.printStackTrace();
      Status status = Status.fromThrowable(e);
      LOGGER.warning(
          "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      fail();
    }

    LOGGER.info("get artifact from cloud test stop................................");
  }
}
