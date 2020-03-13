package ai.verta.modeldb.monitoring;

import io.prometheus.client.Counter;

public class QPSCountResource {
  private QPSCountResource() {}

  private static final Counter qpsCountRequests =
      Counter.build()
          .subsystem("verta_backend")
          .name("query_per_second_total")
          .help("Total QPS requests started on the server.")
          .register();

  public static void inc() {
    qpsCountRequests.inc();
  }
}
