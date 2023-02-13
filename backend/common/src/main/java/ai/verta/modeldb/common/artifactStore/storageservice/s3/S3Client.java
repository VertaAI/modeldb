package ai.verta.modeldb.common.artifactStore.storageservice.s3;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.config.S3Config;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// S3Client provides a wrapper to the regular AmazonS3 object. The goal is to ensure that the
// AmazonS3 object is
// _always_ valid when performing operations. If it is not, then that's a bug. This simplifies the
// handling of the API
// calls so that we don't have to keep worrying about refreshing the credentials on every call.
// We use a reference counter to keep track of when we have to shutdown a previous client that
// should not be used anymore.
public class S3Client {
  private static final Logger LOGGER = LogManager.getLogger(S3Service.class);

  private final String bucketName;

  private AmazonS3 s3Client;
  private AtomicInteger referenceCounter;
  private AWSCredentials awsCredentials;

  public S3Client(S3Config s3Config) throws IOException, ModelDBException {
    String cloudAccessKey = s3Config.getCloudAccessKey();
    String cloudSecretKey = s3Config.getCloudSecretKey();
    String minioEndpoint = s3Config.getMinioEndpoint();
    Regions awsRegion = Regions.fromName(s3Config.getAwsRegion());
    this.bucketName = s3Config.getCloudBucketName();

    // Start the counter with one because this class has a reference to it
    referenceCounter = new AtomicInteger(1);

    if (cloudAccessKey != null && cloudSecretKey != null) {
      if (minioEndpoint == null) {
        LOGGER.info("config based credentials based s3 client");
        initializeS3ClientWithAccessKey(cloudAccessKey, cloudSecretKey, awsRegion);
      } else {
        LOGGER.info("minio client");
        initializeMinioClient(cloudAccessKey, cloudSecretKey, awsRegion, minioEndpoint);
      }
    } else if (CommonUtils.isEnvSet(CommonConstants.AWS_ROLE_ARN)
        && CommonUtils.isEnvSet(CommonConstants.AWS_WEB_IDENTITY_TOKEN_FILE)) {
      LOGGER.info("temporary token based s3 client");
      initializeWithWebIdentity(awsRegion);
    } else {
      LOGGER.info("environment credentials based s3 client");
      // reads credential from OS Environment
      initializeWithEnvironment(awsRegion);
    }
  }

  private void initializeWithEnvironment(Regions awsRegion) {
    this.s3Client = AmazonS3ClientBuilder.standard().withRegion(awsRegion).build();
  }

  private void initializeMinioClient(
      String cloudAccessKey, String cloudSecretKey, Regions awsRegion, String minioEndpoint) {
    awsCredentials = new BasicAWSCredentials(cloudAccessKey, cloudSecretKey);
    var clientConfiguration = new ClientConfiguration();
    clientConfiguration.setSignerOverride("VertaSignOverrideS3Signer");
    SignerFactory.registerSigner("VertaSignOverrideS3Signer", SignOverrideS3Signer.class);

    this.s3Client =
        AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(minioEndpoint, awsRegion.getName()))
            .withPathStyleAccessEnabled(true)
            .withClientConfiguration(clientConfiguration)
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .build();
  }

  private void initializeS3ClientWithAccessKey(
      String cloudAccessKey, String cloudSecretKey, Regions awsRegion) {
    awsCredentials = new BasicAWSCredentials(cloudAccessKey, cloudSecretKey);
    this.s3Client =
        AmazonS3ClientBuilder.standard()
            .withRegion(awsRegion)
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .build();
  }

  public RefCountedS3Client getRefCountedClient() {
    return new RefCountedS3Client(awsCredentials, s3Client, referenceCounter);
  }

  private void initializeWithWebIdentity(Regions awsRegion) throws IOException {
    var roleCredentials = getCredentialFromWebIdentity(awsRegion);

    var now = Date.from(Instant.now()).getTime();
    /* While creating RoleCredentials we have set time (900 seconds (15 minutes))
    in the AssumeRoleWithWebIdentityRequest so here expiration will be 900 seconds */
    var expiration = roleCredentials.getExpiration().getTime();
    // set cron to half of the duration of the credentials which will be ~(450 Second (7.5 minutes))
    var refreshTokenFrequency = (expiration - now) / 2;
    LOGGER.info(String.format("S3 Client refresh frequency %d ms", refreshTokenFrequency));

    CommonUtils.scheduleTask(
        new RefreshS3ClientCron(bucketName, awsRegion, roleCredentials, this),
        refreshTokenFrequency /*initialDelay*/,
        refreshTokenFrequency /*periodic refresh frequency*/,
        TimeUnit.MILLISECONDS);
  }

  void refreshS3Client(AWSCredentials awsCredentials, AmazonS3 s3Client) {
    // Once we get to this point, we know that we have a good new s3 client, so it's time to swap
    // it. No fail can happen now
    LOGGER.debug("replacing S3 Client");
    try (RefCountedS3Client client = getRefCountedClient()) {
      // Decrement the current reference counter represented by this object pointing to it
      this.referenceCounter.decrementAndGet();

      // Swap the references
      this.referenceCounter = new AtomicInteger(1);
      this.awsCredentials = awsCredentials;
      this.s3Client = s3Client;
      LOGGER.debug("S3 Client replaced");
      // At the end of the try, the reference counter will be decremented again and shutdown will
      // be
      // called
    }
  }

  private Credentials getCredentialFromWebIdentity(Regions awsRegion) throws IOException {
    AWSSecurityTokenService stsClient = null;
    try {
      stsClient = createStsClient(awsRegion);
      String roleArn = System.getenv(CommonConstants.AWS_ROLE_ARN);
      String token = getWebIdentityToken();

      // Obtain credentials for the IAM role. Note that you cannot assume the role of
      // an AWS root account;
      // Amazon S3 will deny access. You must use credentials for an IAM user or an
      // IAM role.
      AssumeRoleWithWebIdentityRequest roleRequest =
          new AssumeRoleWithWebIdentityRequest()
              .withDurationSeconds(900) /*900 seconds (15 minutes)*/
              .withRoleArn(roleArn)
              .withWebIdentityToken(token)
              .withRoleSessionName("model_db_" + UUID.randomUUID());

      LOGGER.info("assuming role with web identity");
      // Call STS to assume the role
      AssumeRoleWithWebIdentityResult roleResponse =
          stsClient.assumeRoleWithWebIdentity(roleRequest);
      LOGGER.info("assumed role with web identity");
      return roleResponse.getCredentials();
    } finally {
      if (stsClient != null) {
        stsClient.shutdown();
      }
    }
  }

  private static String getWebIdentityToken() throws IOException {
    return new String(
        Files.readAllBytes(
            Paths.get(
                CommonUtils.appendOptionalTelepresencePath(
                    System.getenv(CommonConstants.AWS_WEB_IDENTITY_TOKEN_FILE)))));
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
