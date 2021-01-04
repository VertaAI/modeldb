package ai.verta.modeldb.config;

public class GrpcServerConfig {
  public int port;
  public int requestTimeout = 30;

  public void Validate(String base) throws InvalidConfigException {
    if (port == 0) throw new InvalidConfigException(base + ".port", Config.MISSING_REQUIRED);
  }
}
