package ai.verta.modeldb.common.artifactStore;

import ai.verta.modeldb.GetUrlForArtifact.Response;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.amazonaws.services.s3.model.PartETag;
import com.google.rpc.Code;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArtifactStoreDAODisabled implements ArtifactStoreDAO {

  private static final Logger LOGGER = LogManager.getLogger(ArtifactStoreDAODisabled.class);

  @Override
  public Response getUrlForArtifact(String s3Key, String method) throws ModelDBException {
    LOGGER.debug(CommonMessages.ARTIFACT_STORE_DISABLED_LOGS);
    throw new ModelDBException(CommonMessages.ARTIFACT_STORE_DISABLED_LOGS, Code.UNIMPLEMENTED);
  }

  @Override
  public Response getUrlForArtifactMultipart(
      String s3Key, String method, long partNumber, String uploadId) throws ModelDBException {
    LOGGER.debug(CommonMessages.ARTIFACT_STORE_DISABLED_LOGS);
    throw new ModelDBException(CommonMessages.ARTIFACT_STORE_DISABLED_LOGS, Code.UNIMPLEMENTED);
  }

  @Override
  public Optional<String> initializeMultipart(String s3Key) throws ModelDBException {
    LOGGER.debug(CommonMessages.ARTIFACT_STORE_DISABLED_LOGS);
    throw new ModelDBException(CommonMessages.ARTIFACT_STORE_DISABLED_LOGS, Code.UNIMPLEMENTED);
  }

  @Override
  public void commitMultipart(String s3Path, String uploadId, List<PartETag> partETags)
      throws ModelDBException {
    LOGGER.debug(CommonMessages.ARTIFACT_STORE_DISABLED_LOGS);
    throw new ModelDBException(CommonMessages.ARTIFACT_STORE_DISABLED_LOGS, Code.UNIMPLEMENTED);
  }

  @Override
  public InputStream downloadArtifact(String artifactPath) throws ModelDBException {
    LOGGER.debug(CommonMessages.ARTIFACT_STORE_DISABLED_LOGS);
    throw new ModelDBException(CommonMessages.ARTIFACT_STORE_DISABLED_LOGS, Code.UNIMPLEMENTED);
  }
}
