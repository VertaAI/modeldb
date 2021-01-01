package ai.verta.modeldb.config;

public class NFSEndpointConfig {
  public String getArtifact;
  public String storeArtifact;

  public void Validate(String base) throws InvalidConfigException {
    if (getArtifact == null || getArtifact.isEmpty())
      throw new InvalidConfigException(base + ".nfsUrlProtocol", Config.MISSING_REQUIRED);
    if (storeArtifact == null || storeArtifact.isEmpty())
      throw new InvalidConfigException(base + ".storeArtifact", Config.MISSING_REQUIRED);
  }
}
