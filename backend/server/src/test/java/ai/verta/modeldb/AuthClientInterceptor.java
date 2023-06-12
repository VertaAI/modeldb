package ai.verta.modeldb;

import ai.verta.modeldb.common.config.ServiceUserConfig;
import ai.verta.modeldb.common.connections.Connection;
import ai.verta.modeldb.config.TestConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.grpc.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.NONE)
@SuppressWarnings("unchecked")
public class AuthClientInterceptor {

  @JsonProperty private String client1Email;
  @JsonProperty private String client1UserName;
  @JsonProperty private long client1WorkspaceId;
  @JsonProperty private String client1DevKey;
  @JsonProperty private String client2Email;
  @JsonProperty private final ClientAuthInterceptor serviceAccountClientAuthInterceptor;
  @JsonProperty private String client2UserName;
  @JsonProperty private long client2WorkspaceId;
  @JsonProperty private String client2DevKey;
  @JsonProperty private final Client1AuthInterceptor client1AuthInterceptor;
  @JsonProperty private final Client2AuthInterceptor client2AuthInterceptor;

  public AuthClientInterceptor(TestConfig testConfig) {
    serviceAccountClientAuthInterceptor = new ClientAuthInterceptor(testConfig.getService_user());
    client1AuthInterceptor =
        new Client1AuthInterceptor(testConfig.getTestUsers().getOrDefault("primaryUser", null));
    client2AuthInterceptor =
        new Client2AuthInterceptor(testConfig.getTestUsers().getOrDefault("secondaryUser", null));
  }

  private class Client1AuthInterceptor extends ClientAuthInterceptor {

    Client1AuthInterceptor(ServiceUserConfig user) {
      super(user);
      client1Email = user.getEmail();
      client1WorkspaceId = 1L;
      client1UserName = "testUser1";
    }

    public String getClient1Email() {
      return client1Email;
    }
  }

  private class Client2AuthInterceptor extends ClientAuthInterceptor {

    Client2AuthInterceptor(ServiceUserConfig user) {
      super(user);
      client2Email = user.getEmail();
      client2WorkspaceId = 2L;
      client2UserName = "testUser2";
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
          Metadata.Key<String> email_key = Connection.EMAIL_GRPC_METADATA_KEY;
          Metadata.Key<String> dev_key = Connection.DEV_KEY_GRPC_METADATA_KEY;
          Metadata.Key<String> source_key = Connection.SOURCE_GRPC_METADATA_KEY;

          headers.put(email_key, clientEmail);
          headers.put(dev_key, clientDevKey);
          headers.put(source_key, "PythonClient");
          super.start(responseListener, headers);
        }
      };
    }
  }
}
