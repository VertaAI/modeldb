package ai.verta.modeldb.common.configuration;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.metrics.otelprom.PrometheusCollector;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import io.opentelemetry.instrumentation.resources.ContainerResource;
import io.opentelemetry.instrumentation.resources.HostResource;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentracing.Tracer;
import io.opentracing.contrib.jdbc.TracingDriver;
import io.opentracing.util.GlobalTracer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
          JaegerGrpcSpanExporter.builder()
              .setEndpoint(System.getenv("OTEL_EXPORTER_JAEGER_ENDPOINT"))
              .build();
      tracerProvider =
          SdkTracerProvider.builder()
              .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
              .setSampler(sampler)
              .setResource(resource)
              .build();
    }
    SdkMeterProvider sdkMeterProvider = initializeMetricSdk();

    OpenTelemetrySdk openTelemetry =
        OpenTelemetrySdk.builder()
            .setMeterProvider(sdkMeterProvider)
            .setTracerProvider(tracerProvider)
            .setPropagators(
                ContextPropagators.create(
                    TextMapPropagator.composite(
                        W3CTraceContextPropagator.getInstance(), JaegerPropagator.getInstance())))
            .buildAndRegisterGlobal();
    initializeOpenTracingShim(openTelemetry);
    return openTelemetry;
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

  private void initializeOpenTracingShim(OpenTelemetry openTelemetry) {
    Tracer tracerShim = OpenTracingShim.createTracerShim(openTelemetry);
    GlobalTracer.registerIfAbsent(tracerShim);
    TracingDriver.load();
    TracingDriver.setInterceptorMode(true);
    TracingDriver.setInterceptorProperty(true);
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
}
