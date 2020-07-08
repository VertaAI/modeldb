package ai.verta.modeldb.artifactStore.storageservice;

import ai.verta.modeldb.ModelDBException;
import com.amazonaws.services.s3.model.PartETag;
import java.util.List;
import java.util.Optional;

public interface ArtifactStoreService {

  Optional<String> initiateMultipart(String s3Key) throws ModelDBException;

  String generatePresignedUrl(String s3Key, String method, long partNumber, String s)
      throws ModelDBException;

  void commitMultipart(String s3Path, String uploadId, List<PartETag> partETags)
      throws ModelDBException;
}
