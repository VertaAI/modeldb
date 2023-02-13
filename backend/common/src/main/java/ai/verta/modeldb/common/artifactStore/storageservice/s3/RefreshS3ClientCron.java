package ai.verta.modeldb.common.artifactStore.storageservice.s3;

import ai.verta.modeldb.common.CommonConstants;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RefreshS3ClientCron implements Runnable {
  private static final Logger LOGGER = LogManager.getLogger(RefreshS3ClientCron.class);
  private final String bucketName;
  private final Regions awsRegion;
  private final AssumeRoleWithWebIdentityRequest roleRequest;
  private final S3Client s3Client;

  public RefreshS3ClientCron(
      String bucketName,
      Regions awsRegion,
      AssumeRoleWithWebIdentityRequest roleRequest,
      S3Client s3Client) {
    this.bucketName = bucketName;
    this.awsRegion = awsRegion;
    this.roleRequest = roleRequest;
    this.s3Client = s3Client;
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("Refreshing S3Client cron wakeup");
    try {
      var awsCredentials = getBasicSessionCredentials();
      createAndRefreshNewClient(awsCredentials);
    } catch (Throwable ex) {
      LOGGER.error("Failed to refresh S3 Client: " + ex.getMessage(), ex);
      throw ex;
    }
    LOGGER.info("Refreshing S3Client finish tasks and reschedule");
  }

  private void createAndRefreshNewClient(BasicSessionCredentials awsCredentials) {
    LOGGER.info("Creating new S3 Client");
    var newS3Client =
        AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .withRegion(awsRegion)
            .build();

    newS3Client.doesBucketExistV2(bucketName);
    LOGGER.info("New S3 Client created");

    s3Client.refreshS3Client(awsCredentials, newS3Client);
  }

  private BasicSessionCredentials getBasicSessionCredentials() {
    AWSSecurityTokenService stsClient = null;
    try {
      LOGGER.info("Creating sts client");
      stsClient = createStsClient(awsRegion);
      LOGGER.info("Sts client created");

      LOGGER.info("assuming role with web identity");
      // Call STS to assume the role
      AssumeRoleWithWebIdentityResult roleResponse =
          stsClient.assumeRoleWithWebIdentity(roleRequest);
      LOGGER.info("Assumed role with web identity");
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
                      System.getenv(CommonConstants.AWS_WEB_IDENTITY_TOKEN_FILE)))
              .build());
    }
    return stsClientBuilder.build();
  }
}
