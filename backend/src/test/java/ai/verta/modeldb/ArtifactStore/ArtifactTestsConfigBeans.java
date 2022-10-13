package ai.verta.modeldb.ArtifactStore;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.GrpcServerConfig;
import ai.verta.modeldb.common.config.SpringServerConfig;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.config.TestConfig;
import java.util.Random;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class ArtifactTestsConfigBeans {

  @Bean
  public TestConfig config() {
    return initializeTestConfig();
  }

  private TestConfig initializeTestConfig() {
    var config =
        TestConfig.getInstance(
            "src/test/java/ai/verta/modeldb/ArtifactStore/nfs-config-test-h2.yaml");

    final int randomGrpcPort = new Random().nextInt(10000) + 1024;
    final int randomWebPort = randomGrpcPort + 1;
    final int useRandomPortsPort = 99999;

    SpringServerConfig springServerConfig = config.getSpringServer();
    if (springServerConfig.getPort() == useRandomPortsPort) {
      springServerConfig.setPort(randomWebPort);
      config.setSpringServer(springServerConfig);
    }

    GrpcServerConfig grpcServerConfig = config.getGrpcServer();
    if (config.getGrpcServer().getPort() == useRandomPortsPort) {
      grpcServerConfig.setPort(randomGrpcPort);
      config.setGrpcServer(grpcServerConfig);
    }

    System.getProperties().put("server.port", config.getSpringServer().getPort());
    var artifactStoreConfig = config.getArtifactStoreConfig();
    artifactStoreConfig.setHost("localhost:" + config.getSpringServer().getPort());
    var nfsConfig = artifactStoreConfig.getNFS();
    String rootPath = System.getProperty("user.dir");
    nfsConfig.setNfsRootPath(rootPath);
    artifactStoreConfig.setNFS(nfsConfig);
    config.setArtifactStoreConfig(artifactStoreConfig);
    return config;
  }

  @Bean
  UAC uac(Config config) {
    return mock(UAC.class, RETURNS_DEEP_STUBS);
  }
}
