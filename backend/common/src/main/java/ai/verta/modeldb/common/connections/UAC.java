package ai.verta.modeldb.common.connections;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import ai.verta.uac.*;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UAC extends Connection {
  private static final Logger LOGGER = LogManager.getLogger(UAC.class);

  private final ManagedChannel authServiceChannel;

  private final CollaboratorServiceGrpc.CollaboratorServiceFutureStub collaboratorServiceFutureStub;
  private final UACServiceGrpc.UACServiceFutureStub uacServiceFutureStub;
  private final WorkspaceServiceGrpc.WorkspaceServiceFutureStub workspaceServiceFutureStub;
  private final AuthzServiceGrpc.AuthzServiceFutureStub authzServiceFutureStub;
  private final RoleServiceGrpc.RoleServiceFutureStub roleServiceFutureStub;
  private final RoleServiceGrpc.RoleServiceFutureStub serviceAccountRoleServiceFutureStub;
  private final OrganizationServiceGrpc.OrganizationServiceFutureStub organizationServiceFutureStub;

  public static UAC FromConfig(Config config) {
    if (!config.hasAuth()) return null;
    else return new UAC(config);
  }

  private UAC(Config config) {
    this(
        config.authService.host,
        config.authService.port,
        config);
  }

  public UAC(
      String host, Integer port, Config config) {
    super(config);
    LOGGER.trace(CommonMessages.HOST_PORT_INFO_STR, host, port);
    if (host != null && port != null) { // AuthService not available.
      authServiceChannel =
          ManagedChannelBuilder.forTarget(host + CommonConstants.STRING_COLON + port)
              .usePlaintext()
              .build();
    } else {
      throw new UnavailableException(
          "Host OR Port not found for contacting authentication service");
    }

    collaboratorServiceFutureStub = CollaboratorServiceGrpc.newFutureStub(authServiceChannel);
    uacServiceFutureStub = UACServiceGrpc.newFutureStub(authServiceChannel);
    workspaceServiceFutureStub = WorkspaceServiceGrpc.newFutureStub(authServiceChannel);
    authzServiceFutureStub = AuthzServiceGrpc.newFutureStub(authServiceChannel);
    roleServiceFutureStub = RoleServiceGrpc.newFutureStub(authServiceChannel);
    serviceAccountRoleServiceFutureStub = RoleServiceGrpc.newFutureStub(authServiceChannel)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(getServiceUserMetadata(config)));
    organizationServiceFutureStub = OrganizationServiceGrpc.newFutureStub(authServiceChannel);
  }

  private Metadata getServiceUserMetadata(Config config) {
    Metadata requestHeaders = new Metadata();
    Metadata.Key<String> email_key = Metadata.Key.of("email", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> dev_key =
            Metadata.Key.of("developer_key", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> dev_key_hyphen =
            Metadata.Key.of("developer-key", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> source_key = Metadata.Key.of("source", Metadata.ASCII_STRING_MARSHALLER);

    requestHeaders.put(email_key, config.service_user.email);
    requestHeaders.put(dev_key, config.service_user.devKey);
    requestHeaders.put(dev_key_hyphen, config.service_user.devKey);
    requestHeaders.put(source_key, "PythonClient");
    return requestHeaders;
  }

  public CollaboratorServiceGrpc.CollaboratorServiceFutureStub getCollaboratorService() {
    return attachInterceptors(collaboratorServiceFutureStub);
  }

  public UACServiceGrpc.UACServiceFutureStub getUACService() {
    return attachInterceptors(uacServiceFutureStub);
  }

  public WorkspaceServiceGrpc.WorkspaceServiceFutureStub getWorkspaceService() {
    return attachInterceptors(workspaceServiceFutureStub);
  }

  public AuthzServiceGrpc.AuthzServiceFutureStub getAuthzService() {
    return attachInterceptors(authzServiceFutureStub);
  }

  public OrganizationServiceGrpc.OrganizationServiceFutureStub getOrganizationService( ) {
    return attachInterceptors(organizationServiceFutureStub);
  }

  public RoleServiceGrpc.RoleServiceFutureStub getRoleService() {
    return attachInterceptors(roleServiceFutureStub);
  }

  public RoleServiceGrpc.RoleServiceFutureStub getServiceAccountRoleServiceFutureStub() {
    return serviceAccountRoleServiceFutureStub;
  }
}
