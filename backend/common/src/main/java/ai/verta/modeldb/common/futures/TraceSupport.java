package ai.verta.modeldb.common.futures;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.ActiveSpanContextSource;
import io.opentracing.contrib.grpc.ActiveSpanSource;
import io.opentracing.util.GlobalTracer;
import java.util.Map;
import java.util.function.Supplier;

public class TraceSupport {
  private TraceSupport() {}

  public static <T> T traceNonFuture(
      Supplier<T> supplier, String operationName, Map<String, String> tags) {
    if (!GlobalTracer.isRegistered()) return supplier.get();

    final var tracer = GlobalTracer.get();

    final var spanContext = TraceSupport.getActiveSpanContext(tracer);
    final var span = TraceSupport.createSpanFromParent(tracer, spanContext, operationName, tags);

    try (final var scope = tracer.scopeManager().activate(span)) {
      return supplier.get();
    } finally {
      span.finish();
    }
  }

  public static SpanContext getActiveSpanContext(Tracer tracer) {
    Span activeSpan = ActiveSpanSource.GRPC_CONTEXT.getActiveSpan();
    if (activeSpan != null) {
      return activeSpan.context();
    }

    SpanContext spanContext = ActiveSpanContextSource.GRPC_CONTEXT.getActiveSpanContext();
    if (spanContext != null) {
      return spanContext;
    }

    return tracer.activeSpan() != null ? tracer.activeSpan().context() : null;
  }

  public static Span createSpanFromParent(
      Tracer tracer,
      SpanContext parentSpanContext,
      String operationName,
      Map<String, String> tags) {
    Tracer.SpanBuilder spanBuilder;
    if (parentSpanContext == null) {
      spanBuilder = tracer.buildSpan(operationName);
    } else {
      spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpanContext);
    }

    if (tags != null) {
      for (var entry : tags.entrySet()) {
        spanBuilder = spanBuilder.withTag(entry.getKey(), entry.getValue());
      }
    }

    return spanBuilder.start();
  }
}
