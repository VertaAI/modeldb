package ai.verta.modeldb.authservice;

import ai.verta.modeldb.App;

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
        app.getRequestTimeout(),
        AuthInterceptor.METADATA_INFO);
  }
}
