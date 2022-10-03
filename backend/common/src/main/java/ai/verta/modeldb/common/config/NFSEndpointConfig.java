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
public class NFSEndpointConfig {
  @JsonProperty private String getArtifact;
  @JsonProperty private String storeArtifact;

  public void validate(String base) throws InvalidConfigException {
    if (getArtifact == null || getArtifact.isEmpty())
      throw new InvalidConfigException(base + ".getArtifact", CommonMessages.MISSING_REQUIRED);
    if (storeArtifact == null || storeArtifact.isEmpty())
      throw new InvalidConfigException(base + ".storeArtifact", CommonMessages.MISSING_REQUIRED);
  }
}
