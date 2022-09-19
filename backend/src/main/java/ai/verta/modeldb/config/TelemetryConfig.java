package ai.verta.modeldb.config;

import ai.verta.modeldb.common.config.InvalidConfigException;
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
public class TelemetryConfig {
  @JsonProperty private boolean opt_out = false;
  @JsonProperty private int frequency = 1;
  // TODO: add default consumer
  @JsonProperty private String consumer;

  public void validate(String base) throws InvalidConfigException {
    // Do nothing
  }
}
