package ai.verta.modeldb.artifactStore.storageservice.s3;

import ai.verta.modeldb.cron_jobs.CronJobUtils;
import com.amazonaws.services.s3.AmazonS3;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RefCountedS3Client implements AutoCloseable {
  private static final Logger LOGGER = LogManager.getLogger(CronJobUtils.class);
  private AmazonS3 s3Client;
  private AtomicInteger referenceCounter;

  RefCountedS3Client(AmazonS3 client, AtomicInteger counter) {
    s3Client = client;
    referenceCounter = counter;
    referenceCounter.incrementAndGet();
  }

  public AmazonS3 getClient() {
    return s3Client;
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
