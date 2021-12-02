package ai.verta.modeldb.common.config;

@SuppressWarnings({"squid:S116", "squid:S100"})
public class DatabaseConfig {
  private String DBType;
  private Integer timeout;
  private Integer liquibaseLockThreshold = 60;
  private String changeSetToRevertUntilTag;
  private String idleTimeout = "60000";
  private String maxLifetime = "300000";
  private String minConnectionPoolSize = "0";
  private String maxConnectionPoolSize = "20";
  private int threadCount = 8;
  private String connectionTimeout = "300";
  private Long leakDetectionThresholdMs = 3000L;

  private RdbConfig RdbConfiguration;

  public void Validate(String base) throws InvalidConfigException {
    if (DBType == null || DBType.isEmpty())
      throw new InvalidConfigException(base + ".DBType", Config.MISSING_REQUIRED);

    if ("relational".equals(DBType)) {
      if (RdbConfiguration == null)
        throw new InvalidConfigException(base + ".RdbConfiguration", Config.MISSING_REQUIRED);
      RdbConfiguration.Validate(base + ".RdbConfiguration");
    } else {
      throw new InvalidConfigException(base + ".DBType", "unknown type " + DBType);
    }
  }

  public Integer getTimeout() {
    return timeout;
  }

  public Integer getLiquibaseLockThreshold() {
    return liquibaseLockThreshold;
  }

  public String getChangeSetToRevertUntilTag() {
    return changeSetToRevertUntilTag;
  }

  public String getMinConnectionPoolSize() {
    return minConnectionPoolSize;
  }

  public String getMaxConnectionPoolSize() {
    return maxConnectionPoolSize;
  }

  public int getThreadCount() {
    return threadCount;
  }

  public String getConnectionTimeout() {
    return connectionTimeout;
  }

  public RdbConfig getRdbConfiguration() {
    return RdbConfiguration;
  }

  public Long getLeakDetectionThresholdMs() {
    return leakDetectionThresholdMs;
  }

  public void setLeakDetectionThresholdMs(Long leakDetectionThresholdMs) {
    this.leakDetectionThresholdMs = leakDetectionThresholdMs;
  }
}
