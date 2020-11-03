package ai.verta.modeldb.artifactStore.storageservice;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import com.amazonaws.services.s3.model.PartETag;
import com.google.rpc.Code;
import io.grpc.Metadata;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

public interface ArtifactStoreService {

  Optional<String> initiateMultipart(String s3Key) throws ModelDBException;

  String generatePresignedUrl(String s3Key, String method, long partNumber, String s)
      throws ModelDBException;

  void commitMultipart(String s3Path, String uploadId, List<PartETag> partETags)
      throws ModelDBException;

  /**
   * Return public url for artifact file download
   *
   * @param parameters : like artifact path
   * @return {@link String} : public URL for file download
   */
  default String getDownloadUrl(
      Map<String, Object> parameters,
      String artifactStoreUrlProtocol,
      String getArtifactEndpoint,
      boolean pickNFSHostFromConfig,
      String artifactStoreServerHost) {
    String scheme =
        ModelDBAuthInterceptor.METADATA_INFO
            .get()
            .get(Metadata.Key.of("scheme", Metadata.ASCII_STRING_MARSHALLER));
    if (scheme == null || scheme.isEmpty()) {
      scheme = artifactStoreUrlProtocol;
    }
    return getUrl(
        parameters, getArtifactEndpoint, scheme, pickNFSHostFromConfig, artifactStoreServerHost);
  }

  /**
   * Return public url for upload artifact file
   *
   * @param parameters : like artifact path
   * @return {@link String} : public URL for file upload
   */
  default String getUploadUrl(
      Map<String, Object> parameters,
      String artifactStoreUrlProtocol,
      String storeArtifactEndpoint,
      boolean pickNFSHostFromConfig,
      String artifactStoreServerHost) {
    String scheme =
        ModelDBAuthInterceptor.METADATA_INFO
            .get()
            .get(Metadata.Key.of("scheme", Metadata.ASCII_STRING_MARSHALLER));
    if (scheme == null || scheme.isEmpty()) {
      String url =
          ModelDBAuthInterceptor.METADATA_INFO
              .get()
              .get(Metadata.Key.of("grpcgateway-origin", Metadata.ASCII_STRING_MARSHALLER));

      try {
        scheme = new URL(url).getProtocol();
      } catch (MalformedURLException e) {
        scheme = artifactStoreUrlProtocol;
      }
    }
    return getUrl(
        parameters, storeArtifactEndpoint, scheme, pickNFSHostFromConfig, artifactStoreServerHost);
  }

  default String getUrl(
      Map<String, Object> parameters,
      String endpoint,
      String scheme,
      boolean pickNFSHostFromConfig,
      String artifactStoreServerHost) {

    String host =
        ModelDBAuthInterceptor.METADATA_INFO
            .get()
            .get(Metadata.Key.of("x-forwarded-host", Metadata.ASCII_STRING_MARSHALLER));
    if (host == null || host.isEmpty() || pickNFSHostFromConfig) {
      host = artifactStoreServerHost;
    }

    String[] hostArr = host.split(":");
    String finalHost = hostArr[0];

    UriComponentsBuilder uriComponentsBuilder =
        ServletUriComponentsBuilder.newInstance().scheme(scheme).host(finalHost).path(endpoint);
    for (Map.Entry<String, Object> queryParam : parameters.entrySet()) {
      // Adding the filename to the path to enable file saved with that name
      if (queryParam.getKey().equals(ModelDBConstants.FILENAME)) {
        uriComponentsBuilder.path(String.valueOf(queryParam.getValue()));
      } else {
        uriComponentsBuilder.queryParam(queryParam.getKey(), queryParam.getValue());
      }
    }

    if (hostArr.length > 1) {
      String finalPort = hostArr[1];
      uriComponentsBuilder.port(finalPort);
    }

    return uriComponentsBuilder.toUriString();
  }

  default void validateArtifactSizeForTrial(App app, int artifactSize, Long partNumber)
      throws ModelDBException {
    if (app.getTrialEnabled()) {
      if (partNumber != null && partNumber != 0) {
        throw new ModelDBException(
            "Multipart artifact upload not supported on the trial version",
            Code.FAILED_PRECONDITION);
      }

      double uploadedArtifactSize = ((double) artifactSize / 1024); // In KB
      if (uploadedArtifactSize > ((double) app.getMaxArtifactSizeMB() * 1024)) {
        throw new ModelDBException(
            "Artifact size more then " + app.getMaxArtifactSizeMB() + " MB not supported",
            Code.FAILED_PRECONDITION);
      }
    }
  }
}
