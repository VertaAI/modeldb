package ai.verta.modeldb.common.interceptors;

import io.grpc.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ActiveCountGrpcInterceptor implements ServerInterceptor {
  public static final AtomicInteger activeRequestCount = new AtomicInteger();

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    ServerCall.Listener<ReqT> delegate =
        Contexts.interceptCall(Context.current(), call, headers, next);
    activeRequestCount.incrementAndGet();
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
      @Override
      public void onCancel() {
        activeRequestCount.decrementAndGet();
        super.onCancel();
      }

      @Override
      public void onComplete() {
        activeRequestCount.decrementAndGet();
        super.onComplete();
      }
    };
  }
}
