package ai.verta.modeldb.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.catalina.connector.Connector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

public class GracefulShutdown
    implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {

  private Connector connector;
  private final long shutdownTimeout;
  private static final Logger LOGGER = LogManager.getLogger(GracefulShutdown.class);

  public GracefulShutdown(Long shutdownTimeout) {
    this.shutdownTimeout = shutdownTimeout;
  }

  @Override
  public void customize(Connector connector) {
    this.connector = connector;
  }

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    LOGGER.info("GracefulShutdown starting for Spring server");
    this.connector.pause();
    var executor = this.connector.getProtocolHandler().getExecutor();
    if (executor instanceof ExecutorService) {
      try {
        var executorService = (ExecutorService) executor;
        executorService.shutdown();
        long pollInterval = 5L;
        long timeoutRemaining = shutdownTimeout;
        while (timeoutRemaining > pollInterval
            && !executorService.awaitTermination(pollInterval, TimeUnit.SECONDS)) {
          LOGGER.info("Spring server executor service awaiting termination after shutdown");
          timeoutRemaining -= pollInterval;
        }

        if (!executorService.awaitTermination(timeoutRemaining, TimeUnit.SECONDS)) {
          LOGGER.warn(
              "Spring server executor service did not shut down gracefully within "
                  + "{} seconds. Proceeding with forceful shutdown",
              shutdownTimeout);

          executorService.shutdownNow();

          // Give the service one more poll interval to finish shutting down
          if (!executorService.awaitTermination(pollInterval, TimeUnit.SECONDS)) {
            LOGGER.error("Spring server executor service did not terminate");
          }
        } else {
          LOGGER.info("*** Spring server Shutdown ***");
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
    LOGGER.info("GracefulShutdown finished for Spring server");
  }
}
