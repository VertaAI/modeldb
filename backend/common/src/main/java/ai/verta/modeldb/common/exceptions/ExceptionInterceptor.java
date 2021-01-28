package ai.verta.modeldb.common.exceptions;

import ai.verta.modeldb.utils.ModelDBUtils;
import io.grpc.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// From https://sultanov.dev/blog/exception-handling-in-grpc-java-server/
public class ExceptionInterceptor implements ServerInterceptor {
  private static final Logger LOGGER = LogManager.getLogger(ExceptionInterceptor.class);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> serverCall,
      Metadata metadata,
      ServerCallHandler<ReqT, RespT> serverCallHandler) {
    ServerCall.Listener<ReqT> listener = serverCallHandler.startCall(serverCall, metadata);
    return new ExceptionHandlingServerCallListener<>(listener, serverCall, metadata);
  }

  private class ExceptionHandlingServerCallListener<ReqT, RespT>
      extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
    private ServerCall<ReqT, RespT> serverCall;
    private Metadata metadata;

    ExceptionHandlingServerCallListener(
        ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
      super(listener);
      this.serverCall = serverCall;
      this.metadata = metadata;
    }

    @Override
    public void onHalfClose() {
      try {
        super.onHalfClose();
      } catch (RuntimeException ex) {
        handleException(ex, serverCall, metadata);
        throw ex;
      }
    }

    @Override
    public void onReady() {
      try {
        super.onReady();
      } catch (RuntimeException ex) {
        handleException(ex, serverCall, metadata);
        throw ex;
      }
    }

    private void handleException(
        RuntimeException exception, ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
      StatusRuntimeException status = ModelDBUtils.logError(exception);
      serverCall.close(status.getStatus(), metadata);
    }
  }
}
