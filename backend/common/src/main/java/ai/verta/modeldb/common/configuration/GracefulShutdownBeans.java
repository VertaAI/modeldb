package ai.verta.modeldb.common.configuration;

import ai.verta.modeldb.common.GracefulShutdown;
import ai.verta.modeldb.common.config.SpringServerConfig;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GracefulShutdownBeans {

  @Bean
  @Conditional(ServerEnabled.class)
  public GracefulShutdown gracefulShutdown(SpringServerConfig springServerConfig) {
    if (springServerConfig.getShutdownTimeout() == null) {
      return new GracefulShutdown(30L);
    }
    return new GracefulShutdown(springServerConfig.getShutdownTimeout());
  }

  @Bean
  @Conditional(ServerEnabled.class)
  public ServletWebServerFactory servletContainer(final GracefulShutdown gracefulShutdown) {
    var factory = new TomcatServletWebServerFactory();
    factory.addConnectorCustomizers(gracefulShutdown);
    return factory;
  }
}
