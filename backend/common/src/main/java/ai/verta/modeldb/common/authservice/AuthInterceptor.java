package ai.verta.modeldb.common.authservice;

import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.protobuf.StatusProto;

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

    // To protect empty headers from request
    Metadata.Key<String> email_key = Metadata.Key.of("email", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> dev_key =
        Metadata.Key.of("developer_key", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> source_key = Metadata.Key.of("source", Metadata.ASCII_STRING_MARSHALLER);

    if (requestHeaders == null
        || !requestHeaders.containsKey(email_key)
        || !requestHeaders.containsKey(dev_key)
        || !requestHeaders.containsKey(source_key)) {
      Status status = Status.newBuilder().setMessage("Required parameter is missing in metadata").build();
      throw StatusProto.toStatusRuntimeException(status);
    }

    ServerCall.Listener<R> delegate = Contexts.interceptCall(context, call, requestHeaders, next);
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<R>(delegate) {};
  }
}
