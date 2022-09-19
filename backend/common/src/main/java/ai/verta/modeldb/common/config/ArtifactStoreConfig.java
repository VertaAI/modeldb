package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.rpc.Code;
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
public class ArtifactStoreConfig {
  @JsonProperty private String artifactStoreType;
  @JsonProperty private boolean pickArtifactStoreHostFromConfig = false;
  @JsonProperty private boolean enabled = true;
  @JsonProperty private String protocol = "https";
  @JsonProperty private String host = "";

  @SuppressWarnings({"squid:S116"})
  @JsonProperty
  private S3Config S3;

  @SuppressWarnings({"squid:S116"})
  @JsonProperty
  private NFSConfig NFS;

  @JsonProperty private NFSEndpointConfig artifactEndpoint;

  public void validate(String base) throws InvalidConfigException {
    if (getArtifactStoreType() == null || getArtifactStoreType().isEmpty()) {
      throw new InvalidConfigException(
          base + ".artifactStoreType", CommonMessages.MISSING_REQUIRED);
    }

    switch (getArtifactStoreType()) {
      case "S3":
        if (getS3() == null) {
          throw new InvalidConfigException(base + ".S3", CommonMessages.MISSING_REQUIRED);
        }
        getS3().validate(base + ".S3");
        break;
      case "NFS":
        if (getNFS() == null) {
          throw new InvalidConfigException(base + ".NFS", CommonMessages.MISSING_REQUIRED);
        }
        getNFS().validate(base + ".NFS");
        break;
      default:
        throw new InvalidConfigException(
            base + ".artifactStoreType", "unknown type " + getArtifactStoreType());
    }

    if (getArtifactEndpoint() != null) {
      getArtifactEndpoint().validate(base + ".artifactEndpoint");
    }
  }

  public String storeTypePathPrefix() {
    switch (getArtifactStoreType()) {
      case "S3":
        return getS3().storeTypePathPrefix();
      case "NFS":
        return getNFS().storeTypePathPrefix();
      default:
        throw new ModelDBException("Unknown artifact store type", Code.INTERNAL);
    }
  }

  public String getPathPrefixWithSeparator() {
    switch (getArtifactStoreType()) {
      case "S3":
        return getS3().getCloudBucketPrefix();
      case "NFS":
        return getNFS().getNfsPathPrefix();
      default:
        return "";
    }
  }
}
