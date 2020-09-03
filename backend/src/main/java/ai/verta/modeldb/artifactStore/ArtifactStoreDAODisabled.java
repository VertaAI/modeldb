package ai.verta.modeldb.artifactStore;

import ai.verta.modeldb.GetUrlForArtifact.Response;
import ai.verta.modeldb.ModelDBException;
import com.amazonaws.services.s3.model.PartETag;
import com.google.rpc.Code;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArtifactStoreDAODisabled implements ArtifactStoreDAO {

  private static final Logger LOGGER = LogManager.getLogger(ArtifactStoreDAODisabled.class);

  public ArtifactStoreDAODisabled() {}

  @Override
  public Response getUrlForArtifact(String s3Key, String method) throws ModelDBException {
    LOGGER.debug("Artifact store is disabled");
    throw new ModelDBException("Artifact store is disabled", Code.UNIMPLEMENTED);
  }

  @Override
  public Response getUrlForArtifactMultipart(
      String s3Key, String method, long partNumber, String uploadId) throws ModelDBException {
    LOGGER.debug("Artifact store is disabled");
    throw new ModelDBException("Artifact store is disabled", Code.UNIMPLEMENTED);
  }

  @Override
  public Optional<String> initializeMultipart(String s3Key) throws ModelDBException {
    LOGGER.debug("Artifact store is disabled");
    throw new ModelDBException("Artifact store is disabled", Code.UNIMPLEMENTED);
  }

  @Override
  public void commitMultipart(String s3Path, String uploadId, List<PartETag> partETags)
      throws ModelDBException {
    LOGGER.debug("Artifact store is disabled");
    throw new ModelDBException("Artifact store is disabled", Code.UNIMPLEMENTED);
  }
}
