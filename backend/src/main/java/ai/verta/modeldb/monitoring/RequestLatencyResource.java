package ai.verta.modeldb.monitoring;

import io.prometheus.client.Histogram;
import java.io.Closeable;

public class RequestLatencyResource implements Closeable {
  private static final Histogram requestLatency =
      Histogram.build()
          .labelNames("grpc_method")
          .name("verta_backend_requests_latency_seconds")
          .help("Request latency in seconds.")
          .buckets(
              0.2, 0.4, 0.6, 0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.2, 2.4, 2.6, 2.8, 3.0, 3.2, 3.4,
              3.6, 3.8, 4.0)
          .register();
  private final Histogram.Timer timer;

  public RequestLatencyResource(String label) {
    timer = requestLatency.labels(label).startTimer();
  }

  @Override
  public void close() {
    timer.observeDuration();
  }
}
