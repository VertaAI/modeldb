package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.CommonMessages;
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
@SuppressWarnings({"squid:S116", "squid:S100"})
public class GrpcServerConfig {
  @JsonProperty private int port;
  @JsonProperty private int threadCount = 8;
  @JsonProperty private int requestTimeout = 30;
  @JsonProperty private int metrics_port = 8087;
  @JsonProperty private Integer maxInboundMessageSize = 4194304; // bytes

  public void validate(String base) throws InvalidConfigException {
    if (port == 0)
      throw new InvalidConfigException(base + ".port", CommonMessages.MISSING_REQUIRED);
  }
}
