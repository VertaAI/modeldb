/*
 * Note: this code has been pulled in from the OpenTelemetry project.
 *
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.verta.modeldb.common.metrics.otelprom;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.internal.export.MetricProducer;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class PrometheusCollector implements MetricReader {

  private final Collector collector;
  private volatile MetricProducer metricProducer = MetricProducer.noop();

  PrometheusCollector() {
    this.collector = new CollectorImpl(() -> getMetricProducer().collectAllMetrics());
    this.collector.register();
  }

  /**
   * Returns a new {@link PrometheusCollector} to be registered with a {@link
   * io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder}.
   */
  public static PrometheusCollector create() {
    return new PrometheusCollector();
  }

  private MetricProducer getMetricProducer() {
    return metricProducer;
  }

  @Override
  public void register(CollectionRegistration registration) {
    this.metricProducer = MetricProducer.asMetricProducer(registration);
  }

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return AggregationTemporality.CUMULATIVE;
  }

  // Prometheus cannot flush.
  @Override
  public CompletableResultCode forceFlush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    CollectorRegistry.defaultRegistry.unregister(collector);
    return CompletableResultCode.ofSuccess();
  }

  private static class CollectorImpl extends Collector {

    private final Supplier<Collection<MetricData>> metricSupplier;

    private CollectorImpl(Supplier<Collection<MetricData>> metricSupplier) {
      this.metricSupplier = metricSupplier;
    }

    @Override
    public List<MetricFamilySamples> collect() {
      Collection<MetricData> allMetrics = metricSupplier.get();
      List<MetricFamilySamples> allSamples = new ArrayList<>(allMetrics.size());
      for (MetricData metricData : allMetrics) {
        allSamples.add(MetricAdapter.toMetricFamilySamples(metricData));
      }
      return Collections.unmodifiableList(allSamples);
    }
  }
}
