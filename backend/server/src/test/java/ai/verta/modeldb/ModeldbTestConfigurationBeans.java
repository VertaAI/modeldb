package ai.verta.modeldb;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.verta.modeldb.common.authservice.AuthServiceChannel;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.GrpcServerConfig;
import ai.verta.modeldb.common.config.ServiceConfig;
import ai.verta.modeldb.common.config.SpringServerConfig;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.config.TestConfig;
import ai.verta.uac.*;
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

  private static UAC buildFullyMockedUac(Config config) {
    CollaboratorServiceGrpc.CollaboratorServiceFutureStub collab =
        mock(CollaboratorServiceGrpc.CollaboratorServiceFutureStub.class);
    when(collab.withInterceptors(any())).thenReturn(collab);
    CollaboratorServiceGrpc.CollaboratorServiceFutureStub serviceAccountCollab =
        mock(CollaboratorServiceGrpc.CollaboratorServiceFutureStub.class);
    when(serviceAccountCollab.withInterceptors(any())).thenReturn(serviceAccountCollab);
    UACServiceGrpc.UACServiceFutureStub uacService =
        mock(UACServiceGrpc.UACServiceFutureStub.class);
    when(uacService.withInterceptors(any())).thenReturn(uacService);
    WorkspaceServiceGrpc.WorkspaceServiceFutureStub workspace =
        mock(WorkspaceServiceGrpc.WorkspaceServiceFutureStub.class);
    when(workspace.withInterceptors(any())).thenReturn(workspace);
    AuthzServiceGrpc.AuthzServiceFutureStub authZ =
        mock(AuthzServiceGrpc.AuthzServiceFutureStub.class);
    when(authZ.withInterceptors(any())).thenReturn(authZ);

    RoleServiceGrpc.RoleServiceFutureStub role = mock(RoleServiceGrpc.RoleServiceFutureStub.class);
    when(role.withInterceptors(any())).thenReturn(role);
    RoleServiceGrpc.RoleServiceFutureStub serviceAccountRole =
        mock(RoleServiceGrpc.RoleServiceFutureStub.class);
    when(serviceAccountRole.withInterceptors(any())).thenReturn(serviceAccountRole);
    OrganizationServiceGrpc.OrganizationServiceFutureStub org =
        mock(OrganizationServiceGrpc.OrganizationServiceFutureStub.class);
    when(org.withInterceptors(any())).thenReturn(org);
    EventServiceGrpc.EventServiceFutureStub event =
        mock(EventServiceGrpc.EventServiceFutureStub.class);
    when(event.withInterceptors(any())).thenReturn(event);
    AuthServiceChannel authServiceChannel = mock(AuthServiceChannel.class);
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
    };
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
}
