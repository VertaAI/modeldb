package ai.verta.modeldb.authservice;

import ai.verta.modeldb.App;
import ai.verta.modeldb.config.Config;

public class AuthServiceChannel extends ai.verta.modeldb.common.authservice.AuthServiceChannel {
  public AuthServiceChannel() {
    this(Config.getInstance());
  }

  private AuthServiceChannel(Config config) {
    super(
            config.authService.host,
            config.authService.port,
            config.mdb_service_user.email,
            config.mdb_service_user.devKey,
        AuthInterceptor.METADATA_INFO);
  }
}
