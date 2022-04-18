package ai.verta.modeldb.common;

import ai.verta.modeldb.App;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.config.MDBConfig;
import io.grpc.Server;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.jmx.BuildInfoCollector;
import io.prometheus.jmx.JmxCollector;
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
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;

public abstract class CommonApp implements ApplicationContextAware {
  private static final Logger LOGGER = LogManager.getLogger(CommonApp.class);

  protected ApplicationContext applicationContext;
  private Optional<Server> server = Optional.empty();

  // Export all JMX metrics to Prometheus
  private static final String JMX_RULES = "---\n" + "rules:\n" + "  - pattern: \".*\"";
  private static final AtomicBoolean metricsInitialized = new AtomicBoolean(false);

  /**
   * Shut down the spring boot server
   *
   * @param returnCode : for system exit - 0
   */
  public static void initiateShutdown(int returnCode) {
    var app = App.getInstance();
    SpringApplication.exit(app.applicationContext, () -> returnCode);
  }

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
    var app = App.getInstance();
    app.applicationContext = applicationContext;
  }

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

  protected void registeredBean(String controllerBeanName, Class<?> className) {
    AutowireCapableBeanFactory factory =
        App.getInstance().applicationContext.getAutowireCapableBeanFactory();
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

}
