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
@SuppressWarnings({"squid:S100"})
public class ServiceUserConfig {
  @JsonProperty private String email;
  @JsonProperty private String devKey;

  public void validate(String base) throws InvalidConfigException {
    if (email == null || email.isEmpty())
      throw new InvalidConfigException(base + ".email", CommonMessages.MISSING_REQUIRED);
    if (devKey == null || devKey.isEmpty())
      throw new InvalidConfigException(base + ".devKey", CommonMessages.MISSING_REQUIRED);
  }
}
