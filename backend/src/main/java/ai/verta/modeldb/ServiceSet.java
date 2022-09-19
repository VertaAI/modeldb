package ai.verta.modeldb;

import ai.verta.modeldb.authservice.MDBAuthServiceUtils;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.authservice.MDBRoleServiceUtils;
import ai.verta.modeldb.common.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.config.MDBConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter(AccessLevel.PRIVATE)
public class ServiceSet {
  @JsonProperty private ArtifactStoreService artifactStoreService = null;
  @JsonProperty private AuthService authService;
  @JsonProperty private UAC uac;
  @JsonProperty private MDBRoleService mdbRoleService;
  @JsonProperty private App app;

  public static ServiceSet fromConfig(
      MDBConfig mdbConfig, ArtifactStoreService artifactStoreService) {
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
}
