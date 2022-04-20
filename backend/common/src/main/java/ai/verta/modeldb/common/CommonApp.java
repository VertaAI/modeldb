package ai.verta.modeldb.common;

import ai.verta.modeldb.App;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.futures.FutureGrpc;
import io.grpc.Server;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.jmx.BuildInfoCollector;
import io.prometheus.jmx.JmxCollector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MalformedObjectNameException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
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

  public static void main(String[] args) throws Exception {
    var path = System.getProperty(CommonConstants.USER_DIR) + "/" + CommonConstants.BACKEND_PID;
    resolvePortCollisionIfExists(path);
    SpringApplication application = new SpringApplication(App.class);
    application.addListeners(new ApplicationPidFileWriter(path));
    application.run(args);
  }

  private static void resolvePortCollisionIfExists(String path) throws Exception {
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
}
