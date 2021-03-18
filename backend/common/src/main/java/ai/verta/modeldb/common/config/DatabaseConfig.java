package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.CommonConstants;

public class DatabaseConfig {
  public String DBType;
  public Integer timeout;
  public Integer liquibaseLockThreshold = 60;
  public String changeSetToRevertUntilTag;
  public Integer minConnectionPoolSize = CommonConstants.MIN_CONNECTION_SIZE_DEFAULT;
  public Integer maxConnectionPoolSize = CommonConstants.MAX_CONNECTION_SIZE_DEFAULT;
  public Integer connectionTimeout = CommonConstants.CONNECTION_TIMEOUT_DEFAULT;

  public RdbConfig RdbConfiguration;

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
