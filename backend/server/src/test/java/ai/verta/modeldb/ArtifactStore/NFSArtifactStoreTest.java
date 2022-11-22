package ai.verta.modeldb.ArtifactStore;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import ai.verta.common.Artifact;
import ai.verta.common.ArtifactTypeEnum.ArtifactType;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.App;
import ai.verta.modeldb.AuthClientInterceptor;
import ai.verta.modeldb.CreateExperiment;
import ai.verta.modeldb.CreateExperimentRun;
import ai.verta.modeldb.CreateProject;
import ai.verta.modeldb.DeleteProject;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.ExperimentRunServiceGrpc;
import ai.verta.modeldb.ExperimentServiceGrpc;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.ModeldbTestConfigurationBeans;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.ProjectServiceGrpc;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.modeldb.configuration.ReconcilerInitializer;
import ai.verta.uac.AuthzServiceGrpc;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResources.Response;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetSelfAllowedResources;
import ai.verta.uac.GetWorkspaceById;
import ai.verta.uac.GetWorkspaceByName;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Resources;
import ai.verta.uac.RoleServiceGrpc;
import ai.verta.uac.SetResource;
import ai.verta.uac.SetRoleBinding;
import ai.verta.uac.UACServiceGrpc;
import ai.verta.uac.UserInfo;
import ai.verta.uac.VertaUserInfo;
import ai.verta.uac.Workspace;
import ai.verta.uac.WorkspaceServiceGrpc;
import com.google.api.client.util.IOUtils;
import com.google.common.util.concurrent.Futures;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class, webEnvironment = DEFINED_PORT)
@ContextConfiguration(classes = {ModeldbTestConfigurationBeans.class})
public class NFSArtifactStoreTest {

  private static final Logger LOGGER = LogManager.getLogger(NFSArtifactStoreTest.class);
  protected static ProjectServiceBlockingStub projectServiceStub;
  protected static ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub;
  protected static ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub
      experimentRunServiceStub;
  private static final String artifactKey = "readme.md";
  private static Project project;
  private static Experiment experiment;
  private static ExperimentRun experimentRun;

  @Autowired UAC uac;

  @Autowired TestConfig testConfig;

  @Autowired Executor executor;

  @Autowired ReconcilerInitializer reconcilerInitializer;

  private void setupMockUacEndpoints(UAC uac) {
    var uacMock = mock(UACServiceGrpc.UACServiceFutureStub.class);
    when(uac.getUACService()).thenReturn(uacMock);
    when(uacMock.getCurrentUser(any()))
        .thenReturn(
            Futures.immediateFuture(
                UserInfo.newBuilder()
                    .setEmail("testUser@verta.ai")
                    .setVertaInfo(
                        VertaUserInfo.newBuilder()
                            .setUserId("testUser")
                            .setDefaultWorkspaceId(1L)
                            .setWorkspaceId("1")
                            .build())
                    .build()));
    when(uacMock.getUser(any()))
        .thenReturn(
            Futures.immediateFuture(
                UserInfo.newBuilder()
                    .setEmail("testUser@verta.ai")
                    .setVertaInfo(
                        VertaUserInfo.newBuilder()
                            .setUserId("testUser")
                            .setDefaultWorkspaceId(1L)
                            .setWorkspaceId("1")
                            .build())
                    .build()));

    var authzMock = mock(AuthzServiceGrpc.AuthzServiceFutureStub.class);
    when(uac.getAuthzService()).thenReturn(authzMock);
    when(authzMock.isSelfAllowed(any()))
        .thenReturn(
            Futures.immediateFuture(IsSelfAllowed.Response.newBuilder().setAllowed(true).build()));

    var collaboratorMock = mock(CollaboratorServiceGrpc.CollaboratorServiceFutureStub.class);
    when(uac.getCollaboratorService()).thenReturn(collaboratorMock);
    // allow any SetResource call
    when(collaboratorMock.setResource(any()))
        .thenReturn(Futures.immediateFuture(SetResource.Response.newBuilder().build()));
    var workspaceMock = mock(WorkspaceServiceGrpc.WorkspaceServiceFutureStub.class);
    when(uac.getWorkspaceService()).thenReturn(workspaceMock);
    when(workspaceMock.getWorkspaceById(GetWorkspaceById.newBuilder().setId(1L).build()))
        .thenReturn(Futures.immediateFuture(Workspace.newBuilder().setId(1L).build()));
    when(workspaceMock.getWorkspaceByName(GetWorkspaceByName.newBuilder().setName("").build()))
        .thenReturn(Futures.immediateFuture(Workspace.newBuilder().setId(1L).build()));

    when(collaboratorMock.getResourcesSpecialPersonalWorkspace(any()))
        .thenReturn(
            Futures.immediateFuture(
                Response.newBuilder()
                    .addItem(
                        GetResourcesResponseItem.newBuilder()
                            .setVisibility(ResourceVisibility.PRIVATE)
                            .setResourceType(
                                ResourceType.newBuilder()
                                    .setModeldbServiceResourceType(
                                        ModelDBServiceResourceTypes.PROJECT)
                                    .build())
                            .setOwnerId(1L)
                            .setWorkspaceId(1L)
                            .build())
                    .build()));
    var roleServiceMock = mock(RoleServiceGrpc.RoleServiceFutureStub.class);
    when(uac.getServiceAccountRoleServiceFutureStub()).thenReturn(roleServiceMock);
    when(roleServiceMock.setRoleBinding(any()))
        .thenReturn(Futures.immediateFuture(SetRoleBinding.Response.newBuilder().build()));
    when(authzMock.getSelfAllowedResources(any()))
        .thenReturn(
            Futures.immediateFuture(
                GetSelfAllowedResources.Response.newBuilder()
                    .addResources(Resources.newBuilder().setAllResourceIds(true).build())
                    .build()));
  }

  @Before
  public void createEntities() {
    if (testConfig.getDatabase().getRdbConfiguration().isH2()) {
      initializedChannelBuilderAndExternalServiceStubs();

      setupMockUacEndpoints(uac);
    }
    createProjectEntities();
    createExperimentEntities();
    createExperimentRunEntities();
  }

  private void initializedChannelBuilderAndExternalServiceStubs() {
    var authClientInterceptor = new AuthClientInterceptor(testConfig);
    var channel =
        ManagedChannelBuilder.forAddress("localhost", testConfig.getGrpcServer().getPort())
            .maxInboundMessageSize(testConfig.getGrpcServer().getMaxInboundMessageSize())
            .intercept(authClientInterceptor.getClient1AuthInterceptor())
            .usePlaintext()
            .executor(executor)
            .build();

    // Create all service blocking stub
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);

    LOGGER.info("Test service infrastructure config complete.");
  }

  @After
  public void removeEntities() throws InterruptedException, IOException {
    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    cleanUpResources();
  }

  private void cleanUpResources() throws InterruptedException, IOException {
    // Remove all entities
    // removeEntities();
    // Delete entities by cron job
    var softDeleteProjects = reconcilerInitializer.getSoftDeleteProjects();
    var softDeleteExperiments = reconcilerInitializer.getSoftDeleteExperiments();
    var softDeleteExperimentRuns = reconcilerInitializer.getSoftDeleteExperimentRuns();

    softDeleteProjects.resync();
    while (!softDeleteProjects.isEmpty()) {
      LOGGER.trace("Project deletion is still in progress");
      Thread.sleep(10);
    }
    softDeleteExperiments.resync();
    while (!softDeleteExperiments.isEmpty()) {
      LOGGER.trace("Experiment deletion is still in progress");
      Thread.sleep(10);
    }
    softDeleteExperimentRuns.resync();
    while (!softDeleteExperimentRuns.isEmpty()) {
      LOGGER.trace("ExperimentRun deletion is still in progress");
      Thread.sleep(10);
    }

    reconcilerInitializer.getSoftDeleteDatasets().resync();
    reconcilerInitializer.getSoftDeleteRepositories().resync();

    File downloadedFile = new File(testConfig.getArtifactStoreConfig().getNFS().getNfsPathPrefix());
    if (downloadedFile.exists()) {
      FileUtils.deleteDirectory(downloadedFile);
    }
    LOGGER.trace("test artifact removed from storage: {}", !downloadedFile.exists());
  }

  private void createProjectEntities() {
    var name = "Project" + new Date().getTime();
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(CreateProject.newBuilder().setName(name).build());
    project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals("Project name not match with expected Project name", name, project.getName());

    if (testConfig.getDatabase().getRdbConfiguration().isH2()) {
      var collaboratorMock = mock(CollaboratorServiceGrpc.CollaboratorServiceFutureStub.class);
      when(uac.getCollaboratorService()).thenReturn(collaboratorMock);
      when(collaboratorMock.getResources(any()))
          .thenReturn(
              Futures.immediateFuture(
                  GetResources.Response.newBuilder()
                      .addItem(
                          GetResourcesResponseItem.newBuilder()
                              .setResourceId(project.getId())
                              .setWorkspaceId(1L)
                              .build())
                      .build()));
    }
  }

  private static void createExperimentEntities() {
    var name = "Experiment" + new Date().getTime();
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(
            CreateExperiment.newBuilder().setName(name).setProjectId(project.getId()).build());
    experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name", name, experiment.getName());
  }

  private static void createExperimentRunEntities() {
    var name = "ExperimentRun" + new Date().getTime();
    var createExperimentRunRequest =
        CreateExperimentRun.newBuilder()
            .setName(name)
            .setProjectId(project.getId())
            .setExperimentId(experiment.getId())
            .addArtifacts(
                Artifact.newBuilder()
                    .setKey(artifactKey)
                    .setPath(artifactKey)
                    .setArtifactType(ArtifactType.IMAGE)
                    .build())
            .build();
    CreateExperimentRun.Response createExperimentRunResponse =
        experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
    experimentRun = createExperimentRunResponse.getExperimentRun();
    LOGGER.info("ExperimentRun created successfully");
    assertEquals(
        "ExperimentRun name not match with expected ExperimentRun name",
        createExperimentRunRequest.getName(),
        experimentRun.getName());
  }

  @Test
  public void getUrlForArtifactTest() throws IOException {
    GetUrlForArtifact getUrlForArtifactRequest =
        GetUrlForArtifact.newBuilder()
            .setId(experimentRun.getId())
            .setKey(artifactKey)
            .setMethod("put")
            .setArtifactType(ArtifactType.STRING)
            .build();
    GetUrlForArtifact.Response getUrlForArtifactResponse =
        experimentRunServiceStub.getUrlForArtifact(getUrlForArtifactRequest);

    try (InputStream inputStream =
        new FileInputStream("src/test/java/ai/verta/modeldb/updateProjectReadMe.md")) {

      HttpURLConnection httpClient =
          (HttpURLConnection) new URL(getUrlForArtifactResponse.getUrl()).openConnection();
      httpClient.setRequestMethod("PUT");
      httpClient.setDoOutput(true);
      httpClient.setRequestProperty("Content-Type", "application/json");
      OutputStream out = httpClient.getOutputStream();
      IOUtils.copy(inputStream, out);
      out.flush();
      out.close();

      int responseCode = httpClient.getResponseCode();
      LOGGER.info("POST Response Code :: {}", responseCode);
      assumeTrue(responseCode == HttpURLConnection.HTTP_OK);
    }
  }

  @Test
  public void getArtifactTest() throws IOException {
    LOGGER.info("get artifact test start................................");
    getUrlForArtifactTest();

    GetUrlForArtifact getUrlForArtifactRequest =
        GetUrlForArtifact.newBuilder()
            .setId(experimentRun.getId())
            .setKey(artifactKey)
            .setMethod("GET")
            .setArtifactType(ArtifactType.STRING)
            .build();
    GetUrlForArtifact.Response getUrlForArtifactResponse =
        experimentRunServiceStub.getUrlForArtifact(getUrlForArtifactRequest);

    URL url = new URL(getUrlForArtifactResponse.getUrl());
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    InputStream inputStream = connection.getInputStream();

    String rootPath = System.getProperty("user.dir");
    FileOutputStream fileOutputStream =
        new FileOutputStream(new File(rootPath + File.separator + artifactKey));
    IOUtils.copy(inputStream, fileOutputStream);
    fileOutputStream.close();
    inputStream.close();

    File downloadedFile = new File(rootPath + File.separator + artifactKey);
    if (!downloadedFile.exists()) {
      fail("File not fount at download destination");
    }
    var fileDeleted = downloadedFile.delete();
    LOGGER.info("test artifact removed from storage: {}", fileDeleted);

    LOGGER.info("get artifact test stop................................");
  }
}
