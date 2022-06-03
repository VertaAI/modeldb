package ai.verta.modeldb.common.configuration;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class AppContext implements ApplicationContextAware {

  private static AppContext appContext;
  public ApplicationContext applicationContext;

  public static AppContext getInstance() {
    if (appContext == null) {
      appContext = new AppContext();
    }
    return appContext;
  }

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Shut down the spring boot server
   *
   * @param returnCode : for system exit - 0
   */
  public void initiateShutdown(int returnCode) {
    SpringApplication.exit(this.applicationContext, () -> returnCode);
  }

  public void registeredBean(String controllerBeanName, Class<?> className) {
    AutowireCapableBeanFactory factory = this.applicationContext.getAutowireCapableBeanFactory();
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
