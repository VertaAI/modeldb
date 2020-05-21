package ai.verta.modeldb.artifactStore;

import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.GetUrlForArtifact.Response;
import ai.verta.modeldb.ModelDBException;
import java.util.Optional;

public interface ArtifactStoreDAO {
  GetUrlForArtifact.Response getUrlForArtifact(String path, String method) throws ModelDBException;

  Response getUrlForArtifactMultipart(String s3Key, String method, long partNumber, String uploadId)
      throws ModelDBException;

  Optional<String> initializeMultipart(String s3Path) throws ModelDBException;
}
