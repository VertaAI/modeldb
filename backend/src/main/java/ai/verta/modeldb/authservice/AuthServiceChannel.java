package ai.verta.modeldb.authservice;

import ai.verta.modeldb.App;
import ai.verta.modeldb.config.Config;

public class AuthServiceChannel extends ai.verta.modeldb.common.authservice.AuthServiceChannel {
  public AuthServiceChannel() {
    this(App.getInstance());
  }

  private AuthServiceChannel(App app) {
    super(
        Config.getInstance().authService.host,
        Config.getInstance().authService.port,
        app.getServiceUserEmail(),
        app.getServiceUserDevKey(),
        AuthInterceptor.METADATA_INFO);
  }
}
