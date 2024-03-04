package ai.verta.modeldb.common.artifactStore.storageservice.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class RefCountedS3Client implements AutoCloseable {
  private static final Logger LOGGER = LogManager.getLogger(RefCountedS3Client.class);
  private final AWSCredentials credentials;
  private final AmazonS3 s3Client;
  private final S3AsyncClient asyncClient;
  private final AtomicInteger referenceCounter;

  RefCountedS3Client(
      AWSCredentials credentials,
      AmazonS3 client,
      S3AsyncClient asyncClient,
      AtomicInteger counter) {
    this.credentials = credentials;
    this.s3Client = client;
    this.asyncClient = asyncClient;
    this.referenceCounter = counter;
    this.referenceCounter.incrementAndGet();
  }

  public AmazonS3 getClient() {
    return s3Client;
  }

  public S3AsyncClient getAsyncClient() {
    return asyncClient;
  }

  public AWSCredentials getCredentials() {
    return credentials;
  }

  @Override
  public void close() {
    if (referenceCounter.decrementAndGet() == 0) {
      if (s3Client != null) {
        LOGGER.debug("shutting client down");
        s3Client.shutdown();
      }
      if (asyncClient != null) {
        asyncClient.close();
      }
    }
  }
}
