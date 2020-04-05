package ai.verta.modeldb;

import static ai.verta.modeldb.utils.TestConstants.RESOURCE_OWNER_ID;
import static org.junit.Assert.assertEquals;

import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.GetRepositoryRequest;
import ai.verta.modeldb.versioning.ListRepositoriesRequest;
import ai.verta.modeldb.versioning.Pagination;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.RepositoryNamedIdentification;
import ai.verta.modeldb.versioning.SetRepository;
import ai.verta.modeldb.versioning.SetRepository.Response;
import ai.verta.modeldb.versioning.VersioningServiceGrpc;
import ai.verta.modeldb.versioning.VersioningServiceGrpc.VersioningServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
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
  public static final String NAME = "repository_name";
  public static final String NAME_2 = "repository_name2";
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

  public static Long createRepository(
      VersioningServiceBlockingStub versioningServiceBlockingStub, String repoName) {
    SetRepository setRepository =
        SetRepository.newBuilder()
            .setId(
                RepositoryIdentification.newBuilder()
                    .setNamedId(
                        RepositoryNamedIdentification.newBuilder().setName(repoName).build())
                    .build())
            .setRepository(Repository.newBuilder().setName(repoName))
            .build();
    Response result = versioningServiceBlockingStub.createRepository(setRepository);
    return result.getRepository().getId();
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

  @Test
  public void createDeleteRepositoryNegativeTest() {
    LOGGER.info("Create and delete repository negative test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);
    VersioningServiceBlockingStub versioningServiceBlockingStubClient2 =
        VersioningServiceGrpc.newBlockingStub(client2Channel);

    long id = createRepository(versioningServiceBlockingStub, NAME);
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
      if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
        assertEquals(Status.PERMISSION_DENIED.getCode(), e.getStatus().getCode());
      } else {
        assertEquals(Status.ALREADY_EXISTS.getCode(), e.getStatus().getCode());
      }
    }
    try {
      versioningServiceBlockingStubClient2.updateRepository(
          SetRepository.newBuilder()
              .setId(RepositoryIdentification.newBuilder().setRepoId(id))
              .setRepository(Repository.newBuilder().setName("new_name"))
              .build());
      if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
        Assert.fail();
      }
    } catch (StatusRuntimeException e) {
      assertEquals(Code.PERMISSION_DENIED, e.getStatus().getCode());
    }
    try {
      versioningServiceBlockingStubClient2.getRepository(
          GetRepositoryRequest.newBuilder()
              .setId(RepositoryIdentification.newBuilder().setRepoId(id))
              .build());
      if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
        Assert.fail();
      }
    } catch (StatusRuntimeException e) {
      assertEquals(Code.PERMISSION_DENIED, e.getStatus().getCode());
    }

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    try {
      versioningServiceBlockingStubClient2.deleteRepository(deleteRepository);
      if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
        Assert.fail();
      }
    } catch (StatusRuntimeException e) {
      checkEqualsAssert(e);
    }

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      Assert.assertTrue(deleteResult.getStatus());
    }

    try {
      deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
              .build();
      versioningServiceBlockingStub.deleteRepository(deleteRepository);
      Assert.fail();
    } catch (StatusRuntimeException e) {
      Assert.assertEquals(Code.NOT_FOUND, e.getStatus().getCode());
    }

    LOGGER.info("Create and delete repository negative test end................................");
  }

  @Test
  public void updateRepositoryByNameTest() {
    LOGGER.info("Update repository by name test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, NAME);

    try {
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

      GetRepositoryRequest getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(
                  RepositoryIdentification.newBuilder()
                      .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME_2)))
              .build();
      GetRepositoryRequest.Response getByNameResult =
          versioningServiceBlockingStub.getRepository(getRepositoryRequest);
      Assert.assertEquals(
          "Repository Id not match with expected repository Id",
          id,
          getByNameResult.getRepository().getId());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          NAME_2,
          getByNameResult.getRepository().getName());
      if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
        Assert.assertEquals(RESOURCE_OWNER_ID, getByNameResult.getRepository().getOwner());
      }
    } finally {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      Assert.assertTrue(deleteResult.getStatus());
    }

    LOGGER.info("Update repository by name test end................................");
  }

  @Test
  public void getRepositoryByIdTest() {
    LOGGER.info("Get repository by Id test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, NAME);

    // check id
    GetRepositoryRequest getRepositoryRequest =
        GetRepositoryRequest.newBuilder()
            .setId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();
    GetRepositoryRequest.Response getByIdResult =
        versioningServiceBlockingStub.getRepository(getRepositoryRequest);
    Assert.assertEquals(
        "Repository Id not match with expected repository Id",
        id,
        getByIdResult.getRepository().getId());

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());

    LOGGER.info("Get repository by Id test end................................");
  }

  @Test
  public void getRepositoryByNameTest() {
    LOGGER.info("Get repository by name test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, NAME);

    GetRepositoryRequest getRepositoryRequest =
        GetRepositoryRequest.newBuilder()
            .setId(
                RepositoryIdentification.newBuilder()
                    .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME)))
            .build();
    GetRepositoryRequest.Response getByNameResult =
        versioningServiceBlockingStub.getRepository(getRepositoryRequest);
    Assert.assertEquals(
        "Repository name not match with expected repository name",
        NAME,
        getByNameResult.getRepository().getName());
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

    LOGGER.info("Get repository by name test end................................");
  }

  @Test
  public void listRepositoryTest() {
    LOGGER.info("List repository test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long repoId1 = createRepository(versioningServiceBlockingStub, NAME);
    long repoId2 = createRepository(versioningServiceBlockingStub, NAME_2);
    Long[] repoIds = new Long[2];
    repoIds[0] = repoId1;
    repoIds[1] = repoId2;
    try {

      ListRepositoriesRequest listRepositoriesRequest =
          ListRepositoriesRequest.newBuilder().build();
      ListRepositoriesRequest.Response listRepositoriesResponse =
          versioningServiceBlockingStub.listRepositories(listRepositoriesRequest);
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          2,
          listRepositoriesResponse.getRepositoriesCount());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          NAME_2,
          listRepositoriesResponse.getRepositories(0).getName());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          NAME,
          listRepositoriesResponse.getRepositories(1).getName());

      listRepositoriesRequest =
          ListRepositoriesRequest.newBuilder()
              .setPagination(Pagination.newBuilder().setPageLimit(1).setPageNumber(1).build())
              .build();
      listRepositoriesResponse =
          versioningServiceBlockingStub.listRepositories(listRepositoriesRequest);
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          1,
          listRepositoriesResponse.getRepositoriesCount());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          NAME_2,
          listRepositoriesResponse.getRepositories(0).getName());
    } finally {
      for (long repoId : repoIds) {
        DeleteRepositoryRequest deleteRepository =
            DeleteRepositoryRequest.newBuilder()
                .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repoId))
                .build();
        DeleteRepositoryRequest.Response deleteResult =
            versioningServiceBlockingStub.deleteRepository(deleteRepository);
        Assert.assertTrue(deleteResult.getStatus());
      }
    }

    LOGGER.info("List repository test end................................");
  }
}
