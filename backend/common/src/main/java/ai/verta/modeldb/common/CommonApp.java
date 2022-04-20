package ai.verta.modeldb.common;

import ai.verta.modeldb.common.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.common.artifactStore.storageservice.NoopArtifactStoreService;
import ai.verta.modeldb.common.artifactStore.storageservice.nfs.FileStorageProperties;
import ai.verta.modeldb.common.artifactStore.storageservice.nfs.NFSController;
import ai.verta.modeldb.common.artifactStore.storageservice.nfs.NFSService;
import ai.verta.modeldb.common.artifactStore.storageservice.s3.S3Controller;
import ai.verta.modeldb.common.artifactStore.storageservice.s3.S3Service;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import io.grpc.Server;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.jmx.BuildInfoCollector;
import io.prometheus.jmx.JmxCollector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MalformedObjectNameException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;

public abstract class CommonApp implements ApplicationContextAware {
  private static final Logger LOGGER = LogManager.getLogger(CommonApp.class);

  protected ApplicationContext applicationContext;
  protected Optional<Server> server = Optional.empty();

  // Export all JMX metrics to Prometheus
  private static final String JMX_RULES = "---\n" + "rules:\n" + "  - pattern: \".*\"";
  private static final AtomicBoolean metricsInitialized = new AtomicBoolean(false);

  @Bean
  public ServletRegistrationBean<MetricsServlet> servletRegistrationBean()
      throws MalformedObjectNameException {
    if (!metricsInitialized.getAndSet(true)) {
      DefaultExports.initialize();
      new BuildInfoCollector().register();
      new JmxCollector(JMX_RULES).register();
    }
    return new ServletRegistrationBean<>(new MetricsServlet(), "/metrics");
  }

  @Bean
  public GracefulShutdown gracefulShutdown(Config config) {
    if (config == null || config.getSpringServer().getShutdownTimeout() == null) {
      return new GracefulShutdown(30L);
    }
    return new GracefulShutdown(config.getSpringServer().getShutdownTimeout());
  }

  @Bean
  public ServletWebServerFactory servletContainer(final GracefulShutdown gracefulShutdown) {
    var factory = new TomcatServletWebServerFactory();
    factory.addConnectorCustomizers(gracefulShutdown);
    return factory;
  }

  @Bean
  Executor executor(Config config) {
    // Initialize executor so we don't lose context using Futures
    return FutureGrpc.initializeExecutor(config.getGrpcServer().getThreadCount());
  }

  protected void registeredBean(
      ApplicationContext applicationContext, String controllerBeanName, Class<?> className) {
    AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;

    // Remove nfsController bean if exists
    if (registry.containsBeanDefinition(controllerBeanName)) {
      registry.removeBeanDefinition(controllerBeanName);
    }
    // create nfsController bean based on condition
    GenericBeanDefinition gbd = new GenericBeanDefinition();
    gbd.setBeanClass(className);
    registry.registerBeanDefinition(controllerBeanName, gbd);
  }

  protected static void resolvePortCollisionIfExists(String path) throws Exception {
    File pidFile = new File(path);
    if (pidFile.exists()) {
      try (BufferedReader reader = new BufferedReader(new FileReader(pidFile))) {
        String pidString = reader.readLine();
        var pid = Long.parseLong(pidString);
        var process = ProcessHandle.of(pid);
        if (process.isPresent()) {
          var processHandle = process.get();
          LOGGER.warn("Port is already used by backend PID: {}", pid);
          boolean destroyed = processHandle.destroy();
          LOGGER.warn("Process kill completed `{}` for PID: {}", destroyed, pid);
          processHandle = processHandle.onExit().get();
          LOGGER.warn("Process is alive after kill: `{}`", processHandle.isAlive());
        }
      }
    }
  }

  protected void cleanUpPIDFile() {
    var path = System.getProperty(CommonConstants.USER_DIR) + "/" + CommonConstants.BACKEND_PID;
    File pidFile = new File(path);
    if (pidFile.exists()) {
      pidFile.deleteOnExit();
      LOGGER.trace(CommonConstants.BACKEND_PID + " file is deleted: {}", pidFile.exists());
    }
  }

  @Bean
  public ArtifactStoreService artifactStoreService(
      ApplicationContext applicationContext, final Config config) throws IOException {

    final var artifactStoreConfig = config.artifactStoreConfig;
    if (artifactStoreConfig.isEnabled()) {
      //
      // TODO: This is backwards, these values can be extracted from the environment or injected
      // using profiles instead
      // ------------- Start Initialize Cloud storage base on configuration ------------------
      ArtifactStoreService artifactStoreService;

      if (artifactStoreConfig.getArtifactEndpoint() != null) {
        System.getProperties()
            .put(
                "artifactEndpoint.storeArtifact",
                artifactStoreConfig.getArtifactEndpoint().getStoreArtifact());
        System.getProperties()
            .put(
                "artifactEndpoint.getArtifact",
                artifactStoreConfig.getArtifactEndpoint().getGetArtifact());
      }

      if (artifactStoreConfig.getArtifactStoreType().equals("NFS")
          && artifactStoreConfig.getNFS() != null
          && artifactStoreConfig.getNFS().getArtifactEndpoint() != null) {
        System.getProperties()
            .put(
                "artifactEndpoint.storeArtifact",
                artifactStoreConfig.getNFS().getArtifactEndpoint().getStoreArtifact());
        System.getProperties()
            .put(
                "artifactEndpoint.getArtifact",
                artifactStoreConfig.getNFS().getArtifactEndpoint().getGetArtifact());
      }

      switch (artifactStoreConfig.getArtifactStoreType()) {
        case "S3":
          if (!artifactStoreConfig.getS3().getS3presignedURLEnabled()) {
            registeredBean(applicationContext, "s3Controller", S3Controller.class);
          }
          artifactStoreService =
              new S3Service(artifactStoreConfig, artifactStoreConfig.getS3().getCloudBucketName());
          break;
        case "NFS":
          registeredBean(applicationContext, "nfsController", NFSController.class);
          String rootDir = artifactStoreConfig.getNFS().getNfsRootPath();
          LOGGER.trace("NFS server root path {}", rootDir);
          final var props = new FileStorageProperties(rootDir);
          artifactStoreService = new NFSService(props, artifactStoreConfig);
          break;
        default:
          throw new ModelDBException("Configure valid artifact store name in config.yaml file.");
      }
      // ------------- Finish Initialize Cloud storage base on configuration ------------------

      LOGGER.info(
          "ArtifactStore service initialized and resolved storage dependency before server start");
      return artifactStoreService;
    } else {
      LOGGER.info("Artifact store service is disabled.");
      return new NoopArtifactStoreService();
    }
  }
}
