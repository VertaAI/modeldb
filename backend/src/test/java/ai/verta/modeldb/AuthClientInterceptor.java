package ai.verta.modeldb;

import ai.verta.modeldb.common.config.ServiceUserConfig;
import ai.verta.modeldb.config.TestConfig;
import io.grpc.*;
import java.util.Map;

@SuppressWarnings("unchecked")
public class AuthClientInterceptor {

  private String client1Email;
  private String client2Email;
  private final ClientAuthInterceptor serviceAccountClientAuthInterceptor;
  private final Client1AuthInterceptor client1AuthInterceptor;
  private final Client2AuthInterceptor client2AuthInterceptor;

  public AuthClientInterceptor(TestConfig testConfig) {
     serviceAccountClientAuthInterceptor =
      new ClientAuthInterceptor(testConfig.getService_user());
    client1AuthInterceptor =
        new Client1AuthInterceptor(testConfig.testUsers.getOrDefault("primaryUser", null));
    client2AuthInterceptor =
        new Client2AuthInterceptor(testConfig.testUsers.getOrDefault("secondaryUser", null));
  }

  public ClientAuthInterceptor getServiceAccountClientAuthInterceptor() {
    return serviceAccountClientAuthInterceptor;
  }

  public Client1AuthInterceptor getClient1AuthInterceptor() {
    return client1AuthInterceptor;
  }

  public Client2AuthInterceptor getClient2AuthInterceptor() {
    return client2AuthInterceptor;
  }

  public String getClient1Email() {
    return client1Email;
  }

  public String getClient2Email() {
    return client2Email;
  }

  private class Client1AuthInterceptor extends ClientAuthInterceptor {

    Client1AuthInterceptor(ServiceUserConfig user) {
      super(user);
      client1Email = user.getEmail();
    }

    public String getClient1Email() {
      return client1Email;
    }
  }

  private class Client2AuthInterceptor extends ClientAuthInterceptor {

    Client2AuthInterceptor(ServiceUserConfig user) {
      super(user);
      client2Email = user.getEmail();
    }

    public String getClient2Email() {
      return client2Email;
    }
  }

  private class ClientAuthInterceptor implements ClientInterceptor {

    private final String clientEmail;
    private final String clientDevKey;

    ClientAuthInterceptor(ServiceUserConfig user) {
      clientEmail = user.getEmail();
      clientDevKey = user.getDevKey();
    }

    public String getClient2Email() {
      return client2Email;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
      return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
          channel.newCall(methodDescriptor, callOptions)) {
        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
          // TODO: Here set request metadata
          Metadata.Key<String> email_key =
              Metadata.Key.of("email", Metadata.ASCII_STRING_MARSHALLER);
          Metadata.Key<String> dev_key =
              Metadata.Key.of("developer_key", Metadata.ASCII_STRING_MARSHALLER);
          Metadata.Key<String> source_key =
              Metadata.Key.of("source", Metadata.ASCII_STRING_MARSHALLER);

          headers.put(email_key, clientEmail);
          headers.put(dev_key, clientDevKey);
          headers.put(source_key, "PythonClient");
          super.start(responseListener, headers);
        }
      };
    }
  }
}
