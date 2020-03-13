package ai.verta.modeldb;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

public class GracefulShutdown
    implements TomcatConnectorCustomizer, ApplicationListener<ContextClosedEvent> {

  private volatile Connector connector;
  private long shutdownTimeout;

  public GracefulShutdown(Long shutdownTimeout) {
    this.shutdownTimeout = shutdownTimeout;
  }

  @Override
  public void customize(Connector connector) {
    this.connector = connector;
  }

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    this.connector.pause();
    Executor executor = this.connector.getProtocolHandler().getExecutor();
    if (executor instanceof ThreadPoolExecutor) {
      try {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
        int nfsActiveRequestCount = threadPoolExecutor.getActiveCount();

        while (nfsActiveRequestCount > 0) {
          nfsActiveRequestCount = threadPoolExecutor.getActiveCount();
          System.err.println("NFS Active Request Count in while: " + nfsActiveRequestCount);
          try {
            Thread.sleep(1000); // wait for 1s
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        threadPoolExecutor.shutdown();
        if (!threadPoolExecutor.awaitTermination(shutdownTimeout, TimeUnit.SECONDS)) {
          System.err.println(
              "NFS Server thread pool did not shut down gracefully within "
                  + shutdownTimeout
                  + " seconds. Proceeding with forceful shutdown");

          threadPoolExecutor.shutdownNow();

          if (!threadPoolExecutor.awaitTermination(shutdownTimeout, TimeUnit.SECONDS)) {
            System.err.println("NFS Server thread pool did not terminate");
          }
        } else {
          System.err.println("*** NFS Server Shutdown ***");
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
