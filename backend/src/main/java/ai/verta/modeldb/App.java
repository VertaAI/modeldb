package ai.verta.modeldb;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.config.MDBConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
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

  private static App app = null;
  public MDBConfig mdbConfig;

  public static App getInstance() {
    if (app == null) {
      app = new App();
    }
    return app;
  }

  public static void main(String[] args) throws Exception {
    var pathToPidFile =
        System.getProperty(CommonConstants.USER_DIR) + "/" + CommonConstants.BACKEND_PID_FILENAME;
    CommonUtils.resolvePortCollisionIfExists(pathToPidFile);
    SpringApplication application = new SpringApplication(App.class);
    application.addListeners(new ApplicationPidFileWriter(pathToPidFile));
    application.run(args);
  }
}
