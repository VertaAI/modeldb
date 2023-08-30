package ai.verta.common.testing.utils;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.lang.reflect.Method;
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
  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create("tracing");

  private Tracer getTracer() {
    return GlobalOpenTelemetry.getTracer("tracingExtension");
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
      log.info("Starting root span with name: " + spanName);
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
}
