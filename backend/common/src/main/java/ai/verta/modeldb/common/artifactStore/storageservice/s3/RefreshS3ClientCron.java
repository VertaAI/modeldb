package ai.verta.modeldb.common.artifactStore.storageservice.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.model.Credentials;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RefreshS3ClientCron extends TimerTask {
  private static final Logger LOGGER = LogManager.getLogger(RefreshS3ClientCron.class);
  private final String bucketName;
  private final Regions awsRegion;
  private final Credentials credentials;
  private final S3Client s3Client;

  public RefreshS3ClientCron(
      String bucketName, Regions awsRegion, Credentials credentials, S3Client s3Client) {
    this.bucketName = bucketName;
    this.awsRegion = awsRegion;
    this.credentials = credentials;
    this.s3Client = s3Client;
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.trace("Refreshing S3Client cron wakeup");
    try {
      // Extract the session credentials
      var awsCredentials =
          new BasicSessionCredentials(
              credentials.getAccessKeyId(),
              credentials.getSecretAccessKey(),
              credentials.getSessionToken());

      LOGGER.debug("creating new S3 Client");

      var newS3Client =
          AmazonS3ClientBuilder.standard()
              .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
              .withRegion(awsRegion)
              .build();

      newS3Client.doesBucketExistV2(bucketName);
      s3Client.refreshS3Client(awsCredentials, newS3Client);
    } catch (Throwable ex) {
      LOGGER.error("Failed to refresh S3 Client: " + ex.getMessage(), ex);
      throw ex;
    }
    LOGGER.trace("Refreshing S3Client finish tasks and reschedule");
  }
}
