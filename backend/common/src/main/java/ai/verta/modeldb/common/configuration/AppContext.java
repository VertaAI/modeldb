package ai.verta.modeldb.common.configuration;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.SpringServerConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
@Log4j2
public class AppContext implements ApplicationContextAware {

  private static final AppContext appContext = new AppContext();
  private ApplicationContext applicationContext;

  public static AppContext getInstance() {
    return appContext;
  }

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Bean
  @Conditional(ServerEnabled.class)
  public TomcatConnectorCustomizer forceConfigToLoadBeforeTomcat(Config config) {
    // when we load our configuration, we set system properties that need to be in place
    // before tomcat starts up. By having the config as a parameter to this method, we force spring
    // to load the config before it sets up tomcat.
    SpringServerConfig springServerConfig = config.getSpringServer();
    log.info("Starting the spring server with config: " + springServerConfig);
    return connector -> {};
  }

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  /**
   * Shut down the spring boot server
   *
   * @param returnCode : for system exit - 0
   */
  public void initiateShutdown(int returnCode) {
    SpringApplication.exit(this.applicationContext, () -> returnCode);
  }

  public void registerBean(String controllerBeanName, Class<?> className) {
    AutowireCapableBeanFactory factory = this.applicationContext.getAutowireCapableBeanFactory();
    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;

    // Remove bean if it is already exists in registry before creating new one.
    if (registry.containsBeanDefinition(controllerBeanName)) {
      registry.removeBeanDefinition(controllerBeanName);
    }
    // Create bean as per given function arguments
    GenericBeanDefinition gbd = new GenericBeanDefinition();
    gbd.setBeanClass(className);
    registry.registerBeanDefinition(controllerBeanName, gbd);
  }
}
