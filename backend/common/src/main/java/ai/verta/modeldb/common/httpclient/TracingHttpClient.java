package ai.verta.modeldb.common.httpclient;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class TracingHttpClient {
  private final HttpClient httpClient;
  private final Tracer tracer;

  public TracingHttpClient(HttpClient httpClient, OpenTelemetry openTelemetry) {
    this.httpClient = httpClient;
    this.tracer = openTelemetry.getTracer("ai.verta.modeldb.common.httpclient");
  }

  public <T> HttpResponse<T> send(
      HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
      throws IOException, InterruptedException {
    Span span = createRequestSpan(request);
    try (Scope ignored = span.makeCurrent()) {
      HttpResponse<T> httpResponse = httpClient.send(request, responseBodyHandler);
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

  public <T> CompletableFuture<HttpResponse<T>> sendAsync(
      HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
    Span span = createRequestSpan(request);
    CompletableFuture<HttpResponse<T>> response =
        httpClient.sendAsync(request, responseBodyHandler);
    handleResponseCompletion(span, response);
    return response;
  }

  public <T> CompletableFuture<HttpResponse<T>> sendAsync(
      HttpRequest request,
      HttpResponse.BodyHandler<T> responseBodyHandler,
      HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
    Span span = createRequestSpan(request);
    CompletableFuture<HttpResponse<T>> response =
        httpClient.sendAsync(request, responseBodyHandler, pushPromiseHandler);
    handleResponseCompletion(span, response);
    return response;
  }

  private <T> void handleResponseCompletion(
      Span span, CompletableFuture<HttpResponse<T>> response) {
    response.whenComplete(
        (httpResponse, throwable) -> {
          if (throwable != null) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(throwable);
          } else {
            span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, httpResponse.statusCode());
          }
          span.end();
        });
  }

  private Span createRequestSpan(HttpRequest request) {
    URI requestUri = request.uri();
    return tracer
        .spanBuilder("HTTP " + request.method())
        .setAttribute(SemanticAttributes.HTTP_METHOD, request.method())
        .setSpanKind(SpanKind.CLIENT)
        .setAttribute(SemanticAttributes.NET_PEER_NAME, requestUri.getHost())
        //                .setAttribute(SemanticAttributes.HTTP_URL, requestUri.toString()) TODO:
        // figure out how to sanitize this
        .startSpan();
  }
}
