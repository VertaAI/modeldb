package ai.verta.modeldb.common.metrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;

public class ThreadSchedulingMonitor {

  public ThreadSchedulingMonitor(OpenTelemetry openTelemetry) {
    Meter meter = openTelemetry.getMeter("verta.uac");
    LongHistogram histogram =
        meter
            .histogramBuilder("thread_lateness")
            .ofLongs()
            .setDescription("How late a thread gets scheduled in microseconds")
            .setUnit("ms")
            .build();

    Thread monitoringThread =
        new Thread(
            () -> {
              while (true) {
                try {
                  long startNanos = System.nanoTime();
                  Thread.sleep(100);
                  long endNanos = System.nanoTime();
                  long elapsedNanos = endNanos - startNanos;
                  long elapsedMicros = elapsedNanos / 1_000;
                  long overtime = elapsedMicros - 100_000;
                  histogram.record(overtime);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  break;
                }
              }
            });
    monitoringThread.setDaemon(true);
    monitoringThread.start();
  }
}
