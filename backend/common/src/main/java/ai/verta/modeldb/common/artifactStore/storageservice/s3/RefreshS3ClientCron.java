package ai.verta.modeldb.common.artifactStore.storageservice.s3;

import ai.verta.modeldb.common.CommonUtils;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.WebIdentityTokenCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

public class RefreshS3ClientCron implements Runnable {
  private static final Logger LOGGER = LogManager.getLogger(RefreshS3ClientCron.class);
  private final String bucketName;
  private final Regions awsRegion;
  private final Integer durationSeconds;
  private final S3Client s3Client;

  public RefreshS3ClientCron(
      String bucketName, Regions awsRegion, Integer durationSeconds, S3Client s3Client) {
    this.bucketName = bucketName;
    this.awsRegion = awsRegion;
    this.durationSeconds = durationSeconds;
    this.s3Client = s3Client;
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("Refreshing S3Client");
    try {
      var awsCredentials = getBasicSessionCredentials();
      createAndRefreshNewClient(awsCredentials);
    } catch (Throwable ex) {
      LOGGER.error("Failed to refresh S3 Client: " + ex.getMessage(), ex);
      return;
    }
    LOGGER.info("Refreshing S3Client complete");
  }

  private void createAndRefreshNewClient(BasicSessionCredentials awsCredentials) {
    LOGGER.trace("Creating new S3 Client");
    var newS3Client =
        AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .withRegion(awsRegion)
            .build();

    newS3Client.doesBucketExistV2(bucketName);
    LOGGER.trace("New S3 Client created");

    s3Client.refreshS3Client(
        awsCredentials,
        newS3Client,
        AwsSessionCredentials.create(
            awsCredentials.getAWSAccessKeyId(),
            awsCredentials.getAWSSecretKey(),
            awsCredentials.getSessionToken()));
  }

  private BasicSessionCredentials getBasicSessionCredentials() throws IOException {
    AWSSecurityTokenService stsClient = null;
    try {
      LOGGER.trace("Creating sts client");
      stsClient = createStsClient(awsRegion);
      LOGGER.trace("Sts client created");

      LOGGER.trace("assuming role with web identity");
      // Call STS to assume the role
      var roleRequest = getCredentialFromWebIdentity();
      AssumeRoleWithWebIdentityResult roleResponse =
          stsClient.assumeRoleWithWebIdentity(roleRequest);
      LOGGER.trace("Assumed role with web identity");
      var credentials = roleResponse.getCredentials();

      // Extract the session credentials
      return new BasicSessionCredentials(
          credentials.getAccessKeyId(),
          credentials.getSecretAccessKey(),
          credentials.getSessionToken());
    } finally {
      if (stsClient != null) {
        stsClient.shutdown();
      }
    }
  }

  private static AWSSecurityTokenService createStsClient(Regions awsRegion) {
    final var stsClientBuilder =
        AWSSecurityTokenServiceClientBuilder.standard().withRegion(awsRegion);
    if (!CommonUtils.appendOptionalTelepresencePath("foo").equals("foo")) {
      stsClientBuilder.setCredentials(
          WebIdentityTokenCredentialsProvider.builder()
              .webIdentityTokenFile(
                  CommonUtils.appendOptionalTelepresencePath(
                      System.getenv(S3Client.AWS_WEB_IDENTITY_TOKEN_FILE)))
              .build());
    }
    return stsClientBuilder.build();
  }

  private AssumeRoleWithWebIdentityRequest getCredentialFromWebIdentity() throws IOException {
    String roleArn = System.getenv(S3Client.AWS_ROLE_ARN);
    String token = getWebIdentityToken();

    // Obtain credentials for the IAM role. Note that you cannot assume the role of
    // an AWS root account;
    // Amazon S3 will deny access. You must use credentials for an IAM user or an
    // IAM role.
    return new AssumeRoleWithWebIdentityRequest()
        .withDurationSeconds(durationSeconds) /*900 seconds (15 minutes)*/
        .withRoleArn(roleArn)
        .withWebIdentityToken(token)
        .withRoleSessionName("model_db_" + UUID.randomUUID());
  }

  private static String getWebIdentityToken() throws IOException {
    return new String(
        Files.readAllBytes(
            Paths.get(
                CommonUtils.appendOptionalTelepresencePath(
                    System.getenv(S3Client.AWS_WEB_IDENTITY_TOKEN_FILE)))));
  }
}
