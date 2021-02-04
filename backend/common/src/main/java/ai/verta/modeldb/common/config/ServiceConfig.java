package ai.verta.modeldb.common.config;

public class ServiceConfig {
  public int port;
  public String host;

  public void Validate(String base) throws InvalidConfigException {
    if (port == 0) throw new InvalidConfigException(base + ".port", Config.MISSING_REQUIRED);
    if (host == null || host.isEmpty())
      throw new InvalidConfigException(base + ".host", Config.MISSING_REQUIRED);
  }
}
