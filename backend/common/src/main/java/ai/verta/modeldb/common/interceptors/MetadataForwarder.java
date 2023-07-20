package ai.verta.modeldb.common.interceptors;

import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import java.util.Optional;

public class MetadataForwarder implements ServerInterceptor {
  public static final Context.Key<Metadata> METADATA_INFO = Context.key("metadata");
  public static final String ORGANIZATION_ID = "organization-id";

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

  public static Optional<String> getMetadataKey(String keyName) {
    if (getMetadata() == null) {
      return Optional.empty();
    }
    var value = getMetadata().get(Metadata.Key.of(keyName, Metadata.ASCII_STRING_MARSHALLER));
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(value);
  }

  public static Optional<String> getOrganizationId() {
    return getMetadataKey(ORGANIZATION_ID);
  }
}
