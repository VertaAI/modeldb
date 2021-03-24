package ai.verta.modeldb.common.connections;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.UACServiceGrpc;
import ai.verta.uac.WorkspaceServiceGrpc;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UAC {
  private static final Logger LOGGER = LogManager.getLogger(UAC.class);

  private final ManagedChannel authServiceChannel;
  private final String serviceUserEmail;
  private final String serviceUserDevKey;

  private ClientInterceptor clientInterceptor = null;

  private final CollaboratorServiceGrpc.CollaboratorServiceFutureStub collaboratorServiceFutureStub;
  private final UACServiceGrpc.UACServiceFutureStub uacServiceFutureStub;
  private final WorkspaceServiceGrpc.WorkspaceServiceFutureStub workspaceServiceFutureStub;

  public static UAC FromConfig(Config config) {
    if (!config.hasAuth()) return null;
    else return new UAC(config);
  }

  private UAC(Config config) {
    this(
        config.authService.host,
        config.authService.port,
        config.service_user.email,
        config.service_user.devKey);
  }

  public UAC(String host, Integer port, String serviceUserEmail, String serviceUserDevKey) {
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

    collaboratorServiceFutureStub = CollaboratorServiceGrpc.newFutureStub(authServiceChannel);
    uacServiceFutureStub = UACServiceGrpc.newFutureStub(authServiceChannel);
    workspaceServiceFutureStub = WorkspaceServiceGrpc.newFutureStub(authServiceChannel);
  }

  private UAC(UAC other) {
    authServiceChannel = other.authServiceChannel;
    serviceUserDevKey = other.serviceUserDevKey;
    serviceUserEmail = other.serviceUserEmail;

    collaboratorServiceFutureStub = other.collaboratorServiceFutureStub;
    uacServiceFutureStub = other.uacServiceFutureStub;
    workspaceServiceFutureStub = other.workspaceServiceFutureStub;
  }

  public UAC withServiceAccount() {
    return this.withServiceAccount(this.serviceUserEmail, this.serviceUserDevKey);
  }

  public UAC withServiceAccount(String serviceUserEmail, String serviceUserDevKey) {
    UAC c = new UAC(this);
    Metadata requestHeaders = new Metadata();
    Metadata.Key<String> email_key = Metadata.Key.of("email", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> dev_key =
        Metadata.Key.of("developer_key", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> dev_key_hyphen =
        Metadata.Key.of("developer-key", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> source_key = Metadata.Key.of("source", Metadata.ASCII_STRING_MARSHALLER);

    requestHeaders.put(email_key, serviceUserEmail);
    requestHeaders.put(dev_key, serviceUserDevKey);
    requestHeaders.put(dev_key_hyphen, serviceUserDevKey);
    requestHeaders.put(source_key, "PythonClient");

    c.clientInterceptor = MetadataUtils.newAttachHeadersInterceptor(requestHeaders);

    return c;
  }

  public CollaboratorServiceGrpc.CollaboratorServiceFutureStub getCollaboratorService() {
    if (clientInterceptor != null) {
      return collaboratorServiceFutureStub.withInterceptors(clientInterceptor);
    }
    return collaboratorServiceFutureStub.withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(AuthInterceptor.METADATA_INFO.get()));
  }

  public UACServiceGrpc.UACServiceFutureStub getUACService() {
    if (clientInterceptor != null) {
      return uacServiceFutureStub.withInterceptors(clientInterceptor);
    }
    return uacServiceFutureStub.withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(AuthInterceptor.METADATA_INFO.get()));
  }

  public WorkspaceServiceGrpc.WorkspaceServiceFutureStub getWorkspaceService() {
    if (clientInterceptor != null) {
      return workspaceServiceFutureStub.withInterceptors(clientInterceptor);
    }
    return workspaceServiceFutureStub.withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(AuthInterceptor.METADATA_INFO.get()));
  }
}
