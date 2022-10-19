package ai.verta.modeldb;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.GrpcServerConfig;
import ai.verta.modeldb.common.config.ServiceConfig;
import ai.verta.modeldb.common.config.SpringServerConfig;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.config.TestConfig;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class ModeldbTestConfigurationBeans {
  public static final Logger LOGGER = LogManager.getLogger(ModeldbTestConfigurationBeans.class);

  @Bean
  public TestConfig config() {
    return initializeTestConfig();
  }

  @Bean
  public boolean runningIsolated(TestConfig testConfig) {
    return testConfig.testsShouldRunIsolatedFromDependencies();
  }

  @Bean
  UAC uac(Config config, boolean runningIsolated) {
    return runningIsolated ? mock(UAC.class, RETURNS_DEEP_STUBS) : UAC.FromConfig(config);
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

      if (testConfig.getArtifactStoreConfig().isEnabled()) {
        System.getProperties().put("server.port", testConfig.getSpringServer().getPort());
        var artifactStoreConfig = testConfig.getArtifactStoreConfig();
        artifactStoreConfig.setHost("localhost:" + testConfig.getSpringServer().getPort());
        var nfsConfig = artifactStoreConfig.getNFS();
        String rootPath = System.getProperty("user.dir");
        nfsConfig.setNfsRootPath(rootPath);
        artifactStoreConfig.setNFS(nfsConfig);
        testConfig.setArtifactStoreConfig(artifactStoreConfig);
      }
    }
    return testConfig;
  }
}
