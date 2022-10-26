package ai.verta.modeldb.common.interceptors;

import io.grpc.*;
import io.grpc.stub.MetadataUtils;

public class MetadataForwarder implements ServerInterceptor {
  public static final Context.Key<Metadata> METADATA_INFO = Context.key("metadata");

  @Override
  public <R, S> ServerCall.Listener<R> interceptCall(
      ServerCall<R, S> call, Metadata requestHeaders, ServerCallHandler<R, S> next) {
    var context = Context.current().withValue(METADATA_INFO, requestHeaders);
    ServerCall.Listener<R> delegate = Contexts.interceptCall(context, call, requestHeaders, next);
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<R>(delegate) {};
  }

  public static ClientInterceptor clientInterceptor() {
    return MetadataUtils.newAttachHeadersInterceptor(METADATA_INFO.get());
  }

  public static Metadata getMetadata() {
    return METADATA_INFO.get();
  }
}
