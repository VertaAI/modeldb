package ai.verta.modeldb.config;

import ai.verta.modeldb.exceptions.ModelDBException;
import com.google.rpc.Code;

public class ArtifactStoreConfig {
  public String artifactStoreType;
  public boolean enabled = true;
  public String protocol = "https";
  public String host = "";
  public boolean pickArtifactStoreHostFromConfig = false;

  public S3Config S3;
  public NFSConfig NFS;

  public NFSEndpointConfig artifactEndpoint;

  public void Validate(String base) throws InvalidConfigException {
    if (artifactStoreType == null || artifactStoreType.isEmpty())
      throw new InvalidConfigException(base + ".artifactStoreType", Config.MISSING_REQUIRED);

    switch (artifactStoreType) {
      case "S3":
        if (S3 == null) throw new InvalidConfigException(base + ".S3", Config.MISSING_REQUIRED);
        S3.Validate(base + ".S3");
        break;
      case "NFS":
        if (NFS == null) throw new InvalidConfigException(base + ".NFS", Config.MISSING_REQUIRED);
        NFS.Validate(base + ".NFS");
        break;
      default:
        throw new InvalidConfigException(
            base + ".artifactStoreType", "unknown type " + artifactStoreType);
    }

    if (artifactEndpoint != null) {
      artifactEndpoint.Validate(base + ".artifactEndpoint");
    }
  }

  public String storeTypePathPrefix() {
    switch (artifactStoreType) {
      case "S3":
        return S3.storeTypePathPrefix();
      case "NFS":
        return NFS.storeTypePathPrefix();
    }
    throw new ModelDBException("Unknown artifact store type", Code.INTERNAL);
  }
}
