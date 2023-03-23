package ai.verta.modeldb;

import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.authservice.MDBRoleServiceUtils;
import ai.verta.modeldb.common.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.common.authservice.UACApisUtil;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.futures.FutureExecutor;
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
@Setter(AccessLevel.NONE)
public class ServiceSet {
  @JsonProperty private ArtifactStoreService artifactStoreService = null;
  @JsonProperty private UACApisUtil uacApisUtil;
  @JsonProperty private UAC uac;
  @JsonProperty private MDBRoleService mdbRoleService;
  @JsonProperty private App app;

  public static ServiceSet fromConfig(
      MDBConfig mdbConfig,
      ArtifactStoreService artifactStoreService,
      UAC uac,
      FutureExecutor executor) {
    var set = new ServiceSet();
    set.uac = uac;
    set.uacApisUtil = new UACApisUtil(executor, uac);
    set.mdbRoleService = MDBRoleServiceUtils.fromConfig(mdbConfig, set.uacApisUtil, set.uac);

    // Initialize App.java singleton instance
    set.app = App.getInstance();
    set.app.mdbConfig = mdbConfig;

    set.artifactStoreService = artifactStoreService;

    return set;
  }
}
