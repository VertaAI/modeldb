package ai.verta.modeldb.common.monitoring;

import io.grpc.*;
import io.grpc.ServerCall.Listener;
import io.prometheus.client.Counter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public class AuditLogInterceptor implements ServerInterceptor {

  private static final Logger LOGGER = LogManager.getLogger(AuditLogInterceptor.class);
  public static final Context.Key<AtomicInteger> CALL_COUNT = Context.key("audit_count");
  public static final String AUDIT_WAS_NOT_CALLED = "Audit was not called for method %s";
  public static final String AUDIT_WAS_CALLED_WRONG = "Audit was not called %d times for method %s";

  private static final Counter failed_audit_logging =
      Counter.build()
          .labelNames("grpc_method")
          .name("verta_backend_failed_audit_logging_total")
          .help("Total failed requests to logging audit on the UAC server.")
          .register();
  private final boolean shouldQuitOnAuditMissing;

  public AuditLogInterceptor(boolean shouldQuitOnAuditMissing) {
    this.shouldQuitOnAuditMissing = shouldQuitOnAuditMissing;
  }

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

    Context context = Context.current().withValue(CALL_COUNT, new AtomicInteger(0));
    ServerCall<R, S> wrappedCall =
        new ForwardingServerCall.SimpleForwardingServerCall<R, S>(call) {
          @Override
          public void close(Status status, Metadata trailers) {
            if (status.getCode() == Status.Code.OK) {
              AtomicInteger getCallCount = CALL_COUNT.get(context);
              int callCount = getCallCount.get();
              if (callCount != 1) {
                final String message;
                if (callCount == 0) {
                  message = String.format(AUDIT_WAS_NOT_CALLED, methodName);
                } else {
                  message = String.format(AUDIT_WAS_CALLED_WRONG, callCount, methodName);
                }
                failed_audit_logging.labels(methodName).inc();
                if (shouldQuitOnAuditMissing) {
                  status = Status.INTERNAL.withDescription(message);
                }
              }
            }
            LOGGER.trace("close");
            super.close(status, trailers);
          }
        };
    Listener<R> delegate = Contexts.interceptCall(context, wrappedCall, requestHeaders, next);
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<R>(delegate) {};
  }

  public void ignoreAuditLogCheck() {
    increaseAuditCountStatic();
  }

  public void increaseAuditCount() {
    increaseAuditCountStatic();
  }

  public static void increaseAuditCountStatic() {
    AtomicInteger atomicInteger = CALL_COUNT.get();
    if (atomicInteger == null) {
      LOGGER.warn("Non grpc call to increaseAuditCount");
    } else {
      atomicInteger.getAndIncrement();
    }
  }
}
