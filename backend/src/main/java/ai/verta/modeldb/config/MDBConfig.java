package ai.verta.modeldb.config;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import java.util.List;

public class MDBConfig extends Config {

  private static MDBConfig mdbConfig = null;
  public String starterProject;

  public TelemetryConfig telemetry;
  public List<MigrationConfig> migrations;
  protected FutureJdbi jdbi;

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

  public FutureJdbi getJdbi() {
    if (this.jdbi == null) {
      // Initialize HikariCP and jdbi
      final var databaseConfig = mdbConfig.getDatabase();
      this.jdbi = initializeFutureJdbi(databaseConfig, "modeldb");
    }
    return this.jdbi;
  }
}
