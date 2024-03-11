package ai.verta.modeldb.ArtifactStore;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
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
import ai.verta.modeldb.common.interceptors.MetadataForwarder;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.modeldb.configuration.ReconcilerInitializer;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetSelfAllowedResources;
import ai.verta.uac.GetWorkspaceById;
import ai.verta.uac.GetWorkspaceByName;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Resources;
import ai.verta.uac.SetResource;
import ai.verta.uac.SetRoleBinding;
import ai.verta.uac.UACServiceGrpc;
import ai.verta.uac.UserInfo;
import ai.verta.uac.VertaUserInfo;
import ai.verta.uac.Workspace;
import com.google.common.util.concurrent.Futures;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
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
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = App.class, webEnvironment = DEFINED_PORT)
@ContextConfiguration(classes = {ModeldbTestConfigurationBeans.class})
public class NFSArtifactStoreTest {

  private static final Logger LOGGER = LogManager.getLogger(NFSArtifactStoreTest.class);
  private static final String artifactKey = "readme.md";
  private Project project;
  private Experiment experiment;
  private ExperimentRun experimentRun;
  private ProjectServiceBlockingStub projectServiceStub;
  private ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub;
  private ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub experimentRunServiceStub;

  @Autowired UAC uac;

  @Autowired TestConfig testConfig;

  @Autowired
  @Qualifier("grpcExecutor")
  Executor executor;

  @Autowired ReconcilerInitializer reconcilerInitializer;
  private ManagedChannel channel;

  @BeforeEach
  public void createEntities() {
    initializedChannelBuilderAndExternalServiceStubs();
    if (testConfig.getDatabase().getRdbConfiguration().isH2()) {
      setupMockUacEndpoints(uac);
    }
    createProjectEntities();
    createExperimentEntities();
    createExperimentRunEntities();
  }

  private void setupMockUacEndpoints(UAC uac) {
    Context.current().withValue(MetadataForwarder.METADATA_INFO, new Metadata()).attach();
    UACServiceGrpc.UACServiceFutureStub uacMock = uac.getUACService();
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

    when(uac.getAuthzService().isSelfAllowed(any()))
        .thenReturn(
            Futures.immediateFuture(IsSelfAllowed.Response.newBuilder().setAllowed(true).build()));

    // allow any SetResource call
    when(uac.getCollaboratorService().setResource(any()))
        .thenReturn(Futures.immediateFuture(SetResource.Response.newBuilder().build()));

    when(uac.getWorkspaceService()
            .getWorkspaceById(GetWorkspaceById.newBuilder().setId(1L).build()))
        .thenReturn(Futures.immediateFuture(Workspace.newBuilder().setId(1L).build()));
    when(uac.getWorkspaceService()
            .getWorkspaceByName(GetWorkspaceByName.newBuilder().setName("").build()))
        .thenReturn(Futures.immediateFuture(Workspace.newBuilder().setId(1L).build()));

    var getResources =
        GetResources.Response.newBuilder()
            .addItem(
                GetResourcesResponseItem.newBuilder()
                    .setVisibility(ResourceVisibility.PRIVATE)
                    .setResourceType(
                        ResourceType.newBuilder()
                            .setModeldbServiceResourceType(ModelDBServiceResourceTypes.PROJECT)
                            .build())
                    .setOwnerId(1L)
                    .setWorkspaceId(1L)
                    .build())
            .build();
    if (testConfig.isPermissionV2Enabled()) {
      when(uac.getCollaboratorService().getResources(any()))
          .thenReturn(Futures.immediateFuture(getResources));
    } else {
      when(uac.getCollaboratorService().getResourcesSpecialPersonalWorkspace(any()))
          .thenReturn(Futures.immediateFuture(getResources));
    }
    when(uac.getServiceAccountRoleServiceFutureStub().setRoleBinding(any()))
        .thenReturn(Futures.immediateFuture(SetRoleBinding.Response.newBuilder().build()));
    when(uac.getAuthzService().getSelfAllowedResources(any()))
        .thenReturn(
            Futures.immediateFuture(
                GetSelfAllowedResources.Response.newBuilder()
                    .addResources(Resources.newBuilder().setAllResourceIds(true).build())
                    .build()));
  }

  private void initializedChannelBuilderAndExternalServiceStubs() {
    var authClientInterceptor = new AuthClientInterceptor(testConfig);
    channel =
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

  @AfterEach
  public void removeEntities() throws InterruptedException, IOException {
    if (project != null) {
      DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
      projectServiceStub.deleteProject(deleteProject);
    }
    if (channel != null) {
      channel.shutdown();
      channel.awaitTermination(5, TimeUnit.SECONDS);
    }
    cleanUpResources();
  }

  private void cleanUpResources() throws IOException {
    // Remove all entities
    // removeEntities();
    // Delete entities by cron job
    var softDeleteProjects = reconcilerInitializer.getSoftDeleteProjects();
    var softDeleteExperiments = reconcilerInitializer.getSoftDeleteExperiments();
    var softDeleteExperimentRuns = reconcilerInitializer.getSoftDeleteExperimentRuns();

    softDeleteProjects.resync();
    await().until(softDeleteProjects::isEmpty);
    softDeleteExperiments.resync();
    await().until(softDeleteExperiments::isEmpty);
    softDeleteExperimentRuns.resync();
    await().until(softDeleteExperimentRuns::isEmpty);

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
        projectServiceStub.createProject(
            CreateProject.newBuilder()
                .setName(name)
                .addArtifacts(
                    Artifact.newBuilder()
                        .setKey(artifactKey)
                        .setPath(artifactKey)
                        .setArtifactType(ArtifactType.IMAGE)
                        .build())
                .build());
    project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(name, project.getName(), "Project name not match with expected Project name");

    if (testConfig.getDatabase().getRdbConfiguration().isH2()) {
      when(uac.getCollaboratorService().getResources(any()))
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

  private void createExperimentEntities() {
    var name = "Experiment" + new Date().getTime();
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(
            CreateExperiment.newBuilder().setName(name).setProjectId(project.getId()).build());
    experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        name, experiment.getName(), "Experiment name does not match with expected Experiment name");
  }

  private void createExperimentRunEntities() {
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
        createExperimentRunRequest.getName(),
        experimentRun.getName(),
        "ExperimentRun name not match with expected ExperimentRun name");
  }

  @Test
  public void loggedArtifactByGetUrlForArtifactExperimentRunTest() throws IOException {
    loggedArtifactByUrlExperimentRun();
  }

  private void loggedArtifactByUrlExperimentRun() throws IOException {
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

      String url = getUrlForArtifactResponse.getUrl();
      System.out.println("url = " + url);
      HttpURLConnection httpClient = (HttpURLConnection) new URL(url).openConnection();
      httpClient.setRequestMethod("PUT");
      httpClient.setDoOutput(true);
      httpClient.setRequestProperty("Content-Type", "application/json");
      try (OutputStream out = httpClient.getOutputStream()) {
        inputStream.transferTo(out);
        out.flush();
      }

      int responseCode = httpClient.getResponseCode();
      LOGGER.info("POST Response Code :: {}", responseCode);
      assertEquals(responseCode, HttpURLConnection.HTTP_OK);
    }
  }

  @Test
  public void getUrlForArtifactProjectTest() throws IOException {
    loggedArtifactByUrlProject();
  }

  private void loggedArtifactByUrlProject() throws IOException {
    GetUrlForArtifact getUrlForArtifactRequest =
        GetUrlForArtifact.newBuilder()
            .setId(project.getId())
            .setKey(artifactKey)
            .setMethod("put")
            .setArtifactType(ArtifactType.STRING)
            .build();
    GetUrlForArtifact.Response getUrlForArtifactResponse =
        projectServiceStub.getUrlForArtifact(getUrlForArtifactRequest);

    try (InputStream inputStream =
        new FileInputStream("src/test/java/ai/verta/modeldb/updateProjectReadMe.md")) {

      String url = getUrlForArtifactResponse.getUrl();
      System.out.println("url = " + url);
      HttpURLConnection httpClient = (HttpURLConnection) new URL(url).openConnection();
      httpClient.setRequestMethod("PUT");
      httpClient.setDoOutput(true);
      httpClient.setRequestProperty("Content-Type", "application/json");
      try (OutputStream out = httpClient.getOutputStream()) {
        inputStream.transferTo(out);
        out.flush();
      }

      int responseCode = httpClient.getResponseCode();
      LOGGER.info("POST Response Code :: {}", responseCode);
      assertEquals(responseCode, HttpURLConnection.HTTP_OK);
    }
  }

  @Test
  public void getArtifactByUrlExperimentRunTest() throws IOException {
    LOGGER.info("get artifact test start................................");
    loggedArtifactByUrlExperimentRun();

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
        new FileOutputStream(rootPath + File.separator + artifactKey);
    inputStream.transferTo(fileOutputStream);
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

  @Test
  public void getArtifactByUrlProjectTest() throws IOException {
    LOGGER.info("get artifact test start................................");
    loggedArtifactByUrlProject();

    GetUrlForArtifact getUrlForArtifactRequest =
        GetUrlForArtifact.newBuilder()
            .setId(project.getId())
            .setKey(artifactKey)
            .setMethod("GET")
            .setArtifactType(ArtifactType.STRING)
            .build();
    GetUrlForArtifact.Response getUrlForArtifactResponse =
        projectServiceStub.getUrlForArtifact(getUrlForArtifactRequest);

    URL url = new URL(getUrlForArtifactResponse.getUrl());
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    InputStream inputStream = connection.getInputStream();

    String rootPath = System.getProperty("user.dir");
    FileOutputStream fileOutputStream =
        new FileOutputStream(rootPath + File.separator + artifactKey);
    inputStream.transferTo(fileOutputStream);
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
