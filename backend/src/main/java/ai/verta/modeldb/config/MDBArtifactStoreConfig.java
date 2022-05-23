package ai.verta.modeldb.config;

import ai.verta.modeldb.common.config.ArtifactStoreConfig;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.config.InvalidConfigException;
import ai.verta.modeldb.common.config.S3Config;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.rpc.Code;
import org.springframework.http.HttpStatus;

public class MDBArtifactStoreConfig extends ArtifactStoreConfig {
  public S3Config S3;

  public void Validate(String base) throws InvalidConfigException {
    if (getArtifactStoreType() == null || getArtifactStoreType().isEmpty())
      throw new InvalidConfigException(base + ".artifactStoreType", Config.MISSING_REQUIRED);

    switch (getArtifactStoreType()) {
      case "S3":
        if (S3 == null) throw new InvalidConfigException(base + ".S3", Config.MISSING_REQUIRED);
        S3.Validate(base + ".S3");
        break;
      case "NFS":
        if (getNFS() == null)
          throw new InvalidConfigException(base + ".NFS", Config.MISSING_REQUIRED);
        getNFS().Validate(base + ".NFS");
        break;
      default:
        throw new InvalidConfigException(
            base + ".artifactStoreType", "unknown type " + getArtifactStoreType());
    }

    if (getArtifactEndpoint() != null) {
      getArtifactEndpoint().Validate(base + ".artifactEndpoint");
    }
  }

  @Override
  public String storeTypePathPrefix() {
    switch (getArtifactStoreType()) {
      case "S3":
        return S3.storeTypePathPrefix();
      case "NFS":
        return getNFS().storeTypePathPrefix();
      default:
        throw new ModelDBException(
            "Unknown artifact store type", Code.INTERNAL, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public String getPathPrefixWithSeparator() {
    switch (getArtifactStoreType()) {
      case "S3":
        return S3.getCloudBucketPrefix();
      case "NFS":
        return getNFS().getNfsPathPrefix();
      default:
        return "";
    }
  }
}
