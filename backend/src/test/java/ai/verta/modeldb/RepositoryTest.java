package ai.verta.modeldb;

import static ai.verta.modeldb.utils.TestConstants.RESOURCE_OWNER_ID;

import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.GetRepositoryRequest;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.RepositoryNamedIdentification;
import ai.verta.modeldb.versioning.SetRepository;
import ai.verta.modeldb.versioning.SetRepository.Response;
import ai.verta.modeldb.versioning.VersioningServiceGrpc;
import ai.verta.modeldb.versioning.VersioningServiceGrpc.VersioningServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
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
public class RepositoryTest {

  private static final Logger LOGGER = LogManager.getLogger(RepositoryTest.class);
  private static final String NAME = "repository_name";
  private static final String NAME_2 = "repository_name2";
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

  public static Long createRepository(VersioningServiceBlockingStub versioningServiceBlockingStub) {
    SetRepository setRepository =
        SetRepository.newBuilder()
            .setId(
                RepositoryIdentification.newBuilder()
                    .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME).build())
                    .build())
            .setRepository(Repository.newBuilder().setName(NAME))
            .build();
    Response result = versioningServiceBlockingStub.createRepository(setRepository);
    return result.getRepository().getId();
  }

  @Test
  public void repositoryTest() {
    LOGGER.info("Create and delete repository test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub);
    try {
      SetRepository setRepository =
          SetRepository.newBuilder()
              .setId(
                  RepositoryIdentification.newBuilder()
                      .setNamedId(
                          RepositoryNamedIdentification.newBuilder()
                              .setWorkspaceName("test1verta_gmail_com")
                              .setName(NAME_2)
                              .build())
                      .build())
              .setRepository(Repository.newBuilder().setName(NAME))
              .build();
      versioningServiceBlockingStub.createRepository(setRepository);
      Assert.fail();
    } catch (StatusRuntimeException e) {
      Assert.assertTrue(
          Code.PERMISSION_DENIED.equals(e.getStatus().getCode())
              || Code.ALREADY_EXISTS.equals(e.getStatus().getCode()));
    }

    // check id
    GetRepositoryRequest getRepositoryRequest =
        GetRepositoryRequest.newBuilder()
            .setId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();
    GetRepositoryRequest.Response getByIdResult =
        versioningServiceBlockingStub.getRepository(getRepositoryRequest);
    Assert.assertTrue(getByIdResult.hasRepository());

    SetRepository setRepository =
        SetRepository.newBuilder()
            .setId(
                RepositoryIdentification.newBuilder()
                    .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME).build())
                    .build())
            .setRepository(Repository.newBuilder().setName(NAME_2))
            .build();
    SetRepository.Response result = versioningServiceBlockingStub.updateRepository(setRepository);
    Assert.assertTrue(result.hasRepository());
    Assert.assertEquals(NAME_2, result.getRepository().getName());

    try {
      // check name
      getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(
                  RepositoryIdentification.newBuilder()
                      .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME)))
              .build();
      versioningServiceBlockingStub.getRepository(getRepositoryRequest);
      Assert.fail();
    } catch (StatusRuntimeException e) {
      Assert.assertEquals(Code.NOT_FOUND, e.getStatus().getCode());
    }

    getRepositoryRequest =
        GetRepositoryRequest.newBuilder()
            .setId(
                RepositoryIdentification.newBuilder()
                    .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME_2)))
            .build();
    GetRepositoryRequest.Response getByNameResult =
        versioningServiceBlockingStub.getRepository(getRepositoryRequest);
    Assert.assertTrue(getByNameResult.hasRepository());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      Assert.assertEquals(RESOURCE_OWNER_ID, getByNameResult.getRepository().getOwner());
    }

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("Create and delete repository test end................................");
  }
}
