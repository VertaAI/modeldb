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
public class NFSConfig {
  @JsonProperty private String nfsUrlProtocol = "https";
  @JsonProperty private String nfsRootPath;
  @JsonProperty private String nfsServerHost = "";
  @JsonProperty private NFSEndpointConfig artifactEndpoint;
  @JsonProperty private String nfsPathPrefix;

  public void validate(String base) throws InvalidConfigException {
    if (nfsRootPath == null || nfsRootPath.isEmpty())
      throw new InvalidConfigException(base + ".nfsRootPath", CommonMessages.MISSING_REQUIRED);

    if (artifactEndpoint == null)
      throw new InvalidConfigException(base + ".artifactEndpoint", CommonMessages.MISSING_REQUIRED);
    artifactEndpoint.validate(base + ".artifactEndpoint");
  }

  public String storeTypePathPrefix() {
    return String.format("nfs://%s/", nfsRootPath);
  }

  public void setNfsRootPath(String nfsRootPath) {
    this.nfsRootPath = nfsRootPath;
  }
}
