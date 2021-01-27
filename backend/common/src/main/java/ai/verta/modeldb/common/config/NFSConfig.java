package ai.verta.modeldb.common.config;

public class NFSConfig {
  public String nfsUrlProtocol = "https";
  public String nfsRootPath;
  public String nfsServerHost = "";
  public NFSEndpointConfig artifactEndpoint;

  public void Validate(String base) throws InvalidConfigException {
    if (nfsRootPath == null || nfsRootPath.isEmpty())
      throw new InvalidConfigException(base + ".nfsRootPath", Config.MISSING_REQUIRED);

    if (artifactEndpoint == null)
      throw new InvalidConfigException(base + ".artifactEndpoint", Config.MISSING_REQUIRED);
    artifactEndpoint.Validate(base + ".artifactEndpoint");
  }

  public String storeTypePathPrefix() {
    return String.format("nfs://%s/", nfsRootPath);
  }
}
