package ai.verta.modeldb.artifactStore;

import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import com.amazonaws.SdkClientException;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArtifactStoreDAORdbImpl implements ArtifactStoreDAO {

  private static final Logger LOGGER = LogManager.getLogger(ArtifactStoreDAORdbImpl.class);
  private ArtifactStoreService artifactStoreService;

  public ArtifactStoreDAORdbImpl(ArtifactStoreService artifactStoreService) {
    this.artifactStoreService = artifactStoreService;
  }

  @Override
  public GetUrlForArtifact.Response getUrlForArtifact(String s3Key, String method) {
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String presignedUrl = artifactStoreService.generatePresignedUrl(s3Key, method);
      return GetUrlForArtifact.Response.newBuilder().setUrl(presignedUrl).build();
    } catch (SdkClientException e) {
      // Amazon S3 couldn't be contacted for a response, or the client
      // couldn't parse the response from Amazon S3.
      String errorMessage = e.getMessage();
      Status status =
          Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(errorMessage).build();
      LOGGER.warn(errorMessage);
      throw StatusProto.toStatusRuntimeException(status);
    }
  }
}
