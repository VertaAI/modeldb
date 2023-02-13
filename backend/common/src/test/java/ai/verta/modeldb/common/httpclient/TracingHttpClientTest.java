package ai.verta.modeldb.common.httpclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.model.HttpRequest.request;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpError;

class TracingHttpClientTest {
  @RegisterExtension
  static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

  private static ClientAndServer mockServer;
  private static final int port = new Random().nextInt(10000) + 1024;

  @BeforeAll
  static void setUp() {
    mockServer = ClientAndServer.startClientAndServer(port);
    mockServer.when(request().withPath("/error")).error(new HttpError().withDropConnection(true));
    mockServer
        .when(request().withPath("/requestPath").withHeader("traceparent", ".*"))
        .respond(new org.mockserver.model.HttpResponse().withStatusCode(420).withBody("foo"));
  }

  @AfterAll
  static void afterAll() {
    mockServer.stop();
  }

  @AfterEach
  void tearDown() {
    otelTesting.clearSpans();
  }

  @Test
  void sync() throws Exception {
    OpenTelemetry openTelemetry = otelTesting.getOpenTelemetry();

    TracingHttpClient httpClient = new TracingHttpClient(HttpClient.newHttpClient(), openTelemetry);

    HttpRequest request =
        HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("http://localhost:" + port + "/requestPath"))
            .build();
    HttpResponse<String> result = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(result.body()).isEqualTo("foo");
    otelTesting
        .assertTraces()
        .hasTracesSatisfyingExactly(
            traceAssert ->
                traceAssert
                    .hasSize(1)
                    .hasSpansSatisfyingExactlyInAnyOrder(
                        spanDataAssert ->
                            spanDataAssert
                                .hasAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                                .hasAttribute(SemanticAttributes.NET_PEER_NAME, "localhost")
                                .hasAttribute(SemanticAttributes.HTTP_STATUS_CODE, 420L)
                                .hasAttribute(SemanticAttributes.PEER_SERVICE, "localhost")
                                .hasAttribute(
                                    AttributeKey.stringKey("http.request.path"), "/requestPath")
                                .hasStatus(StatusData.unset())
                                .hasKind(SpanKind.CLIENT)));
  }

  @Test
  void async() throws Exception {
    mockServer
        .when(request())
        .respond(new org.mockserver.model.HttpResponse().withStatusCode(420).withBody("foo"));
    OpenTelemetry openTelemetry = otelTesting.getOpenTelemetry();

    TracingHttpClient httpClient = new TracingHttpClient(HttpClient.newHttpClient(), openTelemetry);

    HttpRequest request =
        HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("http://localhost:" + port + "/requestPath"))
            .build();
    CompletableFuture<HttpResponse<String>> responseFuture =
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    HttpResponse<String> result = responseFuture.get();

    assertThat(result.body()).isEqualTo("foo");

    otelTesting
        .assertTraces()
        .hasTracesSatisfyingExactly(
            traceAssert ->
                traceAssert
                    .hasSize(1)
                    .hasSpansSatisfyingExactlyInAnyOrder(
                        spanDataAssert ->
                            spanDataAssert
                                .hasAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                                .hasAttribute(SemanticAttributes.NET_PEER_NAME, "localhost")
                                .hasAttribute(SemanticAttributes.HTTP_STATUS_CODE, 420L)
                                .hasAttribute(SemanticAttributes.PEER_SERVICE, "localhost")
                                .hasAttribute(
                                    AttributeKey.stringKey("http.request.path"), "/requestPath")
                                .hasStatus(StatusData.unset())
                                .hasKind(SpanKind.CLIENT)));
  }

  @Test
  void async_error() {
    OpenTelemetry openTelemetry = otelTesting.getOpenTelemetry();

    TracingHttpClient httpClient = new TracingHttpClient(HttpClient.newHttpClient(), openTelemetry);

    HttpRequest request =
        HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("http://localhost:" + port + "/error"))
            .build();
    CompletableFuture<HttpResponse<String>> responseFuture =
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

    assertThatThrownBy(responseFuture::get);
    otelTesting
        .assertTraces()
        .hasTracesSatisfyingExactly(
            traceAssert ->
                traceAssert
                    .hasSize(1)
                    .hasSpansSatisfyingExactlyInAnyOrder(
                        spanDataAssert ->
                            spanDataAssert
                                .hasAttribute(SemanticAttributes.HTTP_METHOD, "GET")
                                .hasAttribute(SemanticAttributes.NET_PEER_NAME, "localhost")
                                .hasAttribute(SemanticAttributes.PEER_SERVICE, "localhost")
                                .hasAttribute(AttributeKey.stringKey("http.request.path"), "/error")
                                .hasStatus(StatusData.error())
                                .hasKind(SpanKind.CLIENT)));
  }
}
