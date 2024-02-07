package ai.verta.modeldb.common.configuration;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import ai.verta.modeldb.common.config.Config;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.metrics.prometheus.clientbridge.PrometheusCollector;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import io.opentelemetry.instrumentation.resources.ContainerResource;
import io.opentelemetry.instrumentation.resources.HostResource;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ResourceAttributes;
import io.opentelemetry.semconv.SemanticAttributes;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class OpenTelemetryConfig {

  @Bean
  @ConditionalOnMissingBean(Sampler.class) // allow to be overridden by service-specific config
  public Sampler openTelemetrySampler() {
    return Sampler.alwaysOn();
  }

  @Bean
  @ConditionalOnBean(Config.class)
  public OpenTelemetry openTelemetry(Config config, Sampler sampler, Resource resource) {
    boolean tracingEnabled = config.tracingEnabled();

    SdkTracerProvider tracerProvider;
    if (!tracingEnabled) {
      tracerProvider = SdkTracerProvider.builder().build();
    } else {
      SpanExporter spanExporter =
          OtlpHttpSpanExporter.builder()
              .setEndpoint(System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT"))
              .build();
      tracerProvider =
          SdkTracerProvider.builder()
              .addSpanProcessor(new JaegerCompatibilitySpanProcessor())
              .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
              .setSampler(sampler)
              .setResource(resource)
              .build();
    }
    SdkMeterProvider sdkMeterProvider = initializeMetricSdk();

    try {
      return OpenTelemetrySdk.builder()
          .setMeterProvider(sdkMeterProvider)
          .setTracerProvider(tracerProvider)
          .setPropagators(
              ContextPropagators.create(
                  TextMapPropagator.composite(
                      W3CTraceContextPropagator.getInstance(), JaegerPropagator.getInstance())))
          .buildAndRegisterGlobal();
    } catch (IllegalStateException e) {
      log.warn(
          "OpenTelemetry has already been initialized.... returning whatever is in the global.");
      return GlobalOpenTelemetry.get();
    }
  }

  private static SdkMeterProvider initializeMetricSdk() {
    return SdkMeterProvider.builder()
        .registerMetricReader(PrometheusCollector.create())
        .registerView(
            InstrumentSelector.builder().setName("future_exec_delay").build(),
            View.builder()
                .setAggregation(
                    Aggregation.explicitBucketHistogram(generateExponentialBucketBoundaries(5)))
                .build())
        .registerView(
            InstrumentSelector.builder().setName("thread_lateness").build(),
            View.builder()
                .setAggregation(
                    Aggregation.explicitBucketHistogram(generateExponentialBucketBoundaries(5)))
                .build())
        .build();
  }

  @Bean
  public Resource resource() {
    return Resource.getDefault()
        .merge(
            Resource.create(
                Attributes.of(
                    ResourceAttributes.SERVICE_NAME,
                    System.getenv("OTEL_SERVICE_NAME"),
                    stringKey("kubernetes.namespace"),
                    System.getenv("POD_NAMESPACE"))))
        .merge(HostResource.get())
        .merge(ContainerResource.get());
  }

  /**
   * Generate some histogram bucket boundaries. Will generate 15 boundaries, doubling each time,
   * starting with the requested power of 2.
   */
  private static List<Double> generateExponentialBucketBoundaries(int initialPowerOfTwo) {
    AtomicInteger a = new AtomicInteger(initialPowerOfTwo);
    return Stream.generate(() -> Math.pow(2, a.getAndIncrement()))
        .limit(15)
        .collect(Collectors.toList());
  }

  @Bean
  public GrpcTelemetry grpcTelemetry(OpenTelemetry openTelemetry) {
    return GrpcTelemetry.create(openTelemetry);
  }

  @Bean
  public ServerInterceptor grpcServerInterceptor(GrpcTelemetry grpcTelemetry) {
    return grpcTelemetry.newServerInterceptor();
  }

  @Bean
  public ClientInterceptor grpcClientInterceptor(GrpcTelemetry grpcTelemetry) {
    return grpcTelemetry.newClientInterceptor();
  }

  /** Jaeger works better if a peer.service is set on db spans, so add it using a span processor. */
  private static class JaegerCompatibilitySpanProcessor implements SpanProcessor {
    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
      String dbName = span.getAttribute(SemanticAttributes.DB_NAME);
      if (dbName != null) {
        String serverAddress = span.getAttribute(SemanticAttributes.SERVER_ADDRESS);
        if (serverAddress != null) {
          span.setAttribute(SemanticAttributes.PEER_SERVICE, dbName + "[" + serverAddress + "]");
        }
      }
    }

    @Override
    public boolean isStartRequired() {
      return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
      // nothing needed here
    }

    @Override
    public boolean isEndRequired() {
      return false;
    }
  }
}
