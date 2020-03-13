package ai.verta.modeldb.monitoring;

import io.prometheus.client.Histogram;
import java.io.Closeable;

public class RequestLatencyResource implements Closeable {
  private static final Histogram requestLatency =
      Histogram.build()
          .labelNames("grpc_method")
          .name("verta_backend_requests_latency_seconds")
          .help("Request latency in seconds.")
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
