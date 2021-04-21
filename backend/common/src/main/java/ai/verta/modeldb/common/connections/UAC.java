package ai.verta.modeldb.common.connections;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import ai.verta.uac.AuthzServiceGrpc;
import ai.verta.uac.CollaboratorServiceGrpc;
import ai.verta.uac.UACServiceGrpc;
import ai.verta.uac.WorkspaceServiceGrpc;
import io.grpc.*;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import io.opentracing.contrib.grpc.TracingClientInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class UAC {
  private static final Logger LOGGER = LogManager.getLogger(UAC.class);

  private final ManagedChannel authServiceChannel;
  private final String serviceUserEmail;
  private final String serviceUserDevKey;

  private Optional<ClientInterceptor> clientInterceptor = Optional.empty();
  private final Optional<TracingClientInterceptor> tracingClientInterceptor;

  private final CollaboratorServiceGrpc.CollaboratorServiceFutureStub collaboratorServiceFutureStub;
  private final UACServiceGrpc.UACServiceFutureStub uacServiceFutureStub;
  private final WorkspaceServiceGrpc.WorkspaceServiceFutureStub workspaceServiceFutureStub;
  private final AuthzServiceGrpc.AuthzServiceFutureStub authzServiceFutureStub;

  public static UAC FromConfig(Config config) {
    if (!config.hasAuth()) return null;
    else return new UAC(config);
  }

  private UAC(Config config) {
    this(
        config.authService.host,
        config.authService.port,
        config.service_user.email,
        config.service_user.devKey,
        config);
  }

  public UAC(
      String host, Integer port, String serviceUserEmail, String serviceUserDevKey, Config config) {
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
    authzServiceFutureStub = AuthzServiceGrpc.newFutureStub(authServiceChannel);

    tracingClientInterceptor = config.getTracingClientInterceptor();
  }

  private UAC(UAC other) {
    authServiceChannel = other.authServiceChannel;
    serviceUserDevKey = other.serviceUserDevKey;
    serviceUserEmail = other.serviceUserEmail;

    collaboratorServiceFutureStub = other.collaboratorServiceFutureStub;
    uacServiceFutureStub = other.uacServiceFutureStub;
    workspaceServiceFutureStub = other.workspaceServiceFutureStub;
    authzServiceFutureStub = other.authzServiceFutureStub;

    tracingClientInterceptor = other.tracingClientInterceptor;
  }

  private Metadata serviceAccountMetadata() {
    return serviceAccountMetadata(serviceUserEmail, serviceUserDevKey);
  }

  private Metadata serviceAccountMetadata(String serviceUserEmail, String serviceUserDevKey) {
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

    return requestHeaders;
  }

  public Context withServiceAccountMetadata(Context ctx) {
    return ctx.withValue(AuthInterceptor.METADATA_INFO, serviceAccountMetadata());
  }

  // TODO: Get rid of this method and use Context.attach() and .detach() instead
  // because if a method is reliant on our previous context and we've changed it like this,
  // code'll break
  public void updateClientInterceptor(String serviceUserEmail, String serviceUserDevKey) {
    this.clientInterceptor =
        Optional.of(
            MetadataUtils.newAttachHeadersInterceptor(
                serviceAccountMetadata(serviceUserEmail, serviceUserDevKey)));
  }

  public UAC withServiceAccount() {
    return this.withServiceAccount(this.serviceUserEmail, this.serviceUserDevKey);
  }

  public UAC withServiceAccount(String serviceUserEmail, String serviceUserDevKey) {
    UAC c = new UAC(this);
    c.clientInterceptor =
        Optional.of(
            MetadataUtils.newAttachHeadersInterceptor(
                serviceAccountMetadata(serviceUserEmail, serviceUserDevKey)));

    return c;
  }

  private <T extends AbstractStub<T>> T attachInterceptors(io.grpc.stub.AbstractStub<T> stub) {
    if (tracingClientInterceptor.isPresent()) {
      stub = stub.withInterceptors(tracingClientInterceptor.get());
    }

    if (clientInterceptor.isPresent()) {
      stub = stub.withInterceptors(clientInterceptor.get());
    } else {
      stub =
          stub.withInterceptors(
              MetadataUtils.newAttachHeadersInterceptor(AuthInterceptor.METADATA_INFO.get()));
    }

    return (T) stub;
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
}
