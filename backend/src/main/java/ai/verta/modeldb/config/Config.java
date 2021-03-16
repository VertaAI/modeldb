package ai.verta.modeldb.config;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.common.config.ServiceUserConfig;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class Config extends ai.verta.modeldb.common.config.Config {

  private static Config config = null;
  public String starterProject;
  public ArtifactStoreConfig artifactStoreConfig;
  public TelemetryConfig telemetry;
  public TrialConfig trial;
  public List<MigrationConfig> migrations;

  public static Config getInstance() throws InternalErrorException {
    if (config == null) {
      try {
        Yaml yaml = new Yaml(new Constructor(Config.class));
        String filePath = System.getenv(ModelDBConstants.VERTA_MODELDB_CONFIG);
        filePath = CommonUtils.appendOptionalTelepresencePath(filePath);
        InputStream inputStream = new FileInputStream(filePath);
        config = yaml.loadAs(inputStream, Config.class);
        config.Validate();
      } catch (ModelDBException ex) {
        throw ex;
      } catch (NullPointerException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new InternalErrorException(ex.getMessage());
      }
    }
    return config;
  }

  public void Validate() throws InvalidConfigException {
    super.Validate();

    if (artifactStoreConfig == null)
      throw new InvalidConfigException("artifactStoreConfig", MISSING_REQUIRED);
    artifactStoreConfig.Validate("artifactStoreConfig");

    if (mdb_service_user != null) {
      mdb_service_user.Validate("mdb_service_user");
    }

    if (telemetry == null) telemetry = new TelemetryConfig();
    telemetry.Validate("telemetry");

    if (trial != null) {
      trial.Validate("trial");
    }

    if (migrations != null) {
      for (MigrationConfig migrationConfig : migrations) {
        migrationConfig.Validate("migration");
      }
    }
  }

  @Override
  public boolean hasServiceAccount() {
    return mdb_service_user != null;
  }
}
