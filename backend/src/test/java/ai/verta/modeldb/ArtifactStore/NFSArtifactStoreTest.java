package ai.verta.modeldb.ArtifactStore;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import ai.verta.common.Artifact;
import ai.verta.common.ArtifactTypeEnum.ArtifactType;
import ai.verta.modeldb.CreateExperiment;
import ai.verta.modeldb.CreateExperimentRun;
import ai.verta.modeldb.CreateProject;
import ai.verta.modeldb.DeleteProject;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.ExperimentRunTest;
import ai.verta.modeldb.ExperimentTest;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.ProjectTest;
import ai.verta.modeldb.TestsInit;
import com.google.api.client.util.IOUtils;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NFSArtifactStoreTest extends ArtifactStoreTestSetup {

  private static final Logger LOGGER = LogManager.getLogger(NFSArtifactStoreTest.class);
  private static final String artifactKey = "verta_logo.png";
  private static Project project;
  private static Experiment experiment;
  private static ExperimentRun experimentRun;

  @Before
  public void createEntities() {
    createProjectEntities();
    createExperimentEntities();
    createExperimentRunEntities();
  }

  @After
  public void removeEntities() {
    DeleteProject deleteProject = DeleteProject.newBuilder().setId(project.getId()).build();
    DeleteProject.Response deleteProjectResponse = projectServiceStub.deleteProject(deleteProject);
    LOGGER.info("Project deleted successfully");
    LOGGER.info(deleteProjectResponse.toString());
    assertTrue(deleteProjectResponse.getStatus());
  }

  private static void createProjectEntities() {
    var name = "Project" + new Date().getTime();
    CreateProject.Response createProjectResponse =
        projectServiceStub.createProject(CreateProject.newBuilder().setName(name).build());
    project = createProjectResponse.getProject();
    LOGGER.info("Project created successfully");
    assertEquals(
        "Project name not match with expected Project name",
        name,
        project.getName());
  }

  private static void createExperimentEntities() {
    var name = "Experiment" + new Date().getTime();
    CreateExperiment.Response createExperimentResponse =
        experimentServiceStub.createExperiment(CreateExperiment.newBuilder().setName("Experiment" + new Date().getTime()).setProjectId(project.getId()).build());
    experiment = createExperimentResponse.getExperiment();
    LOGGER.info("Experiment created successfully");
    assertEquals(
        "Experiment name not match with expected Experiment name",
        name,
        experiment.getName());
  }

  private static void createExperimentRunEntities() {
    var name = "ExperimentRun" + new Date().getTime();
    var createExperimentRunRequest =
        CreateExperimentRun.newBuilder()
            .setName(name)
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
  public void getUrlForArtifactTest() {
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
  }

  @Test
  public void getArtifactTest() {
    LOGGER.info("get artifact test start................................");

    try {
      getUrlForArtifactTest();

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
