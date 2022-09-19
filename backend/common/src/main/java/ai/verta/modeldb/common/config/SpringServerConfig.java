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
@Setter(AccessLevel.PRIVATE)
@SuppressWarnings({"squid:S100"})
public class SpringServerConfig {
  @JsonProperty private int port;
  @JsonProperty private Long shutdownTimeout = 30L;
  @JsonProperty private int threadCount = 32;

  public void validate(String base) throws InvalidConfigException {
    if (port == 0)
      throw new InvalidConfigException(base + ".port", CommonMessages.MISSING_REQUIRED);
  }

  public void setPort(int port) {
    this.port = port;
  }
}
