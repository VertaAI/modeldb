package ai.verta.modeldb.common.connections;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.authservice.AuthServiceChannel;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import ai.verta.uac.*;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings({"squid:S100"})
public class UAC extends Connection {
  private static final Logger LOGGER = LogManager.getLogger(UAC.class);

  private final Config config;
  private final ManagedChannel authServiceChannel;

  private final CollaboratorServiceGrpc.CollaboratorServiceFutureStub collaboratorServiceFutureStub;
  private final CollaboratorServiceGrpc.CollaboratorServiceFutureStub ServiceAccountCollaboratorServiceFutureStub;
  private final UACServiceGrpc.UACServiceFutureStub uacServiceFutureStub;
  private final WorkspaceServiceGrpc.WorkspaceServiceFutureStub workspaceServiceFutureStub;
  private final AuthzServiceGrpc.AuthzServiceFutureStub authzServiceFutureStub;
  private final RoleServiceGrpc.RoleServiceFutureStub roleServiceFutureStub;
  private final RoleServiceGrpc.RoleServiceFutureStub serviceAccountRoleServiceFutureStub;
  private final OrganizationServiceGrpc.OrganizationServiceFutureStub organizationServiceFutureStub;
  private final EventServiceGrpc.EventServiceFutureStub eventServiceFutureStub;

  public static UAC FromConfig(Config config) {
    if (!config.hasAuth()) return null;
    else return new UAC(config);
  }

  private UAC(Config config) {
    this(config.getAuthService().getHost(), config.getAuthService().getPort(), config);
  }

  public UAC(String host, Integer port, Config config) {
    super(config);
    this.config = config;
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
    ServiceAccountCollaboratorServiceFutureStub = CollaboratorServiceGrpc.newFutureStub(authServiceChannel)
            .withInterceptors(
                    MetadataUtils.newAttachHeadersInterceptor(getServiceUserMetadata(config)));
    uacServiceFutureStub = UACServiceGrpc.newFutureStub(authServiceChannel);
    workspaceServiceFutureStub = WorkspaceServiceGrpc.newFutureStub(authServiceChannel);
    authzServiceFutureStub = AuthzServiceGrpc.newFutureStub(authServiceChannel);
    roleServiceFutureStub = RoleServiceGrpc.newFutureStub(authServiceChannel);
    serviceAccountRoleServiceFutureStub =
        RoleServiceGrpc.newFutureStub(authServiceChannel)
            .withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(getServiceUserMetadata(config)));
    organizationServiceFutureStub = OrganizationServiceGrpc.newFutureStub(authServiceChannel);
    eventServiceFutureStub =
        EventServiceGrpc.newFutureStub(authServiceChannel)
            .withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(getServiceUserMetadata(config)));
  }

  public AuthServiceChannel getBlockingAuthServiceChannel() {
    return new AuthServiceChannel(config);
  }

  private Metadata getServiceUserMetadata(Config config) {
    var requestHeaders = new Metadata();
    var emailKey = Metadata.Key.of("email", Metadata.ASCII_STRING_MARSHALLER);
    var devKey = Metadata.Key.of("developer_key", Metadata.ASCII_STRING_MARSHALLER);
    var devKeyHyphen = Metadata.Key.of("developer-key", Metadata.ASCII_STRING_MARSHALLER);
    var sourceKey = Metadata.Key.of("source", Metadata.ASCII_STRING_MARSHALLER);

    requestHeaders.put(emailKey, config.getService_user().getEmail());
    requestHeaders.put(devKey, config.getService_user().getDevKey());
    requestHeaders.put(devKeyHyphen, config.getService_user().getDevKey());
    requestHeaders.put(sourceKey, "PythonClient");
    return requestHeaders;
  }

  public CollaboratorServiceGrpc.CollaboratorServiceFutureStub getCollaboratorService() {
    return attachInterceptors(collaboratorServiceFutureStub);
  }

  public CollaboratorServiceGrpc.CollaboratorServiceFutureStub getServiceAccountCollaboratorServiceForServiceUser() {
    return attachInterceptors(ServiceAccountCollaboratorServiceFutureStub);
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

  public OrganizationServiceGrpc.OrganizationServiceFutureStub getOrganizationService() {
    return attachInterceptors(organizationServiceFutureStub);
  }

  public RoleServiceGrpc.RoleServiceFutureStub getRoleService() {
    return attachInterceptors(roleServiceFutureStub);
  }

  public RoleServiceGrpc.RoleServiceFutureStub getServiceAccountRoleServiceFutureStub() {
    return serviceAccountRoleServiceFutureStub;
  }

  public EventServiceGrpc.EventServiceFutureStub getEventService() {
    return eventServiceFutureStub;
  }
}
