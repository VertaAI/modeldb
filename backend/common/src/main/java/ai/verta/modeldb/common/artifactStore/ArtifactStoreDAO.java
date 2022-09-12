package ai.verta.modeldb.common.artifactStore;

import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.amazonaws.services.s3.model.PartETag;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface ArtifactStoreDAO {
  GetUrlForArtifact.Response getUrlForArtifact(String path, String method) throws ModelDBException;

  GetUrlForArtifact.Response getUrlForArtifactMultipart(
      String s3Key, String method, long partNumber, String uploadId) throws ModelDBException;

  Optional<String> initializeMultipart(String s3Path) throws ModelDBException;

  void commitMultipart(String s3Path, String uploadId, List<PartETag> partETags)
      throws ModelDBException;

  InputStream downloadArtifact(String artifactPath) throws ModelDBException;
}
