package ai.verta.modeldb.common.authservice;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.connections.Connection;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import ai.verta.uac.*;
import io.grpc.*;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class AuthServiceChannel extends Connection implements AutoCloseable {

  private static final Logger LOGGER = LogManager.getLogger(AuthServiceChannel.class);
  private final ManagedChannel authServiceChannel;
  private RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingStub;
  private RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingStubForServiceUser;
  private AuthzServiceGrpc.AuthzServiceBlockingStub authzServiceBlockingStub;
  private UACServiceGrpc.UACServiceBlockingStub uacServiceBlockingStub;
  private TeamServiceGrpc.TeamServiceBlockingStub teamServiceBlockingStub;
  private OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationServiceBlockingStub;
  private WorkspaceServiceGrpc.WorkspaceServiceBlockingStub workspaceServiceBlockingStub;
  private CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceBlockingStub;
  private CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceBlockingStubForServiceUser;
  private final String serviceUserEmail;
  private final String serviceUserDevKey;
  private final Config config;

  public AuthServiceChannel(
      Config config) {
    super(config);
    String host = config.authService.host;
    int port = config.authService.port;
    LOGGER.trace(CommonMessages.HOST_PORT_INFO_STR, host, port);
    if (host != null && port != 0) { // AuthService not available.
      authServiceChannel =
          ManagedChannelBuilder.forTarget(host + CommonConstants.STRING_COLON + port)
              .usePlaintext()
              .build();

      this.serviceUserEmail = config.service_user.email;
      this.serviceUserDevKey = config.service_user.devKey;
    } else {
      throw new UnavailableException(
          "Host OR Port not found for contacting authentication service");
    }
    this.config = config;
  }

  private Metadata getServiceUserMetadataHeaders() {
      Metadata requestHeaders = new Metadata();
      Metadata.Key<String> email_key = Metadata.Key.of("email", Metadata.ASCII_STRING_MARSHALLER);
      Metadata.Key<String> dev_key =
              Metadata.Key.of("developer_key", Metadata.ASCII_STRING_MARSHALLER);
      Metadata.Key<String> dev_key_hyphen =
              Metadata.Key.of("developer-key", Metadata.ASCII_STRING_MARSHALLER);
      Metadata.Key<String> source_key = Metadata.Key.of("source", Metadata.ASCII_STRING_MARSHALLER);

      requestHeaders.put(email_key, this.serviceUserEmail);
      requestHeaders.put(dev_key, this.serviceUserDevKey);
      requestHeaders.put(dev_key_hyphen, this.serviceUserDevKey);
      requestHeaders.put(source_key, "PythonClient");
    return requestHeaders;
  }

  private <T extends AbstractStub<T>> T attachInterceptorsWithRequestHeaders(
          io.grpc.stub.AbstractStub<T> stub, Metadata requestHeaders) {
    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    stub = config.getTracingClientInterceptor().map(stub::withInterceptors).orElse((T) stub);
    stub = stub.withInterceptors(clientInterceptor);
    return (T) stub;
  }

  private <T extends AbstractStub<T>> T attachInterceptorsForServiceUser(
          io.grpc.stub.AbstractStub<T> stub) {

    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(getServiceUserMetadataHeaders());
    stub = config.getTracingClientInterceptor().map(stub::withInterceptors).orElse((T) stub);
    stub = stub.withInterceptors(clientInterceptor);
    return (T) stub;
  }

  private void initUACServiceStubChannel() {
    uacServiceBlockingStub =
        attachInterceptors(UACServiceGrpc.newBlockingStub(authServiceChannel));
  }

  public UACServiceGrpc.UACServiceBlockingStub getUacServiceBlockingStub() {
    if (uacServiceBlockingStub == null) {
      initUACServiceStubChannel();
    }
    return uacServiceBlockingStub;
  }

  private void initRoleServiceStubChannel() {
    roleServiceBlockingStub =
        attachInterceptors(RoleServiceGrpc.newBlockingStub(authServiceChannel));
  }

  public RoleServiceGrpc.RoleServiceBlockingStub getRoleServiceBlockingStub() {
    if (roleServiceBlockingStub == null) {
      initRoleServiceStubChannel();
    }
    return roleServiceBlockingStub;
  }

  private void initRoleServiceStubChannelForServiceUser() {
    roleServiceBlockingStubForServiceUser =
            attachInterceptorsForServiceUser(RoleServiceGrpc.newBlockingStub(authServiceChannel));
  }

  public RoleServiceGrpc.RoleServiceBlockingStub getRoleServiceBlockingStubForServiceUser() {
    if (roleServiceBlockingStubForServiceUser == null) {
      initRoleServiceStubChannelForServiceUser();
    }
    return roleServiceBlockingStubForServiceUser;
  }

  private void initAuthzServiceStubChannel(Metadata requestHeaders) {
    authzServiceBlockingStub =
        attachInterceptorsWithRequestHeaders(AuthzServiceGrpc.newBlockingStub(authServiceChannel), requestHeaders);
  }

  public AuthzServiceGrpc.AuthzServiceBlockingStub getAuthzServiceBlockingStub(
      Metadata requestHeaders) {
    if (authzServiceBlockingStub == null) {
      initAuthzServiceStubChannel(requestHeaders);
    }
    return authzServiceBlockingStub;
  }

  private void initAuthzServiceStubChannel() {
    authzServiceBlockingStub =
            attachInterceptors(AuthzServiceGrpc.newBlockingStub(authServiceChannel));
  }

  public AuthzServiceGrpc.AuthzServiceBlockingStub getAuthzServiceBlockingStub() {
    if (authzServiceBlockingStub == null) {
      initAuthzServiceStubChannel();
    }
    return authzServiceBlockingStub;
  }

  private void initTeamServiceStubChannel() {
    teamServiceBlockingStub =
        attachInterceptors(TeamServiceGrpc.newBlockingStub(authServiceChannel));
  }

  public TeamServiceGrpc.TeamServiceBlockingStub getTeamServiceBlockingStub() {
    if (teamServiceBlockingStub == null) {
      initTeamServiceStubChannel();
    }
    return teamServiceBlockingStub;
  }

  private void initOrganizationServiceStubChannel() {
    organizationServiceBlockingStub =
        attachInterceptors(OrganizationServiceGrpc.newBlockingStub(authServiceChannel));
  }

  public OrganizationServiceGrpc.OrganizationServiceBlockingStub
      getOrganizationServiceBlockingStub() {
    if (organizationServiceBlockingStub == null) {
      initOrganizationServiceStubChannel();
    }
    return organizationServiceBlockingStub;
  }

  private void initWorkspaceServiceStubChannel() {
    workspaceServiceBlockingStub =
        attachInterceptors(WorkspaceServiceGrpc.newBlockingStub(authServiceChannel));
  }

  public WorkspaceServiceGrpc.WorkspaceServiceBlockingStub getWorkspaceServiceBlockingStub() {
    if (workspaceServiceBlockingStub == null) {
      initWorkspaceServiceStubChannel();
    }
    return workspaceServiceBlockingStub;
  }

  private void initCollaboratorServiceStubChannel() {
    collaboratorServiceBlockingStub =
        attachInterceptors(CollaboratorServiceGrpc.newBlockingStub(authServiceChannel));
  }

  private void initCollaboratorServiceStubChannelForServiceUser() {
    collaboratorServiceBlockingStubForServiceUser =
            attachInterceptorsForServiceUser(CollaboratorServiceGrpc.newBlockingStub(authServiceChannel));
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

  @Override
  public void close() throws StatusRuntimeException {
    try {
      if (authServiceChannel != null) {
        authServiceChannel.shutdown();
      }
    } catch (Exception ex) {
      throw new InternalErrorException(
          CommonMessages.AUTH_SERVICE_CHANNEL_CLOSE_ERROR + ex.getMessage());
    } finally {
      if (authServiceChannel != null && !authServiceChannel.isShutdown()) {
        try {
          authServiceChannel.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
          LOGGER.warn(ex.getMessage(), ex);
          throw new InternalErrorException(
              "AuthService channel termination error: " + ex.getMessage());
        }
      }
    }
  }
}
