package ai.verta.modeldb.common.artifactstore.storageservice.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RefCountedS3Client implements AutoCloseable {
  private static final Logger LOGGER = LogManager.getLogger(RefCountedS3Client.class);
  private final AWSCredentials credentials;
  private final AmazonS3 s3Client;
  private final AtomicInteger referenceCounter;

  RefCountedS3Client(AWSCredentials credentials, AmazonS3 client, AtomicInteger counter) {
    this.credentials = credentials;
    s3Client = client;
    referenceCounter = counter;
    referenceCounter.incrementAndGet();
  }

  public AmazonS3 getClient() {
    return s3Client;
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
    }
  }
}
