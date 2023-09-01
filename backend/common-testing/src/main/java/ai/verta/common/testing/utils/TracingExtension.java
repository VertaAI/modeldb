package ai.verta.common.testing.utils;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.extension.*;

@Log4j2
public class TracingExtension
    implements BeforeAllCallback,
        AfterAllCallback,
        InvocationInterceptor,
        TestWatcher,
        TestInstancePostProcessor {

  private static OpenTelemetry openTelemetry = OpenTelemetry.noop();

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create("tracing");

  private Tracer getTracer() {
    return openTelemetry.getTracer("tracingExtension");
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {}

  private void maybeStartRootSpan(ExtensionContext context) {
    ExtensionContext.Store store = context.getStore(NAMESPACE);
    Span rootSpan = store.get("rootSpan", Span.class);
    if (rootSpan == null) {
      String spanName =
          context
              .getTestClass()
              .map(clas -> "TestClass: " + clas.getSimpleName())
              .orElse(context.getDisplayName());
      System.out.println("Starting root span with name: " + spanName);
      Span classSpan = getTracer().spanBuilder(spanName).startSpan();
      Scope scope = classSpan.makeCurrent();
      store.put("rootScope", scope);
      store.put("rootSpan", classSpan);
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    finish(context);
  }

  private static void finish(ExtensionContext context) {
    ExtensionContext.Store store = context.getStore(NAMESPACE);
    Span span = store.get("rootSpan", Span.class);
    Scope scope = store.get("rootScope", Scope.class);
    if (scope != null) {
      scope.close();
      store.remove("rootScope");
    }
    if (span != null) {
      span.end();
      store.remove("rootSpan");
      log.info("finished root span for class: " + context.getTestClass());
    }
  }

  @Override
  public void testDisabled(ExtensionContext context, Optional<String> reason) {
    finishTestSpan(context, "disabled", null);
  }

  @Override
  public void testSuccessful(ExtensionContext context) {
    finishTestSpan(context, "success", null);
  }

  @Override
  public void testAborted(ExtensionContext context, Throwable cause) {
    finishTestSpan(context, "aborted", cause);
  }

  @Override
  public void testFailed(ExtensionContext context, Throwable cause) {
    finishTestSpan(context, "failed", cause);
  }

  private void maybeStartTestCaseSpan(ExtensionContext context) {
    ExtensionContext.Store store = context.getStore(NAMESPACE);
    Span testCaseSpan = store.get("testCaseSpan", Span.class);
    if (testCaseSpan == null) {
      String spanName = "TestCase: " + context.getDisplayName();
      log.info("Starting testcase span with name: " + spanName);
      Span classSpan = getTracer().spanBuilder(spanName).startSpan();
      Scope scope = classSpan.makeCurrent();
      store.put("testScope", scope);
      store.put("testCaseSpan", classSpan);
    }
  }

  private static void finishTestSpan(ExtensionContext context, String reason, Throwable cause) {
    ExtensionContext.Store store = context.getStore(NAMESPACE);
    Span span = store.get("testCaseSpan", Span.class);
    Scope scope = store.get("testScope", Scope.class);
    if (scope != null) {
      scope.close();
      store.remove("testScope");
    }
    if (span != null) {
      span.setAttribute("testResult", reason);
      if (cause != null) {
        span.recordException(cause);
        span.setStatus(StatusCode.ERROR);
      }
      span.end();
      store.remove("testCaseSpan");
      log.info("finished test span for testcase: " + context.getDisplayName());
    }
  }

  @Override
  public void interceptBeforeAllMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    maybeStartRootSpan(extensionContext);
    runInSpan("BeforeAll: " + extensionContext.getDisplayName(), invocation::proceed);
  }

  @Override
  public void interceptBeforeEachMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    maybeStartRootSpan(extensionContext);
    maybeStartTestCaseSpan(extensionContext);
    runInSpan("BeforeEach: " + extensionContext.getDisplayName(), invocation::proceed);
  }

  @Override
  public void interceptTestMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    maybeStartRootSpan(extensionContext);
    maybeStartTestCaseSpan(extensionContext);
    runInSpan(extensionContext.getDisplayName(), invocation::proceed);
  }

  @Override
  public void interceptAfterEachMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    runInSpan("AfterEach: " + extensionContext.getDisplayName(), invocation::proceed);
  }

  @Override
  public void interceptAfterAllMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    runInSpan("AfterAll: " + extensionContext.getDisplayName(), invocation::proceed);
  }

  private void runInSpan(String spanName, ThrowingRunnable operation) throws Throwable {
    Span span = getTracer().spanBuilder(spanName).startSpan();
    try (Scope ignored = span.makeCurrent()) {
      operation.run();
    } catch (Throwable t) {
      span.recordException(t);
      span.setStatus(StatusCode.ERROR);
      throw t;
    } finally {
      span.end();
    }
  }

  @Override
  public void postProcessTestInstance(Object o, ExtensionContext extensionContext) {
    maybeStartRootSpan(extensionContext);
  }

  /**
   * This method initiates an OpenTelemetrySdk instance using AutoConfiguredOpenTelemetrySdk
   * builder. It also configures various properties including tracing exporter, propagators, and
   * disabled resource providers.
   *
   * <p>For tracing in tests, consider the following steps:
   *
   * <ul>
   *   <li>Run Jaeger locally using: {@code docker run --rm --name jaeger -p 16686:16686 -p
   *       4317:4317 jaegertracing/all-in-one:1.48}
   *   <li>Set the OTEL_TRACES_EXPORTER environment variable to "otlp" when running the tests.
   *   <li>Extend your test with the TracingExtension: {@code @ExtendWith(TracingExtension.class)}
   * </ul>
   *
   * Note: This method sets the 'otel.traces.exporter' as 'none', the 'otel.propagators' as
   * 'jaeger,tracecontext', and the 'otel.java.disabled.resource.providers' as
   * 'io.opentelemetry.instrumentation.resources.ProcessResourceProvider'.
   *
   * @return OpenTelemetrySdk instance with the provided configurations
   */
  public static OpenTelemetrySdk initializeOpenTelemetrySdk() {
    log.info("initializing otel");
    var autoConfiguredOpenTelemetrySdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier(
                () ->
                    Map.of(
                        "otel.traces.exporter",
                        "none",
                        "otel.propagators",
                        "jaeger,tracecontext",
                        "otel.java.disabled.resource.providers",
                        "io.opentelemetry.instrumentation.resources.ProcessResourceProvider"))
            .addPropertiesCustomizer(
                configProperties -> {
                  return Map.of(
                      "otel.metrics.exporter",
                      "none",
                      "otel.logs.exporter",
                      "none",
                      "otel.service.name",
                      "registry-local-its");
                })
            .build();
    var openTelemetrySdk = autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk();
    TracingExtension.setOpenTelemetry(openTelemetrySdk);
    log.debug("openTelemetrySdk initialized = " + openTelemetrySdk);
    return openTelemetrySdk;
  }

  public static void setOpenTelemetry(OpenTelemetry openTelemetry) {
    TracingExtension.openTelemetry = openTelemetry;
  }
}
