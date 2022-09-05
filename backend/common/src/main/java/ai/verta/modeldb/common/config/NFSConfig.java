package ai.verta.modeldb.common.config;

public class NFSConfig {
  private String nfsUrlProtocol = "https";
  private String nfsRootPath;
  private String nfsServerHost = "";
  private NFSEndpointConfig artifactEndpoint;
  private String nfsPathPrefix;

  public void validate(String base) throws InvalidConfigException {
    if (nfsRootPath == null || nfsRootPath.isEmpty())
      throw new InvalidConfigException(base + ".nfsRootPath", Config.MISSING_REQUIRED);

    if (artifactEndpoint == null)
      throw new InvalidConfigException(base + ".artifactEndpoint", Config.MISSING_REQUIRED);
    artifactEndpoint.validate(base + ".artifactEndpoint");
  }

  public String storeTypePathPrefix() {
    return String.format("nfs://%s/", nfsRootPath);
  }

  public String getNfsRootPath() {
    return nfsRootPath;
  }

  public void setNfsRootPath(String nfsRootPath) {
    this.nfsRootPath = nfsRootPath;
  }

  public NFSEndpointConfig getArtifactEndpoint() {
    return artifactEndpoint;
  }

  public String getNfsPathPrefix() {
    return nfsPathPrefix == null ? "" : nfsPathPrefix + "/";
  }
}
