package ai.verta.modeldb.common.authservice;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthInterceptor implements ServerInterceptor {
  private static final Logger LOGGER = LogManager.getLogger(AuthInterceptor.class);
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
    String methodName = call.getMethodDescriptor().getFullMethodName();

    if (!(methodName.equals("ai.verta.modeldb.ProjectService/verifyConnection")
            || methodName.equals("grpc.health.v1.Health/Check")
            || methodName.equals("grpc.health.v1.Health/Watch"))){
      // validate empty headers from user request
      Metadata.Key<String> email_key = Metadata.Key.of("email", Metadata.ASCII_STRING_MARSHALLER);
      Metadata.Key<String> dev_key_underscore =
              Metadata.Key.of("developer_key", Metadata.ASCII_STRING_MARSHALLER);
      Metadata.Key<String> dev_key =
              Metadata.Key.of("developer-key", Metadata.ASCII_STRING_MARSHALLER);
      Metadata.Key<String> source_key = Metadata.Key.of("source", Metadata.ASCII_STRING_MARSHALLER);

      if (requestHeaders == null
              || !requestHeaders.containsKey(email_key)
              || !(requestHeaders.containsKey(dev_key_underscore) || requestHeaders.containsKey(dev_key))
              || !requestHeaders.containsKey(source_key)) {
        var message = "Required parameter is missing in metadata in request: " + methodName;
        call.close(Status.PERMISSION_DENIED
                .withDescription(message), requestHeaders);
        LOGGER.debug(message);
        return new ServerCall.Listener<>(){};
      }
    }

    ServerCall.Listener<R> delegate = Contexts.interceptCall(context, call, requestHeaders, next);
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<R>(delegate) {};
  }
}
