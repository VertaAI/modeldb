package ai.verta.modeldb.common.artifactstore.storageservice;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.amazonaws.services.s3.model.PartETag;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class NoopArtifactStoreService implements ArtifactStoreService {

  @Override
  public Optional<String> initiateMultipart(String s3Key) throws ModelDBException {
    return Optional.empty();
  }

  @Override
  public String generatePresignedUrl(String s3Key, String method, long partNumber, String s)
      throws ModelDBException {
    return "";
  }

  @Override
  public void commitMultipart(String s3Path, String uploadId, List<PartETag> partETags)
      throws ModelDBException {
    // Do nothing because of Noop service.
  }

  @Override
  public InputStream downloadFileFromStorage(String artifactPath) throws ModelDBException {
    return null;
  }
}
