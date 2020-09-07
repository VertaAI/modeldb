package ai.verta.modeldb.artifactStore.storageservice.s3;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.artifactStore.storageservice.nfs.UploadFileResponse;
import ai.verta.modeldb.monitoring.ErrorCountResource;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class S3Controller {

  private static final Logger LOGGER = LogManager.getLogger(S3Controller.class);

  @Autowired private S3Service s3Service;

  @PutMapping(value = {"${artifactEndpoint.storeArtifact}"})
  public ResponseEntity<UploadFileResponse> storeArtifact(
      HttpServletRequest requestEntity,
      HttpServletResponse response,
      @RequestParam("artifact_path") String artifactPath,
      @RequestParam("part_number") Long part_number,
      @RequestParam("upload_id") String upload_id)
      throws ModelDBException, IOException {
    LOGGER.debug("S3 storeArtifact called");
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBConstants.STORE_ARTIFACT_ENDPOINT)) {
      String eTag = s3Service.uploadFile(artifactPath, requestEntity, part_number, upload_id);
      LOGGER.trace("S3 storeArtifact - artifact_path : {}", artifactPath);
      LOGGER.trace("S3 storeArtifact - eTag : {}", eTag);
      HttpHeaders responseHeaders = new HttpHeaders();
      response.addHeader("ETag", String.valueOf(eTag));
      LOGGER.debug("S3 storeArtifact returned");
      return ResponseEntity.ok().body(new UploadFileResponse(artifactPath, null, null, -1, eTag));
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
      Resource resource = s3Service.loadFileAsResource(artifactPath);

      // Try to determine file's content type
      String contentType = getContentType(request, resource);

      // Fallback to the default content type if type could not be determined
      if (contentType == null) {
        contentType = "binary/octet-stream";
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
      LOGGER.info(e.getMessage(), e);
      ErrorCountResource.inc(e);
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
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
