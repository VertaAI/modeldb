package ai.verta.modeldb.common.artifactStore.storageservice.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Data
@Getter
@Setter(AccessLevel.NONE)
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

  @Override
  public void close() {
    if (referenceCounter.decrementAndGet() == 0 && s3Client != null) {
      LOGGER.debug("shutting client down");
      s3Client.shutdown();
    }
  }
}
