package ai.verta.modeldb.common.connections;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.ServiceUserConfig;
import ai.verta.modeldb.common.interceptors.MetadataForwarder;
import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class Connection {
  private final Optional<ClientInterceptor> tracingClientInterceptor;

  protected Connection(Config config) {
    tracingClientInterceptor = config.getTracingClientInterceptor();
  }

  // Place the interceptors in the reverse order of their stacking (the ones attached later will be
  // used first)
  protected <T extends AbstractStub<T>> T attachInterceptors(io.grpc.stub.AbstractStub<T> stub) {
    stub = stub.withInterceptors(MetadataForwarder.clientInterceptor());

    // add the tracing interceptor 2nd, so we preserve the OTel Context when making the client
    // calls.
    if (tracingClientInterceptor.isPresent()) {
      stub = stub.withInterceptors(tracingClientInterceptor.get());
    }

    return (T) stub;
  }

  public static <T> T withContextCredentials(ServiceUserConfig config, Supplier<T> supplier) {
    var previous = inplaceSetContextCredentials(config);

    try {
      return supplier.get();
    } finally {
      Context.current().detach(previous);
    }
  }

  public static void withContextCredentials(ServiceUserConfig config, Runnable run) {
    var previous = inplaceSetContextCredentials(config);

    try {
      run.run();
    } finally {
      Context.current().detach(previous);
    }
  }

  // DO NOT use outside of tests as this causes nasty grpc error messages
  public static Context inplaceSetContextCredentials(ServiceUserConfig config) {
    final var authHeaders = new Metadata();
    var emailKey = Metadata.Key.of("email", Metadata.ASCII_STRING_MARSHALLER);
    var devKey = Metadata.Key.of("developer_key", Metadata.ASCII_STRING_MARSHALLER);
    var devKeyHyphen = Metadata.Key.of("developer-key", Metadata.ASCII_STRING_MARSHALLER);
    var sourceKey = Metadata.Key.of("source", Metadata.ASCII_STRING_MARSHALLER);

    authHeaders.put(emailKey, config.getEmail());
    authHeaders.put(devKey, config.getDevKey());
    authHeaders.put(devKeyHyphen, config.getDevKey());
    authHeaders.put(sourceKey, "PythonClient");

    // Force using the ROOT context so that we must lose any context of the current execution
    return Context.ROOT.withValue(MetadataForwarder.METADATA_INFO, authHeaders).attach();
  }
}
