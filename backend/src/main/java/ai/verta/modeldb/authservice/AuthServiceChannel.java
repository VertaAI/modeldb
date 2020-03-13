package ai.verta.modeldb.authservice;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.uac.AuthzServiceGrpc;
import ai.verta.uac.OrganizationServiceGrpc;
import ai.verta.uac.RoleServiceGrpc;
import ai.verta.uac.TeamServiceGrpc;
import ai.verta.uac.UACServiceGrpc;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.MetadataUtils;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthServiceChannel implements AutoCloseable {

  private static final Logger LOGGER = LogManager.getLogger(AuthServiceChannel.class);
  private ManagedChannel authServiceChannel;
  private RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingStub;
  private AuthzServiceGrpc.AuthzServiceBlockingStub authzServiceBlockingStub;
  private UACServiceGrpc.UACServiceBlockingStub uacServiceBlockingStub;
  private TeamServiceGrpc.TeamServiceBlockingStub teamServiceBlockingStub;
  private OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationServiceBlockingStub;
  public static boolean isMigrationUtilsCall = false;

  public AuthServiceChannel() {
    App app = App.getInstance();
    String host = app.getAuthServerHost();
    Integer port = app.getAuthServerPort();
    LOGGER.trace(ModelDBMessages.HOST_PORT_INFO_STR, host, port);
    if (host != null && port != null) { // AuthService not available.
      authServiceChannel =
          ManagedChannelBuilder.forTarget(host + ModelDBConstants.STRING_COLON + port)
              .usePlaintext()
              .build();
    } else {
      Status status =
          Status.newBuilder()
              .setCode(Code.UNAVAILABLE_VALUE)
              .setMessage("Host OR Port not found for contacting authentication service")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  private Metadata getMetadataHeaders() {
    Metadata requestHeaders;
    if (isMigrationUtilsCall && ModelDBAuthInterceptor.METADATA_INFO.get() == null) {
      Metadata.Key<String> source_key = Metadata.Key.of("source", Metadata.ASCII_STRING_MARSHALLER);
      requestHeaders = new Metadata();
      requestHeaders.put(source_key, "PythonClient");
    } else {
      requestHeaders = ModelDBAuthInterceptor.METADATA_INFO.get();
    }
    return requestHeaders;
  }

  private void initUACServiceStubChannel() {
    Metadata requestHeaders = getMetadataHeaders();
    LOGGER.trace("Header attaching with stub : {}", requestHeaders);
    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    uacServiceBlockingStub =
        UACServiceGrpc.newBlockingStub(authServiceChannel).withInterceptors(clientInterceptor);
    LOGGER.trace("Header attached with stub");
  }

  public UACServiceGrpc.UACServiceBlockingStub getUacServiceBlockingStub() {
    if (uacServiceBlockingStub == null) {
      initUACServiceStubChannel();
    }
    return uacServiceBlockingStub;
  }

  private void initRoleServiceStubChannel() {
    Metadata requestHeaders = getMetadataHeaders();
    LOGGER.trace("Header attaching with stub : {}", requestHeaders);
    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    roleServiceBlockingStub =
        RoleServiceGrpc.newBlockingStub(authServiceChannel).withInterceptors(clientInterceptor);
    LOGGER.trace("Header attached with stub");
  }

  public RoleServiceGrpc.RoleServiceBlockingStub getRoleServiceBlockingStub() {
    if (roleServiceBlockingStub == null) {
      initRoleServiceStubChannel();
    }
    return roleServiceBlockingStub;
  }

  private void initAuthzServiceStubChannel(Metadata requestHeaders) {
    if (requestHeaders == null) requestHeaders = getMetadataHeaders();
    LOGGER.trace("Header attaching with stub : {}", requestHeaders);
    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    authzServiceBlockingStub =
        AuthzServiceGrpc.newBlockingStub(authServiceChannel).withInterceptors(clientInterceptor);
    LOGGER.trace("Header attached with stub");
  }

  public AuthzServiceGrpc.AuthzServiceBlockingStub getAuthzServiceBlockingStub(
      Metadata requestHeaders) {
    if (authzServiceBlockingStub == null) {
      initAuthzServiceStubChannel(requestHeaders);
    }
    return authzServiceBlockingStub;
  }

  private void initTeamServiceStubChannel() {
    Metadata requestHeaders = getMetadataHeaders();
    LOGGER.trace("Header attaching with stub : {}", requestHeaders);
    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    teamServiceBlockingStub =
        TeamServiceGrpc.newBlockingStub(authServiceChannel).withInterceptors(clientInterceptor);
    LOGGER.trace("Header attached with stub");
  }

  public TeamServiceGrpc.TeamServiceBlockingStub getTeamServiceBlockingStub() {
    if (teamServiceBlockingStub == null) {
      initTeamServiceStubChannel();
    }
    return teamServiceBlockingStub;
  }

  private void initOrganizationServiceStubChannel() {
    Metadata requestHeaders = getMetadataHeaders();
    LOGGER.trace("Header attaching with stub : {}", requestHeaders);
    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    organizationServiceBlockingStub =
        OrganizationServiceGrpc.newBlockingStub(authServiceChannel)
            .withInterceptors(clientInterceptor);
    LOGGER.trace("Header attached with stub");
  }

  public OrganizationServiceGrpc.OrganizationServiceBlockingStub
      getOrganizationServiceBlockingStub() {
    if (organizationServiceBlockingStub == null) {
      initOrganizationServiceStubChannel();
    }
    return organizationServiceBlockingStub;
  }

  @Override
  public void close() throws StatusRuntimeException {
    try {
      if (authServiceChannel != null) {
        authServiceChannel.shutdown();
      }
    } catch (Exception ex) {
      LOGGER.trace(ModelDBConstants.AUTH_SERVICE_CHANNEL_CLOSE_ERROR, ex);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL_VALUE)
              .setMessage(ModelDBConstants.AUTH_SERVICE_CHANNEL_CLOSE_ERROR + ex.getMessage())
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    } finally {
      if (authServiceChannel != null && !authServiceChannel.isShutdown()) {
        try {
          authServiceChannel.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
          LOGGER.warn(ex.getMessage(), ex);
          Status status =
              Status.newBuilder()
                  .setCode(Code.INTERNAL_VALUE)
                  .setMessage("AuthService channel termination error: " + ex.getMessage())
                  .build();
          throw StatusProto.toStatusRuntimeException(status);
        }
      }
    }
  }
}
