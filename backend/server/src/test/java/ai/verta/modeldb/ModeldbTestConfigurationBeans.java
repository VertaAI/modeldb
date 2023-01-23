package ai.verta.modeldb;

import static org.mockito.Mockito.*;

import ai.verta.modeldb.common.authservice.AuthServiceChannel;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.GrpcServerConfig;
import ai.verta.modeldb.common.config.ServiceConfig;
import ai.verta.modeldb.common.config.SpringServerConfig;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.uac.*;
import io.grpc.stub.AbstractStub;
import io.opentelemetry.api.OpenTelemetry;
import java.util.Optional;
import java.util.Random;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
@Log4j2
public class ModeldbTestConfigurationBeans {

  @Bean
  public TestConfig config() {
    return initializeTestConfig();
  }

  @Bean
  public boolean runningIsolated(TestConfig testConfig) {
    return testConfig.testsShouldRunIsolatedFromDependencies();
  }

  @Bean
  UAC uac(Config config, @Qualifier("runningIsolated") boolean runningIsolated) {
    return runningIsolated ? buildFullyMockedUac(config) : UAC.fromConfig(config, Optional.empty());
  }

  public static UAC buildFullyMockedUac(Config config) {
    CollaboratorServiceGrpc.CollaboratorServiceFutureStub collab =
        mock(CollaboratorServiceGrpc.CollaboratorServiceFutureStub.class);

    CollaboratorServiceGrpc.CollaboratorServiceFutureStub serviceAccountCollab =
        mock(CollaboratorServiceGrpc.CollaboratorServiceFutureStub.class);

    UACServiceGrpc.UACServiceFutureStub uacService =
        mock(UACServiceGrpc.UACServiceFutureStub.class);

    WorkspaceServiceGrpc.WorkspaceServiceFutureStub workspace =
        mock(WorkspaceServiceGrpc.WorkspaceServiceFutureStub.class);

    AuthzServiceGrpc.AuthzServiceFutureStub authZ =
        mock(AuthzServiceGrpc.AuthzServiceFutureStub.class);

    RoleServiceGrpc.RoleServiceFutureStub role = mock(RoleServiceGrpc.RoleServiceFutureStub.class);

    RoleServiceGrpc.RoleServiceFutureStub serviceAccountRole =
        mock(RoleServiceGrpc.RoleServiceFutureStub.class);

    OrganizationServiceGrpc.OrganizationServiceFutureStub org =
        mock(OrganizationServiceGrpc.OrganizationServiceFutureStub.class);

    EventServiceGrpc.EventServiceFutureStub event =
        mock(EventServiceGrpc.EventServiceFutureStub.class);

    AuthServiceChannel authServiceChannel = new TestAuthServiceChannel(config);
    return new UAC(
        config,
        collab,
        serviceAccountCollab,
        uacService,
        workspace,
        authZ,
        role,
        serviceAccountRole,
        org,
        event) {
      @Override
      public AuthServiceChannel getBlockingAuthServiceChannel() {
        return authServiceChannel;
      }

      @Override
      protected <T extends AbstractStub<T>> T attachInterceptors(AbstractStub<T> stub) {
        return (T) stub;
      }
    };
  }

  public static void resetUacMocks(UAC uac) {
    reset(uac.getBlockingAuthServiceChannel().getCollaboratorServiceBlockingStub());
    reset(uac.getBlockingAuthServiceChannel().getCollaboratorServiceBlockingStubForServiceUser());
    reset(uac.getBlockingAuthServiceChannel().getUacServiceBlockingStub());
    reset(uac.getBlockingAuthServiceChannel().getUacServiceBlockingStubForServiceUser());
    reset(uac.getBlockingAuthServiceChannel().getWorkspaceServiceBlockingStub());
    reset(uac.getBlockingAuthServiceChannel().getRoleServiceBlockingStub());
    reset(uac.getBlockingAuthServiceChannel().getRoleServiceBlockingStubForServiceUser());
    reset(uac.getBlockingAuthServiceChannel().getOrganizationServiceBlockingStub());
    reset(uac.getBlockingAuthServiceChannel().getAuthzServiceBlockingStub());
    reset(uac.getUACService());
    reset(uac.getCollaboratorService());
    reset(uac.getEventService());
    reset(uac.getServiceAccountRoleServiceFutureStub());
    reset(uac.getRoleService());
    reset(uac.getAuthzService());
    reset(uac.getWorkspaceService());
    reset(uac.getServiceAccountCollaboratorServiceForServiceUser());
  }

  @Bean
  OpenTelemetry openTelemetry() {
    return OpenTelemetry.noop();
  }

  private static TestConfig initializeTestConfig() {
    var testConfig = TestConfig.getInstance();

    if (testConfig.testsShouldRunIsolatedFromDependencies()) {
      final int randomGrpcPort = new Random().nextInt(10000) + 1024;
      final int randomWebPort = randomGrpcPort + 1;
      final int useRandomPortsPort = 99999;
      SpringServerConfig springServerConfig = testConfig.getSpringServer();
      if (springServerConfig.getPort() == useRandomPortsPort) {
        springServerConfig.setPort(randomWebPort);
        testConfig.setSpringServer(springServerConfig);
      }

      GrpcServerConfig grpcServerConfig = testConfig.getGrpcServer();
      if (testConfig.getGrpcServer().getPort() == useRandomPortsPort) {
        grpcServerConfig.setPort(randomGrpcPort);
        grpcServerConfig.setMetrics_port(randomGrpcPort + 1);
        testConfig.setGrpcServer(grpcServerConfig);
      }

      ServiceConfig serviceConfig = testConfig.getAuthService();
      if (serviceConfig.getPort() == useRandomPortsPort) {
        serviceConfig.setPort(randomGrpcPort);
        testConfig.setAuthService(serviceConfig);
      }

      var artifactStoreConfig = testConfig.getArtifactStoreConfig();
      if (artifactStoreConfig.isEnabled()) {
        artifactStoreConfig.setHost("localhost:" + testConfig.getSpringServer().getPort());
        if (testConfig.getArtifactStoreConfig().getArtifactStoreType().equalsIgnoreCase("NFS")) {
          var nfsConfig = artifactStoreConfig.getNFS();
          String rootPath = System.getProperty("user.dir");
          nfsConfig.setNfsRootPath(rootPath);
          artifactStoreConfig.setNFS(nfsConfig);
        }
        testConfig.setArtifactStoreConfig(artifactStoreConfig);
      }
    }
    System.getProperties().put("server.port", testConfig.getSpringServer().getPort());
    return testConfig;
  }

  private static class TestAuthServiceChannel extends AuthServiceChannel {

    private final RoleServiceGrpc.RoleServiceBlockingStub roleServiceBlockingStub =
        mock(RoleServiceGrpc.RoleServiceBlockingStub.class);
    private final AuthzServiceGrpc.AuthzServiceBlockingStub authzServiceBlockingStub =
        mock(AuthzServiceGrpc.AuthzServiceBlockingStub.class);
    private final CollaboratorServiceGrpc.CollaboratorServiceBlockingStub
        collaboratorServiceBlockingStub =
            mock(CollaboratorServiceGrpc.CollaboratorServiceBlockingStub.class);
    private final WorkspaceServiceGrpc.WorkspaceServiceBlockingStub workspaceServiceBlockingStub =
        mock(WorkspaceServiceGrpc.WorkspaceServiceBlockingStub.class);
    private final OrganizationServiceGrpc.OrganizationServiceBlockingStub
        organizationServiceBlockingStub =
            mock(OrganizationServiceGrpc.OrganizationServiceBlockingStub.class);
    private final OrganizationServiceGrpc.OrganizationServiceBlockingStub
        organizationServiceBlockingStubForServiceUser =
            mock(OrganizationServiceGrpc.OrganizationServiceBlockingStub.class);
    private final UACServiceGrpc.UACServiceBlockingStub uacServiceBlockingStub =
        mock(UACServiceGrpc.UACServiceBlockingStub.class);
    private final UACServiceGrpc.UACServiceBlockingStub uacServiceBlockingStubForServiceUser =
        mock(UACServiceGrpc.UACServiceBlockingStub.class);

    public TestAuthServiceChannel(Config config) {
      super(config, Optional.empty());
    }

    @Override
    public UACServiceGrpc.UACServiceBlockingStub getUacServiceBlockingStub() {
      return uacServiceBlockingStub;
    }

    @Override
    public RoleServiceGrpc.RoleServiceBlockingStub getRoleServiceBlockingStub() {
      return roleServiceBlockingStub;
    }

    @Override
    public AuthzServiceGrpc.AuthzServiceBlockingStub getAuthzServiceBlockingStub() {
      return authzServiceBlockingStub;
    }

    @Override
    public CollaboratorServiceGrpc.CollaboratorServiceBlockingStub
        getCollaboratorServiceBlockingStub() {
      return collaboratorServiceBlockingStub;
    }

    @Override
    public WorkspaceServiceGrpc.WorkspaceServiceBlockingStub getWorkspaceServiceBlockingStub() {
      return workspaceServiceBlockingStub;
    }

    @Override
    public OrganizationServiceGrpc.OrganizationServiceBlockingStub
        getOrganizationServiceBlockingStub() {
      return organizationServiceBlockingStub;
    }

    @Override
    public CollaboratorServiceGrpc.CollaboratorServiceBlockingStub
        getCollaboratorServiceBlockingStubForServiceUser() {
      return collaboratorServiceBlockingStub;
    }

    @Override
    public OrganizationServiceGrpc.OrganizationServiceBlockingStub
        getOrganizationServiceBlockingStubForServiceUser() {
      return organizationServiceBlockingStubForServiceUser;
    }

    @Override
    public RoleServiceGrpc.RoleServiceBlockingStub getRoleServiceBlockingStubForServiceUser() {
      return roleServiceBlockingStub;
    }

    @Override
    public UACServiceGrpc.UACServiceBlockingStub getUacServiceBlockingStubForServiceUser() {
      return uacServiceBlockingStubForServiceUser;
    }
  }
}
