package ai.verta.modeldb.ArtifactStore;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.verta.common.Artifact;
import ai.verta.common.ArtifactTypeEnum.ArtifactType;
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
import ai.verta.modeldb.Project;
import ai.verta.modeldb.ProjectServiceGrpc;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.modeldb.configuration.ReconcilerInitializer;
import ai.verta.modeldb.reconcilers.SoftDeleteExperimentRuns;
import ai.verta.modeldb.reconcilers.SoftDeleteExperiments;
import ai.verta.modeldb.reconcilers.SoftDeleteProjects;
import ai.verta.uac.AuthzServiceGrpc;
import ai.verta.uac.IsSelfAllowed;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ArtifactTestsConfigBeans.class})
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

  @Before
  public void createEntities() {
    initializedChannelBuilderAndExternalServiceStubs();

    // TODO: FIXME: fix Mockito cannot mock/spy because : - final class error.
    /*var authzMock = mock(AuthzServiceGrpc.AuthzServiceFutureStub.class);
    when(uac.getAuthzService()).thenReturn(authzMock);
    when(authzMock.isSelfAllowed(any()))
        .thenReturn(
            Futures.immediateFuture(IsSelfAllowed.Response.newBuilder().setAllowed(true).build()));*/

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
  public void removeEntities() throws InterruptedException {
    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());

    cleanUpResources();
  }

  private void cleanUpResources() throws InterruptedException {
    // Remove all entities
    // removeEntities();
    // Delete entities by cron job
    SoftDeleteProjects softDeleteProjects = reconcilerInitializer.softDeleteProjects;
    SoftDeleteExperiments softDeleteExperiments = reconcilerInitializer.softDeleteExperiments;
    SoftDeleteExperimentRuns softDeleteExperimentRuns =
        reconcilerInitializer.softDeleteExperimentRuns;

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

    ReconcilerInitializer.softDeleteDatasets.resync();
    ReconcilerInitializer.softDeleteRepositories.resync();
  }

  private static void createProjectEntities() {
    var name = "Project" + new Date().getTime();
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(CreateProject.newBuilder().setName(name).build());
    project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals("Project name not match with expected Project name", name, project.getName());
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
    downloadedFile.delete();

    LOGGER.info("get artifact test stop................................");
  }
}
