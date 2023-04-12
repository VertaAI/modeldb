package ai.verta.modeldb.common.authservice;

import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.connections.Connection;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import ai.verta.uac.*;
import io.grpc.*;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthServiceChannel extends Connection implements AutoCloseable {

  private static final Logger LOGGER = LogManager.getLogger(AuthServiceChannel.class);
  private final ManagedChannel authChannel;
  private RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingStub;
  private RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingStubForServiceUser;
  private AuthzServiceGrpc.AuthzServiceBlockingStub authzServiceBlockingStub;
  private UACServiceGrpc.UACServiceBlockingStub uacServiceBlockingStub;
  private UACServiceGrpc.UACServiceBlockingStub uacServiceBlockingStubForServiceUser;
  private TeamServiceGrpc.TeamServiceBlockingStub teamServiceBlockingStub;
  private OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationServiceBlockingStub;
  private OrganizationServiceGrpc.OrganizationServiceBlockingStub
      organizationServiceBlockingStubForServiceUser;
  private WorkspaceServiceGrpc.WorkspaceServiceBlockingStub workspaceServiceBlockingStub;
  private CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceBlockingStub;
  private CollaboratorServiceGrpc.CollaboratorServiceBlockingStub
      collaboratorServiceBlockingStubForServiceUser;
  private final String serviceUserEmail;
  private final String serviceUserDevKey;

  public AuthServiceChannel(Config config, Optional<ClientInterceptor> tracingClientInterceptor) {
    super(tracingClientInterceptor);
    String host = config.getAuthService().getHost();
    int port = config.getAuthService().getPort();
    LOGGER.trace(CommonMessages.HOST_PORT_INFO_STR, host, port);
    if (host != null && port != 0) { // AuthService not available.
      authChannel = ManagedChannelBuilder.forTarget(host + ":" + port).usePlaintext().build();

      this.serviceUserEmail = config.getService_user().getEmail();
      this.serviceUserDevKey = config.getService_user().getDevKey();
    } else {
      throw new UnavailableException(
          "Host OR Port not found for contacting authentication service");
    }
  }

  private Metadata getServiceUserMetadataHeaders() {
    var requestHeaders = new Metadata();
    var emailKey = Metadata.Key.of("email", Metadata.ASCII_STRING_MARSHALLER);
    var devKey = Metadata.Key.of("developer_key", Metadata.ASCII_STRING_MARSHALLER);
    var devKeyHyphen = Metadata.Key.of("developer-key", Metadata.ASCII_STRING_MARSHALLER);
    var sourceKey = Metadata.Key.of("source", Metadata.ASCII_STRING_MARSHALLER);

    requestHeaders.put(emailKey, this.serviceUserEmail);
    requestHeaders.put(devKey, this.serviceUserDevKey);
    requestHeaders.put(devKeyHyphen, this.serviceUserDevKey);
    requestHeaders.put(sourceKey, "PythonClient");
    return requestHeaders;
  }

  private <T extends AbstractStub<T>> T attachInterceptorsWithRequestHeaders(
      io.grpc.stub.AbstractStub<T> stub, Metadata requestHeaders) {
    var clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    stub = super.getTracingClientInterceptor().map(stub::withInterceptors).orElse((T) stub);
    stub = stub.withInterceptors(clientInterceptor);
    return (T) stub;
  }

  private <T extends AbstractStub<T>> T attachInterceptorsForServiceUser(
      io.grpc.stub.AbstractStub<T> stub) {

    var clientInterceptor =
        MetadataUtils.newAttachHeadersInterceptor(getServiceUserMetadataHeaders());
    stub = super.getTracingClientInterceptor().map(stub::withInterceptors).orElse((T) stub);
    stub = stub.withInterceptors(clientInterceptor);
    return (T) stub;
  }

  private void initUACServiceStubChannel() {
    uacServiceBlockingStub = attachInterceptors(UACServiceGrpc.newBlockingStub(authChannel));
  }

  public UACServiceGrpc.UACServiceBlockingStub getUacServiceBlockingStub() {
    if (uacServiceBlockingStub == null) {
      initUACServiceStubChannel();
    }
    return uacServiceBlockingStub;
  }

  private void initUACServiceStubChannelForServiceUser() {
    uacServiceBlockingStubForServiceUser =
        attachInterceptorsForServiceUser(UACServiceGrpc.newBlockingStub(authChannel));
  }

  public UACServiceGrpc.UACServiceBlockingStub getUacServiceBlockingStubForServiceUser() {
    if (uacServiceBlockingStubForServiceUser == null) {
      initUACServiceStubChannelForServiceUser();
    }
    return uacServiceBlockingStubForServiceUser;
  }

  private void initRoleServiceStubChannel() {
    roleServiceBlockingStub = attachInterceptors(RoleServiceGrpc.newBlockingStub(authChannel));
  }

  public RoleServiceGrpc.RoleServiceBlockingStub getRoleServiceBlockingStub() {
    if (roleServiceBlockingStub == null) {
      initRoleServiceStubChannel();
    }
    return roleServiceBlockingStub;
  }

  private void initRoleServiceStubChannelForServiceUser() {
    roleServiceBlockingStubForServiceUser =
        attachInterceptorsForServiceUser(RoleServiceGrpc.newBlockingStub(authChannel));
  }

  public RoleServiceGrpc.RoleServiceBlockingStub getRoleServiceBlockingStubForServiceUser() {
    if (roleServiceBlockingStubForServiceUser == null) {
      initRoleServiceStubChannelForServiceUser();
    }
    return roleServiceBlockingStubForServiceUser;
  }

  private void initAuthzServiceStubChannel(Metadata requestHeaders) {
    authzServiceBlockingStub =
        attachInterceptorsWithRequestHeaders(
            AuthzServiceGrpc.newBlockingStub(authChannel), requestHeaders);
  }

  public AuthzServiceGrpc.AuthzServiceBlockingStub getAuthzServiceBlockingStub(
      Metadata requestHeaders) {
    if (authzServiceBlockingStub == null) {
      initAuthzServiceStubChannel(requestHeaders);
    }
    return authzServiceBlockingStub;
  }

  private void initAuthzServiceStubChannel() {
    authzServiceBlockingStub = attachInterceptors(AuthzServiceGrpc.newBlockingStub(authChannel));
  }

  public AuthzServiceGrpc.AuthzServiceBlockingStub getAuthzServiceBlockingStub() {
    if (authzServiceBlockingStub == null) {
      initAuthzServiceStubChannel();
    }
    return authzServiceBlockingStub;
  }

  private void initTeamServiceStubChannel() {
    teamServiceBlockingStub = attachInterceptors(TeamServiceGrpc.newBlockingStub(authChannel));
  }

  public TeamServiceGrpc.TeamServiceBlockingStub getTeamServiceBlockingStub() {
    if (teamServiceBlockingStub == null) {
      initTeamServiceStubChannel();
    }
    return teamServiceBlockingStub;
  }

  private void initOrganizationServiceStubChannel() {
    organizationServiceBlockingStub =
        attachInterceptors(OrganizationServiceGrpc.newBlockingStub(authChannel));
  }

  public OrganizationServiceGrpc.OrganizationServiceBlockingStub
      getOrganizationServiceBlockingStub() {
    if (organizationServiceBlockingStub == null) {
      initOrganizationServiceStubChannel();
    }
    return organizationServiceBlockingStub;
  }

  private void initOrganizationServiceStubChannelForServiceUser() {
    organizationServiceBlockingStubForServiceUser =
        attachInterceptorsForServiceUser(OrganizationServiceGrpc.newBlockingStub(authChannel));
  }

  public OrganizationServiceGrpc.OrganizationServiceBlockingStub
      getOrganizationServiceBlockingStubForServiceUser() {
    if (organizationServiceBlockingStubForServiceUser == null) {
      initOrganizationServiceStubChannelForServiceUser();
    }
    return organizationServiceBlockingStubForServiceUser;
  }

  private void initWorkspaceServiceStubChannel() {
    workspaceServiceBlockingStub =
        attachInterceptors(WorkspaceServiceGrpc.newBlockingStub(authChannel));
  }

  public WorkspaceServiceGrpc.WorkspaceServiceBlockingStub getWorkspaceServiceBlockingStub() {
    if (workspaceServiceBlockingStub == null) {
      initWorkspaceServiceStubChannel();
    }
    return workspaceServiceBlockingStub;
  }

  private void initCollaboratorServiceStubChannel() {
    collaboratorServiceBlockingStub =
        attachInterceptors(CollaboratorServiceGrpc.newBlockingStub(authChannel));
  }

  private void initCollaboratorServiceStubChannelForServiceUser() {
    collaboratorServiceBlockingStubForServiceUser =
        attachInterceptorsForServiceUser(CollaboratorServiceGrpc.newBlockingStub(authChannel));
  }

  public CollaboratorServiceGrpc.CollaboratorServiceBlockingStub
      getCollaboratorServiceBlockingStub() {
    if (collaboratorServiceBlockingStub == null) {
      initCollaboratorServiceStubChannel();
    }
    return collaboratorServiceBlockingStub;
  }

  public CollaboratorServiceGrpc.CollaboratorServiceBlockingStub
      getCollaboratorServiceBlockingStubForServiceUser() {
    if (collaboratorServiceBlockingStubForServiceUser == null) {
      initCollaboratorServiceStubChannelForServiceUser();
    }
    return collaboratorServiceBlockingStubForServiceUser;
  }

  @SuppressWarnings({"squid:S1163", "squid:S1143"})
  @Override
  public void close() throws StatusRuntimeException {
    try {
      if (authChannel != null) {
        authChannel.shutdown();
      }
    } catch (Exception ex) {
      throw new InternalErrorException(
          CommonMessages.AUTH_SERVICE_CHANNEL_CLOSE_ERROR + ex.getMessage());
    } finally {
      if (authChannel != null && !authChannel.isShutdown()) {
        try {
          authChannel.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
          LOGGER.warn(ex.getMessage(), ex);
          // Restore interrupted state...
          Thread.currentThread().interrupt();
          throw new InternalErrorException(
              "AuthService channel termination error: " + ex.getMessage());
        }
      }
    }
  }
}
