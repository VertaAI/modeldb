package ai.verta.modeldb.common.authservice;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import ai.verta.uac.*;
import ai.verta.uac.versioning.AuditLogServiceGrpc;
import io.grpc.*;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class AuthServiceChannel implements AutoCloseable {

  private static final Logger LOGGER = LogManager.getLogger(AuthServiceChannel.class);
  private ManagedChannel authServiceChannel;
  private RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingStub;
  private AuthzServiceGrpc.AuthzServiceBlockingStub authzServiceBlockingStub;
  private UACServiceGrpc.UACServiceBlockingStub uacServiceBlockingStub;
  private TeamServiceGrpc.TeamServiceBlockingStub teamServiceBlockingStub;
  private OrganizationServiceGrpc.OrganizationServiceBlockingStub organizationServiceBlockingStub;
  private AuditLogServiceGrpc.AuditLogServiceBlockingStub auditLogServiceBlockingStub;
  private WorkspaceServiceGrpc.WorkspaceServiceFutureStub getWorkspaceServiceFutureStub;
  private CollaboratorServiceGrpc.CollaboratorServiceBlockingStub collaboratorServiceBlockingStub;
  private String serviceUserEmail;
  private String serviceUserDevKey;
  private final Config config;
  public final Context.Key<Metadata> metadataInfo;

  public AuthServiceChannel(
      Config config,
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
    this.config = config;
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

  private <T extends AbstractStub<T>> T attachInterceptors(
      io.grpc.stub.AbstractStub<T> stub, Metadata requestHeaders) {
    if (requestHeaders == null) requestHeaders = getMetadataHeaders();

    ClientInterceptor clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);
    stub = config.getTracingClientInterceptor().map(stub::withInterceptors).orElse((T) stub);
    stub = stub.withInterceptors(clientInterceptor);
    return (T) stub;
  }

  private void initUACServiceStubChannel() {
    uacServiceBlockingStub =
        attachInterceptors(UACServiceGrpc.newBlockingStub(authServiceChannel), null);
  }

  public UACServiceGrpc.UACServiceBlockingStub getUacServiceBlockingStub() {
    if (uacServiceBlockingStub == null) {
      initUACServiceStubChannel();
    }
    return uacServiceBlockingStub;
  }

  private void initRoleServiceStubChannel() {
    roleServiceBlockingStub =
        attachInterceptors(RoleServiceGrpc.newBlockingStub(authServiceChannel), null);
  }

  public RoleServiceGrpc.RoleServiceBlockingStub getRoleServiceBlockingStub() {
    if (roleServiceBlockingStub == null) {
      initRoleServiceStubChannel();
    }
    return roleServiceBlockingStub;
  }

  private void initAuthzServiceStubChannel(Metadata requestHeaders) {
    authzServiceBlockingStub =
        attachInterceptors(AuthzServiceGrpc.newBlockingStub(authServiceChannel), requestHeaders);
  }

  public AuthzServiceGrpc.AuthzServiceBlockingStub getAuthzServiceBlockingStub(
      Metadata requestHeaders) {
    if (authzServiceBlockingStub == null) {
      initAuthzServiceStubChannel(requestHeaders);
    }
    return authzServiceBlockingStub;
  }

  private void initTeamServiceStubChannel() {
    teamServiceBlockingStub =
        attachInterceptors(TeamServiceGrpc.newBlockingStub(authServiceChannel), null);
  }

  public TeamServiceGrpc.TeamServiceBlockingStub getTeamServiceBlockingStub() {
    if (teamServiceBlockingStub == null) {
      initTeamServiceStubChannel();
    }
    return teamServiceBlockingStub;
  }

  private void initOrganizationServiceStubChannel() {
    organizationServiceBlockingStub =
        attachInterceptors(OrganizationServiceGrpc.newBlockingStub(authServiceChannel), null);
  }

  public OrganizationServiceGrpc.OrganizationServiceBlockingStub
      getOrganizationServiceBlockingStub() {
    if (organizationServiceBlockingStub == null) {
      initOrganizationServiceStubChannel();
    }
    return organizationServiceBlockingStub;
  }

  private void initAuditLogServiceStubChannel() {
    auditLogServiceBlockingStub =
        attachInterceptors(AuditLogServiceGrpc.newBlockingStub(authServiceChannel), null);
  }

  public AuditLogServiceGrpc.AuditLogServiceBlockingStub getAuditLogServiceBlockingStub() {
    if (auditLogServiceBlockingStub == null) {
      initAuditLogServiceStubChannel();
    }
    return auditLogServiceBlockingStub;
  }

  private void initWorkspaceServiceStubChannel() {
    getWorkspaceServiceFutureStub =
        attachInterceptors(WorkspaceServiceGrpc.newFutureStub(authServiceChannel), null);
  }

  public WorkspaceServiceGrpc.WorkspaceServiceFutureStub getWorkspaceServiceFutureStub() {
    if (getWorkspaceServiceFutureStub == null) {
      initWorkspaceServiceStubChannel();
    }
    return getWorkspaceServiceFutureStub;
  }

  private void initCollaboratorServiceStubChannel() {
    collaboratorServiceBlockingStub =
        attachInterceptors(CollaboratorServiceGrpc.newBlockingStub(authServiceChannel), null);
  }

  public CollaboratorServiceGrpc.CollaboratorServiceBlockingStub
      getCollaboratorServiceBlockingStub() {
    if (collaboratorServiceBlockingStub == null) {
      initCollaboratorServiceStubChannel();
    }
    return collaboratorServiceBlockingStub;
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
