package ai.verta.modeldb;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModelDBAuthInterceptor implements ServerInterceptor {

  private static final Logger LOGGER = LogManager.getLogger(ModelDBAuthInterceptor.class);
  public static final Context.Key<Metadata> METADATA_INFO = Context.key("metadata");
  public static final Context.Key<String> METHOD_NAME = Context.key("method_name");
  public static final AtomicInteger ACTIVE_REQUEST_COUNT = new AtomicInteger();

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
    LOGGER.trace("Headers : {}", requestHeaders);
    String methodName = call.getMethodDescriptor().getFullMethodName();
    LOGGER.info("methodName: {}", methodName);

    Context context =
        Context.current()
            .withValue(METADATA_INFO, requestHeaders)
            .withValue(METHOD_NAME, methodName);
    ServerCall.Listener<R> delegate = Contexts.interceptCall(context, call, requestHeaders, next);
    ACTIVE_REQUEST_COUNT.incrementAndGet();
    LOGGER.trace("Active Request count {}", ACTIVE_REQUEST_COUNT.get());
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<R>(delegate) {

      @Override
      public void onCancel() {
        ACTIVE_REQUEST_COUNT.decrementAndGet();
        LOGGER.trace("Decrease Request count oon onCancel()");
        LOGGER.trace("Active Request count {}", ACTIVE_REQUEST_COUNT.get());
        super.onCancel();
      }

      @Override
      public void onComplete() {
        ACTIVE_REQUEST_COUNT.decrementAndGet();
        LOGGER.trace("Decrease Request count on onComplete()");
        LOGGER.trace("Active Request count {}", ACTIVE_REQUEST_COUNT.get());
        super.onComplete();
      }
    };
  }
}
