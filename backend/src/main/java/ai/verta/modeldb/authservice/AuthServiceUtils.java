package ai.verta.modeldb.authservice;

import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.config.Config;

public class AuthServiceUtils extends ai.verta.modeldb.common.authservice.AuthServiceUtils {
  public static ai.verta.modeldb.common.authservice.AuthService FromConfig(Config config) {
    if (!config.hasAuth()) return new PublicAuthServiceUtils();
    else return new AuthServiceUtils();
  }

  public AuthServiceUtils() {
    this(Config.getInstance());
  }

  private AuthServiceUtils(Config config) {
    super(
        config,
        config.authService.host,
        config.authService.port,
        config.service_user.email,
        config.service_user.devKey,
        config.grpcServer.requestTimeout,
        AuthInterceptor.METADATA_INFO);
  }
}
