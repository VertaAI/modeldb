package ai.verta.modeldb;

import ai.verta.modeldb.common.configuration.ServerEnabled;
import ai.verta.modeldb.config.MDBConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

/** This class is entry point of modeldb server. */
@SpringBootApplication
@ComponentScan({
  "ai.verta.modeldb.config",
  "ai.verta.modeldb.health",
  "ai.verta.modeldb.configuration",
  "ai.verta.modeldb.common.configuration",
  "ai.verta.modeldb.common.config"
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

  public static void main(String[] args) {
    SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(App.class);
    if (ServerEnabled.serverIsEnabled()) {
      springApplicationBuilder.web(WebApplicationType.SERVLET);
    } else {
      springApplicationBuilder.web(WebApplicationType.NONE);
    }
    SpringApplication application = springApplicationBuilder.build();
    application.run(args);
  }
}
