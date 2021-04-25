package ai.verta.modeldb.common.connections;

import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.ServiceUserConfig;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.interceptors.MetadataForwarder;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.opentracing.contrib.grpc.TracingClientInterceptor;

import java.util.Optional;
import java.util.concurrent.Executor;

public abstract class Connection {
  private final Optional<TracingClientInterceptor> tracingClientInterceptor;

  Connection(Config config) {
    tracingClientInterceptor = config.getTracingClientInterceptor();
  }

  // Place the interceptors in the reverse order of their stacking (the ones attached later will be
  // used first)
  protected <T extends AbstractStub<T>> T attachInterceptors(io.grpc.stub.AbstractStub<T> stub) {
    if (tracingClientInterceptor.isPresent()) {
      stub = stub.withInterceptors(tracingClientInterceptor.get());
    }

    stub = stub.withInterceptors(MetadataForwarder.clientInterceptor());

    return (T) stub;
  }

  public static InternalFuture<Void> setContextCredentials(
      ServiceUserConfig config, Executor executor) {
    return InternalFuture.runAsync(
        () -> {
          inplaceSetContextCredentials(config);
        },
        executor);
  }

  public static void inplaceSetContextCredentials(ServiceUserConfig config) {
    final Metadata authHeaders = new Metadata();
    Metadata.Key<String> email_key = Metadata.Key.of("email", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> dev_key =
        Metadata.Key.of("developer_key", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> dev_key_hyphen =
        Metadata.Key.of("developer-key", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> source_key = Metadata.Key.of("source", Metadata.ASCII_STRING_MARSHALLER);

    authHeaders.put(email_key, config.email);
    authHeaders.put(dev_key, config.devKey);
    authHeaders.put(dev_key_hyphen, config.devKey);
    authHeaders.put(source_key, "PythonClient");

    // Force using the ROOT context so that we must lose any context of the current execution
    Context.ROOT.withValue(MetadataForwarder.METADATA_INFO, authHeaders).attach();
  }
}
