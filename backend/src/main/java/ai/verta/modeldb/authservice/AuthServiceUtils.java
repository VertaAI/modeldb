package ai.verta.modeldb.authservice;

import ai.verta.modeldb.App;
import ai.verta.modeldb.config.Config;

public class AuthServiceUtils extends ai.verta.modeldb.common.authservice.AuthServiceUtils {
  public static ai.verta.modeldb.common.authservice.AuthService FromConfig(Config config) {
    if (!config.hasAuth()) return new PublicAuthServiceUtils();
    else return new AuthServiceUtils();
  }

  public AuthServiceUtils() {
    this(App.getInstance());
  }

  private AuthServiceUtils(App app) {
    super(
        Config.getInstance().authService.host,
        Config.getInstance().authService.port,
        app.getServiceUserEmail(),
        app.getServiceUserDevKey(),
        Config.getInstance().grpcServer.requestTimeout,
        AuthInterceptor.METADATA_INFO);
  }
}
