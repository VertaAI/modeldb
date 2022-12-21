package ai.verta.modeldb.config;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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
public class MDBConfig extends Config {

  @JsonProperty private static MDBConfig mdbConfig = null;
  @JsonProperty public String starterProject;

  @JsonProperty private TelemetryConfig telemetry;
  @JsonProperty private List<MigrationConfig> migrations;
  @JsonProperty protected FutureJdbi jdbi;
  @JsonProperty private boolean enabledPermissionV2 = false;

  public static MDBConfig getInstance() throws InternalErrorException {
    if (mdbConfig == null) {
      mdbConfig = getInstance(MDBConfig.class, ModelDBConstants.VERTA_MODELDB_CONFIG);
      mdbConfig.validate();
    }
    return mdbConfig;
  }

  @Override
  public void validate() throws InvalidConfigException {
    super.validate();

    if (getService_user() != null) {
      getService_user().validate("service_user");
    }

    if (telemetry == null) telemetry = new TelemetryConfig();
    telemetry.validate("telemetry");

    if (migrations != null) {
      for (MigrationConfig migrationConfig : migrations) {
        migrationConfig.validate("migration");
      }
    }
  }

  @Override
  public boolean hasServiceAccount() {
    return getService_user() != null;
  }
}
