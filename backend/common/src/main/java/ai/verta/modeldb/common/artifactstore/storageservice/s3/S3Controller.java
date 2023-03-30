package ai.verta.modeldb.common.artifactstore.storageservice.s3;

import ai.verta.modeldb.common.artifactstore.storageservice.nfs.UploadFileResponse;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    try {
      String eTag = s3Service.uploadFile(artifactPath, requestEntity, part_number, upload_id);
      LOGGER.trace("S3 storeArtifact - artifact_path : {}", artifactPath);
      LOGGER.trace("S3 storeArtifact - eTag : {}", eTag);
      response.addHeader("ETag", String.valueOf(eTag));
      LOGGER.debug("S3 storeArtifact returned");
      return ResponseEntity.ok().body(new UploadFileResponse(artifactPath, null, null, -1, eTag));
    } catch (IOException | ModelDBException e) {
      LOGGER.warn(e.getMessage(), e);
      throw e;
    }
  }

  @GetMapping(value = {"${artifactEndpoint.getArtifact}/{FileName}"})
  public ResponseEntity<Resource> getArtifact(
      @PathVariable(value = "FileName") String fileName,
      @RequestParam("artifact_path") String artifactPath)
      throws ModelDBException {
    LOGGER.debug("getArtifact called");
    try {
      LOGGER.debug("getArtifact started");
      // Load file as Resource
      return s3Service.loadFileAsResource(fileName, artifactPath);
    } catch (ModelDBException e) {
      LOGGER.info(e.getMessage(), e);
      var status =
          Status.newBuilder().setCode(e.getCode().value()).setMessage(e.getMessage()).build();
      throw StatusProto.toStatusRuntimeException(status);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      var status =
          Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(e.getMessage()).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }
}
