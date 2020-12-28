package ai.verta.modeldb.authservice;

import ai.verta.modeldb.App;

public class AuthServiceChannel extends ai.verta.modeldb.common.authservice.AuthServiceChannel {
  public AuthServiceChannel() {
    this(App.getInstance());
  }

  private AuthServiceChannel(App app) {
    super(
        app.getAuthServerHost(),
        app.getAuthServerPort(),
        app.getServiceUserEmail(),
        app.getServiceUserDevKey(),
        AuthInterceptor.METADATA_INFO);
  }
}
