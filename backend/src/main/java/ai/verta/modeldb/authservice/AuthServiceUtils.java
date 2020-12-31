package ai.verta.modeldb.authservice;

import ai.verta.modeldb.App;
import ai.verta.modeldb.config.Config;

public class AuthServiceUtils extends ai.verta.modeldb.common.authservice.AuthServiceUtils {
  public AuthServiceUtils() {
    this(App.getInstance());
  }

  private AuthServiceUtils(App app) {
    super(
        app.getAuthServerHost(),
        app.getAuthServerPort(),
        app.getServiceUserEmail(),
        app.getServiceUserDevKey(),
        Config.getInstance().grpcServer.requestTimeout,
        AuthInterceptor.METADATA_INFO);
  }
}
