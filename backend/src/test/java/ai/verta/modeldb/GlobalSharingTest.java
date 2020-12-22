package ai.verta.modeldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ai.verta.common.CollaboratorTypeEnum.CollaboratorType;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.authservice.AuthInterceptor;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.PublicAuthServiceUtils;
import ai.verta.modeldb.authservice.PublicRoleServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.DeleteRepositoryRequest;
import ai.verta.modeldb.versioning.GetRepositoryRequest;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.RepositoryNamedIdentification;
import ai.verta.modeldb.versioning.SetRepository;
import ai.verta.modeldb.versioning.VersioningServiceGrpc;
import ai.verta.uac.AddUser;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.CollaboratorServiceGrpc.CollaboratorServiceBlockingStub;
import ai.verta.uac.DeleteOrganization;
import ai.verta.uac.GetRoleByName;
import ai.verta.uac.Organization;
import ai.verta.uac.OrganizationServiceGrpc;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Resources;
import ai.verta.uac.RoleScope;
import ai.verta.uac.RoleServiceGrpc;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.SetOrganization;
import ai.verta.uac.SetResources;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GlobalSharingTest {

  private static final Logger LOGGER = LogManager.getLogger(GlobalSharingTest.class);

  private static final String serverName = InProcessServerBuilder.generateName();
  private static final InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static final InProcessChannelBuilder client1ChannelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static final InProcessChannelBuilder client2ChannelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static AuthClientInterceptor authClientInterceptor;

  private static App app;
  private static DeleteEntitiesCron deleteEntitiesCron;

  private static CollaboratorServiceBlockingStub collaboratorServiceStub;
  private static ProjectServiceBlockingStub projectServiceStub;
  private static ProjectServiceBlockingStub client2ProjectServiceStub;
  private static OrganizationServiceGrpc.OrganizationServiceBlockingStub
      organizationServiceBlockingStub;
  private static RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingStub;
  private static DatasetServiceGrpc.DatasetServiceBlockingStub datasetServiceStub;
  private static DatasetServiceGrpc.DatasetServiceBlockingStub client2DatasetServiceStub;
  private static VersioningServiceGrpc.VersioningServiceBlockingStub repositoryServiceStub;
  private static VersioningServiceGrpc.VersioningServiceBlockingStub client2RepositoryServiceStub;
  private final ModelDBResourceEnum.ModelDBServiceResourceTypes resourceType;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT},
          {ModelDBResourceEnum.ModelDBServiceResourceTypes.DATASET},
          {ModelDBResourceEnum.ModelDBServiceResourceTypes.REPOSITORY},
        });
  }

  public GlobalSharingTest(ModelDBResourceEnum.ModelDBServiceResourceTypes resourceType) {
    this.resourceType = resourceType;
  }

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
    serverBuilder.intercept(new AuthInterceptor());

    Map<String, Object> testUerPropMap = (Map<String, Object>) testPropMap.get("testUsers");
    if (testUerPropMap != null && testUerPropMap.size() > 0) {
      authClientInterceptor = new AuthClientInterceptor(testPropMap);
      client1ChannelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
      client2ChannelBuilder.intercept(authClientInterceptor.getClient2AuthInterceptor());
    }

    if (app.getAuthServerHost() != null && app.getAuthServerPort() != null) {
      ManagedChannel authServiceChannel =
          ManagedChannelBuilder.forTarget(app.getAuthServerHost() + ":" + app.getAuthServerPort())
              .usePlaintext()
              .intercept(authClientInterceptor.getClient1AuthInterceptor())
              .build();
      // all service stubs
      organizationServiceBlockingStub = OrganizationServiceGrpc.newBlockingStub(authServiceChannel);
      roleServiceBlockingStub = RoleServiceGrpc.newBlockingStub(authServiceChannel);
      collaboratorServiceStub = CollaboratorServiceGrpc.newBlockingStub(authServiceChannel);
    }

    serverBuilder.build().start();
    ManagedChannel channel = client1ChannelBuilder.maxInboundMessageSize(1024).build();
    ManagedChannel client2Channel = client2ChannelBuilder.maxInboundMessageSize(1024).build();
    deleteEntitiesCron = new DeleteEntitiesCron(authService, roleService, 1000);

    // Create all service blocking stub
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    client2ProjectServiceStub = ProjectServiceGrpc.newBlockingStub(client2Channel);
    datasetServiceStub = DatasetServiceGrpc.newBlockingStub(channel);
    client2DatasetServiceStub = DatasetServiceGrpc.newBlockingStub(client2Channel);
    repositoryServiceStub = VersioningServiceGrpc.newBlockingStub(channel);
    client2RepositoryServiceStub = VersioningServiceGrpc.newBlockingStub(client2Channel);
  }

  @AfterClass
  public static void removeServerAndService() {
    App.initiateShutdown(0);

    // Remove all entities
    // removeEntities();
    // Delete entities by cron job
    deleteEntitiesCron.run();

    // shutdown test server
    serverBuilder.build().shutdownNow();
  }

  private static final Collection<Object[]> requestData =
      Arrays.asList(
          new Object[][] {
            {
              ResourceVisibility.ORG_CUSTOM,
              CollaboratorPermissions.newBuilder().setCollaboratorType(CollaboratorType.READ_WRITE),
              true,
              true
            },
            {ResourceVisibility.ORG_DEFAULT, CollaboratorPermissions.newBuilder(), true, false},
            {
              ResourceVisibility.ORG_CUSTOM,
              CollaboratorPermissions.newBuilder().setCollaboratorType(CollaboratorType.READ_ONLY),
              true,
              false
            },
            {ResourceVisibility.PRIVATE, CollaboratorPermissions.newBuilder(), false, false}
          });

  @Test
  public void createProjectWithGlobalSharingOrganization() {
    LOGGER.info("Global organization Project test start................................");

    if (app.getAuthServerHost() == null || app.getAuthServerPort() == null) {
      Assert.assertTrue(true);
      return;
    }

    String orgName = "Org-test-verta";
    SetOrganization setOrganization =
        SetOrganization.newBuilder()
            .setOrganization(
                Organization.newBuilder()
                    .setName(orgName)
                    .setDescription("This is the verta test organization")
                    .build())
            .build();
    SetOrganization.Response orgResponse =
        organizationServiceBlockingStub.setOrganization(setOrganization);
    Organization organization = orgResponse.getOrganization();
    assertEquals(
        "Organization name not matched with expected organization name",
        orgName,
        organization.getName());

    String orgRoleName = "O_" + organization.getId() + "_GLOBAL_SHARING";
    GetRoleByName getRoleByName =
        GetRoleByName.newBuilder()
            .setName(orgRoleName)
            .setScope(RoleScope.newBuilder().setOrgId(organization.getId()).build())
            .build();
    GetRoleByName.Response getRoleByNameResponse =
        roleServiceBlockingStub.getRoleByName(getRoleByName);
    assertEquals(
        "Expected role name not found in DB",
        orgRoleName,
        getRoleByNameResponse.getRole().getName());
    organizationServiceBlockingStub.addUser(
        AddUser.newBuilder()
            .setOrgId(organization.getId())
            .setShareWith(authClientInterceptor.getClient2Email())
            .build());

    String orgResourceId = null;
    Repository repository = null;
    try {
      for (Object[] data : requestData) {
        ResourceVisibility resourceVisibility = (ResourceVisibility) data[0];
        CollaboratorPermissions.Builder customPermission =
            (CollaboratorPermissions.Builder) data[1];
        boolean isReadAllowed = (boolean) data[2];
        boolean isWriteAllowed = (boolean) data[3];
        if (orgResourceId == null) {
          switch (resourceType) {
            case PROJECT:
              // Create project
              CreateProject createProjectRequest =
                  ProjectTest.getCreateProjectRequest("project-" + new Date().getTime());
              createProjectRequest =
                  createProjectRequest
                      .toBuilder()
                      .setWorkspaceName(organization.getName())
                      .setVisibility(resourceVisibility)
                      .setCustomPermission(customPermission)
                      .build();
              CreateProject.Response createProjectResponse =
                  projectServiceStub.createProject(createProjectRequest);
              orgResourceId = createProjectResponse.getProject().getId();
              break;
            case DATASET:
              // Create dataset
              CreateDataset createDatasetRequest =
                  DatasetTest.getDatasetRequest("dataset-" + new Date().getTime());
              createDatasetRequest =
                  createDatasetRequest
                      .toBuilder()
                      .setWorkspaceName(organization.getName())
                      .setVisibility(resourceVisibility)
                      .setCustomPermission(customPermission)
                      .build();
              CreateDataset.Response createDatasetResponse =
                  datasetServiceStub.createDataset(createDatasetRequest);
              orgResourceId = createDatasetResponse.getDataset().getId();
              break;
            case REPOSITORY:
            default:
              // Create repository
              String repoName = "repository-" + new Date().getTime();
              SetRepository.Response createRepositoryResponse =
                  repositoryServiceStub.createRepository(
                      SetRepository.newBuilder()
                          .setId(
                              RepositoryIdentification.newBuilder()
                                  .setNamedId(
                                      RepositoryNamedIdentification.newBuilder()
                                          .setName(repoName)
                                          .setWorkspaceName(orgName)))
                          .setRepository(
                              Repository.newBuilder()
                                  .setVisibility(resourceVisibility)
                                  .setName(repoName)
                                  .setCustomPermission(customPermission))
                          .build());
              repository = createRepositoryResponse.getRepository();
              orgResourceId = String.valueOf(repository.getId());
              break;
          }
        } else {
          collaboratorServiceStub.setResources(
              SetResources.newBuilder()
                  .setWorkspaceName(organization.getName())
                  .setResources(
                      Resources.newBuilder()
                          .addResourceIds(orgResourceId)
                          .setResourceType(
                              ResourceType.newBuilder()
                                  .setModeldbServiceResourceType(
                                      resourceType
                                              == ModelDBResourceEnum.ModelDBServiceResourceTypes
                                                  .DATASET
                                          ? ModelDBResourceEnum.ModelDBServiceResourceTypes
                                              .REPOSITORY
                                          : resourceType)
                                  .build())
                          .setService(ServiceEnum.Service.MODELDB_SERVICE)
                          .build())
                  .setVisibility(resourceVisibility)
                  .setCollaboratorType(customPermission.getCollaboratorType())
                  .setCanDeploy(customPermission.getCanDeploy())
                  .build());
        }
        try {
          switch (resourceType) {
            case PROJECT:
              client2ProjectServiceStub.getProjectById(
                  GetProjectById.newBuilder().setId(orgResourceId).build());
              break;
            case DATASET:
              client2DatasetServiceStub.getDatasetById(
                  GetDatasetById.newBuilder().setId(orgResourceId).build());
              break;
            case REPOSITORY:
            default:
              repository =
                  client2RepositoryServiceStub
                      .getRepository(
                          GetRepositoryRequest.newBuilder()
                              .setId(
                                  RepositoryIdentification.newBuilder()
                                      .setRepoId(repository.getId()))
                              .build())
                      .getRepository();
              break;
          }
          if (!isReadAllowed) {
            fail();
          }
        } catch (StatusRuntimeException e) {
          if (isReadAllowed) {
            throw e;
          }
        }
        try {
          switch (resourceType) {
            case PROJECT:
              client2ProjectServiceStub.addProjectTag(
                  AddProjectTag.newBuilder()
                      .setId(orgResourceId)
                      .setTag("new-tag" + new Date().getTime())
                      .build());
              break;
            case DATASET:
              client2DatasetServiceStub.updateDatasetName(
                  UpdateDatasetName.newBuilder()
                      .setId(orgResourceId)
                      .setName("new_name" + new Date().getTime())
                      .build());
              break;
            case REPOSITORY:
            default:
              client2RepositoryServiceStub.updateRepository(
                  SetRepository.newBuilder()
                      .setId(
                          RepositoryIdentification.newBuilder()
                              .setRepoId(Long.parseLong(orgResourceId)))
                      .setRepository(
                          repository
                              .toBuilder()
                              .setDescription("new_description" + new Date().getTime()))
                      .build());
              break;
          }
          if (!isWriteAllowed) {
            fail();
          }
        } catch (StatusRuntimeException e) {
          if (isWriteAllowed) {
            throw e;
          }
        }
      }

      LOGGER.info("Project created successfully");
    } finally {
      if (orgResourceId != null) {
        switch (resourceType) {
          case PROJECT:
            DeleteProject deleteProject = DeleteProject.newBuilder().setId(orgResourceId).build();
            DeleteProject.Response deleteProjectResponse =
                projectServiceStub.deleteProject(deleteProject);
            LOGGER.info("Project deleted successfully");
            LOGGER.info(deleteProjectResponse.toString());
            assertTrue(deleteProjectResponse.getStatus());
            break;
          case DATASET:
            DeleteDataset deleteDataset = DeleteDataset.newBuilder().setId(orgResourceId).build();
            DeleteDataset.Response deleteDatasetResponse =
                datasetServiceStub.deleteDataset(deleteDataset);
            LOGGER.info("Dataset deleted successfully");
            LOGGER.info(deleteDatasetResponse.toString());
            assertTrue(deleteDatasetResponse.getStatus());
            break;
          case REPOSITORY:
          default:
            DeleteRepositoryRequest deleteRepository =
                DeleteRepositoryRequest.newBuilder()
                    .setRepositoryId(
                        RepositoryIdentification.newBuilder()
                            .setRepoId(Long.parseLong(orgResourceId))
                            .build())
                    .build();
            DeleteRepositoryRequest.Response deleteRepositoryResponse =
                repositoryServiceStub.deleteRepository(deleteRepository);
            LOGGER.info("Repository deleted successfully");
            LOGGER.info(deleteRepositoryResponse.toString());
            assertTrue(deleteRepositoryResponse.getStatus());
            break;
        }
      }

      DeleteOrganization.Response deleteOrganization =
          organizationServiceBlockingStub.deleteOrganization(
              DeleteOrganization.newBuilder().setOrgId(organization.getId()).build());
      assertTrue(deleteOrganization.getStatus());
    }

    LOGGER.info("Global organization Project test stop................................");
  }
}
