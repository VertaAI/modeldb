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
    var context = Context.current().withValue(METADATA_INFO, requestHeaders);
    String methodName = call.getMethodDescriptor().getFullMethodName();

    if (!(methodName.equals("ai.verta.modeldb.ProjectService/verifyConnection")
        || methodName.equals("grpc.health.v1.Health/Check")
        || methodName.equals("grpc.health.v1.Health/Watch"))) {
      // validate empty headers from user request
      var emailKey = Metadata.Key.of("email", Metadata.ASCII_STRING_MARSHALLER);
      var devKeyUnderscore = Metadata.Key.of("developer_key", Metadata.ASCII_STRING_MARSHALLER);
      var devKey = Metadata.Key.of("developer-key", Metadata.ASCII_STRING_MARSHALLER);
      var bearerAccessToken =
          Metadata.Key.of("bearer_access_token", Metadata.ASCII_STRING_MARSHALLER);
      var sessionId = Metadata.Key.of("sessionId", Metadata.ASCII_STRING_MARSHALLER);
      var sessionIdSig = Metadata.Key.of("sessionIdSig", Metadata.ASCII_STRING_MARSHALLER);
      var sourceKey = Metadata.Key.of("source", Metadata.ASCII_STRING_MARSHALLER);

      var parameterMissing = false;
      if (!requestHeaders.containsKey(sourceKey)) {
        parameterMissing = true;
      } else {
        var sourceValue = requestHeaders.get(sourceKey);
        assert sourceValue != null;
        var isDevKeyUsed = sourceValue.equals("PythonClient");
        if (isDevKeyUsed) {
          if (!requestHeaders.containsKey(emailKey)
              || !(requestHeaders.containsKey(devKeyUnderscore)
                  || requestHeaders.containsKey(devKey))) {
            parameterMissing = true;
          }
        } else if (sourceValue.equals("SessionId")) {
          if (!requestHeaders.containsKey(sessionId) || !requestHeaders.containsKey(sessionIdSig)) {
            parameterMissing = true;
          }
        } else if (!requestHeaders.containsKey(bearerAccessToken)) {
          parameterMissing = true;
        }
      }
      if (parameterMissing) {
        var message = "Required parameter is missing in metadata in request: " + methodName;
        call.close(Status.PERMISSION_DENIED.withDescription(message), requestHeaders);
        LOGGER.debug(message);
        return new ServerCall.Listener<>() {};
      }
    }

    ServerCall.Listener<R> delegate = Contexts.interceptCall(context, call, requestHeaders, next);
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {};
  }
}
