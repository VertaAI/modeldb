package ai.verta.modeldb.config;

import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.common.config.S3Config;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.rpc.Code;

public class ArtifactStoreConfig extends ai.verta.modeldb.common.config.ArtifactStoreConfig {
  public S3Config S3;

  public void Validate(String base) throws InvalidConfigException {
    if (artifactStoreType == null || artifactStoreType.isEmpty())
      throw new InvalidConfigException(
          base + ".artifactStoreType", ai.verta.modeldb.common.config.Config.MISSING_REQUIRED);

    switch (artifactStoreType) {
      case "S3":
        if (S3 == null)
          throw new InvalidConfigException(
              base + ".S3", ai.verta.modeldb.common.config.Config.MISSING_REQUIRED);
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
