package ai.verta.modeldb.common.authservice;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import ai.verta.uac.*;
import ai.verta.uac.versioning.AuditLogServiceGrpc;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class AuthServiceChannel implements AutoCloseable {

  private static final Logger LOGGER = LogManager.getLogger(AuthServiceChannel.class);
  private ManagedChannel authServiceChannel;
  private RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingStub;
  private RoleServiceGrpc.RoleServiceFutureStub roleServiceFutureStub;
  private AuthzServiceGrpc.AuthzServiceBlockingStub authzServiceBlockingStub;
  private UACServiceGrpc.UACServiceBlockingStub uacServiceBlockingStub;
  private TeamServiceGrpc.TeamServiceBlockingStub teamServiceBlockingStub;
  private OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationServiceBlockingStub;
  private AuditLogServiceGrpc.AuditLogServiceBlockingStub auditLogServiceBlockingStub;
  private WorkspaceServiceGrpc.WorkspaceServiceBlockingStub workspaceServiceBlockingStub;
  private CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceBlockingStub;
  private CollaboratorServiceGrpc.CollaboratorServiceFutureStub collaboratorServiceFutureStub;
  private String serviceUserEmail;
  private String serviceUserDevKey;
  public final Context.Key<Metadata> metadataInfo;

  public AuthServiceChannel(
      String host,
      Integer port,
      String serviceUserEmail,
      String serviceUserDevKey,
      Context.Key<Metadata> metadataInfo) {
    LOGGER.trace(CommonMessages.HOST_PORT_INFO_STR, host, port);
    if (host != null && port != null) { // AuthService not available.
      authServiceChannel =
          ManagedChannelBuilder.forTarget(host + CommonConstants.STRING_COLON + port)
              .usePlaintext()
              .build();

      this.serviceUserEmail = serviceUserEmail;
      this.serviceUserDevKey = serviceUserDevKey;
    } else {
      throw new UnavailableException(
          "Host OR Port not found for contacting authentication service");
    }
    this.metadataInfo = metadataInfo;
  }

  private Metadata getMetadataHeaders() {
    int backgroundUtilsCount = CommonUtils.getRegisteredBackgroundUtilsCount();
    LOGGER.trace("Header attaching with stub : backgroundUtilsCount : {}", backgroundUtilsCount);
    Metadata requestHeaders;
    if (backgroundUtilsCount > 0 && (metadataInfo == null || metadataInfo.get() == null)) {
      requestHeaders = new Metadata();
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
    } else {
      requestHeaders = metadataInfo.get();
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

  private void initRoleServiceFutureStubChannel() {
    Metadata requestHeaders = getMetadataHeaders();
    LOGGER.trace("Header attaching with stub : {}", requestHeaders);
    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    roleServiceFutureStub =
        RoleServiceGrpc.newFutureStub(authServiceChannel).withInterceptors(clientInterceptor);
    LOGGER.trace("Header attached with stub");
  }

  public RoleServiceGrpc.RoleServiceFutureStub getRoleServiceFutureStub() {
    if (roleServiceFutureStub == null) {
      initRoleServiceFutureStubChannel();
    }
    return roleServiceFutureStub;
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

  private void initAuditLogServiceStubChannel() {
    Metadata requestHeaders = getMetadataHeaders();
    LOGGER.trace("Header attaching with stub : {}", requestHeaders);
    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    auditLogServiceBlockingStub =
        AuditLogServiceGrpc.newBlockingStub(authServiceChannel).withInterceptors(clientInterceptor);
    LOGGER.trace("Header attached with stub");
  }

  public AuditLogServiceGrpc.AuditLogServiceBlockingStub getAuditLogServiceBlockingStub() {
    if (auditLogServiceBlockingStub == null) {
      initAuditLogServiceStubChannel();
    }
    return auditLogServiceBlockingStub;
  }

  private void initWorkspaceServiceStubChannel() {
    Metadata requestHeaders = getMetadataHeaders();
    LOGGER.trace("Header attaching with stub : {}", requestHeaders);
    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    workspaceServiceBlockingStub =
        WorkspaceServiceGrpc.newBlockingStub(authServiceChannel)
            .withInterceptors(clientInterceptor);
    LOGGER.trace("Header attached with stub");
  }

  public WorkspaceServiceGrpc.WorkspaceServiceBlockingStub getWorkspaceServiceBlockingStub() {
    if (workspaceServiceBlockingStub == null) {
      initWorkspaceServiceStubChannel();
    }
    return workspaceServiceBlockingStub;
  }

  private void initCollaboratorServiceStubChannel() {
    Metadata requestHeaders = getMetadataHeaders();
    LOGGER.trace("Header attaching with stub : {}", requestHeaders);
    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    collaboratorServiceBlockingStub =
        CollaboratorServiceGrpc.newBlockingStub(authServiceChannel)
            .withInterceptors(clientInterceptor);
    LOGGER.trace("Header attached with stub");
  }

  private void initCollaboratorServiceFutureStubChannel() {
    Metadata requestHeaders = getMetadataHeaders();
    LOGGER.trace("Header attaching with stub : {}", requestHeaders);
    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    collaboratorServiceFutureStub =
        CollaboratorServiceGrpc.newFutureStub(authServiceChannel)
            .withInterceptors(clientInterceptor);
    LOGGER.trace("Header attached with stub");
  }

  public CollaboratorServiceGrpc.CollaboratorServiceBlockingStub
      getCollaboratorServiceBlockingStub() {
    if (collaboratorServiceBlockingStub == null) {
      initCollaboratorServiceStubChannel();
    }
    return collaboratorServiceBlockingStub;
  }

  public CollaboratorServiceGrpc.CollaboratorServiceFutureStub getCollaboratorServiceFutureStub() {
    if (collaboratorServiceFutureStub == null) {
      initCollaboratorServiceFutureStubChannel();
    }
    return collaboratorServiceFutureStub;
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
