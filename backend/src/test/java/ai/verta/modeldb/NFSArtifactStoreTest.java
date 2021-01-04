package ai.verta.modeldb;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import ai.verta.common.Artifact;
import ai.verta.common.ArtifactTypeEnum.ArtifactType;
import ai.verta.modeldb.authservice.*;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.cron_jobs.DeleteEntitiesCron;
import com.google.api.client.util.IOUtils;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NFSArtifactStoreTest {

  private static final Logger LOGGER = LogManager.getLogger(NFSArtifactStoreTest.class);

  private static String serverName = InProcessServerBuilder.generateName();
  private static InProcessServerBuilder serverBuilder =
      InProcessServerBuilder.forName(serverName).directExecutor();
  private static InProcessChannelBuilder channelBuilder =
      InProcessChannelBuilder.forName(serverName).directExecutor();
  private static String artifactKey = "verta_logo.png";

  // Project Entity
  private static Project project;

  // Experiment Entity
  private static Experiment experiment;

  // ExperimentRun Entity
  private static ExperimentRun experimentRun;

  // all service stubs
  private static ProjectServiceGrpc.ProjectServiceBlockingStub projectServiceStub;
  private static ExperimentServiceGrpc.ExperimentServiceBlockingStub experimentServiceStub;
  private static ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub experimentRunServiceStub;
  private static DeleteEntitiesCron deleteEntitiesCron;

  @SuppressWarnings("unchecked")
  @BeforeClass
  public static void setServerAndService() throws Exception {
    Config config = Config.getInstance();
    // Initialize services that we depend on
    ServiceSet services = ServiceSet.fromConfig(config);
    // Initialize data access
    DAOSet daos = DAOSet.fromServices(services);
    App.migrate(config);

    App.initializeBackendServices(serverBuilder, services, daos);
    serverBuilder.intercept(new AuthInterceptor());

    if (config.test != null) {
      AuthClientInterceptor authClientInterceptor = new AuthClientInterceptor(config.test);
      channelBuilder.intercept(authClientInterceptor.getClient1AuthInterceptor());
    }

    serverBuilder.build().start();
    ManagedChannel channel = channelBuilder.maxInboundMessageSize(1024).build();
    deleteEntitiesCron = new DeleteEntitiesCron(services.authService, services.roleService, 1000);

    // Create all service blocking stub
    projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);
    experimentServiceStub = ExperimentServiceGrpc.newBlockingStub(channel);
    experimentRunServiceStub = ExperimentRunServiceGrpc.newBlockingStub(channel);

    // Create all entities
    createProjectEntities();
    createExperimentEntities();
    createExperimentRunEntities();
  }

  @AfterClass
  public static void removeServerAndService() {
    App.initiateShutdown(0);

    // Remove all entities
    removeEntities();
    // Delete entities by cron job
    deleteEntitiesCron.run();

    // shutdown test server
    serverBuilder.build().shutdownNow();
  }

  private static void removeEntities() {
    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());
  }

  private static void createProjectEntities() {
    ProjectTest projectTest = new ProjectTest();

    // Create two project of above project
    CreateProject createProjectRequest = projectTest.getCreateProjectRequest("Project_1");
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(createProjectRequest);
    project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        createProjectRequest.getName(),
        project.getName());
  }

  private static void createExperimentEntities() {
    ExperimentTest experimentTest = new ExperimentTest();

    // Create two experiment of above project
    CreateExperiment createExperimentRequest =
        experimentTest.getCreateExperimentRequest(project.getId(), "Experiment_1");
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(createExperimentRequest);
    experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        createExperimentRequest.getName(),
        experiment.getName());
  }

  private static void createExperimentRunEntities() {
    ExperimentRunTest experimentRunTest = new ExperimentRunTest();

    CreateExperimentRun createExperimentRunRequest =
        experimentRunTest.getCreateExperimentRunRequest(
            project.getId(), experiment.getId(), "ExperimentRun_sprt_1");
    createExperimentRunRequest =
        createExperimentRunRequest
            .toBuilder()
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

  private void storeArtifactTest() {
    LOGGER.info("store artifact test start................................");
    try {

      GetUrlForArtifact getUrlForArtifactRequest =
          GetUrlForArtifact.newBuilder()
              .setId(experimentRun.getId())
              .setKey(artifactKey)
              .setMethod("PUT")
              .setArtifactType(ArtifactType.IMAGE)
              .build();
      GetUrlForArtifact.Response getUrlForArtifactResponse =
          experimentRunServiceStub.getUrlForArtifact(getUrlForArtifactRequest);

      URL url =
          new URL("https://www.verta.ai/static/logo-landing-424af27a5fc184c64225f604232db39e.png");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      InputStream inputStream = connection.getInputStream();

      HttpURLConnection httpClient =
          (HttpURLConnection) new URL(getUrlForArtifactResponse.getUrl()).openConnection();
      httpClient.setRequestMethod("PUT");
      httpClient.setDoOutput(true);
      httpClient.setRequestProperty("Content-Type", "application/json");
      OutputStream out = httpClient.getOutputStream();
      IOUtils.copy(inputStream, out);
      out.flush();
      out.close();
      inputStream.close();

      int responseCode = httpClient.getResponseCode();
      LOGGER.info("POST Response Code :: {}", responseCode);
      assumeTrue(responseCode == HttpURLConnection.HTTP_OK);
    } catch (StatusRuntimeException | MalformedURLException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.error(
          "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      fail();
    } catch (IOException e) {
      LOGGER.error("Error : {}", e.getMessage(), e);
      fail();
    }

    LOGGER.info("store artifact test stop................................");
  }

  @Test
  public void getArtifactTest() {
    LOGGER.info("get artifact test start................................");

    try {
      storeArtifactTest();

      GetUrlForArtifact getUrlForArtifactRequest =
          GetUrlForArtifact.newBuilder()
              .setId(experimentRun.getId())
              .setKey(artifactKey)
              .setMethod("GET")
              .setArtifactType(ArtifactType.IMAGE)
              .build();
      GetUrlForArtifact.Response getUrlForArtifactResponse =
          experimentRunServiceStub.getUrlForArtifact(getUrlForArtifactRequest);

      URL url = new URL(getUrlForArtifactResponse.getUrl());
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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

    } catch (Exception e) {
      e.printStackTrace();
      Status status = Status.fromThrowable(e);
      LOGGER.error(
          "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      fail();
    }

    LOGGER.info("get artifact test stop................................");
  }
}
