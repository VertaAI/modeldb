package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.CommonMessages;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter(AccessLevel.NONE)
@SuppressWarnings({"squid:S116", "squid:S100"})
public class DatabaseConfig {
  @JsonProperty private String DBType;
  @JsonProperty private Integer timeout;
  @JsonProperty private Integer liquibaseLockThreshold = 60;
  @JsonProperty private String changeSetToRevertUntilTag;
  @JsonProperty private String idleTimeout = "60000";
  @JsonProperty private String maxLifetime = "300000";
  @JsonProperty private String minConnectionPoolSize = "0";
  @JsonProperty private String maxConnectionPoolSize = "20";
  @JsonProperty private int threadCount = 8;
  @JsonProperty private String connectionTimeout = "5000"; //note: milliseconds
  @JsonProperty private Long leakDetectionThresholdMs = 3000L;

  @JsonProperty private RdbConfig RdbConfiguration;

  public void validate(String base) throws InvalidConfigException {
    if (DBType == null || DBType.isEmpty())
      throw new InvalidConfigException(base + ".DBType", CommonMessages.MISSING_REQUIRED);

    if ("relational".equals(DBType)) {
      if (RdbConfiguration == null)
        throw new InvalidConfigException(
            base + ".RdbConfiguration", CommonMessages.MISSING_REQUIRED);
      RdbConfiguration.validate(base + ".RdbConfiguration");
    } else {
      throw new InvalidConfigException(base + ".DBType", "unknown type " + DBType);
    }
  }

  public long getConnectionTimeoutMillis() {
    return Long.parseLong(connectionTimeout);
  }
}
