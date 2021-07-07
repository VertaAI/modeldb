package ai.verta.modeldb.config;

import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.common.config.ArtifactStoreConfig;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import java.util.List;

public class Config extends ai.verta.modeldb.common.config.Config {

  private static Config config = null;
  public String starterProject;
  public TelemetryConfig telemetry;
  public List<MigrationConfig> migrations;
  protected FutureJdbi jdbi;

  public static Config getInstance() throws InternalErrorException {
    if (config == null) {
      config = getInstance(Config.class, ModelDBConstants.VERTA_MODELDB_CONFIG);
      config.Validate();
    }
    return config;
  }

  public void Validate() throws InvalidConfigException {
    super.Validate();

    if (service_user != null) {
      service_user.Validate("service_user");
    }

    if (telemetry == null) telemetry = new TelemetryConfig();
    telemetry.Validate("telemetry");

    if (migrations != null) {
      for (MigrationConfig migrationConfig : migrations) {
        migrationConfig.Validate("migration");
      }
    }
  }

  @Override
  public boolean hasServiceAccount() {
    return service_user != null;
  }

  public FutureJdbi getJdbi() {
    if (this.jdbi == null) {
      // Initialize HikariCP and jdbi
      final var databaseConfig = config.database;
      this.jdbi = initializeFutureJdbi(databaseConfig, "modeldb");
    }
    return this.jdbi;
  }
}
