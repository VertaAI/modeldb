package ai.verta.modeldb;

import ai.verta.modeldb.common.config.ServiceUserConfig;
import io.grpc.*;
import java.util.Map;

@SuppressWarnings("unchecked")
public class AuthClientInterceptor {

  private String client1Email;
  private String client1DevKey;
  private String client2Email;
  private String client2DevKey;
  private Client1AuthInterceptor client1AuthInterceptor;
  private Client2AuthInterceptor client2AuthInterceptor;

  public AuthClientInterceptor(Map<String, ServiceUserConfig> testUsers) {
    client1AuthInterceptor =
        new Client1AuthInterceptor(testUsers.getOrDefault("primaryUser", null));
    client2AuthInterceptor =
        new Client2AuthInterceptor(testUsers.getOrDefault("secondaryUser", null));
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

  private class Client1AuthInterceptor implements ClientInterceptor {

    public Client1AuthInterceptor(ServiceUserConfig user) {
      client1Email = user.email;
      client1DevKey = user.devKey;
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

          headers.put(email_key, client1Email);
          headers.put(dev_key, client1DevKey);
          headers.put(source_key, "PythonClient");
          super.start(responseListener, headers);
        }
      };
    }
  }

  private class Client2AuthInterceptor implements ClientInterceptor {

    Client2AuthInterceptor(ServiceUserConfig user) {
      client2Email = user.email;
      client2DevKey = user.devKey;
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

          headers.put(email_key, client2Email);
          headers.put(dev_key, client2DevKey);
          headers.put(source_key, "PythonClient");
          super.start(responseListener, headers);
        }
      };
    }
  }
}
