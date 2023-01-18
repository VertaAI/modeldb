package ai.verta.modeldb.authservice;

import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.authservice.AuthServiceUtils;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.connections.UAC;

public class MDBAuthServiceUtils extends AuthServiceUtils {
  public static AuthService FromConfig(Config config, UAC uac) {
    if (!config.hasAuth()) return new PublicAuthServiceUtils();
    else return new MDBAuthServiceUtils(config, uac);
  }

  private MDBAuthServiceUtils(Config config, UAC uac) {
    super(uac, config.getGrpcServer().getRequestTimeout(), config.isPermissionV2Enabled());
  }
}
