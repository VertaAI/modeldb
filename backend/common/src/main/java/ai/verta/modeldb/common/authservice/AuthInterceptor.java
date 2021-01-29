package ai.verta.modeldb.common.authservice;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class AuthInterceptor implements ServerInterceptor {
  public static final Context.Key<Metadata> METADATA_INFO = Context.key("metadata");

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
    Context context = Context.current().withValue(METADATA_INFO, requestHeaders);
    ServerCall.Listener<R> delegate = Contexts.interceptCall(context, call, requestHeaders, next);
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<R>(delegate) {};
  }
}
