package ai.verta.modeldb.config;

import ai.verta.modeldb.common.CommonMessages;
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
public class MigrationConfig {
  @JsonProperty public String name;
  @JsonProperty public boolean enabled = false;
  @JsonProperty public int record_update_limit = 100;

  public void validate(String base) throws InvalidConfigException {
    if (name == null || name.isEmpty())
      throw new InvalidConfigException(base + ".name", CommonMessages.MISSING_REQUIRED);
  }
}
