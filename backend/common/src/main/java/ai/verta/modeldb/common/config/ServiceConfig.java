package ai.verta.modeldb.common.config;

@SuppressWarnings({"squid:S100"})
public class ServiceConfig {
  private int port;
  private String host;

  public void validate(String base) throws InvalidConfigException {
    if (port == 0) throw new InvalidConfigException(base + ".port", Config.MISSING_REQUIRED);
    if (host == null || host.isEmpty())
      throw new InvalidConfigException(base + ".host", Config.MISSING_REQUIRED);
  }

  public int getPort() {
    return port;
  }

  public String getHost() {
    return host;
  }
}
