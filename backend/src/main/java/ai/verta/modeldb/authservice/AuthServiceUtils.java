package ai.verta.modeldb.authservice;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.connections.UAC;

public class AuthServiceUtils extends ai.verta.modeldb.common.authservice.AuthServiceUtils {
  public static ai.verta.modeldb.common.authservice.AuthService FromConfig(Config config, UAC uac) {
    if (!config.hasAuth()) return new PublicAuthServiceUtils();
    else return new AuthServiceUtils(config, uac);
  }

  private AuthServiceUtils(Config config, UAC uac) {
    super(uac, config.grpcServer.requestTimeout);
  }
}
