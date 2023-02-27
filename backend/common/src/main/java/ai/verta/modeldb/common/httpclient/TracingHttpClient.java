package ai.verta.modeldb.common.httpclient;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class TracingHttpClient extends HttpClient {
  private static final AttributeKey<String> REQUEST_PATH_KEY =
      AttributeKey.stringKey("http.request.path");
  private final HttpClient delegate;
  private final Tracer tracer;
  private final ContextPropagators contextPropagators;

  public TracingHttpClient(HttpClient httpClient, OpenTelemetry openTelemetry) {
    this.delegate = httpClient;
    this.tracer = openTelemetry.getTracer("ai.verta.modeldb.common.httpclient");
    this.contextPropagators = openTelemetry.getPropagators();
  }

  public <T> HttpResponse<T> send(
      HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
      throws IOException, InterruptedException {
    Span span = createRequestSpan(request);
    try (Scope ignored = span.makeCurrent()) {
      request = addPropagationHeaders(request, Context.current());
      HttpResponse<T> httpResponse = delegate.send(request, responseBodyHandler);
      span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, httpResponse.statusCode());
      return httpResponse;
    } catch (Exception e) {
      span.setStatus(StatusCode.ERROR);
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  private HttpRequest addPropagationHeaders(HttpRequest request, Context context) {
    HttpRequest.Builder builder = newBuilder(request);
    contextPropagators
        .getTextMapPropagator()
        .inject(context, builder, (carrier, key, value) -> carrier.header(key, value));
    return builder.build();
  }

  public <T> CompletableFuture<HttpResponse<T>> sendAsync(
      HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
    Span span = createRequestSpan(request);
    try (Scope ignored = span.makeCurrent()) {
      request = addPropagationHeaders(request, Context.current());
      return delegate.sendAsync(request, responseBodyHandler).whenComplete(responseCompleter(span));
    }
  }

  public <T> CompletableFuture<HttpResponse<T>> sendAsync(
      HttpRequest request,
      HttpResponse.BodyHandler<T> responseBodyHandler,
      HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
    Span span = createRequestSpan(request);
    try (Scope ignored = span.makeCurrent()) {
      request = addPropagationHeaders(request, Context.current());
      return delegate
          .sendAsync(request, responseBodyHandler, pushPromiseHandler)
          .whenComplete(responseCompleter(span));
    }
  }

  private <T> BiConsumer<HttpResponse<T>, Throwable> responseCompleter(Span span) {
    return (httpResponse, throwable) -> {
      if (throwable != null) {
        span.setStatus(StatusCode.ERROR);
        span.recordException(throwable);
      } else {
        span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, httpResponse.statusCode());
      }
      span.end();
    };
  }

  private Span createRequestSpan(HttpRequest request) {
    URI requestUri = request.uri();
    return tracer
        .spanBuilder("HTTP " + request.method())
        .setAttribute(SemanticAttributes.HTTP_METHOD, request.method())
        .setSpanKind(SpanKind.CLIENT)
        .setAttribute(SemanticAttributes.NET_PEER_NAME, requestUri.getHost())
        .setAttribute(SemanticAttributes.PEER_SERVICE, requestUri.getHost())
        .setAttribute(REQUEST_PATH_KEY, requestUri.getPath())
        // .setAttribute(SemanticAttributes.HTTP_URL, requestUri.toString()) TODO: figure out how to
        // sanitize this
        .startSpan();
  }

  @Override
  public Optional<CookieHandler> cookieHandler() {
    return delegate.cookieHandler();
  }

  @Override
  public Optional<Duration> connectTimeout() {
    return delegate.connectTimeout();
  }

  @Override
  public Redirect followRedirects() {
    return delegate.followRedirects();
  }

  @Override
  public Optional<ProxySelector> proxy() {
    return delegate.proxy();
  }

  @Override
  public SSLContext sslContext() {
    return delegate.sslContext();
  }

  @Override
  public SSLParameters sslParameters() {
    return delegate.sslParameters();
  }

  @Override
  public Optional<Authenticator> authenticator() {
    return delegate.authenticator();
  }

  @Override
  public Version version() {
    return delegate.version();
  }

  @Override
  public Optional<Executor> executor() {
    return delegate.executor();
  }

  @Override
  public WebSocket.Builder newWebSocketBuilder() {
    return delegate.newWebSocketBuilder();
  }

  // cribbed from java 17, cleaned up for java 11.
  private static HttpRequest.Builder newBuilder(HttpRequest request) {
    HttpRequest.Builder builder = HttpRequest.newBuilder();
    builder.uri(request.uri());
    builder.expectContinue(request.expectContinue());

    // Filter unwanted headers
    request
        .headers()
        .map()
        .forEach((name, values) -> values.forEach(value -> builder.header(name, value)));

    request.version().ifPresent(builder::version);
    request.timeout().ifPresent(builder::timeout);
    var method = request.method();
    request
        .bodyPublisher()
        .ifPresentOrElse(
            // if body is present, set it
            bodyPublisher -> builder.method(method, bodyPublisher),
            // otherwise, the body is absent, special case for GET/DELETE,
            // or else use empty body
            () -> {
              switch (method) {
                case "GET":
                  builder.GET();
                  break;
                case "DELETE":
                  builder.DELETE();
                  break;
                default:
                  builder.method(method, HttpRequest.BodyPublishers.noBody());
              }
            });
    return builder;
  }
}
