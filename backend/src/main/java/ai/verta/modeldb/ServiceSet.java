package ai.verta.modeldb;

import ai.verta.modeldb.authservice.MDBAuthServiceUtils;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.authservice.MDBRoleServiceUtils;
import ai.verta.modeldb.common.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.config.MDBConfig;
import java.io.IOException;

public class ServiceSet {
  public ArtifactStoreService artifactStoreService = null;
  public AuthService authService;
  public UAC uac;
  public MDBRoleService mdbRoleService;
  public App app;

  public static ServiceSet fromConfig(
      MDBConfig mdbConfig, ArtifactStoreService artifactStoreService) throws IOException {
    var set = new ServiceSet();
    set.uac = UAC.FromConfig(mdbConfig);
    set.authService = MDBAuthServiceUtils.FromConfig(mdbConfig, set.uac);
    set.mdbRoleService = MDBRoleServiceUtils.FromConfig(mdbConfig, set.authService, set.uac);

    // Initialize App.java singleton instance
    set.app = App.getInstance();
    set.app.mdbConfig = mdbConfig;

    set.artifactStoreService = artifactStoreService;

    return set;
  }

  private ServiceSet() {}
}
