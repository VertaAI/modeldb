package ai.verta.modeldb.common.artifactstore.storageservice.s3;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ofPattern;

import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.SigningAlgorithm;
import java.time.LocalDateTime;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;

public class S3SignatureUtil extends AWS4Signer {
  private final String awsServiceName;
  private final AWSCredentials credentials;
  private final String region;

  public S3SignatureUtil(AWSCredentials credentials, String region, String awsServiceName) {
    this.credentials = credentials;
    this.region = region;
    this.awsServiceName = awsServiceName;
  }

  public String getSignature(String policy, LocalDateTime dateTime) {
    try {
      String dateStamp = dateTime.format(ofPattern("yyyyMMdd"));
      return Hex.encodeHexString(
          hmacSha256(newSigningKey(credentials, dateStamp, region, awsServiceName), policy));
    } catch (Exception e) {
      throw new RuntimeException("Error", e);
    }
  }

  private byte[] hmacSha256(byte[] key, String data) throws Exception {
    var mac = Mac.getInstance(SigningAlgorithm.HmacSHA256.name());
    mac.init(new SecretKeySpec(key, SigningAlgorithm.HmacSHA256.name()));
    return mac.doFinal(data.getBytes(UTF_8));
  }

  public String readPolicy(String bucketName, int maxArtifactSize, AWSCredentials awsCredentials) {
    StringBuilder policyBuilder =
        new StringBuilder()
            .append("{'expiration': '2099-12-30T12:00:00.000Z','conditions': [{'bucket': '")
            .append(bucketName)
            .append("'},['starts-with', '$key', ''],['starts-with', '$x-amz-date', '']")
            .append(",['starts-with', '$x-amz-credential', ''],['content-length-range', 0, ")
            .append(maxArtifactSize)
            .append("]");
    if (awsCredentials instanceof AWSSessionCredentials) {
      policyBuilder.append(",['starts-with', '$x-amz-security-token','']");
    }
    policyBuilder.append(",{'x-amz-algorithm': 'AWS4-HMAC-SHA256'}]}");
    return Base64.getEncoder().encodeToString(policyBuilder.toString().getBytes(UTF_8));
  }
}
