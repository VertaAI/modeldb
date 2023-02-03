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
import java.util.UUID;
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
        LOGGER.debug("config based credentials based s3 client");
        initializeS3ClientWithAccessKey(cloudAccessKey, cloudSecretKey, awsRegion);
      } else {
        LOGGER.debug("minio client");
        initializeMinioClient(cloudAccessKey, cloudSecretKey, awsRegion, minioEndpoint);
      }
    } else if (CommonUtils.isEnvSet(CommonConstants.AWS_ROLE_ARN)
        && CommonUtils.isEnvSet(CommonConstants.AWS_WEB_IDENTITY_TOKEN_FILE)) {
      LOGGER.debug("temporary token based s3 client");
      initializeWithTemporaryCredentials(awsRegion);
    } else {
      LOGGER.debug("environment credentials based s3 client");
      // reads credential from OS Environment
      initializetWithEnvironment(awsRegion);
    }
  }

  private void initializetWithEnvironment(Regions awsRegion) {
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

  private void initializeWithTemporaryCredentials(Regions awsRegion)
      throws IOException, ModelDBException {
    String roleSessionName = "modelDB" + UUID.randomUUID();

    AWSSecurityTokenService stsClient = null;
    AmazonS3 newS3Client = null;
    try {
      stsClient = createStsClient(awsRegion, stsClient);

      Credentials credentials = getCredentials(roleSessionName, stsClient);

      newS3Client = createNewS3Client(awsRegion, newS3Client, credentials);

      // this call will fail with an exception if the credentials are out of date, causing us not to
      // start the refresh thread below.
      newS3Client.doesBucketExistV2(bucketName);

      startRefreshThread(awsRegion, credentials);

      // Once we get to this point, we know that we have a good new s3 client, so it's time to swap
      // it. No fail can happen now
      LOGGER.debug("replacing client");
      try (RefCountedS3Client ignored = getRefCountedClient()) {
        // Decrement the current reference counter represented by this object pointing to it
        referenceCounter.decrementAndGet();

        // Swap the references
        referenceCounter = new AtomicInteger(1);
        s3Client = newS3Client;

        // At the end of the try, the reference counter will be decremented again and shutdown will
        // be called
      }

    } finally {
      if (stsClient != null) {
        stsClient.shutdown();
      }
      // Cleanup in case we couldn't perform the switch
      if (newS3Client != null && newS3Client != s3Client) {
        newS3Client.shutdown();
      }
    }
  }

  private AmazonS3 createNewS3Client(
      Regions awsRegion, AmazonS3 newS3Client, Credentials credentials) {
    // Extract the session credentials
    awsCredentials =
        new BasicSessionCredentials(
            credentials.getAccessKeyId(),
            credentials.getSecretAccessKey(),
            credentials.getSessionToken());

    LOGGER.debug("creating new client");

    newS3Client =
        AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .withRegion(awsRegion)
            .build();
    return newS3Client;
  }

  private static Credentials getCredentials(
      String roleSessionName, AWSSecurityTokenService stsClient) throws IOException {
    String roleArn = System.getenv(CommonConstants.AWS_ROLE_ARN);

    var token =
        new String(
            Files.readAllBytes(
                Paths.get(
                    CommonUtils.appendOptionalTelepresencePath(
                        System.getenv(CommonConstants.AWS_WEB_IDENTITY_TOKEN_FILE)))));

    // Obtain credentials for the IAM role. Note that you cannot assume the role of
    // an AWS root account;
    // Amazon S3 will deny access. You must use credentials for an IAM user or an
    // IAM role.
    AssumeRoleWithWebIdentityRequest roleRequest =
        new AssumeRoleWithWebIdentityRequest()
            .withDurationSeconds(900)
            .withRoleArn(roleArn)
            .withWebIdentityToken(token)
            .withRoleSessionName(roleSessionName);

    LOGGER.debug("assuming role");
    // Call STS to assume the role
    AssumeRoleWithWebIdentityResult roleResponse = stsClient.assumeRoleWithWebIdentity(roleRequest);

    return roleResponse.getCredentials();
  }

  private static AWSSecurityTokenService createStsClient(
      Regions awsRegion, AWSSecurityTokenService stsClient) {
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
    stsClient = stsClientBuilder.build();
    return stsClient;
  }

  private void startRefreshThread(Regions awsRegion, Credentials credentials) {
    // Start a thread that will refresh the token. It will just retry for as long as we get an
    // exception and die right after.
    LOGGER.debug("scheduling refresh");
    var thread =
        new Thread(
            () -> {
              Long now = System.currentTimeMillis();
              Long expiration = credentials.getExpiration().getTime();
              // Wait for half of the duration of the credentials
              long waitPeriod = (expiration - now) / 2;

              LOGGER.debug(String.format("sleeping for %d ms", waitPeriod));

              try {
                Thread.sleep(waitPeriod);
              } catch (InterruptedException e) {
                // Restore interrupted state and return.
                Thread.currentThread().interrupt();
                return;
              }

              LOGGER.debug("starting refresh");

              // Loop forever until we get to refresh without an exception
              while (true) {
                try {
                  // Sleep for a second to avoid overwhelming the service
                  Thread.sleep(1000);
                  initializeWithTemporaryCredentials(awsRegion);
                  return;
                } catch (InterruptedException e) {
                  // Restore interrupted state and break out of the loop
                  Thread.currentThread().interrupt();
                  break;
                } catch (Exception ex) {
                  LOGGER.warn("Failed to refresh S3 session: " + ex.getMessage());
                }
              }
            });
    thread.setDaemon(true);
    thread.start();
  }

  public RefCountedS3Client getRefCountedClient() {
    return new RefCountedS3Client(awsCredentials, s3Client, referenceCounter);
  }
}
