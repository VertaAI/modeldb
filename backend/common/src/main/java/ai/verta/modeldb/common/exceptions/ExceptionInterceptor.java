package ai.verta.modeldb.common.exceptions;

import ai.verta.modeldb.common.CommonUtils;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.StatusRuntimeException;

@SuppressWarnings({"squid:S119"})
// From https://sultanov.dev/blog/exception-handling-in-grpc-java-server/
public class ExceptionInterceptor implements ServerInterceptor {

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> serverCall,
      Metadata metadata,
      ServerCallHandler<ReqT, RespT> serverCallHandler) {
    ServerCall.Listener<ReqT> listener = serverCallHandler.startCall(serverCall, metadata);
    return new ExceptionHandlingServerCallListener<>(listener, serverCall, metadata);
  }

  private static class ExceptionHandlingServerCallListener<ReqT, RespT>
      extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
    private final ServerCall<ReqT, RespT> serverCall;
    private final Metadata metadata;

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
      StatusRuntimeException status = CommonUtils.logError(exception);
      serverCall.close(status.getStatus(), metadata);
    }
  }
}
