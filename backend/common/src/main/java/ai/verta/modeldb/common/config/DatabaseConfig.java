package ai.verta.modeldb.common.config;

public class DatabaseConfig {
  public String DBType;
  public Integer timeout;
  public Integer liquibaseLockThreshold = 60;
  public String changeSetToRevertUntilTag;
  public String idleTimeout = "60000";
  public String maxLifetime = "300000";
  public String minConnectionPoolSize = "0";
  public String maxConnectionPoolSize = "20";
  public String connectionTimeout = "300";

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
