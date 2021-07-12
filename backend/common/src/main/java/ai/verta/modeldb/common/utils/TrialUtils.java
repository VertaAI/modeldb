package ai.verta.modeldb.common.utils;

import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.common.artifactStore.storageservice.s3.S3SignatureUtil;
import ai.verta.modeldb.common.config.TrialConfig;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.google.rpc.Code;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ofPattern;

public class TrialUtils {
  public static void validateArtifactSizeForTrial(
      TrialConfig config, String artifactPath, int artifactSize) throws ModelDBException {
    if (config != null) {
      double uploadedArtifactSize = ((double) artifactSize / 1024); // In KB
      if (config.restrictions.max_artifact_size_MB != null
          && uploadedArtifactSize > ((double) config.restrictions.max_artifact_size_MB * 1024)) {
        throw new ModelDBException(
            ModelDBConstants.LIMIT_RUN_ARTIFACT_SIZE
                + "“"
                + artifactPath
                + " exceeds maximum allowed artifact size of "
                + config.restrictions.max_artifact_size_MB
                + "MB”: The artifact you are trying to log exceeds the allowable limit for your trial account",
            Code.RESOURCE_EXHAUSTED);
      }
    }
  }

  public static Map<String, String> getBodyParameterMapForTrialPresignedURL(
      AWSCredentials awsCredentials,
      String bucketName,
      String s3Key,
      String region,
      int maxArtifactSize) {
    LocalDateTime localDateTime = LocalDateTime.now();
    String dateTimeStr = localDateTime.format(ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    String date = localDateTime.format(ofPattern("yyyyMMdd"));

    S3SignatureUtil s3SignatureUtil =
        new S3SignatureUtil(awsCredentials, region, ModelDBConstants.S3.toLowerCase());

    String policy = s3SignatureUtil.readPolicy(bucketName, maxArtifactSize, awsCredentials);
    String signature = s3SignatureUtil.getSignature(policy, localDateTime);

    Map<String, String> bodyParametersMap = new HashMap<>();
    // TODO: add expiration
    bodyParametersMap.put("key", s3Key);
    bodyParametersMap.put("Policy", policy);
    bodyParametersMap.put("X-Amz-Signature", signature);
    bodyParametersMap.put("X-Amz-Algorithm", "AWS4-HMAC-SHA256");
    bodyParametersMap.put("X-Amz-Date", dateTimeStr);
    bodyParametersMap.put(
        "X-Amz-Credential",
        String.format(
            "%s/%s/%s/s3/aws4_request", awsCredentials.getAWSAccessKeyId(), date, region));
    if (awsCredentials instanceof AWSSessionCredentials) {
      AWSSessionCredentials sessionCreds = (AWSSessionCredentials) awsCredentials;
      bodyParametersMap.put("X-Amz-Security-Token", sessionCreds.getSessionToken());
    }
    return bodyParametersMap;
  }
}
