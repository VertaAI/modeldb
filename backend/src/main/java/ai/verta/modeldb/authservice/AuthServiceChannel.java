package ai.verta.modeldb.authservice;

import ai.verta.modeldb.common.authservice.AuthInterceptor;
import ai.verta.modeldb.config.Config;

public class AuthServiceChannel extends ai.verta.modeldb.common.authservice.AuthServiceChannel {
  public AuthServiceChannel() {
    this(Config.getInstance());
  }

  private AuthServiceChannel(Config config) {
    super(
        config.authService.host,
        config.authService.port,
        config.service_user.email,
        config.service_user.devKey,
        AuthInterceptor.METADATA_INFO);
  }
}
