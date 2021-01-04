package ai.verta.modeldb.config;

import ai.verta.modeldb.ModelDBConstants;
import java.util.List;

public class DatabaseConfig {
  public String DBType;
  public Integer timeout;
  public Integer liquibaseLockThreshold = 60;
  public String changeSetToRevertUntilTag;
  public Integer minConnectionPoolSize = ModelDBConstants.MIN_CONNECTION_SIZE_DEFAULT;
  public Integer maxConnectionPoolSize = ModelDBConstants.MAX_CONNECTION_SIZE_DEFAULT;
  public Integer connectionTimeout = ModelDBConstants.CONNECTION_TIMEOUT_DEFAULT;

  public RdbConfig RdbConfiguration;
  public List<MigrationConfig> migrations;

  public void Validate(String base) throws InvalidConfigException {
    if (DBType == null || DBType.isEmpty())
      throw new InvalidConfigException(base + ".DBType", Config.MISSING_REQUIRED);

    switch (DBType) {
      case "relational":
        if (RdbConfiguration == null)
          throw new InvalidConfigException(base + ".RdbConfiguration", Config.MISSING_REQUIRED);
        RdbConfiguration.Validate(base + ".RdbConfiguration");
        break;
      default:
        throw new InvalidConfigException(base + ".DBType", "unknown type " + DBType);
    }
  }
}
