package ai.verta.modeldb.common.config;

public abstract class ArtifactStoreConfig {

  private String artifactStoreType;
  private boolean pickArtifactStoreHostFromConfig = false;
  private boolean enabled = true;
  private String protocol = "https";
  private String host = "";

  @SuppressWarnings({"squid:S116"})
  private NFSConfig NFS;

  private NFSEndpointConfig artifactEndpoint;

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

  public NFSConfig getNFS() {
    return NFS;
  }

  public NFSEndpointConfig getArtifactEndpoint() {
    return artifactEndpoint;
  }

  public abstract String storeTypePathPrefix();
}
