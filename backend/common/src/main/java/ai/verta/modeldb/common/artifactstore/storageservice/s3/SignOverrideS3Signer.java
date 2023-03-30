package ai.verta.modeldb.common.artifactstore.storageservice.s3;

import com.amazonaws.SignableRequest;
import com.amazonaws.services.s3.internal.AWSS3V4Signer;

public class SignOverrideS3Signer extends AWSS3V4Signer {
  @Override
  protected String getCanonicalizedHeaderString(SignableRequest<?> request) {
    String hostKey = null;
    String hostValue = null;
    if (System.getenv().containsKey("AWS_S3_HOST")) {
      final var desiredHost = System.getenv().get("AWS_S3_HOST");
      for (var entry : request.getHeaders().entrySet()) {
        if (entry.getKey().toLowerCase().equals("host")) {
          hostKey = entry.getKey();
          hostValue = entry.getValue();
          request.getHeaders().put(hostKey, desiredHost);
        }
      }
    }
    final var ret = super.getCanonicalizedHeaderString(request);
    if (hostKey != null) {
      request.getHeaders().put(hostKey, hostValue);
    }
    return ret;
  }
}
