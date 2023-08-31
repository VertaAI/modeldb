package ai.verta.common.testing;

import static org.assertj.core.api.Assertions.assertThat;

import ai.verta.common.testing.utils.TracingExtension;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TracingExtensionTest {

  private static final InMemorySpanExporter inMemorySpanExporter;

  static {
    inMemorySpanExporter = InMemorySpanExporter.create();
    OpenTelemetry openTelemetry =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(inMemorySpanExporter))
                    .build())
            .build();
    TracingExtension.setOpenTelemetry(openTelemetry);
  }

  // register this _after_ the above static initializer, so that we have a clean global otel
  // instance.
  @RegisterExtension static TracingExtension e = new TracingExtension();

  @BeforeAll
  static void beforeAll() {
    // no-op to see if we have a span
  }

  @BeforeEach
  void setUp() {
    // no-op to see if we have a span
  }

  @AfterEach
  void tearDown() {
    // no-op to see if we have a span
  }

  @AfterAll
  static void afterAll() {
    // a little weird here, since we won't get a couple of the spans for the test class itself.
    // we should have: BeforeAll, BeforeEach, TestCase, test(), AfterEach
    // we're missing AfterAll and TestClass itself
    assertThat(inMemorySpanExporter.getFinishedSpanItems())
        .hasSize(5)
        .extracting(SpanData::getName)
        .containsExactly(
            "BeforeAll: TracingExtensionTest",
            "BeforeEach: test()",
            "test()",
            "AfterEach: test()",
            "TestCase: test()");
  }

  @Test
  void test() {
    // at this point we should have one span for beforeAll and one for beforeEach
    assertThat(inMemorySpanExporter.getFinishedSpanItems()).hasSize(2);
  }
}
