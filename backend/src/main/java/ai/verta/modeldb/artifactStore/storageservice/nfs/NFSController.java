package ai.verta.modeldb.artifactStore.storageservice.nfs;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.monitoring.ErrorCountResource;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NFSController {

  private static final Logger LOGGER = LogManager.getLogger(NFSController.class);

  @Autowired private NFSService nfsService;

  @PutMapping(value = {"${artifactEndpoint.storeArtifact}"})
  public UploadFileResponse storeArtifact(
      HttpServletRequest requestEntity, @RequestParam("artifact_path") String artifactPath)
      throws ModelDBException, IOException {
    LOGGER.debug("storeArtifact called");
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBConstants.STORE_ARTIFACT_ENDPOINT)) {
      InputStream inputStream = requestEntity.getInputStream();
      String fileName = nfsService.storeFile(artifactPath, inputStream);
      LOGGER.trace("storeArtifact - file name : {}", fileName);
      LOGGER.debug("storeArtifact returned");
      return new UploadFileResponse(fileName, null, null, -1L);
    } catch (IOException | ModelDBException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      throw e;
    }
  }

  @GetMapping(value = {"${artifactEndpoint.getArtifact}"})
  public ResponseEntity<Resource> getArtifact(
      @RequestParam("artifact_path") String artifactPath, HttpServletRequest request)
      throws ModelDBException {
    LOGGER.debug("getArtifact called");
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBConstants.GET_ARTIFACT_ENDPOINT)) {
      // Load file as Resource
      Resource resource = nfsService.loadFileAsResource(artifactPath);

      // Try to determine file's content type
      String contentType = getContentType(request, resource);

      // Fallback to the default content type if type could not be determined
      if (contentType == null) {
        contentType = "application/octet-stream";
      }
      LOGGER.trace("getArtifact - file content type : {}", contentType);

      LOGGER.debug("getArtifact returned");
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(contentType))
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"" + resource.getFilename() + "\"")
          .body(resource);
    } catch (ModelDBException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      throw e;
    }
  }

  public String getContentType(HttpServletRequest request, Resource resource) {
    try {
      return request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
    } catch (Exception ex) {
      LOGGER.info("Could not determine file type.");
      return null;
    }
  }
}
