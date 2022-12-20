package ai.verta.modeldb.monitoring;

import ai.verta.modeldb.ModelDBMessages;
import io.grpc.*;
import io.grpc.ServerCall.Listener;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonitoringInterceptor implements ServerInterceptor {

  private static final Logger LOGGER = LogManager.getLogger(MonitoringInterceptor.class);
  public static final AtomicInteger ACTIVE_REQUEST_COUNT = new AtomicInteger();
  public static final Context.Key<String> METHOD_NAME = Context.key("method_name");
  private static final String GRPC_METHOD_LABEL = "grpc_method";
  private static final Counter qpsCountRequests =
      Counter.build()
          .labelNames(GRPC_METHOD_LABEL)
          .name("verta_backend_query_per_second_total")
          .help("Total QPS requests started on the server.")
          .register();
  private static final Histogram requestLatency =
      Histogram.build()
          .labelNames(GRPC_METHOD_LABEL)
          .name("verta_backend_requests_latency_seconds")
          .help("Request latency in seconds.")
          .register();

  private static final Counter failed_4XX_Requests =
      Counter.build()
          .labelNames(GRPC_METHOD_LABEL)
          .name("verta_backend_4XX_failed_requests_total")
          .help("Total 4XX failed requests on the server.")
          .register();

  private static final Counter failed_5XX_Requests =
      Counter.build()
          .labelNames(GRPC_METHOD_LABEL)
          .name("verta_backend_5XX_failed_requests_total")
          .help("Total 5XX failed requests on the server.")
          .register();

  /**
   * @param call: ServerCall
   * @param requestHeaders : Metadata request headers
   * @param next: ServerCallHandler
   * @param <R>: Request
   * @param <S>: Response
   * @return {@link Contexts}
   */
  @Override
  public <R, S> Listener<R> interceptCall(
      ServerCall<R, S> call, Metadata requestHeaders, ServerCallHandler<R, S> next) {
    String methodName = call.getMethodDescriptor().getFullMethodName();

    var context = Context.current().withValue(METHOD_NAME, methodName);
    ServerCall.Listener<R> delegate = Contexts.interceptCall(context, call, requestHeaders, next);
    ACTIVE_REQUEST_COUNT.incrementAndGet();
    LOGGER.trace(ModelDBMessages.ACTIVE_REQUEST_COUNT_TRACE, ACTIVE_REQUEST_COUNT.get());

    qpsCountRequests.labels(methodName).inc();
    final var timer = requestLatency.labels(methodName).startTimer();
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<R>(delegate) {

      @Override
      public void onCancel() {
        ACTIVE_REQUEST_COUNT.decrementAndGet();
        LOGGER.trace("Decrease Request count on onCancel()");
        LOGGER.trace(ModelDBMessages.ACTIVE_REQUEST_COUNT_TRACE, ACTIVE_REQUEST_COUNT.get());
        try {
          super.onCancel();
        } finally {
          timer.observeDuration();
        }
      }

      @Override
      public void onComplete() {
        ACTIVE_REQUEST_COUNT.decrementAndGet();
        LOGGER.trace("Decrease Request count on onComplete()");
        LOGGER.trace(ModelDBMessages.ACTIVE_REQUEST_COUNT_TRACE, ACTIVE_REQUEST_COUNT.get());
        try {
          super.onComplete();
        } finally {
          timer.observeDuration();
        }
      }

      @Override
      public void onHalfClose() {
        try {
          super.onHalfClose();
        } catch (RuntimeException ex) {
          checkForErrors(ex, methodName);
          throw ex;
        }
      }

      @Override
      public void onReady() {
        try {
          super.onReady();
        } catch (RuntimeException ex) {
          checkForErrors(ex, methodName);
          throw ex;
        }
      }
    };
  }

  private static void checkForErrors(Throwable ex, String methodName) {
    if (ex instanceof StatusRuntimeException) {
      var status = Status.fromThrowable(ex);
      registerFailedRequestCount(status, methodName);
    } else {
      failed_5XX_Requests.labels(methodName).inc();
    }
  }

  private static void registerFailedRequestCount(Status status, String methodName) {
    switch (status.getCode().value()) {
      case 0: // OK : 200 OK
        break;
      case 1: // CANCELLED : 499 Client Closed Request
      case 3: // INVALID_ARGUMENT: 400 Bad Request
      case 5: // NOT_FOUND: 404 Not Found
      case 7: // PERMISSION_DENIED: 403 Forbidden
      case 6: // ALREADY_EXISTS: 409 Conflict
      case 8: // RESOURCE_EXHAUSTED: 429 Too Many Requests
      case 9: // FAILED_PRECONDITION: 400 Bad Request
      case 10: // ABORTED: 409 Conflict
      case 11: // OUT_OF_RANGE: 400 Bad Request
      case 16: // UNAUTHENTICATED: 401 Unauthorized
        failed_4XX_Requests.labels(methodName).inc();
        break;
      case 2: // UNKNOWN: 500 Internal Server Error
      case 4: // DEADLINE_EXCEEDED: 504 Gateway Timeout
      case 12: // UNIMPLEMENTED: 501 Not Implemented
      case 13: // INTERNAL: 500 Internal Server Error
      case 14: // UNAVAILABLE: 503 Service Unavailable
      case 15: // DATA_LOSS: 500 Internal Server Error
      default:
        failed_5XX_Requests.inc();
    }
  }
}
