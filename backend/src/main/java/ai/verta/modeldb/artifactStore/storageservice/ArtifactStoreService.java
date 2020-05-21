package ai.verta.modeldb.artifactStore.storageservice;

import ai.verta.modeldb.ModelDBException;
import java.util.Optional;

public interface ArtifactStoreService {

  Optional<String> initiateMultipart(String s3Key) throws ModelDBException;

  String generatePresignedUrl(String s3Key, String method, long partNumber, String s)
      throws ModelDBException;
}
