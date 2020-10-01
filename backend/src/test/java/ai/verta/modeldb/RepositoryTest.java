package ai.verta.modeldb;

import static ai.verta.modeldb.utils.TestConstants.RESOURCE_OWNER_ID;
import static org.junit.Assert.*;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.EntitiesEnum;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.OperatorEnum;
import ai.verta.common.Pagination;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.cron_jobs.CronJobUtils;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.metadata.AddLabelsRequest;
import ai.verta.modeldb.metadata.DeleteLabelsRequest;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.metadata.IdentificationType;
import ai.verta.modeldb.metadata.MetadataServiceGrpc;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.CreateCommitRequest;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.FindRepositories;
import ai.verta.modeldb.versioning.GetBranchRequest;
import ai.verta.modeldb.versioning.GetRepositoryRequest;
import ai.verta.modeldb.versioning.ListRepositoriesRequest;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.RepositoryNamedIdentification;
import ai.verta.modeldb.versioning.SetRepository;
import ai.verta.modeldb.versioning.SetRepository.Response;
import ai.verta.modeldb.versioning.SetTagRequest;
import ai.verta.modeldb.versioning.VersioningServiceGrpc;
import ai.verta.modeldb.versioning.VersioningServiceGrpc.VersioningServiceBlockingStub;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.GetUser;
import ai.verta.uac.UACServiceGrpc;
import ai.verta.uac.UserInfo;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
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
  public static final String NAME_3 = "repository_name3";
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

    ModelDBHibernateUtil.runLiquibaseMigration(databasePropMap);
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

  public static Long createRepository(
      VersioningServiceBlockingStub versioningServiceBlockingStub, String repoName) {
    SetRepository setRepository = getSetRepositoryRequest(repoName);
    Response result = versioningServiceBlockingStub.createRepository(setRepository);
    return result.getRepository().getId();
  }

  private static SetRepository getSetRepositoryRequest(String repoName) {
    return SetRepository.newBuilder()
        .setId(
            RepositoryIdentification.newBuilder()
                .setNamedId(RepositoryNamedIdentification.newBuilder().setName(repoName).build())
                .build())
        .setRepository(
            Repository.newBuilder()
                .setName(repoName)
                .setDescription("This is test repository description"))
        .build();
  }

  private void checkEqualsAssert(StatusRuntimeException e) {
    Status status = Status.fromThrowable(e);
    LOGGER.warn("Error Code : " + status.getCode() + " Description : " + status.getDescription());
    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      assertTrue(
          Status.PERMISSION_DENIED.getCode() == status.getCode()
              || Status.NOT_FOUND.getCode()
                  == status.getCode()); // because of shadow delete the response could be 403 or 404
    } else {
      assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
    }
  }

  public static List<KeyValue> getAttributeList() {
    List<KeyValue> attributeList = new ArrayList<>();
    Value intValue = Value.newBuilder().setNumberValue(1.1).build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_1" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueTypeEnum.ValueType.NUMBER)
            .build());
    Value stringValue =
        Value.newBuilder()
            .setStringValue("attributes_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_2_blob_" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueTypeEnum.ValueType.BLOB)
            .build());
    return attributeList;
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
      GetRepositoryRequest getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(RepositoryIdentification.newBuilder().setRepoId(id))
              .build();
      GetRepositoryRequest.Response getByNameResult =
          versioningServiceBlockingStub.getRepository(getRepositoryRequest);

      SetRepository setRepository =
          SetRepository.newBuilder()
              .setId(
                  RepositoryIdentification.newBuilder()
                      .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME).build())
                      .build())
              .setRepository(getByNameResult.getRepository().toBuilder().setName(NAME_2).build())
              .build();
      SetRepository.Response result = versioningServiceBlockingStub.updateRepository(setRepository);
      Assert.assertTrue(result.hasRepository());
      Assert.assertEquals(NAME_2, result.getRepository().getName());

      getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(
                  RepositoryIdentification.newBuilder()
                      .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME_2)))
              .build();
      getByNameResult = versioningServiceBlockingStub.getRepository(getRepositoryRequest);
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
  public void updateRepositoryDescriptionTest() {
    LOGGER.info("Update repository description test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, NAME);

    try {
      GetRepositoryRequest getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(
                  RepositoryIdentification.newBuilder()
                      .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME)))
              .build();
      GetRepositoryRequest.Response getByNameResult =
          versioningServiceBlockingStub.getRepository(getRepositoryRequest);

      String description = "this is test repository description from update repository call";
      SetRepository setRepository =
          SetRepository.newBuilder()
              .setId(
                  RepositoryIdentification.newBuilder()
                      .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME).build())
                      .build())
              .setRepository(
                  getByNameResult
                      .getRepository()
                      .toBuilder()
                      .setName(NAME)
                      .setDescription(description)
                      .build())
              .build();
      SetRepository.Response result = versioningServiceBlockingStub.updateRepository(setRepository);
      Assert.assertTrue(result.hasRepository());
      Assert.assertEquals(description, result.getRepository().getDescription());

      getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(
                  RepositoryIdentification.newBuilder()
                      .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME)))
              .build();
      getByNameResult = versioningServiceBlockingStub.getRepository(getRepositoryRequest);
      Assert.assertEquals(
          "Repository Id not match with expected repository Id",
          id,
          getByNameResult.getRepository().getId());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          description,
          getByNameResult.getRepository().getDescription());
    } finally {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      Assert.assertTrue(deleteResult.getStatus());
    }

    LOGGER.info("Update repository description test end................................");
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
          listRepositoriesResponse.getTotalRecords());
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
          2,
          listRepositoriesResponse.getTotalRecords());
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

  private IdentificationType createLabels(Long repoId, List<String> labels) {
    MetadataServiceGrpc.MetadataServiceBlockingStub serviceBlockingStub =
        MetadataServiceGrpc.newBlockingStub(channel);

    IdentificationType identificationType =
        IdentificationType.newBuilder()
            .setIdType(IDTypeEnum.IDType.VERSIONING_REPOSITORY)
            .setIntId(repoId)
            .build();

    AddLabelsRequest addLabelsRequest1 =
        AddLabelsRequest.newBuilder().setId(identificationType).addAllLabels(labels).build();
    AddLabelsRequest.Response addLabelsResponse1 = serviceBlockingStub.addLabels(addLabelsRequest1);
    assertTrue("Labels not persist successfully", addLabelsResponse1.getStatus());
    return identificationType;
  }

  private void deleteLabels(IdentificationType id, List<String> labels) {
    MetadataServiceGrpc.MetadataServiceBlockingStub serviceBlockingStub =
        MetadataServiceGrpc.newBlockingStub(channel);
    DeleteLabelsRequest deleteLabelsRequest =
        DeleteLabelsRequest.newBuilder().setId(id).addAllLabels(labels).build();
    DeleteLabelsRequest.Response deleteLabelsResponse =
        serviceBlockingStub.deleteLabels(deleteLabelsRequest);
    assertTrue(deleteLabelsResponse.getStatus());
  }

  @Test
  public void findRepositoryTest() {
    LOGGER.info("List repository test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);
    DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub =
        DatasetServiceGrpc.newBlockingStub(channel);

    long repoId1 = createRepository(versioningServiceBlockingStub, NAME);
    long repoId2 = createRepository(versioningServiceBlockingStub, NAME_2);
    long repoId3 = createRepository(versioningServiceBlockingStub, NAME_3);

    DatasetTest datasetTest = new DatasetTest();
    CreateDataset createDatasetRequest =
        datasetTest.getDatasetRequest("rental_TEXT_train_data.csv");
    CreateDataset.Response createDatasetResponse =
        datasetServiceStub.createDataset(createDatasetRequest);
    LOGGER.info("CreateDataset Response : \n" + createDatasetResponse.getDataset());
    Dataset dataset = createDatasetResponse.getDataset();

    Long[] repoIds = new Long[3];
    repoIds[0] = repoId1;
    repoIds[1] = repoId2;
    repoIds[2] = repoId3;
    List<String> labels = new ArrayList<>();
    labels.add("Backend");
    IdentificationType id1 = createLabels(repoId1, labels);
    labels.add("Frontend");
    IdentificationType id2 = createLabels(repoId2, Collections.singletonList(labels.get(1)));
    IdentificationType id3 = createLabels(repoId3, labels);
    try {

      FindRepositories findRepositoriesRequest = FindRepositories.newBuilder().build();
      FindRepositories.Response findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          3,
          findRepositoriesResponse.getTotalRecords());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          NAME_3,
          findRepositoriesResponse.getRepositories(0).getName());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          NAME_2,
          findRepositoriesResponse.getRepositories(1).getName());
      Repository repo2 = findRepositoriesResponse.getRepositories(0);

      findRepositoriesRequest =
          FindRepositories.newBuilder().setPageLimit(1).setPageNumber(1).build();
      findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          3,
          findRepositoriesResponse.getTotalRecords());
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          1,
          findRepositoriesResponse.getRepositoriesCount());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          NAME_3,
          findRepositoriesResponse.getRepositories(0).getName());

      findRepositoriesRequest = FindRepositories.newBuilder().addRepoIds(repoId1).build();
      findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          1,
          findRepositoriesResponse.getTotalRecords());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          NAME,
          findRepositoriesResponse.getRepositories(0).getName());

      findRepositoriesRequest =
          FindRepositories.newBuilder()
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey("name")
                      .setValue(Value.newBuilder().setStringValue(NAME_2).build())
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .setOperator(OperatorEnum.Operator.NOT_CONTAIN)
                      .build())
              .build();
      findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          2,
          findRepositoriesResponse.getTotalRecords());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          NAME_3,
          findRepositoriesResponse.getRepositories(0).getName());

      findRepositoriesRequest =
          FindRepositories.newBuilder()
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey(ModelDBConstants.LABEL)
                      .setValue(Value.newBuilder().setStringValue("Backend").build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build())
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey(ModelDBConstants.LABEL)
                      .setValue(Value.newBuilder().setStringValue("Frontend").build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build())
              .build();
      findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          1,
          findRepositoriesResponse.getTotalRecords());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          NAME_3,
          findRepositoriesResponse.getRepositories(0).getName());

      findRepositoriesRequest =
          FindRepositories.newBuilder()
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey(ModelDBConstants.LABEL)
                      .setValue(Value.newBuilder().setStringValue("Backend").build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build())
              .build();
      findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      Assert.assertEquals(
          "Repository count not match with expected repository count",
          2,
          findRepositoriesResponse.getTotalRecords());
      Assert.assertEquals(
          "Repository name not match with expected repository name",
          NAME_3,
          findRepositoriesResponse.getRepositories(0).getName());

      findRepositoriesRequest =
          FindRepositories.newBuilder()
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey("tags")
                      .setValue(Value.newBuilder().setStringValue("Backend").build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build())
              .build();
      try {
        versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
        fail();
      } catch (StatusRuntimeException exc) {
        Status status = Status.fromThrowable(exc);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
        assertTrue(status.getDescription().contains(": tags"));
      }

      findRepositoriesRequest =
          FindRepositories.newBuilder()
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey("tags")
                      .setValue(Value.newBuilder().setStringValue("Backend").build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build())
              .build();
      try {
        versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
        fail();
      } catch (StatusRuntimeException exc) {
        Status status = Status.fromThrowable(exc);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
        assertTrue(status.getDescription().contains(": tags"));
      }

      if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
        findRepositoriesRequest =
            FindRepositories.newBuilder()
                .addPredicates(
                    KeyValueQuery.newBuilder()
                        .setKey(ModelDBConstants.OWNER)
                        .setValue(Value.newBuilder().setStringValue(repo2.getOwner()).build())
                        .setOperator(OperatorEnum.Operator.EQ)
                        .setValueType(ValueTypeEnum.ValueType.STRING)
                        .build())
                .build();
        findRepositoriesResponse =
            versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
        Assert.assertEquals(
            "Repository count not match with expected repository count",
            3,
            findRepositoriesResponse.getTotalRecords());
        Assert.assertEquals(
            "Repository count not match with expected repository count",
            3,
            findRepositoriesResponse.getRepositoriesCount());
        Assert.assertEquals(
            "Repository name not match with expected repository name",
            NAME_3,
            findRepositoriesResponse.getRepositories(0).getName());
        Assert.assertEquals(
            "Repository name not match with expected repository name",
            "This is test repository description",
            findRepositoriesResponse.getRepositories(0).getDescription());

        findRepositoriesRequest =
            FindRepositories.newBuilder()
                .addPredicates(
                    KeyValueQuery.newBuilder()
                        .setKey(ModelDBConstants.OWNER)
                        .setValue(Value.newBuilder().setStringValue(repo2.getOwner()).build())
                        .setOperator(OperatorEnum.Operator.NE)
                        .setValueType(ValueTypeEnum.ValueType.STRING)
                        .build())
                .build();
        findRepositoriesResponse =
            versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
        Assert.assertEquals(
            "Repository count not match with expected repository count",
            0,
            findRepositoriesResponse.getTotalRecords());
      }
    } finally {
      deleteLabels(id1, Collections.singletonList(labels.get(0)));
      deleteLabels(id2, Collections.singletonList(labels.get(1)));
      deleteLabels(id3, labels);

      DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(dataset.getId()).build();
      DeleteDataset.Response deleteDatasetResponse =
          datasetServiceStub.deleteDataset(deleteDataset);
      LOGGER.info("Dataset deleted successfully");
      LOGGER.info(deleteDatasetResponse.toString());
      assertTrue(deleteDatasetResponse.getStatus());

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

  @Test
  public void findRepositoriesByFuzzyOwnerTest() {
    LOGGER.info(
        "FindRepositories by owner fuzzy search test start................................");
    if (app.getAuthServerHost() == null || app.getAuthServerPort() == null) {
      assertTrue(true);
      return;
    }
    UACServiceGrpc.UACServiceBlockingStub uacServiceStub =
        UACServiceGrpc.newBlockingStub(authServiceChannel);

    GetUser getUserRequest =
        GetUser.newBuilder().setEmail(authClientInterceptor.getClient1Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo testUser1 = uacServiceStub.getUser(getUserRequest);
    String testUser1UserName = testUser1.getVertaInfo().getUsername();

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long repoId1 = createRepository(versioningServiceBlockingStub, NAME);
    long repoId2 = createRepository(versioningServiceBlockingStub, NAME_2);
    long repoId3 = createRepository(versioningServiceBlockingStub, NAME_3);
    Long[] repoIds = new Long[3];
    repoIds[0] = repoId1;
    repoIds[1] = repoId2;
    repoIds[2] = repoId3;

    try {
      Value stringValue =
          Value.newBuilder().setStringValue(testUser1UserName.substring(0, 2)).build();
      KeyValueQuery keyValueQuery =
          KeyValueQuery.newBuilder()
              .setKey("owner")
              .setValue(stringValue)
              .setOperator(OperatorEnum.Operator.CONTAIN)
              .build();
      FindRepositories findRepositoriesRequest =
          FindRepositories.newBuilder().addPredicates(keyValueQuery).build();
      FindRepositories.Response findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      LOGGER.info("FindProjects Response : " + findRepositoriesResponse.getRepositoriesList());
      assertEquals(
          "Project count not match with expected project count",
          3,
          findRepositoriesResponse.getRepositoriesCount());

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          findRepositoriesResponse.getTotalRecords());

      keyValueQuery =
          KeyValueQuery.newBuilder()
              .setKey("owner")
              .setValue(stringValue)
              .setOperator(OperatorEnum.Operator.NOT_CONTAIN)
              .build();
      findRepositoriesRequest = FindRepositories.newBuilder().addPredicates(keyValueQuery).build();
      findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      LOGGER.info("FindProjects Response : " + findRepositoriesResponse.getRepositoriesList());
      assertEquals(
          "Project count not match with expected project count",
          0,
          findRepositoriesResponse.getRepositoriesCount());
      assertEquals(
          "Total records count not matched with expected records count",
          0,
          findRepositoriesResponse.getTotalRecords());

      stringValue = Value.newBuilder().setStringValue("asdasdasd").build();
      keyValueQuery =
          KeyValueQuery.newBuilder()
              .setKey("owner")
              .setValue(stringValue)
              .setOperator(OperatorEnum.Operator.CONTAIN)
              .build();

      findRepositoriesRequest = FindRepositories.newBuilder().addPredicates(keyValueQuery).build();
      findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      LOGGER.info("FindProjects Response : " + findRepositoriesResponse.getRepositoriesList());
      assertEquals(
          "Project count not match with expected project count",
          0,
          findRepositoriesResponse.getRepositoriesCount());
      assertEquals(
          "Total records count not matched with expected records count",
          0,
          findRepositoriesResponse.getTotalRecords());

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

    LOGGER.info(
        "FindRepositories by owner fuzzy search test stop ................................");
  }

  @Test
  public void findRepositoriesByOwnerArrWithInOperatorTest() {
    LOGGER.info(
        "FindRepositories by owner fuzzy search test start................................");
    if (app.getAuthServerHost() == null || app.getAuthServerPort() == null) {
      assertTrue(true);
      return;
    }
    UACServiceGrpc.UACServiceBlockingStub uacServiceStub =
        UACServiceGrpc.newBlockingStub(authServiceChannel);

    GetUser getUserRequest =
        GetUser.newBuilder().setEmail(authClientInterceptor.getClient1Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo testUser1 = uacServiceStub.getUser(getUserRequest);

    getUserRequest = GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo testUser2 = uacServiceStub.getUser(getUserRequest);

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long repoId1 = createRepository(versioningServiceBlockingStub, NAME);
    long repoId2 = createRepository(versioningServiceBlockingStub, NAME_2);
    long repoId3 = createRepository(versioningServiceBlockingStub, NAME_3);
    Long[] repoIds = new Long[3];
    repoIds[0] = repoId1;
    repoIds[1] = repoId2;
    repoIds[2] = repoId3;

    try {
      String[] ownerArr = {
        testUser1.getVertaInfo().getUserId(), testUser2.getVertaInfo().getUserId()
      };
      Value stringValue = Value.newBuilder().setStringValue(String.join(",", ownerArr)).build();
      KeyValueQuery keyValueQuery =
          KeyValueQuery.newBuilder()
              .setKey("owner")
              .setValue(stringValue)
              .setOperator(OperatorEnum.Operator.IN)
              .build();
      FindRepositories findRepositoriesRequest =
          FindRepositories.newBuilder().addPredicates(keyValueQuery).build();
      FindRepositories.Response findRepositoriesResponse =
          versioningServiceBlockingStub.findRepositories(findRepositoriesRequest);
      LOGGER.info("FindProjects Response : " + findRepositoriesResponse.getRepositoriesList());
      assertEquals(
          "Project count not match with expected project count",
          3,
          findRepositoriesResponse.getRepositoriesCount());

      assertEquals(
          "Total records count not matched with expected records count",
          3,
          findRepositoriesResponse.getTotalRecords());

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

    LOGGER.info(
        "FindRepositories by owner fuzzy search test stop ................................");
  }

  @Test
  public void deleteRepositoryWithCommitTagsTest()
      throws NoSuchAlgorithmException, ModelDBException {
    LOGGER.info(
        "Delete Repository contains commit with tags test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    long id = createRepository(versioningServiceBlockingStub, RepositoryTest.NAME);
    GetBranchRequest getBranchRequest =
        GetBranchRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .setBranch(ModelDBConstants.MASTER_BRANCH)
            .build();
    GetBranchRequest.Response getBranchResponse =
        versioningServiceBlockingStub.getBranch(getBranchRequest);

    CreateCommitRequest createCommitRequest =
        CommitTest.getCreateCommitRequest(
            id, 111, getBranchResponse.getCommit(), Blob.ContentCase.DATASET);

    CreateCommitRequest.Response commitResponse =
        versioningServiceBlockingStub.createCommit(createCommitRequest);
    assertTrue("Commit not found in response", commitResponse.hasCommit());

    String tag = "v1.0";
    SetTagRequest setTagRequest =
        SetTagRequest.newBuilder()
            .setTag(tag)
            .setCommitSha(commitResponse.getCommit().getCommitSha())
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id).build())
            .build();
    versioningServiceBlockingStub.setTag(setTagRequest);

    DeleteRepositoryRequest deleteRepository =
        DeleteRepositoryRequest.newBuilder()
            .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(id))
            .build();
    DeleteRepositoryRequest.Response deleteResult =
        versioningServiceBlockingStub.deleteRepository(deleteRepository);
    Assert.assertTrue(deleteResult.getStatus());
    LOGGER.info(
        "Delete Repository contains commit with tags test end................................");
  }

  @Test
  public void createRepositoryWithAttributesTest() {
    LOGGER.info("Create repository with attributes test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    SetRepository setRepository = getSetRepositoryRequest(NAME);
    Repository repository = setRepository.toBuilder().getRepository();
    repository = repository.toBuilder().addAllAttributes(getAttributeList()).build();
    setRepository = setRepository.toBuilder().setRepository(repository).build();
    SetRepository.Response repositoryResponse =
        versioningServiceBlockingStub.createRepository(setRepository);

    try {
      Assert.assertEquals(
          "Repository Id not match with expected repository Id",
          repositoryResponse.getRepository().getId(),
          repositoryResponse.getRepository().getId());
      Assert.assertEquals(
          "Repository attributes not match with expected repository attributes",
          repository.getAttributesList(),
          repositoryResponse.getRepository().getAttributesList());

      GetRepositoryRequest getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(
                  RepositoryIdentification.newBuilder()
                      .setNamedId(RepositoryNamedIdentification.newBuilder().setName(NAME)))
              .build();
      GetRepositoryRequest.Response getByNameResult =
          versioningServiceBlockingStub.getRepository(getRepositoryRequest);
      Assert.assertEquals(
          "Repository attributes not match with expected repository attributes",
          repository.getAttributesList(),
          getByNameResult.getRepository().getAttributesList());
    } finally {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder()
                      .setRepoId(repositoryResponse.getRepository().getId()))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      Assert.assertTrue(deleteResult.getStatus());
    }

    LOGGER.info("Create repository with attributes test end................................");
  }

  @Test
  public void addRepositoryAttributes() {
    LOGGER.info("Add Repository Attributes test start................................");

    VersioningServiceGrpc.VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    // Create repository
    SetRepository setRepository = getSetRepositoryRequest(NAME);
    Repository repository = setRepository.toBuilder().getRepository();
    setRepository = setRepository.toBuilder().setRepository(repository).build();
    SetRepository.Response response = versioningServiceBlockingStub.createRepository(setRepository);
    repository = response.getRepository();

    try {
      List<KeyValue> attributeList = getAttributeList();

      repository = repository.toBuilder().addAllAttributes(attributeList).build();
      setRepository = setRepository.toBuilder().setRepository(repository).build();
      response = versioningServiceBlockingStub.updateRepository(setRepository);
      Assert.assertEquals(
          "Repository attributes not match with expected repository attributes",
          attributeList,
          response.getRepository().getAttributesList());

      GetRepositoryRequest getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
              .build();
      GetRepositoryRequest.Response getByNameResult =
          versioningServiceBlockingStub.getRepository(getRepositoryRequest);
      Assert.assertEquals(
          "Repository attributes not match with expected repository attributes",
          attributeList,
          getByNameResult.getRepository().getAttributesList());

    } finally {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      Assert.assertTrue(deleteResult.getStatus());
    }

    LOGGER.info("Add Repository Attributes test stop................................");
  }

  @Test
  public void updateRepositoryAttributes() {
    LOGGER.info("Update Repository Attributes test start................................");

    VersioningServiceGrpc.VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    SetRepository setRepository = getSetRepositoryRequest(NAME);
    Repository repository = setRepository.toBuilder().getRepository();
    repository = repository.toBuilder().addAllAttributes(getAttributeList()).build();
    setRepository = setRepository.toBuilder().setRepository(repository).build();
    SetRepository.Response repositoryResponse =
        versioningServiceBlockingStub.createRepository(setRepository);
    repository = repositoryResponse.getRepository();

    try {
      List<KeyValue> attributes = repository.getAttributesList();
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
              .setValueType(ValueTypeEnum.ValueType.STRING)
              .build();
      repository = repository.toBuilder().addAttributes(keyValue).build();
      RepositoryIdentification repositoryIdentification =
          RepositoryIdentification.newBuilder().setRepoId(repository.getId()).build();
      SetRepository updateRepositoryAttributesRequest =
          SetRepository.newBuilder()
              .setId(repositoryIdentification)
              .setRepository(repository)
              .build();

      repositoryResponse =
          versioningServiceBlockingStub.updateRepository(updateRepositoryAttributesRequest);
      Assert.assertTrue(
          "Repository attributes not match with expected repository attributes",
          repositoryResponse.getRepository().getAttributesList().contains(keyValue));

      GetRepositoryRequest getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
              .build();
      GetRepositoryRequest.Response getByNameResult =
          versioningServiceBlockingStub.getRepository(getRepositoryRequest);
      Assert.assertTrue(
          "Repository attributes not match with expected repository attributes",
          getByNameResult.getRepository().getAttributesList().contains(keyValue));

      Value intValue =
          Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
      keyValue =
          KeyValue.newBuilder()
              .setKey(attributes.get(1).getKey())
              .setValue(intValue)
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      repository = repository.toBuilder().addAttributes(keyValue).build();
      updateRepositoryAttributesRequest =
          SetRepository.newBuilder()
              .setId(repositoryIdentification)
              .setRepository(repository)
              .build();

      repositoryResponse =
          versioningServiceBlockingStub.updateRepository(updateRepositoryAttributesRequest);
      Assert.assertTrue(
          "Repository attributes not match with expected repository attributes",
          repositoryResponse.getRepository().getAttributesList().contains(keyValue));

      getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
              .build();
      getByNameResult = versioningServiceBlockingStub.getRepository(getRepositoryRequest);
      Assert.assertTrue(
          "Repository attributes not match with expected repository attributes",
          getByNameResult.getRepository().getAttributesList().contains(keyValue));

      Value listValue =
          Value.newBuilder()
              .setListValue(
                  ListValue.newBuilder().addValues(intValue).addValues(stringValue).build())
              .build();
      keyValue =
          KeyValue.newBuilder()
              .setKey(attributes.get(0).getKey())
              .setValue(listValue)
              .setValueType(ValueTypeEnum.ValueType.LIST)
              .build();
      repository = repository.toBuilder().addAttributes(keyValue).build();
      updateRepositoryAttributesRequest =
          SetRepository.newBuilder()
              .setId(repositoryIdentification)
              .setRepository(repository)
              .build();

      repositoryResponse =
          versioningServiceBlockingStub.updateRepository(updateRepositoryAttributesRequest);
      Assert.assertTrue(
          "Repository attributes not match with expected repository attributes",
          repositoryResponse.getRepository().getAttributesList().contains(keyValue));

      getRepositoryRequest =
          GetRepositoryRequest.newBuilder()
              .setId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
              .build();
      getByNameResult = versioningServiceBlockingStub.getRepository(getRepositoryRequest);
      Assert.assertTrue(
          "Repository attributes not match with expected repository attributes",
          getByNameResult.getRepository().getAttributesList().contains(keyValue));

    } finally {
      DeleteRepositoryRequest deleteRepository =
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(RepositoryIdentification.newBuilder().setRepoId(repository.getId()))
              .build();
      DeleteRepositoryRequest.Response deleteResult =
          versioningServiceBlockingStub.deleteRepository(deleteRepository);
      Assert.assertTrue(deleteResult.getStatus());
    }

    LOGGER.info("Update Repository Attributes test stop................................");
  }

  @Test
  public void findRepositoriesFoSharedUserTest() {
    LOGGER.info(
        "FindRepositories by owner fuzzy search test start................................");
    if (app.getAuthServerHost() == null || app.getAuthServerPort() == null) {
      assertTrue(true);
      return;
    }
    UACServiceGrpc.UACServiceBlockingStub uacServiceStub =
        UACServiceGrpc.newBlockingStub(authServiceChannel);
    CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceBlockingStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);

    GetUser getUserRequest =
        GetUser.newBuilder().setEmail(authClientInterceptor.getClient2Email()).build();
    // Get the user info by vertaId form the AuthService
    UserInfo testUser2 = uacServiceStub.getUser(getUserRequest);

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);
    VersioningServiceBlockingStub versioningServiceBlockingStubClient2 =
        VersioningServiceGrpc.newBlockingStub(client2Channel);

    long repoId1 = createRepository(versioningServiceBlockingStub, NAME);
    long repoId2 = createRepository(versioningServiceBlockingStub, NAME_2);
    long repoId3 = createRepository(versioningServiceBlockingStub, NAME_3);
    Long[] repoIds = new Long[3];
    repoIds[0] = repoId1;
    repoIds[1] = repoId2;
    repoIds[2] = repoId3;

    try {
      AddCollaboratorRequest addCollaboratorRequest =
          AddCollaboratorRequest.newBuilder()
              .setShareWith(testUser2.getEmail())
              .setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_WRITE)
              .setAuthzEntityType(EntitiesEnum.EntitiesTypes.USER)
              .addEntityIds(String.valueOf(repoId1))
              .build();
      AddCollaboratorRequest.Response collaboratorResponse =
          collaboratorServiceBlockingStub.addOrUpdateRepositoryCollaborator(addCollaboratorRequest);
      assertTrue(collaboratorResponse.getStatus());

      FindRepositories findRepositoriesRequest =
          FindRepositories.newBuilder()
              .setWorkspaceName(testUser2.getVertaInfo().getUsername())
              .build();
      FindRepositories.Response findRepositoriesResponse =
          versioningServiceBlockingStubClient2.findRepositories(findRepositoriesRequest);
      LOGGER.info("FindProjects Response : " + findRepositoriesResponse.getRepositoriesList());
      assertEquals(
          "Project count not match with expected project count",
          1,
          findRepositoriesResponse.getRepositoriesCount());

      assertEquals(
          "Total records count not matched with expected records count",
          1,
          findRepositoriesResponse.getTotalRecords());

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

    LOGGER.info(
        "FindRepositories by owner fuzzy search test stop ................................");
  }

  @Test
  public void checkRepositoryNameWithColonAndSlashesTest() {
    LOGGER.info(
        "check repository name with colon and slashes test start................................");

    VersioningServiceBlockingStub versioningServiceBlockingStub =
        VersioningServiceGrpc.newBlockingStub(channel);

    try {
      createRepository(versioningServiceBlockingStub, "Repo: colons test repository");
      fail();
    } catch (StatusRuntimeException e) {
      assertEquals(Status.INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
    }

    try {
      createRepository(versioningServiceBlockingStub, "Repo/ colons test repository");
      fail();
    } catch (StatusRuntimeException e) {
      assertEquals(Status.INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
    }

    try {
      createRepository(versioningServiceBlockingStub, "Repo\\\\ colons test repository");
      fail();
    } catch (StatusRuntimeException e) {
      assertEquals(Status.INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
    }

    LOGGER.info(
        "check repository name with colon and slashes test end................................");
  }
}
