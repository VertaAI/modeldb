package ai.verta.modeldb.common.config;

public class NFSEndpointConfig {
  private String getArtifact;
  private String storeArtifact;

  public void Validate(String base) throws InvalidConfigException {
    if (getArtifact == null || getArtifact.isEmpty())
      throw new InvalidConfigException(base + ".getArtifact", Config.MISSING_REQUIRED);
    if (storeArtifact == null || storeArtifact.isEmpty())
      throw new InvalidConfigException(base + ".storeArtifact", Config.MISSING_REQUIRED);
  }

  public String getGetArtifact() {
    return getArtifact;
  }

  public String getStoreArtifact() {
    return storeArtifact;
  }
}
