package ai.verta.modeldb.common.artifactStore.storageservice.s3;

import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.config.S3Config;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.utils.Utils;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
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

  private String bucketName;
  private Regions awsRegion;

  private AmazonS3 s3Client;
  private AtomicInteger referenceCounter;
  private AWSCredentials awsCredentials;

  public S3Client(String cloudBucketName, S3Config config) throws IOException, ModelDBException {
    String cloudAccessKey = config.cloudAccessKey;
    String cloudSecretKey = config.cloudSecretKey;
    String minioEndpoint = config.minioEndpoint;
    awsRegion = Regions.fromName(config.awsRegion);
    this.bucketName = cloudBucketName;

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
    } else if (Utils.isEnvSet(ModelDBConstants.AWS_ROLE_ARN)
        && Utils.isEnvSet(ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE)) {
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
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setSignerOverride("AWSS3V4SignerType");

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
    String roleSessionName = "modelDB" + UUID.randomUUID().toString();

    AWSSecurityTokenService stsClient = null;
    AmazonS3 newS3Client = null;
    try {
      stsClient = AWSSecurityTokenServiceClientBuilder.standard().withRegion(awsRegion).build();

      String roleArn = System.getenv(ModelDBConstants.AWS_ROLE_ARN);

      String token =
          new String(
              Files.readAllBytes(
                  Paths.get(
                      CommonUtils.appendOptionalTelepresencePath(
                          System.getenv(ModelDBConstants.AWS_WEB_IDENTITY_TOKEN_FILE)))));

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
      AssumeRoleWithWebIdentityResult roleResponse =
          stsClient.assumeRoleWithWebIdentity(roleRequest);

      Credentials credentials = roleResponse.getCredentials();

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

      newS3Client.doesBucketExistV2(bucketName);

      // Start a thread that will refresh the token. It will just retry for as long as we get an
      // exception and die right after.
      LOGGER.debug("scheduling refresh");
      Thread thread =
          new Thread(
              () -> {
                Long now = System.currentTimeMillis();
                Long expiration = credentials.getExpiration().getTime();
                // Wait for half of the duration of the credentials
                Long waitPeriod = (expiration - now) / 2;

                LOGGER.debug(String.format("sleeping for %d ms", waitPeriod));

                try {
                  Thread.sleep(waitPeriod);
                } catch (InterruptedException e) {
                  // Continue as this just refreshes the session earlier
                }

                LOGGER.debug("starting refresh");

                // Loop forever until we get to refresh without an exception
                while (true) {
                  try {
                    // Sleep for a second to avoid overwhelming the service
                    Thread.sleep(1000);
                    initializeWithTemporaryCredentials(awsRegion);
                    return;
                  } catch (Exception ex) {
                    LOGGER.warn("Failed to refresh S3 session: " + ex.getMessage());
                    continue;
                  }
                }
              });
      thread.setDaemon(true);
      thread.start();

      // Once we get to this point, we know that we have a good new s3 client, so it's time to swap
      // it. No fail can happen now
      LOGGER.debug("replacing client");
      try (RefCountedS3Client client = getRefCountedClient()) {
        // Decrement the current reference counter represented by this object pointing to it
        referenceCounter.decrementAndGet();

        // Swap the references
        referenceCounter = new AtomicInteger(1);
        s3Client = newS3Client;

        // At the end of the try, the reference counter will be decremented again and shutdown will
        // be
        // called
      }

    } finally {
      if (stsClient != null) stsClient.shutdown();
      // Cleanup in case we couldn't perform the switch
      if (newS3Client != null && newS3Client != s3Client) newS3Client.shutdown();
    }
  }

  public RefCountedS3Client getRefCountedClient() {
    return new RefCountedS3Client(awsCredentials, s3Client, referenceCounter);
  }
}
