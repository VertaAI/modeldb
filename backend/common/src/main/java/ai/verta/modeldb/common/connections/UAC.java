package ai.verta.modeldb.common.connections;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.authservice.AuthServiceChannel;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import ai.verta.uac.*;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings({"squid:S100"})
public class UAC extends Connection {
  private static final Logger LOGGER = LogManager.getLogger(UAC.class);

  private final Config config;

  private final CollaboratorServiceGrpc.CollaboratorServiceFutureStub collaboratorServiceFutureStub;
  private final CollaboratorServiceGrpc.CollaboratorServiceFutureStub
      serviceAccountCollaboratorServiceFutureStub;
  private final UACServiceGrpc.UACServiceFutureStub uacServiceFutureStub;
  private final WorkspaceServiceGrpc.WorkspaceServiceFutureStub workspaceServiceFutureStub;
  private final AuthzServiceGrpc.AuthzServiceFutureStub authzServiceFutureStub;
  private final RoleServiceGrpc.RoleServiceFutureStub roleServiceFutureStub;
  private final RoleServiceGrpc.RoleServiceFutureStub serviceAccountRoleServiceFutureStub;
  private final OrganizationServiceGrpc.OrganizationServiceFutureStub organizationServiceFutureStub;
  private final EventServiceGrpc.EventServiceFutureStub eventServiceFutureStub;
  private final OrganizationServiceV2Grpc.OrganizationServiceV2FutureStub
      organizationServiceV2FutureStub;

  private final WorkspaceServiceV2Grpc.WorkspaceServiceV2FutureStub workspaceServiceV2FutureStub;

  public static UAC fromConfig(
      Config config, Optional<ClientInterceptor> tracingClientInterceptor) {
    if (!config.hasAuth()) {
      return null;
    }
    return new UAC(config, tracingClientInterceptor);
  }

  /** For use only for testing. */
  public UAC(
      Config config,
      CollaboratorServiceGrpc.CollaboratorServiceFutureStub collaboratorServiceFutureStub,
      CollaboratorServiceGrpc.CollaboratorServiceFutureStub
          serviceAccountCollaboratorServiceFutureStub,
      UACServiceGrpc.UACServiceFutureStub uacServiceFutureStub,
      WorkspaceServiceGrpc.WorkspaceServiceFutureStub workspaceServiceFutureStub,
      AuthzServiceGrpc.AuthzServiceFutureStub authzServiceFutureStub,
      RoleServiceGrpc.RoleServiceFutureStub roleServiceFutureStub,
      RoleServiceGrpc.RoleServiceFutureStub serviceAccountRoleServiceFutureStub,
      OrganizationServiceGrpc.OrganizationServiceFutureStub organizationServiceFutureStub,
      EventServiceGrpc.EventServiceFutureStub eventServiceFutureStub,
      OrganizationServiceV2Grpc.OrganizationServiceV2FutureStub organizationServiceV2FutureStub,
      WorkspaceServiceV2Grpc.WorkspaceServiceV2FutureStub workspaceServiceV2FutureStub) {
    super(Optional.empty());
    this.config = config;
    this.collaboratorServiceFutureStub = collaboratorServiceFutureStub;
    this.serviceAccountCollaboratorServiceFutureStub = serviceAccountCollaboratorServiceFutureStub;
    this.uacServiceFutureStub = uacServiceFutureStub;
    this.workspaceServiceFutureStub = workspaceServiceFutureStub;
    this.authzServiceFutureStub = authzServiceFutureStub;
    this.roleServiceFutureStub = roleServiceFutureStub;
    this.serviceAccountRoleServiceFutureStub = serviceAccountRoleServiceFutureStub;
    this.organizationServiceFutureStub = organizationServiceFutureStub;
    this.eventServiceFutureStub = eventServiceFutureStub;
    this.organizationServiceV2FutureStub = organizationServiceV2FutureStub;
    this.workspaceServiceV2FutureStub = workspaceServiceV2FutureStub;
  }

  private UAC(Config config, Optional<ClientInterceptor> tracingClientInterceptor) {
    super(tracingClientInterceptor);
    String host = config.getAuthService().getHost();
    int port = config.getAuthService().getPort();
    this.config = config;
    LOGGER.trace(CommonMessages.HOST_PORT_INFO_STR, host, port);
    ManagedChannel authServiceChannel;
    if (host != null) {
      authServiceChannel =
          ManagedChannelBuilder.forTarget(host + CommonConstants.STRING_COLON + port)
              .usePlaintext()
              .build();
    } else {
      throw new UnavailableException(
          "Host OR Port not found for contacting authentication service");
    }

    collaboratorServiceFutureStub = CollaboratorServiceGrpc.newFutureStub(authServiceChannel);
    serviceAccountCollaboratorServiceFutureStub =
        CollaboratorServiceGrpc.newFutureStub(authServiceChannel)
            .withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(
                    getServiceUserMetadata(config.getService_user())));
    uacServiceFutureStub = UACServiceGrpc.newFutureStub(authServiceChannel);
    workspaceServiceFutureStub = WorkspaceServiceGrpc.newFutureStub(authServiceChannel);
    authzServiceFutureStub = AuthzServiceGrpc.newFutureStub(authServiceChannel);
    roleServiceFutureStub = RoleServiceGrpc.newFutureStub(authServiceChannel);
    serviceAccountRoleServiceFutureStub =
        RoleServiceGrpc.newFutureStub(authServiceChannel)
            .withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(
                    getServiceUserMetadata(config.getService_user())));
    organizationServiceFutureStub = OrganizationServiceGrpc.newFutureStub(authServiceChannel);
    eventServiceFutureStub =
        EventServiceGrpc.newFutureStub(authServiceChannel)
            .withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(
                    getServiceUserMetadata(config.getService_user())));
    organizationServiceV2FutureStub = OrganizationServiceV2Grpc.newFutureStub(authServiceChannel);
    workspaceServiceV2FutureStub = WorkspaceServiceV2Grpc.newFutureStub(authServiceChannel);
  }

  public AuthServiceChannel getBlockingAuthServiceChannel() {
    return new AuthServiceChannel(config, super.getTracingClientInterceptor());
  }

  public CollaboratorServiceGrpc.CollaboratorServiceFutureStub getCollaboratorService() {
    return attachInterceptors(collaboratorServiceFutureStub);
  }

  public CollaboratorServiceGrpc.CollaboratorServiceFutureStub
      getServiceAccountCollaboratorServiceForServiceUser() {
    return attachInterceptors(serviceAccountCollaboratorServiceFutureStub);
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
    return attachInterceptors(serviceAccountRoleServiceFutureStub);
  }

  public EventServiceGrpc.EventServiceFutureStub getEventService() {
    return attachInterceptors(eventServiceFutureStub);
  }

  public OrganizationServiceV2Grpc.OrganizationServiceV2FutureStub getOrganizationServiceV2() {
    return attachInterceptors(organizationServiceV2FutureStub);
  }

  public WorkspaceServiceV2Grpc.WorkspaceServiceV2FutureStub getWorkspaceServiceV2() {
    return attachInterceptors(workspaceServiceV2FutureStub);
  }
}
