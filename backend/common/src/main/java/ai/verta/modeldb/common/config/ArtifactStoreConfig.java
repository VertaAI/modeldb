package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.rpc.Code;

public class ArtifactStoreConfig {
  private String artifactStoreType;
  private boolean pickArtifactStoreHostFromConfig = false;
  private boolean enabled = true;
  private String protocol = "https";
  private String host = "";

  private S3Config s3;

  private NFSConfig nfs;

  private NFSEndpointConfig artifactEndpoint;

  public void validate(String base) throws InvalidConfigException {
    if (getArtifactStoreType() == null || getArtifactStoreType().isEmpty()) {
      throw new InvalidConfigException(base + ".artifactStoreType", Config.MISSING_REQUIRED);
    }

    switch (getArtifactStoreType()) {
      case "S3":
        if (s3 == null) {
          throw new InvalidConfigException(base + ".S3", Config.MISSING_REQUIRED);
        }
        s3.validate(base + ".S3");
        break;
      case "NFS":
        if (getNfs() == null) {
          throw new InvalidConfigException(base + ".NFS", Config.MISSING_REQUIRED);
        }
        getNfs().validate(base + ".NFS");
        break;
      default:
        throw new InvalidConfigException(
            base + ".artifactStoreType", "unknown type " + getArtifactStoreType());
    }

    if (getArtifactEndpoint() != null) {
      getArtifactEndpoint().validate(base + ".artifactEndpoint");
    }
  }

  public String getArtifactStoreType() {
    return artifactStoreType;
  }

  public boolean isPickArtifactStoreHostFromConfig() {
    return pickArtifactStoreHostFromConfig;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getHost() {
    return host;
  }

  public S3Config getS3() {
    return s3;
  }

  public NFSConfig getNfs() {
    return nfs;
  }

  public NFSEndpointConfig getArtifactEndpoint() {
    return artifactEndpoint;
  }

  public String storeTypePathPrefix() {
    switch (getArtifactStoreType()) {
      case "S3":
        return s3.storeTypePathPrefix();
      case "NFS":
        return getNfs().storeTypePathPrefix();
      default:
        throw new ModelDBException("Unknown artifact store type", Code.INTERNAL);
    }
  }

  public String getPathPrefixWithSeparator() {
    switch (getArtifactStoreType()) {
      case "S3":
        return s3.getCloudBucketPrefix();
      case "NFS":
        return getNfs().getNfsPathPrefix();
      default:
        return "";
    }
  }
}
