package ai.verta.modeldb.common.config;

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
public class CronJobConfig {
  @JsonProperty private int initial_delay = 30;
  @JsonProperty private int frequency = 10;
  @JsonProperty private int record_update_limit = 100;

  public void validate(String base) throws InvalidConfigException {
    // Do nothing
  }
}
