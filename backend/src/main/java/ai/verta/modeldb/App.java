package ai.verta.modeldb;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.config.MDBConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/** This class is entry point of modeldb server. */
@SpringBootApplication
@ComponentScan({
  "ai.verta.modeldb.config",
  "ai.verta.modeldb.health",
  "ai.verta.modeldb.configuration",
  "ai.verta.modeldb.common.configuration"
})
public class App {

  private static final Logger LOGGER = LogManager.getLogger(App.class);
  private static App app = null;
  public MDBConfig mdbConfig;
  private static ConfigurableApplicationContext applicationContext;

  public static App initialize(MDBConfig config) {
    app = new App();
    app.mdbConfig = config;
    return app;
  }

  public static App getInstance() {
    if (app == null) {
      app = new App();
    }
    return app;
  }

  public static void main(String[] args) throws Exception {
    initializeSystemProperties();
    var pathToPidFile =
        System.getProperty(CommonConstants.USER_DIR) + "/" + CommonConstants.BACKEND_PID_FILENAME;
    CommonUtils.resolvePortCollisionIfExists(pathToPidFile);
    SpringApplication application = new SpringApplication(App.class);
    application.addListeners(new ApplicationPidFileWriter(pathToPidFile));
    applicationContext = application.run(args);
  }

  public ConfigurableApplicationContext getApplicationContext() {
    return applicationContext;
  }

  private static void initializeSystemProperties() {
    var isTestEnvironment = System.getProperties().containsKey("modeldb.test");
    MDBConfig config;
    if (isTestEnvironment) {
      config = App.getInstance().getMdbConfig();
    } else {
      config = MDBConfig.getInstance();
      App.initialize(config);
    }
    // Configure spring HTTP server
    initializeSystemProperties(config);
  }

  private static void initializeSystemProperties(MDBConfig config) {
    // Configure spring HTTP server
    var webPort = config.getSpringServer().getPort();
    if (webPort == 0) {
      var grpcServerConfig = config.getGrpcServer();
      var grpcServerPort = grpcServerConfig.getPort();
      webPort = grpcServerPort + 1;
    }
    System.getProperties().put("server.port", webPort);
    LOGGER.info("Configuring spring HTTP traffic on port: {}", webPort);
  }

  public MDBConfig getMdbConfig() {
    return mdbConfig;
  }
}
